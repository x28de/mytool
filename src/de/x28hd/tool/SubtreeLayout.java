package de.x28hd.tool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import edu.uci.ics.jung.algorithms.layout.PolarPoint;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class SubtreeLayout {

	RadialTreeLayout<Integer,Integer> layout;
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	GraphPanelControler controler;
	Point translation;
	Hashtable<GraphNode,Point> originalLocations = new Hashtable<GraphNode,Point>();
	String loopMsg = "This is no tree; loops were detected at:\n\n";
	Hashtable<GraphEdge,String> normalColors = new Hashtable<GraphEdge,String>();
	Hashtable<GraphEdge,String> alertColors = new Hashtable<GraphEdge,String>();
	
	public SubtreeLayout(GraphNode clickedNode, Hashtable<Integer,GraphNode> nodes, 
			Hashtable<Integer,GraphEdge> edges, GraphPanelControler controler,
			Point translation) {
		new SubtreeLayout(clickedNode, nodes, edges, controler, translation, false);
	}
	public SubtreeLayout(GraphNode clickedNode, Hashtable<Integer,GraphNode> nodes, 
			Hashtable<Integer,GraphEdge> edges, GraphPanelControler controler, 
			Point translation, boolean silent) {
		this.nodes = nodes;
		this.edges = edges;
		this.controler = controler;
		this.translation = translation;
		
		//	Save original locations
		Enumeration<GraphNode> backupList = nodes.elements();
		while (backupList.hasMoreElements()) {
			GraphNode node = backupList.nextElement();
			Point originalLocation = node.getXY();
			originalLocations.put(node, originalLocation);
		}
		
		DirectedSparseGraph<Integer,Integer> graph = 
				new DirectedSparseGraph<Integer,Integer>();
		
		Enumeration<GraphEdge> neighbors = clickedNode.getEdges();
		while (neighbors.hasMoreElements()) {
			
			// First level (immediate neighbors of clickedNode)
			HashSet<GraphNode> done = new HashSet<GraphNode>();
			HashSet<GraphEdge> scheduledEdges = new HashSet<GraphEdge>();
			Hashtable<GraphEdge,Boolean> reversed = new Hashtable<GraphEdge,Boolean>();
			GraphEdge edge = neighbors.nextElement();
			GraphNode related = clickedNode.relatedNode(edge);
			int edgeID = edge.getID();
			int n1 = clickedNode.getID();
			int n2 = related.getID();
			
			// More levels
			if (!growGraph(related, clickedNode, 0, done, scheduledEdges, reversed)) {
				alertColors.put(edge, "#ff00ff");	// and forget that branch that loops
			} else {
				scheduledEdges.add(edge);
				reversed.put(edge, (edge.getNode2() == clickedNode));
				Iterator<GraphEdge> validEdges = scheduledEdges.iterator();
				while (validEdges.hasNext()) {
					GraphEdge validEdge = validEdges.next();
					alertColors.put(validEdge, "#00ffff");
					GraphNode node1 = validEdge.getNode1();
					GraphNode node2 = validEdge.getNode2();
					n1 = node1.getID();
					n2 = node2.getID();
					edgeID = validEdge.getID();
					EdgeType edgeType = EdgeType.DIRECTED; 
					if (reversed.get(validEdge)) {
						n2 = node1.getID();
						n1 = node2.getID();
					}
					try {
						graph.addEdge(edgeID, n1, n2, edgeType);
					} catch (java.lang.IllegalArgumentException e) {
						System.out.println("Error SL101: unexpectedly failed at " + node1.getLabel() + " -> " + node2.getLabel());
						break;
					}
				}
			}
		}
		
		// Loops to show?
		Collection<Integer> nodeIDs = graph.getVertices();
		if (graph.getVertexCount() == 0) {
			if (silent) return;
			colorFeedback(loopMsg, clickedNode);
			controler.getMainWindow().repaint();
			return;
		}

		// Determine slope of tree
		Iterator<Integer> allNodes = nodeIDs.iterator();
		int sumX = 0;
		int sumY = 0;
		int count = 0;
		while (allNodes.hasNext()) {
			count++;
			int nodeID = allNodes.next();
			GraphNode node = nodes.get(nodeID);
			Point xy = node.getXY();
			int x = xy.x;
			int y = xy.y;
			sumX += x;
			sumY += y;
		}
		Point root = clickedNode.getXY();
		double dx = sumX/count - root.x;
		double dy = sumY/count - root.y;
		
		Point slope = new Point((int) dx, (int) -dy);
		PolarPoint polarSlope = PolarPoint.cartesianToPolar(slope);
		double myTheta = polarSlope.getTheta();
		
		// Calculate polar locations
		DelegateForest<Integer,Integer> forest = new DelegateForest<Integer,Integer>(graph);
		layout = new RadialTreeLayout<Integer,Integer>(forest);
//		layout = new BalloonLayout<Integer,Integer>(forest);
//		layout = new TreeLayout<Integer,Integer>(forest, 50, 20);
		int size = graph.getVertexCount();	// copied from CentralityColoring()
		layout.setSize(new Dimension(400 + (200 * ((int) Math.sqrt(size))), 
				300 + (150 * ((int) Math.sqrt(size)))));
		HashMap<Integer,PolarPoint> map = new HashMap<Integer,PolarPoint>();
		map = (HashMap<Integer,PolarPoint>) layout.getPolarLocations();

		//	Rotate to my orientation
		int translateX = 0;
		int translateY = 0;
		Iterator<Integer> nodeIt = nodeIDs.iterator();
		while (nodeIt.hasNext()) {
			int id = nodeIt.next();
			PolarPoint pp = map.get(id);
			double theirTheta = pp.getTheta();
			// just a 30% sector, and rotated by my slope
			double tweak = .16;	// not satisfying (still jerking clockwise?)
			pp.setTheta(theirTheta * .3 - myTheta - tweak);
			
			Point2D p = PolarPoint.polarToCartesian(pp);
			double x = p.getX();
			double y = p.getY();
			
			int ix = (int) x;
			int iy = (int) y;
			GraphNode node = nodes.get(id);
			if (node == clickedNode) {
				Point oldXY = node.getXY();
				translateX = oldXY.x - ix;
				translateY = oldXY.y - iy;
			}
			node.setXY(new Point(ix, iy));
		}
		
		//  Shift to clickedNode
		Iterator<Integer> anotherIterator = nodeIDs.iterator();
		while (anotherIterator.hasNext()) {
			int id = anotherIterator.next();
			GraphNode node = nodes.get(id);
			Point xy = node.getXY();
			xy.translate(translateX, translateY);
			node.setXY(xy);
		}
		
		//	Ask user
		if (!silent) {
			confirmLocations(null, clickedNode);
		} else {
			// colorFeedback(null, clickedNode);	// for diagnosis
		}
	}
	
	public boolean growGraph(GraphNode node, GraphNode previous, int level, 
			HashSet<GraphNode> done, HashSet<GraphEdge> scheduledEdges,
			Hashtable<GraphEdge,Boolean> reversed) {
		done.add(node);
		Enumeration<GraphEdge> neighbors = node.getEdges();
		while (neighbors.hasMoreElements()) {
			GraphEdge edge = neighbors.nextElement();
			GraphNode related = node.relatedNode(edge);
			if (related == previous) {
				continue;
			}
			if (done.contains(related)) {
				loopMsg += node.getLabel() + " -> " + related.getLabel() + ";\n";
				return false;
			}
			done.add(related);
			if (!growGraph(related, node, level + 1, done, scheduledEdges, reversed)) {	//  recursion
				alertColors.put(edge, "#ff00ff");
				return false;
			}
			scheduledEdges.add(edge);
			reversed.put(edge, (edge.getNode2() == node));
		}
		return true;
	}
	
//
//	Accessories
	
	public void confirmLocations(String errorMsg, GraphNode clickedNode) {
		if (colorFeedback(errorMsg, clickedNode)) return;
		
		Enumeration<GraphNode> relocationBack = nodes.elements();
		while (relocationBack.hasMoreElements()) {
			GraphNode node = relocationBack.nextElement();
			Point location = originalLocations.get(node);
			node.setXY(location);
		}
		controler.getMainWindow().repaint();
	}
	
	public boolean colorFeedback(String errorMsg, GraphNode node) {
		boolean confirmed = true;
		Enumeration<GraphEdge> coloringList = edges.elements();
		
		// Highlight changes (turquoise) and garden paths (purple)
		while (coloringList.hasMoreElements()) {
			GraphEdge edge = coloringList.nextElement();
			if (!alertColors.containsKey(edge)) continue;
			String colorString = alertColors.get(edge);
			Color originalColor = edge.getColor();
			int r = originalColor.getRed();
			int g = originalColor.getGreen();
			int b = originalColor.getBlue();
			normalColors.put(edge, String.format("#%02x%02x%02x", r, g, b));
			edge.setColor(colorString);
		}
		
		// Ask or inform the user
		if (errorMsg == null) {  // layout to accept
			JOptionPane confirm = new JOptionPane("Accept the highlighted location change?", 
					JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION); 
			JDialog d = confirm.createDialog(controler.getMainWindow(), "Warning");
			Point p = node.getXY();
			d.setLocation(p.x - 20 + translation.x, p.y + 100 + translation.y);
			d.setVisible(true);
			Object responseObj = confirm.getValue();
			if (responseObj == null) confirmed = false;
			if ((int) responseObj != JOptionPane.YES_OPTION) confirmed = false;
		} else {  // error to acknowledge
			controler.displayPopup(errorMsg);
		}

		// Revert the highlighting colors
		Enumeration<GraphEdge> coloringBack = edges.elements();
		while (coloringBack.hasMoreElements()) {
			GraphEdge edge = coloringBack.nextElement();
			if (!alertColors.containsKey(edge)) continue;
			String colorString = normalColors.get(edge);
			edge.setColor(colorString);
		}
		controler.getMainWindow().repaint();
		return confirmed;
	}
}

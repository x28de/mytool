package de.x28hd.tool.layouts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import de.x28hd.tool.GraphPanel;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class DagLayered implements ActionListener {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	PresentationService controler;
	GraphPanel graphPanel;
	
	Rectangle bounds;
	int newHeight;
	int middle;
	int horizontalDist = 180;
	
	Hashtable<GraphNode,Integer> layers = new Hashtable<GraphNode,Integer>();
	Hashtable<Integer,Vector<GraphNode>> layerSets = new Hashtable<Integer,Vector<GraphNode>>();
	Hashtable<Integer,Vector<GraphEdge>> edgeSets = new Hashtable<Integer,Vector<GraphEdge>>();
	Vector<GraphNode> layerSet = null;
	int len;
	HashSet<GraphNode> dummyNodes = new HashSet<GraphNode>();
	HashSet<GraphEdge> dummyEdges = new HashSet<GraphEdge>();
	HashSet<GraphNode[]> hiddenEdges = new HashSet<GraphNode[]>();
	Vector<GraphNode> varySet;
	Vector<GraphEdge> edgeSet;
	Vector<Point> xySet;
	
	JFrame mainWindow;
	boolean ask = true;
	boolean autoOptimize = false;
	int attempted;
	int successes;
	int errors = 0;
	boolean abort = false;
	JDialog nextReady;
	
	//Trying to follow the algorithm outlined in https://en.wikipedia.org/wiki/Layered_graph_drawing	
	
	public DagLayered(	Hashtable<Integer,GraphNode> n, Hashtable<Integer,GraphEdge> e,
			PresentationService c) {
		// expects nodes ordered by y
		nodes = n;
		edges = e;
		controler = c;
		graphPanel = controler.getGraphPanel();
		bounds = controler.getControlerExtras().getBounds();
		newHeight = 20 * nodes.size();
		
		if (!assignLayers()) {
			controler.displayPopup("Aborted with " + errors + " error(s)");
			return;
		};
		placement();
		
		// resize before modal popups
		mainWindow = controler.getMainWindow();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int w = dim.width - 80;
		int h = dim.height - 80;
		mainWindow.setSize(new Dimension(w, h));
		Object[] options =  {"No", "Yes"};
		int	response = JOptionPane.showOptionDialog(null,
				"Should we try to reduce crossings"
				+ "\n (probably disappointing) ?",
				"Option", JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, 
				options, options[0]);
		autoOptimize = (response != 0);
		if (autoOptimize) {
			Object[] options3 =  {"No", "Yes"};
			int	response3 = JOptionPane.showOptionDialog(null,
					"Add dummy nodes on the layers ?",
					"Option", JOptionPane.YES_NO_OPTION, 
					JOptionPane.QUESTION_MESSAGE, null, 
					options3, options3[0]);
			if (response3 != 0) addDummyNodes();
		}
		
		for (int i = 0; i < layerSets.size(); i++) {
			adjustToNeighbors(i);
		}
		
		graphPanel.repaint();
		if (!autoOptimize) {
			prepareRedraw();
			return;
		}
		
		boolean loop = true;
		while (loop) {
			int varyWin = 0;
			for (int i = 1; i < layerSets.size(); i++) {
				varyWin += variation(i);
			}
			if (varyWin > 0) {
				Object[] options2 =  {"No", "Yes"};
				int	response2 = JOptionPane.showOptionDialog(null,
						"<html><font color=\"red\">" + varyWin + " crossings removed,<br>try more?</font></html>",
						"Option", JOptionPane.YES_NO_OPTION, 
						JOptionPane.QUESTION_MESSAGE, null, 
						options2, options2[1]);
				if (response2 == 0) {
					loop = false;
				}
			} else loop = false;
		}
		restoreHiddenEdges();
		graphPanel.repaint();
		controler.displayPopup("Finished with " + errors + " error(s)."
				+ "\nNow you can manually change the vertical sequence.");
		prepareRedraw();
	}
	
	public boolean assignLayers() {
		
		TopologicalSorting topologicalSorting = new TopologicalSorting(nodes, edges, controler);
		GraphNode[] array = topologicalSorting.getArray();
		for (int i = 0; i < array.length; i++) {
			GraphNode node = array[i];
			node.setXY(new Point(40, i * 50));
		}
		// sort vertically from former to latter
		TreeMap<Integer,GraphNode> yMap = new TreeMap<Integer,GraphNode>();
		SortedMap<Integer,GraphNode> yList = (SortedMap<Integer,GraphNode>) yMap;
		
		Enumeration<GraphNode> nodeList2 = nodes.elements();
		while (nodeList2.hasMoreElements()) {
			GraphNode node = nodeList2.nextElement();
			Point xy = node.getXY();
			yMap.put(xy.y, node);
			node.setXY(new Point(xy.y, xy.x));
		}
		graphPanel.repaint();
		SortedSet<Integer> ySet = (SortedSet<Integer>) yList.keySet();

		String[] colors = { "#ffbbbb", "#ffe8aa", "#ffff99", "#bbffbb", "#bbbbff", "#d2bbd2", 
							"#ff0000", "#ffc800", "#ffff00", "#00ff00", "#0000ff", "#b200b2"}; 

		int layer = -1;
		
		// Vertically iterate through nodes
		Iterator<Integer> iter = ySet.iterator();
		while (iter.hasNext()) {
			int nextY = iter.next();
			GraphNode node = yMap.get(nextY);
			int maxLayer = Integer.MIN_VALUE;
			Enumeration<GraphEdge> neighbors = node.getEdges();
			int ancestors = 0;

			// Find highest layer among the above nodes
			while (neighbors.hasMoreElements()) {
				GraphEdge edge = neighbors.nextElement();
				GraphNode node2 = node.relatedNode(edge);
				if (node2.getXY().x > node.getXY().x) continue;
				ancestors++;
				if (!layers.containsKey(node2)) {
					System.out.println("Error DL101 " + node2.getLabel());
					errors++;
					return false;
				}
				int layer2 = layers.get(node2);
				if (layer2 > maxLayer) maxLayer = layer2;
			}
			if (ancestors == 0) maxLayer = -1;
			maxLayer++;
			layer = maxLayer;
			layers.put(node, layer);
			node.setColor(colors[layer % 12]);
		}
		
		// Lower the source nodes and create layerSets
		Iterator<Integer> iter3 = ySet.iterator();
		while (iter3.hasNext()) {
			int nextY = iter3.next();
			GraphNode node = yMap.get(nextY);
			layer = layers.get(node);
			
			if (layer == 0) {
				int minLayer = Integer.MAX_VALUE;
				Enumeration<GraphEdge> neighbors = node.getEdges();

				// Find lowest layer among the below nodes
				while (neighbors.hasMoreElements()) {
					GraphEdge edge = neighbors.nextElement();
					GraphNode node2 = node.relatedNode(edge);
					if (node2.getXY().x < node.getXY().x) continue;
					if (!layers.containsKey(node2)) {
						System.out.println("Error DL102 " + node2.getLabel());
						errors++;
						continue;
					}
					int layer2 = layers.get(node2);
					if (layer2 < minLayer) minLayer = layer2;
				}
				minLayer--;
				layer = minLayer;
				layers.put(node, layer);
			}
			
			// Store 
			if (!layerSets.containsKey(layer)) {
				layerSet = new Vector<GraphNode>();
				layerSets.put(layer, layerSet);
			}
			layerSet = layerSets.get(layer);
			layerSet.add(node);
			int y = node.getXY().y;
			node.setXY(new Point(40 + (layer * horizontalDist), y));
		}
		return true;
	}
	
	public void addDummyNodes() {

		// Insert dummy nodes
		boolean alternate = true;
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			GraphNode node1 = edge.getNode1();
			GraphNode node2 = edge.getNode2();
			int layer1 = layers.get(node1);
			int y1 = node1.getXY().y;
			int layer2 = layers.get(node2);
			
			GraphNode source = layer1 < layer2 ? node1 : node2;
			GraphNode target = layer1 < layer2 ? node2 : node1;
			GraphNode from = source;
			GraphNode to = null;
			GraphNode dummy = null;
			String edgeDescr = source.getLabel() + " - " + target.getLabel();
			int first = layers.get(source);
			int last = layers.get(target);
			boolean replaced = false;
			for (int i = first + 1; i < last; i++) {
				int x = 40 + i * horizontalDist;
				dummy = createNode(new Point(x, y1), edgeDescr);
				dummyNodes.add(dummy);
				replaced = true;
				layers.put(dummy, i);
				layerSet = layerSets.get(i);
				if (alternate) {
					layerSet.add(dummy);
				} else {
					layerSet.add(0, dummy);
				}
				alternate = !alternate;
				to = dummy;
				GraphEdge newEdge = createEdge(from, to);
				dummyEdges.add(newEdge);
				from = dummy;
				if (i == last - 1) {
					GraphEdge lastEdge = createEdge(from, target);
					dummyEdges.add(lastEdge);
				}
			}
			if (replaced) {
				GraphNode[] hiddenEdge = new GraphNode[2]; 
				hiddenEdge[0] = source;
				hiddenEdge[1] = target;
				hiddenEdges.add(hiddenEdge);
				edges.remove(edge.getID());
			}
			replaced = false;
		}
	}
	
	public void placement() {
		
		// Initial vertical placement: evenly distributed 
		distributeEvenly();

		
		// Identify edges between adjacent layers i and i - 1
		
		for (int i = 1; i < layerSets.size(); i++) {
			edgeSet = new Vector<GraphEdge>();
			edgeSets.put(i, edgeSet);
			Enumeration<GraphEdge> edgeList2 = edges.elements();
			while (edgeList2.hasMoreElements()) {
				GraphEdge edge = edgeList2.nextElement();
				GraphNode node1 = edge.getNode1();
				int layer1 = layers.get(node1);
				if (layer1 > i + 1 || layer1 < i - 1) continue;
				GraphNode node2 = edge.getNode2();
				int layer2 = layers.get(node2);
				if (layer2 > i + 1 || layer2 < i - 1) continue;
				edgeSet.add(edge);
			}
		}
	}
		
	public void adjustToNeighbors(int i) {	
		
		// average y of node's neighbors in adjacent layers
		
		TreeMap<Double,GraphNode> vertiMap = new TreeMap<Double,GraphNode>();
		SortedMap<Double,GraphNode> vertiList = (SortedMap<Double,GraphNode>) vertiMap;
		SortedSet<Double> vertiSet = (SortedSet<Double>) vertiList.keySet();
		if (!layerSets.containsKey(i)) {
			errors++;
			return;
		}
		Vector<GraphNode> current = layerSets.get(i);
		Iterator<GraphNode> placIter = current.iterator();

		while (placIter.hasNext()) {
			GraphNode node = placIter.next();
			Enumeration<GraphEdge> neighbors = node.getEdges();
			int sumY = 0;
			int count = 0;
			while (neighbors.hasMoreElements()) {
				GraphEdge edge = neighbors.nextElement();
				GraphNode other = node.relatedNode(edge);
				Point xy = (Point) other.getXY();
				sumY += xy.y;
				count++;
			}
			if (count == 0) continue;
			int averageY = (int) (sumY + .0) / count;
			double disambig = .00001;
			double uniq = averageY;
			while (vertiMap.containsKey(uniq)) uniq += disambig;
			vertiMap.put(uniq, node);
		}

		// place by sorted averages
		Iterator<Double> vertiIter = vertiSet.iterator();
		int vertiCount = vertiSet.size();
		int index = 0;
		current.clear();
		while (vertiIter.hasNext()) {
			double yDouble = vertiIter.next();
			GraphNode node = vertiMap.get(yDouble);
			int x = node.getXY().x;
			int y = middle;
			if (vertiCount != 1) {
				int distance = newHeight / (vertiCount - 1);
				y = bounds.y + index * distance;
			}
			node.setXY(new Point(x, y));
			current.add(index, node);
			index++;
		}
		distributeEvenly();
	}
	
	public void distributeEvenly() {
		middle = bounds.y + newHeight/2;
		for (int i = 0; i < layerSets.size(); i++) {
			if (!layerSets.containsKey(i)) continue;
			layerSet = layerSets.get(i);
			int verti = layerSet.size();
			for (int n = 0; n < verti; n++) {
				GraphNode node = layerSet.get(n);
				int x = node.getXY().x;
				int y = middle;
				if (verti != 1) {
					int distance = newHeight / (verti - 1);
					y = bounds.y + n * distance;
				}
				node.setXY(new Point(x, y));
			}
		}
	}
	
	public int variation(int i) {
		if (!layerSets.containsKey(i)) {
			errors++;
			return 0;
		}
		varySet = layerSets.get(i);
		len = varySet.size();
		System.out.println("\nProcessing layer " + i + " with " + len + " nodes");
		xySet = new Vector<Point>();
		if (len == 1) return 0;
		Vector<Integer> a = new Vector<Integer>();
		int n = len;
		for (int j = 0; j < n; j++) {
			a.add(j);
			GraphNode node = varySet.get(j);
			xySet.add(j, node.getXY());
		}
		successes = 0;
		edgeSet = edgeSets.get(i);
		attempted = 0;
		
		permutation(a, n);
		
		graphPanel.repaint();
		return successes;
	}
    
	public void permutation(Vector<Integer> a, int n) {
		// Adapted from https://introcs.cs.princeton.edu/java/23recursion/Permutations.java.html
		int diff = 0;
		if (abort) return;
		
		if (n == 1) {				// final determination of the permutation
			Vector<Point> oldXY = new Vector<Point>();			
			Vector<Point> newXY = new Vector<Point>();			
			
			// Save old and new positions
			for (int j = 0; j < varySet.size(); j++) {
				GraphNode node = varySet.get(j);
				oldXY.add(j, node.getXY());
				int newIndex = a.get(j);
				node = varySet.get(newIndex);
				newXY.add(j, node.getXY());
			}
			
			// Count crossings
			int crossings = countCrossings(edgeSet);
			int crossings2 = Integer.MAX_VALUE;
			
			// Move point
			for (int j = 0; j < varySet.size(); j++) {
				GraphNode node = varySet.get(j);
				Point xy = newXY.get(j);
				node.setXY(xy);
			}
			highLightSwap(a);		// visualize 
			
			// Count crossings again
			crossings2 = countCrossings(edgeSet);
			attempted++;
			if (crossings2 >= crossings) {
				// Reset
				for (int j = 0; j < varySet.size(); j++) {
					GraphNode node = varySet.get(j);
					Point xy = oldXY.get(j);
					node.setXY(xy);
				}
				if (attempted % 1000 == 0) System.out.println(attempted + " attempts");
				if (attempted % 10000 == 0) {
					Object[] options2 =  {"Abort", "Continue"};
					int	response2 = JOptionPane.showOptionDialog(null,
							"<html><font color=\"red\">" + attempted + " attempts failed; continue?</font></html>",
							"Option", JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE, null, 
							options2, options2[1]);
					if (response2 == 0) {
						abort = true;
						return;
					}
				}
			} else {
				diff = crossings - crossings2;
				successes += diff;
				attempted = 0;
			}
			return;
		}
		for (int i = 0; i < n; i++) {
			swap(a, i, n-1);
			permutation(a, n-1);				// recursion
			swap(a, i, n-1);
		}
	}
	
    // swap the integers at indices i and j
    private static void swap(Vector<Integer> a, int i, int j) {
        int c = a.get(i);
        a.set(i, a.get(j));
        a.set(j, c);
    }
	
	public void restoreHiddenEdges() {
		Iterator<GraphEdge> edgeIter = dummyEdges.iterator();
		while (edgeIter.hasNext()) {
			GraphEdge nextEdge = edgeIter.next();
			GraphNode node1 = nextEdge.getNode1();
			node1.removeEdge(nextEdge);
			GraphNode node2 = nextEdge.getNode2();
			node2.removeEdge(nextEdge);
			edges.remove(nextEdge.getID());
		}
		Iterator<GraphNode> nodeIter = dummyNodes.iterator();
		while (nodeIter.hasNext()) {
			GraphNode nextNode = nodeIter.next();
			nodes.remove(nextNode.getID());
		}
		Iterator<GraphNode[]> hiddenIter = hiddenEdges.iterator();
		while (hiddenIter.hasNext()) {
			GraphNode[] hiddenEdge = hiddenIter.next();
			GraphEdge restoredEdge = createEdge(hiddenEdge[0], hiddenEdge[1]);
			restoredEdge.setColor("#e8e8e8");
		}
	}
	
	public void highLightSwap(Vector<Integer> a) {
		if (!ask) return;
		int a0 = a.get(0);
		GraphNode first = varySet.get(a0);
		int a1 = a.get(1);
		GraphNode second = varySet.get(a1);
		// for diagnosis, recolor
		Color originalColor1 = first.getColor();
		first.setColor("#000000");
		Color originalColor2 = second.getColor();
		second.setColor("#bb0000");
		graphPanel.repaint();
		Object[] options =  {"No, skip", "Yes"};
		int	response = JOptionPane.showOptionDialog(null,
				"\nContinue watching?\n",
				"Option", JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, 
				options, options[1]);
		if (response == 0) {
			ask = false;
		}
		// set color back
		int r = originalColor1.getRed();
		int g = originalColor1.getGreen();
		int b = originalColor1.getBlue();
		first.setColor(String.format("#%02x%02x%02x", r, g, b));
		r = originalColor2.getRed();
		g = originalColor2.getGreen();
		b = originalColor2.getBlue();
		second.setColor(String.format("#%02x%02x%02x", r, g, b));
	}

	public int countCrossings(Vector<GraphEdge> edgeSet) {
		int crossings = 0;
		Iterator<GraphEdge> iter1 = edgeSet.iterator();
		while (iter1.hasNext()) {
			GraphEdge edgeOne = iter1.next();
			int id1 = edgeOne.getID();
			Iterator<GraphEdge> iter2 = edgeSet.iterator();
			while (iter2.hasNext()) {
				GraphEdge edgeTwo = iter2.next();
				int id2 = edgeTwo.getID();
				if (id2 <= id1) continue;		// check each pair just once
				if (crossing(edgeOne, edgeTwo)) {
					crossings++;
				}
			}
		}
		return crossings;
	}
	
	// Do the two lines cross?
	public boolean crossing(GraphEdge edgeOne, GraphEdge edgeTwo) {
		GraphNode oneStart = edgeOne.getNode1();
		GraphNode oneEnd = edgeOne.getNode2();
		GraphNode twoStart = edgeTwo.getNode1();
		GraphNode twoEnd = edgeTwo.getNode2();
		if (oneStart.equals(twoStart)) return false;
		if (oneStart.equals(twoEnd)) return false;
		if (oneEnd.equals(twoStart)) return false;
		if (oneEnd.equals(twoEnd)) return false;
		Line2D one = new Line2D.Double(oneStart.getXY(), oneEnd.getXY());
		Line2D two = new Line2D.Double(twoStart.getXY(), twoEnd.getXY());
		return one.intersectsLine(two);
	}
	
	public GraphNode createNode(Point xy, String detail) {
		int newID = controler.newKey(nodes.keySet());
		GraphNode node = new GraphNode(newID, xy, Color.decode("#eeeeee"), "", detail);
		if (nodes.containsKey(newID)) {
			System.out.println("Error DL103 with node id " + newID);
			errors++;
			return null;
		}
		nodes.put(newID, node);
		return node;
	}
	public GraphEdge createEdge(GraphNode node1, GraphNode node2) {
		int newID = controler.newKey(edges.keySet());
		GraphEdge edge = new GraphEdge(newID, node1, node2, Color.decode("#f0f0f0"), "");
		if (edges.containsKey(newID)) {
			System.out.println("Error DL104 with Edge id " + newID);
			errors++;
			return null;
		}
		edges.put(newID, edge);
		node1.addEdge(edge);
		node2.addEdge(edge);
		return edge;
	}
	
	public void prepareRedraw() {
		nextReady = new JDialog(controler.getMainWindow(), "Optimizing the sequence ...");
		nextReady.setLayout(new BorderLayout());
		nextReady.setLocation(800, 30);
		nextReady.setMinimumSize(new Dimension(300, 200));
		nextReady.setVisible(true);
		JLabel info = new JLabel("<html>Click <b>Redraw</b> after moving an icon "
				+ "<br />into a gap."
				+ "<br /><br />Click <b>Done</b> when finished. "
				+ "<br /><br />Note: After the Auto-Layout, the map"
				+ "<br />may appear empty. Then use" 
				+ "<br />Advanced > Zoom</html>");
		info.setBorder(new EmptyBorder(10, 45, 10, 45));
		nextReady.add(info, "North");
		JPanel southWest = new JPanel();
		southWest.setLayout(new FlowLayout());
		JButton fixButton = new JButton("Redraw");
		fixButton.addActionListener(this);
		southWest.add(fixButton);
		nextReady.add(southWest, "West");
		JButton nextButton = new JButton("Done");
		nextButton.addActionListener(this);
		nextReady.add(nextButton, "East");
		nextButton.requestFocusInWindow();
		nextReady.pack();
	}
	
	public void reorder() {
		GraphNode selected = controler.getSelectedNode();
		if (!layers.containsKey(selected)) return;
		int layer = layers.get(selected);
		varySet = layerSets.get(layer);
		int y = selected.getXY().y;

		varySet.removeElement(selected);
		for (int j = 0; j < varySet.size(); j++) {
			GraphNode node = varySet.get(j);
			if (node.equals(selected)) continue;
			int yTest = node.getXY().y;
			if (yTest >= y) {
				varySet.add(j, selected);
				distributeEvenly();
				graphPanel.repaint();
				return;
			}
		}
		controler.getMainWindow().repaint();
	}

	public void actionPerformed(ActionEvent arg0) {
		String command = arg0.getActionCommand();
		if (command == "Redraw") {
			reorder();
		} else {
			nextReady.dispose();
		}
	}
}

package de.x28hd.tool.accessories;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class StatsWindow implements ActionListener {
	Hashtable<Integer,GraphNode> nodes; 
	Hashtable<Integer,GraphEdge> edges;
	PresentationService controler;	
	JDialog frame;
	JLabel left;
	int totalDistance = 0;
	int overlapCount = 0;
	int previousTotalDistance;
	int previousOverlapCount;
	
	public StatsWindow(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges,
			PresentationService controler) {
		this.nodes = nodes;
		this.edges = edges;
		this.controler = controler;
		update();	// to initialize the previousXXX counters
	}
	
	public void update() {
		if (frame == null) {
			frame = new JDialog(controler.getMainWindow(), "Counters");
			frame.setLocation(800, 30);
			frame.setMinimumSize(new Dimension(300, 120));
			frame.setLayout(new BorderLayout());
			left = new JLabel("");
			left.setBorder(new EmptyBorder(10, 10, 10, 10));
			frame.add(left, "West");
			JPanel right = new JPanel();
			right.setLayout(new BorderLayout());
			right.setBorder(new EmptyBorder(10, 10, 10, 10));
			JButton refreshButton = new JButton("Refresh");
			refreshButton.addActionListener(this);
			right.add(refreshButton, "South");
			frame.add(right, "East");
		}
		frame.setVisible(true);
		overlapCount = 0;
		totalDistance = 0;
		
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			Point node1 = edge.getNode1().getXY();
			Point node2 = edge.getNode2().getXY();
			
			// distances
			double dist = distance(node1, node2);
			totalDistance += dist;
			
			// overlaps
			Enumeration<GraphEdge> edgeList2 = edges.elements();
			while (edgeList2.hasMoreElements()) {
				GraphEdge edge2 = edgeList2.nextElement();
				if (edge2.equals(edge)) continue;
				if (edge.getID() >= edge2.getID()) continue;
				if (crossing(edge, edge2))
//				System.out.println(edge.getNode1().getLabel() + " -> " + edge.getNode2().getLabel() + "  x  " +
//						edge2.getNode1().getLabel() + " -> " + edge2.getNode2().getLabel());
				overlapCount += crossing(edge, edge2) ? 1 : 0;
			}
		}
		
		// Color the numbers green or red
		String change = overlapCount == previousOverlapCount ? "#000000" : 
			(overlapCount < previousOverlapCount ? "#00bb00" : "#ff0000");
		String change2 = totalDistance == previousTotalDistance ? "#000000" : 
			(totalDistance < previousTotalDistance ? "#00bb00" : "#ff0000");

		// List 
		int nodesCount = nodes.size();
		String stat = "<html>"
				+ "Nodes: " + nodesCount 
				+ "<br>Edges: " + edges.size() 
				+ "<br>Edge overlaps: "
				+ "<b><font color = \"" + change + "\">" + overlapCount + "</font></b>, ";
		if (nodesCount > 0) {
				stat += "<br> Average distance: "
				+ "<b><font color = \"" + change2 + "\">" + totalDistance/nodesCount + "</font></b>";
		}
		previousOverlapCount = overlapCount;
		previousTotalDistance = totalDistance;
		
		left.setText(stat);
		frame.repaint();
	}
	
	// TODO integrate with CreateCircle
	public Double distance(Point node1, Point node2) {
		double x1 = node1.getX();
		double y1 = node1.getY();
		double x2 = node2.getX();
		double y2 = node2.getY();
		Double d = (new Point2D.Double(x1, y1)).distance(new Point2D.Double(x2, y2));
		return d;
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

	// Refresh button
	public void actionPerformed(ActionEvent e) {
		update();
	}

}

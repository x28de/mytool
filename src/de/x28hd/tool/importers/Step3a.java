package de.x28hd.tool.importers;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class Step3a {
	Hashtable<Integer, GraphNode> newNodes;
	Hashtable<Integer, GraphEdge> newEdges;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	Point dropLocation;
	boolean existingMap;
	
	public Step3a(Hashtable<Integer, GraphNode> newNodes, Hashtable<Integer, GraphEdge> newEdges, 
			Rectangle bounds, boolean existingMap, PresentationService controler) {
		   System.out.println("3a existingMap " + existingMap);
		this.newNodes = newNodes;
		this.newEdges = newEdges;
		   System.out.println("3a " + newNodes.size() + " nodes, " + newEdges.size() + " edges");
		this.bounds = bounds;
		this.existingMap = existingMap;
		this.newNodes = fetchToUpperLeft(this.newNodes);
//		step3b();
//		step3a(this.newNodes, newEdges, controler);
		controler.getControlerExtras().triggerUpdate(this);
	}
	
	public void step3a(Hashtable<Integer, GraphNode> newNodes, Hashtable<Integer, GraphEdge> newEdges, 
			PresentationService controler) {
		// formerly Step3b
		controler.getControlerExtras().triggerUpdate(this);
//		dropLocation = null;
	}
	
	public Point getDropLocation() {
		return dropLocation;
	}
	public boolean isExistingMap() {
		return existingMap;
	}
	public Hashtable<Integer, GraphNode> getNodes() {
		return newNodes;
	}
	public Hashtable<Integer, GraphEdge> getEdges() {
		return newEdges;
	}
	
	public Hashtable<Integer, GraphNode> fetchToUpperLeft(Hashtable<Integer,GraphNode> nodes) {
		Point adjust = determineCorner(nodes);
		Enumeration<GraphNode> e = nodes.elements();
		while (e.hasMoreElements()) {
			GraphNode node =e.nextElement();
			Point xy = node.getXY();
			xy.translate(- adjust.x, - adjust.y);;
		}
		return nodes;
	}
	
	public Point determineCorner(Hashtable<Integer,GraphNode> nodes) {

		int maxX = bounds.x + bounds.width;
		int minY = bounds.y;
		int minXtop = maxX;
		if (bounds.width < 726) {	//	graphPanel width, 960 window - 232 right pane
			minXtop = maxX - bounds.width/2;
		}
		Enumeration<GraphNode> nodesEnum = newNodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Point xy = node.getXY();
			int x = xy.x;
			int y = xy.y;
			if (y < minY + 100) {
				if (x < minXtop) minXtop = x;
			}
		}
		return new Point(minXtop - 40, minY - 40);
	}
}

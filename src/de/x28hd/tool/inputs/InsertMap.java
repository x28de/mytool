package de.x28hd.tool.inputs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class InsertMap {
	Hashtable<Integer, GraphNode> newNodes;
	Hashtable<Integer, GraphEdge> newEdges;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	boolean existingMap;
	
	public InsertMap(PresentationService controler, Hashtable<Integer, GraphNode> nodes,
			Hashtable<Integer, GraphEdge> edges, Rectangle bounds, boolean existingMap) {
		this.newNodes = nodes;
		this.newEdges = edges;
		this.existingMap = existingMap;
		this.bounds = bounds;
		if (existingMap) this.newNodes = fetchToUpperLeft(this.newNodes);

		controler.getControlerExtras().triggerUpdate(this);
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
	
//
//	Accessories
	
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
	
//	Determine upper left visible corner
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

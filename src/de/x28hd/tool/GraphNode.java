package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.util.Vector;
import java.util.Enumeration;

public class GraphNode  {
	protected int id;	// redundant, same as key in Hashtable
	private Point xy;
	private Color color;
	private String label;
	private String detail;
	// 	redundant but useful for quicker finding of clusters:
	private Vector<GraphEdge> associations = new Vector<GraphEdge>();

	public GraphNode(int id, Point xy, Color color, String label, String detail)  {
		this.id = id;
		this.xy = xy;
		this.color = color;
		this.label = label;
		this.detail = detail;
	}

//
//  Get Properties
	
	public int getID() {
		return id;
	}

	public Point getXY() {
		return xy;
	}

	public Color getColor() {
		return color;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getDetail() {
		return detail;
	}

//
//  Set Properties
	
	public void setID(int id) {
		this.id = id;
	}
	
	public void setXY(Point xy) {
		this.xy = xy;
	}
	
	public void setColor(String hex) {
		this.color = Color.decode(hex);
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public void setDetail(String detail) {
		this.detail = detail;
	}
	
//
//  Get associations
	
	public GraphNode relatedNode(GraphEdge edge) {
		if (this == edge.getNode1()) {
			return edge.getNode2();
		} else if (this == edge.getNode2()) {
			return edge.getNode1();
		} else {
			System.out.println("*** GraphNode.relatedNode(): " + this + " is not part of edge \"" + edge + "\"");
		return null;
		}
	}
	
	public Enumeration<GraphEdge> getEdges() {
		return associations.elements();		
	}

//
//  Echo association
	
	public void addEdge(GraphEdge edge) {
		associations.addElement(edge);
	}
	
	public void removeEdge(GraphEdge edge) {
		associations.removeElement(edge);
	}
}

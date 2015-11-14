package de.x28hd;

import java.awt.Color;

public class GraphEdge {
	protected int id;	// redundant, same as key in Hashtable
	private GraphNode node1;
	private GraphNode node2;
	private Color color;
	private String detail;
	private int n1;		// redundant, see node1
	private int n2;		// redundant, see node2

	public GraphEdge (int id, GraphNode node1, GraphNode node2, Color color, String detail) {
		this.id = id;
		this.node1 = node1;
		this.node2 = node2;
		this.color = color;
		this.detail = detail;

		n1 = node1.getID();
		n2 = node2.getID();
	}

//
//  Get Properties
	
	public int getID() {
		return id;
	}

	public GraphNode getNode1() {
		return node1;
	}

	public GraphNode getNode2() {
		return node2;
	}

	public Color getColor() {
		return color;
	}

	public String getDetail() {
		return detail;
	}

	public int getN1() {
		return n1;
	}

	public int getN2() {
		return n2;
	}
	
//
//  Set Properties
	
	public void setID(int id) {
		this.id = id;
	}
	
	public void setColor(String hex) {
		this.color = Color.decode(hex);
	}
	
	public void setDetail(String detail) {
		this.detail = detail;
		
	}
	
}

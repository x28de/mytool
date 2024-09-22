package de.x28hd.tool.layouts;

import java.awt.Color;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class CentralityColoring2 {
	Hashtable<Integer, GraphNode> nodes; 
	Hashtable<Integer, GraphEdge> edges;	
	Hashtable<Integer, Color> nodesSavedColors = new Hashtable<Integer, Color>();
	Hashtable<Integer, Color> edgesSavedColors = new Hashtable<Integer, Color>();
	
	HashSet<GraphNode> leaves = new HashSet<GraphNode>();
	Hashtable<GraphNode,Integer> nodesPos = new Hashtable<GraphNode,Integer>(); 
	
	
	public CentralityColoring2(Hashtable<Integer, GraphNode> nodes, 
			Hashtable<Integer, GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;

		// Save node colors and find leaves 
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Color originalColor = node.getColor();
			nodesSavedColors.put(node.getID(), originalColor);
			Enumeration<GraphEdge> neighbors = node.getEdges();
			int neighborCount = 0;
			while (neighbors.hasMoreElements()) {
				neighbors.nextElement();
				neighborCount++;
			}
			if (neighborCount < 2) leaves.add(node);
		}
	}

	public void changeColors() {

		// Save edge colors
		Enumeration<GraphEdge> edgesEnum = edges.elements();
		while (edgesEnum.hasMoreElements()) {
			GraphEdge edge = edgesEnum.nextElement();	
			Color originalColor = edge.getColor();
			edgesSavedColors.put(edge.getID(), originalColor);
		}

//		
//		Call the Ranker program	
		
		Brandes brandes = new Brandes(nodes, edges);
		GraphNode [] array = brandes.getArray();
		for (int i = 0; i < array.length; i++) {
			// record nodes' ranks for edge coloring
			nodesPos.put(array[i], i + 1);
		}
		
		int nonLeaves = nodes.size() - leaves.size();
		int numPerColor = nonLeaves/6;
		
		// Color Nodes by Rank
		
		for (int pos = 1; pos <= nonLeaves; pos++) {
			GraphNode node = array[pos - 1];
			
			String colorString = "#d8d8d8";
			if (pos < numPerColor * 6) colorString = "#b200b2";
			if (pos < numPerColor * 5) colorString = "#0000ff";
			if (pos < numPerColor * 4) colorString = "#00ff00";
			if (pos < numPerColor * 3) colorString = "#ffff00";
			if (pos < numPerColor * 2) colorString = "#ffaa00";
			if (pos < numPerColor) colorString = "#ff0000";
			node.setColor(colorString);
		}
			
		// Color the Edges
		
		GraphNode model = null;
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			GraphNode node1 = edge.getNode1();
			GraphNode node2 = edge.getNode2();
			if (leaves.contains(node1) || leaves.contains(node2)) {
				edge.setColor("#d8d8d8");
				continue;
			}
			int pos1 = nodesPos.get(node1);
			int pos2 = nodesPos.get(node2);
			model = node1;
			if (pos2 >= pos1) model = node2;
			Color color = model.getColor();
			int r = color.getRed();
			int g = color.getGreen();
			int b = color.getBlue();
			edge.setColor(String.format("#%02x%02x%02x", r, g, b));
		}
	}	
}

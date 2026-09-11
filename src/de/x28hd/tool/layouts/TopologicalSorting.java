package de.x28hd.tool.layouts;

import java.awt.Color;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class TopologicalSorting {
	
	List<GraphNode> l = new Stack<GraphNode>();	// Empty list that will contain the sorted elements
	HashSet<GraphNode> s = new HashSet<GraphNode>();	// Set of all nodes with no incoming edge
	GraphNode [] array;
	HashSet<GraphEdge> removed = new HashSet<GraphEdge>();
	Color color;
	boolean subGraph;
	
	public TopologicalSorting(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges,
			PresentationService controler, Color color) {
		this.color = color;
		subGraph = color != null; 
		
		// Topological Sorting after pseudo code (Kahn's algorithm) from Wikipedia
		
		// initial start nodes s
		Enumeration<GraphNode> nodesList = nodes.elements();
		while (nodesList.hasMoreElements()) {
			GraphNode node = nodesList.nextElement();
			if (subGraph && !node.getColor().equals(color)) continue;
			if (!hasIncomingEdges(node)) s.add(node);
		}
		
		while (!s.isEmpty()) {
			Iterator<GraphNode> sList = s.iterator();
			while (sList.hasNext()) {
				GraphNode n = sList.next();
				s.remove(n);
				l.add(n);

				// find outgoing neighbor m 
				Enumeration<GraphEdge> neighbors = n.getEdges();
				while (neighbors.hasMoreElements()) {
					GraphEdge e = neighbors.nextElement();
					if (subGraph && !e.getColor().equals(color)) continue;
					if (removed.contains(e)) continue;
					GraphNode m = n.relatedNode(e);
					if (subGraph && !m.getColor().equals(color)) continue;
					removed.add(e);			// don't touch the genuine edges HashTable

					// check if m has no more incoming neighbors
					if (!hasIncomingEdges(m)) {
						s.add(m);
					}
				}
				break;		// just 1 element per iterator
			}
		}
		if (!subGraph && edges.size() > removed.size()) controler.displayPopup("Cycles found");

		array = new GraphNode [l.size()];
		array = (GraphNode[]) getArray();
	}
	
	public boolean hasIncomingEdges(GraphNode node) {
		Enumeration<GraphEdge> neighbors = node.getEdges();
		while (neighbors.hasMoreElements()) {
			GraphEdge edge = neighbors.nextElement();
			if (subGraph && !edge.getColor().equals(color)) continue;
			if (removed.contains(edge)) continue;
			GraphNode n2 = edge.getNode2();
			if (n2.equals(node)) {
				return true;
			}
		}
		return false;
	}
	
	public GraphNode[] getArray() {
		return l.toArray(array);
	}
}

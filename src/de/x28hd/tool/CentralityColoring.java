package de.x28hd.tool;

import java.awt.Color;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.importance.NodeRanking;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.StringLabeller.UniqueLabelException;
//import edu.uci.ics.jung.graph.impl.*;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;

public class CentralityColoring {
	Hashtable<Integer, GraphNode> nodes; 
	Hashtable<Integer, GraphEdge> edges;	
	Hashtable<Integer, Color> nodesSavedColors = new Hashtable<Integer, Color>();
	Hashtable<Integer, Color> edgesSavedColors = new Hashtable<Integer, Color>();
	
	public CentralityColoring(Hashtable<Integer, GraphNode> nodes, 
			Hashtable<Integer, GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}

	public void changeColors() {
		Hashtable<Integer, UndirectedSparseVertex> vertices = new Hashtable<Integer, UndirectedSparseVertex>();
		Hashtable<UndirectedSparseVertex, Integer> verticeIDs = new Hashtable<UndirectedSparseVertex, Integer>();
		Hashtable<Integer, GraphEdge> neighborIDs = new Hashtable<Integer, GraphEdge>();

		UndirectedSparseGraph g = new UndirectedSparseGraph();
		boolean debug = true;
		StringLabeller labeller = null;
		
		if (debug) labeller = StringLabeller.getLabeller(g);
		
//
//		Read GraphNode's and GraphEdge's from Hashtables nodes and edges 
		
		Enumeration<GraphNode>nodesEnum = nodes.elements();
		Enumeration<GraphEdge>edgesEnum = edges.elements();
		
//
//		Write vertices into the Graph
		
		UndirectedSparseVertex v;

		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Color originalColor = node.getColor();
			nodesSavedColors.put(node.getID(), originalColor);
			node.setColor("#c0c0c0");
			int nodeID = node.getID();
			v = (UndirectedSparseVertex) g.addVertex(new UndirectedSparseVertex());
			
			// Remember IDs to lookup back and forth
			vertices.put(nodeID, v);
			verticeIDs.put(v, nodeID);
			if (debug) {
				try {
					labeller.setLabel(v, nodeID + " " + node.getLabel());
//					System.out.println(v + " " + node.getLabel());
				} catch (UniqueLabelException e1) {
				}
			}
		}
		
//		
//		Write Edges into the Graph		
		
		int edgeID = 0;
		HashSet<String> uniqEdges = new HashSet<String>();
		while (edgesEnum.hasMoreElements()) {
			edgeID++;
			GraphEdge edge = edgesEnum.nextElement();	
			Color originalColor = edge.getColor();
			edgesSavedColors.put(edge.getID(), originalColor);
			edge.setColor("#d8d8d8");
			int n1 = edge.getN1();
			int n2 = edge.getN2();
			
			//	Avoid duplicate edges with uci.jung
			String uniqID = null;
			if (n1 < n2) {
				uniqID = n1 + "-" + n2;
			} else {
				uniqID = n2 + "-" + n1;
			}
			if (uniqEdges.contains(uniqID)) {
				System.out.println("CC: Duplicate skipped");
				continue;
			} else {
				uniqEdges.add(uniqID);
			}
			
			GraphNode node1 = nodes.get(n1);
			GraphNode node2 = nodes.get(n2);
			
			node1.addEdge(edge);
			node2.addEdge(edge);
			UndirectedSparseVertex v1 = vertices.get(n1);
			UndirectedSparseVertex v2 = vertices.get(n2);
			g.addEdge(new UndirectedSparseEdge(v1, v2));
			if (debug) {
			String label1 = node1.getLabel();
			String label2 = node2.getLabel();
			System.out.println("Edge added " + edgeID + " (" + label1 + " -- " + label2 + ")");
			}
		}

//		
//		Call the Ranker program
		
		BetweennessCentrality ranker = new BetweennessCentrality(g, true, false);
		ranker.evaluate();

		List<NodeRanking> ranks = ranker.getRankings();
		int nonLeaves = 0;
		int rankpos = 0;
		
		// Remember for lookup
		int ranksSorted[] = new int[900];
		int nodesSorted[] = new int[900];
		Double scoresSorted[] = new Double[900];

		for (Iterator rIt=ranks.iterator(); rIt.hasNext();) {
			rankpos++;
			NodeRanking currentRanking = (NodeRanking) rIt.next();
			Vertex vIt = ((NodeRanking) currentRanking).vertex;

			int nodeID = verticeIDs.get(vIt);
			nodesSorted[rankpos] = nodeID;
			scoresSorted[rankpos] = currentRanking.rankScore;
			ranksSorted[nodeID] = rankpos;
			if (currentRanking.rankScore > 0) nonLeaves++;
			if (debug) {
			String label = labeller.getLabel(vIt);
			System.out.println("Rank of node " + nodeID + " (" + label + "): " + currentRanking.toString());
			}
		}
		
		int numPerColor = nonLeaves/6;

		for (int pos = 1; pos <= nonLeaves; pos++) {
			int nodeID = nodesSorted[pos]; 
			GraphNode node = nodes.get(nodeID);
			
			// Color Nodes by Rank
			
			String colorString = "#b200b2";
			if (pos < numPerColor * 5) colorString = "#0000ff";
			if (pos < numPerColor * 4) colorString = "#00ff00";
			if (pos < numPerColor * 3) colorString = "#ffff00";
			if (pos < numPerColor * 2) colorString = "#ffaa00";
			if (pos < numPerColor) colorString = "#ff0000";
			node.setColor(colorString);

			Enumeration<GraphEdge> neighbors = node.getEdges();
			neighborIDs.clear();
			Double maxRank = .0;
			int parentEdgeID = -1;
			int i = 0;
			
			// Find best-ranked neighbor to color the Edge
			
			while (neighbors.hasMoreElements()) {
				i++;
				GraphEdge edge = neighbors.nextElement();
				neighborIDs.put(i, edge);
				int rel = node.relatedNode(edge).getID();
				int nodePos = ranksSorted[rel];
				Double relRank = scoresSorted[nodePos];
				
				if (relRank > maxRank) {
					if (debug) System.out.println(node.getLabel() + ": " + nodes.get(rel).getLabel() +  
							" (" + relRank + ") > " + maxRank);
					maxRank = relRank;
					parentEdgeID = i;
				} else {
					if (debug) System.out.println(node.getLabel() + ": " + nodes.get(rel).getLabel() +  
							" (" + relRank + ") <= " + maxRank);
				}

			}
			GraphEdge parentEdge = neighborIDs.get(parentEdgeID);
			if (parentEdgeID > -1) parentEdge.setColor(colorString); 
		}

//		Experimental variant
//		Enumeration<Integer> edgesKeys = edges.keys();
//
//		nodesEnum = nodes.elements();
//		edgesKeys = edges.keys();
//
//		while (nodesEnum.hasMoreElements()) {
//			GraphNode node = nodesEnum.nextElement();
//			if (node.getColor().equals(Color.decode("#c0c0c0"))) {
//				System.out.println(node.getID() + " " + node.getLabel());
//				nodes.remove(node.getID());
//			}
//		}
//		
//		while (edgesKeys.hasMoreElements()) {
//			int edgeKey = edgesKeys.nextElement();
//			GraphEdge edge = edges.get(edgeKey);
//			if (edge.getColor().equals(Color.decode("#d8d8d8"))) {
//				System.out.println(edge.getN1() + " " + edge.getN2());
//				edges.remove(edgeKey);
//			}
//		}
	}
	
	public void revertColors() {
		Enumeration<GraphNode>nodesEnum = nodes.elements();
		Enumeration<GraphEdge>edgesEnum = edges.elements();
		
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Color originalColor = nodesSavedColors.get(node.getID());
			int r = originalColor.getRed();
			int g = originalColor.getGreen();
			int b = originalColor.getBlue();
			node.setColor(String.format("#%02x%02x%02x", r, g, b));
		}
		while (edgesEnum.hasMoreElements()) {
			GraphEdge edge = edgesEnum.nextElement();
			Color originalColor = edgesSavedColors.get(edge.getID());
			int r = originalColor.getRed();
			int g = originalColor.getGreen();
			int b = originalColor.getBlue();
			edge.setColor(String.format("#%02x%02x%02x", r, g, b));
		}
	}

}

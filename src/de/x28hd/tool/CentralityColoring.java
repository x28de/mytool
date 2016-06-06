package de.x28hd.tool;

import java.awt.Color;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.importance.Ranking;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

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
		UndirectedSparseGraph<Integer, Integer> g = new UndirectedSparseGraph<Integer,Integer>();
		Hashtable<Integer, GraphEdge> neighborIDs = new Hashtable<Integer, GraphEdge>();

//
//		Read GraphNode's and GraphEdge's from Hashtables nodes and edges 
		
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		Enumeration<GraphEdge> edgesEnum = edges.elements();
		
//
//		Write vertices into the Graph

		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Color originalColor = node.getColor();
			nodesSavedColors.put(node.getID(), originalColor);
			node.setColor("#c0c0c0");
			int nodeID = node.getID();
			g.addVertex(nodeID);
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
			EdgeType edgeType = EdgeType.UNDIRECTED; 
			g.addEdge(edgeID, n1, n2, edgeType);
		}

//		
//		Call the Ranker program	
		
		BetweennessCentrality<Integer,Integer> ranker = 
				new BetweennessCentrality<Integer,Integer>(g, true, false);
		ranker.setRemoveRankScoresOnFinalize(false);
		ranker.evaluate();

		int nonLeaves = 0;
		int rankpos = 0;
		
		// Remember for lookup
		int ranksSorted[] = new int[900];
		int nodesSorted[] = new int[900];
		Double scoresSorted[] = new Double[900];

//		List<Ranking<Integer>> list = ranker.getRankings();
		List<Ranking<?>> list = ranker.getRankings();
//		for (Iterator rIt=ranks.iterator(); rIt.hasNext();) {
		for (int rIt = 0; rIt < list.size(); rIt++) {	//	ranks Iterator
			rankpos++;
//			NodeRanking currentRanking = (NodeRanking) rIt.next();
			Ranking<?> currentRanking = list.get(rIt);
//			Vertex vIt = ((NodeRanking) currentRanking).vertex;
			@SuppressWarnings("unchecked")
			int vIt = ((Ranking<Integer>) currentRanking).getRanked();	//	vertex of iterator
			System.out.println(rIt + " -> " + vIt);

//			int nodeID = verticeIDs.get(vIt);
			int nodeID = vIt;
			nodesSorted[rankpos] = nodeID;
			scoresSorted[rankpos] = currentRanking.rankScore;
			ranksSorted[nodeID] = rankpos;
			if (currentRanking.rankScore > 0) nonLeaves++;
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
					maxRank = relRank;
					parentEdgeID = i;
				}

			}
			GraphEdge parentEdge = neighborIDs.get(parentEdgeID);
			if (parentEdgeID > -1) parentEdge.setColor(colorString); 
		}

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

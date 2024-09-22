package de.x28hd.tool.layouts;

import java.util.ArrayDeque;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;

import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class Brandes {
	
	Hashtable<GraphNode,Double> cb = new Hashtable<GraphNode,Double>();
	Hashtable<GraphNode,Double> delta = new Hashtable<GraphNode,Double>();
	Hashtable<GraphNode,Integer> sigma;
	Hashtable<GraphNode,List<GraphNode>> prev = 
			new Hashtable<GraphNode,List<GraphNode>>();
	Hashtable<GraphNode,Integer> dist;
	ArrayDeque<GraphNode> q;
	Stack<GraphNode> visited;
	
	Hashtable<Integer, GraphNode> reverseRanked = new Hashtable<Integer, GraphNode>();
	GraphNode [] array;
	
	public Brandes(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges) {
		
		// Betweenness Centrality after pseudo code from Wikipedia
		
		// Initialize
		Enumeration<GraphNode> initList = nodes.elements();
		while (initList.hasMoreElements()) {
			GraphNode u = initList.nextElement();
			cb.put(u, 0.);		// centrality by betweenness
		}

		// Loop over s
		Enumeration<GraphNode> sList = nodes.elements();
		while (sList.hasMoreElements()) {
			GraphNode s = sList.nextElement();

			// Loop over v
			Enumeration<GraphNode> vList = nodes.elements();
			sigma = new Hashtable<GraphNode,Integer>();	// Number of shortest paths
			dist = new Hashtable<GraphNode,Integer>();	// distance, depth

			while (vList.hasMoreElements()) {
				GraphNode v = vList.nextElement();
				delta.put(v, 0.);		// "Single dependency"
				List<GraphNode> vPrev = new Stack<GraphNode>();	// Immediate predecessors of v
				prev.put(v, vPrev);			// Immediate predecessors during BFS
				sigma.put(v, 0);
			}
			sigma.put(s, 1);
			dist.put(s, 0);
			q = new ArrayDeque<GraphNode>();	// Breadth-first search
			q.add(s);
			visited = new Stack<GraphNode>();
			
			// Stage 1 Single-source shortest path
			
			while (!q.isEmpty()) {
				GraphNode u = q.remove();
				visited.push(u);	// order in which vertices are visited ("S" in the pseudo code)
				
				Enumeration<GraphEdge> neighbors = u.getEdges();
				while (neighbors.hasMoreElements()) {
					GraphEdge edge = neighbors.nextElement();
					GraphNode v = u.relatedNode(edge);
					List<GraphNode> vPrev = prev.get(v);
					
					int distU = dist.get(u);
					if (dist.get(v) == null) {
						dist.put(v, distU + 1);
						q.addLast(v);
					}
					int distV = dist.get(v);
					if (distV == distU + 1) {
						int sigmaV = sigma.get(v);
						int sigmaU = sigma.get(u);
						sigma.put(v, sigmaU + sigmaV);
						vPrev = prev.get(v);
						vPrev.add(u);
						prev.put(v, vPrev);
					}
				}
			}
			
			// Stage 2 Backpropagation of dependencies

			while (!visited.isEmpty()) {
				GraphNode v = visited.pop();
				List<GraphNode> vPrev = prev.get(v);
				Iterator<GraphNode> iter = vPrev.iterator();
				while (iter.hasNext()) {
					GraphNode u = iter.next();
					Double deltaU = delta.get(u);
					Double deltaV = delta.get(v);
					int sigmaU = sigma.get(u);
					int sigmaV = sigma.get(v);
					Double adding = sigmaU / sigmaV * (1 + deltaV);
					deltaU += adding;
					delta.put(u, deltaU);
					if (!u.equals(s)) {
						Double newCB = cb.get(v) + delta.get(v);
						cb.put(v, newCB);
					}
				}
			}
		}
		
		// Sort 
		
		int pos = 0;
		TreeMap<Double,GraphNode> cbMap = new TreeMap<Double,GraphNode>();
		Double disambig = 0.001;
		
		Enumeration<GraphNode> sortList = nodes.elements();
		while (sortList.hasMoreElements()) {
			GraphNode node = sortList.nextElement();
			Double centrality = cb.get(node);
			while (cbMap.containsKey(centrality)) centrality += disambig;
			cbMap.put(centrality, node);
		}
		SortedMap<Double,GraphNode> cbList = (SortedMap<Double,GraphNode>) cbMap;
		SortedSet<Double> cbSet = (SortedSet<Double>) cbList.keySet();
		int mapSize = cbMap.size();

		// Create ranked array like the JUNG ranker
		Iterator<Double> iter = cbSet.iterator();
		while (iter.hasNext()) {
			pos++;
			Double centrality = iter.next();
			GraphNode node = cbMap.get(centrality);
			reverseRanked.put(mapSize + 1 - pos, node);		// how avoid this step?
		}
		
		array = new GraphNode [mapSize];
		for (int i = 0; i < mapSize; i++) {
			GraphNode node = reverseRanked.get(i + 1);
			array[i] = node;
		}
	}
	public GraphNode[] getArray() {
		return array;
	}
}

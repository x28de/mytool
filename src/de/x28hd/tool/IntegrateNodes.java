package de.x28hd.tool;
import java.awt.Point;
import java.util.Enumeration;
import java.util.Hashtable;


public class IntegrateNodes {
	
	Hashtable<Integer, GraphNode> nodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> edges = new Hashtable<Integer, GraphEdge>();
	Hashtable<Integer, GraphNode> newNodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> newEdges = new Hashtable<Integer, GraphEdge>();
	int maxNodeID = 0;
	int maxEdgeID = 0;
	
	public IntegrateNodes(Hashtable<Integer, GraphNode> nodes, Hashtable<Integer, GraphEdge> edges,
			Hashtable<Integer, GraphNode> newNodes, Hashtable<Integer, GraphEdge> newEdges) {
		this.nodes = nodes;
		this.edges = edges;
		this.newNodes = newNodes;
		this.newEdges = newEdges;
		
		maxNodeID = 0;
		maxEdgeID = 0;
		Enumeration<GraphNode> nodesEnum;
		Enumeration<GraphEdge> edgesEnum;
		
		if (nodes.size() > 0) {
			nodesEnum = nodes.elements();
			edgesEnum = edges.elements();
		} else return;
    	
//
//		Relocating each node in turn
    	
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();	
			
			int id = node.getID();
			if (maxNodeID < id) maxNodeID = id;
		}
		while (edgesEnum.hasMoreElements()) {
			GraphEdge edge = edgesEnum.nextElement();	
			
			int id = edge.getID();
			if (maxEdgeID < id) maxEdgeID = id;
		}
		return;
	}

//
//	Add new nodes to the graph model	
	
	public void mergeNodes(Point upperGap, Point translation) {
		Enumeration<GraphNode>nodesEnum = newNodes.elements();
		Enumeration<GraphEdge>edgesEnum = newEdges.elements();
		int newNodeID = maxNodeID;
		int newEdgeID = maxEdgeID;
		Hashtable<Integer, Integer> newNodeIDs = new Hashtable<Integer, Integer>();
		
		while (nodesEnum.hasMoreElements()) {
			newNodeID++;
			GraphNode node = nodesEnum.nextElement();
			int oldTmpID = node.getID(); 	//  Old attributes of NEW nodes !
			newNodeIDs.put(oldTmpID, newNodeID);
			Point oldXY = node.getXY();
			Point newXY = new Point(oldXY.x - translation.x + upperGap.x, 
									oldXY.y - translation.y + upperGap.y);
			GraphNode newNode = new GraphNode(newNodeID, newXY, node.getColor(), 
					node.getLabel(), node.getDetail());
			nodes.put(newNodeID, newNode);
		}
		
		while (edgesEnum.hasMoreElements()) {
			newEdgeID++;
			GraphEdge edge = edgesEnum.nextElement();	

			int n1 = edge.getN1();
			int n2 = edge.getN2();
			
			GraphNode node1 = nodes.get(newNodeIDs.get(n1));
			GraphNode node2 = nodes.get(newNodeIDs.get(n2));

			GraphEdge newEdge = new GraphEdge(newEdgeID, node1, node2, edge.getColor(), edge.getDetail());
			edges.put(newEdgeID, newEdge);
			node1.addEdge(newEdge);
			node2.addEdge(newEdge);
		}
		
	}
	
	public Hashtable<Integer, GraphNode> getNodes() {
		return nodes;
	}
	public Hashtable<Integer, GraphEdge> getEdges() {
		return edges;
	}
}

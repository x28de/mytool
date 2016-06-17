package de.x28hd.tool;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;


public class IntegrateNodes {
	
	Hashtable<Integer, GraphNode> nodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> edges = new Hashtable<Integer, GraphEdge>();
	Hashtable<Integer, GraphNode> newNodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> newEdges = new Hashtable<Integer, GraphEdge>();
	int maxNodeID = 0;
	Point insertion = null;
	Point upperLeft = new Point (50, 20);
	
	public IntegrateNodes(Hashtable<Integer, GraphNode> nodes, Hashtable<Integer, GraphEdge> edges,
			Hashtable<Integer, GraphNode> newNodes, Hashtable<Integer, GraphEdge> newEdges, Point insertion) {
		this.nodes = nodes;
		this.edges = edges;
		this.newNodes = newNodes;
		this.newEdges = newEdges;
		this.insertion = insertion;
	}

//  
//	Make room for inserted nodes
  
	public void driftNodes(Point translation, Point roomNeeded, Rectangle bounds, Point insertion, Point winDim) {
		maxNodeID = 0;
		this.insertion = insertion;
		Enumeration<GraphNode> nodesEnum;
		Enumeration<GraphEdge> edgesEnum;
		boolean inject = false;
		
		System.out.println("IntegrateNodes.driftNodes() started ");
		
		if (nodes.size() > 0) {
			nodesEnum = nodes.elements();
		} else return;
		edgesEnum = edges.elements();

//    	if (roomNeeded.x < winDim.x/4 && roomNeeded.y < winDim.y/4
//    		&& (new Rectangle(winDim.x/3, winDim.y/3, winDim.x/3, winDim.y/3).contains(insertion))) {
//    		inject = true;
//    		
//    		System.out.println("IN: insertion: " + insertion.x + ", " + insertion.y);
//    		System.out.println("IN: bounds: " + bounds.x + " - " + (bounds.x + bounds.width) + "; " +
//    			bounds.y + " - " + (bounds.y + bounds.height));
//    		upperLeft = new Point(insertion.x - roomNeeded.x/2, insertion.y - roomNeeded.y/2);
//    	}
    	
//
//		Relocating each node in turn
    	
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();	
			Point xy = node.getXY();
			int dx =  0;
			int dy =  0;
			int xProjection, yProjection;	// beam from insertion point to node projected to boundary 
			
			System.out.println("\nPoint = " + xy.x + ", " + xy.y);
			
			if (inject) {
				// We try to make room of radius 200 by drifting nodes towards the boundary
				// We consider 3 points: insertion, the node, and a projection on the bounds
				
				Point insToNode = new Point(xy.x - insertion.x, xy.y - insertion.y);
//				System.out.println("insToNode = " + insToNode.x + ", " + insToNode.y);
				
				int xInsToCorner, yInsToCorner;
				if (insToNode.x > 0) {
					xInsToCorner = bounds.x + bounds.width - insertion.x;
				} else {
					xInsToCorner = bounds.x - insertion.x;
				}
				if (insToNode.y > 0) {
					yInsToCorner = bounds.y + bounds.height - insertion.y;
				} else {
					yInsToCorner = bounds.y - insertion.y;
				}
//				System.out.println("InsToCorner = " + xInsToCorner + ", " + yInsToCorner);
				
				Double angleInsToNode = (1.0 * insToNode.y) / insToNode.x;
				Double angleInsToCorner = (1.0 * yInsToCorner) / xInsToCorner;
//				System.out.println("angleInsToNode = " + angleInsToNode + ", angleInsToCorner = " + angleInsToCorner);

				if (Math.abs(angleInsToNode) < Math.abs(angleInsToCorner)) {
					if (insToNode.x > 0) {
						xProjection = bounds.x + bounds.width;
					} else {
						xProjection = bounds.x;
					}
					yProjection = insertion.y + (int) (xInsToCorner * angleInsToNode);
				} else {
					if (insToNode.y > 0) {
						yProjection = bounds.y + bounds.height;
					} else {
						yProjection = bounds.y;
					}
					xProjection = insertion.x + (int) (1.0 * yInsToCorner / angleInsToNode);
				}
//				System.out.println("Projection = " + xProjection + ", " + yProjection);

				Double distToProj = insertion.distance(xProjection, yProjection);
				Double distToNode = insertion.distance(xy.x, xy.y);
				
				//	most important set-screw 
				Double desiredDist = 200 - (distToNode/ distToProj) * 100;
//				System.out.println("distToNode = " + distToNode + ", distToProj = " + distToProj + ", desiredDist = " + desiredDist);

				Double desiredY = desiredDist/ Math.sqrt((1 + 1/(angleInsToNode * angleInsToNode)));
				Double desiredX = desiredDist/ Math.sqrt((1 + angleInsToNode * angleInsToNode));
				desiredX = desiredX * insToNode.x/ Math.abs(insToNode.x);	// algebraic sign
				desiredY = desiredY * insToNode.y/ Math.abs(insToNode.y);
				dx = desiredX.intValue();
				dy = desiredY.intValue();
				
//				System.out.println("dx = " + dx + ", dy = " + dy);
			}
			
			xy.translate(dx, dy);
			node.setXY(xy);
			
			int id = node.getID();
			if (maxNodeID < id) maxNodeID = id;
		}
		while (edgesEnum.hasMoreElements()) {
			GraphEdge edge = edgesEnum.nextElement();
//			do nothing yet
		}

//	Temporary Visualisazion of the insertion point
//		Point p = new Point( insertion.x - translation.x, insertion.y - translation.y);
//		GraphNode insnode = new GraphNode(999, p, Color.decode("#ff0000"), "INSERTION POINT", "");
//		nodes.put(999,  insnode); 
		return;
	}

//
//	Add new nodes to the graph model	
	
	public void mergeNodes(Point upperLeft, Point roomNeeded, Point translation, Point insertion) {
		Enumeration<GraphNode>nodesEnum = newNodes.elements();
		Enumeration<GraphEdge>edgesEnum = newEdges.elements();
		int newNodeID = maxNodeID;
		int newEdgeID = edges.size();
		Hashtable<Integer, Integer> newNodeIDs = new Hashtable<Integer, Integer>();

		System.out.println("IntegrateNodes.mergeNodes() started");
		
		while (nodesEnum.hasMoreElements()) {
			newNodeID++;
			GraphNode node = nodesEnum.nextElement();
			int oldTmpID = node.getID(); 	//  Old attributes of NEW nodes !
//			System.out.println("old = " + oldTmpID + ", new = " + newNodeID);
			newNodeIDs.put(oldTmpID, newNodeID);
			Point oldXY = node.getXY();
			Point newXY = new Point(oldXY.x - translation.x + upperLeft.x, 
									oldXY.y - translation.y + upperLeft.y);
			GraphNode newNode = new GraphNode(newNodeID, newXY, node.getColor(), 
					node.getLabel(), node.getDetail());
			nodes.put(newNodeID, newNode);
		}
		
		while (edgesEnum.hasMoreElements()) {
			newEdgeID++;
			GraphEdge edge = edgesEnum.nextElement();	

			int n1 = edge.getN1();
//			System.out.println("n1 = " + n1 + ", newNodeID 1 = " + newNodeIDs.get(n1));
			int n2 = edge.getN2();
//			System.out.println("n2 = " + n2 + ", newNodeID 2 = " + newNodeIDs.get(n2));
			
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
	
	public Point getUpperLeft() {
		return upperLeft;
	}

}

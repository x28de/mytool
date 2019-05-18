package de.x28hd.tool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.tree.DefaultMutableTreeNode;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

//
//	In a copy of the map, all subtrees and transit chains are deleted;
//	the remaining core nodes are arranged as a circle and offered for manual 
//	rearrangement; after this, the full map is used again, with trees and chains 
//	in radial layout.
//	Note: circle optimization and subtree dispersion is limited and not for final maps. 

public class MakeCircle implements Comparator<Integer>, ActionListener {
	Hashtable<Integer,GraphNode> nodes; 
	Hashtable<Integer,GraphNode> realNodes; 
	Hashtable<Integer,GraphEdge> edges;	
	Hashtable<Integer,GraphEdge> realEdges;	
	GraphPanelControler controler;
	UndirectedSparseGraph<Integer, Integer> graph = new UndirectedSparseGraph<Integer,Integer>();
	CircleLayout<Integer,Integer> layout = new CircleLayout<Integer,Integer>(graph);
	
	Hashtable<Integer,Double> lengthsCache = new Hashtable<Integer,Double>();
	Hashtable<Integer,Integer> positions = new Hashtable<Integer,Integer>();
	TreeMap<Integer,Integer> posMap = new TreeMap<Integer,Integer>();
	Hashtable<Integer,Integer> parents = new Hashtable<Integer,Integer>();
	HashSet<Integer> transit = new HashSet<Integer>();
	HashSet<GraphEdge> edgesDone = new HashSet<GraphEdge>();
	public class Chain extends LinkedList<Integer>{
		private static final long serialVersionUID = 8340239963129661032L;
	}
	HashSet<Chain> chains = new HashSet<Chain>();
	DefaultMutableTreeNode top = new DefaultMutableTreeNode(new BranchInfo(0, "ROOT"));
	String [] colors = 
		{"#b200b2", "#0000ff", "#00ff00", "#ffff00", 	// purple, blue, green, yellow
		"#ffc800", "#ff0000", "#c0c0c0", "#808080"};	// orange, red, pale, dark
	int size;
	Point center;
	JDialog nextReady;
	
	public MakeCircle(Hashtable<Integer,GraphNode> realNodes, Hashtable<Integer,GraphEdge> realEdges, 
			GraphPanelControler controler) {
		nodes = new Hashtable<Integer,GraphNode>();
		edges = new Hashtable<Integer,GraphEdge>();
		this.realNodes = realNodes;
		this.realEdges = realEdges;
		this.controler = controler;
		
//		
//		Create a copy of the map
		
		// Copy the 'realNodes' to work 'nodes'
		Enumeration<GraphNode> nodeList1 = realNodes.elements();
		while (nodeList1.hasMoreElements()) {
			GraphNode toClone = nodeList1.nextElement();
			GraphNode clone = new GraphNode(toClone.getID(), toClone.getXY(), 
					toClone.getColor(), toClone.getLabel(), toClone.getDetail());
			nodes.put(toClone.getID(), clone);
		}
		
		// Copy the 'realEdges' to work 'edges'
		Enumeration<GraphEdge> edgeList = realEdges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge toClone = edgeList.nextElement();
			int n1 = toClone.getN1();
			int n2 = toClone.getN2();
			GraphNode node1 = nodes.get(n1);
			GraphNode node2 = nodes.get(n2);
			GraphEdge cloned = new GraphEdge(toClone.getID(), node1, node2, 
					toClone.getColor(), toClone.getDetail());
			edges.put(toClone.getID(), cloned);
		}
		
		//	Now add its 'edges' to the each of the 'nodes'
		Enumeration<GraphNode> nodeList2 = realNodes.elements();
		while (nodeList2.hasMoreElements()) {
			GraphNode realNode = nodeList2.nextElement();
			
			Enumeration<GraphEdge> neighbors = realNode.getEdges();
			while (neighbors.hasMoreElements()) {
				GraphEdge toClone = neighbors.nextElement(); 
				GraphNode clonedNode = nodes.get(realNode.getID());
				GraphEdge cloneEdge = edges.get(toClone.getID());
				clonedNode.addEdge(cloneEdge);
			}
		}
		
//
//		Simplify
		
		// Prune subtrees and leaves (nodes with just 1 neighbor)
		int pruneCount = 1;
		while (pruneCount > 0) pruneCount = prune();
		
		// Eliminate transit nodes ((with exactly 2 neighbors)
		int elimCount = 1;
		while (elimCount > 0) elimCount = eliminate();
		
		// First iteration (shorten distances by swapping nodes)
		nextReady = new JDialog(controler.getMainWindow(), "Optimizing the core circle...");
		nextReady.setLocation(800, 30);
		nextReady.setMinimumSize(new Dimension(300, 170));
		nextReady.setVisible(true);
		
		initializeGraph();
		size = graph.getVertexCount();
		circleLayout();
		calculateDistances();
		
//
//		Core part; more iterations
		
		int success = 1;
		int iteration = 0;
		while (success > 0 && iteration < 50) {
			success = 0;
			Collection<Integer> sourceIDs = graph.getVertices();
			Iterator<Integer> sources = sourceIDs.iterator();
			while (sources.hasNext()) {
				int source = sources.next();
				int rightWay = 0;

				Collection<Integer> targetIDs = graph.getVertices();
				Iterator<Integer> targets = targetIDs.iterator();
				while (targets.hasNext()) {
					int target = targets.next();
					if (source == target) continue;
					if (swapVertices(source, target)) {
						success++;
						rightWay = 1;	// it's promising to also try its neighbor
					} else {
						rightWay = -rightWay;	
						if (rightWay < 0) break;
					}
				}
			}
			System.out.println("Iteration number " + iteration++ + " ...");
			nextReady.setTitle("Optimizing the core circle, step " + iteration + " ...");

		}
		
//
//		Utilize the core circle nodes' coordinates
		
		Collection<Integer> nodeIDs = graph.getVertices();
		Iterator<Integer> nodeIt = nodeIDs.iterator();
		while (nodeIt.hasNext()) {
			int id = nodeIt.next();
			
			GraphNode cloneNode = nodes.get(id);
			Point xy = cloneNode.getXY();
			GraphNode node = realNodes.get(id);
			node.setXY(xy);
			
			// Try to recognize a segmentation (doesn't really work)
			visualizeLocalMess(id);
		}
		
		// Hide the tangents (subtrees and transit chains)
		controler.replaceForLayout(nodes,  edges);
		
		// Prompt for Step 2
		JButton nextButton = new JButton("<html>Click here when done with "
				+ "<br />rearranging the core circle "
				+ "<br />to arrange the tangents."
				+ "<br /><br />Note: After the Auto-Layout, "
				+ "<br>the map may appear empty." 
				+ "<br />Then use Advanced > Zoom</html>");
		nextReady.add(nextButton);
		nextButton.addActionListener(this);
		nextReady.setTitle("Ready?");
	}

	public void step2() {

		// Reveal the tangents again
		controler.replaceForLayout(realNodes, realEdges);
		
		
		// Find center (no easier way?) for radial placing() later
		int maxx = Integer.MIN_VALUE;
		int maxy = Integer.MIN_VALUE;
		Enumeration<GraphNode> nodeList = nodes.elements();
		while (nodeList.hasMoreElements()) {
			GraphNode node = nodeList.nextElement();
			Point xy = node.getXY();
			int x = xy.x;
			int y = xy.y;
			if (x > maxx) maxx = x;
			if (y > maxy) maxy = y;
		}
		int radius = (int) layout.getRadius();
		center = new Point(maxx - radius, maxy - radius);
//		Draw the center
//		GraphNode tmp = new GraphNode(size + 1000, center, Color.RED, "Center", "");
//		nodes.put(size + 1000, tmp);

		//	Start a parent tree
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(new BranchInfo(0, "ROOT"));
		Enumeration<Integer> anchorSearch = nodes.keys();
		while (anchorSearch.hasMoreElements()) {
			int nodeID = anchorSearch.nextElement();
			if (!parents.containsKey(nodeID)) {
				GraphNode mapNode = realNodes.get(nodeID);
				String label = mapNode.getLabel();
				DefaultMutableTreeNode core = 
						new DefaultMutableTreeNode(new BranchInfo(nodeID, label));
				top.add(core);
				growTrees(nodeID, core, "");			// recursion
			}
		}
		
		showTree(top, "  ");	// Just on the console
		
		//	Reassemble transit chains
		Enumeration<Integer> allNodes = realNodes.keys();
		while (allNodes.hasMoreElements()) {
			int nodeID = allNodes.nextElement();
			if (parents.containsKey(nodeID)) continue;
			growChain(nodeID, null);
		}
		
		// Place the chains
		Iterator<Chain> chainList = chains.iterator();
		while (chainList.hasNext()) {
			Chain chain = chainList.next();
			int head = chain.getFirst();
			int tail = chain.getLast();
			if (tail == head) {
				tail = chain.get(chain.size() - 2);
			}
			GraphNode headNode = realNodes.get(head);
			GraphNode tailNode = realNodes.get(tail);
			Point headXY = headNode.getXY();
			Point tailXY = tailNode.getXY();
			int dx = headXY.x - tailXY.x;
			int dy = headXY.y - tailXY.y;
			double count = chain.size();
			Iterator<Integer> listIterator = chain.listIterator(0);
			int item = -1;
			while (listIterator.hasNext()) {
				item++;
				int nextID = listIterator.next();
				GraphNode node = realNodes.get(nextID);
				double doubleX = headXY.x - (item * dx)/(count - 1.0);
				double doubleY = headXY.y - (item * dy)/(count - 1.0);
				int x = (int) doubleX;
				int y = (int) doubleY;
				if (item == 0 || item == chain.size() - 1) continue;
				int fugal = 0;
				if (distance(head, tail) < radius/3.) fugal = 1;
				placing(nextID, fugal, 0, new Point(x, y));
			}
		}
		
		// Disentangle the simple trees (not those anchored in a chain link)
		Enumeration<Integer> anchorList = nodes.keys();
		while (anchorList.hasMoreElements()) {
			int nodeID = anchorList.nextElement();
			GraphNode node = realNodes.get(nodeID);
			new SubtreeLayout(node, realNodes, realEdges, controler, null, true);
		}
		
		controler.getMainWindow().repaint();
	}

//
//	Simplification methods
	
	public int prune() {
		HashSet<GraphNode> leaves = new HashSet<GraphNode>(); 
		int pruneCount = 0;
		Enumeration<GraphNode> nodelist = nodes.elements();
		while (nodelist.hasMoreElements()) {
			GraphNode node = nodelist.nextElement();
			Enumeration<GraphEdge> neighbors = node.getEdges();
			int count = 0;
			while (neighbors.hasMoreElements()) {
				neighbors.nextElement();
				count++;
			}
			if (count <= 1) {
				leaves.add(node);
			}
		}
		Iterator<GraphNode> leavesList = leaves.iterator();
		while (leavesList.hasNext()) {
			GraphNode leaf = leavesList.next();
			int leafID = leaf.getID();
			Enumeration<GraphEdge> neighbors = leaf.getEdges();
			while (neighbors.hasMoreElements()) {
				GraphEdge stipe = neighbors.nextElement();
				GraphNode parent = leaf.relatedNode(stipe);
				parent.removeEdge(stipe);
				edges.remove(stipe.getID());
				// helped to gracefully dissolve a tree (why exactly?)
				if (!leaves.contains(parent)) {
					parents.put(leafID, parent.getID());
				}
			}
			nodes.remove(leafID);
//			System.out.println("Pruned: " + leaf.getLabel());
			
			pruneCount++;
		}
		return pruneCount;
	}
	
	public int eliminate() {
		int elimCount = 0;
		Enumeration<GraphNode> nodelist = nodes.elements();
		while (nodelist.hasMoreElements()) {
			GraphNode node = nodelist.nextElement();
			Enumeration<GraphEdge> neighbors = node.getEdges();
			int count = 0;
			while (neighbors.hasMoreElements()) {
				GraphEdge edge = neighbors.nextElement();
				count++;
			}
			if (count == 2) {
				if (!elimNode(node)) {
					System.out.println("Error GS105 " + node.getLabel());
					return 0;
				} else elimCount++;
			} 
		}
		return elimCount;
	}
	
	public boolean elimNode(GraphNode node) {
		Enumeration<GraphEdge> neighbors = node.getEdges();
		GraphEdge edge1 = null;
		GraphEdge edge2 = null;
		if (neighbors.hasMoreElements()) {
			edge1 = neighbors.nextElement();
		} else {
			System.out.println("Error GS101 " + node.getLabel());
			return false;
		}
		if (neighbors.hasMoreElements()) {
			edge2 = neighbors.nextElement();
		} else {
			System.out.println("Error GS102 " + node.getLabel());
			return false;
		}
		int nodeID = node.getID();
		GraphNode node1 = node.relatedNode(edge1);
		GraphNode node2 = node.relatedNode(edge2);
		if (node1 == node2) return false;
		int edgeNum = newKey(edges.keySet());
		GraphEdge replacement = new GraphEdge(edgeNum, node1, node2, Color.decode("#c0c0c0"), "");
		edges.put(edgeNum, replacement);
		node1.addEdge(replacement);
		node2.addEdge(replacement);
		
		// not safe to omit these but convenient for mixed tree/ chain cases
		// more: it even leaves chain links in the core circle! TODO rewrite
//		node1.removeEdge(edge1);
//		node2.removeEdge(edge2);
		
		edges.remove(edge1.getID());
		edges.remove(edge2.getID());
		nodes.remove(nodeID);
//		System.out.println("Eliminated: " + node.getLabel());
		int id1 = node1.getID();
		int id2 = node2.getID();
		parents.put(nodeID, (id1 < id2 ? id1 : id2));
		transit.add(nodeID);
		return true;
	}
	
//
//	Layout methods
	
	public void initializeGraph() {
		Enumeration<GraphEdge> edgesEnum = edges.elements();
		int edgeID = 0;
		while (edgesEnum.hasMoreElements()) {
			edgeID++;
			GraphEdge edge = edgesEnum.nextElement();	
			int n1 = edge.getN1();
			if (!nodes.containsKey(n1)) continue;
			int n2 = edge.getN2();
			if (!nodes.containsKey(n2)) continue;
			EdgeType edgeType = EdgeType.UNDIRECTED; 
			graph.addEdge(edgeID, n1, n2, edgeType);
		}
		
		// Sort the initial positions (= copied IDs)
		
		Collection<Integer> nodeIDs = graph.getVertices();
		Iterator<Integer> nodeIt = nodeIDs.iterator();
		while (nodeIt.hasNext()) {
			int id = nodeIt.next();
			posMap.put(id, id);
		}
		SortedMap<Integer,Integer> posList = (SortedMap<Integer,Integer>) posMap;
		SortedSet<Integer> posSet = (SortedSet<Integer>) posList.keySet();
		Iterator<Integer> posIt = posSet.iterator();
		int relPos = 0;
		while (posIt.hasNext()) {
			int pos = posIt.next();
			int id = posList.get(pos);
			positions.put(id, relPos);
			relPos++;
		}
	}
	
	public void circleLayout() {
		layout = new CircleLayout<Integer,Integer>(graph);
		layout.setVertexOrder((Comparator<Integer>) this); // see compare()
		layout.initialize();
		layout.setSize(new Dimension(400 + (200 * ((int) Math.sqrt(size))), 
				300 + (150 * ((int) Math.sqrt(size)))));

		Collection<Integer> nodeIDs = graph.getVertices();
		Iterator<Integer> nodeIt = nodeIDs.iterator();
		while (nodeIt.hasNext()) {
			int id = nodeIt.next();
			double x = layout.getX(id);
			double y = layout.getY(id);
			int ix = (int) x;
			int iy = (int) y;
			GraphNode node = nodes.get(id);
			node.setXY(new Point(ix, iy));
			
			nodes.put(id, node);
		}
	}
	
	public boolean swapVertices(int first, int second) {
		Double oldsum = 0D;
		Double newsum = 0D;
		Collection<Integer> edges1 = graph.getIncidentEdges(first);
		Iterator<Integer> edgeIter1 = edges1.iterator();
		while (edgeIter1.hasNext()) {
			int id = edgeIter1.next();
			oldsum = oldsum + lengthsCache.get(id);
		}
		Collection<Integer> nodes1 = graph.getNeighbors(first);
		Iterator<Integer> nodeIter = nodes1.iterator();
		while (nodeIter.hasNext()) {
			int id = nodeIter.next();
			newsum = newsum + distance(second, id);
		}
		
		Collection<Integer> edges2 = graph.getIncidentEdges(second);
		Iterator<Integer> edgeIter2 = edges2.iterator();
		while (edgeIter2.hasNext()) {
			int id = edgeIter2.next();
			oldsum = oldsum + lengthsCache.get(id);
		}
		Collection<Integer> nodes2 = graph.getNeighbors(second);
		Iterator<Integer> nodeIter2 = nodes2.iterator();
		while (nodeIter2.hasNext()) {
			int id = nodeIter2.next();
			newsum = newsum + distance(first, id);
		}
		if (oldsum - newsum > 200) {	// tweak this ?
			int third = positions.get(first);
			positions.put(first, positions.get(second));
			positions.put(second, third);
			
			// Next step
			
			circleLayout();
			calculateDistances();
			
			return true;
			
		} else {
			return false;
		}
	}
	
	
	//
	//		Place the pruned nodes
		
	public void growTrees(int mapParent, DefaultMutableTreeNode treeParent, String indent) {
		Enumeration<Integer> parentsEnum = parents.keys();
		while (parentsEnum.hasMoreElements()) {
			int child = parentsEnum.nextElement();
			int testParent = parents.get(child);
			if (testParent != mapParent) continue;
			
			int level = treeParent.getLevel();
			GraphNode node = realNodes.get(child);
			DefaultMutableTreeNode newNode = 
					new DefaultMutableTreeNode(new BranchInfo(child, node.getLabel()));
			treeParent.add(newNode);
			int index = treeParent.getIndex(newNode);
			placing(child, level, index);
			growTrees(child, newNode, indent + "  ");	// recursion
		}
	}

	//
	//	Place the eliminated nodes
		
	public void growChain(int nodeID, Chain partial) {
		
		//	Analyze neigbors
		GraphNode node = realNodes.get(nodeID);
		Enumeration<GraphEdge> neighborhood = node.getEdges();
		int tail = -1;
		while (neighborhood.hasMoreElements()) {
			GraphEdge edge = neighborhood.nextElement();
			if (edgesDone.contains(edge)) continue;
			edgesDone.add(edge);
			GraphNode neighbor = node.relatedNode(edge);
			int neighborID = neighbor.getID();
			Chain chain = partial;
			if (chain != null) {
				//	Don't go back
				int previous = chain.getLast();
				if (neighborID == previous) continue;
			} else {
				chain = new Chain();
			}
				
			//	Is the neighbor a core node?
			if (!transit.contains(neighborID)) {
				if (!parents.containsKey(neighborID)) {
					if (!transit.contains(nodeID)) continue;
					tail = neighborID;
					chain.add(nodeID);
					chain.add(tail);
					chains.add(chain);
//					listChain(chain);
					return;
				} else continue;
			}
			
			chain.add(nodeID);
			growChain(neighborID, chain);	// recursion
		}
		return;
	}

	public int anchor(int nodeID) {
		int a;
		if (nodes.containsKey(nodeID)) {
			a = nodeID;
		} else {
			if (parents.containsKey(nodeID)) {
				int parentID = parents.get(nodeID);
					a = anchor(parentID);		// recursion
			} else {
				System.out.println("Error GS106 " + realNodes.get(nodeID).getLabel());
				a = nodeID;
			}
		}
		return a;
	}

	public void placing(int nodeID, int level, int index) {
		int anchorID = anchor(nodeID);
		GraphNode anchorNode = realNodes.get(anchorID);
		Point anchorPoint = anchorNode.getXY();
		placing(nodeID, level, index, anchorPoint);
	}

	public void placing(int nodeID, int level, int index, Point anchorPoint) {
		double dx = anchorPoint.x - center.x;
		double dy = anchorPoint.y - center.y;

		double factor = 1 + level/5.;
		int newX = center.x + (int) (dx * factor);
		int newY = center.y + (int) (dy * factor);
		newX = newX + (index * 15);
		newY = newY - (index * 15);
		Point nodePoint = new Point(newX, newY);
		GraphNode thisNode = realNodes.get(nodeID);
		thisNode.setXY(nodePoint);
		}


//
//	Accessories
	
	public int compare(Integer arg0, Integer arg1) {
		return positions.get(arg0) - positions.get(arg1);
	}

	public void calculateDistances() {
		Collection<Integer> edgeIDs = graph.getEdges();
		Iterator<Integer> edgeIt = edgeIDs.iterator();
		while (edgeIt.hasNext()) {
			int id = edgeIt.next();
			Pair<Integer> pair = graph.getEndpoints(id);
			int id1 = pair.getFirst();
			int id2 = pair.getSecond();
			Double edgeLength = distance(id1, id2);
			lengthsCache.put(id, edgeLength);
		}
	}

	public Double distance(int id1, int id2) {
		double x1 = layout.getX(id1);
		double y1 = layout.getY(id1);
		double x2 = layout.getX(id2);
		double y2 = layout.getY(id2);
		Double d = (new Point2D.Double(x1, y1)).distance(new Point2D.Double(x2, y2));
		return d;
	}

	public void visualizeLocalMess(int nodeID) {
		// Start with the red nodes to disentangle
		Collection<Integer> edgeIDs = graph.getIncidentEdges(nodeID);
		Iterator<Integer> edgeIt = edgeIDs.iterator();
		int nearCount = 0;
		int count = 0;
		while (edgeIt.hasNext()) {
			int id = edgeIt.next();
			Pair<Integer> pair = graph.getEndpoints(id);
			if (pair == null) return;
			int id1 = pair.getFirst();
			int id2 = pair.getSecond();
			int pos1 = positions.get(id1);
			int pos2 = positions.get(id2);
			int relDist = Math.abs(pos1 - pos2);
			if (relDist >= (int) ((size +1)/2.)) {
				relDist = size - relDist;
			}
			if (relDist < size * .05) nearCount++;
			count++;
		}
		int colorWarmth = (int) (nearCount * 5.) / count;
		GraphNode node = nodes.get(nodeID);
		node.setColor(colors[colorWarmth]);
		nodes.put(nodeID, node);
	}

	public int newKey(Set<Integer> keySet) {
		int idTest = realEdges.size();
		while (keySet.contains(idTest)) idTest++;
		return idTest;
	}

	public void actionPerformed(ActionEvent arg0) {
		nextReady.dispose();
		step2();
	}
	
	
	//	Diagnostics, on the console
	public void showTree(DefaultMutableTreeNode treeNode, String indent) {
		Enumeration<DefaultMutableTreeNode> treeEnum = treeNode.children();
		while (treeEnum.hasMoreElements()) {
			DefaultMutableTreeNode child = treeEnum.nextElement();
			BranchInfo branchInfo = (BranchInfo) child.getUserObject();
//			System.out.println(indent + treeNode.getIndex(child) + " " + branchInfo);
			showTree(child, indent + "  ");
		}
	}
	public void listChain(Chain chain) {
		int i = 0;
		Iterator<Integer> chainIterator = chain.iterator();
		while (chainIterator.hasNext()) {
			i++;
			int nodeID = chainIterator.next();
			GraphNode node = realNodes.get(nodeID);
			System.out.println("  " + i + " " + node.getLabel() + " of " + chain.size());
		}
	}
}

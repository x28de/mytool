package de.x28hd.tool.layouts;

import java.awt.Color;
import java.awt.Point;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import de.x28hd.tool.BranchInfo;
import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.Utilities;

public class DAG {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	Hashtable<Integer,Integer> nodesSequence = new Hashtable<Integer,Integer>();
	Hashtable<Integer,Integer> discoveredVia = new Hashtable<Integer,Integer>(); // "parents" didn't fit
	Hashtable<Integer,Boolean> discoveryPointsBack = new Hashtable<Integer,Boolean>();
	HashSet<Integer> done = new HashSet<Integer>();
    DefaultMutableTreeNode topNode = null;
    DefaultMutableTreeNode treeNode = null;
    DefaultMutableTreeNode newNode = null;
	Hashtable<Integer,Integer> colFill = new Hashtable<Integer,Integer>();
	String dataString;
	HashSet<GraphEdge> nonTreeEdges = new HashSet<GraphEdge>();
	int anf = 0;
	
	// TODO integrate the genealogical DAG of GEDcom-Import here which used the same 
	// 'depth last layout' (?) algorithm (references welcome) 
	
	public DAG(Hashtable<Integer,GraphNode> inputNodes, 
			Hashtable<Integer,GraphEdge> inputEdges, PresentationService controler) {
		nodes = inputNodes;
		edges = inputEdges;
		DefaultMutableTreeNode topTop = new DefaultMutableTreeNode(new BranchInfo(0, "1"));

//
//		Big loop over new tree tops (hoping that nodeIDs are descending by age)
		
		int nodeCount = 0;
		int tryID = -1;
		while (nodeCount < nodes.size()) {
			tryID++;
			if (nodes.containsKey(tryID)) {
				nodeCount++;
				nodesSequence.put(nodeCount, tryID);
			}
		}
		for (int i = nodeCount; i > 0; i--) {
			int topID = nodesSequence.get(i);
			if (done.contains(topID)) continue;
			done.add(topID);
			discoveredVia.put(topID, -1);

			connectFamiliesAll(topID, "");
			
			topNode = new DefaultMutableTreeNode(new BranchInfo(topID, "1"));
			topTop.add(topNode);

			gatherRelatives(topID, topNode, "");
		}
		
		// traverse the discovery tree
		colFill.put(1,0);	// initialize odd cases
		colFill.put(-1,0);
		growGraph(topTop, "", 0, "1");
		
		Enumeration<Integer> edgeList = edges.keys();
		while (edgeList.hasMoreElements()) {
			int edgeID = edgeList.nextElement();
			GraphEdge edge = edges.get(edgeID);
			GraphEdge forwardEdge = edges.get(edgeID);
			Color color = forwardEdge.getColor();
			int r = color.getRed();
			int g = color.getGreen();
			int b = color.getBlue();
			String colorString = String.format("#%02x%02x%02x", r, g, b);
			// Particularly for agreement (green) and disagreement (red) edges from 
			// https://www.denizcemonduygu.com/philo/browse/ but shouldn't harm others
			if (nonTreeEdges.contains(edge)) {
				if (colorString.equals("#bbffbb")) forwardEdge.setColor("#e8ffe8");
				if (colorString.equals("#ffbbbb")) forwardEdge.setColor("#ffe8e8");
				if (colorString.equals("#c0c0c0")) forwardEdge.setColor("#f0f0f0");
				if (colorString.equals("#00ff00")) forwardEdge.setColor("#bbffbb");
				if (colorString.equals("#ff0000")) forwardEdge.setColor("#ffbbbbb");
			} else {
				if (colorString.equals("#bbffbb")) forwardEdge.setColor("#00ff00");
				if (colorString.equals("#ffbbbb")) forwardEdge.setColor("#ff0000");
			}
		}
		Utilities.displayLayoutWarning(controler, false);
	}
	
	public void connectFamiliesAll(int nodeID, String indent) {
		GraphNode node = nodes.get(nodeID);
		Enumeration<GraphEdge> neighbors = node.getEdges();
		while (neighbors.hasMoreElements()) {
			GraphEdge edge = neighbors.nextElement();
			GraphNode otherEnd = node.relatedNode(edge);

			// Peculiar naming is due to GedcomImport
			// If the arrow points to the neighbor, the discovery direction is "back"
			int otherEndNum = otherEnd.getID();
			boolean back = false;
			int n2 = edge.getN2();
			if (otherEndNum == n2){
				back = true;
			}
			int previousTrail = discoveredVia.get(nodeID);
		
//
// Analyze "otherEnd" to detect cross links
					
			boolean xref = false;

			if (otherEndNum == previousTrail) continue;  // trivial case
			if (done.contains(otherEndNum)) xref = true;
			else if (discoveredVia.containsKey(otherEndNum)) xref = true;
			
			if (xref) {
				nonTreeEdges.add(edge);
				continue;
			}
			
			// Normal tree links
			discoveredVia.put(otherEndNum, nodeID);
			discoveryPointsBack.put(otherEndNum, back);
			done.add(otherEndNum);
			
			connectFamiliesAll(otherEndNum, indent + "  ");	// recurse
		}
	}
	
	public void gatherRelatives(int parent, DefaultMutableTreeNode treeNode, String indent) {
		Enumeration<Integer> parentsEnum = discoveredVia.keys();
		while (parentsEnum.hasMoreElements()) {
			int testChild = parentsEnum.nextElement();
			int testParent = discoveredVia.get(testChild);
			if (testParent != parent) {
				continue;
			} else {
				String hori = "1";
				if (discoveryPointsBack.get(testChild)) hori = "-1";
				newNode = new DefaultMutableTreeNode(new BranchInfo(testChild, hori));
				treeNode.add(newNode);
				
				gatherRelatives(testChild, newNode, indent + "  ");	// recurse
//				BranchInfo branchInfo = (BranchInfo) newNode.getUserObject();
			}
		}
	}

	public void growGraph(DefaultMutableTreeNode nestNode, String indent, int myCol, String hori) {
		
		// Similar to GedcomImport. But
		// in BranchInfo, ID and "hori" are swapped; 
		
		int newCol  = 0;
		
		BranchInfo branchInfo = (BranchInfo) nestNode.getUserObject();
		int nodeNum = branchInfo.getKey();
		if (nodes.containsKey(nodeNum)) {
			GraphNode node = nodes.get(nodeNum);
			int depth = nestNode.getDepth();

			//	Add current node to its column
			Point drawLoc = addToFill(myCol, Integer.parseInt(hori), depth);
			node.setXY(drawLoc);
		}
		
		// Recursively open the nested subtrees
		
		Enumeration<TreeNode> treeChildren = nestNode.children();
		
		//	Prepare for reordering the current level of children
		Hashtable<Integer,DefaultMutableTreeNode> childrenMap = 
				new Hashtable<Integer,DefaultMutableTreeNode>();
		TreeMap<Double,Integer> orderMap = new TreeMap<Double,Integer>();
		SortedMap<Double,Integer> orderList = (SortedMap<Double,Integer>) orderMap;
		double disambig = 0.001;

		//	Unordered list
		while (treeChildren.hasMoreElements()) {
			TreeNode child = treeChildren.nextElement();
			BranchInfo branchInfo2 = (BranchInfo) ((DefaultMutableTreeNode) child).getUserObject();
			int itemID = branchInfo2.getKey();
			childrenMap.put(itemID, (DefaultMutableTreeNode) child);
			int weight = ((DefaultMutableTreeNode) child).getLeafCount();
			double dweight = weight;
			if (orderMap.containsKey(dweight)) {
				dweight = dweight + disambig;
				disambig = disambig + 0.001;
			}
			orderMap.put(dweight, itemID);
        }
		
		// Ordered list
		SortedSet<Double> orderSet = (SortedSet<Double>) orderList.keySet();
		Iterator<Double> ixit = orderSet.iterator(); 
		while (ixit.hasNext()) {
        	double rankNum = ixit.next();
        	int weight = (int) rankNum;
        	int itemID = orderMap.get(rankNum);
			DefaultMutableTreeNode child = childrenMap.get(itemID);
		
			BranchInfo nodeRef = (BranchInfo) child.getUserObject();
			hori = nodeRef.toString(); 	// the term "key" is abused here; it is +- 1
			
			//	just for diag
//			int nodeNum2 = inputID2num.get(itemID);
			GraphNode node2 = nodes.get(itemID);
			String diag = node2.getLabel();
//			if (weight > 1) node2.setLabel(diag + " " + weight);
//			System.out.println(indent + itemID + " " + diag);

			newCol = myCol + Integer.parseInt(hori);
			growGraph(child, indent + " ", newCol, hori);	// the recursion
		}
	}
	
	public Point addToFill(int col, int hori, int depth) {
		if (!colFill.containsKey(col)) colFill.put(col, 0);
		int fill = colFill.get(col);
		
		int testCol = col;
		while (colFill.containsKey(testCol)) {
			int testFill = colFill.get(testCol);
			if (testFill > fill) fill = testFill;
			if (Math.abs(testCol - col) >= depth) break;
			if (hori > 0) {
				testCol++;
			} else {
				testCol--;
			}
		}
		fill++;
		int minCol = col - hori;
		if (!colFill.containsKey(minCol)) colFill.put(minCol,0);
		int min = colFill.get(minCol);
		if (fill < min) fill = min;
		colFill.put(col, fill);
		
		int x = col * 200;
		int y = fill * 50;
		return new Point(x, y);
	}

}

package de.x28hd.tool.importers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.tree.DefaultMutableTreeNode;

import org.json.JSONArray;
import org.json.JSONException;

import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.layouts.TreeMapLayout;

public class TreeMapCounting {
	// temporary intermediate class with copies and detour via JSONArray
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	TreeMapLayout treeMapLayout;
	
	public TreeMapCounting(DefaultMutableTreeNode jsonTop) {
		treeMapLayout = new TreeMapLayout();
		JSONArray dirList = jsonStruc(jsonTop, "");
		DefaultMutableTreeNode countersTop = collectJSON(dirList, "");
		treeMapLayout.setTop(countersTop);
	}
	
	public TreeMapCounting(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
		GraphNode rootNode = null;
		Enumeration<GraphNode> nodeList = nodes.elements();
		while (nodeList.hasMoreElements()) {
			GraphNode node = nodeList.nextElement();
			if (!node.getLabel().equals("ROOT")) {
				continue;
			} else {
				rootNode = node;
				break;
			}
		}
		treeMapLayout = new TreeMapLayout();
		DefaultMutableTreeNode jsonTop = collectNeighbors(rootNode, "");
		JSONArray dirList = jsonStruc(jsonTop, "");
		DefaultMutableTreeNode countersTop = collectJSON(dirList, "");
		treeMapLayout.setTop(countersTop);
	}

	public DefaultMutableTreeNode collectNeighbors(GraphNode node, String indent) {
		DefaultMutableTreeNode constructed = new DefaultMutableTreeNode(node.getLabel());
		
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			GraphNode n1 = edge.getNode1();
			if (n1 != node) continue;
			GraphNode n2 = edge.getNode2();
			DefaultMutableTreeNode child = collectNeighbors(n2, indent + "  ");
			constructed.add(child);
		}
			
		return constructed;
	}
	
	public JSONArray jsonStruc(DefaultMutableTreeNode node, String indent) {
		
		JSONArray tree = new JSONArray();
		int index = 0;
		String nodeName = "";
		try {
			nodeName = (String) node.getUserObject().toString(); 
			tree.put(index, nodeName);
			Enumeration children = node.children();
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
				index++;
				if (!childNode.isLeaf()) {
					JSONArray child = jsonStruc(childNode, indent + "  ");		// recursion
					tree.put(index,child);
				} else {
					tree.put(index, (String) childNode.getUserObject().toString());
				}
			}
		} catch (JSONException e) {
			System.out.println("Error TMC01 " + e.getMessage() + " with " + nodeName);
		}
		return tree;
	}
	
	public DefaultMutableTreeNode collectJSON(JSONArray dirList, String indent) {
		// Generalized from folder tree, and still using the same variable names for compatibility
		String itemName = "";
		try {
			itemName = dirList.getString(0);
		} catch (JSONException e) {
			System.out.println("Error TMC02 " + e.getMessage() + " with " + dirList.toString());
		}
		DefaultMutableTreeNode constructed = null;

		// Buffers for sorting
		TreeMap<Double,DefaultMutableTreeNode> bufferMap = new TreeMap<Double,DefaultMutableTreeNode>(Collections.reverseOrder());
		SortedMap<Double,DefaultMutableTreeNode> bufferList = (SortedMap<Double,DefaultMutableTreeNode>) bufferMap;

		int leafCount = 0;
		int folderCount = 0;
		double disambig = .00001;
		int sum = 0;

		// Scan the folder
		int len = dirList.length();
		for (int index = 0; index < len; index++) {		// iterator() from Javadoc did not work
			Object f = null;
			try {
				f = dirList.get(index);
			} catch (JSONException e) {
				System.out.println("Error TMC03 " + e.getMessage() + " with child no " + index);
			}
			if (f.getClass() == JSONArray.class) {
				folderCount++;
				DefaultMutableTreeNode child = collectJSON((JSONArray) f, indent + "  ");
				TreeMapLayout.MyBranchInfo info = (TreeMapLayout.MyBranchInfo) child.getUserObject();
				int total = info.getTotal();
				sum += total;
				
				double uniq = total + 0.;
				while (bufferMap.containsKey(uniq)) uniq += disambig;
				bufferMap.put(uniq, child);
			} else {
				leafCount++;
			}
		}

		// pseudo folder of leaf files
		if (leafCount > 0) {
			if (folderCount > 0) {
				double uniq = leafCount + .0;
				while (bufferMap.containsKey(uniq)) uniq += disambig;
				TreeMapLayout.MyBranchInfo myBranchInfo = treeMapLayout.new MyBranchInfo(leafCount, itemName + " items");
				DefaultMutableTreeNode pseudoFolder = new DefaultMutableTreeNode(myBranchInfo);
				bufferMap.put(uniq, pseudoFolder);
			} else {
				TreeMapLayout.MyBranchInfo myBranchInfo = treeMapLayout.new MyBranchInfo(leafCount, itemName);
				DefaultMutableTreeNode pseudoFolder = new DefaultMutableTreeNode(myBranchInfo);
				return pseudoFolder;
			}
		}
		
		// sort by totals
		SortedSet<Double> bufferSet = (SortedSet<Double>) bufferList.keySet();
		Iterator<Double> iter = bufferSet.iterator();

		// add child nodes
		TreeMapLayout.MyBranchInfo constructedInfo = treeMapLayout.new MyBranchInfo(sum + leafCount, itemName);
		constructed = new DefaultMutableTreeNode(constructedInfo);
		while (iter.hasNext()) {
			double sortedNode = iter.next();
			DefaultMutableTreeNode treeNode = bufferMap.get(sortedNode);
			constructed.add(treeNode);
		}
		return constructed;
	}
}

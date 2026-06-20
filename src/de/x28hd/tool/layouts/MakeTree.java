package de.x28hd.tool.layouts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.BranchInfo;
import de.x28hd.tool.accessories.Utilities;
import de.x28hd.tool.PresentationExtras;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.importers.TreeMapCounting;
import edu.uci.ics.jung.algorithms.layout.PolarPoint;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class MakeTree implements TreeSelectionListener, ActionListener {
	Hashtable<Integer, GraphNode> nodes; 
	Hashtable<Integer, GraphEdge> edges;	
	Hashtable<Integer, Color> nodesSavedColors = new Hashtable<Integer, Color>();
	Hashtable<Integer, Color> edgesSavedColors = new Hashtable<Integer, Color>();
	
	boolean layout = false;
	String[] colors = {
			"#ff0000",
			"#ffaa00",
			"#ffff00",
			"#00ff00",
			"#0000ff",
			"#b200b2"};
	JTree tree;
	int level = -1;
	JFrame frame;
	PresentationService controler;
	JCheckBox treeMapBox = new JCheckBox("Show treemap?");
	DefaultMutableTreeNode topTop;
	
	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			Utilities.displayLayoutWarning(controler, true);
		}
	};
	Hashtable<Integer,Integer> parents = new Hashtable<Integer,Integer>();
	TreeMap<Integer,Integer> rankedNodes = new TreeMap<Integer,Integer>();
	int ranksSorted[] = new int[5000];	// TODO get rid of array
	DirectedSparseGraph<Integer, Integer> g2 = new DirectedSparseGraph<Integer,Integer>();

	Hashtable<Integer, GraphEdge> neighborIDs = new Hashtable<Integer, GraphEdge>();
	Hashtable<GraphNode,Integer> nodesPos = new Hashtable<GraphNode,Integer>(); 
	HashSet<GraphNode> leaves = new HashSet<GraphNode>();
	
	
	public MakeTree(Hashtable<Integer, GraphNode> nodes, 
			Hashtable<Integer, GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;

		// Save node colors and find leaves 
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Color originalColor = node.getColor();
			nodesSavedColors.put(node.getID(), originalColor);
			if (!layout) node.setColor("#c0c0c0");
			Enumeration<GraphEdge> neighbors = node.getEdges();
			int neighborCount = 0;
			while (neighbors.hasMoreElements()) {
				neighbors.nextElement();
				neighborCount++;
			}
			if (neighborCount < 2) leaves.add(node);
		}
	}

	public void changeColors(boolean andLayout, PresentationService controler) {
		layout = andLayout;
		this.controler = controler;
		changeColors();
	}

	public void changeColors() {
		HashSet<GraphEdge> nonTreeEdges = new HashSet<GraphEdge>();

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
		
		int nonLeaves = array.length - leaves.size();
		int rankpos = 0;
		
		// Reconstruct old ID structures -- cleanup later

		int nodesSorted[] = new int[5000];
		Double scoresSorted[] = new Double[5000];
		
		for (int i = 0; i < array.length; i++) {
			rankpos++;
			GraphNode v = array[i];		// was ranking.getRanked();
			int nodeID = v.getID();
			nodesSorted[rankpos] = nodeID;
			double rankScore = nonLeaves - i + .0;
			if (i >= nonLeaves) rankScore = (i - nonLeaves - .0) / (array.length - nonLeaves - .0); 
			scoresSorted[rankpos] = rankScore;
			ranksSorted[nodeID] = rankpos;
			rankedNodes.put(rankpos, nodeID);		//	TODO replace old arrays
		}
		
		int numPerColor = nonLeaves/6;
		int treeEdgeID = 0;
		
		HashSet<String> uniqEdges = new HashSet<String>();
		uniqEdges.clear();
		HashSet<Integer> done = new HashSet<Integer>();
		
		// Color Nodes by Rank
		
		for (int pos = 1; pos <= nonLeaves; pos++) {
			GraphNode node = array[pos - 1];
			
			String colorString = "#d8d8d8";
			if (!layout) {
			if (pos < nonLeaves) colorString = "#b200b2";
			if (pos < numPerColor * 5) colorString = "#0000ff";
			if (pos < numPerColor * 4) colorString = "#00ff00";
			if (pos < numPerColor * 3) colorString = "#ffff00";
			if (pos < numPerColor * 2) colorString = "#ffaa00";
			if (pos < numPerColor) colorString = "#ff0000";
			node.setColor(colorString);
			}
			
			// Color the Edges
			Enumeration<GraphEdge> neighbors = node.getEdges();
			neighborIDs.clear();
			Double maxRank = .0;
			int parentEdgeID = -1;
			String parentUniq = "";
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
			if (parentEdgeID > -1) parentEdge.setColor(colorString); // TODO exotic case
		}

		if (!layout) return;
		
		TreeMap<Integer,Integer> eligible = new TreeMap<Integer,Integer>();
		SortedMap<Integer,Integer> todoMap = (SortedMap<Integer,Integer>) eligible; 
		SortedSet<Integer> todoSet = (SortedSet<Integer>) todoMap.keySet();

		boolean annex = false;

		int pos = 0;
		TreeMap<Integer,Integer> treeTops = new TreeMap<Integer,Integer>();
		int treeCount = 0;
		while (pos < rankedNodes.size()) {
			pos++;

			// Next tree

			int centerCandidate = rankedNodes.get(pos);
			if (done.contains(centerCandidate)) continue;
			treeCount++;
			treeTops.put(treeCount, pos);
			
			eligible = new TreeMap<Integer,Integer>();		
			todoMap = (SortedMap<Integer,Integer>) eligible;
			todoSet = (SortedSet<Integer>) todoMap.keySet();
			eligible.put(pos, rankedNodes.get(pos));	// TODO start with 0

			//	Examine all eligible nose, i.e., those connected but not yet done
			
			while (eligible.size() > 0) {
				int currentParentRank = todoSet.first();
				int nodeID = rankedNodes.get(currentParentRank);

				GraphNode node = nodes.get(nodeID);
				Enumeration<GraphEdge> neighbors = node.getEdges();
				neighborIDs.clear();
				String parentUniq = "";
				int i = 0;

				// Find all neighbor to prepare the tree graph g2

				annex = false;
				while (neighbors.hasMoreElements()) {
					i++;
					GraphEdge edge = neighbors.nextElement();
					neighborIDs.put(i, edge);
					int rel = node.relatedNode(edge).getID();
					if (done.contains(rel)) continue;

					//	Avoid duplicate edges with uci.jung
					String uniqID = null;
					if (nodeID < rel) {
						uniqID = nodeID + "-" + rel;
					} else {
						uniqID = rel + "-" + nodeID;
					}
					if (uniqEdges.contains(uniqID)) {
						continue;
					}

					parentUniq = uniqID;

					int otherEnd = rel;
					eligible.put(ranksSorted[otherEnd], otherEnd);

					g2.addEdge(treeEdgeID, nodeID, otherEnd, EdgeType.DIRECTED);
					parents.put(otherEnd, nodeID);
					nonTreeEdges.remove(edge);
					done.add(otherEnd);
					uniqEdges.add(parentUniq);
					treeEdgeID++;
					annex = false;
				}
				eligible.remove(ranksSorted[nodeID]);
			}
			done.add(centerCandidate);
		}
		
//
//		Temporarily show a JTree here

		SortedMap<Integer,Integer> treeTopsSorted = (SortedMap<Integer,Integer>) treeTops;
		SortedSet<Integer> treeTopsSet = (SortedSet<Integer>) treeTopsSorted.keySet();
		Iterator<Integer> ttix = treeTopsSet.iterator();
	    topTop = new DefaultMutableTreeNode(new BranchInfo(-1, " "));
	    
		while (ttix.hasNext()) {
		int treeTopIndex = ttix.next();
		int treeTop = treeTops.get(treeTopIndex);
		int topID = nodesSorted[treeTop];
		
		//	Show tree
	    DefaultMutableTreeNode top = 
	    		new DefaultMutableTreeNode(new BranchInfo(topID, nodes.get(topID).getLabel()));
	    topTop.add(top);
	    createSelectionNodes(top);
		}
	    
	    DefaultTreeModel model = new DefaultTreeModel(topTop);
	    PresentationExtras controlerExtras = controler.getControlerExtras();
	    controlerExtras.setTreeModel(model);
	    controlerExtras.setNonTreeEdges(nonTreeEdges);
	    model = null;
	    model = controlerExtras.getTreeModel();
	    tree = new JTree(model);
	    
	    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	    tree.addTreeSelectionListener(this);
	    
        frame = new JFrame("Using this tree structure");
        frame.setLocation(100, 170);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);	// closing triggers further processing
		frame.addWindowListener(myWindowAdapter);
		frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(tree));

		JPanel toolbar = new JPanel();
		toolbar.setLayout(new BorderLayout());
		toolbar.setBorder(new EmptyBorder(10, 10, 10, 10));
		treeMapBox.setToolTipText("for node counts, like WinDirStat but without cushion shading and tiling algorithm)");
		toolbar.add(treeMapBox, "West");
		JButton continueButton = new JButton("Continue");
		continueButton.addActionListener(this);
		toolbar.add(continueButton, "East");
		frame.add(toolbar,"South");
        
        frame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
        frame.setMinimumSize(new Dimension(596, 418));
        frame.setVisible(true);
        
        treeLayout(g2);
	}
	public void treeLayout(HashSet<GraphEdge> nonTreeEdges) {
		treeLayout(nonTreeEdges, false);
	}	
	public void treeLayout(HashSet<GraphEdge> nonTreeEdges, boolean reverse) {
//		this.nonTreeEdges = nonTreeEdges;
		Enumeration<GraphEdge> edgesEnum = edges.elements();

//		
//		Write Edges into the Graph		
		
		int edgeID = 0;
		while (edgesEnum.hasMoreElements()) {
			edgeID++;
			GraphEdge edge = edgesEnum.nextElement();	
			if (nonTreeEdges.contains(edge)) continue;
			int n1 = edge.getN1();
			int n2 = edge.getN2();
			EdgeType edgeType = EdgeType.DIRECTED; 	//	For trees
			if (reverse) {
				g2.addEdge(edgeID, n2, n1, edgeType);
			} else {
			g2.addEdge(edgeID, n1, n2, edgeType);
			}
		}
		treeLayout(g2);
	}
	
	public void treeLayout(DirectedSparseGraph<Integer, Integer> g2) {    
		Collection<Integer> nodeIDs = g2.getVertices();
		Iterator<Integer> nodeIt = nodeIDs.iterator();
		
		DelegateForest<Integer,Integer> forest = new DelegateForest<Integer,Integer>(g2);
		if (forest.getTrees().size() <= 0) {
			System.out.println("Map seems inappropriate (no trees in forest.)");
			return;
		}
		RadialTreeLayout<Integer,Integer> layout = new RadialTreeLayout<Integer,Integer>(forest);

		layout.initialize();
		int size = g2.getVertexCount();
		layout.setSize(new Dimension(400 + (200 * ((int) Math.sqrt(size))), 
				300 + (150 * ((int) Math.sqrt(size)))));

		HashMap<Integer,PolarPoint> map = new HashMap<Integer,PolarPoint>();
		map = (HashMap<Integer,PolarPoint>) layout.getPolarLocations();
		
//
//		Write out x and y
		
		while (nodeIt.hasNext()) {
			int id = nodeIt.next();
			Point2D p = null;
			try {
				p = PolarPoint.polarToCartesian(map.get(id));
			} catch(Throwable e) {
				e.printStackTrace();
			}
			double x = p.getX();
			double y = p.getY();
			
			int ix = (int) x;
			int iy = (int) y;
			GraphNode node = nodes.get(id);
			node.setXY(new Point(ix, iy));
			nodes.put(id, node);
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

//
//	Accessories for branch selection
    
    private void createSelectionNodes(DefaultMutableTreeNode top) {
        DefaultMutableTreeNode branch = null;
        BranchInfo categoryInfo = (BranchInfo) top.getUserObject();
        int parentKey = categoryInfo.getKey();
        Integer childKey = -1;
        String branchLabel = "";
        level++;
        
        Enumeration<Integer> children = parents.keys();
        
		TreeMap<Integer,Integer> orderMap = new TreeMap<Integer,Integer>();
		SortedMap<Integer,Integer> orderList = (SortedMap<Integer,Integer>) orderMap;
        while (children.hasMoreElements()) {
        	childKey = children.nextElement();
        	orderMap.put(ranksSorted[childKey], childKey);
        }
		SortedSet<Integer> orderSet = (SortedSet<Integer>) orderList.keySet();
		Iterator<Integer> ixit = orderSet.iterator(); 
        	
		GraphNode node;
		while (ixit.hasNext()) {
        	int childRank = ixit.next();
        	childKey = rankedNodes.get(childRank);
       			
        	int testParent = parents.get(childKey);
        	if (testParent == parentKey) {
                node = nodes.get(childKey);
        		branchLabel = node.getLabel();
    			if (branchLabel.length() > 25) branchLabel = branchLabel.substring(0, 25) + " ..."; 
                branch = new DefaultMutableTreeNode(new BranchInfo(childKey, branchLabel));
                top.add(branch);
                
                createSelectionNodes(branch);	// recursive
                
                String colorString = colors[level % 6];
    			node.setColor(colorString);
    			
    			//	Find and color the edge
    			Enumeration<Integer> edgeList = edges.keys();
    			while (edgeList.hasMoreElements()) {
    				int edgeID = edgeList.nextElement();
    				GraphEdge edge = edges.get(edgeID);
    				int n1 = edge.getN1();
    				if (n1 == parentKey) {
        				int n2 = edge.getN2();
        				if (n2 == childKey) {
        					edge.setColor(colorString);
        				} else continue;
    				} else if (n1 == childKey) {
        				int n2 = edge.getN2();
        				if (n2 == parentKey) {
        					edge.setColor(colorString);
        				} else continue;
    				} else continue;
    			}
        	}
        }
		level--;
    }

	public void valueChanged(TreeSelectionEvent arg0) {
		TreePath[] paths = arg0.getPaths();

		for (int i = 0; i < paths.length; i++) {
			TreePath selectedPath = paths[i];
			Object o = selectedPath.getLastPathComponent();
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) o;
//			toggleSelection(selectedNode, arg0.isAddedPath(i));
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		frame.dispose();
		if (treeMapBox.isSelected()) new TreeMapCounting(topTop);
		Utilities.displayLayoutWarning(controler, true);
	}
}


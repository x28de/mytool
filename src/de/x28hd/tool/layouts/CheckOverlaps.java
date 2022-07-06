package de.x28hd.tool.layouts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;

import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.GraphPanelControler;
import de.x28hd.tool.PresentationExtras;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class CheckOverlaps implements Comparator<Integer>, ActionListener {
	
	// Main fields
	Hashtable<Integer,GraphNode> nodes; 
	Hashtable<Integer,GraphNode> realNodes; 
	Hashtable<Integer,GraphEdge> edges;	
	Hashtable<Integer,GraphEdge> realEdges;	
	GraphPanelControler controler;
	PresentationExtras controlerExtras;
	
	// Major collections
	UndirectedSparseGraph<Integer, Integer> graph = new UndirectedSparseGraph<Integer,Integer>();
	CircleLayout<Integer,Integer> layout = new CircleLayout<Integer,Integer>(graph);
	Hashtable<GraphNode,HashSet<Integer>> allNeighbors = new Hashtable<GraphNode,HashSet<Integer>>();
	HashSet<GraphNode> chain = new HashSet<GraphNode>();
	Hashtable<Integer,Integer> positions = new Hashtable<Integer,Integer>();
	Hashtable<GraphEdge,Pair<Integer>> edgePairs = new Hashtable<GraphEdge,Pair<Integer>>();
	HashSet<GraphEdge> uncolored = new HashSet<GraphEdge>();
	HashSet<GraphEdge> justCrossed;
	Hashtable<GraphNode,Integer> node2vertex = new Hashtable<GraphNode,Integer>();
	Hashtable<Integer,GraphNode> vertex2node = new Hashtable<Integer,GraphNode>();
	
	// For the GUI
	JDialog nextReady;	// inelegant, TODO find out how to designate the default button otherwise 
	String [] flipColor = {"#00ff00", "#ff0000"};
	String [] flipColorName = {"GREEN", "RED"};
	int flip = 1;
	String note = "";
	boolean found;
	boolean initial = true;
	boolean cancel = false;
	boolean impatience = false;
	int microSteps = 0;

	// For findHamiltonian
    private int vertexCount, pathCount;
    private int[] path;     
    private int[][] matrix;

	// Miscellaneous
	GraphNode startNode = null;
	int size;
	boolean success = false;
//	boolean step4 = false;
	boolean diag = false;
	
//
//	Step 1: Prepare the graph
	
	public CheckOverlaps(GraphPanelControler controler, 
			Hashtable<Integer,GraphNode> realNodes, 
			Hashtable<Integer,GraphEdge> realEdges) {
		nodes = new Hashtable<Integer,GraphNode>();
		edges = new Hashtable<Integer,GraphEdge>();
		this.realNodes = realNodes;
		this.realEdges = realEdges;
		this.controler = controler;
		controlerExtras = controler.getControlerExtras();

		// Create a copy of the map (TODO: integrate with MakeCircle)
		// Copy the 'realNodes' to work 'nodes'
		
		Enumeration<GraphNode> nodeList1 = realNodes.elements();
		while (nodeList1.hasMoreElements()) {
			GraphNode toClone = nodeList1.nextElement();
			Enumeration<GraphEdge> neighbors = toClone.getEdges();
			HashSet<Integer> myNeighbors = new HashSet<Integer>();
			while (neighbors.hasMoreElements()) {
				GraphEdge edge = neighbors.nextElement();
				GraphNode neighbor = toClone.relatedNode(edge);
				int id = neighbor.getID();
				myNeighbors.add(id);
			}
			if (myNeighbors.size() < 2) continue;
			GraphNode clone = new GraphNode(toClone.getID(), toClone.getXY(), 
					toClone.getColor(), toClone.getLabel(), toClone.getDetail());
			nodes.put(toClone.getID(), clone);
			allNeighbors.put(clone, myNeighbors);
		}
		
		// Simplify:
		// prune subtrees and leaves (nodes with just 1 neighbor);
		// eliminate transit nodes ((with exactly 2 neighbors)
		
		int simplifyCount = 1;
		while (simplifyCount > 0) simplifyCount = simplify();

		//	Now add 'edges' to each of the 'nodes'
		int edgeID = 0;
		Enumeration<GraphNode> nodeList2 = nodes.elements();
		while (nodeList2.hasMoreElements()) {
			GraphNode node = nodeList2.nextElement();
			HashSet<Integer> neighbors = allNeighbors.get(node);
			Iterator<Integer> iter3 = neighbors.iterator();
			while (iter3.hasNext()) {
				int id = iter3.next();
				if (id < node.getID()) continue;
				GraphNode otherEnd = nodes.get(id);
				GraphEdge edge = new GraphEdge(node.getID(), node, otherEnd, 
				Color.LIGHT_GRAY, "");
				edgeID++;
				edges.put(edgeID, edge);
				node.addEdge(edge);;
				otherEnd.addEdge(edge);
			}
		}
		
		// Trivial case
		controlerExtras.replaceForLayout(nodes,  edges);
		if (nodes.size() < 1) {
			controler.displayPopup("<html>After iterative simplification,<br> "
				+ "(eliminating icons with less than 3 neighbors),<br>"
				+ "no icons are left. So, the graph is <b>planar</b>.");
			controlerExtras.replaceForLayout(realNodes, realEdges);
			return;
		}
		
		// Create graph for uci.jung circle and sanfoundry.com algorithm
		initializeGraph();
		size = graph.getVertexCount();
		
		// Prompt for option regarding Hamiltonian
		nextReady = new JDialog(controler.getMainWindow(), "Next step");
		nextReady.setLayout(new BorderLayout());
		nextReady.setLocation(800, 30);
		nextReady.setMinimumSize(new Dimension(300, 200));
		nextReady.setVisible(true);
		JLabel info = new JLabel("<html>Here is the core of the graph <br>"
				+ "(maybe you need to pan the map).<br><br>"
				+ "Now we need a Hamiltonian cycle path (one <br>"
				+ "that visits each icon exactly once). <br><br>"
				+ "Click <b>Next</b> for a simple brute-force <br>"
				+ "algorithm, or <b>Manual</b> if you have provided <br>"
				+ "such a cycle yourself by coloring its <br>"
				+ "lines in lurid blue.</html>");
			
		info.setBorder(new EmptyBorder(10, 45, 10, 45));
		nextReady.add(info, "North");
		JButton nextButton = new JButton("Next");
		nextButton.addActionListener(this);
		nextReady.add(nextButton, "East");

		JButton manButton = new JButton("Manual");
		manButton.addActionListener(this);
		nextReady.add(manButton);
		nextReady.add(manButton, "West");
		
		nextButton.requestFocusInWindow();
		nextReady.pack();
		
		// Proceeds with either manualHamiltonian(), or autoHamiltonian()
	}
	
	// Called from step 1
	
	public int simplify() {
		int simplifyCount = 0;
		Enumeration<GraphNode> nodeList = nodes.elements();
		while (nodeList.hasMoreElements()) {
			GraphNode node = nodeList.nextElement();
			HashSet<Integer> neighbors = allNeighbors.get(node);
			Iterator<Integer> iter = neighbors.iterator();
	
			// Remove references to deleted nodes
			while (iter.hasNext()) {
				int id = iter.next();
				if (!nodes.containsKey(id)) {
					iter.remove();
					neighbors.remove(id);
					simplifyCount++;
				}
			}
			
			// Prune leaves
			if (neighbors.size() < 2) {
				nodes.remove(node.getID());
				simplifyCount++;
				
			// Eliminate bridge nodes	
			} else if (neighbors.size() == 2) {
				boolean first = true;
				int id1 = 0; 
				int id2 = 0;
				Iterator<Integer> iter2 = neighbors.iterator();
				while (iter2.hasNext()) {
					if (first) id1 = iter2.next();
					if (!first) id2 = iter2.next();
					first = !first;
				}
				GraphNode node1 = nodes.get(id1);
				GraphNode node2 = nodes.get(id2);
				HashSet<Integer> neighbors1 = allNeighbors.get(node1);
				HashSet<Integer> neighbors2 = allNeighbors.get(node2);
				neighbors1.add(id2);
				neighbors2.add(id1);
				neighbors1.remove(node.getID());
				neighbors2.remove(node.getID());
				nodes.remove(node.getID());
				simplifyCount++;
			}
		}
		return simplifyCount;
	}

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
	}

//
//	Step 2a: Find the Hamiltonian without the algorithm -- can be repeated from actionPerformed()
	
	public void manualHamiltonian() {
		chain.clear();
		int pos = 0;
		int startID = 0;
		while (!nodes.containsKey(startID)) startID++;
		startNode = nodes.get(startID);
		GraphNode node = startNode;
		GraphNode otherEnd = null;
		chain.add(node);
		while (pos <= nodes.size()) {
			pos++;
			boolean nextFound = false;
			Enumeration<GraphEdge> neighbors = node.getEdges();
			while (neighbors.hasMoreElements()) {
				GraphEdge edge = neighbors.nextElement();
				if (!edge.getColor().equals(Color.BLUE) &&
					!edge.getColor().equals(Color.decode("#bbbbff"))) {
					continue;
				} else {
					otherEnd = node.relatedNode(edge);
					if (otherEnd.equals(startNode)) {
						if (pos < nodes.size()) continue;
						int id = otherEnd.getID();
						positions.put(id, 0);
						
						nextReady.dispose();
						analyzeEdges();
						
						return;
					} 
					if (chain.contains(otherEnd)) continue;
					nextFound = true;
					chain.add(otherEnd);
					int id = otherEnd.getID();
					positions.put(id, pos);
					break;
				}
			}
			if (!nextFound) {
				controler.displayPopup("<html>Cannot validate your input: <br>"
				+ "no successor found for node \"" + node.getLabel() + "\"<br>"
				+ "Please try again.");
				controler.getMainWindow().repaint();
				return;
			} else {
				node = otherEnd;
			}
		}
	}

//
//	Step 2b: Automatic search for Hamiltonian -- may be canceled
	
	public void autoHamiltonian() {
		
		node2vertex = new Hashtable<GraphNode,Integer>();
		vertex2node = new Hashtable<Integer,GraphNode>();

		// Create an adjacency matrix
		int vertexCount = nodes.size();
		int[][] matrix = new int[vertexCount][vertexCount];
		for (int i = 0; i < vertexCount; i++)
			for (int j = 0; j < vertexCount; j++)
				matrix[i][j] = 0;
		Enumeration<GraphNode> nodeList = nodes.elements();
		int id = 0;
		while (nodeList.hasMoreElements()) {
			GraphNode node = nodeList.nextElement();
			node2vertex.put(node, id);
			vertex2node.put(id, node);
			if (diag) node.setLabel(id + " " + node.getLabel());
			id++;
		}
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			GraphNode node1 = edge.getNode1();
			GraphNode node2 = edge.getNode2();
			int id1 = node2vertex.get(node1);
			int id2 = node2vertex.get(node2);
			matrix[id1][id2] = 1;
			matrix[id2][id1] = 1;
		}

		success = false;
		success = hamFind(matrix);
		if (!success) {
			offerResult();
			return;
		}

		// Derive vertex order for circle layout
		for (int i = 0; i < path.length; i++) {
			id = path[i];
			GraphNode pathNode = vertex2node.get(id);
			int nodeID = pathNode.getID();
			positions.put(nodeID, i);
		}

		analyzeEdges();
	}
	
	// Algorithm for Hamiltonian Cycle, 
	// adapted from from https://www.sanfoundry.com/

	public boolean hamFind(int[][] g) {
		vertexCount = g.length;
		path = new int[vertexCount];
		for (int i = 0; i < vertexCount - 1; i++) path[i] = -1;
		matrix = g;        
		try {            
			path[0] = 0;
			pathCount = 1;            
			hamSolve(0);
			if (!cancel) controler.displayPopup("There is no Hamiltonian cycle.\n"
					+ "No simple solution, sorry.");
		}
		catch (Exception e) {
			return true;
		}
		return false;
	}

	public void hamSolve(int vertex) throws Exception {
		microSteps++;
		if ((microSteps % 1000000) == 0) {
			
			Object[] options = {"Cancel", "Continue"};
			int response = JOptionPane.showOptionDialog(controler.getMainWindow(),
					"<html>Searching for a Hamiltonian cycle,<br>"
					+ microSteps + " steps processed so far.<br><br>"
					+ "If the simple algorithm takes too long<br>"
					+ "you may click <b>Cancel</b> and <br>"
					+ "try again with a manual solution,<br>"
					+ "or with parts of the map.</html>",
					"Searching",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[1]);
			if (response == 0) {
				cancel = true;
				success = false;
				return;
			}
		}

		// solution 
		if (matrix[vertex][0] == 1 && pathCount == vertexCount) 
			throw new Exception("Solution found");
		if (pathCount == vertexCount)
			return;

		for (int v = 0; v < vertexCount; v++) {
			if (matrix[vertex][v] == 1 ) {

				// add to path             
				path[pathCount++] = v;  

				// remove connection            
				matrix[vertex][v] = 0;
				matrix[v][vertex] = 0;

				boolean present = false;
				for (int i = 0; i < pathCount - 1; i++) {
					if (path[i] == v) {
						present = true;
						break;
					}
				}
				if (!present) hamSolve(v);			// recursion
				if (cancel) return;

				// restore connection 
				matrix[vertex][v] = 1;
				matrix[v][vertex] = 1;
				// remove path 
				path[--pathCount] = -1;   
			}
		}
	}    
		
//
//	Step 3 (the main step): Analyze and color the edges
	
	public void analyzeEdges() {
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			int n1 = edge.getN1();
			int n2 = edge.getN2();
			int pos1 = positions.get(n1);
			int pos2 = positions.get(n2);
			int distanz = Math.abs(pos1 - pos2);
			if (distanz == 1 || distanz == nodes.size() - 1) {
				edge.setColor("#0000ff");
			} else {
				uncolored.add(edge);
				edgePairs.put(edge, new Pair<Integer>(pos1, pos2));
				edge.setColor("#a0a0a0");
			}
		}
		
		circleLayout();
	}
	
	public void watch() {
		
		success = true;
		
		while (uncolored.size() > 0) pickUncolored();
		
		if (success) controler.displayPopup("<html>No more uncolored edges:<br>"
				+ "The graph is <b>planar</b>!<br><br>"
				+ "You get an overlap-free map by moving<br> "
				+ "the red lines outside the blue outline.");
		offerResult();
	}

//
//	Called from step 3
	
	public void circleLayout() {
		layout = new CircleLayout<Integer,Integer>(graph);
		layout.setVertexOrder((Comparator<Integer>) this); // see compare()
		layout.initialize();
		int diameter = (int) (Toolkit.getDefaultToolkit().getScreenSize().height * .8);
		layout.setSize(new Dimension(diameter, diameter)); 
	
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
		
		controler.getMainWindow().repaint();
		nextReady = new JDialog(controler.getMainWindow(), "Next step");
		nextReady.setLayout(new BorderLayout());
		nextReady.setLocation(800, 30);
		nextReady.setMinimumSize(new Dimension(300, 100));
		nextReady.setVisible(true);
		JLabel info = new JLabel("<html>Now checking if overlaps are unavoidable.<br><br>" 
				+ "Make sure you see the entire circle;<br>"
				+ "if necessary,  resize or/and pan your window.");
		info.setBorder(new EmptyBorder(10, 45, 10, 45));
		nextReady.add(info, "North");
		JButton nextButton = new JButton("Ready?");
		nextButton.setActionCommand("Ready");
		nextButton.addActionListener(this);
		nextReady.add(nextButton, "East");
		nextButton.requestFocusInWindow();
		nextReady.pack();
	}

	public void pickUncolored() {
		GraphEdge edge1 = uncolored.iterator().next();
		edge1 = uncolored.iterator().next();
		edge1 = uncolored.iterator().next();	// Add or delete for varying tests
		edge1.setColor("#00ff00");
		note = "We picked an uncolored edge and colored it green.<br><br>";
		
		if (initial) {
			note += "Crossing lines will be colored red and green by turns.<br>"
					+ "Lines of the same color must not cross.<br><br>";
			found = true;
			initial = false;
		}
		uncolored.remove(edge1);
		
		justCrossed = new HashSet<GraphEdge>();
		justCrossed.add(edge1);
		flip = 1;
		while (justCrossed.size() > 0) findCrossingEdges(justCrossed);
	}

	public void findCrossingEdges(HashSet<GraphEdge> crossed) {
		HashSet<GraphEdge> crossingEdges = new HashSet<GraphEdge>();	// output
		Iterator<GraphEdge> iter1 = crossed.iterator();					// input
	
		// Look at one edge
		while (iter1.hasNext()) {
			GraphEdge edge1 = iter1.next();
			Pair<Integer> one = edgePairs.get(edge1);
			
			emphasize(1 - flip, edge1);
			controler.getMainWindow().repaint();
			if (!found) note = "Nothing found.<br>" + note;
			if (!impatience) {
				nextReady = new JDialog(controler.getMainWindow(), "Watch", true);
				nextReady.setLayout(new BorderLayout());
				nextReady.setLocation(800, 30);
				nextReady.setMinimumSize(new Dimension(300, 100));
				JLabel info = new JLabel("<html>" + note 
						+ "Searching for uncolored edges <br>crossing the highlighted "
						+ "<font color=\"" + flipColorName[1 - flip] + "\">"
						+ flipColorName[1 - flip] + "</font> edge" );
				info.setBorder(new EmptyBorder(10, 45, 10, 45));
				nextReady.add(info, "North");
				JButton nextButton = new JButton("Show steps");
				nextButton.setActionCommand("Step");
				nextButton.addActionListener(this);
				nextReady.add(nextButton, "West");

				JButton manButton = new JButton("Finish");
				manButton.addActionListener(this);
				nextReady.add(manButton);
				nextReady.add(manButton, "East");
				nextReady.pack();
				nextReady.setVisible(true);
			}
			note = "";
			found = false;

			// Pick another
			Iterator<GraphEdge> iter2 = uncolored.iterator();
			HashSet<GraphEdge> justColored = new HashSet<GraphEdge>();
			while (iter2.hasNext()) {
				GraphEdge edge2 = iter2.next();
				if (crossingEdges.contains(edge2)) continue;
				if (edge2.equals(edge1)) continue;
				Pair<Integer> two = edgePairs.get(edge2);
				
				// Cross?
				if (crossing(one, two)) {
					if (eachOther(edge2, crossingEdges)) {
						justCrossed.clear();
						success = false;
						offerResult();
						return;
					}
					justColored.add(edge2);
					found = true;
					crossingEdges.add(edge2);
				} 
			}

			// Color the new ones
			deEmphasize(flip);
			Iterator<GraphEdge> iter3 = justColored.iterator();
			while (iter3.hasNext()) {
				GraphEdge edge = iter3.next();
				edge.setColor(flipColor[flip]);
				uncolored.remove(edge);
			}
			controler.getMainWindow().repaint();
			if (found && impatience == false ){
				nextReady = new JDialog(controler.getMainWindow(), "Watch", true);
				nextReady.setLayout(new BorderLayout());
				nextReady.setLocation(800, 30);
				nextReady.setMinimumSize(new Dimension(300, 100));
				JLabel info = new JLabel("<html>Found the highlighted "
				+ "<font color=\"" + flipColorName[flip] + "\">"
				+ flipColorName[flip] + "</font> edges" );
				info.setBorder(new EmptyBorder(10, 45, 10, 45));
				nextReady.add(info, "North");
				JButton nextButton = new JButton("Show steps");
				nextButton.setActionCommand("Step");
				nextButton.addActionListener(this);
				nextReady.add(nextButton, "West");

				JButton manButton = new JButton("Finish");
				manButton.addActionListener(this);
				nextReady.add(manButton);
				nextReady.add(manButton, "East");
				nextReady.pack();
				nextReady.setVisible(true);
			}
		}
		justCrossed = crossingEdges;
		flip = 1 - flip;
	}
	
//
//	Accessories for Step 3
	
	// Do the two lines cross?

	public boolean crossing(Pair<Integer> one, Pair<Integer> two) {
		int oneStart = one.getFirst();
		int oneEnd = one.getSecond();
		int twoStart = two.getFirst();
		int twoEnd = two.getSecond();
		if (oneStart == twoStart || oneStart == twoEnd 
		  || oneEnd == twoStart || oneEnd == twoEnd) return false;
		int smallerOfOne = Math.min(oneStart, oneEnd);
		int largerOfOne = Math.max(oneStart, oneEnd);
		boolean startRangeOfTwo = (smallerOfOne < twoStart && twoStart < largerOfOne);
		boolean endRangeOfTwo = (smallerOfOne < twoEnd && twoEnd < largerOfOne);
		return startRangeOfTwo != endRangeOfTwo;
	}
	
	// Abort (return true) if new crossingEdges cross each other
	
	public boolean eachOther(GraphEdge edge4, HashSet<GraphEdge> crossingEdges) {
	
		Iterator<GraphEdge> iter2 = crossingEdges.iterator();
		while (iter2.hasNext()) {
			GraphEdge edge3 = iter2.next();
			edge3.setColor(flipColor[flip]);
			Pair<Integer> three = edgePairs.get(edge3);
			if (edge4.equals(edge3)) continue;
			Pair<Integer> four = edgePairs.get(edge4);
			if (crossing(three, four)) {
				edge3.setColor("#ffff00");
				edge4.setColor("#ffff00");
				controler.getMainWindow().repaint();
				controler.displayPopup("<html>The graph is <b>not planar</b>,<br>"
						+ "because the yellow edges cross each other.</html>");
				uncolored.clear();
				return true;
			} 
		}
		return false;
	}

	// TODO make less clumsy

	public void emphasize(int flip, GraphEdge emphasis) {
		Enumeration<GraphEdge> paleList = edges.elements();
		while (paleList.hasMoreElements()) {
			GraphEdge edge = paleList.nextElement();
			if (edge.equals(emphasis)) continue;
			if (edge.getColor().equals(Color.RED)) edge.setColor("#ffbbbb");
			if (edge.getColor().equals(Color.GREEN)) edge.setColor("#bbffbb");
		}
		if (flip == 1 && emphasis.getColor().equals(Color.decode("#ffbbbb"))) emphasis.setColor("#ff0000");
		if (flip == 0 && emphasis.getColor().equals(Color.decode("#bbffbb"))) emphasis.setColor("#00ff00");
		return;
	}

	public void deEmphasize(int flip) {
		Enumeration<GraphEdge> paleList = edges.elements();
		while (paleList.hasMoreElements()) {
			GraphEdge edge = paleList.nextElement();
			if (flip == 1 && edge.getColor().equals(Color.RED)) edge.setColor("#ffbbbb");
			if (flip == 0 && edge.getColor().equals(Color.GREEN)) edge.setColor("#bbffbb");
		}
	}

//
//	Step 4: Finish
	
	public void offerResult() {
		
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			if (edge.getColor().equals(Color.decode("#ffbbbb"))) edge.setColor("#ff0000");
			if (edge.getColor().equals(Color.decode("#bbffbb"))) edge.setColor("#00ff00");
		}
		nextReady.dispose();
		controler.getMainWindow().repaint();
		
		nextReady = new JDialog(controler.getMainWindow(), "Next step");
		nextReady.setLayout(new BorderLayout());
		nextReady.setLocation(800, 30);
		nextReady.setMinimumSize(new Dimension(300, 100));
		nextReady.setVisible(true);
		JLabel info = new JLabel("<html>Before returning the hidden nodes,<br>"
				+ "you may rearrange the map, <br>"
				+ "or save it for later manual improvements.<br>");
		info.setBorder(new EmptyBorder(10, 45, 10, 45));
		nextReady.add(info, "North");
		JButton nextButton = new JButton("Ready?");
		nextButton.setActionCommand("restore");
		nextButton.addActionListener(this);
		nextReady.add(nextButton, "East");
		nextButton.requestFocusInWindow();
		nextReady.pack();
	}
	
	public void restoreMap() {
		
		if (success) {
			
			// Apply the new coordinates
			Collection<Integer> nodeIDs = graph.getVertices();
			Iterator<Integer> nodeIt = nodeIDs.iterator();
			while (nodeIt.hasNext()) {
				int id = nodeIt.next();
				GraphNode cloneNode = nodes.get(id);
				Point xy = cloneNode.getXY();
				GraphNode node = realNodes.get(id);
				node.setXY(xy);
			}
		}
		controlerExtras.replaceForLayout(realNodes, realEdges);
	}
	
//
//	Accessories for the interfaces
	
	public int compare(Integer arg0, Integer arg1) {
		return positions.get(arg0) - positions.get(arg1);
	}


	public void actionPerformed(ActionEvent arg0) {
		String command = arg0.getActionCommand();
		if (command == "Manual") {
			manualHamiltonian();
			return;
		} else if (command == "Next") {
			nextReady.dispose();
			autoHamiltonian();
		} else if (command == "Ready") {
			nextReady.dispose();
			watch();
		} else if (command == "Step") {
			nextReady.dispose();
		} else if (command == "Finish") {
			impatience = true;
			nextReady.dispose();
		} else if (command == "restore") {
			nextReady.dispose();
			restoreMap();
		}
	}
}

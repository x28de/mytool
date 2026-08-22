package de.x28hd.tool.layouts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import de.x28hd.tool.GraphPanel;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;  

public class SimilarityColoring implements ActionListener {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	PresentationService controler;
	Hashtable<Integer, Color> nodesSavedColors = new Hashtable<Integer, Color>();
	Hashtable<Integer, Color> edgesSavedColors = new Hashtable<Integer, Color>();
	
	Hashtable<GraphNode,Integer> nodesNewColors = new Hashtable<GraphNode,Integer>();
	HashSet<Pair<Integer>> colorCombis = new HashSet<Pair<Integer>>();
	Hashtable<Integer,Integer> positions = new Hashtable<Integer,Integer>();
	Hashtable<Integer,Integer> sizes = new Hashtable<Integer,Integer>();
	Hashtable<Integer,Integer> startPlaces = new Hashtable<Integer,Integer>();
	Hashtable<Integer,Integer> spreads = new Hashtable<Integer,Integer>();
	Hashtable<Color,HashSet<GraphEdge>> groupEdgeSets = new Hashtable<Color,HashSet<GraphEdge>>();
	HashSet<GraphEdge> groupEdgeSet = null;
	
	JSlider slider;
	JDialog panel;
	JCheckBox paletteBox;
	boolean palette = false;
	HashSet<GraphNode> paletteNodes = new HashSet<GraphNode>();
	JTextField numEdgesToRemoveBox;
	int numEdgesToRemove = -1;
	JCheckBox xByColor;
	JCheckBox yByColor;
	boolean sortByColor;
	boolean hori;
	int oldsum = 0;
	int newsum = 0;
	int xGap = 150;
	int yGap = 120; 

	public SimilarityColoring(Hashtable<Integer,GraphNode> nodes,
			Hashtable<Integer,GraphEdge> edges, PresentationService controler) {
		this.controler = controler;
		this.nodes = controler.getNodes();
		this.edges = controler.getEdges();
		
		createSlider();
	}
	
	public void mainPart() {
		int var = slider.getValue();
		double linear = .5 * var;
		if (var < 50) {
			var = (int) linear;
		} else {
			double squared = Math.pow(var - 50, 2) /50.;
			var = (int) Math.round(linear + squared);
		}
		var = var * edges.size()/200;
		if (numEdgesToRemove > 0) var = numEdgesToRemove;
		System.out.println(slider.getValue() + " selected; use " + var + " of " + edges.size() + " edges");

		UndirectedSparseGraph<Integer, Integer> g = new UndirectedSparseGraph<Integer,Integer>();
		EdgeType edgeType = EdgeType.UNDIRECTED; 
		EdgeBetweennessClusterer<Integer,Integer> clusterer = 
				new EdgeBetweennessClusterer<Integer,Integer>(var);  // 10
		Enumeration<GraphEdge> edgesEnum = edges.elements();
		int edgeID = 0;
		while (edgesEnum.hasMoreElements()) {
			edgeID++;
			GraphEdge edge = edgesEnum.nextElement();	
			int n1 = edge.getN1();
			if (!nodes.containsKey(n1)) continue;
			int n2 = edge.getN2();
			if (!nodes.containsKey(n2)) continue;
			g.addEdge(edgeID, n1, n2, edgeType);
		}
		
		// Record savedColors for reverting
		Enumeration<GraphNode> nodeList  = nodes.elements();
		while (nodeList.hasMoreElements()) {
			GraphNode node = nodeList.nextElement();
			Color oldColor = node.getColor();
			int id = node.getID();
			nodesSavedColors.put(id,oldColor);
		}
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			Color oldColor = edge.getColor();
			int id = edge.getID();
			edgesSavedColors.put(id,oldColor);
		}
		
		Set<Set<Integer>> clusterSet = clusterer.transform(g);
		
		String colorString = "#ccdddd";
		Iterator<Set<Integer>> outerPreview = clusterSet.iterator();
		int colorCount = 0;
		int colorId = 0;
		while (outerPreview.hasNext()) {
			Set<Integer> set = outerPreview.next();
			if (set.size() > 1) colorCount++;
		}
		
		// Outer loop, colors
		
		Iterator<Set<Integer>> outer = clusterSet.iterator();
		boolean odd = false;
		while (outer.hasNext()) {
			Set<Integer> set = outer.next();
			if (set.size() > 1) {
				float saturation = 1;
				if (odd) saturation = (float) .5;	// Distinguish from near hues
				Color color = Color.getHSBColor((float) ((colorId + .001)/colorCount + .16), 
						saturation, (float) 1); 
				int red = color.getRed();
				int green = color.getGreen();
				int blue = color.getBlue();
				colorString = String.format("#%02x%02x%02x", red, green, blue);
				if (palette) {
					GraphNode specimen = controler.createNode(new Point(0, 40 + 20 * colorId));
					specimen.setColor(colorString);
					paletteNodes.add(specimen);
				}
				float [] hsbvalues = {0, 0, 0};
				Color.RGBtoHSB(red,  green,  blue, hsbvalues);
				colorId++;
				sizes.put(colorId, set.size());
				odd =  !odd;
			} else {
				colorString = "#ccdddd";
			}
			
			// Inner loop, plus optional sorting
			
			Iterator<Integer> inner = set.iterator();
			TreeMap<Integer,GraphNode> orthogonalMap = new TreeMap<Integer,GraphNode>(); 
			while (inner.hasNext()) {
				int n = inner.next();
				GraphNode node = nodes.get(n);
				node.setColor(colorString);
				nodesNewColors.put(node, colorId);
				if (!sortByColor) continue;

				Point xy = node.getXY();
				int z = hori ? xy.x : xy.y;
				orthogonalMap.put(z, node);
			}

			// Create line-ups (= hori ? columns : rows) by color
			// (orthogonal = hori ? rows : columns)
			if (!sortByColor) continue;
			SortedMap<Integer,GraphNode> orthogonalList = (SortedMap<Integer,GraphNode>) orthogonalMap;
			SortedSet<Integer> orthogonalSet = (SortedSet<Integer>) orthogonalList.keySet();
			Iterator<Integer> iter = orthogonalSet.iterator();
			int z = 40;
			int increment = hori ? 40 : 200;
			boolean first = true;
			while (iter.hasNext()) {
				int orthogonalRank = iter.next();
				GraphNode node = orthogonalMap.get(orthogonalRank);
				Point xy = node.getXY();
				if (first) {								// anchor line by top item
					int ortho = hori ? xy.y : xy.x;
					if (hori) z = ortho * colorCount/nodes.size();		// scaled by average line size
					first  = false;
				}
				// provisional placement for testing; only z is kept
				xy = hori ? new Point(xGap * colorId, z) : new Point(z, yGap * colorId);
				node.setXY(xy);
				z += increment;
			}
		}
		
		if (sortByColor) arrangeLines(colorCount);
		if (hori) return; 
		
		new DagLayered(nodes, edges, controler, true, false);			// arrange by layers
		
		panel = new JDialog(controler.getMainWindow(), "Select an edge", false);
		panel.setMinimumSize(new Dimension(250, 100));
		panel.setLocation(200, 200);
		panel.setLayout(new BorderLayout());
		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());
		bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
		JButton subgraph = new JButton("Continue");
		subgraph.setActionCommand("Subgraph");
		subgraph.addActionListener(this);
		JLabel label = new JLabel("Show a subgraph ");
		bottom.add(label, "West");
		bottom.add(subgraph, "East");
		panel.add(bottom, "South");
		panel.setVisible(true);
	}

	public void createSlider() {
		slider = new JSlider(JSlider.VERTICAL);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setMajorTickSpacing(10);
		slider.setValue((int) Math.round((edges.size() 	// dense network need higher value
				- nodes.size() + .0001)/edges.size() * 100)); 
		
		panel = new JDialog(controler.getMainWindow(), "Few or many groups?", true);
		panel.setMinimumSize(new Dimension(250, 500));
		panel.setLocation(200, 200);
		panel.setLayout(new BorderLayout());
		slider.setLabelTable(slider.createStandardLabels(10));
		panel.add(slider);
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());
		bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
		JPanel advanced = new JPanel();
		advanced.add(new JLabel("Advanced: R"));
		numEdgesToRemoveBox = new JTextField(4); 	// numEdgesToRemove 
		numEdgesToRemoveBox.setToolTipText("Set 'numEdgesToRemove' variable directly");
		advanced.add(numEdgesToRemoveBox);
		xByColor = new JCheckBox("H");
		xByColor.setToolTipText("Arrange horizontally by Color");
		xByColor.addActionListener(this);
		advanced.add(xByColor);
		yByColor = new JCheckBox("V");
		yByColor.setToolTipText("Arrange vertically by Color");
		yByColor.addActionListener(this);
		advanced.add(yByColor);
		bottom.add(advanced, "North");
		paletteBox = new JCheckBox("Show palette");
		paletteBox.setSelected(true);
		bottom.add(paletteBox, "West");
		JButton continueButton = new JButton("OK");
		continueButton.addActionListener(this);
		continueButton.setSelected(true);
		bottom.add(continueButton, "East");
		panel.add(bottom, "South");
		panel.setVisible(true);
	}
	
	public void arrangeLines(int colorCount) {

		// Find edges between two different colors
		
		if (!sortByColor) return;
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			GraphNode node1 = edge.getNode1();
			GraphNode node2 = edge.getNode2();
			int color1 = nodesNewColors.get(node1);
			int color2 = nodesNewColors.get(node2);
			Pair<Integer> pair = null;
			if (color1 < color2) {
				pair = new Pair<Integer>(color1, color2);
			} else if (color1 > color2) {
				pair = new Pair<Integer>(color2, color1);
			}
			if (pair == null) continue;
			colorCombis.add(pair);
		}
		
		// Initialize line positions
		
		for (int i = 1; i <= colorCount; i++) {
			positions.put(i, i);
		}
		
		// Minimize sum of distances by swapping neighbors
		
		oldsum = calculateDistance();
		int success = 1;
		int iteration = 0;
		while (success > 0 && iteration < 50) {
			success = 0;
			
			Collection<Integer> sourceIDs = positions.keySet();
			Iterator<Integer> sources = sourceIDs.iterator();
			while (sources.hasNext()) {
				int source = sources.next();

				Collection<Integer> targetIDs = positions.keySet();
				Iterator<Integer> targets = targetIDs.iterator();
				while (targets.hasNext()) {
					int target = targets.next();
					if (source == target) continue;
					if (Math.abs(positions.get(source) - positions.get(target)) > 1) continue;
					if (swapLines(source, target)) {
						success++;
					}
				}
			}
			System.out.println("Iteration number " + iteration++ + " ...");
		}
		
		if (hori) return;
		groupPlaces();
		nodePlacement();
		colorGroups();
	}

	public boolean swapLines(int first, int second) {
		int third = positions.get(first);
		positions.put(first, positions.get(second));
		positions.put(second, third);
		newsum = calculateDistance();
		if (oldsum - newsum >= 0) {
			linesRearrange();
			oldsum = newsum;
			return true;
		} else {
			return false;
		}
	}

	public int calculateDistance() {
		// Calculate sum of edges lengths between lines
		int sum = 0;
		Iterator<Pair<Integer>> pairList2 = colorCombis.iterator();
		while (pairList2.hasNext()) {
			Pair<Integer> pair = pairList2.next();
			int diff = positions.get(pair.getSecond()) - positions.get(pair.getFirst());
			sum += Math.abs(diff);
		}
		return sum;
	}
	
	public void linesRearrange() {
		Enumeration<GraphNode> nodesList = nodesNewColors.keys();
		while (nodesList.hasMoreElements()) {
			GraphNode node = nodesList.nextElement();
			int line = nodesNewColors.get(node);
			int sortOrder = positions.get(line);
			Point xy = node.getXY();
			int ortho = hori ? xy.y : xy.x;
			xy = hori ? new Point(sortOrder * xGap, ortho) : new Point(ortho, sortOrder * yGap);
			node.setXY(xy);
		}
	}
	
	public void groupPlaces() {
		Hashtable<Integer,Integer> colorsByPosition = new Hashtable<Integer,Integer>(); 
		Hashtable<Integer,Integer> sizesByPosition = new Hashtable<Integer,Integer>(); 
		for (int colorId = 1; colorId <= positions.size(); colorId++) {
			
			colorsByPosition.put(positions.get(colorId), colorId);
			sizesByPosition.put(positions.get(colorId), sizes.get(colorId));
		}
		int startPlace = 0;
		for (int pos = 1; pos <= positions.size(); pos++) {
			int colorId = colorsByPosition.get(pos);
			startPlaces.put(colorId, startPlace);
			startPlace += sizesByPosition.get(pos) * 15;
			startPlace += 150;
		}
	}
	
	public void nodePlacement() {
		Enumeration<GraphNode> nodesList = nodesNewColors.keys();
		while (nodesList.hasMoreElements()) {
			GraphNode node = nodesList.nextElement();
			int line = nodesNewColors.get(node);
			int spread = 0;
			if (spreads.containsKey(line)) {
				spread = spreads.get(line) + 15;
			} 
			spreads.put(line, spread);
			Point xy = node.getXY();
			xy = new Point(xy.x, startPlaces.get(line) + spread);
			node.setXY(xy);
		}
	}
	public void colorGroups() {
		Enumeration<GraphEdge> edgeList = edges.elements();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			GraphNode node1 = edge.getNode1();
			GraphNode node2 = edge.getNode2();
			Color color1 = node1.getColor();
			Color color2 = node2.getColor();
			if (!color1.equals(color2)) {
				edge.setColor("#c0c0c0");
				continue;
			} else {
				int r = color1.getRed();
				int g = color1.getGreen();
				int b = color1.getBlue();
				String colorString1 = String.format("#%02x%02x%02x", r, g, b);
				edge.setColor(colorString1);
			}
			if (!groupEdgeSets.containsKey(color1)) {
				groupEdgeSet = new HashSet<GraphEdge>();
				groupEdgeSets.put(color1, groupEdgeSet);
			} else {
				groupEdgeSet = groupEdgeSets.get(color1);
			}
			groupEdgeSet.add(edge);
		}
	}
	
	public void drawGraph() {
		GraphEdge selected = controler.getSelectedEdge();
		Color color = selected.getColor();
		if (color == null || Color.decode("#c0c0c0").equals(color)) {
			controler.displayPopup("Select a colored edge");
			return;
		}
		int test = 0;
		HashSet<GraphEdge> groupEdgeSet = groupEdgeSets.get(color);
		
		// construct clip map
		
		Hashtable<Integer,GraphNode> clipNodes = new Hashtable<Integer,GraphNode>();
		Hashtable<Integer,GraphEdge> clipEdges = new Hashtable<Integer,GraphEdge>();
		
		Enumeration<Integer> nodeList = nodes.keys();
		while (nodeList.hasMoreElements()) {
			int nodeId = nodeList.nextElement();
			GraphNode node = nodes.get(nodeId);
			Color testColor = node.getColor();
			if (!color.equals(testColor)) continue;
			Point xy = node.getXY();
			GraphNode clipNode = new GraphNode(nodeId, xy, color, "", ""); 	// just xy and edges will be used
			clipNodes.put(nodeId, clipNode);
		}
		Enumeration<Integer> edgeList = edges.keys();
		while (edgeList.hasMoreElements()) {
			int edgeId = edgeList.nextElement();
			GraphEdge edge = edges.get(edgeId);
			if (!groupEdgeSet.contains(edge)) continue;
			int n1 = edge.getN1();
			if (!clipNodes.containsKey(n1)) continue;
			int n2 = edge.getN2();
			if (!clipNodes.containsKey(n2)) continue;
			GraphNode clipNode1 = clipNodes.get(n1);
			GraphNode clipNode2 = clipNodes.get(n2);
			GraphEdge clipEdge = new GraphEdge(edgeId, clipNode1, clipNode2, color, "");	//use just the nodes
			clipEdges.put(edgeId, clipEdge);
			clipNode1.addEdge(clipEdge);
			clipNode2.addEdge(clipEdge);
		}

		
		System.out.println("Calling the optimizer with " + clipNodes.size() + " nodes");
		new DagLayered(clipNodes, clipEdges, controler, true, true);
		Enumeration<GraphNode> clipList = clipNodes.elements();
		while (clipList.hasMoreElements()) {
			GraphNode clipNode = clipList.nextElement();
			int id = clipNode.getID();
			GraphNode realNode = nodes.get(id);
			realNode.setXY(clipNode.getXY());
		}
		GraphPanel graphPanel = controler.getGraphPanel();
		graphPanel.repaint();
	}
	
	public void revertColors() {
		Iterator<GraphNode> iter = paletteNodes.iterator();
		while (iter.hasNext()) controler.deleteNode(iter.next(), true);
		
		int response = 1;
		if (!hori) {
			// suggestion, in particular for red/ green argument maps
			Object[] options =  {"Yes, sounds good", "No, revert it all"};
			response = JOptionPane.showOptionDialog(null,
					"Should we keep the group colors with the nodes?",
					"Suggestion", JOptionPane.YES_NO_OPTION, 
					JOptionPane.QUESTION_MESSAGE, null, 
					options, options[1]);

			Enumeration<GraphEdge>edgesEnum = edges.elements();
			while (edgesEnum.hasMoreElements()) {
				GraphEdge edge = edgesEnum.nextElement();
				Color originalColor = edgesSavedColors.get(edge.getID());
				int r = originalColor.getRed();
				int g = originalColor.getGreen();
				int b = originalColor.getBlue();
				edge.setColor(String.format("#%02x%02x%02x", r, g, b));
			}
		}
		if (response == 0) return;
		
		Enumeration<GraphNode>nodesEnum = nodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Color originalColor = nodesSavedColors.get(node.getID());
			int r = originalColor.getRed();
			int g = originalColor.getGreen();
			int b = originalColor.getBlue();
			node.setColor(String.format("#%02x%02x%02x", r, g, b));
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand() == "H") {
			sortByColor = true;
			hori = true;
			paletteBox.setSelected(false);
			return;
		}
		if (arg0.getActionCommand() == "V") {
			sortByColor = true;
			hori = false;
			paletteBox.setSelected(false);
			return;
		}
		if (arg0.getActionCommand() == "Subgraph") {
			drawGraph();
			return;
		}
		palette = paletteBox.isSelected();
		if (!numEdgesToRemoveBox.getText().isEmpty()) try {
			numEdgesToRemove = Integer.parseInt(numEdgesToRemoveBox.getText());
		} catch (NumberFormatException e) {
			numEdgesToRemove = -1;
		}
		panel.dispose();
		mainPart();
	}
}

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
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
	
	Hashtable<GraphNode,Integer> nodesNewColors = new Hashtable<GraphNode,Integer>();
	HashSet<Pair<Integer>> colorCombis = new HashSet<Pair<Integer>>();
	Hashtable<Integer,Integer> positions = new Hashtable<Integer,Integer>();
	
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
		
		// Record nodesSavedColors for reverting
		Enumeration<GraphNode> nodeList  = nodes.elements();
		while (nodeList.hasMoreElements()) {
			GraphNode node = nodeList.nextElement();
			Color oldColor = node.getColor();
			int id = node.getID();
			nodesSavedColors.put(id,oldColor);
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

			// Create lines (= hori ? columns : rows) by color
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
					z = ortho * colorCount/nodes.size();		// scaled by average line size
					first  = false;
				}
				xy = hori ? new Point(xGap * colorId, z) : new Point(z, yGap * colorId);
				node.setXY(xy);
				z += increment;
			}
		}
		if (sortByColor) arrangeLines(colorCount);
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
		
		// Initialize column positions
		
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
//						System.out.println("Swapping " + positions.get(source) + " and " + positions.get(target));
						success++;
					}
				}
			}
			System.out.println("Iteration number " + iteration++ + " ...");
		}
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
	
	public void revertColors() {
		Iterator<GraphNode> iter = paletteNodes.iterator();
		while (iter.hasNext()) controler.deleteNode(iter.next(), true);
		
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

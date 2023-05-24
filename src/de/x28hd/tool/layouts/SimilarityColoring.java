package de.x28hd.tool.layouts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class SimilarityColoring implements ActionListener {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	PresentationService controler;
	Hashtable<Integer, Color> nodesSavedColors = new Hashtable<Integer, Color>();

	Hashtable<Integer,String> dic = new Hashtable<Integer,String>();
	Hashtable<Integer,GraphEdge> edgeLookup = new Hashtable<Integer,GraphEdge>();
	
	JSlider slider;
	JDialog panel;
	JCheckBox paletteBox;
	boolean palette = false;
	HashSet<GraphNode> paletteNodes = new HashSet<GraphNode>();
	JTextField numEdgesToRemoveBox;
	int numEdgesToRemove = -1;

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
//			dic.put(n1, nodes.get(n1).getLabel());
			int n2 = edge.getN2();
			if (!nodes.containsKey(n2)) continue;
//			dic.put(n2, nodes.get(n2).getLabel());
			g.addEdge(edgeID, n1, n2, edgeType);
//			edgeLookup.put(edgeID, edge);
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
			Iterator<Integer> inner = set.iterator();
			while (inner.hasNext()) {
				int n = inner.next();
				GraphNode node = nodes.get(n);
				Color originalColor = node.getColor();
				nodesSavedColors.put(node.getID(), originalColor);

				node.setColor(colorString);

			}
		}
	}

	public void createSlider() {
		slider = new JSlider(JSlider.VERTICAL);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setMajorTickSpacing(10);
		slider.setValue((int) Math.round((edges.size() 	// dense network need higher value
				- nodes.size() + .0001)/edges.size() * 100)); 
		
		panel = new JDialog(controler.getMainWindow(), "Few or many groups?", true);
		panel.setMinimumSize(new Dimension(200, 500));
		panel.setLocation(200, 200);
		panel.setLayout(new BorderLayout());
		slider.setLabelTable(slider.createStandardLabels(10));
		panel.add(slider);
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());
		bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
		JPanel advanced = new JPanel();
		advanced.add(new JLabel("Advanced"));
		numEdgesToRemoveBox = new JTextField(4); 	// numEdgesToRemove 
		numEdgesToRemoveBox.setToolTipText("Set 'numEdgesToRemove' variable directly");
		advanced.add(numEdgesToRemoveBox);
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

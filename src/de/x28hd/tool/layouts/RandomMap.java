package de.x28hd.tool.layouts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class RandomMap implements ActionListener {
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	PresentationService controler;
	String dataString = "";
	int edgesNum = 0;
	Random random;
	int total = 100;
	JSlider slider;
	JDialog panel;
	JCheckBox colorBox;
	JCheckBox loopsBox;
	JCheckBox planarityBox;
	boolean planarity = false;
	JCheckBox shuffleBox;

	public RandomMap(PresentationService controler) {
		this.controler = controler;
		
		random = new Random((new Date()).getTime() + 8432570);
		createSlider();		// after user response, mainPart() is called or new Planarity()
	}
	
	public void mainPart() {
		for (int i = 0; i < total; i++) {
			int x = (int) (random.nextDouble() * 800);
			int y = (int) (random.nextDouble() * 600);
			GraphNode node = new GraphNode(i, new Point(x, y), Color.decode("#ccdddd"), i + "", "");
			nodes.put(i,  node);
		}
		
		for (int i = 0; i < total; i++) {
			GraphNode node = nodes.get(i);
			boolean found = false;
			while (!found) {
				int otherID = (int) (random.nextDouble() * total);
				if (otherID == i) continue;
				GraphNode otherNode = nodes.get(otherID);
				GraphEdge edge = new GraphEdge(edgesNum, node, otherNode, Color.decode("#c0c0c0"), "");
				edges.put(edgesNum,  edge);
				edgesNum++;
				node.setDetail("<a href=\"#" + otherID + "\">" + otherID + "</a>");	// hyperhopping just for fun
				if (!loopsBox.isSelected() || random.nextDouble() > .05) found = true;
			}
		}
		
		// pass on
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error RM108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error RM109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error RM110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.getControlerExtras().toggleHashes(true);
	}
	
	public void createSlider() {
		slider = new JSlider(JSlider.VERTICAL);
		slider.setValue(40);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setMaximum(125);
		slider.setMinimum(20);
		slider.setMajorTickSpacing(10);
		Hashtable<Integer,JComponent> labelDict = new Hashtable<Integer,JComponent>();
		slider.createStandardLabels(125);
		for (int i = 12; i > 1; i--) {
//			labelDict.put(i * 10, (JComponent) new JLabel((int) Math.pow(i, 2.5) + " items"));
//			labelDict.put(i * 10, (JComponent) new JLabel((i + 2) * (i + 1) / 2 + " items"));
			labelDict.put(i * 10, (JComponent) new JLabel("Level " + (i - 1)));
//			System.out.println((int) Math.pow(i, 2.5) + " / " + ((i * 3) - 2) * ((i * 3) - 3) / 2);
  		}
		panel = new JDialog(controler.getMainWindow(), "How many items?", true);
		panel.setMinimumSize(new Dimension(200, 500));
		panel.setLocation(200, 200);
		panel.setLayout(new BorderLayout());
		
		slider.setLabelTable((Dictionary<Integer,JComponent>) labelDict);
		panel.add(slider);
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());
		bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		JPanel options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
		colorBox = new JCheckBox(" Coloring for ease?", true);
		options.add(colorBox);
		loopsBox = new JCheckBox(" More loops?", false);
		options.add(loopsBox);
		shuffleBox = new JCheckBox(" Shuffled?");
		shuffleBox.setSelected(true);
		shuffleBox.setEnabled(false);
		options.add(shuffleBox);
		planarityBox = new JCheckBox(" Like planarity.net ?");
		planarityBox.setToolTipText("Crossings resolve; Algorithm by J. Tantalo");
		planarityBox.setActionCommand("planarity");
		planarityBox.addActionListener(this);
		options.add(planarityBox);
		bottom.add(options, "North");
		
		JButton continueButton = new JButton("OK");
		continueButton.addActionListener(this);
		continueButton.setSelected(true);
		bottom.add(continueButton, "East");

		panel.add(bottom, "South");

		panel.setVisible(true);
	}
	
	public boolean triggerColoring() {
		return colorBox.isSelected();
	}
	
	public class Planarity {

		// Using the Pseudocode http://johntantalo.com/wiki/Planarity/ by John Tantalo
		
		// Input "list L": l
		TreeMap<Integer, Pair<Double>> l = new TreeMap<Integer, Pair<Double>>();
		GraphNode uNode;
		GraphNode vNode;
		
		Random random;
		int n;
		int size;
		boolean shuffled;
		
		// Condensr specific
		Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
		Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
		String dataString = "";
		int id = 0;
		int edgesNum = 0;
		
		// "Graph G": g
		UndirectedSparseGraph<Integer, Integer> g = new UndirectedSparseGraph<Integer,Integer>();
		EdgeType edgeType = EdgeType.UNDIRECTED; 
		CircleLayout<Integer,Integer> layout = new CircleLayout<Integer,Integer>(g);

		
		public Planarity(int n, boolean shuffled) {
			this.n = n;
			this.shuffled = shuffled;
			
			random = new Random((new Date()).getTime() + 8432570);

			// "Labeling A" of line p or q: ap or aq
			for (int ap = 0; ap < n; ap++) {
				double a = random.nextDouble() * 600 - 300;
				double b = random.nextDouble() * 600 - 300;
				Pair<Double> line = new Pair<Double>(a, b);
				l.put(ap + 1, line);
			}

			// Avoid parallel lines
			for (int ap = 2; ap <= n; ap++) {
				Pair<Double> lineP = l.get(ap);
				double a1 = lineP.getFirst();
				
				for (int aq = 1; aq < ap; aq++) {
					Pair<Double> lineQ = l.get(aq);
					double a2 = lineQ.getFirst();
					double b2 = lineQ.getSecond();
					
					while (a1 == a2) {
						System.out.println("Parallel");
						a2 = random.nextDouble() * 600 - 300;
						lineQ = new Pair<Double>(a2, b2);
					}
					
					// Assign an intersection node
					id++;
					GraphNode v = new GraphNode(id, new Point(0, 0), Color.decode("#0033ff"), "", "");
					nodes.put(id, v);
				}
			}
			
			// "For each line p in L: 
			// Let M be the lines q in L ordered by the intersection points of p"

			for (int ap = 1; ap <= n; ap++) {
				Pair<Double> lineP = l.get(ap);
				double a1 = lineP.getFirst();
				double b1 = lineP.getSecond();
				
				// Creating "M": m
				// (Double is y-coordinate of intersection point,
				// Integer is lookup for line q)
				
				TreeMap<Double, Integer> mLookups = new TreeMap<Double, Integer>();
				SortedMap<Double, Integer> m = (SortedMap<Double, Integer>) mLookups;
				
				for (int aq = 1; aq <= n; aq++) {
					if (ap == aq) continue;
					Pair<Double> lineQ = l.get(aq);
					double a2 = lineQ.getFirst();
					double b2 = lineQ.getSecond();
					double x = (b2 - b1) / (a1 - a2); 		// solving y = a1 * x + b1  &&  y = a2 * x + b2
					double y = a1 * x + b1;
					mLookups.put(y, aq);
				}
				
				// Create nodes u and v
				
				SortedSet<Double> mSet = (SortedSet<Double>) m.keySet();
				Iterator<Double> iter = mSet.iterator();
				boolean first = true;
				while(iter.hasNext()) {
					double y = iter.next();
					int aq = m.get(y);
					int p = Math.min(ap, aq);
					int q = Math.max(ap, aq);
					
					// "Pair index" formula
					int v = (p - 1) * (2 * n - p)/2 + q - p;
					
					vNode = nodes.get(v);
					if (first) {
						uNode = vNode;
						first = false;
						continue;
					}
					
					// Create edge
					edgesNum++;
					GraphEdge edge = new GraphEdge(edgesNum, uNode, vNode, Color.decode("#555555"), "");
					edges.put(edgesNum, edge);
					uNode = vNode;
				}
			}
			
			initializeGraph();
			size = g.getVertexCount();
			if (shuffled) {
				shuffleLayout(); 
			} else {
				circleLayout();
			}

			// Add credit
			int id = nodes.size() + 1;
			nodes.put(id, new GraphNode(id, new Point(40, 20), Color.decode("#eeeeee"), "Source", 
					"Adapted from <a href=\"http://johntantalo.com/wiki/Planarity/\">"
					+ "Planarity's Algorithm</a> by John Tantalo "));

			// pass on
	    	try {
	    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
	    	} catch (TransformerConfigurationException e1) {
	    		System.out.println("Error PL108 " + e1);
	    	} catch (IOException e1) {
	    		System.out.println("Error PL109 " + e1);
	    	} catch (SAXException e1) {
	    		System.out.println("Error PL110 " + e1);
	    	}
	    	controler.getNSInstance().setInput(dataString, 2);
	    	controler.getControlerExtras().toggleHashes(true);

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
				g.addEdge(edgeID, n1, n2, edgeType);
			}
		}
		
		public void circleLayout() {
			layout = new CircleLayout<Integer,Integer>(g);
			Vector<Integer> vertexOrder = new Vector<Integer>();
			vertexOrder.addAll(nodes.keySet());
			Collections.shuffle(vertexOrder);		// still a tidy circle
			layout.setVertexOrder(vertexOrder); 
			layout.initialize();
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			layout.setSize(new Dimension(dim.width- 40, dim.height - 120));

			Collection<Integer> nodeIDs = g.getVertices();
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
		
		public void shuffleLayout() {
			Enumeration<GraphNode> nodesList = nodes.elements();
			while (nodesList.hasMoreElements()) {
				int x = (int) (random.nextDouble() * 800);
				int y = (int) (random.nextDouble() * 600);
				GraphNode node = nodesList.nextElement();
				node.setXY(new Point(x, y));
			}

		}
	}

	public void actionPerformed(ActionEvent arg0) {
		planarity = planarityBox.isSelected();
		
		// Enforce dependencies
		if (arg0.getActionCommand() == "planarity") {
			if (planarity) {
				colorBox.setSelected(false);
				colorBox.setEnabled(false);
				loopsBox.setSelected(false);
				loopsBox.setEnabled(false);
				shuffleBox.setSelected(true);
				shuffleBox.setEnabled(true);
				return;
			} else {
				shuffleBox.setSelected(true);
				shuffleBox.setEnabled(false);
				colorBox.setSelected(true);
				colorBox.setEnabled(true);
				loopsBox.setSelected(false);
				loopsBox.setEnabled(true);
				return;
			}
		}
		if (planarity) {
			total = (int) slider.getValue()/10 + 2;
			new Planarity(total, shuffleBox.isSelected());
		} else {
			total = (int) Math.pow(slider.getValue()/10., 2.5);
			mainPart();
		}
		System.out.println(total + " items wanted");
		panel.dispose();
	}
}

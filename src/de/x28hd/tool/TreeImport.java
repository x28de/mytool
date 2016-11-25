package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TreeImport implements ActionListener {
	
	// Main fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	
	// Auxiliary stuff
	Hashtable<String,String> inputItems = new Hashtable<String,String>();
	Hashtable<String,String> relationships = new Hashtable<String,String>();
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	HashSet<GraphEdge> nonTreeEdges = new HashSet<GraphEdge>();
	
	JTree tree;
	JFrame frame;
	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			System.out.println("TI tmp: JTree closed");
		}
	};
	
	int j = -1;
	int readCount = 0;
	int edgesNum = 0;
	boolean top = true;
	String htmlOut = "";
	JPanel radioPanel = null;
	
	//	Constants
	int maxVert = 10;
	
	//	Overriden later
	int knownFormat = 12;	//	OPML
	String topNode = "body";
	String nestNode = "outline";
	String labelAttr = "text";
	String idAttr = "";
	
	public TreeImport(Document inputXml, GraphPanelControler controler, int knownFormat) {

		this.knownFormat = knownFormat;
		if (knownFormat == 11) {	//	FreeMind
			topNode = "map";
			nestNode = "node";
			labelAttr = "TEXT";
			idAttr = "ID";
		}
		NodeList graphContainer = inputXml.getElementsByTagName(topNode);
		inputItems.put(topNode, "ROOT");
		addNode(topNode, "");
		Element graph = (Element) graphContainer.item(0);
//		edges.clear();
		
		int idForJTree = inputID2num.get(topNode);
	    DefaultMutableTreeNode top = 
	    		new DefaultMutableTreeNode(new BranchInfo(idForJTree, " "));
		
		//	Collect nested nodes
		nest(graph, topNode, top);
		
		//	Collect relationships
		Enumeration<String> relEnum = relationships.keys();
		while (relEnum.hasMoreElements()) {
			String fromRef = relEnum.nextElement();
			String toRef = relationships.get(fromRef);
			addEdge(fromRef, toRef, true);
		}
		
//
//		Create a JTree here
	    
	    DefaultTreeModel model = new DefaultTreeModel(top);
	    controler.setTreeModel(model);
//	    controler.setNonTreeEdges(nonTreeEdges);
	    model = null;
	    model = controler.getTreeModel();
		
//		Temporarily show it here
	    
	    tree = new JTree(model);
	    
	    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
//	    tree.addTreeSelectionListener(this);
	    
        frame = new JFrame("Using this tree structure");
        frame.setLocation(100, 170);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);	// closing triggers further processing
		frame.addWindowListener(myWindowAdapter);
		frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(tree));

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BorderLayout());
//        JButton continueButton = new JButton("No");
//        continueButton.addActionListener(this);
//        continueButton.setSelected(true);
//		JButton cancelButton = new JButton("Yes");
//	    cancelButton.addActionListener(this);
//	    
//	    String okLocation = "East";
//	    String cancelLocation = "West";
//	    String multSel = "CTRL";
//		if (System.getProperty("os.name").equals("Mac OS X")) {
//	    	okLocation = "East";
//	    	cancelLocation = "West";
//	    	multSel = "CMD";
//	    }
		ButtonGroup buttonGroup = new ButtonGroup();
        radioPanel = new JPanel(new GridLayout(0, 1));
		radioPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		JLabel instruction = new JLabel("<html><body>" +
	    "You may use this tree structure for re-exorting \n"
				+ "and use the map for nothing else.");
		JRadioButton radio1 = new JRadioButton("Yes, just for exporting");
		radio1.setActionCommand("export");
		radio1.addActionListener(this);
		buttonGroup.add(radio1);
        radioPanel.add(radio1);
		JRadioButton radio2 = new JRadioButton("No, continue.");
		radio2.setActionCommand("normal");
		radio2.setSelected(true);
		radio2.addActionListener(this);
		buttonGroup.add(radio2);
        radioPanel.add(radio2);
        toolbar.add(radioPanel);
		
		toolbar.add(instruction, "North");
		toolbar.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.add(toolbar,"South");
        frame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
//        frame.setMinimumSize(new Dimension(400, 300));
        frame.setMinimumSize(new Dimension(596, 418));

//        frame.setVisible(true);
		
//		
//	Pass on the new map
	
	System.out.println("TI Map: " + nodes.size() + " " + edges.size());
	try {
		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
	} catch (TransformerConfigurationException e1) {
		System.out.println("Error TI108 " + e1);
	} catch (IOException e1) {
		System.out.println("Error TI109 " + e1);
	} catch (SAXException e1) {
		System.out.println("Error TI110 " + e1);
	}
	
	controler.getNSInstance().setInput(dataString, 2);
//    controler.setNonTreeEdges(nonTreeEdges);
//	controler.replaceByTree(nodes, edges);
	}
	
	public void nest(Node parent, String parentID, DefaultMutableTreeNode parentInTree) {
		
		NodeList children = parent.getChildNodes();
		Node child;
		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			String name = child.getNodeName();
			if (!name.equals(nestNode)) {
				continue;
			}
			
			//	Extract stuff 
			String label = ((Element) child).getAttribute(labelAttr);
			String detail = "";
			String id = "";
			if (knownFormat == 11) {	//	FreeMind
				id = ((Element) child).getAttribute(idAttr);

				//	Notes
				NodeList richContainer = ((Element) child).getElementsByTagName("richcontent");
				if (richContainer.getLength() > 0) detail = richContainer.item(0).getTextContent();
				if (label.isEmpty()) {
					label = filterHTML(detail);
					int len = label.length();
					if (len > 30) label = label.substring(0, 29) + "...";
				}
				
				//	Arrows 
				NodeList arrowCandidates = child.getChildNodes();
				for (int k = 0; k < arrowCandidates.getLength(); k++) {
					Node arrowCandidate = arrowCandidates.item(k);
					String nodeName = arrowCandidate.getNodeName();
					if (nodeName.equals("arrowlink")) {
						String relDesti = ((Element) arrowCandidate).getAttribute("DESTINATION");
						relationships.put(id, relDesti);
					}
				}
			} else {
				id = readCount++ + "";
				detail = ((Element) child).getAttribute("_note");
				detail = detail.replace("\n", " X<br />");
			}
			
			//	add node
			inputItems.put(id, label);
			addNode(id, detail);
			DefaultMutableTreeNode branch = 
					new DefaultMutableTreeNode(new BranchInfo(inputID2num.get(id), label));
            parentInTree.add(branch);
			
			//	add link
			addEdge(parentID, id, false);
			
			//	recurse
			nest(child, id, branch);
		}
	}
		
	public void addNode(String nodeRef, String detail) { 
		j++;
		String newNodeColor;
		String newLine = "\r";
		String topicName = ""; 
		topicName = inputItems.get(nodeRef);
		newNodeColor = "#ccdddd";
		String verbal = detail;
		topicName = topicName.replace("\r"," ");
		if (topicName.equals(newLine)) topicName = "";
		if (verbal == null || verbal.equals(newLine)) verbal = "";
		if (topicName.isEmpty() && verbal.isEmpty()) return;
		int id = 100 + j;

		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		inputID2num.put(nodeRef, id);
	}

	public void addEdge(String fromRef, String toRef, boolean xref) {
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
		if (xref) newEdgeColor = "#ffff00";
		edgesNum++;
		if (!inputID2num.containsKey(fromRef)) {
			System.out.println("Error TI101 " + fromRef + ", " + xref);
			return;
		}
		GraphNode sourceNode = nodes.get(inputID2num.get(fromRef));
		if (!inputID2num.containsKey(toRef)) {
			System.out.println("Error TI102 " + toRef);
			return;
		}
		GraphNode targetNode = nodes.get(inputID2num.get(toRef));
		edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), "");
		edges.put(edgesNum, edge);
		if (xref) nonTreeEdges.add(edge);
	}
	
//
//	Accessories to eliminate HTML tags 
//	Duplicate of NewStuff TODO reuse

	private String filterHTML(String html) {
		htmlOut = "";
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				htmlOut = htmlOut + dataString + " ";
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error IM109 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error IM110 " + e3.toString());
		}
		return htmlOut;
	}

	private static class MyHTMLEditorKit extends HTMLEditorKit {
		private static final long serialVersionUID = 7279700400657879527L;

		public Parser getParser() {
			return super.getParser();
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}

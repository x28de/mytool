package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ZknImport implements ActionListener {
	
	// Main fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	
	// Auxiliary stuff
	Hashtable<String,String> childlists = new Hashtable<String,String>();
	Hashtable<String,String> linklists = new Hashtable<String,String>();
	Hashtable<String,String> parents = new Hashtable<String,String>();
	Hashtable<Integer,String> relationshipFrom = new Hashtable<Integer,String>();
	Hashtable<Integer,String> relationshipTo = new Hashtable<Integer,String>();
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>(); // TODO rename
	HashSet<GraphEdge> nonTreeEdges = new HashSet<GraphEdge>();
//	Hashtable<String,String> kwlists = new Hashtable<String,String>();
	Hashtable<String,String> keywords = new Hashtable<String,String>();
	HashSet<String> usedKw = new HashSet<String>();
	Hashtable<Integer,Integer> edgeColors = new Hashtable<Integer,Integer>();
	
	GraphPanelControler controler;
	
	String topNode = "zettelkasten";
	String idAttr = "zknid";
	
	int x;
	int y;
	int maxVert = 10;
	int j = -1;
	int edgesNum = 0;
	int relID = -1;
	String[] colors = {"#ff0000", "#ffaa00", "#ffff00", "#00ff00", "#0000ff", "#b200b2"};
	String[] colors2 = {"#ffbbbb", "#ffe8aa", "#ffff99", "#bbffbb", "#bbbbff", "#d2bbd2",
			"#f0f0f0"};
	
	JTree tree;
	JFrame frame;
	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			finish();
		}
	};
	boolean transit = false;
	JCheckBox transitBox = null;
	boolean layoutOpt = true;
	JCheckBox layoutBox = null;
	
	public ZknImport(File file, GraphPanelControler controler) {
		Charset CP850 = Charset.forName("CP850");
		ZipFile zfile = null;
		try {
			zfile = new ZipFile(file,CP850);
			final ZipEntry kwEntry = zfile.getEntry("keywordFile.xml");
			if (kwEntry != null) {
				InputStream stream = zfile.getInputStream(kwEntry);
				loadKeywords(stream);
			}
			final ZipEntry zknEntry = zfile.getEntry("zknFile.xml");
			if (zknEntry != null) {
				InputStream stream = zfile.getInputStream(zknEntry);
//				new ZknImport(stream, controler);
				zknImport(stream, controler);
			} else {
				controler.displayPopup("Import failed, file(s) not found in Zip");
			}
		} catch (IOException e1) {
			System.out.println("Error ZI111 " + e1);
		}
	}
	public void zknImport(InputStream stream, GraphPanelControler controler) {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error ZI102 " + e2 );
		}
		
		try {
			inputXml = db.parse(stream);
			
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != topNode) {
				System.out.println("Error ZI105, unexpected: " + inputRoot.getTagName() );
				stream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error ZI106 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error ZI107 " + e );
		}
//		new ZknImport(inputXml, controler, 13);
//	}
//		
//	
//	public ZknImport(Document inputXml, GraphPanelControler controler, int knownFormat) {
		this.controler = controler;
//
//		Read nodes
	    
		NodeList graphContainer = inputXml.getElementsByTagName(topNode);
		Element graph = (Element) graphContainer.item(0);
		NodeList zettels = graph.getElementsByTagName("zettel");

		for (int zettelNumber = 1; zettelNumber <= zettels.getLength(); zettelNumber++) {
			Node zettelNode = zettels.item(zettelNumber - 1);
			
			//	Extract stuff 
			
			String id = ((Element) zettelNode).getAttribute(idAttr);
			
			String label = "";
			NodeList labelContainer = ((Element) zettelNode).getElementsByTagName("title");
			if (labelContainer.getLength() > 0) label = labelContainer.item(0).getTextContent();
			
			String detail = "";
			NodeList detailContainer = ((Element) zettelNode).getElementsByTagName("content");
			if (detailContainer.getLength() > 0) detail = detailContainer.item(0).getTextContent();
			detail = detail.replaceAll("\\[br\\]", "<br />");
			
			addNode(zettelNumber, id, label, detail, false);

			//	Record the "luhmann" hierarchy

			NodeList childlistContainer = ((Element) zettelNode).getElementsByTagName("luhmann");
			Node childlistNode = childlistContainer.item(0);
			String childlist = childlistNode.getTextContent();
			childlists.put(id, childlist);
			if (!childlist.isEmpty()) {
				String [] children = childlist.split(",");
				for (int c = 0; c < children.length; c++) {
					parents.put(children[c], id);
				}
			}
			
			//	Record the xref links

			NodeList linklistContainer = ((Element) zettelNode).getElementsByTagName("manlinks");
			Node linklistNode = linklistContainer.item(0);
			String linklist = linklistNode.getTextContent();
			linklists.put(id, linklist);
			if (!linklist.isEmpty()) {
				String [] linkTargets = linklist.split(",");
				for (int c = 0; c < linkTargets.length; c++) {
					relID++;
					relationshipTo.put(relID, linkTargets[c]);
					relationshipFrom.put(relID, id);
				}
			}
			
			//	Record the keywords

			NodeList kwlistContainer = ((Element) zettelNode).getElementsByTagName("keywords");
			Node kwlistNode = kwlistContainer.item(0);
			String kwlist = kwlistNode.getTextContent();
//			kwlists.put(id, kwlist);
			if (!kwlist.isEmpty()) {
				String [] kwIDs = kwlist.split(",");
				for (int c = 0; c < kwIDs.length; c++) {
//					if (!usedKw.add(kwIDs[c])) System.out.println("Duplicate " + kwIDs[c]);
					relID++;
					relationshipTo.put(relID, "kw-" + kwIDs[c]);
					relationshipFrom.put(relID, id);
				}
			}
		}
		
//		
//		Build nested nodes
		
		DefaultMutableTreeNode top = 
				new DefaultMutableTreeNode(new BranchInfo(-1, " "));

		//	Tree tops
		Enumeration<String> childInfos = childlists.keys();
		while (childInfos.hasMoreElements()) {
			String id = childInfos.nextElement();
			if (parents.containsKey(id)) continue;
			String label = "";
			int topicID = inputID2num.get(id);
			GraphNode topic = nodes.get(topicID);
			label = topic.getLabel();
			DefaultMutableTreeNode treeNode = 
					new DefaultMutableTreeNode(new BranchInfo(topicID, label));
			String childlist = childlists.get(id);
			if (!childlist.isEmpty()) {
				nest(id, "-1", top, 0);
			} else {
				top.add(treeNode);
			}
		}

//
//		Create keyword nodes on graph
		
		Iterator<String> kwIx = usedKw.iterator();
		int j2 = nodes.size();
		while (kwIx.hasNext()) {
			j2++;
			String id = kwIx.next();
			String label = keywords.get(id);
//			System.out.println("kw " + id + " of " + keywords.size() + " , label = " + label);
			addNode(j2, "kw-" + id, label, "", true);
		}
		
//		
//		Process xref link relationships
		
		Enumeration<Integer> relEnum = relationshipFrom.keys();
		while (relEnum.hasMoreElements()) {
			int relID = relEnum.nextElement();
			String fromRef = relationshipFrom.get(relID);
			String toRef = relationshipTo.get(relID);
			addEdge(fromRef, toRef, true);
		}
		
//
//		Create a JTree 
	    
	    DefaultTreeModel model = new DefaultTreeModel(top);
	    controler.getControlerExtras().setTreeModel(model);
		
	    tree = new JTree(model);
	    
	    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	    
        frame = new JFrame("Found this tree structure:");
        frame.setLocation(100, 170);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(myWindowAdapter);
		frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(tree));

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BorderLayout());
		toolbar.setBorder(new EmptyBorder(10, 10, 10, 10));
		JLabel instruction = new JLabel("<html><body>" 
				+ "You may use this tree structure for re-exporting \n"
				+ "if you use the map for nothing else:</body></html>");
		toolbar.add(instruction, "North");
        JPanel buttons = new JPanel();
        buttons.setLayout(new BorderLayout());
        JButton continueButton = new JButton("Continue");
        continueButton.addActionListener(this);
        continueButton.setSelected(true);
		JButton cancelButton = new JButton("Cancel");
	    cancelButton.addActionListener(this);
		layoutBox = new JCheckBox ("Tree layout           ", true);
		buttons.add(layoutBox, "West");
		buttons.add(cancelButton, "Center");
        buttons.add(continueButton, "East");
		toolbar.add(buttons,"East");
		transitBox = new JCheckBox ("Just for re-export", false);
		transitBox.setActionCommand("transit");
		transitBox.addActionListener(this);
		toolbar.add(transitBox, "West");

		frame.add(toolbar,"South");
        frame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
        frame.setMinimumSize(new Dimension(596, 418));

        frame.setVisible(true);
        controler.getControlerExtras().stopHint();
        //	Continues with finish() after acterPerformed
	}
	
//
//	Add node
	
	public void addNode(int myZettel, String inputID, String label, String detail, boolean kw) {
		int j = myZettel - 1;
		String newNodeColor = "#ccdddd";
		if (kw) newNodeColor = "#ffff99";
		String topicName = label;
		String verbal = detail;
		int id = 101 + j;
		
		y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		inputID2num.put(inputID, id);
	}
	
//
//	Descend the tree
	
	public void nest(String myID, String parentID, DefaultMutableTreeNode parentInTree,
			int level) {
		
		String label = "";
		int topicID = -2;
		if (inputID2num.containsKey(myID)) {
			topicID = inputID2num.get(myID);
		} else {
			System.out.println("Error ZI123 " + myID + " bad");
			return;
		}
		GraphNode topic = nodes.get(topicID);
		label = topic.getLabel();
		BranchInfo info = new BranchInfo(topicID, label);
		DefaultMutableTreeNode child = new DefaultMutableTreeNode(info);
		parentInTree.add(child);
		if (inputID2num.containsKey(parentID)) {
			int otherEndID = inputID2num.get(parentID);
			GraphNode otherEnd = nodes.get(otherEndID);
			String newEdgeColor = "#c0c0c0";
			edgeColors.put(edgesNum,  level % 6);
			GraphEdge edge = new GraphEdge(edgesNum, topic, otherEnd, Color.decode(newEdgeColor), "");
			edges.put(edgesNum, edge);
			edgesNum++;
		}
		
		String childlist = childlists.get(myID);
		if (!childlist.isEmpty()) {
			String [] children = childlist.split(",");
			for (int c = 0; c < children.length; c++) {
				String grandChild = children[c];
				nest(grandChild, myID, child, level + 1);
			}
		}
	}
	
//
//	Add xref edge (see nest for tree edges)
	
	public void addEdge(String fromRef, String toRef, boolean xref) {
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
//		if (xref) newEdgeColor = "#ffff00";
		edgesNum++;
		if (!inputID2num.containsKey(fromRef)) {
			System.out.println("Error ZI111 " + fromRef + ", " + xref);
			return;
		}
		GraphNode sourceNode = nodes.get(inputID2num.get(fromRef));
		if (!inputID2num.containsKey(toRef)) {
			System.out.println("Error ZI112 " + toRef);
			return;
		}
		if (xref) {
			newEdgeColor = "#ffff00";
			edgeColors.put(edgesNum, 6);
		}
		GraphNode targetNode = nodes.get(inputID2num.get(toRef));
		edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), "");
		edges.put(edgesNum, edge);
		if (xref) nonTreeEdges.add(edge);
	}
	
//	
//	Pass on the new map

	public void finish() {
		if (layoutOpt) {
			// no tree, force a circle
			if (nonTreeEdges.size() == edges.size()) {
				HashSet<GraphEdge> extTreeEdges = new HashSet<GraphEdge>();
				HashSet<GraphNode> extTreeNodes = new HashSet<GraphNode>();
				GraphNode center = new GraphNode(j + 1, new Point(0, 0), Color.decode("#c0c0c0"), "", "");
				extTreeNodes.add(center);
				Enumeration<GraphNode> allNodes = nodes.elements();
				while (allNodes.hasMoreElements()) {
					edgesNum++;
					GraphNode node = allNodes.nextElement();
					GraphEdge edge = new GraphEdge(edgesNum, node, center, Color.decode("#f0f0f0"), "");
					edges.put(edgesNum, edge);
					extTreeEdges.add(edge);
				}
				nodes.put(j + 1, center);
				CentralityColoring centralityColoring = new CentralityColoring(nodes, edges);
				centralityColoring.treeLayout(nonTreeEdges, true);
				Iterator<GraphEdge> extTreeIter = extTreeEdges.iterator();
				while (extTreeIter.hasNext()) {
					GraphEdge edge = extTreeIter.next();
					int id = edge.getID();
					edges.remove(id);
				}
				Iterator<GraphNode> extTreeIter2 = extTreeNodes.iterator();
				while (extTreeIter2.hasNext()) {
					GraphNode node = extTreeIter2.next();
					int id = node.getID();
					nodes.remove(id);
				}
			} else {
			CentralityColoring centralityColoring = new CentralityColoring(nodes, edges);
			centralityColoring.treeLayout(nonTreeEdges, true);
			}
			
			// recolor
			Enumeration<Integer> edgeColEnum = edgeColors.keys();
			while (edgeColEnum.hasMoreElements()) {
				int key = edgeColEnum.nextElement();
				int treeColor = edgeColors.get(key);
				GraphEdge edge = edges.get(key);
				edge.setColor(colors2[treeColor]);
				GraphNode node1 = edge.getNode1();
				if (treeColor < 6) node1.setColor(colors[treeColor]);
			}
		}
		if (transit) { 
			controler.getControlerExtras().setNonTreeEdges(nonTreeEdges);
			controler.getControlerExtras().replaceByTree(nodes, edges);
		} else {
			try {
				dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
			} catch (TransformerConfigurationException e1) {
				System.out.println("Error ZI108 " + e1);
			} catch (IOException e1) {
				System.out.println("Error ZI109 " + e1);
			} catch (SAXException e1) {
				System.out.println("Error ZI110 " + e1);
			}
			controler.getNSInstance().setInput(dataString, 2);
	    	controler.getControlerExtras().setTreeModel(null);
	    	controler.getControlerExtras().setNonTreeEdges(null);
		} 
	}

//
//	Accessory for import dialog
	
	public void actionPerformed(ActionEvent arg0) {
		String command = arg0.getActionCommand();
		if (command == "Cancel") {
			transit = false;
		} else if (command == "Continue") {
			transit = transitBox.isSelected();
		} else if (command == "transit") {
			transit = transitBox.isSelected();
			layoutBox.setEnabled(!transit);
			layoutBox.setSelected(!transit);
			return;
		}
		layoutOpt = layoutBox.isSelected();
        frame.setVisible(false);
        frame.dispose();
        finish();
	}
	
//
//	Accessory for keywords
	
	public void loadKeywords(InputStream kwstream) {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document kwXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error ZI122 " + e2 );
		}

		Element inputRoot = null;
		try {
			kwXml = db.parse(kwstream);
//			Element inputRoot = null;
			inputRoot = kwXml.getDocumentElement();
			if (inputRoot.getTagName() != "keywords") {
				System.out.println("Error ZI125, unexpected: " + inputRoot.getTagName() );
				kwstream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error ZI126 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error ZI127 " + e );
		}
		
		NodeList kws = inputRoot.getElementsByTagName("entry");
		int kwCount = kws.getLength();
		if (kwCount <= 0) return;
		for (int k = 0; k < kwCount; k++) {
			Node entry = kws.item(k);
			Element el = (Element) entry;
			String id = el.getAttribute("keywid");
			if (id.isEmpty()) continue;
			id = id.substring(0, 1);
			String label = el.getTextContent();
			keywords.put(id, label);
			System.out.println(id + " " + label);
		}
	}
}

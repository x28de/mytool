package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DwzImport  implements TreeSelectionListener, ActionListener {
	
	//	Major fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	
	//	Recording the hierarchy
	Hashtable<String,String> parents = new Hashtable<String,String>();
	Hashtable<String,String> parents0 = new Hashtable<String,String>();
	Hashtable<String,String> parents2 = new Hashtable<String,String>();
	Hashtable<String,String> inverses = new Hashtable<String,String>();
	TreeSet<String> hierarchicalRelations = new TreeSet<String>();
	TreeSet<String> expandableRelations = new TreeSet<String>();
	Hashtable<String,Boolean> topDown = new Hashtable<String,Boolean>();
	
	//	From DWZ 
	private static final String XML_ROOT = "kgif";
	NodeList dwzNodes = null;
	NodeList dwzLinks = null;
	Hashtable<String,Integer> dwz2index = new  Hashtable<String,Integer>();
	Hashtable<String,String> typeDict = new Hashtable<String,String>();
	
	//	For selection tree
	JTree tree;
	JFrame frame;
	Hashtable<String,DefaultMutableTreeNode> treeNodes = new Hashtable<String,DefaultMutableTreeNode>();
	Hashtable<String,Boolean> selected = new Hashtable<String,Boolean>();
	boolean noSelectionMade = true;
	
	//	For the map
	Hashtable<String,Integer> dwz2num = new  Hashtable<String,Integer>();
	int j = -1;
	int maxVert = 10;
	int edgesNum = 0;
	int order;
	TreeMap<Integer,String> orderMap = new TreeMap<Integer,String>();
	SortedMap<Integer,String> orderList = (SortedMap<Integer,String>) orderMap;
	
	//	Misc
	boolean success = false;
	GraphPanelControler controler;
	int loopDetector = 0;
	
//
//	Accessories for UI
	
	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			noSelectionMade = true;
			processChildren();
			controler.getNSInstance().setInput(dataString, 2);
		}
	};
	
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand().equals("Cancel")) {
			noSelectionMade = true;
		}
		frame.dispose();
		processChildren();
		if (!success) failed();
		controler.getNSInstance().setInput(dataString, 2);
	}


	public DwzImport(JFrame mainWindow, GraphPanelControler controler) {
		
		this.controler = controler;
//
//		Open XML document
		
//		File file = new File("C:\\Users\\Matthias\\Desktop\\kgif.xml");
		FileDialog fd = new FileDialog(mainWindow);
		fd.setTitle("Select a DenkWerkZeug KGIF (Knowledge Graph) file");
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		File file = new File(filename);
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error DI101 " + e);
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document dwz = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error DI102 " + e2 );
		}
		
		try {
			dwz = db.parse(fileInputStream);
			
			Element dwzRoot = null;
			dwzRoot = dwz.getDocumentElement();
			if (dwzRoot.getTagName() != XML_ROOT) {
				System.out.println("Error DI105, unexpected: " + dwzRoot.getTagName() );
				fileInputStream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error DI106 " + e1 + "\n" + e1.getClass());
			if (e1.getClass().equals(java.net.UnknownHostException.class))
				controler.displayPopup("Currently, a host is not reachable, and therefore " +
						"the second line of the KGIF file must be removed.");
		} catch (SAXException e) {
			System.out.println("Error DI107 " + e );
		}
		
		NodeList graphContainer = dwz.getElementsByTagName("graph");

//
//		Find DWZ nodes
		
		dwzNodes = ((Element) graphContainer.item(0)).getElementsByTagName("node");
		System.out.println("DI How many Nodes found? " + dwzNodes.getLength());
		
		for (int i = 0; i < dwzNodes.getLength(); i++) {
			Element node = (Element) dwzNodes.item(i);
			String nodeID = node.getAttribute("id");
			dwz2index.put(nodeID, i);
			
			//	Label
			NodeList labelContainer = node.getElementsByTagName("label");
			Element label = (Element) labelContainer.item(0);
			String labelString = label.getTextContent();
			typeDict.put(nodeID, labelString);
			
			//	Prepare choice
			selected.put(nodeID, false);
			DefaultMutableTreeNode branch = new DefaultMutableTreeNode(
					new BranchInfo(nodeID, labelString));
			treeNodes.put(nodeID, branch);
		}
		
		selected.put("root", false);
		DefaultMutableTreeNode top = 
	    		new DefaultMutableTreeNode(new BranchInfo("root", "All"));
		treeNodes.put("root", top);
		
//		
//		Read DWZ links
		
		dwzLinks = ((Element) graphContainer.item(0)).getElementsByTagName("link");
		System.out.println("DI How many links found: " + dwzLinks.getLength());
		edgesNum = 0;
		
		for (int i = 0; i < dwzLinks.getLength(); i++) {

			Element link = (Element) dwzLinks.item(i);
			String fromNode = link.getAttribute("from");
			String toNode = link.getAttribute("to");
			String linkType = link.getAttribute("type");
			if (fromNode.equals(toNode)) continue;
			
			//	Extract the type hierarchy of relations
			if (linkType.equals("cds-rel-hasSubType")) {
				parents0.put(toNode, fromNode);
			} else if (linkType.equals("cds-rel-hasSuperType")) {
				parents0.put(fromNode, toNode);
			} else if (linkType.equals("cds-rel-hasInverse")) {
				if (!toNode.equals(fromNode)) {
					inverses.put(toNode, fromNode);
				}
			}
		}
		
		//	Recursively build the set of expandable relations 

		hierarchicalRelations.add("cds-rel-hasDetail");
		topDown.put("cds-rel-hasDetail", true);
		
		for (int i = 0; i < dwzNodes.getLength(); i++) {
			Element node = (Element) dwzNodes.item(i);
			String nodeID = node.getAttribute("id");
			boolean result = false;
			loopDetector = 0;
			result = isHierarchical(nodeID);
			if (inverses.containsKey(nodeID)) {
				String inverse = inverses.get(nodeID);
				result = result || isHierarchical(inverse);
			}
		}
		
		//	Building the superset of expandable relations (such as tag or type)
		
		Iterator<String> itix = hierarchicalRelations.iterator();
		while (itix.hasNext()) {
			String next = itix.next();
			System.out.println("Hierarchical: " + next);
			expandableRelations.add(next);
		}
		
		expandableRelations.add("cds-rel-hasTagMember");
		topDown.put("cds-rel-hasTagMember", true);
		expandableRelations.add("vocabulary-rel-hasTerm");
		topDown.put("vocabulary-rel-hasTerm", true);
		
		for (int i = 0; i < dwzNodes.getLength(); i++) {
			Element node = (Element) dwzNodes.item(i);
			String nodeID = node.getAttribute("id");
			boolean result = false;
			loopDetector = 0;
			result = isExpandable(nodeID);
			if (inverses.containsKey(nodeID)) {
				String inverse = inverses.get(nodeID);
				result = result || isExpandable(inverse);
			}
		}
		
		itix = expandableRelations.iterator();
		while (itix.hasNext()) {
			String next = itix.next();
			System.out.println("Expandable: " + next);
			expandableRelations.add(next);
		}
		
		//	Record hierarchical or at least expandable relations

		for (int i = 0; i < dwzLinks.getLength(); i++) {
			Element link = (Element) dwzLinks.item(i);
			String fromNode = link.getAttribute("from");
			String toNode = link.getAttribute("to");
			String linkType = link.getAttribute("type");

			boolean hier = false;
			boolean parentToChild = false;
			if (hierarchicalRelations.contains(linkType)) {
				hier = true;
				parentToChild = topDown.get(linkType);
			} else if (inverses.containsKey(linkType)) {
				String inverseLink = inverses.get(linkType);
				if (hierarchicalRelations.contains(inverseLink)) { 
					hier = true;
					parentToChild = topDown.get(inverseLink);
				}
			}
			if (hier) {	
				if (parentToChild) {
					if (parents.containsKey(toNode)) continue;
					parents.put(toNode, fromNode);
				} else {
					if (parents.containsKey(fromNode)) continue;
					parents.put(fromNode, toNode);
				}
			}
		}
		
		//	Record expandable relations that are not already recorded as hierarchical

		for (int i = 0; i < dwzLinks.getLength(); i++) {
			Element link = (Element) dwzLinks.item(i);
			String fromNode = link.getAttribute("from");
			String toNode = link.getAttribute("to");
			String linkType = link.getAttribute("type");

			boolean exp = false;
			boolean parentToChild = false;
			if (expandableRelations.contains(linkType)) {
				exp = true;
				parentToChild = topDown.get(linkType);
			} else if (inverses.containsKey(linkType)) {
				String inverseLink = inverses.get(linkType);
				if (expandableRelations.contains(inverseLink)) { 
					exp = true;
					parentToChild = topDown.get(inverseLink);
				}
			}
			if (exp) {	
				if (parentToChild) {
					if (parents.containsKey(toNode)) continue;
					parents.put(toNode, fromNode);
				} else {
					if (parents.containsKey(fromNode)) continue;
					parents.put(fromNode, toNode);
				}
			}
		}
		
		//	Add rest and top

		Enumeration<String> children = parents.keys();
		while (children.hasMoreElements()) {
			String childKey = children.nextElement();
			String parentKey = parents.get(childKey);
			if (!parents.containsKey(parentKey)) {
				parents2.put(parentKey, "root");
			}
		}
		//	Laggards
		Enumeration<String> moreChildren = parents2.keys();
		while (moreChildren.hasMoreElements()) {
			String childKey = moreChildren.nextElement();
			parents.put(childKey, "root");
		}
		//	Kitchen sink
		for (int i = 0; i < dwzNodes.getLength(); i++) {
			Element node = (Element) dwzNodes.item(i);
			String nodeID = node.getAttribute("id");
			if (!parents.containsKey(nodeID)) {
				parents.put(nodeID, "root");
				selected.put(nodeID,  false);
			}
		}
		
		//	Build selection tree

	    createSelectionNodes(top);
			
	    DefaultTreeModel model = new DefaultTreeModel(top);
	    tree = new JTree(model);
	    
	    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	    tree.addTreeSelectionListener(this);
	    order = 0;	//	Record the order to arrange the nodes in the same order
	    
		//	UI for selection (duplicated from ImappingImport //	TODO reuse
		
        frame = new JFrame("Pick a collection?");
        frame.setLocation(100, 170);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);	// closing triggers further processing
		frame.addWindowListener(myWindowAdapter);
		frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(tree));
        
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BorderLayout());
        JButton continueButton = new JButton("Continue");
        continueButton.addActionListener(this);
        continueButton.setSelected(true);
		JButton cancelButton = new JButton("Cancel");
	    cancelButton.addActionListener(this);
	    
	    String okLocation = "East";
	    String cancelLocation = "West";
	    String multSel = "CTRL";
		if (System.getProperty("os.name").equals("Mac OS X")) {
	    	okLocation = "East";
	    	cancelLocation = "West";
	    	multSel = "CMD";
	    }
		JLabel instruction = new JLabel("<html><body>" +
	    "You may restrict your import. " +
		"Do do so, select one or more branches. <br />" + 
	    "Specify multiple selections as usual by holding " + multSel + 
	    " or Shift while clicking. <br />&nbsp;<br />");
        toolbar.add(continueButton, okLocation);
		toolbar.add(cancelButton, cancelLocation);
		
		toolbar.add(instruction, "North");
		toolbar.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.add(toolbar,"South");
        frame.pack();

        frame.setMinimumSize(new Dimension(400, 300));
        frame.setVisible(true);
		
	}
	
	public void processChildren() {		// Bad name
	    
		if (noSelectionMade) {
			DefaultMutableTreeNode top = treeNodes.get("root");
			toggleSelection(top, true);
		}
		
		j = 0;

		SortedSet<Integer> orderSet = (SortedSet<Integer>) orderList.keySet();
		Iterator<Integer> ixit = orderSet.iterator(); 

//
//		Create my nodes
		
		while (ixit.hasNext()) {
			int nextnum = ixit.next();
			String nodeRef = orderMap.get(nextnum);
			int i = dwz2index.get(nodeRef);
//		for (int i = 0; i < dwzNodes.getLength(); i++) {
			Element node = (Element) dwzNodes.item(i);
			String nodeID = node.getAttribute("id");
			if (!selected.get(nodeID)) {
				continue;
			}
			
			//	Label
			NodeList labelContainer = node.getElementsByTagName("label");
			Element label = (Element) labelContainer.item(0);
			String labelString = label.getTextContent();
			
			//	Content
			NodeList contentContainer = node.getElementsByTagName("content");
			String content = "";
			int found = contentContainer.getLength();
			if (found > 0) {
				content = contentContainer.item(0).getTextContent().toString();
			}
			
			//	Optional: mark pale if BuiltIn 
			String newNodeColor = "#ccdddd";
			NodeList attrContainer = node.getElementsByTagName("attributes");
			NodeList attrNodes = ((Element) attrContainer.item(0)).getElementsByTagName("attribute");
			String createdBy = "";
			for (int a = 0; a < attrNodes.getLength(); a++) {
				Element attrNode = (Element) attrNodes.item(a);
				String attrName = attrNode.getAttribute("name");
				if (!attrName.equals("pmodel-att-creationSource")) continue;
				createdBy = attrNode.getTextContent();
			}
			if (createdBy.equals("builtin-cds")) {
				newNodeColor = "#eeeeee";
			}
			
			String newLine = "\r";
			String topicName = labelString;
			String verbal = content;
			if (topicName.equals(newLine)) topicName = "";
			if (verbal == null || verbal.equals(newLine)) verbal = "";
			if (!verbal.isEmpty()) newNodeColor = "#ccdddd";
			if (topicName.isEmpty() && verbal.isEmpty()) continue;
			int id = 100 + j;

			int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			int x = 40 + (j/maxVert) * 150;
			Point p = new Point(x, y);
			GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

			nodes.put(id, topic);
			dwz2num.put(nodeID, id);
			j++;
		}
		
//
//		Create edges
		
		for (int i = 0; i < dwzLinks.getLength(); i++) {

			// Determine edge
			Element link = (Element) dwzLinks.item(i);
			String fromNode = link.getAttribute("from");
			String toNode = link.getAttribute("to");
			String linkType = link.getAttribute("type");
			String linkLabel = typeDict.get(linkType);
			
			if (!selected.containsKey(toNode)) {
				System.out.println("Error DI113: target node missing: KGIF inconsistent? " +
				"Link mentions missing item " + toNode); 
				continue;
			}
			if (expandableRelations.contains(linkType)) {
				if (!selected.get(fromNode) || !selected.get(toNode)) continue;
			} else {
				if (!selected.get(fromNode) && !selected.get(toNode)) continue;
			}
			
			if (!dwz2num.containsKey(fromNode)) {
				if (selected.get(toNode)) {
					createRelatedNode(fromNode);
				} else continue;
			}
			int sourceNodeNum = dwz2num.get(fromNode);
			
			if (!dwz2num.containsKey(toNode)) {
				if (selected.get(fromNode)) {
					createRelatedNode(toNode);
				} else continue;
			}
			int targetNodeNum = dwz2num.get(toNode);
			GraphNode sourceNode = nodes.get(sourceNodeNum);
			GraphNode targetNode = nodes.get(targetNodeNum);
			
			//	See if it is duplicate
			Enumeration<GraphEdge> neighbors = sourceNode.getEdges();
			GraphEdge existingEdge = null;
			GraphEdge testEdge = null;
			while (neighbors.hasMoreElements()) { 
				testEdge = neighbors.nextElement();
				GraphNode otherEnd = sourceNode.relatedNode(testEdge);
				if (otherEnd.equals(targetNode)) {
					existingEdge = testEdge;
					continue;
				}
			}
			
			//	Generate it	
			GraphEdge edge = null;
			if (existingEdge != null) {
				String anotherLink = existingEdge.getDetail();
				if (anotherLink.equals(linkLabel)) continue;
				linkLabel = anotherLink + "<br />+ " + linkLabel;
				existingEdge.setColor("#aaaaaa");
				existingEdge.setDetail(linkLabel);
			} else {
				String newEdgeColor = "#c0c0c0";
				String extraEdgeColor = "#ffff99";
				NodeList attrContainer = link.getElementsByTagName("attributes");
				NodeList attrNodes = ((Element) attrContainer.item(0)).getElementsByTagName("attribute");
				String createdBy = "";
				for (int a = 0; a < attrNodes.getLength(); a++) {
					Element attrNode = (Element) attrNodes.item(a);
					String attrName = attrNode.getAttribute("name");
					if (!attrName.equals("pmodel-att-creationSource")) continue;
					createdBy = attrNode.getTextContent();
				}
				if (createdBy.equals("InfModelXy") || createdBy.equals("builtin-cds")) {
					newEdgeColor = "#eeeeee";
					extraEdgeColor = "#eeeeee";
//				} else {
//					System.out.println(createdBy);
				}
				edgesNum++;
				if (expandableRelations.contains(linkType)) {
					edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), linkLabel);
				} else {
					edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(extraEdgeColor), linkLabel);
				}
				edges.put(edgesNum, edge);
				sourceNode.addEdge(edge);
				targetNode.addEdge(edge);
			}
		}
	
//		layoutLikePlanarity();
		layoutLikeIkeaAssembly();
		
		
//			
//		Pass on the new map
		
		System.out.println("DI Map: " + nodes.size() + " " + edges.size());
		try {
			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error DI108 " + e1);
		} catch (IOException e1) {
			System.out.println("Error DI109 " + e1);
		} catch (SAXException e1) {
			System.out.println("Error DI110 " + e1);
		}
		
		success = true;
	}
	
	public boolean isHierarchical(String rel) {
//		System.out.println("isHierarchical? " + rel);
		loopDetector++;
		if (loopDetector > 20) return false;
		boolean result = false;
		String parent = "";
		if (hierarchicalRelations.contains(rel)) {
			return true;
		}
		if (parents0.containsKey(rel)){
			parent = parents0.get(rel);
			result = isHierarchical(parent);
		}
		if (result) {
			hierarchicalRelations.add(rel);
			boolean upDown = topDown.get(parent); 
			topDown.put(rel, upDown);
		} else if (inverses.containsKey(rel)) {
			String inverse = inverses.get(rel);
			result = isHierarchical(inverse);
			if (result) {
				hierarchicalRelations.add(rel);
				boolean upDown = topDown.get(inverse); 
				topDown.put(rel, !upDown);
			}
		}
		return result;
	}
	
	public boolean isExpandable(String rel) {
		loopDetector++;
		if (loopDetector > 20) return false;
		boolean result = false;
		String parent = "";
		if (expandableRelations.contains(rel)) {
			return true;
		}
		if (parents0.containsKey(rel)){
			parent = parents0.get(rel);
			result = isExpandable(parent);
		}
		if (result) {
			expandableRelations.add(rel);
			boolean upDown = topDown.get(parent); 
			topDown.put(rel, upDown);
		} else if (inverses.containsKey(rel)) {
			String inverse = inverses.get(rel);
			result = isExpandable(inverse);
			if (result) {
				expandableRelations.add(rel);
				boolean upDown = topDown.get(inverse); 
				topDown.put(rel, !upDown);
			}
		}
		return result;
	}
	
	public void createRelatedNode(String nodeRef) {
		
		if (nodeRef.equals("root")) {
			if (dwz2num.containsKey("root")) return;
			int id = 100 + j;
			
			int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			int x = 40 + (j/maxVert) * 150;
			Point p = new Point(x, y);
			GraphNode topic = new GraphNode (id, p, Color.decode("#eeeeee"), "root", "(Root's details?)");	

			nodes.put(id, topic);
			dwz2num.put(nodeRef, id);
			j++;
			return;
		}
		
		if (!dwz2index.containsKey(nodeRef)) {
			System.out.println("Error DI112: KGIF inconsistent? " +
					"Link mentions missing item " + nodeRef);
			createRelatedNode("root");
			int rootNum = dwz2num.get("root");
			dwz2num.put(nodeRef, rootNum);
			return;
		}
		int index = dwz2index.get(nodeRef);
		Element node = (Element) dwzNodes.item(index);
		
//		String newNodeColor = controler.getNewNodeColor();	// TODO 
		String newNodeColor = "#eeeeee";
		String newLine = "\r";
		
		//	Label
		NodeList labelContainer = node.getElementsByTagName("label");
		Element label = (Element) labelContainer.item(0);
		String labelString = label.getTextContent();
		String topicName = labelString;
		
		//	Content
		NodeList contentContainer = node.getElementsByTagName("content");
		String content = "";
		int found = contentContainer.getLength();
		if (found > 0) {
			content = contentContainer.item(0).getTextContent().toString();
		}
		String verbal = content;

		if (topicName.equals(newLine)) topicName = "";
		if (verbal == null || verbal.equals(newLine)) verbal = "";
		int id = 100 + j;
		
		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		dwz2num.put(nodeRef, id);
		j++;

		String parentNodeUri = parents.get(nodeRef); 
		int targetNodeNum;
		if (!dwz2num.containsKey(parentNodeUri)) {
			createRelatedNode(parentNodeUri);	//	recursive
		}
		targetNodeNum = dwz2num.get(parentNodeUri);
		edgesNum++;
		GraphNode sourceNode = nodes.get(id);
		GraphNode targetNode = nodes.get(targetNodeNum);
		GraphEdge edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode("#eeeeee"), "");
		edges.put(edgesNum,  edge);
		sourceNode.addEdge(edge);
		targetNode.addEdge(edge);
	}
	
	
//
//	Accessories for branch selection
    
    private void createSelectionNodes(DefaultMutableTreeNode top) {
        DefaultMutableTreeNode branch = null;
        BranchInfo categoryInfo = (BranchInfo) top.getUserObject();
        String parentKey = categoryInfo.getKey();
        String childKey = "";
        
        Enumeration<String> children = parents.keys();
        while (children.hasMoreElements()) {
        	childKey = children.nextElement();
        	String testParent = parents.get(childKey);
        	if (testParent.equals(parentKey)) {
        		branch = treeNodes.get(childKey);
                top.add(branch);
        		order++;
        		orderMap.put(order, childKey);
                createSelectionNodes(branch);	// recursive
        	}
        }
    }
    
    private class BranchInfo {
        public String branchKey;
        public String branchLabel;
 
        public BranchInfo(String branchKey, String branchLabel) {
        	this.branchKey = branchKey;
        	this.branchLabel = branchLabel;
            
            if (branchLabel == null) {
                System.err.println("Error DI114 Couldn't find info for " + branchKey);
            }
        }
 
        public String getKey() {
            return branchKey;
        }
        
        public String toString() {
            return branchLabel;
        }
    }

	public void valueChanged(TreeSelectionEvent arg0) {
		TreePath[] paths = arg0.getPaths();

		for (int i = 0; i < paths.length; i++) {
			TreePath selectedPath = paths[i];
			Object o = selectedPath.getLastPathComponent();
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) o;
			toggleSelection(selectedNode, arg0.isAddedPath(i));
		}
	}

	public void toggleSelection(DefaultMutableTreeNode selectedNode, boolean fluct) {
		noSelectionMade = false;
		String fluctText = "removed";
		if (fluct) fluctText = "added";
		BranchInfo branch = (BranchInfo) selectedNode.getUserObject();
		String keyOfSel = branch.getKey();
		if (selected.containsKey(keyOfSel)) {
			boolean currentSetting = selected.get(keyOfSel);
			selected.put(keyOfSel, !currentSetting);
		} else {
			System.out.println("Error DI120 " + keyOfSel);
			if (!success) failed();
			frame.dispose();
		}

		@SuppressWarnings("rawtypes")
		Enumeration children =  selectedNode.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
			toggleSelection(child, fluct);
		}
	}
	
//
//	Accessories for layout
	
	public void layoutLikeIkeaAssembly() {
		Enumeration<GraphNode> nodes2 = nodes.elements();
		while (nodes2.hasMoreElements()) {
			GraphNode node = nodes2.nextElement();
			Enumeration<GraphEdge> neighbors = node.getEdges();
			int count = 0;
			while (neighbors.hasMoreElements()) {
				neighbors.nextElement();	// Just to count; TODO better way
				count++;
			}
			if (count > 2) {
				Point prelim = node.getXY();
				int x = prelim.x;
				int y = prelim.y;
				Point adjusted = new Point(x + 30, y);
				node.setXY(adjusted);
			}
		}
	}
	
	//	Rearrange nodes in a circle
	public void layoutLikePlanarity() {
		int nodesTotal = nodes.size();

		Enumeration<GraphNode> nodes2 = nodes.elements();
		int i = 0;
		while (nodes2.hasMoreElements()) {
			GraphNode node = nodes2.nextElement();
			Point p = circleEvenly(i, nodesTotal, new Point(10, 10));
			node.setXY(p);
			i++;
		}
	}
	public Point circleEvenly(int item, int total, Point corner) {
		int radius = total * 7;
		double incr = 360.0 / total;
			double x = radius * Math.cos((incr * item) * (Math.PI / 180));					
			double y = radius * Math.sin((incr * item) * (Math.PI / 180));					
			int xInt = (int) Math.round(x) + corner.x - radius/2;
			int yInt = (int) Math.round(y) + corner.y - radius/2;
		return new Point(xInt, yInt);
	}
	
//
//	Misc
	
	public void failed() {
		controler.displayPopup("Import failed.");		
	}
}

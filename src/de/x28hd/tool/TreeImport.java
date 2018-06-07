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
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.transform.TransformerConfigurationException;

import org.stackoverflowusers.file.WindowsShortcut;
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
	
	//	Input file list sorting 
	Hashtable<String,String> byPaths = new Hashtable<String,String>();;
	TreeMap<String,String> pathsMap = new TreeMap<String,String>();
	SortedMap<String,String> pathsList = (SortedMap<String,String>) pathsMap;
	
	// Auxiliary stuff
	Hashtable<String,String> inputItems = new Hashtable<String,String>();
	Hashtable<Integer,String> relationshipFrom = new Hashtable<Integer,String>();
	Hashtable<Integer,String> relationshipTo = new Hashtable<Integer,String>();
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	HashSet<GraphEdge> nonTreeEdges = new HashSet<GraphEdge>();
	HashSet<GraphEdge> xrefTreeEdges = new HashSet<GraphEdge>();
	HashSet<GraphNode> xrefTreeNodes = new HashSet<GraphNode>();
	GraphPanelControler controler;
	Hashtable<Integer,String> nodeColors = new Hashtable<Integer,String>();
	
	JTree tree;
	JFrame frame;
	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			finish();
		}
	};
	
	int j = -1;
	int readCount = 0;
	int edgesNum = 0;
    DefaultMutableTreeNode top;
    String htmlOut = "";
	JPanel radioPanel = null;
	boolean transit = false;
	JCheckBox transitBox = null;
	int relID = -1;
	boolean extended = false;
	boolean windows = false;
	int monitor = 0;
	
	//	Constants
	int maxVert = 10;
	String[] colors = {
			"#ff0000",
			"#ffaa00",
			"#ffff00",
			"#00ff00",
			"#0000ff",
			"#b200b2"};
	String fs = "";
	
	//	Overriden later
	int knownFormat = 12;	//	OPML
	String topNode = "body";
	String nestNode = "outline";
	String labelAttr = "text";
	String idAttr = "";
	
	public TreeImport(File file, GraphPanelControler controler, int knownFormat) {
		this.controler = controler;
		this.knownFormat = knownFormat;
		extended = controler.getExtended();
        controler.stopHint();
		windows = (System.getProperty("os.name").startsWith("Windows"));
		
		if (knownFormat == 17) {	//	Filepaths list
			fs = System.getProperty("file.separator");
			topNode = file.getAbsolutePath();
			topNode = createRelatedNode(file.getAbsolutePath(), false);
		    top = new DefaultMutableTreeNode(new BranchInfo(inputID2num.get(topNode), file.getName()));
		    fileTree(file, topNode, top, 0);
		}
		
		//	Collect relationships
		Enumeration<Integer> relEnum = relationshipFrom.keys();
		while (relEnum.hasMoreElements()) {
			Integer relID = relEnum.nextElement();
			String fromRef = relationshipFrom.get(relID);
			String toRef = relationshipTo.get(relID);
			addEdge(fromRef, toRef, true, true, "");
		}
		commonPart();
		return;
	}
	
	public void fileTree(File file, String parentID, DefaultMutableTreeNode parentInTree,
			int level) {
	
		File[] dirList = file.listFiles();
		for (File f : dirList) {
			monitor++;
			if (monitor % 250 > 248) controler.displayPopup(monitor + " enties processed ...");
			String id = readCount++ + "";
			String desti = "";
			if (f.isDirectory()) {
				if (!windows) {
					try {
						desti = f.getCanonicalPath();
					} catch (IOException e) {
						System.out.println("Error TI111 " + e.getMessage());
						continue;
					}
					if (!desti.equals(f.getAbsolutePath())) {
						
						//	Recursive
						String destID = createRelatedNode(desti, false);

						relID++;
						relationshipFrom.put(relID, parentID);
						relationshipTo.put(relID, destID);
						continue;
					}
				} 
				//	add node
				String label = f.getName();
				inputItems.put(id, label);
				byPaths.put(id, f.getAbsolutePath());
				String detail = "<html><body>Open file <a href=\"" + f.toURI().toString()  + "\">" + f.getName() + "</a></body></html>";
				addNode(id, detail);
				DefaultMutableTreeNode branch = 
						new DefaultMutableTreeNode(new BranchInfo(inputID2num.get(id), label));
				parentInTree.add(branch);

				// recurse
				fileTree(f, id, branch, level + 1);

				// add color and edge
				String treeColor = "";
				if (extended) {
					treeColor = colors[level % 6];
					int nodeNum = inputID2num.get(id);
					nodeColors.put(nodeNum, treeColor);
				}
				addEdge(parentID, id, false, treeColor);
			} else { // not a directory 
				if (windows) {
					try {
						WindowsShortcut ws = new WindowsShortcut(f);

						// record shortcut, skip leaf nodes
						if (!ws.isPotentialValidLink(f)) continue;
						if (!ws.isDirectory()) continue;
						desti = ws.getRealFilename();

						//	Recursive
						String destID = createRelatedNode(desti, false);

						relID++;
						relationshipFrom.put(relID, parentID);
						relationshipTo.put(relID, destID);
					} catch (IOException e) {
						System.out.println("Error TI104 " + e.getMessage());
						continue;
					} catch (ParseException e) {
						if (!e.toString().endsWith("magic is missing")) {
							System.out.println("Error TI105 " + e.getMessage());
						}
						continue;
					}
				}
			}
		}
	}
	
	public String createRelatedNode(String desti, boolean siteMap) {
		String fromRef = "";
		if (byPaths.containsValue(desti) && !siteMap) {
			// Find key
			Enumeration<String> pathsEnum = byPaths.keys();
			while (pathsEnum.hasMoreElements()) {
				String testKey = pathsEnum.nextElement();
				String testPath = byPaths.get(testKey);
				if (testPath.equals(desti)) {
					fromRef = testKey;
					break;
				}
			}
			return fromRef;
		} else {
			String destID = readCount++ + "";
			int slashPos = desti.lastIndexOf(fs);
			if (slashPos <= 1) {
				inputItems.put(destID, desti);
				byPaths.put(destID, desti);
				addNode(destID, desti, true);
				return destID;
			}
			String ancestors = desti.substring(0, slashPos);
			String leaf = desti.substring(slashPos + 1);
			inputItems.put(destID, leaf);
			byPaths.put(destID, desti);
			File f = new File(desti);
			String detail = "<html><body>Open file <a href=\"" + f.toURI().toString()  + "\">" + desti + "</a></body></html>";
			int type = 1;
//			if (desti == topNode) type = 0;
			addNode(destID, detail, desti != topNode);
			
			fromRef = createRelatedNode(ancestors, false);	// recurse
			
			addEdge(fromRef, destID, false, true, "");
			return destID;
		}
	}
	
	public TreeImport(Document inputXml, GraphPanelControler controler, int knownFormat) {

		this.controler = controler;
		this.knownFormat = knownFormat;
		extended = controler.getExtended();
		if (knownFormat == 11) {	//	FreeMind
			topNode = "map";
			nestNode = "node";
			labelAttr = "TEXT";
			idAttr = "ID";
		}
	if (knownFormat != 18) {
		NodeList graphContainer = inputXml.getElementsByTagName(topNode);
		inputItems.put(topNode, "ROOT");
		addNode(topNode, "");
		Element graph = (Element) graphContainer.item(0);
		
		int idForJTree = inputID2num.get(topNode);
	    top = new DefaultMutableTreeNode(new BranchInfo(idForJTree, " "));
		
		//	Collect nested nodes
		nest(graph, topNode, top, 0);
		
		//	Collect relationships
		Enumeration<Integer> relEnum = relationshipFrom.keys();
		while (relEnum.hasMoreElements()) {
			Integer relID = relEnum.nextElement();
			String fromRef = relationshipFrom.get(relID);
			String toRef = relationshipTo.get(relID);
			addEdge(fromRef, toRef, true, "");
		}
		
		} else {	

			// Sitemap

			topNode = "urlset";
			nestNode = "url";
			NodeList graphContainer = inputXml.getElementsByTagName(topNode);
			inputItems.put(topNode, "ROOT");
			addNode(topNode, "");
			int idForJTree = inputID2num.get(topNode);
			top = new DefaultMutableTreeNode(new BranchInfo(idForJTree, " "));

			if (graphContainer.getLength() <= 0) return;
			for (int i = 0; i < graphContainer.getLength(); i++) {
				Element url = (Element) graphContainer.item(i);
				NodeList locContainer = url.getElementsByTagName("loc");
				if (locContainer.getLength() <= 0) continue;
				for (int j = 0; j < locContainer.getLength(); j++) {
					Node child = locContainer.item(j);
					String name = child.getNodeName();
					if (!name.equals("loc")) continue;

					//	Extract stuff 
					String path = ((Element) child).getTextContent();
					String id = readCount++ + "";
//					path = path.replace("https://", "");
					if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
					if (path.indexOf("/") < 0) continue;
					inputItems.put(id, path.substring(path.lastIndexOf("/") + 1));
					byPaths.put(id, path);
					pathsMap.put(path, id);
				}
			}
		}
		fs = "/";
		SortedSet<String> pathsSet = (SortedSet<String>) pathsList.keySet();
		Iterator<String> ixit = pathsSet.iterator(); 
		if (pathsSet.size() > 0) {
			while (ixit.hasNext()) {
				String alphaPos = ixit.next();
				String key = pathsList.get(alphaPos);
				String path = byPaths.get(key);
				int slashPos = path.lastIndexOf(fs) + 1;
				String detail = "<html><body><a href=\"" + path + "\">" + path.substring(slashPos) + "</a></body></html>";
				addNode(key, detail);
				String path2 = path.replace(fs, "/");
				String levels[] = path2.split("/");
				int level = levels.length;
				linkToParent(path, "", key, level);
			}
		}
		commonPart();
	}
	
	public void linkToParent(String ancestorsAndMe, String descendants, String myKey,
			int level) {	// TODO integrate with new file tree import
		int slashPos = ancestorsAndMe.lastIndexOf(fs);
		if (slashPos <= 7) return;	// TODO very short labels
		String ancestors = ancestorsAndMe.substring(0, slashPos);
		String meAndDescendants = ancestorsAndMe.substring(slashPos) + descendants;
		
		String fromRef = "";
		String treeColor = "";
		if (byPaths.containsValue(ancestors)) {
			// Find key
			Enumeration<String> pathsEnum = byPaths.keys();
			while (pathsEnum.hasMoreElements()) {
				String testKey = pathsEnum.nextElement();
				String testPath = byPaths.get(testKey);
				if (testPath.equals(ancestors)) {
					fromRef = testKey;
					break;
				}
			}
		} else {
			String id = readCount++ + "";
			String label = ancestors.substring(ancestors.lastIndexOf("/") + 1);
			inputItems.put(id, label);
			String detail = "<html><bodyy><a href=\"" + ancestors + "\">" + ancestors + "</a></body></html>";
			addNode(id, detail);
			byPaths.put(id, ancestors);
			fromRef = id;
			linkToParent(ancestors, meAndDescendants, fromRef, level - 1);	// recurse 
		}
		String toRef = myKey;
		if (extended) {
			treeColor = colors[level % 6];
			int nodeNum = inputID2num.get(toRef);
			nodeColors.put(nodeNum, treeColor);
		}
		
		addEdge(fromRef, toRef, false, treeColor);
	}
	
	public void commonPart() {
		
		if (knownFormat == 18) { // not available for sitemap and file tree
			finish();
			return;
		}
//
//		Create a JTree 
	    
	    DefaultTreeModel model = new DefaultTreeModel(top);
	    controler.setTreeModel(model);
		
	    tree = new JTree(model);
	    
	    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
//	    tree.addTreeSelectionListener(this);
	    
        frame = new JFrame("Found this tree structure:");
        frame.setLocation(100, 170);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(myWindowAdapter);
		frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(tree));

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BorderLayout());
		toolbar.setBorder(new EmptyBorder(10, 10, 10, 10));
		JLabel instruction = new JLabel("<html><body>" +
	    "You may use this tree structure for re-exporting \n"
				+ "if you use the map for nothing else:</body></html>");
		toolbar.add(instruction, "North");
        JPanel buttons = new JPanel();
        buttons.setLayout(new BorderLayout());
        JButton continueButton = new JButton("Continue");
        continueButton.addActionListener(this);
        continueButton.setSelected(true);
		JButton cancelButton = new JButton("Cancel");
	    cancelButton.addActionListener(this);
        buttons.add(continueButton, "East");
		buttons.add(cancelButton, "West");
		toolbar.add(buttons,"East");
		transitBox = new JCheckBox ("Just for re-export", false);
		toolbar.add(transitBox, "West");

		frame.add(toolbar,"South");
        frame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
//        frame.setMinimumSize(new Dimension(400, 300));
        frame.setMinimumSize(new Dimension(596, 418));

        frame.setVisible(true);
        controler.stopHint();
	}
//		
//		Pass on the new map

	public void finish() {
        if (transit) { 
        	if (knownFormat == 17) {
        		Iterator<GraphEdge> xrefTreeIter = xrefTreeEdges.iterator();
        		while (xrefTreeIter.hasNext()) {
        			GraphEdge edge = xrefTreeIter.next();
        			int id = edge.getID();
        			GraphNode node1 = edge.getNode1();
        			GraphNode node2 = edge.getNode2();
        			System.out.println("Removing edge from " + node1.getLabel() + " to " + node2.getLabel());
        			edges.remove(id);
        		}
        		Iterator<GraphNode> xrefTreeIter2 = xrefTreeNodes.iterator();
        		while (xrefTreeIter2.hasNext()) {
        			System.out.println("# nodes " + xrefTreeNodes.size());
        			GraphNode node = xrefTreeIter2.next();
        			System.out.println("Removing node " + node.getDetail());
        			int id = node.getID();
        			nodes.remove(id);
        		}
        	}
        	controler.setNonTreeEdges(nonTreeEdges);
        	controler.replaceByTree(nodes, edges);
        } else {
			if (extended) {
				CentralityColoring centralityColoring = new CentralityColoring(nodes, edges);
				centralityColoring.treeLayout(nonTreeEdges);
				//	Recolor the nodes
				Enumeration<Integer> nodeColEnum = nodeColors.keys();
				while (nodeColEnum.hasMoreElements()) {
					int key = nodeColEnum.nextElement();
					String treeColor = nodeColors.get(key);
					GraphNode node = nodes.get(key);
					node.setColor(treeColor);
				}
			}
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
        	controler.setTreeModel(null);
        	controler.setNonTreeEdges(null);
        } 
	}
	
	public int nest(Node parent, String parentID, DefaultMutableTreeNode parentInTree,
			int level) {
		
		NodeList children = parent.getChildNodes();
		Node child;
		int count = 0; 
		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			String name = child.getNodeName();
			if (!name.equals(nestNode)) {
				continue;
			}
			count++;
			
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
						relID++;
						relationshipFrom.put(relID, id);
						relationshipTo.put(relID, relDesti);
					}
				}
			} else {
				if (label.isEmpty()) label = " ";
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
			
			//	recurse
			int childcount = nest(child, id, branch, level + 1);
			
			int nodeNum = inputID2num.get(id);
//			System.out.println(nodes.get(nodeNum).getLabel() + ": childcount " + childcount);
			String treeColor = "";
			if (childcount > 0 && extended) {
				treeColor = colors[level % 6];
				nodeColors.put(nodeNum, treeColor);
			}
			
			//	add link
			addEdge(parentID, id, false, treeColor);
		}
		return count;
	}
		
	public void addNode(String nodeRef, String detail) { 
		addNode(nodeRef, detail, false);
	}
	public void addNode(String nodeRef, String detail, boolean removeBeforeReexport) { 
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
		if (removeBeforeReexport) xrefTreeNodes.add(topic);
		inputID2num.put(nodeRef, id);
	}

	public void addEdge(String fromRef, String toRef, boolean xref, String treeColor) {
		addEdge(fromRef, toRef, xref, false, treeColor);
	}
	public void addEdge(String fromRef, String toRef, boolean xref, boolean removeBeforeReexport, String treeColor) {
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
		if (xref) {
			if (extended) {
				newEdgeColor = "#f0f0f0";
			} else {
				newEdgeColor = "#ffff00";
			}
		}
		if (!treeColor.isEmpty()) newEdgeColor = treeColor;
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
//		if (type == 1 || type == 3) nonTreeEdges.add(edge);
//		if (type == 2 || type == 3) xrefTreeEdges.add(edge);
		if (xref) nonTreeEdges.add(edge);
		if (removeBeforeReexport) xrefTreeEdges.add(edge);
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
			System.out.println("Error TI109 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error TI110 " + e3.toString());
		}
		return htmlOut;
	}

	private static class MyHTMLEditorKit extends HTMLEditorKit {
		private static final long serialVersionUID = 7279700400657879527L;

		public Parser getParser() {
			return super.getParser();
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		String command = arg0.getActionCommand();
		if (command == "Cancel") {
			transit = false;
		} else if (command == "Continue") {
			transit = transitBox.isSelected();
		}
        frame.setVisible(false);
        frame.dispose();
        finish();
	}
}

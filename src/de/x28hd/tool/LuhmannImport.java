package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class LuhmannImport implements Comparator<String>, ActionListener {
	
	// Main fields
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	GraphPanelControler controler;
	String dataString = null;
	
	// Input
	DocumentBuilderFactory dbf = null;
	DocumentBuilder db = null;
	Document inputXml = null;
	InputStream stream = null;
	TreeSet<String> inputs =  new TreeSet<String>();
	Hashtable<String,Integer> inputs2num = new  Hashtable<String,Integer>();
	HashSet<String> edgesDone = new HashSet<String>(); 
	TreeMap<String,String> headerMap = new TreeMap<String,String>(this);
	SortedMap<String,String> headerList = (SortedMap<String,String>) headerMap;
	SortedSet<String> headerSet = (SortedSet<String>) headerList.keySet();
	Hashtable<String,String> contents = new Hashtable<String,String>();
	boolean headersLoaded = false;
	boolean tree = false;
	boolean links = true;
	String[] colorChanges = new String[7];
	
	// Accessories
	int edgesNum =0;
	int j = 0;
	int maxVert = 10;
	DefaultMutableTreeNode top;
	static final String [] palette = 
		{"#d2bbd2", "#bbbbff", "#bbffbb", "#ffff99",	// purple, blue, green, yellow
		"#ffe8aa", "#ffbbbb",   "#ccdddd"};	// orange, red,   dark
	JDialog dialog;
	JButton nextButton;
	JCheckBox treeBox = null;
	JCheckBox linksBox = null;

	public LuhmannImport(GraphPanelControler controler) {
		this.controler = controler;
		
		JFrame mainWindow = controler.getMainWindow();
		String baseDir = "";
		try {
			baseDir = System.getProperty("user.home") + File.separator + "Desktop";
		} catch (Throwable e) {
			System.out.println("Error LI103" + e );
		}
		File b = new File(baseDir);
		File d = null;
		File h = null;

		JFileChooser chooser = new JFileChooser();
		
		// Ask for zettels folder
		chooser.setCurrentDirectory(b);
		chooser.setDialogTitle("Open a folder containing downloaded Zettels");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
        		"(Select your \"zettels\" folder)", "folder");
        chooser.setFileFilter(filter);        
	    int returnVal = chooser.showOpenDialog(mainWindow);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	d = chooser.getSelectedFile();
	    } else return;
	    
		// Ask for header file
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(b);
		chooser.setDialogTitle("Open a file containing header lines");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        filter = new FileNameExtensionFilter(
        		"A headers file (optional)", "txt");
        chooser.setFileFilter(filter);        
	    returnVal = chooser.showOpenDialog(mainWindow);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	 h = chooser.getSelectedFile();
	    }
	    
	    // Ask if display the hierarchy or the cross references
		dialog = new JDialog(controler.getMainWindow(), "Options");
		dialog.setModal(true);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setLocation(dim.width/2-dialog.getSize().width/2 - 298, 
				dim.height/2-dialog.getSize().height/2 - 209);		
		dialog.setMinimumSize(new Dimension(300, 170));
		dialog.setLayout(new BorderLayout());
		
		JLabel nextInfo = new JLabel("Do you want to visualize a network or a tree?");
		nextInfo.setBorder(new EmptyBorder(10, 10, 10, 10));
		JPanel infoContainer = new JPanel();
		infoContainer.add(nextInfo);
		dialog.add(infoContainer, "North");
		
		treeBox = new JCheckBox ("The 'Nummern' tree", false);
		treeBox.setActionCommand("tree");
		treeBox.addActionListener(this);
		treeBox.setSelected(false);
		linksBox = new JCheckBox ("The 'Verweisungen' links", false);
		linksBox.setActionCommand("links");
		linksBox.addActionListener(this);
		linksBox.setSelected(true);
		JPanel boxesContainer = new JPanel();
		boxesContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
		boxesContainer.setLayout(new GridLayout(2, 1));

		boxesContainer.add(treeBox);
		boxesContainer.add(linksBox);
		dialog.add(boxesContainer, "Center");
		
		nextButton = new JButton("OK");
		nextButton.addActionListener(this);
		nextButton.setEnabled(true);
		JPanel buttonContainer = new JPanel();
		buttonContainer.add(nextButton);
		dialog.add(buttonContainer, "South");
		dialog.setVisible(true);
//	    System.out.println("Tree " + tree + ", links " + links);
			
		if (h != null) loadHeaders(h.getPath());	// makes slow; switch off for testing
//		d = new File("c:\\Users\\Matthias\\Desktop\\luh");
		File[] dirList = d.listFiles();

		// Show loading progress and advice
		
		int count = 0;
		dialog = new JDialog(controler.getMainWindow(), "Loading...");
		dialog.setLocation(dim.width/2-dialog.getSize().width/2 - 298, 
				dim.height/2-dialog.getSize().height/2 - 209);		
		dialog.setMinimumSize(new Dimension(300, 270));
		dialog.setLayout(new BorderLayout());
		
		String advice = "<html>Recommended next actions: <br /><br />";
		if (tree & !links) advice += 
				"- <b>Advanced > Make Tree</b> and then<br /> "
				+ "<b>Centrality Heatmap</b>,<br /><br />"
				+ "- or <b>Advanced > DAG layout</b>, <br />"
				+ "then find the root in the upper left, <br />"
				+ "rightclick it, <b>Advanced > Tree Layout</b>.";
		if (!tree & links) advice += "<b>Advanced > Make Circle</b> and<br />" 
				+ "then, if it looks cluttered,<br />"
				+ "find the main circle and drag <br />"
				+ "it away from the little islands. <br /><br />"
				+ "Then take a few minutes to tidy up.";
		if (tree & links) advice += "If you have many items, <br />"
				+ "there is no good recommendation.<br />"
				+ "Enjoy the impressive view but <br />"
				+ "consider restricting to either links or tree.";
		if (!tree & !links) advice += "Click the colored items and <br />"
				+ "try the links in the text pane.<br />"
				+ "For layout, consider choosing links or tree.";
		advice += "</html>";
		nextInfo = new JLabel(advice);
		nextInfo.setBorder(new EmptyBorder(10, 10, 10, 10));
		infoContainer = new JPanel();
		infoContainer.add(nextInfo);
		dialog.add(infoContainer, "North");
		
		nextButton = new JButton("OK");
		nextButton.addActionListener(this);
		nextButton.setEnabled(true);
		buttonContainer = new JPanel();
		buttonContainer.add(nextButton);
		dialog.add(buttonContainer, "South");
		dialog.setVisible(true);
		
		//	Load the stuff
		
		if (dirList == null) return;
		for (File f : dirList) {
			File file = f.getAbsoluteFile();
			loadFile(file);		// calls recordLink which adds nodes and edges
		}
		
		Iterator<String> allEnds = inputs.iterator();
		while (allEnds.hasNext()) {
			String current = allEnds.next();
			count++;
			if (count % 10 == 0)
				dialog.setTitle("Loaded " + count + " items ...");
			addContent(current);
		}
		
		if (tree) hierarchy();
		
		System.out.println(nodes.size() + " nodes, " + edges.size() + " edges");
		
		// pass on 
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error LI108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error LI109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error LI110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.setTreeModel(null);
    	controler.setNonTreeEdges(null);
    	
    	controler.toggleHyp(1, true);
	}
	
	public void loadFile(File file) {
		String filename = file.getName();
		try {
			stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error LI123 " + e);
		}
		dbf = DocumentBuilderFactory.newInstance();
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error LI105 " + e2 );
		}
		db.setErrorHandler(null);
		try {
			inputXml = db.parse(stream);
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (!inputRoot.getNodeName().equals("TEI")) return;
		} catch (IOException e1) {
			System.out.println("Error LI106 " + e1 + "\n" + e1.getClass());
			controler.displayPopup("Import failed:\n" + e1);
			return;
		} catch (SAXParseException e) {
			System.out.println("Error LI107 " + filename );
			return;
		} catch (SAXException e) {
			return;
		}
		filename = filename.replaceAll("_V$", "");
		
		NodeList divs = inputXml.getElementsByTagName("div");
		for (int i = 0; i < divs.getLength(); i++) {
			Node div = divs.item(i);	
			NamedNodeMap attr = div.getAttributes();
			if (attr.getLength() <= 0) continue;
			String type = ((Element) div).getAttribute("type");
			if (!type.contains("zettel-vorderseite")) continue;
			String content = allTags(div);
			contents.put(filename, content);
		}
		NodeList refs = inputXml.getElementsByTagName("ref");	// TODO eliminate 
		if (refs.getLength() < 2) return;
		for (int i = 1; i < refs.getLength(); i++) {
			Node ref = refs.item(i);	
			NamedNodeMap attr = ref.getAttributes();
			if (attr.getLength() <= 0) return;
			String type = ((Element) ref).getAttribute("type");
			if (!type.contains("entf")) continue;
			String target = ((Element) ref).getAttribute("target");
			target = target.replaceAll("_V$", "");
			recordLink(filename, target.substring(1));
		}
	}
	
	public void loadHeaders(String filename) {
//		filename = "c:\\Users\\Matthias\\Desktop\\hdrs.txt";
		int colorChangeNum = -1;
		boolean change = true;
		try {
			stream = new FileInputStream(new File(filename));
		} catch (FileNotFoundException e) {
			System.out.println("Error LI124 " + e);
		}
		Utilities utilities = new Utilities();
		String inputString = utilities.convertStreamToString(stream);		
		String [] lines = inputString.split("\\r?\\n");
		for (int lin = 0; lin < lines.length; lin++) {
			String line = lines[lin];
			
			// read the change indicators interspersed between the header lines
			if (line.startsWith("COLOR CHANGE")) {
				change = true;
				continue;
			} 
			
			// read the records
			String [] fields = line.split("\\t");
			String key = "";
			try {
				key = fields[0];
				headerMap.put(key, fields[1]);
			} catch (ArrayIndexOutOfBoundsException e) {
				controler.displayPopup("Header file not recognized, problem:\n " + line);
				return;
			}
			if (change) {
				if (colorChangeNum >= 6) continue;	
				colorChangeNum++;
				colorChanges[colorChangeNum] = key;
				change = false;
			}
		}
		headersLoaded = true;
	}

	public void recordLink(String source, String target) {
		if (!inputs.contains(source)) {
			inputs.add(source);
			addNode(source, "");
		}
		if (!inputs.contains(target)) {
			inputs.add(target);
			addNode(target, "");
		}
		if (links) addEdge(source, target, true);
	}

	public Vector<String> parseNummer(String id) {
		Vector<String> vector = new Vector<String>();
		String rest = id;
		String el = id.substring(0, 8); // "ZK_1_NB_"
		vector.add(el);
		int len = el.length();
		while (!rest.isEmpty()) {
			rest = rest.substring(len);
			String numString = "";
			String nextChar = "";
			for (int i = 0; i < rest.length(); i++) {
				nextChar = rest.substring(i, i + 1);
				if (nextChar.matches("[0-9]+")) {
					numString += nextChar; 
				} else {
					break;
				}
			}
			if (!numString.isEmpty()) {
				el = numString;
			} else {
				el = nextChar;
			}
			if (!el.isEmpty()) vector.add(el);
			len = el.length();
		}
		return vector;
	}
	
	public void addNode(String nodeRef, String colorString) {

		if (colorString.isEmpty()) {
			colorString = palette[0];
			for (int c = 0; c < 7; c++) {
				String boundary = colorChanges[c];
				if (boundary == null) break;
				if (compare(nodeRef, boundary) >= 0) colorString = palette[c];
			}
//			if (compare(nodeRef, "ZK_1_NB_5") >= 0) colorString = palette[1]; 
//			if (compare(nodeRef, "ZK_1_NB_6") >= 0) colorString = palette[2]; 
//			if (compare(nodeRef, "ZK_1_NB_7") >= 0) colorString = palette[3]; 
//			if (compare(nodeRef, "ZK_1_NB_7-2m") >= 0) colorString = palette[4];
//			if (compare(nodeRef, "ZK_1_NB_7-2o") >= 0) colorString = palette[3];
//			if (compare(nodeRef, "ZK_1_NB_7-7") >= 0) colorString = palette[5];
//			if (compare(nodeRef, "ZK_1_NB_8") >= 0) colorString = palette[7]; 
		}
		
		if (inputs2num.containsKey(nodeRef)) return;
		
		j++;
		int id = 100 + j;
		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		
		String label = nodeRef;
		label = label.replaceAll("^ZK_1_NB_", "");
		String detail = "<html>";
		detail += "<a href=\"https://niklas-luhmann-archiv.de/bestand/zettelkasten/zettel/" 
				+ nodeRef + "_V/\">" + label + "</a>";
//		String imageUrl = "https://images.niklas-luhmann-archiv.de/image/ZK_1_05_06_009_V_N_NB_" 
//				+ label + "?size=1";
//		detail += "<br /><img src=\"" + imageUrl + "\" width=\"432\" height= \"318\">";	// does not always work
//		detail += "<br />" + imageUrl;
		
		GraphNode topic = new GraphNode (id, p, Color.decode(colorString), label, detail);	

		nodes.put(id, topic);
		inputs2num.put(nodeRef, id);
	}

	public void addEdge(String fromRef, String toRef, boolean xref) {
		String unique = fromRef.compareTo(toRef) > 0 ? 
				(fromRef + " -- " + toRef) : (toRef + " -- " + fromRef);
		if (edgesDone.contains(unique) && xref) {
			System.out.println("Duplicate link " + unique + " skipped");
			return;
		}
		
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
		if (!xref) newEdgeColor = "#f0f0f0";
		edgesNum++;
		
		if (!inputs2num.containsKey(fromRef)) {
			System.out.println("Error LI101 " + fromRef + ", " + xref);
			return;
		}
		GraphNode sourceNode = nodes.get(inputs2num.get(fromRef));

		if (!inputs2num.containsKey(toRef)) {
			System.out.println("Error LI102 " + toRef);
			return;
		}
		GraphNode targetNode = nodes.get(inputs2num.get(toRef));
			
		edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), "");
		edges.put(edgesNum, edge);
		sourceNode.addEdge(edge);
		targetNode.addEdge(edge);
		edgesDone.add(unique);
	}
	
	public void addContent(String nodeRef) {
		int nodeNum = inputs2num.get(nodeRef);
		GraphNode node = nodes.get(nodeNum);
		String detail = node.getDetail();
		
		String header = "";
		Iterator<String> iter = headerSet.iterator();
		while (iter.hasNext()) {
			String headerNum = iter.next();
			int compared = compare(nodeRef, headerNum);
			if (compared >= 0) {
				header = headerList.get(headerNum);
				continue;
			}
			break;
		}
		if (headersLoaded) detail += " " + header;
		
		if (contents.containsKey(nodeRef)) {
			String content = contents.get(nodeRef);
			detail += content;
		} else {
			detail += "<br /><br />Sorry, no transcription found.";
			node.setColor(palette[6]);
		}
		detail += "</html>";
		node.setDetail(detail);

	}
	public int compare(String nummer1, String nummer2) {
		Vector<String> vector1 = parseNummer(nummer1);
		Vector<String> vector2 = parseNummer(nummer2);
		int min = Math.min(vector1.size(), vector2.size());
		for (int i = 0; i < min; i++) {
			String element1 = vector1.get(i);
			String element2 = vector2.get(i);
			boolean numeric = true;
			int num1 = 0;
			int num2 = 0;
			try {
				num1 = Integer.parseInt(element1);
				num2 = Integer.parseInt(element2);
			} catch (NumberFormatException e) {
				numeric = false;
			}
			if (!numeric) {
				element1 = element1.replaceAll("_", " ");
				element2 = element2.replaceAll("_", " ");
				int compared = element1.compareTo(element2);
				if (compared == 0) continue;
				return compared;
			} else {
				int compared = Integer.compare(num1, num2);
				if (compared == 0) continue;
				return compared;
			}
		}
		return Integer.compare(vector1.size(), vector2.size());
	}

	public String allTags(Node node) {
		String myString = "";
		boolean ref = false;
		
		NodeList list = node.getChildNodes();
		int len = list.getLength();
		if (len <= 0) {
			String text = node.getTextContent();
			return text; 
		}
		for (int i = 0; i < len; i++) {
			Node child = list.item(i);
			String nodeName = child.getNodeName();

			if (nodeName == "lb") {
				myString += "<br />";
				continue;
			} else if (nodeName == "note") {
				myString += "[*]";
				continue;
			} else if (nodeName == "expan") {
				continue;
			} else if (nodeName == "p") {
				myString += "<br /><br />";	
			} 
			
			// Links
			if (node.getNodeName() == "ref") ref = true;
			String target = "";
			String anchorString = "";
			if (ref) {
				NamedNodeMap attr = node.getAttributes();
				if (attr.getLength() > 0) {
					String type = ((Element) node).getAttribute("type");
					if (type.contains("entf")) {
						target = ((Element) node).getAttribute("target");
//						target = target.replaceAll("_V$", "");
						target = "https://niklas-luhmann-archiv.de/bestand/zettelkasten/zettel/" 
								+ target.substring(1);
					} else ref = false;
				}
				if (!target.isEmpty()) {
					anchorString = "<a href=\"" + 
//					"#" + target.substring(9) + "\">";
					target + "\">";
				}
			}
			
			// All
			String childString = allTags(child);	// recursion
			
			// Links (cont'd)
			if (ref) {
				myString += anchorString + childString + "</a>";
				ref = false;
			} else {
				myString = myString + childString;
			}
		}
		return myString;
	}
	public void hierarchy() {
		top = new DefaultMutableTreeNode(new BranchInfo(0, "(Root)"));
		Iterator<String> allEnds = inputs.iterator();
		while (allEnds.hasNext()) {
			String current = allEnds.next();
			Vector<String> currentVector = parseNummer(current);
			findOrCreate(currentVector, true);
		}
		// Mark the used nodes
		Iterator<String> allEnds2 = inputs.iterator();
		while (allEnds2.hasNext()) {
			String current = allEnds2.next();
			Vector<String> currentVector = parseNummer(current);
			DefaultMutableTreeNode result = findOrCreate(currentVector, false);
			BranchInfo info = (BranchInfo) result.getUserObject();
			String label = info.toString();
			info = new BranchInfo(1, label);
			result.setUserObject(info);
		}
		// temporarily show the whole tree, with pale transit nodes 
		Enumeration<DefaultMutableTreeNode> skeleton = top.breadthFirstEnumeration();
		while (skeleton.hasMoreElements()) {
			DefaultMutableTreeNode sourceItem = skeleton.nextElement();
			DefaultMutableTreeNode targetItem = (DefaultMutableTreeNode) sourceItem.getParent();
			if (targetItem == null || targetItem == top) continue; 	// top
			String sourceNode = resolvePath(sourceItem);
			String targetNode = resolvePath(targetItem);
			BranchInfo info = (BranchInfo) sourceItem.getUserObject();
			int key = info.getKey();
			String colorString = key == 0 ? "#f0f0f0" : "#808080";
			addNode(sourceNode, colorString);
			addNode(targetNode, colorString);
			addEdge(sourceNode, targetNode, false);
		}
	}
	
	public DefaultMutableTreeNode findOrCreate(Vector<String> zettel, boolean create) {
		DefaultMutableTreeNode previousLevel = top;
		DefaultMutableTreeNode foundNode = null;
		for (int level = 0; level < zettel.size(); level++) {
			String element = zettel.get(level);
//			BranchInfo info = (BranchInfo) previousLevel.getUserObject();
//			System.out.println (level + ": " + info.toString());
			Enumeration<DefaultMutableTreeNode> children = previousLevel.children();
			boolean found = false;
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode child = children.nextElement();
				BranchInfo elInfo = (BranchInfo) child.getUserObject();
				String elString = elInfo.toString();
				if (!elString.equals(element)) {
					continue;
				} else {
					previousLevel = child;
					found = true;
					foundNode = child;
					break;
				}
			}
			if (!found && create) {
				DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(new BranchInfo(0, element));
				previousLevel.add(newChild);
				previousLevel = newChild;
			}
			if (level == zettel.size() - 1) return foundNode;
		}
		return foundNode;
	}
	
	public String resolvePath(DefaultMutableTreeNode node) {
		if (node == null) return "(null)";
		Object[] path = node.getUserObjectPath();
		String label = "";
		for (int i = 1; i < path.length; i++) {
			BranchInfo info = (BranchInfo) path[i];
			String element = info.toString();
			label += element;
		}
		return label;
	}

	public void actionPerformed(ActionEvent arg0) {
		String command = arg0.getActionCommand();
//		if (command == "tree") {
//			tree = treeBox.isSelected();
//		} else if (command == "links") {
//			links = linksBox.isSelected();
//		}
			tree = treeBox.isSelected();
			links = linksBox.isSelected();
			if (command == "OK") {
			dialog.dispose();
		}
	}
}

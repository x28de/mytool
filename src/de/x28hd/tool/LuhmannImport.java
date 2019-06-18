package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
	Hashtable<Integer,GraphNode> threadsNodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> threadsEdges = new Hashtable<Integer,GraphEdge>();
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
	boolean hop = true;
	boolean threads = false;
	boolean print = false;
	boolean seq = true;
	String[] colorChanges = new String[7];
	HashSet<GraphEdge> colorDone = new HashSet<GraphEdge>();
	Hashtable<Integer,GraphNode> virtualNodes = new Hashtable<Integer,GraphNode>();
	File d = null;
	File t = null;
	
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
	JCheckBox threadsBox = null;
	JCheckBox hopBox = null;
	JCheckBox printBox = null;
	Hashtable<String,String> successors = new Hashtable<String,String>();
	TreeSet<String> printDone = new TreeSet<String>();
	GraphNode rootNode = null;
	FileWriter list = null;
	String newLine = System.getProperty("line.separator");
	String printOut = "";

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
		d = null;
		File h = null;
		t = null;

		JFileChooser chooser = new JFileChooser();
		
		// Ask for zettels folder
		chooser.setCurrentDirectory(b);
		chooser.setDialogTitle("Open a folder containing downloaded Zettels TEI files");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
        		"Select a zettels folder, optional", "folder");
        chooser.setFileFilter(filter);        
        
	    int returnVal = chooser.showOpenDialog(mainWindow);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	d = chooser.getSelectedFile();
	    } 
//		d = new File("c:\\Users\\Matthias\\Desktop\\luh");
	    
		// Ask for header file
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(b);
		chooser.setDialogTitle("Open a file containing header lines");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        filter = new FileNameExtensionFilter(
        		"A headers file (.txt), optional", "txt");
        chooser.setFileFilter(filter); 
        
	    returnVal = chooser.showOpenDialog(mainWindow);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	 h = chooser.getSelectedFile();
	    }
//		h = new File("c:\\Users\\Matthias\\Desktop\\hdrs.txt");
	    
		// Ask for threads file
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(b);
		chooser.setDialogTitle("Open a file containing threads branchings");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		String description = "A threads file (.opml)";
        if (d != null) description += ", optional";
        filter = new FileNameExtensionFilter(description, "opml");
        chooser.setFileFilter(filter); 
        
	    returnVal = chooser.showOpenDialog(mainWindow);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	 t = chooser.getSelectedFile();
	    }
//		t = new File("c:\\Users\\Matthias\\Desktop\\threads.opml");
	    
		
	    // Ask about options
		dialog = new JDialog(controler.getMainWindow(), "Change Options");
		dialog.setModal(true);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setMinimumSize(new Dimension(300, 470));
		dialog.setLocation(dim.width/2-dialog.getSize().width/2 - 298, 
				dim.height/2-dialog.getSize().height/2 - 209);		
		dialog.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		JPanel question1 = new JPanel();
		question1.setLayout(new BorderLayout());
		JLabel nextInfo = new JLabel("What do you want to visualize?");
		nextInfo.setBorder(new EmptyBorder(10, 10, 10, 10));
		question1.add(nextInfo, "North");
		
		treeBox = new JCheckBox ("The 'Nummer' tree", false);
		treeBox.setActionCommand("tree");
		treeBox.addActionListener(this);
		treeBox.setSelected(false);
		if (d == null) treeBox.setEnabled(false);
		
		linksBox = new JCheckBox ("The 'Verweisung' network", false);
		linksBox.setActionCommand("links");
		linksBox.addActionListener(this);
		if (d == null) links = false;
		linksBox.setSelected(links);
		linksBox.setEnabled(links);
		
		threadsBox = new JCheckBox ("The 'Strang' threads by the editors", false);
		threadsBox.setActionCommand("threads");
		threadsBox.addActionListener(this);
		if (d == null) {
			if (t == null) {
				controler.displayPopup("Specify one of:"
						+ "\n- a zettels folder "
						+ "\n- or a threads file."
						+ "\nThey cannot be both omitted.");
				return;
			}
			threads = true;
			threadsBox.setEnabled(false);
		}
		threadsBox.setSelected(threads);
		if (t == null) {
			threadsBox.setEnabled(false);
		}
		
		JPanel boxesContainer = new JPanel();
		boxesContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
		boxesContainer.setLayout(new GridLayout(3, 1));
		boxesContainer.add(treeBox);
		boxesContainer.add(linksBox);
		boxesContainer.add(threadsBox);
		question1.add(boxesContainer, "South");
		dialog.add(question1);
		
		JPanel question2 = new JPanel();
		question2.setLayout(new BorderLayout());
		question2.setMinimumSize(new Dimension(300, 70));
		JLabel nextInfo2 = new JLabel("More options");
		nextInfo2.setBorder(new EmptyBorder(10, 10, 10, 10));
		question2.add(nextInfo2, "North");
		
		printBox = new JCheckBox ("print a list", true);
		printBox.setActionCommand("print");
		printBox.addActionListener(this);
		printBox.setSelected(true);
		if (d == null) {
			printBox.setSelected(false);
			print = false;
			printBox.setEnabled(false);
		}
		JPanel boxesContainer2 = new JPanel();
		boxesContainer2.setBorder(new EmptyBorder(10, 10, 10, 10));
		boxesContainer2.setLayout(new GridLayout(1, 1));
		boxesContainer2.add(printBox);
		
		question2.add(boxesContainer2, "South");
		dialog.add(question2);
		
		JPanel question3 = new JPanel();
		question3.setLayout(new BorderLayout());
		JLabel nextInfo3 = new JLabel("<html>"
				+ "Should the links drive the selector on the map<br /> "
				+ "(= HyperHopping, which is not available in<br />"
				+ "the browser demo), or should they open up<br />"
				+ "browser pages on the archive site?<html>");
		nextInfo3.setBorder(new EmptyBorder(10, 10, 10, 10));
		question3.add(nextInfo3, "North");
		
		hopBox = new JCheckBox ("HyperHopping", true);
		hopBox.setActionCommand("hop");
		hopBox.addActionListener(this);
		hopBox.setSelected(true);
		if (d == null) {
			hopBox.setSelected(false);
			hop = false;
			hopBox.setEnabled(false);
		}
		JPanel boxesContainer3 = new JPanel();
		boxesContainer3.setBorder(new EmptyBorder(10, 10, 10, 10));
		boxesContainer3.setLayout(new GridLayout(1, 1));
		boxesContainer3.add(hopBox);
		
		question3.add(boxesContainer3, "South");
		dialog.add(question3);
		
		nextButton = new JButton("OK");
		nextButton.addActionListener(this);
		nextButton.setEnabled(true);
		JPanel buttonContainer = new JPanel();
		buttonContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
		buttonContainer.setLayout(new GridLayout(1, 1));
		buttonContainer.add(nextButton);
		
		dialog.add(buttonContainer);
		dialog.setVisible(true);
		
	    System.out.println("Tree " + tree + ", links " + links + ", threads " + 
	    		threads + ", hop " + hop + ", print " + print);
			
		if (h != null) loadHeaders(h.getPath());	// makes slow; switch off for testing
		File[] dirList = null;
		if (d != null) dirList = d.listFiles();

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
		if (threads) advice = "<html>Recommended next actions: <br /><br />"
				+ "You may delete the root node and the<br />"
				+ "colored cluster. Then scroll down<br />"
				+ "the red and black threads, or click<br />"
				+ "<b>Advanced > Zoom the map</b>";
		advice += "</html>";
		nextInfo = new JLabel(advice);
		nextInfo.setBorder(new EmptyBorder(10, 10, 10, 10));
		JPanel infoContainer = new JPanel();
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
		
		if (threads) {
			loadFile(t, true);
			new TreeImport(inputXml, controler, Importer.OPML, true);
		}
		
		if (print) {
		try {
			list = new FileWriter(baseDir + File.separator + "x28list.txt");
		} catch (IOException e2) {
			System.out.println("Error LI111 " + e2);
		}
		}
		
		if (dirList != null) {
		for (File f : dirList) {
			File file = f.getAbsoluteFile();
			loadFile(file, false);		// calls recordLink which adds nodes and edges
		}
		}
		if (tree || threads) hierarchy();
		
		Iterator<String> allEnds = inputs.iterator();
		while (allEnds.hasNext()) {
			String current = allEnds.next();
			count++;
			if (count % 9 == 0)
				dialog.setTitle("Loaded " + count + " items ...");
			addContent(current);
		}
		dialog.setTitle("Next actions:");
		
		if (print && threads) {
			
		// Check the sequence chain for missing zettels
		System.out.println("Successor table size: " + successors.size());
		
		Enumeration<String> predecessors = successors.keys();
		while (predecessors.hasMoreElements()) {
			String pred = predecessors.nextElement();
			while (successors.containsKey(pred)) {
				String succ = successors.get(pred);
				try {
					list.write(pred + "\t" + pred + newLine);
				} catch (IOException e) {
					System.out.println("Error LI112 " + e);
				}
				pred = succ;
			}
			if (printDone.contains(pred)) continue; 
			printDone.add(pred);
		}
		}
		
		if (print && links) {
			try {
				list.write(printOut);
			} catch (IOException e) {
				System.out.println("Error LI112a " + e);
			}
		}
		System.out.println("Stage 2: " + nodes.size() + " nodes, " + edges.size() + " edges");
		
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
    	
    	if (hop) {
    		controler.toggleHashes(true);
     	} else {
       		controler.toggleHyp(1, true);
    	}
	}
	
	public void loadFile(File file, boolean opml) {
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
		if (opml) return;
		
		filename = filename.substring(6);
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
		
		if (!threads || links) {	// ignores zettels without remote references
		NodeList refs = inputXml.getElementsByTagName("ref");
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
		
		if (seq) extractSuccessor(filename);
	}
	
	// To get the sequence
	public void extractSuccessor(String filename) {
		NodeList ptrs = inputXml.getElementsByTagName("ptr");
		for (int i = 0; i < ptrs.getLength(); i++) {
			Node ptr = ptrs.item(i);
			NamedNodeMap attr2 = ptr.getAttributes();
			if (attr2.getLength() > 0) {
				String type = ((Element) ptr).getAttribute("type");
				if (type.contains("naechste-vorderseite-im-zettelkasten")) {
					String ptrTarget = ((Element) ptr).getAttribute("target");
					ptrTarget = ptrTarget.replaceAll("_V$", "");
					if (ptrTarget.length() < 8) continue;
//					System.out.println(filename.substring(8) + "\t" + ptrTarget.substring(9) + "\n");
					if (successors.containsKey(filename.substring(8))) System.out.println("Complain " + filename);
					successors.put(filename.substring(8), ptrTarget.substring(9));
				}
			}
		}
	}
	
	public void loadHeaders(String filename) {
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
		if (links || threads) {
			addEdge(source, target, true);
			if (print && links) printOut += source + "\t" + target + newLine;
		}
	}

	public Vector<String> parseNummer(String id) {
		Vector<String> vector = new Vector<String>();
		String el = "";
		try {
			el = id.substring(0, 8); // "ZK_1_NB_"
		} catch (StringIndexOutOfBoundsException e) {
			controler.displayPopup("Problem with " + id + 
					"\nMaybe filename format not recognized."
					+ "\nMust be xxxxxxZK_n_NB_nn...n_V");
			controler.close();
		}
		vector.add(el);

		String 	rest = id.substring(8);
		String nextChar = "";
		String numString = "";
		for (int i = 0; i < rest.length(); i++) {
			nextChar = rest.substring(i, i + 1);
			if (nextChar.matches("[0-9]+")) {
				numString += nextChar; 
			} else {
				if (!numString.isEmpty()) vector.add(numString);
				if (nextChar.equals("-") || nextChar.equals("_")) {
					numString = nextChar;
				} else {
					numString = "";
					vector.add(nextChar);
				}
			}
		}
		if (!numString.isEmpty()) vector.add(numString);
//		System.out.println(id + " -> " + vector);
		return vector;
	}
	
	public void addNode(String nodeRef, String colorString) {
		addNode(nodeRef, colorString, false);
	}
	public void addNode(String nodeRef, String colorString, boolean onlyVirtual) {

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

		virtualNodes.put(id, topic);
		if (!onlyVirtual) nodes.put(id, topic);
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
			int prio1 = 4;
			int prio2 = 4;
			
			// Order seems to be: _, A-Z, -number, a-z, number 
			if (element1.startsWith("_")) prio1 = 0;
			if (element2.startsWith("_")) prio2 = 0;
			if (element1.matches("^[A-Z]+")) prio1 = 1;
			if (element2.matches("^[A-Z]+")) prio2 = 1;
			if (element1.startsWith("-")) prio1 = 2;
			if (element2.startsWith("-")) prio2 = 2;
			if (element1.matches("^[a-z]+")) prio1 = 3;
			if (element2.matches("^[a-z]+")) prio2 = 3;
			boolean numeric1 = true;
			boolean numeric2 = true;
			int num1 = 0;
			int num2 = 0;
			try {
				num1 = Integer.parseInt(element1);
			} catch (NumberFormatException e) {
				numeric1 = false;
			}
			try {
				num2 = Integer.parseInt(element2);
			} catch (NumberFormatException e) {
				numeric2 = false;
			}
			if (numeric1 && numeric2) {
				int compared = Integer.compare(Math.abs(num1), Math.abs(num2));
				if (compared == 0) continue;
				return compared;
			} else {
				int compared = Integer.compare(prio1, prio2);
				if (compared == 0) {
					compared = element1.compareTo(element2);
				}
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
						if (hop) {
							target = target.replaceAll("_V$", "");
						} else {
							target = "https://niklas-luhmann-archiv.de/bestand/zettelkasten/zettel/" 
							+ target.substring(1);
						}
					} else ref = false;
				}
				if (!target.isEmpty()) {
					if (hop) {
						anchorString = "<a href=\"" + 
								"#" + target.substring(9) + "\">";
					} else {
						anchorString = "<a href=\"" + 
								target + "\">";
					}
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
		
		System.out.println("Stage 1: " + nodes.size() + " nodes, " + edges.size() + " edges");
		
		// copy "threads" by labels from an OPML file first
		// (they play, for once,  the role of Verweisungen links). Use with an empty 
		// zettels folder.
		if (threads) {
			threadsNodes = controler.getNodes();
			threadsEdges = controler.getEdges();
			Enumeration<GraphNode> opmlNodes = threadsNodes.elements();
			while (opmlNodes.hasMoreElements()) {
				GraphNode node = opmlNodes.nextElement();
				String nodeRef = node.getLabel();
				if (nodeRef.equals("ROOT")) nodeRef ="";
				nodeRef = "ZK_1_NB_" + nodeRef;
				addNode(nodeRef, "");
			}
			Enumeration<GraphEdge> opmlEdges = threadsEdges.elements();
			while (opmlEdges.hasMoreElements()) {
				GraphEdge edge = opmlEdges.nextElement();
				GraphNode node1 = edge.getNode1();
				GraphNode node2 = edge.getNode2();
				String source = node1.getLabel();
				String target = node2.getLabel();
				if (source.equals("ROOT")) source = "";
				// TODO deal here with exception 1a
				source = "ZK_1_NB_" + source;
				target = "ZK_1_NB_" + target;
				recordLink(source, target);
			}
		}
		
		// Build the hierarchy
		top = new DefaultMutableTreeNode(new BranchInfo(0, ""));
		addNode("ZK_1_NB_", "#00ffff");
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
		
		// optionally show the whole tree, with pale transit nodes 
		Enumeration<DefaultMutableTreeNode> skeleton = top.breadthFirstEnumeration();
		while (skeleton.hasMoreElements()) {
			DefaultMutableTreeNode sourceItem = skeleton.nextElement();
			if (sourceItem == top) continue;
			DefaultMutableTreeNode targetItem = (DefaultMutableTreeNode) sourceItem.getParent();
			String sourceNode = resolvePath(sourceItem);
			String targetNode = resolvePath(targetItem);
			BranchInfo info = (BranchInfo) sourceItem.getUserObject();
			int key = info.getKey();
			String colorString = key == 0 ? "#f0f0f0" : "#808080";
			addNode(sourceNode, colorString, threads && !tree);
			addNode(targetNode, colorString, threads && !tree);
			if (!threads || tree) addEdge(sourceNode, targetNode, false);

		}
		
		if (!threads) return;
		drawTree(top, new Point(40, 40));
		
		//	Coloring the threads 
		TreeMap<Integer,GraphNode> yMap = new TreeMap<Integer,GraphNode>();
		SortedMap<Integer,GraphNode> yList = (SortedMap<Integer,GraphNode>) yMap;
		int rootNum = inputs2num.get("ZK_1_NB_");
		rootNode = nodes.get(rootNum);
		
		int excepID = inputs2num.get("ZK_1_NB_1a");		// cheating due to exception
		GraphNode excepNode = nodes.get(excepID);
		Point topXY = rootNode.getXY();
		excepNode.setXY(new Point(topXY.x + 600, 160));
		
		Enumeration<GraphEdge> edgeList = rootNode.getEdges();
		while (edgeList.hasMoreElements()) {
			GraphEdge edge = edgeList.nextElement();
			GraphNode threadStart = rootNode.relatedNode(edge);
			Point xy = threadStart.getXY();
			int y = xy.y;
			yMap.put(y,  threadStart);
		}
		SortedSet<Integer> ySet = (SortedSet<Integer>) yList.keySet();
		Iterator<Integer> iter = ySet.iterator();
		boolean alternate = true;
		while (iter.hasNext()) {
			int y = iter.next();
			GraphNode threadStart = yMap.get(y);
			alternate = !alternate;
			String colorString = alternate ? "#ff0000" : "#000000";
			String indent = "";
			colorthread(threadStart, colorString, indent);
			Enumeration<GraphEdge> cleanupList = threadStart.getEdges();
			while (cleanupList.hasMoreElements()) {
				GraphEdge neighborEdge = cleanupList.nextElement();
				GraphNode otherEnd = threadStart.relatedNode(neighborEdge);
				if (otherEnd.equals(rootNode)) {
//				neighborEdge.setColor("#00ffff");
				threadStart.removeEdge(neighborEdge);
				threadsEdges.remove(neighborEdge);
				}
			}
		}
	}
	
	public DefaultMutableTreeNode findOrCreate(Vector<String> zettel, boolean create) {
		DefaultMutableTreeNode previousLevel = top;
		DefaultMutableTreeNode foundNode = null;
		for (int level = 0; level < zettel.size(); level++) {
			String element = zettel.get(level);
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
		Object[] path = node.getUserObjectPath();
		String label = "";
		for (int i = 0; i < path.length; i++) {
			BranchInfo info = (BranchInfo) path[i];
			String element = info.toString();
			label += element;
		}
		return label;
	}
	
	public int drawTree(DefaultMutableTreeNode node, Point xy) {
		String nodeRef = resolvePath(node);
		if (!inputs2num.containsKey(nodeRef)) {
			System.out.println("Error LI104 " + nodeRef);	// ZK 2 ?
			return xy.y;
		}
		int nodeID = inputs2num.get(nodeRef);
		GraphNode gnode = virtualNodes.get(nodeID);
		if (!virtualNodes.containsKey(nodeID)) {
			System.out.println(nodeRef + " not in virtualNodes");
		} else {
			gnode.setXY(xy);
		}

		Enumeration<DefaultMutableTreeNode> children = node.children();
		TreeMap<String,DefaultMutableTreeNode> childrenMap = 
				new TreeMap<String,DefaultMutableTreeNode>(this);
		SortedMap<String,DefaultMutableTreeNode> childrenList =  
				(SortedMap<String,DefaultMutableTreeNode>) childrenMap;

		while (children.hasMoreElements()) { 
			DefaultMutableTreeNode child = children.nextElement();
			BranchInfo elInfo = (BranchInfo) child.getUserObject();
			String elString = elInfo.toString();
			String fullRef = nodeRef + elString; 	// comparator parses beyond 8
			childrenMap.put(fullRef, child);
		}
		SortedSet<String> childrenSet = (SortedSet<String>) childrenList.keySet();
		Iterator<String> childrenOrder = childrenSet.iterator();
		int x = xy.x;
		x += 300;
		int y = xy.y;
		int returnY = y;
		while (childrenOrder.hasNext()) {
			String fullString = childrenOrder.next();
			DefaultMutableTreeNode child = childrenMap.get(fullString);
			y = drawTree(child, new Point(x, y));	// recursion
			returnY = y;
			y += 40;
		}
		return returnY;
	}
	
	public void colorthread(GraphNode node, String colorString, String indent) {
		Enumeration<GraphEdge> edgeList = node.getEdges();
		while (edgeList.hasMoreElements()) {
			GraphEdge neighborEdge = edgeList.nextElement();
			if (colorDone.contains(neighborEdge)) continue;
			neighborEdge.setColor(colorString);
			colorDone.add(neighborEdge);
			GraphNode downStreamNode = node.relatedNode(neighborEdge);
			if (downStreamNode.equals(rootNode)) continue;
			colorthread(downStreamNode, colorString, indent + "  "); 	// recursion
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		String command = arg0.getActionCommand();
			tree = treeBox.isSelected();
			links = linksBox.isSelected();
			threads = threadsBox.isSelected();
			hop = hopBox.isSelected();
			print = printBox.isSelected();
			// either threads (t required) or links/tree (d required)
			if (command == "threads") {
				if (threadsBox.isSelected()) {
					linksBox.setSelected(false);
					links = false;
					treeBox.setSelected(false);
					tree = false;
				}
				linksBox.setEnabled(!threads && !(d == null));
				treeBox.setEnabled(!threads && !(d == null));
			} else if (command == "links") {
				if (linksBox.isSelected()) {
					threadsBox.setSelected(false);
					threads = false;
				}
				threadsBox.setEnabled(!links && !(t == null));
			} else if (command == "tree") {
				if (treeBox.isSelected()) {
					threadsBox.setSelected(false);
					threads = false;
				}
				threadsBox.setEnabled(!tree && !(t == null));
				printBox.setSelected(!tree);
				print = !tree;
				printBox.setEnabled(!tree);
			}
			if (command == "OK") {
			dialog.dispose();
		}
	}
}

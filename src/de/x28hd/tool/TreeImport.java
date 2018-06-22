package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
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

public class TreeImport extends SwingWorker<Void, Void> implements ActionListener, PropertyChangeListener {
	
	// Main fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	
	//	Input file list sorting 
	Hashtable<String,String> byPaths = new Hashtable<String,String>();;
	TreeMap<String,String> pathsMap = new TreeMap<String,String>();
	SortedMap<String,String> pathsList = (SortedMap<String,String>) pathsMap;
	TreeMap<Long,Integer> datesMap = new TreeMap<Long,Integer>();
	SortedMap<Long,Integer> datesList = (SortedMap<Long,Integer>) datesMap;
	
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
	Hashtable<Integer,String> nodeDetails = new Hashtable<Integer,String>();
	Hashtable<Integer,Long> nodeDates = new Hashtable<Integer,Long>();
	Hashtable<Integer,String> edgeColors = new Hashtable<Integer,String>();
	String[] legend = new String[6];
	
	JTree tree;
	JFrame frame;
	JFrame progressFrame;
	File file;
	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			finish();
		}
	};
	
	// Accessories for progress bar
	public Void doInBackground() {
		setProgress(0);
		try {
		loadStuff(file, controler, knownFormat);
		} catch (Exception ex)  {
			System.out.println("Error TI113 ");
			ex.printStackTrace();
		}

		return null;
	}
	public void done() {
		progressFrame.dispose();
		commonPart();
	}
	
	int j = -1;
	int readCount = 0;
	int edgesNum = 0;
    DefaultMutableTreeNode top;
    String htmlOut = "";
	JPanel radioPanel = null;
	boolean transit = false;
	JCheckBox transitBox = null;
	boolean layoutOpt = false;
	JCheckBox layoutBox = null;
	boolean colorOpt = false;
	JCheckBox colorBox = null;
	boolean legendOpt = false;
	JCheckBox legendBox = null;
	boolean hypOpt = true;
	JCheckBox hypBox = null;
	int relID = -1;
	boolean extended = false;
	boolean windows = false;
	boolean showJTree = true;
	int monitor = 0;
	int myProgress = 0;
	int alternate = -1;
	
	//	Constants
	int maxVert = 10;
	String[] colors = {
			"#ff0000",
			"#ffaa00",
			"#ffff00",
			"#00ff00",
			"#0000ff",
			"#b200b2"};
	String[] colors2 = {
			"#ffbbbb",
			"#ffe8aa", 
			"#ffff99", 
			"#bbffbb", 
			"#bbbbff", 
			"#d2bbd2"};
	String fs = "";
	
	//	Overriden later
	int knownFormat = 12;	//	OPML
	String topNode = "body";
	String nestNode = "outline";
	String labelAttr = "text";
	String idAttr = "";
	JProgressBar progressBar;
	
	public TreeImport(File file, GraphPanelControler controler, int knownFormat) {
		this.file = file;
		this.controler = controler;
		this.knownFormat = knownFormat;
		showJTree = false;
		layoutOpt = true;

		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		JPanel panel = new JPanel();
		panel.add(progressBar);

		progressFrame = new JFrame("Loading ...");
		progressFrame.setLocation(100, 170);
		progressFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progressFrame.addWindowListener(myWindowAdapter);
		progressFrame.setLayout(new BorderLayout());
		progressFrame.add(panel, BorderLayout.PAGE_START);

		progressFrame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		progressFrame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
		progressFrame.setMinimumSize(new Dimension(596, 418));

		progressFrame.setVisible(true);
		controler.stopHint();

		addPropertyChangeListener(this);	//	updates progress bar when setProgress()
		execute();							//	calls loadStuff() via doInBackground()
	}
	
	public void loadStuff(File file, GraphPanelControler controler, int knownFormat) {
        
		extended = controler.getExtended();
		windows = (System.getProperty("os.name").startsWith("Windows"));
		
		if (knownFormat == 17) {	//	Filepaths list
			colorOpt = true;
			fs = System.getProperty("file.separator");
			topNode = file.getAbsolutePath();
			topNode = createRelatedNode(topNode, false);
			int topNum = inputID2num.get(topNode);
			top = new DefaultMutableTreeNode(new BranchInfo(topNum, file.getName()));
			myProgress = 5;
			setProgress(myProgress);
			SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
			
		    fileTree(file, topNode, top, 0);
		    
		    String content2 = nodeDetails.get(topNum);
		    String moreDetail = "<html><body>Open folder <a href=\"" + file.toURI().toString()  + "\">" + file.getName() + 
		    		"</a>" + "<br />" + content2 + "<br /></body></html>";
		    GraphNode node = nodes.get(topNum);
		    node.setDetail(moreDetail);
		    
		    // Prepare coloring the folders by age 
		    // (by hierarchy is done via addEdge) 			TODO make more transparent
		    
 			SortedSet<Long> datesSet = (SortedSet<Long>) datesList.keySet();
 			Iterator<Long> ixit = datesSet.iterator(); 
 			if (datesSet.size() > 0) {
 				int rangeSize = (datesSet.size() / 6) + 1;
				int counter = datesSet.size() - 1;
 				int previousCol = -1;
 				
 				while (ixit.hasNext()) {
  					int colID = counter / rangeSize;
					counter--;
 					Long modDate = ixit.next();
 					int nodeNum = datesList.get(modDate);
 					
 					nodeColors.put(nodeNum, colors[colID]);
 					
 					//	Legend item
 					Date date = new Date(modDate);
 					String dateText = df2.format(date);
 					if (colID != previousCol) {
 						String label = dateText + " +";
 						if (colID == 0) label = label + " (newest)";
 						if (colID == 5) label = label + " (oldest)";
 						legend[colID] = label;
 						previousCol = colID;
 					}
 				}
 			}
		}
		
		//	Collect relationships
		Enumeration<Integer> relEnum = relationshipFrom.keys();
		while (relEnum.hasMoreElements()) {
			Integer relID = relEnum.nextElement();
			String fromRef = relationshipFrom.get(relID);
			String toPath = relationshipTo.get(relID);
			String toRef = createRelatedNode(toPath, false);
			addEdge(fromRef, toRef, true, true, "");
		}
	}
	
	public void fileTree(File file, String parentID, DefaultMutableTreeNode parentInTree,
			int level) {
	
		File[] dirList = file.listFiles();
		String content = "";
		Long modDate = 0L;
		Long maxModDate = 0L;
		SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy");

		if (dirList != null) {
		for (File f : dirList) {
	        
			if (f.isHidden()) continue;
			content = content + "<br /><a href=\"" + f.toURI().toString() + "\">" + f.getName() + "</a>";
			
			//	For progress bar (slow shortcut lookup)
			monitor++;
			if (monitor % 250 > 248) {
				if (myProgress < 97) {
					myProgress = myProgress + (int) (.3 * (100 - myProgress));
				} else {	// keep the propertyChange firing
					myProgress = 98 + alternate;
					alternate = -alternate;
				}
				setProgress(myProgress);
			}
			
			String id = readCount++ + "";
			String desti = "";

			if (f.isDirectory()) {
				if (!windows) {	// Maybe symlink
					try {
						desti = f.getCanonicalPath();
					} catch (IOException e) {
						System.out.println("Error TI111 " + e.getMessage());
						continue;
					}
					if (!desti.equals(f.getAbsolutePath())) {
						
						relID++;
						relationshipFrom.put(relID, parentID);
						relationshipTo.put(relID, desti);
						continue;
					}
				} 
				
				//	add folder node
				String label = f.getName();
				inputItems.put(id, label);
				byPaths.put(id, f.getAbsolutePath());
				String detail = "<html><body>Open folder <a href=\"" + f.toURI().toString()  + "\">" + f.getName() + "</a></body></html>";
				addNode(id, detail);
				int nodeNum= inputID2num.get(id);
				
				DefaultMutableTreeNode branch = 
						new DefaultMutableTreeNode(new BranchInfo(nodeNum, label));
				parentInTree.add(branch);

				// recurse
				fileTree(f, id, branch, level + 1);
				
				// specify folder details (leaf entries and their newest date)
		        String content2 = nodeDetails.get(nodeNum);
		        Long modDate2 = nodeDates.get(nodeNum);
		        Date date = new Date(modDate2);
		        String dateText = df2.format(date);
				String moreDetail = "<html><body>Open folder <a href=\"" + f.toURI().toString()  + "\">" + f.getName() + 
						"</a> (" + dateText + ") <br />" + content2 + "<br /></body></html>";
				GraphNode node = nodes.get(nodeNum);
				node.setDetail(moreDetail);

				// add color and edge
				String edgeColor = "";
					nodeColors.put(nodeNum, colors[level % 6]);
					edgeColor = colors2[level % 6];
				addEdge(parentID, id, false, edgeColor);

			} else { // not a directory
				
				modDate = f.lastModified();
				if (modDate > maxModDate) maxModDate = modDate;
				
				if (windows) {	// maybe shortcut
					if (!f.getName().endsWith(".lnk")) continue;
					try {
						WindowsShortcut ws = new WindowsShortcut(f);

						// record shortcut, skip leaf nodes
						if (!ws.isPotentialValidLink(f)) continue;
						if (!ws.isDirectory()) continue;
						desti = ws.getRealFilename();

						relID++;
						relationshipFrom.put(relID, parentID);
						relationshipTo.put(relID, desti);
					} catch (IOException e) {
						System.out.println("Error TI104 " + e.getMessage());
						continue;
					} catch (ParseException e) {
						if (!e.toString().endsWith("magic is missing")) {
							System.out.println("Error TI105 " + f.getAbsolutePath() + " " + e.getMessage());
						}
						continue;
					}
				}
			}
		}
		}
		
		// details for parent
		int parentNum = inputID2num.get(parentID);
		nodeDetails.put(parentNum, content);
		if (!file.isDirectory()) return;
		if (maxModDate <= 0L) maxModDate = file.lastModified(); // only last resort
		
		while (datesMap.containsKey(maxModDate)) maxModDate++;
		datesMap.put(maxModDate, parentNum);
		nodeDates.put(parentNum, maxModDate);
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
			String detail = "<html><body>Open folder <a href=\"" + f.toURI().toString()  + "\">" + desti + "</a></body></html>";
			addNode(destID, detail, desti != topNode);
			
			fromRef = createRelatedNode(ancestors, false);	// recurse
			
			addEdge(fromRef, destID, false, true, "");
			if (!f.exists()) {
				int num = inputID2num.get(destID);
				GraphNode node = nodes.get(num);
				node.setColor("#000000");
				nodeColors.put(num, "#000000");
			}
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
					if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
					if (path.indexOf("/") < 0) continue;
					inputItems.put(id, path.substring(path.lastIndexOf("/") + 1));
					byPaths.put(id, path);
					pathsMap.put(path, id);
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
			treeColor = colors[level % 6];
			int nodeNum = inputID2num.get(toRef);
			nodeColors.put(nodeNum, treeColor);
		
		addEdge(fromRef, toRef, false, treeColor);
	}
	
	public void commonPart() {
		
		if (knownFormat == 18) { // not available for sitemap 
			showJTree = false;
		}
		layoutOpt = !showJTree;
		
//
//		Create a JTree 
	    
	    DefaultTreeModel model = new DefaultTreeModel(top);
	    controler.setTreeModel(model);
		
	    tree = new JTree(model);
	    
	    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	    
        frame = new JFrame("Options");
        frame.setLocation(100, 170);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(myWindowAdapter);
		frame.setLayout(new BorderLayout());
		if (showJTree) {
			frame.add(new JScrollPane(tree));
			frame.setTitle("Found this tree structure:");
		}

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
		transitBox = new JCheckBox ("Just for re-export", false);
		transitBox.setActionCommand("transit");
		transitBox.addActionListener(this);
		if (knownFormat == 18) { 
			instruction.setEnabled(false);
			transitBox.setEnabled(false);
			JLabel sorry = new JLabel("<html><em> (Sorry, currently unavailable)</em></html>");
			toolbar.add(sorry, "Center");
		}
		toolbar.add(transitBox, "West");
		hypBox = new JCheckBox ("Turns hyperlinks on but text editing off", true);
		hypBox.setActionCommand("hyp");
		hypBox.addActionListener(this);
		if (!showJTree) toolbar.add(hypBox, "East");
		JPanel toolbar2 = new JPanel();
		toolbar2.setLayout(new BorderLayout());
		JPanel optics = new JPanel();
		optics.setLayout(new FlowLayout());
		layoutBox = new JCheckBox ("Tree layout", layoutOpt);
		layoutBox.addActionListener(this);
		optics.add(layoutBox);
		colorBox = new JCheckBox ("Node color by change date", true);
		colorBox.setActionCommand("colorDate");
		colorBox.addActionListener(this);
		optics.add(colorBox);
		legendBox = new JCheckBox ("Show legend", false);
		legendBox.addActionListener(this);
		optics.add(legendBox);
		if (knownFormat != 17) {	// folder tree
			colorBox.setVisible(false);
			legendBox.setVisible(false);
		}
		toolbar2.add(buttons,"East");
		toolbar2.add(optics, "West");
		toolbar.add(toolbar2, "South");

		frame.add(toolbar,"South");
        frame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
        frame.setMinimumSize(new Dimension(596, 418));

        frame.setVisible(true);
        controler.stopHint();
	}
//		
//		Pass on the new map

	public void finish() {
		if (legendOpt) {
			String legendID = readCount++ + "";
			inputItems.put(legendID, "Legend");
			addNode(legendID, "Look at the colored nodes connected to this one, "
					+ "<br />from red (newest) to purple (oldest). "
					+ "<br /><br />Black icons are dead shortcuts (maybe just character variations)."
					+ "<br />Grey icons are parents."
					+ "<br /><br />(Note that edge colors don't reflect change dates "
					+ "but just hierarchy levels.)", true);
			for (int colID = 0; colID < 6; colID++) {
				String id2 = readCount++ + "";
				String label = legend[colID];
				if (label == null) break;
				inputItems.put(id2, label);
				addNode(id2, "Folders modified from " + legend[colID] + " are shown in this color", true);
				addEdge(legendID, id2, false, true, "");
				int nodeNumL = inputID2num.get(id2);
				nodeColors.put(nodeNumL, colors[colID]);
			}
		}
		if (transit) { 
			if (knownFormat == 17) {
				Iterator<GraphEdge> xrefTreeIter = xrefTreeEdges.iterator();
				while (xrefTreeIter.hasNext()) {
					GraphEdge edge = xrefTreeIter.next();
					int id = edge.getID();
					edges.remove(id);
				}
				Iterator<GraphNode> xrefTreeIter2 = xrefTreeNodes.iterator();
				while (xrefTreeIter2.hasNext()) {
					GraphNode node = xrefTreeIter2.next();
					int id = node.getID();
					nodes.remove(id);
				}
			}
			controler.setNonTreeEdges(nonTreeEdges);
			controler.replaceByTree(nodes, edges);
		} else {
			if (layoutOpt) {

				CentralityColoring centralityColoring = new CentralityColoring(nodes, edges);
				centralityColoring.treeLayout(nonTreeEdges);
				//	Recolor 
				Enumeration<Integer> edgeColEnum = edgeColors.keys();
				while (edgeColEnum.hasMoreElements()) {
					int key = edgeColEnum.nextElement();
					String treeColor = edgeColors.get(key);
					GraphEdge edge = edges.get(key);
					edge.setColor(treeColor);
				}
				Enumeration<Integer> nodeColEnum = nodeColors.keys();
				while (nodeColEnum.hasMoreElements()) {
					int key = nodeColEnum.nextElement();
					String treeColor = nodeColors.get(key);
					GraphNode node = nodes.get(key);
					node.setColor(treeColor);
				}
				if (!colorOpt) {
					//	Color nodes like edges end
					Enumeration<Integer> edgesEnum = edges.keys();
					while (edgesEnum.hasMoreElements()) {
						int edgeNum = edgesEnum.nextElement();
						GraphEdge edge = edges.get(edgeNum);
						if (nonTreeEdges.contains(edge)) continue;
						if (xrefTreeEdges.contains(edge)) continue;
						GraphNode node2 = edge.getNode2();
						if (edgeColors.containsKey(edgeNum)) {
							String colString = edgeColors.get(edgeNum);
							node2.setColor(colString);
						}
					}
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
			String treeColor = "";
			if (childcount > 0) {
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
			newEdgeColor = "#ffff00";
			treeColor = "#f0f0f0";
		}
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
		if (!treeColor.isEmpty()) edgeColors.put(edgesNum, treeColor);
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
			
		// Options interdependent
		} else if (command == "transit"){
			transit = transitBox.isSelected();
			layoutBox.setEnabled(!transit);
			colorBox.setEnabled(!transit);
			legendBox.setEnabled(!transit);
			frame.repaint();
			return;
		} else if (command == "Tree layout"){
			layoutOpt = layoutBox.isSelected();
			colorBox.setEnabled(layoutOpt);
			legendBox.setEnabled(layoutOpt);
			return;
		} else if (command == "colorDate"){
			colorOpt = colorBox.isSelected();
			legendBox.setEnabled(colorOpt);
			return;
		} else if (command == "Show legend"){
			legendOpt = legendBox.isSelected();
			return;
		} else if (command == "hyp"){
			hypOpt = hypBox.isSelected();
			return;
		}
        frame.setVisible(false);
        frame.dispose();
        finish();
		if (hypOpt) controler.toggleHyp(1, true);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
            progressBar.setString(monitor + "");
        } 
	}
}

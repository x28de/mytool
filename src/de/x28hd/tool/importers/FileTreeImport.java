package de.x28hd.tool.importers;

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
import java.io.File;
import java.io.IOException;
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

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.transform.TransformerConfigurationException;

import org.stackoverflowusers.file.WindowsShortcut;
import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.BranchInfo;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;
import de.x28hd.tool.layouts.CentralityColoring;

public class FileTreeImport implements ActionListener {

	// Main fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();

	//	Input file list sorting 
	Hashtable<String,String> byPaths = new Hashtable<String,String>();;
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
	PresentationService controler;
	Hashtable<Integer,String> nodeColors = new Hashtable<Integer,String>();
	Hashtable<Integer,String> nodeDetails = new Hashtable<Integer,String>();
	Hashtable<Integer,Long> nodeDates = new Hashtable<Integer,Long>();
	Hashtable<Integer,String> edgeColors = new Hashtable<Integer,String>();
	String[] legend = new String[6];

	// for mess
	TreeMap<Double,Integer> messMap = new TreeMap<Double,Integer>();
	SortedMap<Double,Integer> messList = (SortedMap<Double,Integer>) messMap;
	Double disambig = .0001;

	JTree tree;
	DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	JDialog frame;
	JFrame progressFrame;
	File file;
	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			finish();
		}
	};

	int j = -1;
	int readCount = 0;
	int edgesNum = 0;
	DefaultMutableTreeNode top;
	boolean transit = false;
	JCheckBox transitBox = null;
	boolean layoutOpt = false;
	JCheckBox layoutBox = null;
	boolean colorOpt = false;
	JCheckBox colorBox = null;
	boolean messOpt = false;
	JCheckBox messBox = null;
	boolean legendOpt = false;
	JCheckBox legendBox = null;
	boolean hypOpt = true;
	JCheckBox hypBox = null;
	int relID = -1;
	boolean windows = false;
	boolean silent = false;
	boolean silent2 = false;	// no progress bar
	boolean cancelled = false;
	int monitor = 0;
	int myProgress = 0;

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
	String topNode = "";

	public FileTreeImport(File file, PresentationService controler, int knownFormat) {
		new FileTreeImport(file, controler, knownFormat, false);
	}
	public FileTreeImport(File file, PresentationService controler, int knownFormat, boolean silent) {
		this.silent = silent;
		this.silent2 = silent;
		this.file = file;
		this.controler = controler;
		layoutOpt = true;

		progressFrame = new JFrame("Loading ...");
		progressFrame.setLocation(100, 170);
		progressFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progressFrame.addWindowListener(myWindowAdapter);
		progressFrame.setLayout(new BorderLayout());

		progressFrame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		progressFrame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
		progressFrame.setMinimumSize(new Dimension(596, 418));

		if (!silent2) progressFrame.setVisible(true);
		controler.getControlerExtras().stopHint();

		// Load stuff

		windows = (System.getProperty("os.name").startsWith("Windows"));
		fs = System.getProperty("file.separator");
		topNode = file.getName();
		topNode = createRelatedNode(topNode);
		int topNum = inputID2num.get(topNode);
		top = new DefaultMutableTreeNode(new BranchInfo(topNum, file.getName()));
		myProgress = 5;

		// Drill down
		fileTree(file, topNode, top, 0);

		String content2 = nodeDetails.get(topNum);
		String moreDetail = "<html><body>Open folder <a href=\"" + file.toURI().toString()  + "\">" + file.getName() + 
				"</a>" + "<br />" + content2 + "<br /></body></html>";
		GraphNode node = nodes.get(topNum);
		node.setDetail(moreDetail);

		progressFrame.dispose();
		commonPart();	
	}

	public void fileTree(File file, String parentID, DefaultMutableTreeNode parentInTree,
			int level) {

		int folderCounter = 0;
		int leafCounter = 0;
		File[] dirList = file.listFiles();
		String content = "";
		Long modDate = 0L;
		Long maxModDate = 0L;
		SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy");

		if (dirList != null) {
			for (File f : dirList) {

				if (f.isHidden()) continue;
				content = content + "<br /><a href=\"" + f.toURI().toString() + "\">" + f.getName() + "</a>";

				//	For progress monitoring (slow shortcut lookup)
				monitor++;
				if (monitor % 250 == 0) progressFrame.setTitle("Loading: " + monitor + " files");

				String id = readCount++ + "";
				String desti = "";

				if (f.isDirectory()) {
					folderCounter++;
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

					// add hierarchy color and edge
					String edgeColor = "";
					nodeColors.put(nodeNum, colors[level % 6]);
					edgeColor = colors2[level % 6];
					addEdge(parentID, id, false, edgeColor);

				} else { // not a directory

					leafCounter++;
					modDate = f.lastModified();
					if (modDate > maxModDate) maxModDate = modDate;

					if (windows) {	// maybe shortcut
						if (!f.getName().endsWith(".lnk")) continue;
						try {
							WindowsShortcut ws = new WindowsShortcut(f);

							// record shortcut, skip leaf nodes
							if (!WindowsShortcut.isPotentialValidLink(f)) continue;
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
		content = "<br>" + folderCounter + " folders, " + leafCounter + " files<br>" + content;
		nodeDetails.put(parentNum, content);
		if (!file.isDirectory()) return;
		if (maxModDate <= 0L) maxModDate = file.lastModified(); // only last resort

		while (datesMap.containsKey(maxModDate)) maxModDate++;
		datesMap.put(maxModDate, parentNum);
		nodeDates.put(parentNum, maxModDate);

		double uniq = 0.;
		uniq = ((double) leafCounter);
		if (leafCounter > 20 && similarExtensions(file)) {
			System.out.println("  " + file.getName() + " not marked as mess");
			uniq = 1;
		}
		while (messMap.containsKey(uniq)) uniq += disambig;
		messMap.put(uniq, parentNum);

	}

	public String createRelatedNode(String desti) {
		String fromRef = "";
		if (byPaths.containsValue(desti)) {
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

			fromRef = createRelatedNode(ancestors);	// recurse

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

	public void commonPart() {
		layoutOpt = true;

		// Create a JTree 

		DefaultTreeModel model = new DefaultTreeModel(top);
		controler.getControlerExtras().setTreeModel(model);

		frame = new JDialog(controler.getMainWindow(), "Options", true);
		frame.setLocation(100, 170);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(myWindowAdapter);
		frame.setLayout(new BorderLayout());

		Icon myLeaf = renderer.getOpenIcon();
		UIManager.put("Tree.leafIcon", myLeaf);		// leaf entries here are folders, too

		tree = new JTree(model);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		frame.add(new JScrollPane(tree));
		frame.setTitle("Found this tree structure:");

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
		toolbar.add(transitBox, "West");
		hypBox = new JCheckBox ("Turns hyperlinks on but text editing off", true);
		hypBox.setActionCommand("hyp");
		hypBox.addActionListener(this);
		toolbar.add(hypBox, "East");
		JPanel toolbar2 = new JPanel();
		toolbar2.setLayout(new BorderLayout());
		JPanel optics = new JPanel();
		optics.setLayout(new FlowLayout());
		layoutBox = new JCheckBox ("Tree layout", layoutOpt);
		layoutBox.addActionListener(this);
		optics.add(layoutBox);
		colorBox = new JCheckBox ("Node color by change date", false);
		colorBox.setActionCommand("colorDate");
		colorBox.addActionListener(this);
		optics.add(colorBox);
		messBox = new JCheckBox ("Node color by mess", false);
		messBox.setToolTipText("Many files outside of folders?");
		messBox.setActionCommand("colorMess");
		messBox.addActionListener(this);
		optics.add(messBox);
		legendBox = new JCheckBox ("Show legend", false);
		legendBox.setEnabled(false);
		legendBox.addActionListener(this);
		optics.add(legendBox);
		toolbar2.add(buttons,"East");
		toolbar2.add(optics, "West");
		toolbar.add(toolbar2, "South");

		frame.add(toolbar,"South");
		frame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
		frame.setMinimumSize(new Dimension(596, 418));

		if (!silent) {
			frame.setVisible(true);
		} else {
			layoutOpt = true;
			finish();
		}
		controler.getControlerExtras().stopHint();
	}

	public void finish() {
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
				SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
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

		// Prepare coloring the folders by mess 
		if (messOpt) {
			SortedSet<Double> messSet = (SortedSet<Double>) messList.keySet();
			Iterator<Double> iter = messSet.iterator();
			int m = 0;
			while (iter.hasNext()) {
				double pos = iter.next();
				int nodeID = messMap.get(pos);
				GraphNode messNode = nodes.get(nodeID);
				m++;
				if (m <= messMap.size() * .90) continue;
				if (m > messMap.size() * .90) messNode.setColor("#ffff00");
				if (m > messMap.size() * .95) messNode.setColor("#ffc800");
				if (m > messMap.size() * .97) messNode.setColor("#ff0000");
				Color color = messNode.getColor();
				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();
				String colorString = String.format("#%02x%02x%02x", r, g, b);
				colorPath(messNode, colorString);
			}
		}

		if (!messOpt ) {
			//	Collect relationships
			Enumeration<Integer> relEnum = relationshipFrom.keys();
			while (relEnum.hasMoreElements()) {
				Integer relID = relEnum.nextElement();
				String fromRef = relationshipFrom.get(relID);
				String toPath = relationshipTo.get(relID);
				String toRef = createRelatedNode(toPath);
				addEdge(fromRef, toRef, true, true, "");
			}
		}

		if (legendOpt) {
			if (colorOpt) {
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
			} else if (messOpt) {
				String legendID = readCount++ + "";
				inputItems.put(legendID, "Legend");
				addNode(legendID, "Look at the colored nodes connected to this one, "
						+ "<br />from red (worst) to yellow (modest). "
						+ "<br />Mess is where many files are outside folders."
						+ "<br /><br />(Folders with many files of the same 2 types "
						+ "are not marked)", true);
				String[] legend = {"top 3 %", "top 5 %", "top 10 %"};
				for (int colID = 0; colID < 3; colID++) {
					String id2 = readCount++ + "";
					String label = legend[colID];
					if (label == null) break;
					inputItems.put(id2, label);

					addNode(id2, legend[colID] + " of the messiest folders are shown in this or a hotter color.", true);
					int nodeNumL = inputID2num.get(id2);
					GraphNode legendNode = nodes.get(nodeNumL);
					legendNode.setColor(colors[colID]);
					addEdge(legendID, id2, false, true, "");
				}
			}

		}
		if (transit) { 
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
			controler.getControlerExtras().setNonTreeEdges(nonTreeEdges);
			controler.getControlerExtras().replaceByTree(nodes, edges);
		} else {
			if (layoutOpt) {

				CentralityColoring centralityColoring = new CentralityColoring(nodes, edges);
				centralityColoring.changeColors();
				centralityColoring.revertColors();		// TODO reform constructor
				centralityColoring.treeLayout(nonTreeEdges);

				if (!messOpt) {
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

			}
			if (cancelled) return;
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
			controler.getControlerExtras().setTreeModel(null);
			controler.getControlerExtras().setNonTreeEdges(null);
		} 
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
		sourceNode.addEdge(edge);
		targetNode.addEdge(edge);
		if (!treeColor.isEmpty()) edgeColors.put(edgesNum, treeColor);
		if (xref) nonTreeEdges.add(edge);
		if (removeBeforeReexport) xrefTreeEdges.add(edge);
	}

	// Accessories for mess detection
	public boolean similarExtensions(File file) {
		int leafCounter = 0;
		HashSet<String> extensions = new HashSet<String>();
		File[] dirList = file.listFiles();
		if (dirList != null) {
			for (File f : dirList) {
				if (f.isHidden()) continue;
				if (f.isDirectory()) {
					continue;
				} else {
					leafCounter++;
					int extOffset = f.getName().lastIndexOf(".");
					String foundExtension = "";
					if (extOffset > 0) foundExtension = f.getName().substring(extOffset);
					extensions.add(foundExtension.toLowerCase());
				}
			}
		}
		if (extensions.size() <= 2) {
			System.out.println(file.getName() + " " + leafCounter + ": " + extensions);
			return true;
		}
		return false;
	}

	public void colorPath(GraphNode node, String colorString) {
		Enumeration<GraphEdge> neighbors = node.getEdges();
		while (neighbors.hasMoreElements()) {
			GraphEdge edge = neighbors.nextElement();
			GraphNode dest = edge.getNode2();
			if (!dest.equals(node)) continue;
			GraphNode other = node.relatedNode(edge);
			edge.setColor(colorString);
			colorPath(other, colorString); 		// recursion
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		String command = arg0.getActionCommand();
		if (command == "Cancel") {
			transit = false;
			cancelled = true;
			System.out.println("Import cancelled");
		} else if (command == "Continue") {
			transit = transitBox.isSelected();
			System.out.println("ColorOpt: " + colorOpt + ", MessOpt: " + messOpt);

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
			messOpt = !colorBox.isSelected();
			messBox.setSelected(messOpt);
			legendBox.setEnabled(colorOpt || messOpt);
			return;
		} else if (command == "colorMess"){
			messOpt = messBox.isSelected();
			colorOpt = !messBox.isSelected();
			colorBox.setSelected(colorOpt);
			legendBox.setEnabled(colorOpt || messOpt);
			return;
		} else if (command == "Show legend"){
			legendOpt = legendBox.isSelected();
			return;
		} else if (command == "hyp"){
			hypOpt = hypBox.isSelected();
			return;
		}
		frame.setVisible(false);
		Icon myLeaf = renderer.getLeafIcon();
		UIManager.put("Tree.leafIcon", myLeaf);		// leaf entries here are folders, too
		frame.dispose();
		finish();
		if (hypOpt) controler.getControlerExtras().toggleHyp(1, true);
	}
}

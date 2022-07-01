package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.undo.UndoManager;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

public final class PresentationService implements ActionListener, MouseListener, KeyListener, GraphPanelControler, Runnable {

	//	Main fields
	Hashtable<Integer, GraphNode> nodes;
	Hashtable<Integer, GraphEdge> edges;
	GraphPanel graphPanel;
	TextEditorPanel edi = new TextEditorPanel(this);	
	Gui gui;
	LifeCycle lifeCycle;
	PresentationExtras controlerExtras;
	
	// User Interface
	public JFrame mainWindow;
	public Container cp;	// Content Pane
	JPanel labelBox = null;
	JTextField labelField = null;
	private JButton OK;	
	
	JSplitPane splitPane = null;
	JPanel rightPanel = null;
	int dividerPos = 0;
	GraphPanelZoom graphPanelZoom;
	
	JMenuBar myMenuBar = null;
	boolean showMenuBar = true;
	String [] onOff = {"off", "on"};
	public String about =  " ******** Provisional BANNER ********* " +
			"\r\n ******** Provisional BANNER ********* ";
	public String preferences = "No preferences yet (no installation)";
	
	int initialSize = 12;
	
	//	Graphics accessories
	Point clickedSpot = null;
	Point translation = new Point(0, 0);
	Point panning = new Point(3, 0);
	Point upperGap = new Point(3, 0);
	Point dropLocation = null;
	boolean dropHere = false;
	Point pasteLocation = null;
	boolean pasteHere = false;
	int animationPercent = 0;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	CentralityColoring centralityColoring;

	// Input/ output accessories
	NewStuff newStuff = null;
	CompositionWindow compositionWindow = null;
	String dataString = "";	
	int inputType = 0;
	String baseDir;
	
	// Undo accessories 
	UndoManager undoManager;
	
	// Finding accessories
	String findString = "";
	HashSet<Integer> shownResults = null;
	
	// Tree accessories
	DefaultTreeModel treeModel;
	HashSet<GraphEdge> nonTreeEdges;
	boolean treeBug = false;	// TODO fix

	//	Toggles
	boolean rectangle = false;

	// Placeholders
	GraphNode dummyNode = new GraphNode(-1, null, null, null, null);
	GraphNode selectedTopic = dummyNode;		// TODO integrate into Selection()
	GraphEdge dummyEdge = new GraphEdge(-1, dummyNode, dummyNode, null, null);
	GraphEdge selectedAssoc = dummyEdge;
	
	Selection selection = null;
	boolean extended = false;

	
	public PresentationService(boolean ext) {
		extended = ext;
		controlerExtras = new PresentationExtras(this);
		lifeCycle = new LifeCycle();
	}

//
//	Process menu clicks
	
	public void actionPerformed(ActionEvent e) {
		
		hintTimer.stop();	// Any action => no more hint
		graphPanel.jumpingArrow(false);
		
		String command = e.getActionCommand();

		// Open or Insert

		if (command == "open") {
			String filename = lifeCycle.open();
			newStuff.setInput(filename, 1);

		// Quit

		} else if (command == "quit") {
			close();

		//	Undo / Redo
			
		} else if (command == "undo") {
			undoManager.undo();
		} else if (command == "redo") {
			undoManager.redo();
			
		//	Copy / Cut 
			
		} else if (command == "copy") {
			copy(rectangle, selectedAssoc);
		} else if (command == "cut") {
			cut(rectangle, selectedAssoc);
			
		// Paste

		} else if (command == "paste") {
			pasteHere = false;

			Transferable t = newStuff.readClipboard();
			if (!newStuff.transferTransferable(t)) {
				System.out.println("Error PS121");
			} else {
			}
			
		// Paste here

		} else if (command == "pasteHere") {

			pasteHere = true;
			pasteLocation = clickedSpot;
			Transferable t = newStuff.readClipboard();
			if (!newStuff.transferTransferable(t)) {
				System.out.println("Error PS121a");
			} else {
			}

			
		// Save	

		} else if (command == "Store") {
			lifeCycle.save();
			
		// Save as
			
		} else if (command == "SaveAs") {
			lifeCycle.saveAs();
			
		// Export to legacy or exotic formats
			
		} else if (command == "export") {
			String s = lifeCycle.askForLocation("legacy.zip");
			new TopicMapExporter(nodes, edges).createTopicmapArchive(s);
				
		} else if (command == "expJson") {
			String s = lifeCycle.askForLocation("experimental.json");
			new DemoJsonExporter(nodes, edges, s);
				
		} else if (command == "Anonymize") { 
			String s = lifeCycle.askForLocation("anonymized.xml");
			if (startStoring(s, true)) displayPopup(s + " saved.\n" +
			"All letters a-z replaced by x, all A-Z by X");
				
		} else if (command == "zoom") {
			zoom(true);
			
		// using startup data
		} else if (command == "about") {
				displayPopup(about);
		} else if (command == "prefs") {
			displayPopup(preferences);

		} else if (command == "centcol") {
			treeBug = true;
			if (gui.menuItem51.isSelected()) {
			centralityColoring = new CentralityColoring(nodes, edges);
				centralityColoring.changeColors();
			} else {
				centralityColoring.revertColors();
			}
			graphPanel.repaint();
			
		} else if (command == "layout") {
			centralityColoring = new CentralityColoring(nodes, edges);
				centralityColoring.changeColors(true, this);
				treeBug = true;
			graphPanel.repaint();
			gui.menuItem51.setSelected(true);
			
		} else if (command == "sibling") {
			launchSibling();
			
		//	Exports 
			
		} else if (command == "wxr") {
			new Export2WXR(nodes, edges, this);
			
		} else if (command == "imexp") {
			if (!extended) new LimitationMessage(); 
			else {
				String s = lifeCycle.askForLocation("im.iMap");
				if (!s.isEmpty()) new ImappingExport(nodes, edges, s, this);
			}
		} else if (command == "zkexp") {
			String s = lifeCycle.askForLocation("zk.zkn3");	//	zkx3 did not work
			if (!s.isEmpty())new ZknExport(nodes, edges, s, this);
			
		} else if (command == "dwzexp") {
			if (!extended) new LimitationMessage();
			else {
				String s = lifeCycle.askForLocation("dwz.kgif.xml");
				if (!s.isEmpty()) new DwzExport(nodes, edges, s, this);
			}
		} else if (command == "cmapexp") {
				String s = lifeCycle.askForLocation("my.cmap.cxl");
				if (!s.isEmpty()) new CmapExport(nodes, edges, s, this);

		} else if (command == "brainexp") {
				String s = lifeCycle.askForLocation("my.brain.xml");
				if (!s.isEmpty()) new BrainExport(nodes, edges, s, this);

		} else if (command == "vueexp") {
				String s = lifeCycle.askForLocation("my.vue");
				if (!s.isEmpty()) new VueExport(nodes, edges, s, this);

		} else if (command == "metamexp") {
				String s = lifeCycle.askForLocation("export.json");
				if (!s.isEmpty()) new MetamapsExport(nodes, edges, s, this);

		} else if (command == "csvexp") {
				String s = lifeCycle.askForLocation("csv.txt");
				if (!s.isEmpty()) new CsvExport(nodes, edges, s, this);

		} else if (command == "edgeexp") {
				String s = lifeCycle.askForLocation("csv.txt");
				if (!s.isEmpty()) new CsvExport(nodes, edges, s, this, true);

		} else if (command == "h5pexp") {
				new H5pExport(nodes, edges, this);
		
				
		} else if (command == "delCluster") {
				deleteCluster(rectangle, selectedAssoc);
				graphSelected();
				
		} else if (command == "flipHori") {
			flipCluster(selectedAssoc, true);
			graphSelected();
			
		} else if (command == "flipVerti") {
			flipCluster(selectedAssoc, false);
			graphSelected();

		} else if (command == "subtree") {
			new SubtreeLayout(selectedTopic, nodes, edges, this, treeBug, translation);
			
		} else if (command == "extmsg") {
			new LimitationMessage();
						
		//	Context menu command

		} else if (command == "delTopic") {
				deleteNode(selectedTopic);
				selection.topic = null;
				graphSelected();
			graphPanel.repaint();

		} else if (command == "delAssoc") {
				deleteEdge(selectedAssoc);
				graphSelected();
			graphPanel.repaint();

		} else if (command == "NewNode") {
		    	translation = graphPanel.getTranslation();
		    	clickedSpot.translate(- translation.x, - translation.y);
				GraphNode node = createNode(clickedSpot);
				nodeSelected(node);
				labelField.requestFocus();

		} else if (command == "Print") {
			String lastHTMLFilename = lifeCycle.getLastHTMLFilename();
			if (lastHTMLFilename.isEmpty()) {
				if (lifeCycle.askForFilename("htm")) {
					lastHTMLFilename = lifeCycle.getLastHTMLFilename();
					new MakeHTML(true, nodes, edges, lastHTMLFilename, this);
				}
			} else {
				new MakeHTML(true, nodes, edges, lastHTMLFilename, this);
			}
			
		} else if (command == "MakeHTML") {
			String lastHTMLFilename = lifeCycle.getLastHTMLFilename();
			if (lastHTMLFilename.isEmpty()) {
				if (lifeCycle.askForFilename("htm")) {
					lastHTMLFilename = lifeCycle.getLastHTMLFilename();
					new MakeHTML(false, nodes, edges, lastHTMLFilename, this);
				}
			} else {
				new MakeHTML(false, nodes, edges, lastHTMLFilename, this);
			}

		} else if (command == "wxr") {
			new WXR2SQL(mainWindow);
		} else if (command == "dag") {
			new DAG(nodes, edges, this);
		} else if (command == "makecircle") {
			new MakeCircle(nodes, edges, this);
		} else if (command == "planar") {
			new CheckOverlaps(this, nodes, edges);
		} else if (command == "random") {
			RandomMap randomMap = new RandomMap(this);
			if (randomMap.triggerColoring()) {
				centralityColoring = new CentralityColoring(nodes, edges);
				centralityColoring.changeColors();
			}
//		} else if (command == "tst") {
		} else {
			System.out.println("PS: Wrong action: " + command);
		}
	}

//
//	Main Window	
	
	//	Show a hint instead of initial Composition window
	private Timer hintTimer = new Timer(25, new ActionListener() { 
	    public void actionPerformed (ActionEvent e) { 
			graphPanel.jumpingArrow(true);
			graphPanel.grabFocus();
	    } 
	});
	//	Trying animation for map insertion 
	private Timer animationTimer = new Timer(20, new ActionListener() { 
	    public void actionPerformed (ActionEvent e) {
	    	if (animationPercent < 100) {
	    		animationPercent = animationPercent + 5;
	        	Double dX = -panning.x / 20.0;
	        	Double dY = -panning.y / 20.0;
	        	int pannedX = dX.intValue();;
	        	int pannedY = dY.intValue();;
	        	graphPanel.translateGraph(pannedX, pannedY);
	    	} else {
	    		animationTimer.stop();
	    		animationPercent = 0;
	    		performUpdate(); 
	        	graphPanel.setModel(nodes, edges);
	     	}
	    	updateBounds();
	    	translation = graphPanel.getTranslation();
	    	setMouseCursor(Cursor.DEFAULT_CURSOR);
	    	graphPanel.repaint();
	    } 
	});

	public void createMainWindow(String title) {
		mainWindow = new JFrame(title) {
			private static final long serialVersionUID = 1L;
		};
		
		mainWindow.setLocation(0, 30);
		mainWindow.setSize(960, 580);
		mainWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		mainWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		cp = mainWindow.getContentPane();
		JSplitPane splitPane = createMainGUI();

		myMenuBar = gui.createMenuBar();
		if (controlerExtras.showMenuBar) {
			mainWindow.setJMenuBar(myMenuBar);
		} else {
			JMenuBar nullMenuBar = new JMenuBar();
			mainWindow.setJMenuBar(nullMenuBar);
		}
		graphSelected();

		cp.add(splitPane);
		
	}

	public JSplitPane createMainGUI() {
		controlerExtras.setSystemUI(true);
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(960 - 232);
		splitPane.setResizeWeight(.8);
		splitPane.setDividerSize(8);

		graphPanel.setBackground(Color.WHITE);
		
		splitPane.setLeftComponent(graphPanel);

		rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		
		labelBox = new JPanel();
		labelBox.setLayout(new BorderLayout());
		labelBox.add(new JLabel("Label", JLabel.CENTER));
		labelBox.setToolTipText("Short text that also appears on the map. To see it there, click the map.");
		labelField = new JTextField();
		labelField.addMouseListener(this);
		labelField.addKeyListener(this);
		labelBox.add(labelField,"South");
		Dimension dim = new Dimension(1400,150);
		labelBox.setMaximumSize(dim);

		rightPanel.add(labelBox,"North");
		
		JPanel detailBox = new JPanel();
		detailBox.setLayout(new BoxLayout(detailBox, BoxLayout.Y_AXIS));
		JLabel detailBoxLabel = new JLabel("Detail", JLabel.CENTER);
		detailBoxLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		detailBox.add(detailBoxLabel);
		detailBox.setToolTipText("More text about the selected item, always at your fingertips.");
		detailBox.add(edi,"South");
		rightPanel.add(detailBox,"South");

		lifeCycle.setDirty(false);
		
		splitPane.setRightComponent(rightPanel);
		splitPane.repaint();
		
		return splitPane;
}

//
//	Popups

	public void displayContextMenu(String menuID, int x, int y) {
		JPopupMenu menu = controlerExtras.createContextMenu(menuID);
		clickedSpot = new Point(x, y);
		controlerExtras.setSystemUI(false); //	avoid confusing colors when hovering, and indenting  of items in System LaF 
		menu.show(cp, x, y);
	}

	public void displayPopup(String msg) {
		JOptionPane.showMessageDialog(mainWindow, msg);
	}

	public void setDirty(boolean toggle) {
		lifeCycle.setDirty(toggle);
	}

	private void ssssssssssseparator1() {
		//	just for optics
		ssssssssssseparator2();
	}

//  --------------------------------------------------------------------
//
//	Starts here	
	
	public synchronized void run() {
		initialize();
		graphPanel.setSize(initialSize);
		mainWindow.setVisible(true);
		edi.setSize(initialSize);
		controlerExtras.setInitialSize(initialSize);
		if (lifeCycle.getFilename().isEmpty()) {
			hintTimer.start();
		}
	}
	
	//	Input from start parameters	
	public void setFilename(String arg, int type) {
		lifeCycle.setFilename(arg);
		if (mainWindow != null) mainWindow.repaint();
	}
	
	public synchronized void initialize() {
		if (System.getProperty("os.name").equals("Mac OS X")) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");	// otherwise very alien for Mac users
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "My Tool");
			new AppleHandler(this);			// for QuitHandler
			initialSize = 12;
		} else {
			int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
			initialSize = dpi / 8;
		}
		baseDir = null;
		try {
			baseDir = System.getProperty("user.home") + File.separator + "Desktop";
			System.out.println("PS: Base directory is: " + baseDir);
		} catch (Throwable e) {
			System.out.println("Error PS108" + e );
		}

		newStuff = new NewStuff(this);
		controlerExtras.setNewStuff(newStuff);
		
		graphPanel = new GraphPanel(this);
		nodes = new Hashtable<Integer, GraphNode>();
		edges = new Hashtable<Integer, GraphEdge>();
		controlerExtras.setMap();
		
		undoManager = new UndoManager();
		gui = new Gui(this, graphPanel, edi, newStuff, undoManager );
		controlerExtras.setGui(gui);
		controlerExtras.setEdi(edi);
		
		graphPanel.setModel(nodes, edges);
		selection = graphPanel.getSelectionInstance();	//	TODO eliminate again
		about = (new AboutBuild(extended)).getAbout();
		graphPanel.addKeyListener(this);

		createMainWindow(lifeCycle.getMainWindowTitle());
		lifeCycle.add(this, baseDir);
		
		System.out.println("PS: Initialized");
		System.out.println("\n\n**** This console window may be ignored ****\n");
		
		String filename = lifeCycle.getFilename();
		if (!filename.isEmpty()) newStuff.setInput(filename, 1);
	}
	
//	Saving and finishing

	public boolean startStoring(String storeFilename, boolean anonymized) {
		if (storeFilename.isEmpty()) return false;
		try {
			File storeFile = new TopicMapStorer(nodes, edges).createTopicmapFile(storeFilename, anonymized);
			storeFilename = storeFile.getName();
		} catch (IOException e) {
			System.out.println("Error PS123" + e);
			return false;
		} catch (TransformerConfigurationException e) {
			System.out.println("Error PS124" + e);
			return false;
		} catch (SAXException e) {
			System.out.println("Error PS125" + e);
			return false;
		}
		if (!anonymized) {
			lifeCycle.setDirty(false);
			edi.setDirty(false);
		}
		return true;
	}

	public boolean close() {	// Mac needs this
		return lifeCycle.close();
	}


//	Selection processing

	public void deselect(GraphNode node) {
		if (!node.equals(dummyNode)) {
			node.setLabel(labelField.getText());
			if (!controlerExtras.getHyp()) node.setDetail(edi.getText());
			edi.tracking(false);
			edi.setText("");
			labelField.setText("");
		}
	}	

	public void deselect(GraphEdge edge) {
		if (!edge.equals(dummyEdge)) {
			String det = edi.getText();
			if (det.length() > 59) edge.setDetail(det);
			edi.setText("");
		}
	}	
	
	public void nodeSelected(GraphNode node) {
		deselect(selectedTopic);
		deselect(selectedAssoc);
		selectedAssoc = dummyEdge;
		selectedTopic = node;
		edi.setText((selectedTopic).getDetail());
		edi.tracking(true);
		edi.setDirty(false);
		if (controlerExtras.getHyp()) edi.getTextComponent().setCaretPosition(0);
		edi.getTextComponent().requestFocus();
		String labelText = selectedTopic.getLabel();
		labelField.setText(labelText);
		if (controlerExtras.dragFake && labelText.equals("Drop input")) edi.setFake();
		edi.repaint();
		updateCcpGui();
	}

	public void edgeSelected(GraphEdge edge) {
		deselect(selectedAssoc);
		deselect(selectedTopic);
		selectedTopic = dummyNode;
		selectedAssoc = edge;
		edi.setText(selectedAssoc.getDetail());
		edi.getTextComponent().setCaretPosition(0);
		edi.repaint();
		updateCcpGui();
	}

	public void graphSelected() {
		deselect(selectedTopic);
		selectedTopic = dummyNode;
		deselect(selectedAssoc);
		selectedAssoc = dummyEdge;
		edi.setText(gui.getInitText(nodes.size() <= 0));
		graphPanel.grabFocus();		//  was crucial
		updateCcpGui();
	}
	
	private void ssssssssssseparator2() {
		//	just for optics
		ssssssssssseparator1();
	}
	
//  --------------------------------------------------------------------
//
//	Start of non-trivial processing	
	
	public GraphNode createNode(Point xy) {
		if (xy != null) {
			int newId = newKey(nodes.keySet());
			GraphNode topic = new GraphNode(newId, xy, Color.decode(gui.nodePalette[gui.paletteID][7]), "", "");
			nodes.put(newId, topic);
			controlerExtras.recount();
			updateBounds();
			graphPanel.nodeSelected(topic);
			graphPanel.repaint();
			lifeCycle.setDirty(true);
			return topic;
		} else {
			return null;
		}
	}
	
	public GraphEdge createEdge(GraphNode topic1, GraphNode topic2) {
		if (topic1 != null && topic2 != null) {
			int newId = newKey(edges.keySet());
			GraphEdge assoc = new GraphEdge(newId, topic1, topic2, Color.decode(gui.edgePalette[gui.paletteID][7]), "");  // nicht 239
			assoc.setID(newId);
			edges.put(newId, assoc);
			controlerExtras.recount();
			
			topic1.addEdge(assoc);
			topic2.addEdge(assoc);
			
			lifeCycle.setDirty(true);
			return assoc;
		} else {
			return null;
		}
	}
	
	public int newKey(Set<Integer> keySet) {
		int idTest = keySet.size();
		while (keySet.contains(idTest)) idTest++;
		return idTest;
	}

	public void deleteNode(GraphNode topic) {
		deleteNode(topic, false);
	}
	public void deleteNode(GraphNode topic, boolean auto) {
		Enumeration<GraphEdge> e = topic.getEdges();
		Vector<GraphEdge> todoList = new Vector<GraphEdge>();
		GraphEdge edge;
		int neighborCount = 0;
		while (e.hasMoreElements()) {
			edge = (GraphEdge) e.nextElement();
			todoList.addElement(edge);
			neighborCount++;
		};
		
		if (!auto) {
			String topicName = topic.getLabel();
			if (topicName.length() > 30) topicName = topicName.substring(0,30) + "...";
			//	Some effort to position it near the node to be deleted
			JOptionPane confirm = new JOptionPane("Are you sure you want to delete " + 
					"the item \n \"" + topicName +	
					"\" with " + neighborCount + " connections ?\n" + 
					"(There is no good Undo yet!)", 
					JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION); 
			JDialog d = confirm.createDialog(null, "Warning");
			Point p = topic.getXY();
			d.setLocation(p.x - 20 + translation.x, p.y + 100 + translation.y);
			d.setVisible(true);
			Object responseObj = confirm.getValue();
			if (responseObj == null) return;
			if ((int) responseObj != JOptionPane.YES_OPTION) return;
		}

		Enumeration<GraphEdge> e2 = todoList.elements();
		while (e2.hasMoreElements()) {
			edge = (GraphEdge) e2.nextElement();
			deleteEdge(edge, true);
		}
		int topicKey = topic.getID();
		nodes.remove(topicKey);
		controlerExtras.recount();
		updateBounds();
		lifeCycle.setDirty(true);
		commit(0, topic, null, null);
		return;
	}

	public void deleteEdge(GraphEdge assoc) {
		deleteEdge(assoc, false);
	}

	public void deleteEdge(GraphEdge assoc, boolean silent) {
		GraphNode topic1 = assoc.getNode1();	
		GraphNode topic2 = assoc.getNode2();
		if (!silent) {
			String topicName1 = topic1.getLabel();
			String topicName2 = topic2.getLabel();
			if (topicName1.length() > 30) topicName1 = topicName1.substring(0,30) + "...";
			if (topicName2.length() > 30) topicName2 = topicName2.substring(0,30) + "...";
			int response = JOptionPane.showConfirmDialog(OK, 
					"Are you sure you want to delete " + "the connection \n" + 
					"from \"" + topicName1 + "\" to \""+ topicName2 + "\" ?" + 
					"\n(There is no good Undo yet!)"); 
			if (response != JOptionPane.YES_OPTION) return;
		}
		int assocKey = assoc.getID();
		int topicID1 = topic1.getID();
		int topicID2 = topic2.getID();
		topic1.removeEdge(assoc);
		topic2.removeEdge(assoc);
		edges.remove(assocKey);
		nodes.put(topicID1,  topic1);
		nodes.put(topicID2,  topic2);
		controlerExtras.recount();
		graphPanel.repaint();
		lifeCycle.setDirty(true);
		commit(1, null, assoc, null);
		return;
	}

	public void deleteCluster(boolean rectangle, GraphEdge assoc) {
		deleteCluster(rectangle, assoc, false);
	}
	public void deleteCluster(boolean rectangle, GraphEdge assoc, boolean auto) {
		Hashtable<Integer,GraphNode> cluster = new Hashtable<Integer,GraphNode>();
		GraphNode node;
		if (!rectangle) {
			GraphNode topic1 = assoc.getNode1();	
			cluster = graphPanel.createNodeCluster(topic1);
		} else {
			cluster = graphPanel.createNodeRectangle();
		}
		if (!auto) {
			String what = rectangle ? "rectangle" : "cluster";
			int response = JOptionPane.showConfirmDialog(OK, 
				"<html><body>Your command means deleting multiple items.<br />" +
				"Are you absolutely sure you want to delete the entire <br />" + 
				what + " that contains approx. " + (cluster.size() + 2) + " items ? <br />" +
				"(There is no Undo for multiples!)</body></html>");
			if (response != JOptionPane.YES_OPTION) return;
		}
		Enumeration<GraphNode> e2 = cluster.elements();
		while (e2.hasMoreElements()) {
			node = (GraphNode) e2.nextElement();
			deleteNode(node, true);
		}
		graphPanel.nodeRectangle(false);
		graphPanel.repaint();
		lifeCycle.setDirty(true);
		return;
	}

	public void copyCluster(boolean rectangle, GraphEdge assoc) {
		GraphNode topic1 = assoc.getNode1();	
		graphPanel.copyCluster(rectangle, topic1);
		return;
	}

	public void flipCluster(GraphEdge assoc, boolean horizontal) {
		GraphNode topic1 = assoc.getNode1();	
		Hashtable<Integer,GraphNode> cluster = rectangle ? graphPanel.createNodeRectangle() : 
				graphPanel.createNodeCluster(topic1);
		GraphNode node;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int z;
		HashSet<Integer> todoList = new HashSet<Integer>();
		Enumeration<GraphNode> e3 = cluster.elements();
		while (e3.hasMoreElements()) {
			node = (GraphNode) e3.nextElement();
			Point xy = node.getXY();
			if (horizontal) {
				z = xy.x;
			} else {
				z = xy.y;
			}
			if (z < min) min = z;
			if (z > max) max = z;
			int key = node.getID();
			todoList.add(key);
		}
		int mid = (max + min)/ 2;
		Iterator<Integer> todo = todoList.iterator();
		while (todo.hasNext()) {
			int key = todo.next();
			node = (GraphNode) nodes.get(key);
			Point xy = node.getXY();
			int x = xy.x;
			int y = xy.y;
			if (horizontal) {
				x = x + 2*(mid - x);
			} else { 
				y = y + 2*(mid - y);
			}
			xy = new Point(x, y);
			node.setXY(xy);
		}
		graphPanel.repaint();
		lifeCycle.setDirty(true);
	}

	public Rectangle getBounds() {
		updateBounds();
		return bounds;
	}
	
	public void updateBounds() {
		int xMin = Integer.MAX_VALUE;
		int yMin = Integer.MAX_VALUE;
		int xMax = Integer.MIN_VALUE;
		int yMax = Integer.MIN_VALUE;
		Enumeration<GraphNode> e = nodes.elements();
		while (e.hasMoreElements()) {
			GraphNode node = e.nextElement();
			Point p = node.getXY();
			if (p.x < xMin) xMin = p.x;
			if (p.x > xMax) xMax = p.x;
			if (p.y < yMin) yMin = p.y;
			if (p.y > yMax) yMax = p.y;
		}
		bounds.x = xMin;
		bounds.y = yMin;
		bounds.width = xMax - xMin;
		bounds.height = yMax - yMin;
		graphPanel.setBounds(bounds);
	}


	public Point determineBottom(Hashtable<Integer,GraphNode> nodes, Rectangle bounds) {
		int maxX = bounds.x + bounds.width;
		int maxY = bounds.y + bounds.height;
		int minXbottom = maxX -bounds.width/2;
		if (bounds.width < 726) {	//	graphPanel width, 960 window - 232 right pane
			minXbottom = maxX -bounds.width/2;
		}
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Point xy = node.getXY();
			int x = xy.x;
			int y = xy.y;
			if (y > maxY - 100){
				if (x < minXbottom) minXbottom = x;
			}
		}
		return new Point(minXbottom, maxY);
	}
	
	public void addToLabel(String textToAdd) {
		if (selectedTopic == dummyNode) return;
		String oldText = labelField.getText();
		String newText = oldText + " " + textToAdd;
		labelField.setText(newText);
		GraphNode justUpdated = selectedTopic;
		graphSelected();
		graphPanel.labelUpdateToggle(true);
		nodeSelected(justUpdated);
		graphPanel.labelUpdateToggle(false);
		mainWindow.repaint();   // this was crucial
	}

//
//	Accessories for cursors and carets
    
	public void setMouseCursor(int cursorType) {
		mainWindow.setCursor(new Cursor(cursorType));
	}
	
//
//	Misc and temp
    
	// tmp diagnostic
	public void findAboutFocus() {
		System.out.println("graphPanel? " + graphPanel.hasFocus());
		System.out.println("labelField? " + labelField.hasFocus());
		System.out.println("edi? " + edi.hasFocus());
		System.out.println("compositionWindow? " + controlerExtras.getCWInstance().compositionWindow.hasFocus());
	}

	// (placeholder)
	public void manip(int x) {
		System.out.println(edi.getText());
	}

	public void launchSibling() {
		new MyTool();
		String[] dummyArg = {""};
		MyTool.main(dummyArg);
	}
	
//
//	Communication with other classes
    
    public NewStuff getNSInstance() {
    	return newStuff;
    }
    
    public GraphExtras getGraphExtras() {
     	return graphPanel.getExtras();
    }
    
    public GraphPanel getGraphPanel() {
     	return graphPanel;
    }
    
    public Point getTranslation() {
    	return graphPanel.getTranslation();
    }

    public JFrame getMainWindow() {
    	return mainWindow;
    }
    
   public String getNewNodeColor() {
	   return gui.nodePalette[gui.paletteID][7];
   }
    
   public DefaultTreeModel getTreeModel() {
	   return treeModel;
   }
   public void setTreeModel(DefaultTreeModel treeModel) {
	   this.treeModel = treeModel;
   }
   
   public void setNonTreeEdges(HashSet<GraphEdge> nonTreeEdges) {
	   this.nonTreeEdges = nonTreeEdges;
   }
   public HashSet<GraphEdge> getNonTreeEdges() {
	   return nonTreeEdges;
   }

   
   public boolean getExtended() {
	   return extended;
   }
   public void toggleRectanglePresent(boolean on) {
	   rectangle = on;
	   updateCcpGui();
   }
   
   public void stopHint() {
	   hintTimer.stop();
	   graphPanel.jumpingArrow(false);
   }
	
   // Major class exchanges
   
   public void triggerUpdate() {
	   translation = graphPanel.getTranslation();
	   if (nodes.size() < 1) {
		   panning = new Point(0, 0);
		   upperGap = new Point(0, 0);
		   performUpdate();
	   } else {
		   Point bottomOfExisting = determineBottom(nodes, bounds);
		   panning = new Point(bottomOfExisting.x - 40 + translation.x, 
				   bottomOfExisting.y - 100 + translation.y); 
		   upperGap = new Point(40, 140); 
		   dropLocation = newStuff.getDropLocation();
		   if (dropLocation != null && !gui.menuItem24.isSelected()) dropHere = true; 
		   if (!dropHere && !pasteHere) {
			   animationTimer.start();
			   //  performUpdate is called from timer's ActionPerformed() 
		   } else {
			   panning = new Point(0, 0);
			   //	"upperGap" is misleading in this case
			   if (dropLocation != null) {
				   upperGap = dropLocation;
			   } else {
				   upperGap = pasteLocation;
			   }
			   translation = graphPanel.getTranslation();
			   graphPanel.translateGraph(-panning.x, -panning.y);
			   performUpdate();
			   updateBounds();
			   setMouseCursor(Cursor.DEFAULT_CURSOR);
			   graphPanel.repaint();
		   }
	   }
   }
   
   public void performUpdate() {
	   boolean existingMap = newStuff.isExistingMap();
	   hintTimer.stop();
	   graphPanel.jumpingArrow(false);
	   if (!lifeCycle.isLoaded() && existingMap && nodes.size() < 1) {
		   //  don't set dirty yet
		   lifeCycle.setConfirmedFilename(newStuff.getAdvisableFilename());
		   lifeCycle.setLoaded(true);
	   } else {
		   lifeCycle.setDirty(true);
	   }
	   Hashtable<Integer, GraphNode> newNodes = newStuff.getNodes();
	   Hashtable<Integer, GraphEdge> newEdges = newStuff.getEdges();
	   if (lifeCycle.getFilename().isEmpty()) {
		   lifeCycle.resetFilename(newStuff.getAdvisableFilename());
	   }
	   IntegrateNodes integrateNodes = new IntegrateNodes(nodes, edges, newNodes, newEdges);
	   translation = graphPanel.getTranslation();

	   integrateNodes.mergeNodes(upperGap, translation);
	   nodes = integrateNodes.getNodes();
	   edges = integrateNodes.getEdges();

	   graphPanel.setModel(nodes, edges);
	   controlerExtras.recount();
	   updateBounds();
	   setMouseCursor(Cursor.DEFAULT_CURSOR);
	   graphPanel.repaint();
	   pasteHere = false;
	   dropHere = false;
	   graphSelected();
   }

   public void replaceByTree(Hashtable<Integer,GraphNode> replacingNodes, 
		   Hashtable<Integer,GraphEdge> replacingEdges) {
	   if (nodes.size() > 0) {
		   displayPopup("Please use an empty map if you want to use\n" 
				   + "the imported tree information for re-export.");
		   treeModel = null;
		   nonTreeEdges = null;
		   return;
	   }
	   nodes = replacingNodes;
	   edges = replacingEdges;
	   graphPanel.setModel(nodes, edges);
	   updateBounds();
	   setMouseCursor(Cursor.DEFAULT_CURSOR);
	   hintTimer.stop();	// Any action => no more hint
	   graphPanel.jumpingArrow(false);
	   graphPanel.repaint();
   }
   
   public void replaceForLayout(Hashtable<Integer,GraphNode> replacingNodes, 
		   Hashtable<Integer,GraphEdge> replacingEdges) {	
	   // TODO integrate with replaceByTree
	   nodes = replacingNodes;
	   edges = replacingEdges;
	   graphPanel.setModel(nodes, edges);
	   updateBounds();
	   setMouseCursor(Cursor.DEFAULT_CURSOR);
	   hintTimer.stop();	// Any action => no more hint
	   graphPanel.jumpingArrow(false);
	   graphPanel.repaint();
   }
   
//
//	Accessories intended for right-click (paste) in labelfield
    
	public void mouseClicked(MouseEvent arg0) {
		if ((arg0.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
			JPopupMenu menu = Utilities.showContextMenu();
			menu.show(labelField, arg0.getX(), arg0.getY());
		}
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent arg0) {
	}

	public void mouseReleased(MouseEvent arg0) {
	}

//
//	Accessories intended for entering in labelfield
    
	public void keyPressed(KeyEvent arg0) {
		if (arg0.getKeyChar() == KeyEvent.VK_ENTER) {
			GraphNode justUpdated = selectedTopic;
			graphSelected();
			graphPanel.labelUpdateToggle(true);
			nodeSelected(justUpdated);
			graphPanel.labelUpdateToggle(false);
			mainWindow.repaint();   // this was crucial
		}
	}

	public void keyReleased(KeyEvent arg0) {
	}

	public void keyTyped(KeyEvent arg0) {
		if (arg0.getKeyChar() == KeyEvent.VK_DELETE) {
				if (rectangle) deleteCluster(true, selectedAssoc, false);
		}
	}

	public void commit(int type, GraphNode node, GraphEdge edge, Point move) {
		MyUndoableEdit myUndoableEdit = new MyUndoableEdit(type, node, edge, move, 
				nodes, edges, graphPanel, gui);
		undoManager.addEdit(myUndoableEdit);
		gui.updateUndoGui();
	}
	

	public void copy(boolean rectangle, GraphEdge assoc) {
		GraphNode topic = assoc.getNode1();	
		graphPanel.copyCluster(rectangle, topic);
	}

	public void cut(boolean rectangle, GraphEdge assoc) {
		GraphNode topic = assoc.getNode1();	
		graphPanel.copyCluster(rectangle, topic);
		deleteCluster(rectangle, assoc);
	}

	public void updateCcpGui() {
		gui.menuItem93.setEnabled(rectangle);
		gui.menuItem94.setEnabled(rectangle);
		gui.menuItem95.setEnabled(rectangle);
		gui.menuItem38.setEnabled(rectangle);
		gui.menuItem39.setEnabled(rectangle);
	}

	public void fixDivider() {
		splitPane.setDividerLocation(mainWindow.getWidth() - 464);
		splitPane.setResizeWeight(1);
	}

	public void zoom(boolean on) {
		dividerPos = splitPane.getDividerLocation();
		if (on) {
			Point transl = graphPanel.getTranslation();
			graphPanelZoom = new GraphPanelZoom(transl, this);
			splitPane.setDividerLocation(dividerPos);
			splitPane.setRightComponent(graphPanelZoom.createSlider());
			graphPanelZoom.setModel(nodes, edges);
			splitPane.setLeftComponent(graphPanelZoom);
			controlerExtras.toggleClassicMenu();
		} 
		if (!on) {
			gui.menuItem58.setSelected(false);
			splitPane.setDividerLocation(dividerPos);
			splitPane.setLeftComponent(graphPanel);
			splitPane.setRightComponent(rightPanel);
			controlerExtras.toggleClassicMenu();
		}
	}

	public Hashtable<Integer,GraphNode> getNodes() {
		return nodes;
	}
	public Hashtable<Integer,GraphEdge> getEdges() {
		return edges;
	}

	// For circle refinement
	public GraphNode getSelectedNode() {
		return selectedTopic;
	}
	public GraphEdge getSelectedEdge() {
		return selectedAssoc;
	}

	public void linkTo(String label) {
		controlerExtras.toggleHashes(true);
		GraphNode activeNode = selectedTopic;
		activeNode.setDetail(edi.getText());

		Point activeXY = selectedTopic.getXY();
		Point newXY = new Point(activeXY.x - 30, activeXY.y + 30);
		GraphNode newNode = createNode(newXY);
		labelField.setText(label);

		String labelActive = activeNode.getLabel();
		String detailNew = "<br/>See also <a href=\"#" + labelActive + "\">" + labelActive + "</a>";
		newNode.setDetail(detailNew);
		nodeSelected(newNode);	// see deselect() peculiarities

		createEdge(activeNode, newNode);
		mainWindow.repaint();
	}
	
	public PresentationExtras getControlerExtras() {
		return controlerExtras;
	}
}

package de.x28hd.tool;


// Moved  to GraphPanelControler

public final class PresentationService  {
	
//public final class PresentationService implements ActionListener,  {
//	//	Main fields
//	Hashtable<Integer, GraphNode> nodes;
//	Hashtable<Integer, GraphEdge> edges;
//	GraphPanel graphPanel;
//	TextEditorPanel edi = new TextEditorPanel(n);	
//	Gui gui;
//	LifeCycle lifeCycle;
//	PresentationExtras controlerExtras;
//	
//	// User Interface
//	public JFrame mainWindow;
//	public Container cp;	// Content Pane
//	JPanel labelBox = null;
//	JTextField labelField = null;
//	private JButton OK;	
//	
//	JSplitPane splitPane = null;
//	JPanel rightPanel = null;
//	
//	JMenuBar myMenuBar = null;
//	boolean showMenuBar = true;
//	public String about =  " ******** Provisional BANNER ********* " +
//			"\r\n ******** Provisional BANNER ********* ";
//	public String preferences = "No preferences yet (no installation)";
//	
//	int initialSize = 12;
//	
//	//	Graphics accessories
//	Point clickedSpot = null;
//	Point translation = new Point(0, 0);
//
//	// Input/ output accessories
//	NewStuff newStuff = null;
//	CompositionWindow compositionWindow = null;
//	String dataString = "";	
//	int inputType = 0;
//	String baseDir;
//	
//	// Undo accessories 
//	UndoManager undoManager;
//	
//	//	Toggles
//	boolean rectangle = false;
//
//	// Placeholders
//	GraphNode dummyNode = new GraphNode(-1, null, null, null, null);
//	GraphNode selectedTopic = dummyNode;		// TODO integrate into Selection()
//	GraphEdge dummyEdge = new GraphEdge(-1, dummyNode, dummyNode, null, null);
//	GraphEdge selectedAssoc = dummyEdge;
//	
//	Selection selection = null;
//	boolean extended = false;
//
//	
//	public PresentationService(boolean ext) {
//		extended = ext;
//		controlerExtras = new PresentationExtras(this);
//		lifeCycle = new LifeCycle();
//	}
//
////
////	Process menu clicks
//	
//	public void actionPerformed(ActionEvent e) {
//		
//		controlerExtras.stopHint();
//		String command = e.getActionCommand();
//
//		if (command == "open") {
//			String filename = lifeCycle.open();
//			newStuff.setInput(filename, 1);
//		} else if (command == "importDirector") {
//			new ImportDirector(this);	
//
//		} else if (command == "quit") {
//			close();
//			
//		} else if (command == "undo") {
//			undoManager.undo();
//		} else if (command == "redo") {
//			undoManager.redo();
//			
//		} else if (command == "copy") {
//			copy(rectangle, selectedAssoc);
//		} else if (command == "cut") {
//			cut(rectangle, selectedAssoc);
//			
//		} else if (command == "paste") {
//			controlerExtras.setPasteOptions(false, null);
//			Transferable t = newStuff.readClipboard();
//			if (!newStuff.transferTransferable(t)) {
//				System.out.println("Error PS121");
//			}
//		} else if (command == "pasteHere") {
//			controlerExtras.setPasteOptions(true, clickedSpot);
//			Transferable t = newStuff.readClipboard();
//			if (!newStuff.transferTransferable(t)) {
//				System.out.println("Error PS121a");
//			}
//
//		} else if (command == "Store") {
//			lifeCycle.save();
//		} else if (command == "SaveAs") {
//			lifeCycle.saveAs();
//			
//		// using startup data
//		} else if (command == "about") {
//				displayPopup(about);
//		} else if (command == "prefs") {
//			displayPopup(preferences);
//
//		} else if (command == "delCluster") {
//				deleteCluster(rectangle, selectedAssoc);
//				graphSelected();
//						
//		//	Context menu command
//
//		} else if (command == "delTopic") {
//				deleteNode(selectedTopic);
//				selection.topic = null;
//				graphSelected();
//			graphPanel.repaint();
//
//		} else if (command == "delAssoc") {
//				deleteEdge(selectedAssoc);
//				graphSelected();
//			graphPanel.repaint();
//
//		} else if (command == "NewNode") {
//		    	translation = graphPanel.getTranslation();
//		    	clickedSpot.translate(- translation.x, - translation.y);
//				GraphNode node = createNode(clickedSpot);
//				nodeSelected(node);
//				labelField.requestFocus();
//
////		} else if (command == "tst") {
//		} else {
//			System.out.println("PS: Wrong action: " + command);
//		}
//	}
//
////
////	Main Window	
//	
//	public void createMainWindow(String title) {
//		mainWindow = new JFrame(title) {
//			private static final long serialVersionUID = 1L;
//		};
//		
//		mainWindow.setLocation(0, 30);
//		mainWindow.setSize(960, 580);
//		mainWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
//
//		mainWindow.addWindowListener(new WindowAdapter() {
//			public void windowClosing(WindowEvent e) {
//				close();
//			}
//		});
//
//		cp = mainWindow.getContentPane();
//		JSplitPane splitPane = createMainGUI();
//
//		myMenuBar = gui.createMenuBar();
//		if (controlerExtras.showMenuBar) {
//			mainWindow.setJMenuBar(myMenuBar);
//		} else {
//			JMenuBar nullMenuBar = new JMenuBar();
//			mainWindow.setJMenuBar(nullMenuBar);
//		}
//		graphSelected();
//
//		cp.add(splitPane);
//		
//	}
//
//	public JSplitPane createMainGUI() {
//		controlerExtras.setSystemUI(true);
//		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
//		splitPane.setOneTouchExpandable(true);
//		splitPane.setDividerLocation(960 - 232);
//		splitPane.setResizeWeight(.8);
//		splitPane.setDividerSize(8);
//
//		graphPanel.setBackground(Color.WHITE);
//		
//		splitPane.setLeftComponent(graphPanel);
//
//		rightPanel = new JPanel();
//		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
//		
//		labelBox = new JPanel();
//		labelBox.setLayout(new BorderLayout());
//		labelBox.add(new JLabel("Label", JLabel.CENTER));
//		labelBox.setToolTipText("Short text that also appears on the map. To see it there, click the map.");
//		labelField = new JTextField();
//		labelField.addMouseListener(controlerExtras);
//		labelField.addKeyListener(controlerExtras);
//		labelBox.add(labelField,"South");
//		Dimension dim = new Dimension(1400,150);
//		labelBox.setMaximumSize(dim);
//
//		rightPanel.add(labelBox,"North");
//		
//		JPanel detailBox = new JPanel();
//		detailBox.setLayout(new BoxLayout(detailBox, BoxLayout.Y_AXIS));
//		JLabel detailBoxLabel = new JLabel("Detail", JLabel.CENTER);
//		detailBoxLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
//		detailBox.add(detailBoxLabel);
//		detailBox.setToolTipText("More text about the selected item, always at your fingertips.");
//		detailBox.add(edi,"South");
//		rightPanel.add(detailBox,"South");
//
//		lifeCycle.setDirty(false);
//		
//		splitPane.setRightComponent(rightPanel);
//		splitPane.repaint();
//		
//		controlerExtras.setSplitPane(splitPane, rightPanel);
//		return splitPane;
//}
//
////
////	Popups
//
//	public void displayContextMenu(String menuID, int x, int y) {
//		JPopupMenu menu = controlerExtras.createContextMenu(menuID);
//		clickedSpot = new Point(x, y);
//		controlerExtras.setSystemUI(false); //	avoid confusing colors when hovering, and indenting  of items in System LaF 
//		menu.show(cp, x, y);
//	}
//
//	public void displayPopup(String msg) {
//		JOptionPane.showMessageDialog(mainWindow, msg);
//	}
//
//	public void setDirty(boolean toggle) {
//		lifeCycle.setDirty(toggle);
//	}
//
//	private void ssssssssssseparator1() {
//		//	just for optics
//		ssssssssssseparator2();
//	}
//
////  --------------------------------------------------------------------
////
////	Starts here	
//	
//	public synchronized void run() {
//		initialize();
//		graphPanel.setSize(initialSize);
//		mainWindow.setVisible(true);
//		edi.setSize(initialSize);
//		controlerExtras.setInitialSize(initialSize);
//		if (lifeCycle.getFilename().isEmpty()) {
//			controlerExtras.hintTimer.start();
//		}
//	}
//	
//	//	Input from start parameters	
//	public void setFilename(String arg, int type) {
//		lifeCycle.setFilename(arg);
//		if (mainWindow != null) mainWindow.repaint();
//	}
//	
//	public synchronized void initialize() {
//		if (System.getProperty("os.name").equals("Mac OS X")) {
//			System.setProperty("apple.laf.useScreenMenuBar", "true");	// otherwise very alien for Mac users
//			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "My Tool");
//			new AppleHandler(this);			// for QuitHandler
//			initialSize = 12;
//		} else {
//			int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
//			initialSize = dpi / 8;
//		}
//		baseDir = null;
//		try {
//			baseDir = System.getProperty("user.home") + File.separator + "Desktop";
//			System.out.println("PS: Base directory is: " + baseDir);
//		} catch (Throwable e) {
//			System.out.println("Error PS108" + e );
//		}
//
//		newStuff = new NewStuff(this);
//		controlerExtras.setNewStuff(newStuff);
//		
//		graphPanel = new GraphPanel(this);
//		nodes = new Hashtable<Integer, GraphNode>();
//		edges = new Hashtable<Integer, GraphEdge>();
//		controlerExtras.setMap();
//		
//		undoManager = new UndoManager();
//		gui = new Gui(this, graphPanel, edi, newStuff, undoManager );
//		controlerExtras.setGui(gui);
//		controlerExtras.setEdi(edi);
//		
//		graphPanel.setModel(nodes, edges);
//		selection = graphPanel.getSelectionInstance();	//	TODO eliminate again
//		about = (new AboutBuild(extended)).getAbout();
//		graphPanel.addKeyListener(controlerExtras);
//
//		createMainWindow(lifeCycle.getMainWindowTitle());
//		lifeCycle.add(this, baseDir);
//		
//		System.out.println("PS: Initialized");
//		System.out.println("\n\n**** This console window may be ignored ****\n");
//		
//		String filename = lifeCycle.getFilename();
//		if (!filename.isEmpty()) newStuff.setInput(filename, 1);
//	}
//	
////	Saving and finishing
//
//	public boolean startStoring(String storeFilename, boolean anonymized) {
//		if (storeFilename.isEmpty()) return false;
//		try {
//			File storeFile = new TopicMapStorer(nodes, edges).createTopicmapFile(storeFilename, anonymized);
//			storeFilename = storeFile.getName();
//		} catch (IOException e) {
//			System.out.println("Error PS123" + e);
//			return false;
//		} catch (TransformerConfigurationException e) {
//			System.out.println("Error PS124" + e);
//			return false;
//		} catch (SAXException e) {
//			System.out.println("Error PS125" + e);
//			return false;
//		}
//		if (!anonymized) {
//			lifeCycle.setDirty(false);
//			edi.setDirty(false);
//		}
//		return true;
//	}
//
//	public boolean close() {	// Mac needs this
//		return lifeCycle.close();
//	}
//
//
////	Selection processing
//
//	public void deselect(GraphNode node) {
//		if (!node.equals(dummyNode)) {
//			node.setLabel(labelField.getText());
//			if (!controlerExtras.getHyp()) node.setDetail(edi.getText());
//			edi.tracking(false);
//			edi.setText("");
//			labelField.setText("");
//		}
//	}	
//
//	public void deselect(GraphEdge edge) {
//		if (!edge.equals(dummyEdge)) {
//			String det = edi.getText();
//			if (det.length() > 59) edge.setDetail(det);
//			edi.setText("");
//		}
//	}	
//	
//	public void nodeSelected(GraphNode node) {
//		deselect(selectedTopic);
//		deselect(selectedAssoc);
//		selectedAssoc = dummyEdge;
//		selectedTopic = node;
//		edi.setText((selectedTopic).getDetail());
//		edi.tracking(true);
//		edi.setDirty(false);
//		if (controlerExtras.getHyp()) edi.getTextComponent().setCaretPosition(0);
//		edi.getTextComponent().requestFocus();
//		String labelText = selectedTopic.getLabel();
//		labelField.setText(labelText);
//		if (controlerExtras.dragFake && labelText.equals("Drop input")) edi.setFake();
//		edi.repaint();
//		updateCcpGui();
//	}
//
//	public void edgeSelected(GraphEdge edge) {
//		deselect(selectedAssoc);
//		deselect(selectedTopic);
//		selectedTopic = dummyNode;
//		selectedAssoc = edge;
//		edi.setText(selectedAssoc.getDetail());
//		edi.getTextComponent().setCaretPosition(0);
//		edi.repaint();
//		updateCcpGui();
//	}
//
//	public void graphSelected() {
//		deselect(selectedTopic);
//		selectedTopic = dummyNode;
//		deselect(selectedAssoc);
//		selectedAssoc = dummyEdge;
//		edi.setText(gui.getInitText(nodes.size() <= 0));
//		graphPanel.grabFocus();		//  was crucial
//		updateCcpGui();
//	}
//	
//	private void ssssssssssseparator2() {
//		//	just for optics
//		ssssssssssseparator1();
//	}
//	
////  --------------------------------------------------------------------
////
////	Start of non-trivial processing	
//	
//	public GraphNode createNode(Point xy) {
//		if (xy != null) {
//			int newId = newKey(nodes.keySet());
//			GraphNode topic = new GraphNode(newId, xy, Color.decode(gui.nodePalette[gui.paletteID][7]), "", "");
//			nodes.put(newId, topic);
//			controlerExtras.recount();
//			controlerExtras.updateBounds();
//			graphPanel.nodeSelected(topic);
//			graphPanel.repaint();
//			lifeCycle.setDirty(true);
//			return topic;
//		} else {
//			return null;
//		}
//	}
//	
//	public GraphEdge createEdge(GraphNode topic1, GraphNode topic2) {
//		if (topic1 != null && topic2 != null) {
//			int newId = newKey(edges.keySet());
//			GraphEdge assoc = new GraphEdge(newId, topic1, topic2, Color.decode(gui.edgePalette[gui.paletteID][7]), "");  // nicht 239
//			assoc.setID(newId);
//			edges.put(newId, assoc);
//			controlerExtras.recount();
//			
//			topic1.addEdge(assoc);
//			topic2.addEdge(assoc);
//			
//			lifeCycle.setDirty(true);
//			return assoc;
//		} else {
//			return null;
//		}
//	}
//	
//	public int newKey(Set<Integer> keySet) {
//		int idTest = keySet.size();
//		while (keySet.contains(idTest)) idTest++;
//		return idTest;
//	}
//
//	public void deleteNode(GraphNode topic) {
//		deleteNode(topic, false);
//	}
//	public void deleteNode(GraphNode topic, boolean auto) {
//		Enumeration<GraphEdge> e = topic.getEdges();
//		Vector<GraphEdge> todoList = new Vector<GraphEdge>();
//		GraphEdge edge;
//		int neighborCount = 0;
//		while (e.hasMoreElements()) {
//			edge = (GraphEdge) e.nextElement();
//			todoList.addElement(edge);
//			neighborCount++;
//		};
//		
//		if (!auto) {
//			String topicName = topic.getLabel();
//			if (topicName.length() > 30) topicName = topicName.substring(0,30) + "...";
//			//	Some effort to position it near the node to be deleted
//			JOptionPane confirm = new JOptionPane("Are you sure you want to delete " + 
//					"the item \n \"" + topicName +	
//					"\" with " + neighborCount + " connections ?\n" + 
//					"(There is no good Undo yet!)", 
//					JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION); 
//			JDialog d = confirm.createDialog(null, "Warning");
//			Point p = topic.getXY();
//			d.setLocation(p.x - 20 + translation.x, p.y + 100 + translation.y);
//			d.setVisible(true);
//			Object responseObj = confirm.getValue();
//			if (responseObj == null) return;
//			if ((int) responseObj != JOptionPane.YES_OPTION) return;
//		}
//
//		Enumeration<GraphEdge> e2 = todoList.elements();
//		while (e2.hasMoreElements()) {
//			edge = (GraphEdge) e2.nextElement();
//			deleteEdge(edge, true);
//		}
//		int topicKey = topic.getID();
//		nodes.remove(topicKey);
//		controlerExtras.recount();
//		controlerExtras.updateBounds();
//		lifeCycle.setDirty(true);
//		commit(0, topic, null, null);
//		return;
//	}
//
//	public void deleteEdge(GraphEdge assoc) {
//		deleteEdge(assoc, false);
//	}
//
//	public void deleteEdge(GraphEdge assoc, boolean silent) {
//		GraphNode topic1 = assoc.getNode1();	
//		GraphNode topic2 = assoc.getNode2();
//		if (!silent) {
//			String topicName1 = topic1.getLabel();
//			String topicName2 = topic2.getLabel();
//			if (topicName1.length() > 30) topicName1 = topicName1.substring(0,30) + "...";
//			if (topicName2.length() > 30) topicName2 = topicName2.substring(0,30) + "...";
//			int response = JOptionPane.showConfirmDialog(OK, 
//					"Are you sure you want to delete " + "the connection \n" + 
//					"from \"" + topicName1 + "\" to \""+ topicName2 + "\" ?" + 
//					"\n(There is no good Undo yet!)"); 
//			if (response != JOptionPane.YES_OPTION) return;
//		}
//		int assocKey = assoc.getID();
//		int topicID1 = topic1.getID();
//		int topicID2 = topic2.getID();
//		topic1.removeEdge(assoc);
//		topic2.removeEdge(assoc);
//		edges.remove(assocKey);
//		nodes.put(topicID1,  topic1);
//		nodes.put(topicID2,  topic2);
//		controlerExtras.recount();
//		graphPanel.repaint();
//		lifeCycle.setDirty(true);
//		commit(1, null, assoc, null);
//		return;
//	}
//
//	public void deleteCluster(boolean rectangle, GraphEdge assoc) {
//		deleteCluster(rectangle, assoc, false);
//	}
//	public void deleteCluster(boolean rectangle, GraphEdge assoc, boolean auto) {
//		Hashtable<Integer,GraphNode> cluster = new Hashtable<Integer,GraphNode>();
//		GraphNode node;
//		if (!rectangle) {
//			GraphNode topic1 = assoc.getNode1();	
//			cluster = graphPanel.createNodeCluster(topic1);
//		} else {
//			cluster = graphPanel.createNodeRectangle();
//		}
//		if (!auto) {
//			String what = rectangle ? "rectangle" : "cluster";
//			int response = JOptionPane.showConfirmDialog(OK, 
//				"<html><body>Your command means deleting multiple items.<br />" +
//				"Are you absolutely sure you want to delete the entire <br />" + 
//				what + " that contains approx. " + (cluster.size() + 2) + " items ? <br />" +
//				"(There is no Undo for multiples!)</body></html>");
//			if (response != JOptionPane.YES_OPTION) return;
//		}
//		Enumeration<GraphNode> e2 = cluster.elements();
//		while (e2.hasMoreElements()) {
//			node = (GraphNode) e2.nextElement();
//			deleteNode(node, true);
//		}
//		graphPanel.nodeRectangle(false);
//		graphPanel.repaint();
//		lifeCycle.setDirty(true);
//		return;
//	}
//
//	public void copyCluster(boolean rectangle, GraphEdge assoc) {
//		GraphNode topic1 = assoc.getNode1();	
//		graphPanel.copyCluster(rectangle, topic1);
//		return;
//	}
//
//	public void addToLabel(String textToAdd) {
//		if (selectedTopic == dummyNode) return;
//		String oldText = labelField.getText();
//		String newText = oldText + " " + textToAdd;
//		labelField.setText(newText);
//		GraphNode justUpdated = selectedTopic;
//		graphSelected();
//		graphPanel.labelUpdateToggle(true);
//		nodeSelected(justUpdated);
//		graphPanel.labelUpdateToggle(false);
//		mainWindow.repaint();   // this was crucial
//	}
//
////
////	Misc 
//    
//	public void setMouseCursor(int cursorType) {
//		mainWindow.setCursor(new Cursor(cursorType));
//	}
//	
////
////	Communication with other classes
//    
//    public NewStuff getNSInstance() {
//    	return newStuff;
//    }
//    
//    public GraphExtras getGraphExtras() {
//     	return graphPanel.getExtras();
//    }
//    
//    public GraphPanel getGraphPanel() {
//     	return graphPanel;
//    }
//    
//    public Point getTranslation() {
//    	return graphPanel.getTranslation();
//    }
//
//    public JFrame getMainWindow() {
//    	return mainWindow;
//    }
//    
//   public String getNewNodeColor() {
//	   return gui.nodePalette[gui.paletteID][7];
//   }
//    
//	public void setModel(Hashtable<Integer, GraphNode> nodes, 
//			Hashtable<Integer, GraphEdge> edges) {
//		this.nodes = nodes;
//		this.edges = edges;
//		graphPanel.setModel(nodes, edges);
//	}
//   
//   public boolean getExtended() {
//	   return extended;
//   }
//   public void toggleRectanglePresent(boolean on) {
//	   rectangle = on;
//	   updateCcpGui();
//   }
//   
//	public void commit(int type, GraphNode node, GraphEdge edge, Point move) {
//		MyUndoableEdit myUndoableEdit = new MyUndoableEdit(type, node, edge, move, 
//				nodes, edges, graphPanel, gui);
//		undoManager.addEdit(myUndoableEdit);
//		gui.updateUndoGui();
//	}
//	
//
//	public void copy(boolean rectangle, GraphEdge assoc) {
//		GraphNode topic = assoc.getNode1();	
//		graphPanel.copyCluster(rectangle, topic);
//	}
//
//	public boolean getRectangle() {
//		return rectangle;
//	}
//	
//	public void cut(boolean rectangle, GraphEdge assoc) {
//		GraphNode topic = assoc.getNode1();	
//		graphPanel.copyCluster(rectangle, topic);
//		deleteCluster(rectangle, assoc);
//	}
//
//	public void updateCcpGui() {
//		gui.menuItem93.setEnabled(rectangle);
//		gui.menuItem94.setEnabled(rectangle);
//		gui.menuItem95.setEnabled(rectangle);
//		gui.menuItem38.setEnabled(rectangle);
//		gui.menuItem39.setEnabled(rectangle);
//	}
//
//	public void fixDivider() {
//		splitPane.setDividerLocation(mainWindow.getWidth() - 464);
//		splitPane.setResizeWeight(1);
//	}
//
//	public Hashtable<Integer,GraphNode> getNodes() {
//		return nodes;
//	}
//	public Hashtable<Integer,GraphEdge> getEdges() {
//		return edges;
//	}
//
//	public GraphNode getSelectedNode() {
//		return selectedTopic;
//	}
//	public GraphEdge getSelectedEdge() {
//		return selectedAssoc;
//	}
//
//	public PresentationExtras getControlerExtras() {
//		return controlerExtras;
//	}
//
//	public LifeCycle getLifeCycle() {
//		return lifeCycle;
//	}
//
//	public JTextField getLabelField() {
//		return labelField;
//	}
//	
}

package de.x28hd.tool;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

public final class PresentationService implements ActionListener, GraphPanelControler, Runnable, PopupMenuListener {

	//	Main fields
	Hashtable<Integer, GraphNode> nodes;
	Hashtable<Integer, GraphEdge> edges;
	GraphPanel graphPanel;
	TextEditorPanel edi = new TextEditorPanel(this);	
	
	// User Interface
	public JFrame mainWindow;
	public Container cp;	// Content Pane
	JPanel labelBox = null;
	JTextField labelField = null;
//	JTextComponent textDisplay = null;
	private JButton OK;	
	
	JMenuBar myMenuBar = null;
	int shortcutMask;
	boolean showMenuBar = true;
	JMenuItem menuItem21 = null;
	JCheckBoxMenuItem menuItem22 = null;
	JCheckBoxMenuItem menuItem23 = null;
	JCheckBoxMenuItem menuItem41 = null;
	JCheckBoxMenuItem menuItem42 = null;
	JCheckBoxMenuItem menuItem43 = null;
	JCheckBoxMenuItem menuItem45 = null;
	JCheckBoxMenuItem menuItem51 = null;
	JCheckBoxMenuItem menuItem52 = null;
	String [] tooltip22 = { 
			"Click to disable editing in the detail pane but enable hyperlinks",
			"Click to enable editing in the detail pane but disable hyperlinks"};
	String [] tooltip41 = { 
			"Click to disable hyperlinks but enable editing in the detail pane",
			"Click to enable hyperlinks but disable editing in the detail pane"};
	String [] popupHyp = {
			"Editing is now enabled. But unfortunately,\n" +
				"hyperlinks will not work any more.",
			"Hyperlinks are now enabled. But unfortunately,\n" +
				"editing the detail text is now impossible."};
	String [] onOff = {"off", "on"};
	String [] showHide = {"Hide", "Show"};
	public String about =  " ******** Provisional BANNER ********* " +
			"\r\n This is My Tool, Release 18 " + 
			"\r\n running on Java version " + System.getProperty("java.version") +
			"\r\n using components of de.deepamehta " + 
			"\r\n and edu.uci.ics.jung under GPL" +
			"\r\n ******** Provisional BANNER ********* ";
	public String preferences = "No preferences yet (no installation)";
	
	//	Graphics accessories
	String [][] nodePalette = 
		{{"#b200b2", "#0000ff", "#00ff00", "#ffff00", 	// purple, blue, green, yellow
		"#ffc800", "#ff0000", "#c0c0c0", "#808080"},	// orange, red, pale, dark
		{"#d2bbd2", "#bbbbff", "#bbffbb", "#ffff99", 
		"#ffe8aa", "#ffbbbb", "#eeeeee", "#ccdddd"}};
//		"#ffe8aa", "#ffbbbb", "#e0e0e0", "#a0a0a0"}};
	String [][] edgePalette = 
		{{"#b200b2", "#0000ff", "#00ff00", "#ffff00", 
		"#ffc800", "#ff0000", "#d8d8d8", "#a0a0a0"},
		{"#d2bbd2", "#bbbbff", "#bbffbb", "#ffff99", 
		"#ffe8aa", "#ffbbbb", "#f0f0f0", "#c0c0c0"}};
	String [][] menuPalette = 
		{{"#b200b2", "#0000ff", "#00ff00", "#ffff00", 	
		"#ffc800", "#ff0000", "#c0c0c0", "#808080"},	
		{"#b277b2", "#7777ff", "#77ff77", "#ffff77", 
		"#ffc877", "#ff7777", "#d8d8d8", "#b0b0b0"}};
	Point clickedSpot = null;
	Point translation = new Point(0, 0);
	Point roomNeeded = new Point(2, 2);
	Point upperLeft = new Point(3, 0);
	Point insertion = null;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	CentralityColoring centralityColoring;

	// Input/ output accessories
	NewStuff newStuff = null;
	CompositionWindow compositionWindow = null;
	String dataString = "";	
	int inputType = 0;
	String baseDir;
	String filename = "";
	String confirmedFilename = "";
	String lastHTMLFilename = "";
	boolean maybeJustPeek = true;

	//	Toggles
	int toggle4 = 0;   // => hide classicMenu 
	boolean isDirty = false;
	boolean presoSizedMode = false;
	boolean contextPasteAllowed = true;
	int paletteID = 1;

	// Placeholders
	GraphNode dummyNode = new GraphNode(-1, null, null, null, null);
	GraphNode selectedTopic = dummyNode;		// TODO integrate into Selection()
	GraphEdge dummyEdge = new GraphEdge(-1, dummyNode, dummyNode, null, null);
	GraphEdge selectedAssoc = dummyEdge;
	String initText = "<body><font color=\"gray\">" +
	"<em>Click a node for its details, ALT+drag " +
	"a node to connect it." + 
	"<br />&nbsp;<br />For help, right-click " +  
	"the canvas background.</em></font></body>";
	
	Selection selection = null;

//
//	Process menu clicks
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		JMenuItem item = (JMenuItem) e.getSource();
		JPopupMenu menu = (JPopupMenu) item.getParent();
		String menuID = menu.getLabel();	// the menuID is stored in the menu's label
		if (menuID == null) menuID = "Menu Bar";
		System.out.println("Action Performed (PS) " + command.toString() + " from menu " + menuID);

		// Open

		if (command == "insert" || command == "new") {
			openComposition();

		} else if (command == "imapimp") {
//			ImappingImport i = new ImappingImport(mainWindow, this);
			new ImappingImport(mainWindow, this);
			
		} else if (command == "open") {
			FileDialog fd = new FileDialog(mainWindow);
			fd.setMode(FileDialog.LOAD);
			fd.setMultipleMode(true);
			fd.setDirectory(baseDir);
			fd.setVisible(true);
			File f[] = fd.getFiles();
			// Compare transferTransferable() case File(s)
			dataString = "";
			int fileCount = 0;
			for (fileCount = 0; fileCount < f.length; fileCount++) {
				if (fileCount < 1) {
					dataString = f[fileCount].getAbsolutePath();
					inputType = 1;
				} else {
					dataString = dataString + "\r\n" + f[fileCount].getAbsolutePath();
					inputType = 4;
				}
				System.out.println(f[fileCount].getParent() + " " + f[fileCount].getName());
			}
			if (!dataString.isEmpty()) {
				newStuff.setInput(dataString, inputType);
				System.out.println("ist wie javaFileListFlavor: \r\n" );
				System.out.println("Filename is (" + fileCount + ") " + dataString);
			}

		// Quit

		} else if (command == "quit") {
			System.out.println("Quit");
			close();

		// Paste

		} else if (command == "paste") {
			System.out.println("Paste Action");
			beginLongTask();

			Transferable t = newStuff.readClipboard();
			if (!newStuff.transferTransferable(t)) {
				System.out.println("Error PS121");
			} else {
				System.out.println("PS: Pasted Stuff successfully processed.");
			}
			
		// Save	

		} else if (command == "Store") {
			if (confirmedFilename.isEmpty()) {
				if (askForFilename("xml")) {
					if (startStoring(confirmedFilename)) {
						setDirty(false);
					}
				}
			} else {
				if (startStoring(confirmedFilename)) {
					setDirty(false);
				}
			}
			
		// Save as
			
		} else if (command == "SaveAs") {
			if (askForFilename("xml")) {
				if (startStoring(confirmedFilename)) {
					setDirty(false);
				}
			}
			
		// Export to legacy format
			
		} else if (command == "export") {
				startExport();
				
		//	Various toggles	
			
		} else if (command == "ToggleHyp") {
			toggleHyp(1);
		} else if (command == "ToggleDetEdit") {
			toggleHyp(0);
			
		} else if (command == "TogglePreso") {
			graphPanel.togglePreso();
			
		} else if (command =="ToggleBorders") {
			graphPanel.toggleBorders();
			
		} else if (command == "TogglePalette") {
			paletteID = 1 - paletteID;

		} else if (command == "ToggleHeavy") {
			graphPanel.toggleAntiAliasing();
		
		} else if (command == "classicMenu") {
			toggleClassicMenu();
			toggle4 = 1 - toggle4;
			
		} else if (command == "power") {
			if (menuItem52.isSelected()) {
				if (!menuItem42.isSelected()) {
					graphPanel.toggleBorders();
					menuItem42.setSelected(true);
				}
				if (!menuItem23.isSelected()) {
					paletteID = 0;
					menuItem23.setSelected(true);
				}
				if (!menuItem45.isSelected()) {
					graphPanel.toggleAntiAliasing();
					menuItem45.setSelected(true);
				}
			} else {
				if (menuItem42.isSelected()) {
					graphPanel.toggleBorders();
					menuItem42.setSelected(false);
				}
				if (menuItem23.isSelected()) {
					paletteID = 1;
					menuItem23.setSelected(false);
				}
				if (menuItem45.isSelected()) {
					graphPanel.toggleAntiAliasing();
					menuItem45.setSelected(false);
				}
			}
			
		} else if (command == "?") {
			displayHelp();
			
		} else if (command == "about") {
				displayPopup(about);

		} else if (command == "prefs") {
			displayPopup(preferences);

		} else if (command == "centcol") {
			if (menuItem51.isSelected()) {
			centralityColoring = new CentralityColoring(nodes, edges);
				centralityColoring.changeColors();
			} else {
				centralityColoring.revertColors();
			}
			graphPanel.repaint();
			
		} else if (command == "experimental") {
			launchSibling();
			
		} else if (command == "wxp") {
			FileDialog fd = new FileDialog(mainWindow, "Specify filename", FileDialog.SAVE);
			fd.setFile("wxp.xml"); 
			fd.setVisible(true);
			if (fd.getFile() != null) {
			String storeFilename = fd.getFile();
			storeFilename = fd.getDirectory() + fd.getFile();

			try {
				File storeFile = new Export2WXP(nodes, edges).createTopicmapFile(storeFilename);
				storeFilename = storeFile.getName();
			} catch (IOException e2) {
				System.out.println("Error PS128" + e2);
			} catch (TransformerConfigurationException e2) {
				System.out.println("Error PS129" + e2);
			} catch (SAXException e2) {
				System.out.println("Error PS130" + e2);
			}
			}
		} else if (command == "imexp") {
			FileDialog fd = new FileDialog(mainWindow, "Specify filename", FileDialog.SAVE);
			fd.setFile("im.iMap"); 
			fd.setVisible(true);
			if (fd.getFile() != null) {
				String storeFilename = fd.getFile();
				storeFilename = fd.getDirectory() + fd.getFile();

				new ImappingExport(nodes, edges,storeFilename);
			}
			
		//	Context menu command

		// Node menu
		} else if (menuID.equals("node")) {

			String colorString = "";
			
			if (command == "purple") colorString = nodePalette[paletteID][0];
			if (command == "blue") colorString =  nodePalette[paletteID][1];
			if (command == "green") colorString =  nodePalette[paletteID][2];
			if (command == "yellow") colorString =  nodePalette[paletteID][3];
			if (command == "orange") colorString =  nodePalette[paletteID][4];
			if (command == "red") colorString =  nodePalette[paletteID][5];
			if (command == "lightGray") colorString =  nodePalette[paletteID][6];
			if (command == "gray") colorString =  nodePalette[paletteID][7];
			
			if (!colorString.isEmpty()) selectedTopic.setColor(colorString);

			if (command == "delTopic") {
				deleteNode(selectedTopic);
//				selection.mode = SELECTED_NONE;
//				selection.topic = null;
				graphSelected();
			}
			graphPanel.repaint();

		// Edge menu		
		} else if (menuID.equals("edge")) {

			String colorString = "";

			if (command == "purple") colorString = edgePalette[paletteID][0];
			if (command == "blue") colorString =  edgePalette[paletteID][1];
			if (command == "green") colorString =  edgePalette[paletteID][2];
			if (command == "yellow") colorString =  edgePalette[paletteID][3];
			if (command == "orange") colorString =  edgePalette[paletteID][4];
			if (command == "red") colorString =  edgePalette[paletteID][5];
			if (command == "lightGray") colorString =  edgePalette[paletteID][6];
			if (command == "gray") colorString =  edgePalette[paletteID][7];
			
			if (!colorString.isEmpty()) selectedAssoc.setColor(colorString);

			if (command == "delAssoc") {
				deleteEdge(selectedAssoc);
//				selection.mode = SELECTED_NONE;
//				selection.assoc = null;
				graphSelected();
			}
			graphPanel.repaint();

		//	Graph menu
		} else if (menuID.equals("graph")) {

			if (command == "NewNode") {
		    	translation = graphPanel.getTranslation();
		    	clickedSpot.translate(- translation.x, - translation.y);
				GraphNode node = createNode(clickedSpot);
				nodeSelected(node);

			} else if (command == "?") {
				displayHelp();

			} else if (command == "tmppaste") {
				DefaultEditorKit.PasteAction tmpPasteAction = new DefaultEditorKit.PasteAction();
				tmpPasteAction.actionPerformed(e);
			}
			endTask();
			
		} else if (command == "Print") {
			if (lastHTMLFilename.isEmpty()) {
				if (askForFilename("htm")) {
					new MakeHTML(true, nodes, edges, lastHTMLFilename, this);
				}
			} else {
				new MakeHTML(true, nodes, edges, lastHTMLFilename, this);
			}
			
		} else if (command == "MakeHTML") {
			if (lastHTMLFilename.isEmpty()) {
				if (askForFilename("htm")) {
					new MakeHTML(false, nodes, edges, lastHTMLFilename, this);
				}
			} else {
				new MakeHTML(false, nodes, edges, lastHTMLFilename, this);
			}
			
		} else {
			System.out.println("PS: Wrong action: " + command);
		}
	}

//
//	Main Window	
	
	public void createMainWindow(String title) {
		mainWindow = new JFrame(title) {
			private static final long serialVersionUID = 1L;

//			public void paint(Graphics g) {
//				try {
//					super.paint(g);
//				} catch (Throwable t) {
//					System.out.println("Error PS103 " + t);
//				}
//			}
		};
		
		mainWindow.setSize(960, 580);
		mainWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		mainWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		cp = mainWindow.getContentPane();
		JSplitPane splitPane = createMainGUI();

		myMenuBar = createMenuBar();
		if (showMenuBar) {
			mainWindow.setJMenuBar(myMenuBar);
		} else {
			JMenuBar nullMenuBar = new JMenuBar();
			mainWindow.setJMenuBar(nullMenuBar);
		}

		cp.add(splitPane);
		
	}

	public JSplitPane createMainGUI() {
		setSystemUI(true);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(960 - 232);
		splitPane.setResizeWeight(.8);
		splitPane.setDividerSize(8);
		splitPane.setToolTipText("sptt");

		graphPanel.setBackground(Color.WHITE);
//		graphPanel.setBackground(Color.decode("#ffffdd"));
		splitPane.setLeftComponent(graphPanel);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		
		labelBox = new JPanel();
		labelBox.setLayout(new BorderLayout());
		labelBox.add(new JLabel("Label", JLabel.CENTER));
		labelBox.setToolTipText("Short text that also appears on the map. To see it there, click the map.");
		labelField = new JTextField();
		labelBox.add(labelField,"South");
		Dimension dim = new Dimension(1400,150);
		labelBox.setMaximumSize(dim);

		rightPanel.add(labelBox,"North");
		
		JPanel detailBox = new JPanel();
		detailBox.setLayout(new BoxLayout(detailBox, BoxLayout.Y_AXIS));
		JLabel detailBoxLabel = new JLabel("Detail", JLabel.CENTER);
		detailBoxLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		detailBox.add(detailBoxLabel);
		detailBox.setToolTipText("More text about the selected node, always at yout fingertips.");
		detailBox.add(edi,"South");
		rightPanel.add(detailBox,"South");

		boolean dirtyBefore = isDirty;
		edi.setText("<html>Es ist untersagt, metallbeschichtete Luftballons mit Gasf�llung "
				+ "in Tunnelbahnh�fen mit elektrischer Oberleitung unverpackt "
				+ "<a href=\"http://x28hd.de\">mitzuf�hren</a>.</html>");
		isDirty = dirtyBefore;
//		edi.getTextComponent().setCaretPosition(18);
		
		splitPane.setRightComponent(rightPanel);
		splitPane.repaint();

		graphSelected();
		
		return splitPane;
}

//
//	Menu bar
		
	public JMenuBar createMenuBar() {

		shortcutMask = ActionEvent.CTRL_MASK;
		if (System.getProperty("os.name").equals("Mac OS X")) shortcutMask = ActionEvent.META_MASK;

		JMenuBar menuBar;
		menuBar = new JMenuBar();
		menuBar.setBackground(Color.white);

		//	File menu
		
		JMenu menu1;
		menu1 = new JMenu("File  ");
		menu1.setMnemonic(KeyEvent.VK_F);

		JMenuItem menuItem10 = new JMenuItem("New", new ImageIcon(getClass().getResource("new.gif")));
		menuItem10.setActionCommand("new");
		menuItem10.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask));
		menuItem10.setToolTipText("Start a new map via the Composition Window");
		menuItem10.addActionListener(this);
		menu1.add(menuItem10);

		JMenuItem menuItem11 = new JMenuItem("Open...",  new ImageIcon(getClass().getResource("open.gif")));
		menuItem11.setActionCommand("open");
		menuItem11.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask));
		menuItem11.setToolTipText("Open an existing map, or one or more files whose text or names are to be inserted");
		menuItem11.addActionListener(this);
		menu1.add(menuItem11);

		JMenuItem menuItem12 = new JMenuItem("Save", new ImageIcon(getClass().getResource("save.gif")));
		menuItem12.setActionCommand("Store");
		menuItem12.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask));
		menuItem12.addActionListener(this);
		menu1.add(menuItem12);

		JMenuItem menuItem13 = new JMenuItem("Save As...", KeyEvent.VK_A);
		menuItem13.setActionCommand("SaveAs");
		menuItem13.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, shortcutMask));
		menuItem13.addActionListener(this);
		menu1.add(menuItem13);

		JMenuItem menuItem14 = new JMenuItem("Print", KeyEvent.VK_P);
		menuItem14.setActionCommand("Print");
		menuItem14.setToolTipText("HTML graphics to zoom out");
		menuItem14.addActionListener(this);
		menu1.add(menuItem14);
		
		JMenuItem menuItem15 = new JMenuItem("Snapshot", KeyEvent.VK_H);
		menuItem15.setActionCommand("MakeHTML");
		menuItem15.setToolTipText("An HTML snapshot for interactive Read-Only mode");
		menuItem15.addActionListener(this);
		menu1.add(menuItem15);
		

		JMenuItem menuItem16 = new JMenuItem("Legacy Save...");
		menuItem16.setActionCommand("export");
		menuItem16.setToolTipText("Export to lagacy zip file format");
		menuItem16.addActionListener(this);
		menu1.add(menuItem16);

		menu1.addSeparator();

		JMenuItem menuItem17 = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuItem17.setActionCommand("quit");
		if (!System.getProperty("os.name").equals("Mac OS X"))
			menuItem17.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, shortcutMask));
		menuItem17.addActionListener(this);
		menu1.add(menuItem17);

		//	Edit menu
		
		JMenu menu2;
		menu2 = new JMenu("Edit  ");
		menu2.setMnemonic(KeyEvent.VK_E);

		menuItem21 = new JMenuItem("Paste", new ImageIcon(getClass().getResource("paste.gif")));
		menuItem21.setActionCommand("paste");
		menuItem21.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask));
		menuItem21.setToolTipText("Paste contents of the system clipboard");
		menuItem21.addActionListener(this);
		menu2.add(menuItem21);

		menuItem22 = new JCheckBoxMenuItem("Detail Pane", true);
		menuItem21.setMnemonic(KeyEvent.VK_D);
		menuItem22.setActionCommand("ToggleDetEdit");
		menuItem22.setToolTipText(tooltip22[0]);
		menuItem22.addActionListener(this);
		menu2.add(menuItem22);
		
		menuItem23 = new JCheckBoxMenuItem("Lurid Colors", false);
		menuItem23.setMnemonic(KeyEvent.VK_L);
		menuItem23.setActionCommand("TogglePalette");
		menuItem23.setToolTipText("Color scheme for new nodes and edges");
		menuItem23.addActionListener(this);
		menu2.add(menuItem23);
		

		//	Insert menu
		
		JMenu menu3;
		menu3 = new JMenu("Insert  ");
		menu3.setMnemonic(KeyEvent.VK_I);

		JMenuItem menuItem31 = new JMenuItem("Insert Items",  KeyEvent.VK_I);
		menuItem31.setActionCommand("insert");
		menuItem31.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcutMask));
		menuItem31.setToolTipText("Paste, Drop, or Type into a Composition Window");
		menuItem31.addActionListener(this);
		menu3.add(menuItem31);

		JMenuItem menuItem32 = new JMenuItem("Import iMap",  KeyEvent.VK_R);
		menuItem32.setActionCommand("imapimp");
		menuItem32.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutMask));
		menuItem32.setToolTipText("Import from iMapping");
		menuItem32.addActionListener(this);
		menu3.add(menuItem32);

		//	Export menu
		
		JMenu menu7;	// TODO change number
		menu7 = new JMenu("Export  ");
		menu7.setMnemonic(KeyEvent.VK_E);

		JMenuItem menuItem71 = new JMenuItem("to interactive HTML page", KeyEvent.VK_H);
		menuItem71.setActionCommand("MakeHTML");
		menuItem71.setToolTipText("An HTML snapshot for interactive Read-Only mode");
		menuItem71.addActionListener(this);
		menu7.add(menuItem71);
		
		JMenuItem menuItem72 = new JMenuItem("to printable HTML page", KeyEvent.VK_P);
		menuItem72.setActionCommand("Print");
		menuItem72.setToolTipText("HTML graphics to zoom out");
		menuItem72.addActionListener(this);
		menu7.add(menuItem72);
		
		JMenuItem menuItem73 = new JMenuItem("to Wordpress WXP format",  KeyEvent.VK_W);
		menuItem73.setActionCommand("wxp");
		menuItem73.setToolTipText("<html><body><em>(Wordpress Export Format)</em></body></html>");
		menuItem73.addActionListener(this);
		menu7.add(menuItem73);
		
		JMenuItem menuItem74 = new JMenuItem("to iMapping iMap file",  KeyEvent.VK_I);
		menuItem74.setActionCommand("imexp");
		menuItem74.setToolTipText("<html><body><em>(Think Tool iMapping,info)</em></body></html>");
		menuItem74.addActionListener(this);
		menu7.add(menuItem74);
		
		//	View menu
		
		JMenu menu4;
		menu4 = new JMenu("View  ");
		menu4.setMnemonic(KeyEvent.VK_V);

		menuItem41 = new JCheckBoxMenuItem("Hyperlinks", false);
		menuItem41.setActionCommand("ToggleHyp");
		menuItem41.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, shortcutMask));
		menuItem41.setToolTipText(tooltip41[1]);
		menuItem41.addActionListener(this);
		menu4.add(menuItem41);

		menuItem45 = new JCheckBoxMenuItem("Accelerate", false);
		menuItem45.setActionCommand("ToggleHeavy");
		menuItem45.setToolTipText("Fast but coarse graphics");
		menuItem45.addActionListener(this);
		menu4.add(menuItem45);

		menuItem42 = new JCheckBoxMenuItem("Borders", false);
		menuItem42.setActionCommand("ToggleBorders");
		menuItem42.setToolTipText("Display arrows pointing to lost areas");
		menuItem42.addActionListener(this);
		menu4.add(menuItem42);

		menuItem43 = new JCheckBoxMenuItem("Menu Bar", true);
		menuItem43.setActionCommand("classicMenu");
		menuItem43.setToolTipText("Hide menu bar (restore via rightclick on canvas)");
		menuItem43.addActionListener(this);
		menu4.add(menuItem43);

		JCheckBoxMenuItem menuItem44 = new JCheckBoxMenuItem("Big Icons", false);
		menuItem44.setActionCommand("TogglePreso");
		menuItem44.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, shortcutMask));
		menuItem44.setToolTipText("Presentation Mode");
		menuItem44.addActionListener(this);
		menu4.add(menuItem44);

		//	Tools menu
		
		JMenu menu5;
		menu5 = new JMenu("Tools  ");
		menu5.setMnemonic(KeyEvent.VK_T);

		menuItem51 = new JCheckBoxMenuItem("Centrality Heatmap", false);
		menuItem51.setActionCommand("centcol");
		menuItem51.setToolTipText("Warmer colors represent higher betweenness centrality");
		menuItem51.addActionListener(this);
		menu5.add(menuItem51);
		
		menu5.addSeparator();
		
		menuItem52 = new JCheckBoxMenuItem("Power User Mode", false);
		menuItem52.setActionCommand("power");
		menuItem52.setToolTipText("Acceleration, Lurid Colors, and Borders");
		menuItem52.addActionListener(this);
		menu5.add(menuItem52);
		
		JMenuItem menuItem54 = new JMenuItem("Preferences",  KeyEvent.VK_S);
		menuItem54.setActionCommand("prefs");
		menuItem54.setToolTipText("<html><body><em>(Not yet interesting)</em></body></html>");
		menuItem54.addActionListener(this);
		menu5.add(menuItem54);
		
		JMenuItem menuItem53 = new JMenuItem("Experimantal 1",  KeyEvent.VK_E);
		menuItem53.setActionCommand("experimental");
		menuItem53.setToolTipText("<html><body><em>(Tries to launch another window)</em></body></html>");
		menuItem53.addActionListener(this);
		menu5.add(menuItem53);
		
		
		//	Help menu
		
		JMenu menu6;
		menu6 = new JMenu("?");
		menu6.setMnemonic(KeyEvent.VK_H);

		JMenuItem menuItem61 = new JMenuItem("Help",  KeyEvent.VK_H);
		menuItem61.setActionCommand("?");
		menuItem61.addActionListener(this);
		menu6.add(menuItem61);
		
		menu6.addSeparator();

		JMenuItem menuItem62 = new JMenuItem("About",  KeyEvent.VK_A);
		menuItem62.setActionCommand("about");
		menuItem62.addActionListener(this);
		menu6.add(menuItem62);

		menuBar.add(menu1);
		menuBar.add(menu2);
		menuBar.add(menu3);
		menuBar.add(menu7);
		menuBar.add(menu4);
		menuBar.add(menu5);
		menuBar.add(menu6);
		return menuBar;
	}

//
//	Context Menu

	public void displayContextMenu(String menuID, int x, int y) {
		JPopupMenu menu = new JPopupMenu(menuID);
		clickedSpot = new Point(x, y);
		menu.addPopupMenuListener(this);
		setSystemUI(false); //	avoid confusing colors when hovering, and indenting  of items in System LaF 

		if (menuID.equals("graph")) {
			JMenuItem item = new JMenuItem();
			item.addActionListener(this);
			item.setActionCommand("Store");
			item.setText("Save map...");
			menu.add(item);

			JMenuItem item2 = new JMenuItem();
			item2.addActionListener(this);
			item2.setActionCommand("NewNode");
			item2.setText("New node");
			menu.add(item2);

			JMenuItem menuItem71 = new JMenuItem("Paste here", KeyEvent.VK_V);
			menuItem71.setActionCommand("paste");
			menuItem71.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask));
			menuItem71.addActionListener(this);
			if (!contextPasteAllowed) menuItem71.setEnabled(false);
			menu.add(menuItem71);

			if (!menuItem43.isSelected()) {
			JMenuItem item9 = new JMenuItem();
			item9.addActionListener(this);
			item9.setActionCommand("classicMenu");
			item9.setText(showHide[toggle4] + " Classic Menu");
			menu.add(item9);
			}

			JMenuItem item3 = new JMenuItem();
			item3.addActionListener(this);
			item3.setActionCommand("?");
			item3.setText("Help");
			menu.add(item3);

		} else if (menuID.equals("node")) {
			
			menu.add(styleSwitcher(1, "purple", menuPalette[paletteID][0], true));
			menu.add(styleSwitcher(2, "blue", menuPalette[paletteID][1], true));
			menu.add(styleSwitcher(3, "green", menuPalette[paletteID][2], false));
			menu.add(styleSwitcher(4, "yellow", menuPalette[paletteID][3], false));
			menu.add(styleSwitcher(5, "orange", menuPalette[paletteID][4], false));
			menu.add(styleSwitcher(6, "red", menuPalette[paletteID][5], true));
			menu.add(styleSwitcher(7, "lightGray", menuPalette[paletteID][6], false));
			menu.add(styleSwitcher(8, "gray", menuPalette[paletteID][7], true));

			menu.addSeparator();

			JMenuItem delTopic = new JMenuItem();
			delTopic.addActionListener(this);
			delTopic.setActionCommand("delTopic");
			delTopic.setText("Delete");
			menu.add(delTopic);

		} else if (menuID.equals("edge")) {

			menu.add(styleSwitcher(1, "purple", menuPalette[paletteID][0], true));
			menu.add(styleSwitcher(2, "blue", menuPalette[paletteID][1], true));
			menu.add(styleSwitcher(3, "green", menuPalette[paletteID][2], false));
			menu.add(styleSwitcher(4, "yellow", menuPalette[paletteID][3], false));
			menu.add(styleSwitcher(5, "orange", menuPalette[paletteID][4], false));
			menu.add(styleSwitcher(6, "red", menuPalette[paletteID][5], true));
			menu.add(styleSwitcher(7, "lightGray", menuPalette[paletteID][6], false));
			menu.add(styleSwitcher(8, "gray", menuPalette[paletteID][7], true));

			menu.addSeparator();

			JMenuItem delAssoc = new JMenuItem();
			delAssoc.addActionListener(this);
			delAssoc.setActionCommand("delAssoc");
			delAssoc.setText("Delete");
			menu.add(delAssoc);
		}

		menu.show(cp, x, y);
	}

	public void popupMenuCanceled(PopupMenuEvent arg0) {
		setSystemUI(true);
	}
	public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
		setSystemUI(true);
	}
	public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
	}

	public void setSystemUI(boolean toggle) {
		if (toggle) {
			try {	//	switch back LaF after context menus  TODO also when leaving the menu without choice
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (UnsupportedLookAndFeelException e2) {
				System.out.println("Error PS104 " + e2);
			} catch (ClassNotFoundException e2) {
				System.out.println("Error PS105" + e2);
			} catch (InstantiationException e2) {
				System.out.println("Error PS106 " + e2);
			} catch (IllegalAccessException e2) {
				System.out.println("Error PS107 " + e2);
			}  
		} else {
			try {
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			} catch (UnsupportedLookAndFeelException e) {
				System.out.println("Error PS144 " + e);
			} catch (ClassNotFoundException e) {
				System.out.println("Error PS145" + e);
			} catch (InstantiationException e) {
				System.out.println("Error PS146 " + e);
			} catch (IllegalAccessException e) {
				System.out.println("Error PS147 " + e);
			}  
		}
	}
	
	public JMenuItem styleSwitcher(int styleNumber, String styleName, String color, boolean reverse) {
		JMenuItem styleItem = new JMenuItem();
		styleItem.addActionListener(this);
		styleItem.setActionCommand(styleName);
		styleItem.setOpaque(true);
		styleItem.setBackground(Color.decode(color));
		if (reverse) styleItem.setForeground(Color.white);
		styleItem.setText("Change color");
		if (styleNumber == 7) styleItem.setText("Pale");
		if (styleNumber == 8) styleItem.setText("Dark");
		return styleItem;
	}

	public void displayPopup(String msg) {
		JOptionPane.showMessageDialog(mainWindow, msg);
	}

	public void displayHelp() {
		JOptionPane.showMessageDialog(mainWindow,"Provisional Help\n" +
	"New features since release 16 not yet described.\n" +
	"\n" +
	"Nodes:\n" +			
	"(Left-) Click on a node to view its detail in the right panel;\n" +
	"(Left-) Drag a node to move it;\n" +
	"Middlebutton-Drag a node to connect it with another one;\n" +
	"Right-Click a node to change its style or delete it;\n" +
	"\n" +
	"Lines:\n" +			
	"(Left-) Drag a connector-line to move all connected nodes;\n" +
	"Right-Click a line to change its style or delete it;\n" +
	"To create a line, drag a node with the Middle Mouse button or ALT.\n" +
	"\n" +
	"Background:\n" +			
	"Right-Click the canvas background to \n" +
	"-- create a new node,\n" +
	"-- save your map to Desktop\\deepamehta-files\\,\n" +
	"-- or view this help;\n" +
	"(Left-) Drag the canvas background to move all nodes;\n" +
	"\n" +
	"In the right panel,\n" + 
	"-- click the \"B\"/ \"I\"/ \"U\" for bold/ italic/ underline,\n" +
	"-- click the \"B+\" Bold Special to add the marked text to\n" +
	"   the node's label above and on the map\n" +
	"\n" +
	"In the Paste Window at Startup map creation,\n" +
	"-- Tab-separated text lines are interpreted as label + detail;\n" +
	"-- short lines are used as labels;\n" +
	"-- longer lines will be numbered, instead;\n" +
	"-- if most lines have a comma near the beginning,\n" +
	"   the text is treated as a reference list.");
	}
	
	public void toggleHyp(int whichWasToggled) {
		
		edi.toggleHyp();
		if (whichWasToggled == 1) { 	// hyperlinks 
			menuItem22.setSelected(!menuItem22.isSelected());
		} else {
			menuItem41.setSelected(!menuItem41.isSelected());
		}
		int stateHyp = 0;
		if (menuItem41.isSelected()) stateHyp = 1;
		displayPopup(popupHyp[stateHyp]);
		menuItem22.setToolTipText(tooltip22[stateHyp]);
		menuItem41.setToolTipText(tooltip41[1 - stateHyp]);
	}

	public void toggleClassicMenu() {
		showMenuBar = !showMenuBar;
		if (showMenuBar) {
//			myMenuBar = createMenuBar();
			menuItem43.setSelected(true);
			mainWindow.setJMenuBar(myMenuBar);
			mainWindow.validate();
			cp.repaint();
		} else {
			JMenuBar nullMenuBar = new JMenuBar();
			mainWindow.setJMenuBar(nullMenuBar);
			mainWindow.validate();
			cp.repaint();
		}
	}
	
	public void openComposition() {
		compositionWindow = new CompositionWindow(this);
		menuItem21.setEnabled(false);	// Main Menu Paste
		contextPasteAllowed = false;	// Context Menu Paste
		newStuff.setCompositionMode(true);
	
	}

	public void setDirty(boolean toggle) {
		isDirty = toggle;
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
		mainWindow.setVisible(true);
		//	For compatibility, prompt a compositionWindow
		if (filename.isEmpty()) openComposition();
	}
	
	//	Input from start parameters	
	public void setFilename(String arg, int type) {
		System.out.println("PS newstuff = " + newStuff);
		filename = arg;
		if (filename.endsWith(".xml")) confirmedFilename = filename;
		maybeJustPeek = true;
	}
	
	public synchronized void initialize() {
		if (System.getProperty("os.name").equals("Mac OS X")) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");	// otherwise very alien for Mac users
			new AppleHandler(this);			// for QuitHandler
			com.apple.eawt.Application app = com.apple.eawt.Application.getApplication();
			java.awt.Image dockImage = (new ImageIcon(getClass().getResource("logo.png"))).getImage();
			app.setDockIconImage(dockImage);
		}
		baseDir = null;
		try {
			baseDir = System.getProperty("user.home") + File.separator + "Desktop";
			System.out.println("PS: Base directory is: " + baseDir);
		} catch (Throwable e) {
			System.out.println("Error PS108" + e );
		}

		newStuff = new NewStuff(this);
		
		graphPanel = new GraphPanel(this);
		nodes = new Hashtable<Integer, GraphNode>();
		edges = new Hashtable<Integer, GraphEdge>();
		graphPanel.setModel(nodes, edges);
		selection = graphPanel.getSelectionInstance();	//	TODO eliminate again
		about = (new AboutBuild()).getAbout();

		createMainWindow("Main Window");
		System.out.println("PS: Initialized");
		
//		newStuff.tmpInit();
		if (!filename.isEmpty()) newStuff.setInput(filename, 1);
	}
	
//	Saving and finishing

	public boolean askForFilename(String extension) {
		FileDialog fd = new FileDialog(mainWindow, "Specify filename", FileDialog.SAVE);
		String newName; 
		int offs = filename.lastIndexOf(".");
		if (filename.isEmpty() || offs < 0) {
			newName = baseDir + File.separator + "storefile." + extension;
			System.out.println(baseDir + " + " +  File.separator + " + " + 
					"(default)" + " = " + newName);
		} else { 
			newName = filename.substring(0, offs) + "." + extension;
//			newName = (new File(filename)).getName();
//			newName = newName.substring(0, newName.lastIndexOf(".")) + "." + extension;
		}
		if (System.getProperty("os.name").equals("Mac OS X")) {
			
			newName = newName.substring(newName.lastIndexOf(File.separator) + 1);
		}
		fd.setFile(newName);
		fd.setDirectory(baseDir);
		fd.setVisible(true);
		if (fd.getFile() == null) {
			System.out.println("PS: File dialog aborted.");
			return false; 
		}
		newName = fd.getDirectory() + fd.getFile();
		System.out.println(fd.getDirectory() + " + " + fd.getFile() + " = " + newName);
		if (extension == "xml") {
			confirmedFilename = newName;
		} else if (extension == "htm") {
			lastHTMLFilename = newName;
		} else {
			System.out.println("Error PS121");
			return false;
		}
		return true;
	}
	
	public boolean startStoring(String storeFilename) {
		System.out.println("PS: Store is starting...");
		if (storeFilename.isEmpty()) return false;
		System.out.println("PS: Storing in progress. " + storeFilename);
		try {
			File storeFile = new TopicMapStorer(nodes, edges).createTopicmapFile(storeFilename);
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
		System.out.println("PS: Storing finished: " + storeFilename);
		isDirty = false;
		edi.setDirty(false);
		return true;
	}

	public void startExport() {
		System.out.println("PS: Export is starting...");
		FileDialog fd = new FileDialog(mainWindow, "Select Zip File to Update");
		fd.setMode(FileDialog.LOAD);
		fd.setMultipleMode(false);
		fd.setDirectory(baseDir);
		fd.setVisible(true);
		try {
			String zipFilename = fd.getDirectory() + File.separator + fd.getFile();
			System.out.println("PS: Selected " + zipFilename);
			new TopicMapExporter(nodes, edges).createTopicmapArchive(zipFilename);
			System.out.println("PS: Export finished. " + zipFilename);
		} catch (IOException e) {
			System.out.println("Error PS127 " + e);
		}
	}

	public boolean close() {
		Object[] closeOptions =  {"Save", "Discard changes", "Cancel"};
		int closeResponse = JOptionPane.YES_OPTION;
		if (isDirty) {
			closeResponse = JOptionPane.showOptionDialog(null,
//				"If you have unsaved changes, consider saving the map.\n" + 
//				"The default location for new maps is \"savefile.zip\"\n" +
//				" in your \"Desktop\\deepamehta-files\" folder.\n", 
					"Do you want to save your changes?\n",
					"Warning", JOptionPane.YES_NO_CANCEL_OPTION, 
					JOptionPane.WARNING_MESSAGE, null, 
					closeOptions, closeOptions[0]);  
			if (closeResponse == JOptionPane.CANCEL_OPTION ||
					closeResponse == JOptionPane.CLOSED_OPTION) {
				return false;
			} else if (closeResponse != JOptionPane.NO_OPTION) {
				System.out.println("saving scheduled");
				if (confirmedFilename.isEmpty()) {
					if (askForFilename("xml")) {
						confirmedFilename = filename;
						if (!startStoring(confirmedFilename)) {
							return false;
						}
					} else {
						return false;
					}
				} else {
					if (!startStoring(confirmedFilename)) {
						return false;
					}
				}
			}
		}

		mainWindow.dispose();
		System.out.println("Closed");
		System.exit(0);
		return true;
	}


//	Selection processing

	public void deselect(GraphNode node) {
		if (!node.equals(dummyNode)) {
//			String nodeName = node.getLabel();
			node.setLabel(labelField.getText());
			node.setDetail(edi.getText());
			edi.setText("");
			labelField.setText("");
		}
	}	

	public void deselect(GraphEdge edge) {
		if (!edge.equals(dummyEdge)) {
			edge.setDetail(edi.getText());
			edi.setText("");
		}
	}	
	
	public void nodeSelected(GraphNode node) {
		System.out.println("PS: Node selected");
		deselect(selectedTopic);
		deselect(selectedAssoc);
		selectedAssoc = dummyEdge;
		selectedTopic = node;
		edi.setText((selectedTopic).getDetail());
		edi.setDirty(false);
//		edi.getTextComponent().setCaretPosition(0);
		edi.getTextComponent().requestFocus();
		String labelText = selectedTopic.getLabel();
		labelField.setText(labelText);
		edi.repaint();
	}
	
	public void edgeSelected(GraphEdge edge) {
		System.out.println("PS: Edge selected");
		deselect(selectedAssoc);
		deselect(selectedTopic);
		selectedTopic = dummyNode;
		selectedAssoc = edge;
		edi.setText(selectedAssoc.getDetail());
		edi.getTextComponent().setCaretPosition(0);
		edi.getTextComponent().requestFocus();
		edi.repaint();
	}

	public void graphSelected() {
		System.out.println("PS: Graph selected");
		deselect(selectedTopic);
		selectedTopic = dummyNode;
		deselect(selectedAssoc);
		selectedAssoc = dummyEdge;
		edi.setText(initText);
		graphPanel.grabFocus();		//  was crucial
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
			GraphNode topic = new GraphNode(newId, xy, Color.decode(nodePalette[paletteID][7]), "", "");
			nodes.put(newId, topic);
			updateBounds();
			graphPanel.nodeSelected(topic);
			graphPanel.repaint();
//			edi.setDirty(false);
			setDirty(true);
			return topic;
		} else {
			return null;
		}
	}
	
	public GraphEdge createEdge(GraphNode topic1, GraphNode topic2) {
		if (topic1 != null && topic2 != null) {
			int newId = newKey(edges.keySet());
			GraphEdge assoc = new GraphEdge(newId, topic1, topic2, Color.decode(edgePalette[paletteID][7]), "");  // nicht 239
			assoc.setID(newId);
			edges.put(newId, assoc);
			
			topic1.addEdge(assoc);
			topic2.addEdge(assoc);
			
			setDirty(true);
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
		Enumeration<GraphEdge> e = topic.getEdges();
		Vector<GraphEdge> todoList = new Vector<GraphEdge>();
		GraphEdge edge;
		int neighborCount = 0;
		while (e.hasMoreElements()) {
			edge = (GraphEdge) e.nextElement();
			todoList.addElement(edge);
			neighborCount++;
		};
		String topicName = topic.getLabel();
		if (topicName.length() > 30) topicName = topicName.substring(0,30) + "...";
		int response = JOptionPane.showConfirmDialog(OK, 
				"Are you sure you want to delete " + "the topic \n \"" + 
				topicName +	"\" with " + neighborCount + " associations ?");
		if (response != JOptionPane.YES_OPTION) return;

		Enumeration<GraphEdge> e2 = todoList.elements();
		while (e2.hasMoreElements()) {
			edge = (GraphEdge) e2.nextElement();
			deleteEdge(edge, true);
		}
		int topicKey = topic.getID();
		nodes.remove(topicKey);
		updateBounds();
		setDirty(true);
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
					"Are you sure you want to delete " + "the association \n" + 
					"from \"" + topicName1 + "\" to \""+ topicName2 + "\" ?");
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
		graphPanel.repaint();
		setDirty(true);
		return;
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

	public void addToLabel(String textToAdd) {
		if (selectedTopic == dummyNode) return;
		String oldText = labelField.getText();
		String newText = oldText + " " + textToAdd;
		labelField.setText(newText);
		System.out.println(newText);
		GraphNode justUpdated = selectedTopic;
		graphSelected();
		graphPanel.labelUpdateToggle(true);
		nodeSelected(justUpdated);
		graphPanel.labelUpdateToggle(false);
		mainWindow.repaint();   // this was crucial
	}

//
//	Accessories for cursors and carets
    
	public void beginTranslation() {
		System.out.println("PS Hand cursor");
		setHandCursor();
	}

	public void beginCreatingEdge() {
		setCrosshairCursor();
	}

	void setWaitCursor() {
		setMouseCursor(Cursor.WAIT_CURSOR);
		graphPanel.repaint();
	}

	void setHandCursor() {
		setMouseCursor(Cursor.HAND_CURSOR);
	}

	public void setCrosshairCursor() {
		setMouseCursor(Cursor.CROSSHAIR_CURSOR);
	}
	
	public void setDefaultCursor() {
		setMouseCursor(Cursor.DEFAULT_CURSOR);
	}

	void setMouseCursor(int cursorType) {
		mainWindow.setCursor(new Cursor(cursorType));
	}
	
	public void beginLongTask() {
		setWaitCursor();
	}
	public void endTask() {
		setDefaultCursor();
	}
	
//    public void caretUpdate(CaretEvent arg0) {
//		System.out.println("PS dot " + arg0.getDot());
//	}
	
//
//	Misc and temp
    
	// tmp diagnostic
	public void findAboutFocus() {
		System.out.println("graphPanel? " + graphPanel.hasFocus());
		System.out.println("labelField? " + labelField.hasFocus());
		System.out.println("edi? " + edi.hasFocus());
		System.out.println("compositionWindow? " + getCWInstance().compositionWindow.hasFocus());
	}

	// (placeholder)
		public void manip(int x) {
//			this.x = x;
//			GraphNode testnode = new GraphNode(4, new Point(x, 170), Color.gray, "Testnode", "Tesdetails");
//			nodes.put(4,testnode);
//			System.out.println(edi.getText());
//			graphPanel.repaint();
		}

		public void launchSibling() {
			File jarfile = new File(baseDir + "/fromscratch.jar");
			try {
				Runtime.getRuntime().exec(new String[] {"java", "-jar", jarfile.getAbsolutePath() });
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	
//
//	Communication with other clases
    
    public NewStuff getNSInstance() {
    	return newStuff;
    }
    
    public CompositionWindow getCWInstance() {
    	return compositionWindow;
    }
    
   public void finishCompositionMode() {
    	newStuff.setCompositionMode(false);
    	menuItem21.setEnabled(true);
    	contextPasteAllowed = true;
    	setSystemUI(true);
    }
   
   public String getNewNodeColor() {
	   return nodePalette[paletteID][7];
   }
    
    public void triggerUpdate(boolean justOneMap) {
    	System.out.println("TriggerUpdate");
    	if (maybeJustPeek && justOneMap && nodes.size() < 1) {
    		//  don't set dirty yet
    		maybeJustPeek = false;
    	} else {
    		setDirty(true);
    	}
    	Hashtable<Integer, GraphNode> newNodes = newStuff.getNodes();
    	Hashtable<Integer, GraphEdge> newEdges = newStuff.getEdges();
    	insertion = newStuff.getInsertion();
    	System.out.println("PS: mouse position while inserting stuff: " + insertion);
    	if (filename.isEmpty()) filename = newStuff.getAdvisableFilename();

    	IntegrateNodes integrateNodes = new IntegrateNodes(nodes, edges, newNodes, newEdges, insertion);
 
    	roomNeeded = newStuff.roomNeeded();
    	System.out.println("PS: Room needed: " + roomNeeded.x + ", " + roomNeeded.y);
    	translation = graphPanel.getTranslation();
    	
		if (insertion == null) insertion = upperLeft;
    	integrateNodes.driftNodes(translation, roomNeeded, this.bounds, insertion, 
    			new Point(graphPanel.getWidth(), graphPanel.getHeight()));
    	upperLeft = integrateNodes.getUpperLeft();
		if (upperLeft == null) {
			upperLeft = new Point(0, bounds.y + bounds.height + 20);
			int beyond = bounds.height - graphPanel.getHeight() + translation.y;
			System.out.println("PS: bounds.height " + bounds.height + ", beyond " + beyond);
			if (beyond > -100) {
				int scroll = - beyond - 200;
				graphPanel.translateGraph(0, scroll);
				graphPanel.repaint();
				upperLeft = new Point(0, graphPanel.getHeight() - 100);
			}
		}
    	integrateNodes.mergeNodes(upperLeft, roomNeeded, translation, this.insertion);
    	nodes = integrateNodes.getNodes();
    	edges = integrateNodes.getEdges();
    	
		graphPanel.setModel(nodes, edges);
		updateBounds();
		setDefaultCursor();
		graphPanel.repaint();
    }
}
package de.x28hd.tool;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

public class Gui {
	
//
//	Contains buttons and (yet) only closely related toggles
	
	PresentationService controler;
	GraphPanel graphPanel;
	TextEditorPanel edi;
	NewStuff newStuff;

	JMenuBar menuBar;

	int shortcutMask;
//	JMenuBar myMenuBar = null;
//	boolean showMenuBar = true;
	JMenuItem menuItem21 = null;
	JRadioButtonMenuItem menuItem22 = null;
	JCheckBoxMenuItem menuItem23 = null;
	JCheckBoxMenuItem menuItem24 = null;
	JCheckBoxMenuItem menuItem25 = null;
	JCheckBoxMenuItem menuItem26 = null;
	JCheckBoxMenuItem menuItem27 = null;
	JMenuItem menuItem91 = null;
	JMenuItem menuItem92 = null;
	JMenuItem menuItem93 = null;
	JMenuItem menuItem94 = null;
	JMenuItem menuItem95 = null;
	JMenuItem menuItem98 = null;
	JRadioButtonMenuItem menuItem41 = null;
	JCheckBoxMenuItem menuItem42 = null;
	JCheckBoxMenuItem menuItem43 = null;
	JCheckBoxMenuItem menuItem45 = null;
	JCheckBoxMenuItem menuItem46 = null;
	JCheckBoxMenuItem menuItem47 = null;
	JCheckBoxMenuItem menuItem51 = null;
	JCheckBoxMenuItem menuItem52 = null;
	JCheckBoxMenuItem menuItem55 = null;
	JCheckBoxMenuItem menuItem58 = null;
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
	JPopupMenu menu;
	String [] showHide = {"Hide", "Show"};
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
	
	
	public Gui(PresentationService ps, GraphPanel gp, TextEditorPanel te, NewStuff ns) {
		controler = ps;
		graphPanel = gp;
		edi = te;
		newStuff = ns;
	}

	public JMenuBar createMenuBar() {

		shortcutMask = ActionEvent.CTRL_MASK;
		if (System.getProperty("os.name").equals("Mac OS X")) shortcutMask = ActionEvent.META_MASK;

		menuBar = new JMenuBar();
		
		fileMenu();
		editMenu();
		insertMenu();
		exportMenu();
//		viewMenu();
		advancedMenu();
		helpMenu();
		
		return menuBar;
	}
	
	public void fileMenu() {

		JMenu menu1;
		menu1 = new JMenu("File  ");

		JMenuItem menuItem10 = new JMenuItem("New", new ImageIcon(getClass().getResource("new.gif")));
		menuItem10.setActionCommand("new");
		menuItem10.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask));
		menuItem10.setToolTipText("Start a new map via the Composition Window");
		menuItem10.addActionListener(controler);
		menu1.add(menuItem10);

		JMenuItem menuItem11 = new JMenuItem("Open...",  new ImageIcon(getClass().getResource("open.gif")));
		menuItem11.setActionCommand("open");
		menuItem11.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask));
		menuItem11.setToolTipText("Open an existing map, or one or more files whose text or names are to be inserted");
		menuItem11.addActionListener(controler);
		menu1.add(menuItem11);

		JMenuItem menuItem12 = new JMenuItem("Save", new ImageIcon(getClass().getResource("save.gif")));
		menuItem12.setActionCommand("Store");
		menuItem12.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask));
		menuItem12.addActionListener(controler);
		menu1.add(menuItem12);

		JMenuItem menuItem13 = new JMenuItem("Save As...");
		menuItem13.setActionCommand("SaveAs");
		menuItem13.addActionListener(controler);
		menu1.add(menuItem13);

		JMenuItem menuItem14 = new JMenuItem("Print ?");
		menuItem14.setActionCommand("HowToPrint");
		menuItem14.setToolTipText("How to print");
		menuItem14.addActionListener(controler);
		menu1.add(menuItem14);

		menu1.addSeparator();

//		JMenuItem menuItem17 = new JMenuItem("Quit", KeyEvent.VK_Q);
		JMenuItem menuItem17 = new JMenuItem("Quit");
		menuItem17.setActionCommand("quit");
		if (!System.getProperty("os.name").equals("Mac OS X"))
			menuItem17.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, shortcutMask));
		menuItem17.addActionListener(controler);
		menu1.add(menuItem17);

		menuBar.add(menu1);
	}

	public void editMenu() {

		JMenu menu2;
		menu2 = new JMenu("Edit  ");

		menuItem91 = new JMenuItem("Undo");
		menuItem91.setActionCommand("undo");
		menuItem91.setEnabled(false);
		menuItem91.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask));
		menuItem91.setToolTipText("Revert last map change");
		menuItem91.addActionListener(controler);
		menu2.add(menuItem91);

		menuItem92 = new JMenuItem("Redo");
		menuItem92.setActionCommand("redo");
		menuItem92.setEnabled(false);
		menuItem92.setToolTipText("Revert last undo");
		menuItem92.addActionListener(controler);
		menu2.add(menuItem92);

		menu2.addSeparator();

		menuItem93 = new JMenuItem("Cut");
		menuItem93.setActionCommand("cut");
		menuItem93.setEnabled(false);
		menuItem93.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcutMask));
		menuItem93.setToolTipText("Cut the content of the rubberband rectangle");
		menuItem93.addActionListener(controler);
		menu2.add(menuItem93);

		menuItem94 = new JMenuItem("Copy");
		menuItem94.setActionCommand("copy");
		menuItem94.setEnabled(false);
		menuItem94.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask));
		menuItem94.setToolTipText("Copy the content of the rubberband rectangle");
		menuItem94.addActionListener(controler);
		menu2.add(menuItem94);

		menuItem21 = new JMenuItem("Paste", new ImageIcon(getClass().getResource("paste.gif")));
		menuItem21.setActionCommand("paste");
		menuItem21.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask));
		menuItem21.setToolTipText("Paste contents of the system clipboard");
		menuItem21.addActionListener(controler);
		menu2.add(menuItem21);

		menu2.addSeparator();

		menuItem95 = new JMenuItem("Delete");
		menuItem95.setActionCommand("delCluster");
		menuItem95.setEnabled(false);
		menuItem95.setToolTipText("Delete the content of the rubberband rectangle");
		menuItem95.addActionListener(controler);
		menu2.add(menuItem95);

		JMenuItem menuItem96 = new JMenuItem("Find...");
		menuItem96.setActionCommand("find");
		menuItem96.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcutMask));
		menuItem96.setToolTipText("Find a label");
		menuItem96.addActionListener(controler);
		menu2.add(menuItem96);

		menuItem98 = new JMenuItem("Find again");
		menuItem98.setActionCommand("findagain");
		menuItem98.setEnabled(false);
		menuItem98.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, shortcutMask));
		menuItem98.setToolTipText("Find a label again");
		menuItem98.addActionListener(controler);
		menu2.add(menuItem98);

		JMenuItem menuItem97 = new JMenuItem("Select ?");
		menuItem97.setActionCommand("select");
		menuItem97.setToolTipText("How to select");
		menuItem97.addActionListener(controler);
		menu2.add(menuItem97);

		menu2.addSeparator();

		menuItem23 = new JCheckBoxMenuItem("Lurid Colors", false);
		menuItem23.setActionCommand("TogglePalette");
		menuItem23.setToolTipText("Color scheme for new icons and lines");
		menuItem23.addActionListener(controler);
		menu2.add(menuItem23);

		menuBar.add(menu2);
	}

	public void insertMenu() {

		JMenu menu3;
		menu3 = new JMenu("Insert  ");

		JMenuItem menuItem31 = new JMenuItem("Insert Items");
		menuItem31.setActionCommand("insert");
		menuItem31.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcutMask));
		menuItem31.setToolTipText("Paste, Drop, or Type into a Composition Window");
		menuItem31.addActionListener(controler);
		menu3.add(menuItem31);

		menu3.addSeparator();

		JMenuItem menuItem37 = new JMenuItem("Launch the Import Wizard",  new ImageIcon(getClass().getResource("wizard.gif")));
		menuItem37.setActionCommand("testimp");
		menuItem37.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutMask));
		menuItem37.setToolTipText("Import data in one of 15 formats");
		menuItem37.addActionListener(controler);
		menu3.add(menuItem37);

		menuBar.add(menu3);
	}

	public void exportMenu() {

		JMenu menu7;	// TODO change number
		menu7 = new JMenu("Export  ");

		JMenuItem menuItem73 = new JMenuItem("to Wordpress WXP format");
		menuItem73.setActionCommand("wxp");
		menuItem73.setToolTipText("(Wordpress Export Format)");
		menuItem73.addActionListener(controler);
		menu7.add(menuItem73);

		JMenuItem menuItem76 = new JMenuItem("to CMap CXL file");
		menuItem76.setActionCommand("cmapexp");
		menuItem76.setToolTipText("ConceptMap by cmap.ihmc.us");
		menuItem76.addActionListener(controler);
		menu7.add(menuItem76);

		JMenuItem menuItem77 = new JMenuItem("to Brain XML file");
		menuItem77.setActionCommand("brainexp");
		menuItem77.setToolTipText("Create a TheBrain PB-XML import file");
		menuItem77.addActionListener(controler);
		menu7.add(menuItem77);

		JMenuItem menuItem78 = new JMenuItem("to VUE map file");
		menuItem78.setActionCommand("vueexp");
		menuItem78.setToolTipText("(For Visual Understanding Environment)");
		menuItem78.addActionListener(controler);
		menu7.add(menuItem78);

		JMenuItem menuItem81 = new JMenuItem("to Zettelkasten XML file");
		menuItem81.setActionCommand("zkexp");
		menuItem81.setToolTipText("Note-taking application according to Luhmann ");
		menuItem81.addActionListener(controler);
		menu7.add(menuItem81);

		JMenuItem menuItem79 = new JMenuItem("to CSV text file");
		menuItem79.setActionCommand("csvexp");
		menuItem79.setToolTipText("Just Character separated Values");
		menuItem79.addActionListener(controler);
		menu7.add(menuItem79);

		JMenuItem menuItem74 = new JMenuItem("to iMapping iMap file *)");
		menuItem74.setActionCommand("imexp");
		menuItem74.setToolTipText("(Think Tool iMapping,info)");
		menuItem74.addActionListener(controler);
		menu7.add(menuItem74);

		JMenuItem menuItem75 = new JMenuItem("to DenkWerkZeug KGIF file *)");
		menuItem75.setActionCommand("dwzexp");
		menuItem75.setToolTipText("(Think Tool DenkWerkZeug.org)");
		menuItem75.addActionListener(controler);
		menu7.add(menuItem75);

		JMenuItem menuItem83 = new JMenuItem("to Metamaps JSON file (test)");
		menuItem83.setActionCommand("metamexp");
		menuItem83.setToolTipText("(Think Tool Metamaps.cc)");
		menuItem83.addActionListener(controler);
		menu7.add(menuItem83);

		menu7.addSeparator();

		JMenuItem menuItem71 = new JMenuItem("to interactive HTML page");
		menuItem71.setActionCommand("MakeHTML");
		menuItem71.setToolTipText("An HTML snapshot for interactive Read-Only mode");
		menuItem71.addActionListener(controler);
		menu7.add(menuItem71);

		JMenuItem menuItem72 = new JMenuItem("to printable HTML page");
		menuItem72.setActionCommand("Print");
		menuItem72.setToolTipText("HTML graphics to zoom out");
		menuItem72.addActionListener(controler);
		menu7.add(menuItem72);

		menu7.addSeparator();

		JMenuItem menuItem82 = new JMenuItem("*) = Extended version only");
		menuItem82.setActionCommand("extmsg");
		menuItem82.setToolTipText("click here for the download link");
		menuItem82.addActionListener(controler);
		menu7.add(menuItem82);

		menuBar.add(menu7);
	}

	public void advancedMenu() {
		//	was Tools menu

		JMenu menu5;
		menu5 = new JMenu("Advanced  ");
		
		ButtonGroup buttonGroup = new ButtonGroup();

		menuItem41 = new JRadioButtonMenuItem("Hyperlinks on", false);
		menuItem41.setActionCommand("ToggleHyp");
		menuItem41.setToolTipText(tooltip41[1]);
		menuItem41.addActionListener(controler);
		buttonGroup.add(menuItem41);
		menu5.add(menuItem41);

		menuItem22 = new JRadioButtonMenuItem("Hyperlinks off  (Edit Mode)", true);
		menuItem22.setActionCommand("ToggleDetEdit");
		menuItem22.setToolTipText(tooltip22[0]);
		menuItem22.addActionListener(controler);
		buttonGroup.add(menuItem22);
		menu5.add(menuItem22);

		menu5.addSeparator();

		menuItem51 = new JCheckBoxMenuItem("Centrality Heatmap *)", false);
		menuItem51.setActionCommand("centcol");
		menuItem51.setToolTipText("Warmer colors represent higher betweenness centrality");
		menuItem51.addActionListener(controler);
		menu5.add(menuItem51);

		JMenuItem menuItem53 = new JMenuItem("Make Tree *)");
		menuItem53.setActionCommand("layout");
		menuItem53.setToolTipText("Generates a tree layout and structure for exporting");
		menuItem53.addActionListener(controler);
		menu5.add(menuItem53);

		menuItem55 = new JCheckBoxMenuItem("Tablet Pen Mode", false);
		menuItem55.setActionCommand("tablet");
		menuItem55.setToolTipText("Doubleclick improvement, Alt-Key for Pen and Touch");
		menuItem55.addActionListener(controler);
		menu5.add(menuItem55);

		menu5.addSeparator();

		JMenuItem menuItem54 = new JMenuItem("Preferences");
		menuItem54.setActionCommand("prefs");
		menuItem54.setToolTipText("<html><body><em>(Not yet interesting)</em></body></html>");
		menuItem54.addActionListener(controler);
		menu5.add(menuItem54);

		menuItem46 = new JCheckBoxMenuItem("Items as Index Cards", false);
		menuItem46.setActionCommand("ToggleCards");
		menuItem46.setSelected(true);
		menuItem46.setToolTipText("Rectangles or circles");
		menuItem46.addActionListener(controler);
		menu5.add(menuItem46);

		menuItem47 = new JCheckBoxMenuItem("Icon Shape Automatic", false);
		menuItem47.setActionCommand("AutoCircles");
		menuItem47.setSelected(true);
		menuItem47.setToolTipText("Show items as circles if more lines than items exist");
		menuItem47.addActionListener(controler);
		menu5.add(menuItem47);

		menuItem58 = new JCheckBoxMenuItem("Zoom", false);
		menuItem58.setActionCommand("zoom");
		menuItem58.setSelected(false);
		menuItem58.setToolTipText("Zoomable view (less-than-ideal solution)");
		menuItem58.addActionListener(controler);
		menu5.add(menuItem58);

		JCheckBoxMenuItem menuItem44 = new JCheckBoxMenuItem("Big Icons", false);
		menuItem44.setActionCommand("TogglePreso");
//		menuItem44.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, shortcutMask));
		menuItem44.setToolTipText("Presentation Mode");
		menuItem44.addActionListener(controler);
		menu5.add(menuItem44);

		menuItem42 = new JCheckBoxMenuItem("Borders", false);
		menuItem42.setActionCommand("ToggleBorders");
		menuItem42.setToolTipText("Display arrows pointing to lost areas");
		menuItem42.addActionListener(controler);
		menu5.add(menuItem42);

		menuItem45 = new JCheckBoxMenuItem("Accelerate", false);
		if (System.getProperty("os.name").equals("Linux")) {
			menuItem45.setSelected(true);
			graphPanel.toggleAntiAliasing();
		}
		menuItem45.setActionCommand("ToggleHeavy");
		menuItem45.setToolTipText("Fast but coarse graphics");
		menuItem45.addActionListener(controler);
		menu5.add(menuItem45);

		menuItem52 = new JCheckBoxMenuItem("Power User Mode", false);
		menuItem52.setActionCommand("power");
		menuItem52.setToolTipText("Acceleration, Lurid Colors, Borders, and Rubberband Selection");
		menuItem52.addActionListener(controler);
		menu5.add(menuItem52);

		menuItem43 = new JCheckBoxMenuItem("Menu Bar", true);
		menuItem43.setActionCommand("classicMenu");
		menuItem43.setToolTipText("Hide menu bar (restore via rightclick on canvas)");
		menuItem43.addActionListener(controler);
		menu5.add(menuItem43);

		menu5.addSeparator();

		menuItem26 = new JCheckBoxMenuItem("Rubberband selection enabled", false);
		menuItem26.setActionCommand("ToggleRectangle");
		menuItem26.setToolTipText("Enables to Alt-Drag for selection -- may be confusing");
		menuItem26.addActionListener(controler);
		menu5.add(menuItem26);

		menuItem27 = new JCheckBoxMenuItem("Cluster Drag&Drop enabled", false);
		menuItem27.setActionCommand("ToggleClusterCopy");
		menuItem27.setToolTipText("Enables to copy entire clusters -- may be confusing");
		menuItem27.addActionListener(controler);
		menu5.add(menuItem27);

		menuItem24 = new JCheckBoxMenuItem("Appending dropped stuff", true);
		menuItem24.setToolTipText("Exact drop position is ignored and new stuff is just appended");
		menuItem24.addActionListener(controler);
		menu5.add(menuItem24);

		menuItem25 = new JCheckBoxMenuItem("Parsing dropped HTML", false);
		menuItem25.setToolTipText("Try to find headings or lists");
		menuItem25.setActionCommand("toggleParse");
		menuItem25.addActionListener(controler);
		menu5.add(menuItem25);

		menu5.addSeparator();

		JMenuItem menuItem80 = new JMenuItem("Copy to anonymized map...");
		menuItem80.setActionCommand("Anonymize");
		menuItem80.setToolTipText("Saves a copy with all a-z replaced by x");
		menuItem80.addActionListener(controler);
		menu5.add(menuItem80);

		JMenuItem menuItem16 = new JMenuItem("Legacy Save...");
		menuItem16.setActionCommand("export");
		menuItem16.setToolTipText("Export to legacy zip file format");
		menuItem16.addActionListener(controler);
		menu5.add(menuItem16);

		menu5.addSeparator();

		JMenuItem menuItem56 = new JMenuItem("Another Map Window");
		menuItem56.setActionCommand("sibling");
		menuItem56.setToolTipText("One more map (to ALT + Drag item clusters)");
		menuItem56.addActionListener(controler);
		menu5.add(menuItem56);

		menu5.addSeparator();

		JMenuItem menuItem57 = new JMenuItem("*) = Extended version only");
		menuItem57.setActionCommand("extmsg");
		menuItem57.setToolTipText("click here for the download link");
		menuItem57.addActionListener(controler);
		menu5.add(menuItem57);

		menuBar.add(menu5);
	}

	public void helpMenu() {

		JMenu menu6;
		menu6 = new JMenu("?");
		menu6.setToolTipText("Help");

		JMenuItem menuItem61 = new JMenuItem("Help");
		menuItem61.setToolTipText("<html><em>Shows a short help page</em></html>");
		menuItem61.setActionCommand("?");
		menuItem61.addActionListener(controler);
		menu6.add(menuItem61);

		JMenuItem menuItem62 = new JMenuItem("About");
		menuItem62.setActionCommand("about");
		menuItem62.setToolTipText("Shows version number etc.");
		menuItem62.addActionListener(controler);
		menu6.add(menuItem62);

		menuBar.add(menu6);
	}
	public void displayHelp() {
		controler.displayPopup("<html>" +
	"Do you have any questions? Contact support@x28hd.de<br /><br />" +
	"<b>Icons:</b><br />" +			
	"Click on an icon to view its detail in the right pane;<br />" +
	"Drag an icon to move it;<br />" +
	"ALT-Drag an icon to connect it with another one;<br />" +
	"Right-Click an icon to change its style or delete it;<br />" +
	"<br />" +
	"<b>Lines:</b><br />" +			
	"Right-Click a line to change its style or delete it;<br />" +
	"To create a line, drag an icon while pressing the ALT key,<br /> " +
	"or while pressing the Middle Mouse-button (the wheel), <br />" +
	"or double-click before dragging.<br />" +
	"Drag a connector-line to move all connected items;<br />" +
	"<br />" +
	"<b>Background:</b><br />" +			
	"Right-Click the canvas background to <br />" +
	"-- create a new item,<br />" +
	"-- save your map,<br />" +
	"-- or to paste new input;<br />" +
	"Drag the canvas background to pan the map;<br />" +
	"<br />" +
	"<b>Details pane</b> (at the right):<br />" + 
	"-- click the \"B\"/ \"I\"/ \"U\" for bold/ italic/ underline,<br />" +
	"-- click the \"B+\" Bold Special to add the marked text to<br />" +
	"   the item's label above and on the map<br />");
	}
		
//
//	Context menu variants
	
	public void createGraphMenu(JPopupMenu menu) {
		
		JMenuItem item = new JMenuItem();
		item.addActionListener(controler);
		item.setActionCommand("Store");
		item.setText("Save map...");
		menu.add(item);

		JMenuItem item2 = new JMenuItem();
		item2.addActionListener(controler);
		item2.setActionCommand("NewNode");
		item2.setText("New item");
		menu.add(item2);

		JMenuItem item21 = new JMenuItem("Paste");
		item21.setActionCommand("paste");
		item21.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask));
		item21.addActionListener(controler);
		if (!controler.contextPasteAllowed) item21.setEnabled(false);
		menu.add(item21);

		JMenu sub1 = new JMenu("Advanced");
		
		JMenuItem item71 = new JMenuItem("Paste here");
		item71.setActionCommand("pasteHere");
		item71.addActionListener(controler);
		if (!controler.contextPasteAllowed) item71.setEnabled(false);
		sub1.add(item71);
		
		menu.add(sub1);

		if (!menuItem43.isSelected()) {
		JMenuItem item9 = new JMenuItem();
		item9.addActionListener(controler);
		item9.setActionCommand("classicMenu");
		item9.setText(showHide[controler.toggle4] + " Classic Menu");
		menu.add(item9);
		}

		JMenuItem item3 = new JMenuItem();
		item3.addActionListener(controler);
		item3.setActionCommand("?");
		item3.setText("Help");
		menu.add(item3);
	}
	
	public void createNodeMenu(JPopupMenu menu, int paletteID) {
		
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
		delTopic.addActionListener(controler);
		delTopic.setActionCommand("delTopic");
		delTopic.setText("Delete");
		menu.add(delTopic);
	}
	
	public void createEdgeMenu(JPopupMenu menu, int paletteID) {
		
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
		delAssoc.addActionListener(controler);
		delAssoc.setActionCommand("delAssoc");
		delAssoc.setText("Delete");
		menu.add(delAssoc);
		
		JMenu sub2 = new JMenu("Advanced");
		
		JMenuItem delCluster = new JMenuItem();
		delCluster.addActionListener(controler);
		delCluster.setActionCommand("delCluster");
		delCluster.setText("Delete Cluster");
		sub2.add(delCluster);
		
		JMenuItem cutCluster = new JMenuItem();
		cutCluster.addActionListener(controler);
		cutCluster.setActionCommand("cut");
		cutCluster.setText("Cut Cluster");
		sub2.add(cutCluster);
		
		JMenuItem copyCluster = new JMenuItem();
		copyCluster.addActionListener(controler);
		copyCluster.setActionCommand("copy");
		copyCluster.setText("Copy Cluster");
		sub2.add(copyCluster);
		
		JMenuItem flipHori = new JMenuItem();
		flipHori.addActionListener(controler);
		flipHori.setActionCommand("flipHori");
		flipHori.setText("Flip Cluster Horizontal");
		sub2.add(flipHori);
		
		JMenuItem flipVerti = new JMenuItem();
		flipVerti.addActionListener(controler);
		flipVerti.setActionCommand("flipVerti");
		flipVerti.setText("Flip Cluster Vertical");
		sub2.add(flipVerti);
		
		JMenuItem jump = new JMenuItem();
		jump.addActionListener(controler);
		jump.setActionCommand("jump");
		jump.setText("Jump to End");
		sub2.add(jump);

		menu.add(sub2);
	}
	
	public JMenuItem styleSwitcher(int styleNumber, String styleName, String color, boolean reverse) {
		JMenuItem styleItem = new JMenuItem();
		styleItem.addActionListener(controler);
		styleItem.setActionCommand(styleName);
		styleItem.setOpaque(true);
		styleItem.setBackground(Color.decode(color));
		if (reverse) styleItem.setForeground(Color.white);
		styleItem.setText("Change color");
		if (styleNumber == 7) styleItem.setText("Pale");
		if (styleNumber == 8) styleItem.setText("Dark");
		return styleItem;
	}

}

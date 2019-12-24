package de.x28hd.tool;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Locale;

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
	TextEditorPanel edi;	// TODO remove
	NewStuff newStuff;		// TODO remove

	JMenuBar menuBar;
	
	private Image newImage;
	private Image openImage;
	private Image saveImage;
	private Image pasteImage;
	private Image wizardImage;

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
	JCheckBoxMenuItem menuItem28 = null;
	JMenuItem menuItem91 = null;
	JMenuItem menuItem92 = null;
	JMenuItem menuItem93 = null;
	JMenuItem menuItem94 = null;
	JMenuItem menuItem95 = null;
	JMenuItem menuItem98 = null;
	JMenuItem menuItem38 = null;
	JMenuItem menuItem39 = null;
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
	JCheckBoxMenuItem menuItem63 = null;
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
		"#ffe8aa", "#ffbbbb", "#eeeeee", "#ccdddd",
		"#fbefe8", "#fcf0d0", "#fdf1b8", "#e59f63", "#65473c"}};	
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
		"#ffc877", "#ff7777", "#d8d8d8", "#b0b0b0",
		"#fbefe8", "#fcf0d0", "#fdf1b8", "#e59f63", "#65473c"}};	
//		"#fbcad0", "#fde1a8", "#d1651d", "#977649", "#65473c"}};	
	
	
	public Gui(PresentationService ps, GraphPanel gp, TextEditorPanel te, NewStuff ns) {
		controler = ps;
		graphPanel = gp;
		edi = te;
		newStuff = ns;
	}

	public JMenuBar createMenuBar() {

		shortcutMask = ActionEvent.CTRL_MASK;
		if (System.getProperty("os.name").equals("Mac OS X")) shortcutMask = ActionEvent.META_MASK;

		newImage = getImage("new.gif");
		openImage = getImage("open.gif");
		saveImage = getImage("save.gif");
		pasteImage = getImage("paste.gif");
		wizardImage = getImage("wizard.gif");
		
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

		JMenuItem menuItem10 = new JMenuItem("New", new ImageIcon(newImage));
		menuItem10.setActionCommand("new");
		menuItem10.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask));
		menuItem10.setToolTipText("Start a new map via the Composition Window");
		menuItem10.addActionListener(controler);
		menu1.add(menuItem10);

		JMenuItem menuItem11 = new JMenuItem("Open...",  new ImageIcon(openImage));
		menuItem11.setActionCommand("open");
		menuItem11.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask));
		menuItem11.setToolTipText("Open an existing map, or one or more files whose text or names are to be inserted");
		menuItem11.addActionListener(controler);
		menu1.add(menuItem11);

		JMenuItem menuItem12 = new JMenuItem("Save", new ImageIcon(saveImage));
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

		menuItem21 = new JMenuItem("Paste", new ImageIcon(pasteImage));
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

		JMenuItem menuItem32 = new JMenuItem("Import the Intro Game");
		menuItem32.setActionCommand("introgame");
		menuItem32.setToolTipText("Load a little whodunnit ");
		menuItem32.addActionListener(controler);
		menu3.add(menuItem32);

		JMenuItem menuItem33 = new JMenuItem("Import the Tutorial");
		menuItem33.setActionCommand("loadhelp");
		menuItem33.setToolTipText("Insert items that contain some help info ");
		menuItem33.addActionListener(controler);
		menu3.add(menuItem33);

		menu3.addSeparator();

		JMenuItem menuItem31 = new JMenuItem("Insert Items");
		menuItem31.setActionCommand("insert");
		menuItem31.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcutMask));
		menuItem31.setToolTipText("Paste, Drop, or Type into a Composition Window");
		menuItem31.addActionListener(controler);
		menu3.add(menuItem31);

		JMenuItem menuItem37 = new JMenuItem("Launch the Import Wizard",  new ImageIcon(wizardImage));
		menuItem37.setActionCommand("testimp");		// TODO rename
		menuItem37.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutMask));
		menuItem37.setToolTipText("Import data in one of 15 formats");
		menuItem37.addActionListener(controler);
		menu3.add(menuItem37);

		menuBar.add(menu3);
	}

	public void exportMenu() {

		JMenu menu7;	// TODO change number
		menu7 = new JMenu("Export  ");

		JMenuItem menuItem73 = new JMenuItem("to Wordpress WXR format");
		menuItem73.setActionCommand("wxr");
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

		JMenuItem menuItem79 = new JMenuItem("items to CSV text file");
		menuItem79.setActionCommand("csvexp");
		menuItem79.setToolTipText("Just Character separated Values");
		menuItem79.addActionListener(controler);
		menu7.add(menuItem79);

		JMenuItem menuItem84 = new JMenuItem("lines to CSV text file");
		menuItem84.setActionCommand("edgeexp");
		menuItem84.setToolTipText("Labels, IDs and relationships to CSV");
		menuItem84.addActionListener(controler);
		menu7.add(menuItem84);

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
		
		JMenu sub3 = new JMenu("Zoom the text");

		JMenuItem zoomin = new JMenuItem("Larger text");
		zoomin.setActionCommand("zoomin");
		zoomin.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, shortcutMask));
		zoomin.addActionListener(controler);
		sub3.add(zoomin);

		JMenuItem zoomreset = new JMenuItem("Reset text size");
		zoomreset.setActionCommand("zoomreset");
		zoomreset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, shortcutMask));
		zoomreset.addActionListener(controler);
		sub3.add(zoomreset);

		JMenuItem zoomout = new JMenuItem("Smaller text");
		zoomout.setActionCommand("zoomout");
		zoomout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, shortcutMask));
		zoomout.addActionListener(controler);
		sub3.add(zoomout);
		
		menu5.add(sub3);

		menuItem58 = new JCheckBoxMenuItem("Zoom the map...", false);
		menuItem58.setActionCommand("zoom");
		menuItem58.setSelected(false);
		menuItem58.setToolTipText("Zoomable view (less-than-ideal solution)");
		menuItem58.addActionListener(controler);
		menu5.add(menuItem58);

		menu5.addSeparator();

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

		menuItem63 = new JCheckBoxMenuItem("HyperHopping enabled", false);
		menuItem63.setActionCommand("hashes");
		menuItem63.setToolTipText("Local hyperlinks (#...) used for Find in labels");
		menuItem63.addActionListener(controler);
		menu5.add(menuItem63);

		menu5.addSeparator();

		JMenu sub2 = new JMenu("Layouts");
		
		JMenuItem menuItem53 = new JMenuItem("Make Tree");
		menuItem53.setActionCommand("layout");
		menuItem53.setToolTipText("Generates a tree layout and structure for exporting");
		menuItem53.addActionListener(controler);
		sub2.add(menuItem53);

		JMenuItem menuItem60 = new JMenuItem("Make Circle");
		menuItem60.setActionCommand("makecircle");
		menuItem60.setToolTipText("Separates trees and chains from core circle");
		menuItem60.addActionListener(controler);
		sub2.add(menuItem60);

		JMenuItem menuItem59 = new JMenuItem("DAG Layout");
		menuItem59.setActionCommand("dag");
		menuItem59.setToolTipText("Directed Acyclic Graph, depth last");
		menuItem59.addActionListener(controler);
		sub2.add(menuItem59);

		menuItem51 = new JCheckBoxMenuItem("Centrality Heatmap", false);
		menuItem51.setActionCommand("centcol");
		menuItem51.setToolTipText("Warmer colors represent higher betweenness centrality");
		menuItem51.addActionListener(controler);
		sub2.add(menuItem51);

		menuItem38 = new JMenuItem("Flip rectangle horizontal");
		menuItem38.setActionCommand("flipHori");
		menuItem38.setEnabled(false);
		menuItem38.setToolTipText("Flip the selected clipping horizontally");
		menuItem38.addActionListener(controler);
		sub2.add(menuItem38);

		menuItem39 = new JMenuItem("Flip rectangle vertical");
		menuItem39.setActionCommand("flipVerti");
		menuItem39.setEnabled(false);
		menuItem39.setToolTipText("Flip the selected clipping vertically");
		menuItem39.addActionListener(controler);
		sub2.add(menuItem39);

		menu5.add(sub2);
		
		JMenu sub4 = new JMenu("Dropping");

		menuItem27 = new JCheckBoxMenuItem("Cluster Drag&Drop enabled", false);
		menuItem27.setActionCommand("ToggleClusterCopy");
		menuItem27.setToolTipText("Enables to copy entire clusters -- may be confusing");
		menuItem27.addActionListener(controler);
		sub4.add(menuItem27);

		menuItem24 = new JCheckBoxMenuItem("Appending dropped stuff", true);
		menuItem24.setToolTipText("Exact drop position is ignored and new stuff is just appended");
		menuItem24.addActionListener(controler);
		sub4.add(menuItem24);

		menuItem25 = new JCheckBoxMenuItem("Parsing dropped HTML", false);
		menuItem25.setToolTipText("Try to find headings or lists");
		menuItem25.setActionCommand("toggleParse");
		menuItem25.addActionListener(controler);
		sub4.add(menuItem25);

		menuItem28 = new JCheckBoxMenuItem("Dropped files are UTF-8", true);
		menuItem28.setToolTipText("Disable for old Windows files");
		menuItem28.setActionCommand("toggleEncoding");
		if (!System.getProperty("os.name").startsWith("Windows")) menuItem28.setEnabled(false);
		menuItem28.addActionListener(controler);
		sub4.add(menuItem28);

		menu5.add(sub4);

		JMenu sub5 = new JMenu("Appearance");

		menuItem46 = new JCheckBoxMenuItem("Items as Index Cards", false);
		menuItem46.setActionCommand("ToggleCards");
		menuItem46.setSelected(true);
		menuItem46.setToolTipText("Rectangles or circles");
		menuItem46.addActionListener(controler);
		sub5.add(menuItem46);

		menuItem47 = new JCheckBoxMenuItem("Icon Shape Automatic", false);
		menuItem47.setActionCommand("AutoCircles");
		menuItem47.setSelected(true);
		menuItem47.setToolTipText("Show items as circles if more lines than items exist");
		menuItem47.addActionListener(controler);
		sub5.add(menuItem47);
		
		JCheckBoxMenuItem menuItem44 = new JCheckBoxMenuItem("Big Icons", false);
		menuItem44.setActionCommand("TogglePreso");
//		menuItem44.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, shortcutMask));
		menuItem44.setToolTipText("Presentation Mode");
		menuItem44.addActionListener(controler);
		sub5.add(menuItem44);

		menuItem42 = new JCheckBoxMenuItem("Borders", false);
		menuItem42.setActionCommand("ToggleBorders");
		menuItem42.setToolTipText("Display arrows pointing to lost areas");
		menuItem42.addActionListener(controler);
		sub5.add(menuItem42);

		menuItem45 = new JCheckBoxMenuItem("Accelerate", false);
		if (System.getProperty("os.name").equals("Linux")) {
			menuItem45.setSelected(true);
			graphPanel.toggleAntiAliasing();
		}
		menuItem45.setActionCommand("ToggleHeavy");
		menuItem45.setToolTipText("Fast but coarse graphics");
		menuItem45.addActionListener(controler);
		sub5.add(menuItem45);

		menuItem43 = new JCheckBoxMenuItem("Menu Bar", true);
		menuItem43.setActionCommand("classicMenu");
		menuItem43.setToolTipText("Hide menu bar (restore via rightclick on canvas)");
		menuItem43.addActionListener(controler);
		sub5.add(menuItem43);
		
		menu5.add(sub5);

		JMenu sub6 = new JMenu("Miscellaneous");

		menuItem55 = new JCheckBoxMenuItem("Tablet Pen Mode", false);
		menuItem55.setActionCommand("tablet");
		menuItem55.setToolTipText("Doubleclick improvement, Alt-Key for Pen and Touch");
		menuItem55.addActionListener(controler);
		sub6.add(menuItem55);

		JMenuItem menuItem54 = new JMenuItem("Preferences");
		menuItem54.setActionCommand("prefs");
		menuItem54.setToolTipText("<html><body><em>(Not yet interesting)</em></body></html>");
		menuItem54.addActionListener(controler);
		sub6.add(menuItem54);

		JMenuItem menuItem80 = new JMenuItem("Copy to anonymized map...");
		menuItem80.setActionCommand("Anonymize");
		menuItem80.setToolTipText("Saves a copy with all a-z replaced by x");
		menuItem80.addActionListener(controler);
		sub6.add(menuItem80);

		JMenuItem menuItem16 = new JMenuItem("Legacy Save...");
		menuItem16.setActionCommand("export");
		menuItem16.setToolTipText("Export to legacy zip file format");
		menuItem16.addActionListener(controler);
		sub6.add(menuItem16);

		menuItem52 = new JCheckBoxMenuItem("Power User Mode", false);
		menuItem52.setActionCommand("power");
		menuItem52.setToolTipText("Acceleration, Lurid Colors, Borders, and Rubberband Selection");
		menuItem52.addActionListener(controler);
		sub6.add(menuItem52);

		menuItem26 = new JCheckBoxMenuItem("Rubberband selection enabled", true);
		menuItem26.setActionCommand("ToggleRectangle");
		menuItem26.setToolTipText("Enables to Alt-Drag for selection -- may be confusing");
		menuItem26.addActionListener(controler);
		sub6.add(menuItem26);

		JMenuItem menuItem56 = new JMenuItem("Another Map Window");
		menuItem56.setActionCommand("sibling");
		menuItem56.setToolTipText("One more map (to ALT + Drag item clusters)");
		menuItem56.addActionListener(controler);
		sub6.add(menuItem56);

		JMenuItem menuItem57 = new JMenuItem("WXR to SQL");
		menuItem57.setActionCommand("wxr");
		menuItem57.setToolTipText("experimental");
		menuItem57.addActionListener(controler);
		sub6.add(menuItem57);
		
		menu5.add(sub6);

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
		
		JMenu sub4 = new JMenu("Human");
		sub4.add(styleSwitcher(9, "faceColor1", menuPalette[1][8], false));
		sub4.add(styleSwitcher(10, "faceColor2", menuPalette[1][9], false));
		sub4.add(styleSwitcher(11, "faceColor3", menuPalette[1][10], false));
		sub4.add(styleSwitcher(12, "faceColor4", menuPalette[1][11], true));
		sub4.add(styleSwitcher(13, "faceColor5", menuPalette[1][12], true));
		menu.add(sub4);

		menu.addSeparator();

		JMenuItem subtree = new JMenuItem();
		subtree.addActionListener(controler);
		subtree.setActionCommand("subtree");
		subtree.setText("Tree layout");
		menu.add(subtree);
		
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
		if (controler.rectangle) sub2.setEnabled(false);
		
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

	// TODO consolidate with GraphPanel
	public Image getImage(String imagefile) {
		URL imgURL = getClass().getResource(imagefile);
		ImageIcon ii;
		Image img = null;
		if (imgURL == null) {
			imgURL = getClass().getClassLoader().getResource(imagefile);
		}
		if (imgURL == null) {
			controler.displayPopup("Image " + imagefile + " not loaded");
		} else {
			ii = new ImageIcon(imgURL);
			img = ii.getImage();
		}
		return img;
	}

	public String getInitText(boolean empty) {
		if (empty) {
			return initText1;
		} else {
			return initText2;
		}
	}

	public static final String initText1 = "<body><font color=\"gray\">"
			+ "<em>To get started, insert some items. Then: </em><br />&nbsp;<br />"
			+ "<em>Click an icon for its details, ALT+drag an icon to connect it."
			+ "</font></body>";

	public static final String initText2 = "<body><font color=\"gray\">"
			+ "<em>Click an icon for its details, ALT+drag an icon to connect it."
			+ "<br />&nbsp;<br />Do you have any questions? Contact "
			+ "<a href=\"mailto:support@x28hd.de\">support@x28hd.de</a></em>"
			+ "</font></body>";

	public String getSample(boolean help) {
		if (help) return HELP_EN;
		String lang = Locale.getDefault().getLanguage();
		if (lang == "de") {
			return INTRO_DE;
		} else{
			return INTRO_EN;
		}
	}
	
	public static final String HELP_EN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<!-- This is not for human readers but for http://x28hd.de/tool/ --><x28map>"
			+ "<topic x=\"40\" ID=\"1\" color=\"#ffbbbb\" y=\"40\">"
			+ "<label><![CDATA[Click this and look right]]></label>"
			+ "<detail><![CDATA[Click an item on the left pane to view its details on the "
			+ "right pane.<br /><br />It is a bit like turning cards face up in the game "
			+ "of Pairs (aka Memory or Concentration) -- just that it won't cost you scores."
			+ " 'Turn' as often as you need.]]></detail>"
			+ "</topic>"
			
			+ "<topic x=\"40\" ID=\"2\" color=\"#d2bbd2\" y=\"90\">"
			+ "<label><![CDATA[Move]]></label>"
			+ "<detail><![CDATA[To move an icon, drag it, i.e., press and hold the left "
			+ "mouse-button, move the mouse-pointer, and release the mouse-button.<br />"
			+ "<br />Do that to move similar items close to each other.]]></detail>"
			+ "</topic>"
			
			+ "<topic x=\"40\" ID=\"3\" color=\"#d2bbd2\" y=\"140\">"
			+ "<label><![CDATA[Connect]]></label>"
			+ "<detail><![CDATA[To connect one icon to a second icon, you will ALT + drag it, "
			+ "i.e. <br />- point at the first icon, <br />- press and hold the ALT key, "
			+ "<br />- then drag the mouse until you reach the second icon,<br />- then "
			+ "release both the mouse-button and the ALT key.<br /><br />Exercise: Connect "
			+ "the 'ALT + drag' icon to some related icon.]]></detail>"
			+ "</topic>"
			
			+ "<topic x=\"40\" ID=\"4\" color=\"#d2bbd2\" y=\"190\">"
			+ "<label><![CDATA[Pan]]></label>"
			+ "<detail><![CDATA[To pan the canvas, drag its background.<br /><br />Try it! "
			+ "Does it work?]]></detail>"
			+ "</topic>"
			
			+ "<topic x=\"40\" ID=\"5\" color=\"#bbbbff\" y=\"240\">"
			+ "<label><![CDATA[Drop input]]></label>"
			+ "<detail><![CDATA[The easiest way to get your input into the map is <br />- "
			+ "to select some text in another window<br />- and just drag and drop it onto "
			+ "the canvas.<br /><br />Just try it (except on Safari & IE). Don't be confused "
			+ "by the unexpected shapes of the mouse pointer -- once the mouse is over the "
			+ "canvas, it will change.<br /><br />Exercise: Select the two items below and "
			+ "drag them to the canvas: "
			+ "<br /><pre>Item 1\tdemo\nItem 2\tdemo</pre>"
			+ "<br />Now try text from a different window.]]></detail>"
			+ "</topic>"
			
			+ "<topic x=\"40\" ID=\"6\" color=\"#bbbbff\" y=\"290\">"
			+ "<label><![CDATA[Drop a file]]></label>"
			+ "<detail><![CDATA[You may drop a file onto the canvas. Plenty of import formats "
			+ "are supported (see Insert > Import wizard) -- including a simple text file, "
			+ "in which case each line becomes an item. You may separate the 'detail' part from "
			+ "the 'label' by a TAB character.]]></detail>"
			+ "</topic>"
			
			+ "<topic x=\"40\" ID=\"7\" color=\"#bbbbff\" y=\"340\">"
			+ "<label><![CDATA[Add single items]]></label>"
			+ "<detail><![CDATA[Right-click the canvas and select 'New item', "
			+ "then fill in the 'Label' and/ or 'Details' fields."
			+ "<br /><br />Single icons are "
			+ "useful if you want to create 'towns' amidst the 'villages' on your thought "
			+ "map. But unlike categories, they don't even need a name!]]></detail>"
			+ "</topic>"
			
			+ "<topic x=\"40\" ID=\"9\" color=\"#bbffbb\" y=\"390\">"
			+ "<label><![CDATA[Export]]></label>"
			+ "<detail><![CDATA[Use the 'Export' menu to take your map to various other apps..]]></detail>"
			+ "</topic>"
			
			+ "<topic x=\"40\" ID=\"10\" color=\"#ffff99\" y=\"440\">"
			+ "<label><![CDATA[Re-color]]></label>"
			+ "<detail><![CDATA[Right-click an icon, and select a new color.]]></detail>"
			+ "</topic>"
			+ "<topic x=\"190\" ID=\"11\" color=\"#d2bbd2\" y=\"90\">"
			+ "<label><![CDATA[Drag]]></label>"
			+ "<detail><![CDATA[Drag an icon to move it. Drag the canvas background to "
			+ "pan.]]></detail>"
			+ "</topic>"
			
			+ "<topic x=\"190\" ID=\"12\" color=\"#d2bbd2\" y=\"140\">"
			+ "<label><![CDATA[ALT + drag]]></label>"
			+ "<detail><![CDATA[ALT + drag an icon to connect it.]]></detail>"
			+ "</topic></x28map>";
	
	public static final String INTRO_EN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<!-- This is not for human readers but for http://x28hd.de/tool/ --><x28map>"
			
			+ "<topic ID=\"1\" x=\"40\" y=\"40\" color=\"#ccdddd\">"
			+ "<label><![CDATA[Lady's Uncle]]></label>"
			+ "<detail><![CDATA[lies dead in the fishpond. Who does not have an alibi?]]></detail>"
			+ "</topic>"
			
			+ "<topic ID=\"2\" x=\"40\" y=\"90\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Butler]]></label>"
			+ "<detail><![CDATA[was in the fireside lounge]]></detail></topic>"
			
			+ "<topic ID=\"3\" x=\"40\" y=\"140\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Gardener]]></label>"
			+ "<detail><![CDATA[was in the horse stable]]></detail></topic>"
			
			+ "<topic ID=\"4\" x=\"40\" y=\"190\" color=\"#ffbbbb\">"
			+ "<label><![CDATA[Cook]]></label>"
			+ "<detail><![CDATA[was in the fireside lounge]]></detail>"
			
			+ "</topic><topic ID=\"5\" x=\"40\" y=\"240\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Nephew]]></label>"
			+ "<detail><![CDATA[was in the tower chamber]]></detail></topic>"
			
			+ "<topic ID=\"6\" x=\"40\" y=\"290\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Coachman]]></label>"
			+ "<detail><![CDATA[was in the smoking room]]></detail></topic>"
			
			+ "<topic ID=\"7\" x=\"40\" y=\"340\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Pianist]]></label>"
			+ "<detail><![CDATA[was in the music room]]></detail></topic>"
			
			+ "<topic ID=\"8\" x=\"40\" y=\"390\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Pastor]]></label>"
			+ "<detail><![CDATA[was in the wine cellar]]></detail></topic>"
			
			+ "<topic ID=\"9\" x=\"40\" y=\"440\" color=\"#ffbbbb\">"
			+ "<label><![CDATA[Chambermaid]]></label>"
			+ "<detail><![CDATA[was in the tower chamber]]></detail></topic>"
			
			+ "<topic ID=\"10\" x=\"40\" y=\"490\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Stable-lad]]></label>"
			+ "<detail><![CDATA[was in the garden shed]]></detail></topic>"
			
			+ "<topic ID=\"11\" x=\"190\" y=\"40\" color=\"#ffbbbb\">"
			+ "<label><![CDATA[Flower girl]]></label>"
			+ "<detail><![CDATA[was in der Library]]></detail></topic>"
			
			+ "<topic ID=\"12\" x=\"190\" y=\"90\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Tutor]]></label>"
			+ "<detail><![CDATA[was in the Blue Parlour]]></detail></topic>"
			
			+ "<topic ID=\"13\" x=\"190\" y=\"140\" color=\"#bbbbff\"><label>"
			+ "<![CDATA[Neighbour boy]]></label>"
			+ "<detail><![CDATA[was in the garden shed]]></detail></topic>"
			
			+ "<topic ID=\"14\" x=\"190\" y=\"190\" color=\"#ffbbbb\">"
			+ "<label><![CDATA[Governess]]></label>"
			+ "<detail><![CDATA[was in the wine cellar]]></detail></topic>"
			
			+ "<topic ID=\"15\" x=\"190\" y=\"240\" color=\"#ffbbbb\">"
			+ "<label><![CDATA[Aunt from America]]></label>"
			+ "<detail><![CDATA[was in the horse stable]]></detail></topic>"
			
			+ "<topic ID=\"16\" x=\"190\" y=\"290\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Riding instructor]]></label>"
			+ "<detail><![CDATA[was in the smoking room]]></detail></topic>"
			
			+ "<topic ID=\"17\" x=\"190\" y=\"340\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Brother in law]]></label>"
			+ "<detail><![CDATA[was in the Onyx bathroom]]></detail></topic>"
			
			+ "<topic ID=\"18\" x=\"190\" y=\"390\" color=\"#bbbbff\">"
			+ "<label><![CDATA[Lord]]></label>"
			+ "<detail><![CDATA[was in the music room]]></detail></topic>"
			
			+ "<topic ID=\"19\" x=\"190\" y=\"440\" color=\"#ffbbbb\">"
			+ "<label><![CDATA[Lady]]></label>"
			+ "<detail><![CDATA[was in der Library]]></detail></topic>"
			
			+ "<topic ID=\"20\" x=\"190\" y=\"490\" color=\"#ffbbbb\">"
			+ "<label><![CDATA[Lady's Sister]]></label>"
			+ "<detail><![CDATA[was in the Blue Parlour]]></detail></topic>"
			+ "</x28map>";
	
	public static final String INTRO_DE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<!-- This is not for human readers but for http://x28hd.de/tool/ --><x28map>"
			+ "<topic x=\"40\" ID=\"1\" color=\"#808080\" y=\"40\">"
			+ "<label><![CDATA[Lady's Onkel]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> liegt tot im Fischteich. Wer hat kein Alibi? </body></html>]]></detail></topic>"
			+ "<topic x=\"42\" ID=\"2\" color=\"#bbbbff\" y=\"88\">"
			+ "<label><![CDATA[Butler]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Kaminzimmer </body></html>]]></detail></topic>"
			
			+ "<topic x=\"42\" ID=\"3\" color=\"#bbbbff\" y=\"138\">"
			+ "<label><![CDATA[Gärtner]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Pferdestall </body></html>]]></detail></topic>"
			
			+ "<topic x=\"42\" ID=\"4\" color=\"#ffbbbb\" y=\"188\">"
			+ "<label><![CDATA[Köchin]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Kaminzimmer </body></html>]]></detail></topic>"
			
			+ "<topic x=\"42\" ID=\"5\" color=\"#ffbbbb\" y=\"238\">"
			+ "<label><![CDATA[Zimmermädchen]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Turmzimmer </body></html>]]></detail></topic>"
			
			+ "<topic x=\"42\" ID=\"6\" color=\"#bbbbff\" y=\"288\">"
			+ "<label><![CDATA[Chauffeur]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Raucherzimmer </body></html>]]></detail></topic>"
			
			+ "<topic x=\"42\" ID=\"7\" color=\"#bbbbff\" y=\"338\">"
			+ "<label><![CDATA[Klavierspieler]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Musikzimmer </body></html>]]></detail></topic>"
			
			+ "<topic x=\"42\" ID=\"8\" color=\"#bbbbff\" y=\"388\">"
			+ "<label><![CDATA[Pastor]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Weinkeller </body></html>]]></detail></topic>"
			
			+ "<topic x=\"42\" ID=\"9\" color=\"#bbbbff\" y=\"438\">"
			+ "<label><![CDATA[Neffe]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Turmzimmer </body></html>]]></detail></topic>"
			
			+ "<topic x=\"42\" ID=\"10\" color=\"#bbbbff\" y=\"488\">"
			+ "<label><![CDATA[Stallbursche]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Gartenh&#228;uschen </body></html>]]></detail></topic>"
			
			+ "<topic x=\"189\" ID=\"11\" color=\"#ffbbbb\" y=\"41\">"
			+ "<label><![CDATA[Gouvernante]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war in der Bibliothek </body></html>]]></detail></topic>"
			
			+ "<topic x=\"192\" ID=\"12\" color=\"#bbbbff\" y=\"93\">"
			+ "<label><![CDATA[Hauslehrer]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Blauen Salon </body></html>]]></detail></topic>"
			
			+ "<topic x=\"192\" ID=\"13\" color=\"#bbbbff\" y=\"143\">"
			+ "<label><![CDATA[Nachbarsjunge]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Gartenh&#228;uschen </body></html>]]></detail></topic>"
			
			+ "<topic x=\"192\" ID=\"14\" color=\"#ffbbbb\" y=\"193\">"
			+ "<label><![CDATA[Blumenfrau]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Weinkeller </body></html>]]></detail></topic>"
			
			+ "<topic x=\"192\" ID=\"15\" color=\"#ffbbbb\" y=\"243\">"
			+ "<label><![CDATA[Tante aus Amerika]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Pferdestall </body></html>]]></detail></topic>"
			
			+ "<topic x=\"192\" ID=\"16\" color=\"#bbbbff\" y=\"293\">"
			+ "<label><![CDATA[Reitlehrer]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Raucherzimmer </body></html>]]></detail></topic>"
			
			+ "<topic x=\"192\" ID=\"17\" color=\"#bbbbff\" y=\"343\">"
			+ "<label><![CDATA[Schwager]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Onyx-Bad </body></html>]]></detail></topic>"
			
			+ "<topic x=\"192\" ID=\"18\" color=\"#0000ff\" y=\"393\">"
			+ "<label><![CDATA[Lord]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Musikzimmer </body></html>]]></detail></topic>"
			
			+ "<topic x=\"192\" ID=\"19\" color=\"#ff0000\" y=\"443\">"
			+ "<label><![CDATA[Lady]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war in der Bibliothek </body></html>]]></detail></topic>"
			
			+ "<topic x=\"192\" ID=\"20\" color=\"#ffbbbb\" y=\"493\">"
			+ "<label><![CDATA[Lady's Schwester]]></label>"
			+ "<detail><![CDATA[<html> <head> </head> <body> war im Blauen Salon </body></html>]]></detail></topic>"
			+ "</x28map>";


}

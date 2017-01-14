package de.x28hd.tool;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
	JCheckBoxMenuItem menuItem22 = null;
	JCheckBoxMenuItem menuItem23 = null;
	JCheckBoxMenuItem menuItem24 = null;
	JMenuItem menuItem91 = null;
	JMenuItem menuItem92 = null;
	JMenuItem menuItem93 = null;
	JMenuItem menuItem94 = null;
	JMenuItem menuItem95 = null;
	JMenuItem menuItem98 = null;
	JCheckBoxMenuItem menuItem41 = null;
	JCheckBoxMenuItem menuItem42 = null;
	JCheckBoxMenuItem menuItem43 = null;
	JCheckBoxMenuItem menuItem45 = null;
	JCheckBoxMenuItem menuItem51 = null;
	JCheckBoxMenuItem menuItem52 = null;
	JCheckBoxMenuItem menuItem55 = null;
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
		viewMenu();
		advancedMenu();
		helpMenu();
		
		return menuBar;
	}
	
	public void fileMenu() {

		JMenu menu1;
		menu1 = new JMenu("File  ");
		menu1.setMnemonic(KeyEvent.VK_F);

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

		JMenuItem menuItem14 = new JMenuItem("Print", KeyEvent.VK_P);
		menuItem14.setActionCommand("Print");
		menuItem14.setToolTipText("HTML graphics to zoom out");
		menuItem14.addActionListener(controler);
		menu1.add(menuItem14);

		JMenuItem menuItem15 = new JMenuItem("Snapshot", KeyEvent.VK_H);
		menuItem15.setActionCommand("MakeHTML");
		menuItem15.setToolTipText("An HTML snapshot for interactive Read-Only mode");
		menuItem15.addActionListener(controler);
		menu1.add(menuItem15);


		JMenuItem menuItem16 = new JMenuItem("Legacy Save...");
		menuItem16.setActionCommand("export");
		menuItem16.setToolTipText("Export to lagacy zip file format");
		menuItem16.addActionListener(controler);
		menu1.add(menuItem16);

		menu1.addSeparator();

		JMenuItem menuItem17 = new JMenuItem("Quit", KeyEvent.VK_Q);
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
		menu2.setMnemonic(KeyEvent.VK_E);

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
		menuItem93.setToolTipText("Cut the cluster containing the selected edge");
		menuItem93.addActionListener(controler);
		menu2.add(menuItem93);

		menuItem94 = new JMenuItem("Copy");
		menuItem94.setActionCommand("copy");
		menuItem94.setEnabled(false);
		menuItem94.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask));
		menuItem94.setToolTipText("Copy the cluster containing the selected edge");
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
		menuItem95.setActionCommand("delete");
		menuItem95.setEnabled(false);
		menuItem95.setToolTipText("Delete the selected item");
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

		menuItem22 = new JCheckBoxMenuItem("Detail Pane", true);
		menuItem22.setMnemonic(KeyEvent.VK_D);
		menuItem22.setActionCommand("ToggleDetEdit");
		menuItem22.setToolTipText(tooltip22[0]);
		menuItem22.addActionListener(controler);
		menu2.add(menuItem22);

		menuItem23 = new JCheckBoxMenuItem("Lurid Colors", false);
		menuItem23.setMnemonic(KeyEvent.VK_L);
		menuItem23.setActionCommand("TogglePalette");
		menuItem23.setToolTipText("Color scheme for new nodes and edges");
		menuItem23.addActionListener(controler);
		menu2.add(menuItem23);

		menuItem24 = new JCheckBoxMenuItem("Appending", true);
		menuItem24.setMnemonic(KeyEvent.VK_A);
		menuItem24.setToolTipText("Exact drop position is ignored and new stuff is just appended");
		menuItem24.addActionListener(controler);
		menu2.add(menuItem24);

		menuBar.add(menu2);
	}

	public void insertMenu() {

		JMenu menu3;
		menu3 = new JMenu("Insert  ");
		menu3.setMnemonic(KeyEvent.VK_I);

		JMenuItem menuItem31 = new JMenuItem("Insert Items",  KeyEvent.VK_I);
		menuItem31.setActionCommand("insert");
		menuItem31.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcutMask));
		menuItem31.setToolTipText("Paste, Drop, or Type into a Composition Window");
		menuItem31.addActionListener(controler);
		menu3.add(menuItem31);

		menu3.addSeparator();

		JMenuItem menuItem37 = new JMenuItem("Launch the Import Wizard",  new ImageIcon(getClass().getResource("wizard.gif")));
		menuItem37.setActionCommand("testimp");
		menuItem37.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutMask));
		menuItem37.setToolTipText("");
		menuItem37.addActionListener(controler);
		menu3.add(menuItem37);

		menuBar.add(menu3);
	}

	public void exportMenu() {

		JMenu menu7;	// TODO change number
		menu7 = new JMenu("Export  ");
		menu7.setMnemonic(KeyEvent.VK_X);

		JMenuItem menuItem71 = new JMenuItem("to interactive HTML page", KeyEvent.VK_H);
		menuItem71.setActionCommand("MakeHTML");
		menuItem71.setToolTipText("An HTML snapshot for interactive Read-Only mode");
		menuItem71.addActionListener(controler);
		menu7.add(menuItem71);

		JMenuItem menuItem72 = new JMenuItem("to printable HTML page", KeyEvent.VK_P);
		menuItem72.setActionCommand("Print");
		menuItem72.setToolTipText("HTML graphics to zoom out");
		menuItem72.addActionListener(controler);
		menu7.add(menuItem72);

		JMenuItem menuItem80 = new JMenuItem("to anonymized map", KeyEvent.VK_Y);
		menuItem80.setActionCommand("Anonymize");
		menuItem80.setToolTipText("Saves a copy with all a-z replaced by x");
		menuItem80.addActionListener(controler);
		menu7.add(menuItem80);

		menu7.addSeparator();

		JMenuItem menuItem73 = new JMenuItem("to Wordpress WXP format",  KeyEvent.VK_W);
		menuItem73.setActionCommand("wxp");
		menuItem73.setToolTipText("<html><body><em>(Wordpress Export Format)</em></body></html>");
		menuItem73.addActionListener(controler);
		menu7.add(menuItem73);

		JMenuItem menuItem74 = new JMenuItem("to iMapping iMap file",  KeyEvent.VK_I);
		menuItem74.setActionCommand("imexp");
		menuItem74.setToolTipText("<html><body><em>(Think Tool iMapping,info)</em></body></html>");
		menuItem74.addActionListener(controler);
		menu7.add(menuItem74);

		JMenuItem menuItem75 = new JMenuItem("to DenkWerkZeug KGIF file",  KeyEvent.VK_D);
		menuItem75.setActionCommand("dwzexp");
		menuItem75.setToolTipText("<html><body><em>(Think Tool DenkWerkZeug.org)</em></body></html>");
		menuItem75.addActionListener(controler);
		menu7.add(menuItem75);

		JMenuItem menuItem76 = new JMenuItem("to CMap CXL file",  KeyEvent.VK_M);
		menuItem76.setActionCommand("cmapexp");
		menuItem76.setToolTipText("ConceptMap by cmap.ihmc.us");
		menuItem76.addActionListener(controler);
		menu7.add(menuItem76);

		JMenuItem menuItem77 = new JMenuItem("to Brain XML file",  KeyEvent.VK_M);
		menuItem77.setActionCommand("brainexp");
		menuItem77.setToolTipText("Create a TheBrain PB-XML import file");
		menuItem77.addActionListener(controler);
		menu7.add(menuItem77);

		JMenuItem menuItem78 = new JMenuItem("to VUE map file",  KeyEvent.VK_U);
		menuItem78.setActionCommand("vueexp");
		menuItem78.setToolTipText("Create a VUE map file");
		menuItem78.addActionListener(controler);
		menu7.add(menuItem78);

		JMenuItem menuItem81 = new JMenuItem("to Zettelkasten XML file", KeyEvent.VK_K);
		menuItem81.setActionCommand("zkexp");
		menuItem81.setToolTipText("Note-taking application according to Luhmann ");
		menuItem81.addActionListener(controler);
		menu7.add(menuItem81);

		JMenuItem menuItem79 = new JMenuItem("to CSV text file",  KeyEvent.VK_S);
		menuItem79.setActionCommand("csvexp");
		menuItem79.setToolTipText("Just Character separated Values");
		menuItem79.addActionListener(controler);
		menu7.add(menuItem79);

		menuBar.add(menu7);
	}

	public void viewMenu() {

		JMenu menu4;
		menu4 = new JMenu("View  ");
		menu4.setMnemonic(KeyEvent.VK_V);

		menuItem41 = new JCheckBoxMenuItem("Hyperlinks", false);
		menuItem41.setActionCommand("ToggleHyp");
		menuItem41.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, shortcutMask));
		menuItem41.setToolTipText(tooltip41[1]);
		menuItem41.addActionListener(controler);
		menu4.add(menuItem41);

		menuItem45 = new JCheckBoxMenuItem("Accelerate", false);
		if (System.getProperty("os.name").equals("Linux")) {
			menuItem45.setSelected(true);
			graphPanel.toggleAntiAliasing();
		}
		menuItem45.setActionCommand("ToggleHeavy");
		menuItem45.setToolTipText("Fast but coarse graphics");
		menuItem45.addActionListener(controler);
		menu4.add(menuItem45);

		menuItem42 = new JCheckBoxMenuItem("Borders", false);
		menuItem42.setActionCommand("ToggleBorders");
		menuItem42.setToolTipText("Display arrows pointing to lost areas");
		menuItem42.addActionListener(controler);
		menu4.add(menuItem42);

		menuItem43 = new JCheckBoxMenuItem("Menu Bar", true);
		menuItem43.setActionCommand("classicMenu");
		menuItem43.setToolTipText("Hide menu bar (restore via rightclick on canvas)");
		menuItem43.addActionListener(controler);
		menu4.add(menuItem43);

		JCheckBoxMenuItem menuItem44 = new JCheckBoxMenuItem("Big Icons", false);
		menuItem44.setActionCommand("TogglePreso");
		menuItem44.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, shortcutMask));
		menuItem44.setToolTipText("Presentation Mode");
		menuItem44.addActionListener(controler);
		menu4.add(menuItem44);

		menuBar.add(menu4);
	}

	public void advancedMenu() {
		//	was Tools menu

		JMenu menu5;
		menu5 = new JMenu("Tools  ");
		menu5.setMnemonic(KeyEvent.VK_T);

		menuItem51 = new JCheckBoxMenuItem("Centrality Heatmap", false);
		menuItem51.setActionCommand("centcol");
		menuItem51.setToolTipText("Warmer colors represent higher betweenness centrality");
		menuItem51.addActionListener(controler);
		menu5.add(menuItem51);

		menu5.addSeparator();

		menuItem55 = new JCheckBoxMenuItem("Tablet Mode", false);
		menuItem55.setActionCommand("tablet");
		menuItem55.setToolTipText("Doubleclick improvement, Alt-Key for Pen and Touch");
		menuItem55.addActionListener(controler);
		menu5.add(menuItem55);

		menuItem52 = new JCheckBoxMenuItem("Power User Mode", false);
		menuItem52.setActionCommand("power");
		menuItem52.setToolTipText("Acceleration, Lurid Colors, and Borders");
		menuItem52.addActionListener(controler);
		menu5.add(menuItem52);

		JMenuItem menuItem54 = new JMenuItem("Preferences",  KeyEvent.VK_S);
		menuItem54.setActionCommand("prefs");
		menuItem54.setToolTipText("<html><body><em>(Not yet interesting)</em></body></html>");
		menuItem54.addActionListener(controler);
		menu5.add(menuItem54);

		JMenuItem menuItem53 = new JMenuItem("Make Tree (Experimental)",  KeyEvent.VK_F);
		menuItem53.setActionCommand("layout");
		menuItem53.setToolTipText("Generates a tree layout and structure for exporting");
		menuItem53.addActionListener(controler);
		menu5.add(menuItem53);

		JMenuItem menuItem56 = new JMenuItem("Another Map Window",  KeyEvent.VK_E);
		menuItem56.setActionCommand("sibling");
		menuItem56.setToolTipText("One more map (to ALT + Drag node clusters)");
		menuItem56.addActionListener(controler);
		menu5.add(menuItem56);

		menuBar.add(menu5);
	}

	public void helpMenu() {

		JMenu menu6;
		menu6 = new JMenu("?");
		menu6.setMnemonic(KeyEvent.VK_H);

		JMenuItem menuItem61 = new JMenuItem("Help",  KeyEvent.VK_H);
		menuItem61.setActionCommand("?");
		menuItem61.addActionListener(controler);
		menu6.add(menuItem61);

		menu6.addSeparator();

		JMenuItem menuItem62 = new JMenuItem("About");
		menuItem62.setActionCommand("about");
		menuItem62.addActionListener(controler);
		menu6.add(menuItem62);

		menuBar.add(menu6);
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
		item2.setText("New node");
		menu.add(item2);

		JMenuItem item21 = new JMenuItem("Paste", KeyEvent.VK_V);
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

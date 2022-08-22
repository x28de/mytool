package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.tree.DefaultTreeModel;

import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.BrainExport;
import de.x28hd.tool.exporters.CmapExport;
import de.x28hd.tool.exporters.CsvExport;
import de.x28hd.tool.exporters.DemoJsonExporter;
import de.x28hd.tool.exporters.DwzExport;
import de.x28hd.tool.exporters.Export2WXR;
import de.x28hd.tool.exporters.H5pExport;
import de.x28hd.tool.exporters.ImappingExport;
import de.x28hd.tool.exporters.MakeHTML;
import de.x28hd.tool.exporters.MetamapsExport;
import de.x28hd.tool.exporters.TopicMapExporter;
import de.x28hd.tool.exporters.VueExport;
import de.x28hd.tool.exporters.ZknExport;
import de.x28hd.tool.importers.CompositionWindow;
import de.x28hd.tool.importers.IntegrateNodes;
import de.x28hd.tool.importers.NewStuff;
import de.x28hd.tool.importers.Step3a;
import de.x28hd.tool.layouts.CentralityColoring;
import de.x28hd.tool.layouts.CheckOverlaps;
import de.x28hd.tool.layouts.DAG;
import de.x28hd.tool.layouts.GraphPanelZoom;
import de.x28hd.tool.layouts.MakeCircle;
import de.x28hd.tool.layouts.RandomMap;
import de.x28hd.tool.layouts.SubtreeLayout;

public class PresentationExtras implements ActionListener, MouseListener, KeyListener, PopupMenuListener{
	PresentationService controler;
	GraphPanel graphPanel;
	NewStuff newStuff;
	Object newStuffClass;
	Gui gui;
	LifeCycle lifeCycle;
	TextEditorPanel edi;
	Hashtable<Integer, GraphNode> nodes;
	Hashtable<Integer, GraphEdge> edges;
	
	CompositionWindow compositionWindow;
	JPanel footbar = null;
	JPanel altButton = null;
	boolean altDown = false;
	boolean showMenuBar = true;
	int toggle4 = 0;   // => hide classicMenu 
	
	Point panning = new Point(3, 0);
	int animationPercent = 0;
	Point translation;
	
	// Finding accessories
	String findString = "";
	HashSet<Integer> shownResults = null;
	
	int initialSize;
	int zoomedSize;
	boolean contextPasteAllowed = true;
	boolean hyp = false;
	boolean dragFake = false;
	
	// Tree accessories
	DefaultTreeModel treeModel;
	HashSet<GraphEdge> nonTreeEdges;
	boolean treeBug = false;	// TODO fix

	Point upperGap = new Point(3, 0);
	Point dropLocation = null;
	boolean dropHere = false;
	Point pasteLocation = null;
	boolean pasteHere = false;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	CentralityColoring centralityColoring;
	JSplitPane splitPane = null;
	JPanel rightPanel = null;
	int dividerPos = 0;
	GraphPanelZoom graphPanelZoom;
	boolean rectangle = false;

	
	//	Show a hint instead of initial Composition window
	public Timer hintTimer = new Timer(25, new ActionListener() { 
	    public void actionPerformed (ActionEvent e) { 
			graphPanel.jumpingArrow(true);
			graphPanel.grabFocus();
	    } 
	});
	
	//	Trying animation for find result panning 
	private Timer animationTimer2 = new Timer(20, new ActionListener() { 
	    public void actionPerformed (ActionEvent e) {
	    	if (animationPercent < 100) {
	    		animationPercent = animationPercent + 5;
	        	Double dX = -panning.x / 20.0;
	        	Double dY = -panning.y / 20.0;
	        	int pannedX = dX.intValue();;
	        	int pannedY = dY.intValue();;
	        	graphPanel.translateGraph(pannedX, pannedY);
	    	} else {
	    		animationTimer2.stop();
	    		animationPercent = 0;
	        	graphPanel.setModel(nodes, edges);
	     	}
	    	updateBounds();
	    	translation = graphPanel.getTranslation();
	    	controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
	    	graphPanel.repaint();
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
	    	controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
	    	graphPanel.repaint();
	    } 
	});
	
	public PresentationExtras(PresentationService c) {
		controler = c;
		lifeCycle = controler.getLifeCycle();
		edi = controler.getEdi();
		nodes = controler.getNodes();
		edges = controler.getEdges();
	}
	
	public void actionPerformed(ActionEvent arg0) {
		stopHint();
		String command = arg0.getActionCommand();
		JMenuItem item = (JMenuItem) arg0.getSource();
		JPopupMenu menu = (JPopupMenu) item.getParent();
		String menuID = menu.getLabel();	// the menuID is stored in the menu's label
		if (menuID == null) menuID = "Menu Bar";
		
		// Open or Insert

		if (command == "insert" || command == "new") {
			openComposition();
				
		//	Context menu command

		// Node menu
		} else if (menuID.equals("node")) {

			String colorString = "";

			if (command == "purple") colorString = gui.nodePalette[gui.paletteID][0];
			if (command == "blue") colorString =  gui.nodePalette[gui.paletteID][1];
			if (command == "green") colorString =  gui.nodePalette[gui.paletteID][2];
			if (command == "yellow") colorString =  gui.nodePalette[gui.paletteID][3];
			if (command == "orange") colorString =  gui.nodePalette[gui.paletteID][4];
			if (command == "red") colorString =  gui.nodePalette[gui.paletteID][5];
			if (command == "lightGray") colorString =  gui.nodePalette[gui.paletteID][6];
			if (command == "gray") colorString =  gui.nodePalette[gui.paletteID][7];

			if (!colorString.isEmpty()) controler.getSelectedNode().setColor(colorString);

		// Edge menu		
		} else if (menuID.equals("edge")) {

			String colorString = "";

			if (command == "purple") colorString = gui.edgePalette[gui.paletteID][0];
			if (command == "blue") colorString =  gui.edgePalette[gui.paletteID][1];
			if (command == "green") colorString =  gui.edgePalette[gui.paletteID][2];
			if (command == "yellow") colorString =  gui.edgePalette[gui.paletteID][3];
			if (command == "orange") colorString =  gui.edgePalette[gui.paletteID][4];
			if (command == "red") colorString =  gui.edgePalette[gui.paletteID][5];
			if (command == "lightGray") colorString =  gui.edgePalette[gui.paletteID][6];
			if (command == "gray") colorString =  gui.edgePalette[gui.paletteID][7];

			if (!colorString.isEmpty()) controler.getSelectedEdge().setColor(colorString);
			
		} else if (command.startsWith("faceColor")) {
			int faceNum = Integer.parseInt(command.substring(9));
			String colorString =  gui.nodePalette[1][7 + faceNum];			
			controler.getSelectedNode().setColor(colorString);
		}
		
		//	Various toggles	

		if (command == "TogglePreso") {
			graphPanel.togglePreso();

		} else if (command =="ToggleBorders") {
			graphPanel.toggleBorders();
				
		} else if (command == "ToggleHyp") {
			toggleHyp(1, false);
		} else if (command == "ToggleDetEdit") {
			toggleHyp(0, false);

		} else if (command == "find") {
			find(false);
		} else if (command == "findagain") {
			find(true);
		} else if (command == "hashes") {
			toggleHashes(gui.menuItem63.isSelected());
			
		} else if (command == "jump") {
			GraphNode end = controler.getSelectedEdge().getNode2();
			Point xy = end.getXY();
			Point transl = graphPanel.getTranslation();
			Rectangle viewPort = controler.getMainWindow().getBounds();
			viewPort.translate(-transl.x, -transl.y);
			if (viewPort.contains(xy)) {
				GraphNode end2 = controler.getSelectedEdge().getNode1();
				Point xy2 = end2.getXY();
				if (!viewPort.contains(xy2)) {
					xy = xy2;
					end = end2;
				}
			}
			int dx = xy.x - controler.getMainWindow().getWidth()/2 + transl.x + 200;
			int dy = xy.y - controler.getMainWindow().getHeight()/2 + transl.y;
			panning = new Point(dx, dy);
			graphPanel.nodeSelected(end);
			animationTimer2.start();
			
		//	More toggles	
			
		} else if (command == "tablet") {
			toggleTablet();
			
		} else if (command == "classicMenu") {
			toggleClassicMenu();
			toggle4 = 1 - toggle4;
			
		} else if (command == "ToggleCards") {
			boolean desiredState = gui.menuItem46.isSelected();
			graphPanel.toggleCards(desiredState);
			gui.menuItem47.setSelected(false);	// Auto off
			
		} else if (command == "AutoCircles") {
			boolean desiredState = gui.menuItem47.isSelected();
			if (desiredState) recount();
			
		} else if (command == "TogglePalette") {
			gui.togglePalette();
			
		} else if (command =="ToggleClusterCopy") {
			graphPanel.toggleClusterCopy();
			
		} else if (command == "ToggleHeavy") {
			graphPanel.toggleAntiAliasing();
		
		} else if (command == "power") {
			if (gui.menuItem52.isSelected()) {
				if (!gui.menuItem42.isSelected()) {
					graphPanel.toggleBorders();
					gui.menuItem42.setSelected(true);
				}
				if (!gui.menuItem23.isSelected()) {
					gui.paletteID = 0;
					gui.menuItem23.setSelected(true);
				}
				if (!gui.menuItem45.isSelected()) {
					graphPanel.toggleAntiAliasing();
					gui.menuItem45.setSelected(true);
				}
			} else {
				if (gui.menuItem42.isSelected()) {
					graphPanel.toggleBorders();
					gui.menuItem42.setSelected(false);
				}
				if (gui.menuItem23.isSelected()) {
					gui.paletteID = 1;
					gui.menuItem23.setSelected(false);
				}
				if (gui.menuItem45.isSelected()) {
					graphPanel.toggleAntiAliasing();
					gui.menuItem45.setSelected(false);
				}
			}
			
			} else if (command == "toggleParse") {
				String javav = System.getProperty("java.version");
				if (javav.contains("1.8")) {
					newStuff.setParseMode(gui.menuItem25.isSelected());
				} else {
					controler.displayPopup("Your Java Runtime " + javav + " is too old, 1.8 needed.");
					gui.menuItem25.setSelected(false);
				}
			} else if (command == "toggleEncoding") {
				newStuff.setDropEncoding(gui.menuItem28.isSelected());
				
			// Minor actions
				
			} else if (command == "?") {
				gui.displayHelp();
			} else if (command == "extmsg") {
				new LimitationMessage();
			} else if (command == "select") {
				controler.displayPopup("<html><h3>How to Select</h3>" 
						+ "Select a cluster of connected items by clicking any line;<br />" 
						+ "select a single item by clicking its icon.<br /><br />"
						+ "For rectangular rubberband selection, ALT + Drag <br>"
						+ "the mouse on the canvas for spanning the rectangle;<br>"
						+ "click inside the rectangle to dismiss it.</html>");
			} else if (command == "HowToPrint") {
				controler.displayPopup("<html><h3>How to Print or Snapshot</h3>" 
						+ "You can <b>Export</b> to a printable HTML page and then<br />" 
						+ "print, zoom or screenshot from your browser.<br /><br />"
						+ "Instead of a <i>static</i> snapshot, you may also consider<br />"
						+ "an <i>interactive</i> HTML page that allows panning<br /> "
						+ "and selecting.</html>");
			} else if (command == "loadhelp") {
				newStuff.setInput(gui.getSample(true), 2);
				dragFake = true;
			} else if (command == "introgame") {
				newStuff.setInput(gui.getSample(false), 2);
				
			} else if (command == "zoomin") {
				zoomedSize += 4;
				edi.setSize(zoomedSize);
				graphPanel.setSize(zoomedSize);
			} else if (command == "zoomout") {
				zoomedSize -= 4;
				edi.setSize(zoomedSize);
				graphPanel.setSize(zoomedSize);
			} else if (command == "zoomreset") {
				zoomedSize = initialSize;
				edi.setSize(zoomedSize);
				graphPanel.setSize(zoomedSize);
				
			} else if (command == "sibling") {
				launchSibling();
			} else if (command == "wxr2sql") {
				new WXR2SQL(controler.getMainWindow());
				
				
			//	Exports 
				
			} else if (command == "wxr") {
				new Export2WXR(nodes, edges, controler);
				
			} else if (command == "imexp") {
				if (!controler.getExtended()) new LimitationMessage(); 
				else {
					String s = lifeCycle.askForLocation("im.iMap");
					if (!s.isEmpty()) new ImappingExport(nodes, edges, s, controler);
				}
			} else if (command == "zkexp") {
				String s = lifeCycle.askForLocation("zk.zkn3");	//	zkx3 did not work
				if (!s.isEmpty())new ZknExport(nodes, edges, s, controler);
				
			} else if (command == "dwzexp") {
				if (!controler.getExtended()) new LimitationMessage();
				else {
					String s = lifeCycle.askForLocation("dwz.kgif.xml");
					if (!s.isEmpty()) new DwzExport(nodes, edges, s, controler);
				}
			} else if (command == "cmapexp") {
					String s = lifeCycle.askForLocation("my.cmap.cxl");
					if (!s.isEmpty()) new CmapExport(nodes, edges, s, controler);

			} else if (command == "brainexp") {
					String s = lifeCycle.askForLocation("my.brain.xml");
					if (!s.isEmpty()) new BrainExport(nodes, edges, s, controler);

			} else if (command == "vueexp") {
					String s = lifeCycle.askForLocation("my.vue");
					if (!s.isEmpty()) new VueExport(nodes, edges, s, controler);

			} else if (command == "metamexp") {
					String s = lifeCycle.askForLocation("export.json");
					if (!s.isEmpty()) new MetamapsExport(nodes, edges, s, controler);

			} else if (command == "csvexp") {
					String s = lifeCycle.askForLocation("csv.txt");
					if (!s.isEmpty()) new CsvExport(nodes, edges, s, controler);

			} else if (command == "edgeexp") {
					String s = lifeCycle.askForLocation("csv.txt");
					if (!s.isEmpty()) new CsvExport(nodes, edges, s, controler, true);

			} else if (command == "h5pexp") {
					new H5pExport(nodes, edges, controler);
					
			// Export to legacy or exotic formats
					
			} else if (command == "MakeHTML") {
				String lastHTMLFilename = lifeCycle.getLastHTMLFilename();
				if (lastHTMLFilename.isEmpty()) {
					if (lifeCycle.askForFilename("htm")) {
						lastHTMLFilename = lifeCycle.getLastHTMLFilename();
						new MakeHTML(false, nodes, edges, lastHTMLFilename, controler);
					}
				} else {
					new MakeHTML(false, nodes, edges, lastHTMLFilename, controler);
				}
			} else if (command == "Print") {
				String lastHTMLFilename = lifeCycle.getLastHTMLFilename();
				if (lastHTMLFilename.isEmpty()) {
					if (lifeCycle.askForFilename("htm")) {
						lastHTMLFilename = lifeCycle.getLastHTMLFilename();
						new MakeHTML(true, nodes, edges, lastHTMLFilename, controler);
					}
				} else {
					new MakeHTML(true, nodes, edges, lastHTMLFilename, controler);
				}
				
			} else if (command == "export") {
				String s = lifeCycle.askForLocation("legacy.zip");
				new TopicMapExporter(nodes, edges).createTopicmapArchive(s);
					
			} else if (command == "expJson") {
				String s = lifeCycle.askForLocation("experimental.json");
				new DemoJsonExporter(nodes, edges, s);
					
			} else if (command == "Anonymize") { 
				String s = lifeCycle.askForLocation("anonymized.xml");
				if (controler.startStoring(s, true)) controler.displayPopup(s + " saved.\n" +
				"All letters a-z replaced by x, all A-Z by X");
			
			// Layouts
				
			} else if (command == "dag") {
				new DAG(nodes, edges, controler);
			} else if (command == "makecircle") {
				new MakeCircle(nodes, edges, controler);
			} else if (command == "planar") {
				new CheckOverlaps(controler, nodes, edges);
			} else if (command == "centcol") {
				if (gui.menuItem51.isSelected()) {
				centralityColoring = new CentralityColoring(nodes, edges);
					centralityColoring.changeColors();
				} else {
					centralityColoring.revertColors();
				}
				graphPanel.repaint();
				
			} else if (command == "layout") {
				centralityColoring = new CentralityColoring(nodes, edges);
					centralityColoring.changeColors(true, controler);
				graphPanel.repaint();
				gui.menuItem51.setSelected(true);
				
				} else if (command == "subtree") {
				new SubtreeLayout(controler.getSelectedNode(), nodes, edges, 
						controler, true, controler.getTranslation());
				
			} else if (command == "random") {
				RandomMap randomMap = new RandomMap(controler);
				if (randomMap.triggerColoring()) {
					centralityColoring = new CentralityColoring(nodes, edges);
					centralityColoring.changeColors();
				}
				
			} else if (command == "zoom") {
				zoom(true);
				
			} else if (command == "flipHori") {
				flipCluster(controler.getSelectedEdge(), true);
				controler.graphSelected();
				
			} else if (command == "flipVerti") {
				flipCluster(controler.getSelectedEdge(), false);
				controler.graphSelected();

			}
	}
	
	public void openComposition() {
		compositionWindow = new CompositionWindow(this, zoomedSize);
		gui.menuItem21.setEnabled(false);	// Main Menu Paste
		contextPasteAllowed = false;	// Context Menu Paste
		newStuff.setCompositionMode(true);
	}
	
    public CompositionWindow getCWInstance() {
    	return compositionWindow;
    }
	
    public void finishCompositionMode() {
    	newStuff.setCompositionMode(false);
    	gui.menuItem21.setEnabled(true);
    	contextPasteAllowed = true;
    	setSystemUI(true);
    }
    
    //
    // Context menu with accessories
    
    public JPopupMenu createContextMenu(String menuID) {
		JPopupMenu menu = new JPopupMenu(menuID);
		menu.addPopupMenuListener(this);
		setSystemUI(false); //	avoid confusing colors when hovering, and indenting  of items in System LaF 

		if (menuID.equals("graph")) {
			gui.createGraphMenu(menu);
		} else if (menuID.equals("node")) {
			gui.createNodeMenu(menu, gui.paletteID);
		} else if (menuID.equals("edge")) {
			gui.createEdgeMenu(menu, gui.paletteID);
		}
		return menu;
    	
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
			FontUIResource fontResource = new FontUIResource(Font.DIALOG, Font.PLAIN, initialSize);
			UIManager.put("MenuItem.font", fontResource);
			UIManager.put("Menu.font", fontResource);
		}
	}
    
	// More
	
	public boolean getHyp() {
		return hyp;
	}
	public void toggleHyp(int whichWasToggled, boolean silent) {
		
		if (!silent) {
			// Ignore hit if already selected
			if (whichWasToggled == 1) { 	// hyperlinks 
				if (gui.menuItem41.isSelected() == hyp) return;
			} else {
				if (gui.menuItem22.isSelected() == !hyp) return;
			}
		} else {
			if (hyp) return;
			gui.menuItem41.setSelected(!hyp);
			gui.menuItem22.setSelected(hyp);
		}
		// Do the work 
		edi.toggleHyp();
		hyp = !hyp;
		// Reflect change in texts
		int stateHyp = 0;
		if (gui.menuItem41.isSelected()) stateHyp = 1;
		if (!silent) controler.displayPopup(gui.popupHyp[stateHyp]);
		gui.menuItem22.setToolTipText(gui.tooltip22[stateHyp]);
		gui.menuItem41.setToolTipText(gui.tooltip41[1 - stateHyp]);
	}
	
	public void find(boolean again) {
		String userRequest;
		int hitNumber = 0; 
		if (!again) {
			findString = "";
			shownResults = new HashSet<Integer>();
		}
		while (hitNumber >= 0) {	//	Exit by user pressing cancel
			hitNumber++;
			if (findString.isEmpty()) {
				userRequest = (String) JOptionPane.showInputDialog("Find (in labels):", findString);
			} else {
				userRequest = (String) JOptionPane.showInputDialog("Find (in labels) again:", findString);
			}
			if (userRequest == null) {
				break;
			}
			findString = userRequest;
			gui.menuItem98.setEnabled(true);
			Enumeration<Integer> nodesEnum = nodes.keys();
			while (nodesEnum.hasMoreElements()) {
				int nodeID = nodesEnum.nextElement();
				GraphNode node = nodes.get(nodeID);
				String label = node.getLabel().toLowerCase();
				if (label.contains(findString.toLowerCase())) {
					if (shownResults.contains(nodeID)) continue;
					shownResults.add(nodeID);
					Point xy = node.getXY();
					Point transl = graphPanel.getTranslation();
					int dx = xy.x - controler.getMainWindow().getWidth()/2 + transl.x + 200;
					int dy = xy.y - controler.getMainWindow().getHeight()/2 + transl.y;
					panning = new Point(dx, dy);
					graphPanel.nodeSelected(node);
					animationTimer2.start();
					break;
				} else continue;
			}
		}
	}
	
	public void findHash(String hash) {
		boolean found = false;
		findString = hash;
		Enumeration<Integer> nodesEnum = nodes.keys();
		while (nodesEnum.hasMoreElements()) {
			int nodeID = nodesEnum.nextElement();
			GraphNode node = nodes.get(nodeID);
			String label = node.getLabel();
			// Allow for B+ additions
			label = label.trim();
			if (!hyp && label.contains(" ")) {
				int offset = label.indexOf(" ");
				label = label.substring(0, offset);
			} 
			if (label.equalsIgnoreCase(findString)) {	
				Point xy = node.getXY();
				Point transl = controler.getTranslation();
				int dx = xy.x - controler.getMainWindow().getWidth()/2 + transl.x + 200;
				int dy = xy.y - controler.getMainWindow().getHeight()/2 + transl.y;
				panning = new Point(dx, dy);
				graphPanel.nodeSelected(node);
				animationTimer2.start();
				found = true;
				break;
			} else continue;
		}
		if (!found) controler.displayPopup(hash + " not found on this map.");
	}
	public void toggleHashes(boolean onOff) {
		gui.menuItem63.setSelected(onOff);
		edi.toggleHashes(onOff);
		toggleHyp(1, true);
	}
	
	public void setAltSimulation() {
		
		//	Simulate "Alt" button (for pen or touch, since Button3+drag is not available,
		//	and Button2+Drag would interfere with, or delay, the context menu whose immediacy
		//	has a higher relevance for user's perceived control)
		graphPanel.setLayout(new BorderLayout());
		
		footbar = new JPanel();
		footbar.setBackground(Color.WHITE);
		footbar.setLayout(new BorderLayout());
		altButton = new JPanel();
		altButton.add(new JLabel("Alt"));
		altButton.setBorder(new EmptyBorder(10, 10, 10, 10));

		altButton.setBackground(Color.LIGHT_GRAY);
		altButton.setPreferredSize(new Dimension(40, 40));
		altButton.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {	//	On MS Surface not working 
			}
			public void mouseReleased(MouseEvent e) {
				altDown = !altDown;
				graphPanel.toggleAlt(altDown);
				toggleAltColor(altDown);
			}
		});
		footbar.add(altButton, BorderLayout.WEST);
		footbar.setVisible(false);	// until Tools > tablet is specified
		graphPanel.add(footbar, BorderLayout.SOUTH);
		graphPanel.repaint();
		
	}
	
	public void toggleTablet() {
		boolean tablet = gui.menuItem55.isSelected();
		footbar.setVisible(tablet);
		edi.toggleTablet(tablet);
		graphPanel.toggleTablet(tablet);
		if (tablet) controler.displayPopup("Now you can simulate the Alt Key either by a toggle \"button\"\n" +
				"in the lower left, or by double-clicking on an icon or on a line.\n\n" +
				"Warning: \nSince this functionality is still not satisfying it may be changed again.");
	}
	
	public void toggleAltColor(boolean down) {
		if (down) {
			altButton.setBackground(Color.YELLOW);
		} else {
			altButton.setBackground(Color.LIGHT_GRAY);
		}
		altDown = down;
	}
	
	public void toggleClassicMenu() {
		showMenuBar = !showMenuBar;
		if (showMenuBar) {
			gui.menuItem43.setSelected(true);
			controler.getMainWindow().setJMenuBar(gui.getMenuBar());
			controler.getMainWindow().validate();
			controler.getMainWindow().getContentPane().repaint();
		} else {
			JMenuBar nullMenuBar = new JMenuBar();
			controler.getMainWindow().setJMenuBar(nullMenuBar);
			controler.getMainWindow().validate();
			controler.getMainWindow().getContentPane().repaint();
		}
	}
	
	   public void stopHint() {
		   hintTimer.stop();
		   graphPanel.jumpingArrow(false);
	   }
	
	   // Major class exchanges
	   
	   public void triggerUpdate(Object newStuffClass) {
		   this.newStuffClass = newStuffClass;
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
//			   dropLocation = newStuff.getDropLocation();
			   dropLocation = ((Step3a) newStuffClass).getDropLocation();
			   System.out.println("dropLocation " + dropLocation);
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
				   controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
				   graphPanel.repaint();
			   }
		   }
	   }
	   
	   public void performUpdate() {
		   boolean existingMap = ((Step3a) newStuffClass).isExistingMap();
		   System.out.println("PS existingMap " + existingMap);
		   stopHint();
		   if (!lifeCycle.isLoaded() && existingMap && nodes.size() < 1) {
			   //  don't set dirty yet
			   lifeCycle.setConfirmedFilename(newStuff.getAdvisableFilename());
			   lifeCycle.setLoaded(true);
		   } else {
			   lifeCycle.setDirty(true);
		   }
		   System.out.println("Hallo?");
		   Step3a step3a = (Step3a) newStuffClass;
//		   Hashtable<Integer, GraphNode> newNodes = ((Step3a) newStuffClass).getNodes();
//		   Hashtable<Integer, GraphEdge> newEdges = ((Step3a) newStuffClass).getEdges();
		   Hashtable<Integer, GraphNode> newNodes = step3a.getNodes();
		   Hashtable<Integer, GraphEdge> newEdges = step3a.getEdges();
		   System.out.println("PS " + newNodes.size() + " nodes, " + newEdges.size() + " edges");
		   System.out.println("Hallo??");
		   if (lifeCycle.getFilename().isEmpty()) {
			   lifeCycle.resetFilename(newStuff.getAdvisableFilename());
		   }
		   IntegrateNodes integrateNodes = new IntegrateNodes(nodes, edges, newNodes, newEdges);
		   translation = graphPanel.getTranslation();

		   integrateNodes.mergeNodes(upperGap, translation);
		   nodes = integrateNodes.getNodes();
		   edges = integrateNodes.getEdges();

		   graphPanel.setModel(nodes, edges);
		   recount();
		   updateBounds();
		   controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
		   graphPanel.repaint();
		   pasteHere = false;
		   dropHere = false;
		   controler.graphSelected();
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
	//	Display nodes as cards until edges outweigh
	public void recount() {
		boolean moreNodes = (nodes.size() >= edges.size());
		if (gui.menuItem47.isSelected()) {	// auto 
			graphPanel.toggleCards(moreNodes);
			gui.menuItem46.setSelected(moreNodes);
		}
	}
	
	   public void replaceByTree(Hashtable<Integer,GraphNode> replacingNodes, 
			   Hashtable<Integer,GraphEdge> replacingEdges) {
		   if (nodes.size() > 0) {
			   controler.displayPopup("Please use an empty map if you want to use\n" 
					   + "the imported tree information for re-export.");
			   treeModel = null;
			   nonTreeEdges = null;
			   return;
		   }
		   nodes = replacingNodes;
		   edges = replacingEdges;
		   controler.setModel(nodes, edges);
		   updateBounds();
		   controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
		   stopHint();
		   graphPanel.repaint();
	   }
	   
	   public void replaceForLayout(Hashtable<Integer,GraphNode> replacingNodes, 
			   Hashtable<Integer,GraphEdge> replacingEdges) {	
		   // TODO integrate with replaceByTree
		   nodes = replacingNodes;
		   edges = replacingEdges;
		   controler.setModel(nodes, edges);
		   updateBounds();
		   controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
		   stopHint();
		   graphPanel.repaint();
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

	   public void zoom(boolean on) {
		   dividerPos = splitPane.getDividerLocation();
		   if (on) {
			   Point transl = graphPanel.getTranslation();
			   graphPanelZoom = new GraphPanelZoom(transl, controler);
			   splitPane.setDividerLocation(dividerPos);
			   splitPane.setRightComponent(graphPanelZoom.createSlider());
			   graphPanelZoom.setModel(nodes, edges);
			   splitPane.setLeftComponent(graphPanelZoom);
			   toggleClassicMenu();
		   } 
		   if (!on) {
			   gui.menuItem58.setSelected(false);
			   splitPane.setDividerLocation(dividerPos);
			   splitPane.setLeftComponent(graphPanel);
			   splitPane.setRightComponent(rightPanel);
			   toggleClassicMenu();
		   }
		}
		public void flipCluster(GraphEdge assoc, boolean horizontal) {
			rectangle = controler.getRectangle();
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
		
		public void launchSibling() {
			new MyTool();
			String[] dummyArg = {""};
			MyTool.main(dummyArg);
		}

//
//		Accessories intended for right-click (paste) in label field
	    
		public void mouseClicked(MouseEvent arg0) {
			if ((arg0.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
				JPopupMenu menu = Utilities.showContextMenu();
				menu.show(controler.getLabelField(), arg0.getX(), arg0.getY());
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
//		Accessories intended for enter in label field and delete rectangle 
	    
		public void keyPressed(KeyEvent arg0) {
			if (arg0.getKeyChar() == KeyEvent.VK_ENTER) {
				GraphNode justUpdated = controler.getSelectedNode();
				controler.graphSelected();
				graphPanel.labelUpdateToggle(true);
				controler.nodeSelected(justUpdated);
				graphPanel.labelUpdateToggle(false);
				controler.getMainWindow().repaint();   // this was crucial
			}
		}
		public void keyReleased(KeyEvent arg0) {
		}
		public void keyTyped(KeyEvent arg0) {
			if (arg0.getKeyChar() == KeyEvent.VK_DELETE) {
					if (controler.getRectangle()) controler.deleteCluster(true, controler.getSelectedEdge(), false);
			}
		}
		
		// Temporary and experimental
		public void linkTo(String label) {
			toggleHashes(true);
			GraphNode activeNode = controler.getSelectedNode();
			activeNode.setDetail(edi.getText());

			Point activeXY = controler.getSelectedNode().getXY();
			Point newXY = new Point(activeXY.x - 30, activeXY.y + 30);
			GraphNode newNode = controler.createNode(newXY);
			controler.getLabelField().setText(label);

			String labelActive = activeNode.getLabel();
			String detailNew = "<br/>See also <a href=\"#" + labelActive + "\">" + labelActive + "</a>";
			newNode.setDetail(detailNew);
			controler.nodeSelected(newNode);	// see deselect() peculiarities

			controler.createEdge(activeNode, newNode);
			controler.getMainWindow().repaint();
		}
		
//		
// establish addressability
  
    public PresentationService getControler() {
    	return controler;
    }
    
	public void setGui(Gui g) {
		gui = g;
		gui.setControlerExtras(this);
	}
	
	public void setSplitPane (JSplitPane splitPane, JPanel rightPane) {
		this.splitPane = splitPane;
		this.rightPanel = rightPane;
	}
	
	public void init() {
		gui = controler.getGui();
		graphPanel = controler.getGraphPanel();
		setAltSimulation();
		newStuff = controler.getNSInstance();
	}

	public void setInitialSize(int size) {
		initialSize = size;
		zoomedSize = initialSize;

	}

	public void setPasteOptions(boolean pasteHere, Point pasteLocation) {
		this.pasteHere = pasteHere;
		this.pasteLocation = pasteLocation;
		
	}
}

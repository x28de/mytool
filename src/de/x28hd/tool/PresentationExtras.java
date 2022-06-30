package de.x28hd.tool;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.FontUIResource;

public class PresentationExtras implements ActionListener, PopupMenuListener{
	GraphPanelControler controler;
	GraphPanel graphPanel;
	NewStuff newStuff;
	Gui gui;
	TextEditorPanel edi;
	Hashtable<Integer, GraphNode> nodes;
	Hashtable<Integer, GraphEdge> edges;
	
	CompositionWindow compositionWindow;
	// temporary copies
	int paletteID = 1;
	int initialSize = 12;
	int zoomedSize = initialSize;
	Point panning = new Point(3, 0);
	int animationPercent = 0;
	Point translation;
	// Finding accessories
	String findString = "";
	HashSet<Integer> shownResults = null;
	
	boolean contextPasteAllowed = true;
	boolean hyp = false;
	
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
	    	controler.updateBounds();
	    	translation = graphPanel.getTranslation();
	    	controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
	    	graphPanel.repaint();
	    } 
	});
	
	public PresentationExtras(GraphPanelControler c) {
		controler = c;
	}
	
	public void actionPerformed(ActionEvent arg0) {
		controler.stopHint();
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

			if (command == "purple") colorString = gui.nodePalette[paletteID][0];
			if (command == "blue") colorString =  gui.nodePalette[paletteID][1];
			if (command == "green") colorString =  gui.nodePalette[paletteID][2];
			if (command == "yellow") colorString =  gui.nodePalette[paletteID][3];
			if (command == "orange") colorString =  gui.nodePalette[paletteID][4];
			if (command == "red") colorString =  gui.nodePalette[paletteID][5];
			if (command == "lightGray") colorString =  gui.nodePalette[paletteID][6];
			if (command == "gray") colorString =  gui.nodePalette[paletteID][7];

			if (!colorString.isEmpty()) controler.getSelectedNode().setColor(colorString);

		// Edge menu		
		} else if (menuID.equals("edge")) {

			String colorString = "";

			if (command == "purple") colorString = gui.edgePalette[paletteID][0];
			if (command == "blue") colorString =  gui.edgePalette[paletteID][1];
			if (command == "green") colorString =  gui.edgePalette[paletteID][2];
			if (command == "yellow") colorString =  gui.edgePalette[paletteID][3];
			if (command == "orange") colorString =  gui.edgePalette[paletteID][4];
			if (command == "red") colorString =  gui.edgePalette[paletteID][5];
			if (command == "lightGray") colorString =  gui.edgePalette[paletteID][6];
			if (command == "gray") colorString =  gui.edgePalette[paletteID][7];

			if (!colorString.isEmpty()) controler.getSelectedEdge().setColor(colorString);
			
		} else if (command.startsWith("faceColor")) {
			int faceNum = Integer.parseInt(command.substring(9));
			String colorString =  gui.nodePalette[1][7 + faceNum];			
			controler.getSelectedNode().setColor(colorString);
			
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
			gui.createNodeMenu(menu, paletteID);
		} else if (menuID.equals("edge")) {
			gui.createEdgeMenu(menu, paletteID);
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
	
    // establish addressability
  
    public GraphPanelControler getControler() {
    	return controler;
    }
    
	public void setNewStuff(NewStuff ns) {
		newStuff = ns;
		ns.setControlerExtras(this);
	}
	
	public void setGui(Gui g) {
		gui = g;
		gui.setControlerExtras(this);
	}
	
	public void setMap() {
		nodes = controler.getNodes();
		edges = controler.getEdges();
		graphPanel = controler.getGraphPanel();
	}

	public void setEdi(TextEditorPanel e) {
		edi = e;
	}

}

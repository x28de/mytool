package de.x28hd.tool;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
	
	CompositionWindow compositionWindow;
	// temporary copies
	int paletteID = 1;
	int initialSize = 12;
	int zoomedSize = initialSize;
	
	boolean contextPasteAllowed = true;
	
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

}

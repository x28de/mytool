package de.x28hd.tool;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PresentationExtras implements ActionListener{
	GraphPanelControler controler;
	GraphPanel graphPanel;
	NewStuff newStuff;
	Gui gui;
	
	CompositionWindow compositionWindow;
	int initialSize = 12;
	int zoomedSize = initialSize;
	boolean contextPasteAllowed = true;
	
	public PresentationExtras(GraphPanelControler c) {
		controler = c;
	}
	
	public void actionPerformed(ActionEvent arg0) {
		controler.stopHint();
		String command = arg0.getActionCommand();
		// Open or Insert

		if (command == "insert" || command == "new") {
			openComposition();
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
    	controler.setSystemUI(true);
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

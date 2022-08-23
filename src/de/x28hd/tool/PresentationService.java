package de.x28hd.tool;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.undo.UndoManager;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.core.PresentationCore;
import de.x28hd.tool.exporters.TopicMapStorer;
import de.x28hd.tool.importers.CompositionWindow;
import de.x28hd.tool.importers.ImportDirector;
import de.x28hd.tool.importers.NewStuff;

public final class PresentationService extends PresentationCore implements ActionListener, Runnable {

	// Main cooperating classes
	LifeCycle lifeCycle = new LifeCycle(this);
	UndoManager undoManager = new UndoManager();
	// The next 4 are mutually interdependent:
	PresentationExtras controlerExtras;	// needs edi & lifeCycle
	Gui gui;							// needs undoManager
	NewStuff newStuff = null;
	
	// User Interface
	private JButton OK;	
	
	JMenuBar myMenuBar = null;
	boolean showMenuBar = true;
	public String about =  " ******** Provisional BANNER ********* " +
			"\r\n ******** Provisional BANNER ********* ";
	public String preferences = "No preferences yet (no installation)";
	
	int initialSize = 12;
	
	//	Graphics accessories
	Point clickedSpot = null;
	Point translation = new Point(0, 0);

	// Input/ output accessories
	CompositionWindow compositionWindow = null;
	String dataString = "";	
	int inputType = 0;
	String baseDir;
	
	//	Toggles
	boolean rectangle = false;
	
	public boolean extended = false;

	
	public PresentationService(boolean ext) {
		editorClass = new TextEditorPanel(this);
		extended = ext;
	}

//
//	Process menu clicks
	
	public void actionPerformed(ActionEvent e) {
		
		controlerExtras.stopHint();
		String command = e.getActionCommand();

		if (command == "open") {
			String filename = lifeCycle.open();
			newStuff.setInput(filename, 1);
		} else if (command == "importDirector") {
			new ImportDirector(this);	

		} else if (command == "quit") {
			close();
			
		} else if (command == "undo") {
			undoManager.undo();
		} else if (command == "redo") {
			undoManager.redo();
			
		} else if (command == "copy") {
			copy(rectangle, selectedAssoc);
		} else if (command == "cut") {
			cut(rectangle, selectedAssoc);
			
		} else if (command == "paste") {
			controlerExtras.setPasteOptions(false, null);
			Transferable t = newStuff.readClipboard();
			if (!newStuff.transferTransferable(t)) {
				System.out.println("Error PS121");
			}
		} else if (command == "pasteHere") {
			controlerExtras.setPasteOptions(true, clickedSpot);
			Transferable t = newStuff.readClipboard();
			if (!newStuff.transferTransferable(t)) {
				System.out.println("Error PS121a");
			}

		} else if (command == "Store") {
			lifeCycle.save();
		} else if (command == "SaveAs") {
			lifeCycle.saveAs();
			
		// using startup data
		} else if (command == "about") {
				displayPopup(about);
		} else if (command == "prefs") {
			displayPopup(preferences);

		} else if (command == "delCluster") {
				deleteCluster(rectangle, selectedAssoc);
				graphSelected();
						
		//	Context menu command

		} else if (command == "delTopic") {
				deleteNode(selectedTopic);
				selection.topic = null;
				graphSelected();
				graphClass.repaint();

		} else if (command == "delAssoc") {
				deleteEdge(selectedAssoc);
				graphSelected();
				graphClass.repaint();

		} else if (command == "NewNode") {
				translation = ((GraphPanel) graphClass).getTranslation();
		    	clickedSpot.translate(- translation.x, - translation.y);
				GraphNode node = createNode(clickedSpot);
				nodeSelected(node);
				labelField.requestFocus();

//		} else if (command == "tst") {
		} else {
			System.out.println("PS: Wrong action: " + command);
		}
	}

//
//	Main Window	
	
	public void createMainWindow(String title) {
		super.createMainWindow(title);
		
		// Override super's warning-less close()
		WindowListener[] wls = (WindowListener[]) (mainWindow.getListeners(WindowListener.class));
		mainWindow.removeWindowListener(wls[0]);
		mainWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
		});
		
		myMenuBar = gui.createMenuBar();
		if (controlerExtras.showMenuBar) {
			mainWindow.setJMenuBar(myMenuBar);
		} else {
			JMenuBar nullMenuBar = new JMenuBar();
			mainWindow.setJMenuBar(nullMenuBar);
		}
	}

//
//	Popups

	public void displayContextMenu(String menuID, int x, int y) {
		JPopupMenu menu = controlerExtras.createContextMenu(menuID);
		clickedSpot = new Point(x, y);
		controlerExtras.setSystemUI(false); //	avoid confusing colors when hovering, and indenting  of items in System LaF 
		menu.show(contentPane, x, y);
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
		graphClass.setSize(initialSize);
		mainWindow.setVisible(true);
		((TextEditorPanel) editorClass).setSize(initialSize);
		controlerExtras.setInitialSize(initialSize);
		if (lifeCycle.getFilename().isEmpty() 
				&& nodes.size() <= 0) {  // tmp
			controlerExtras.hintTimer.start();
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

		// The next 4 classes need each other
		controlerExtras = new PresentationExtras(this);
		gui = new Gui(this);
		graphClass = new GraphPanel(this);
		newStuff = new NewStuff(this);
		
		// Introduce them to each other
		controlerExtras.init();
		gui.init();
		graphClass.init();
		newStuff.init();
		
		about = (new AboutBuild(extended)).getAbout();
		((GraphPanel) graphClass).addKeyListener(controlerExtras);

		// Main GUI
		controlerExtras.setSystemUI(true);
		super.initialize(lifeCycle.getMainWindowTitle());  // model, main Window @ GUI
		labelField.addMouseListener(controlerExtras);
		labelField.addKeyListener(controlerExtras);
		lifeCycle.setDirty(false);
		controlerExtras.setSplitPane(splitPane, rightPanel);
		lifeCycle.add(baseDir);
		
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
			((TextEditorPanel) editorClass).setDirty(false);
		}
		return true;
	}

	public boolean close() {	// Mac needs this
		return lifeCycle.close();
	}


//	Selection processing

	public void deselect(GraphNode node) {
		super.deselect(node);
		((TextEditorPanel) editorClass).tracking(false);
	}
	
	public void nodeSelected(GraphNode node) {
		super.nodeSelected(node);
		((TextEditorPanel) editorClass).tracking(true);
		((TextEditorPanel) editorClass).setDirty(false);
		((TextEditorPanel) editorClass).getTextComponent().requestFocus();
		if (controlerExtras.getHyp()) 
			((TextEditorPanel) editorClass).getTextComponent().setCaretPosition(0);
		if (controlerExtras.dragFake && node.getLabel().equals("Drop input")) 
			((TextEditorPanel) editorClass).setFake();
		updateCcpGui();
	}

	public void edgeSelected(GraphEdge edge) {
		super.edgeSelected(edge);
		((TextEditorPanel) editorClass).getTextComponent().setCaretPosition(0);
		updateCcpGui();
	}

	public void graphSelected() {
		super.graphSelected();
		editorClass.setText(gui.getInitText(nodes.size() <= 0));
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
			controlerExtras.updateBounds();
			graphClass.nodeSelected(topic);
			graphClass.repaint();
			lifeCycle.setDirty(true);
			return topic;
		} else {
			return null;
		}
	}
	
	public GraphEdge createEdge(GraphNode topic1, GraphNode topic2) {
		GraphEdge assoc = super.createEdge(topic1,  topic2);
		controlerExtras.recount();
		lifeCycle.setDirty(true);
		return assoc;
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
			translation = ((GraphPanel) graphClass).getTranslation();
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
		controlerExtras.updateBounds();
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
		graphClass.repaint();
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
			cluster = ((GraphPanel) graphClass).createNodeCluster(topic1);
		} else {
			cluster = ((GraphPanel) graphClass).createNodeRectangle();
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
		((GraphPanel) graphClass).nodeRectangle(false);
		graphClass.repaint();
		lifeCycle.setDirty(true);
		return;
	}

	public void copyCluster(boolean rectangle, GraphEdge assoc) {
		GraphNode topic1 = assoc.getNode1();	
		((GraphPanel) graphClass).copyCluster(rectangle, topic1);
		return;
	}

//
//	Misc 
    
	public void setMouseCursor(int cursorType) {
		mainWindow.setCursor(new Cursor(cursorType));
	}
	
//
//	Communication with other classes
    
    public NewStuff getNSInstance() {
    	return newStuff;
    }
    
    public GraphExtras getGraphExtras() {
     	return ((GraphPanel) graphClass).getExtras();
    }
    
    public GraphPanel getGraphPanel() {
     	return (((GraphPanel) graphClass));
    }
    
    public Point getTranslation() {
    	return ((GraphPanel) graphClass).getTranslation();
    }

    public JFrame getMainWindow() {
    	return mainWindow;
    }
    
   public String getNewNodeColor() {
	   return gui.nodePalette[gui.paletteID][7];
   }
    
   public boolean getExtended() {
	   return extended;
   }
   public void toggleRectanglePresent(boolean on) {
	   rectangle = on;
	   updateCcpGui();
   }
   
	public void commit(int type, GraphNode node, GraphEdge edge, Point move) {
		MyUndoableEdit myUndoableEdit = new MyUndoableEdit(type, node, edge, move, 
				nodes, edges, ((GraphPanel) graphClass), gui);
		undoManager.addEdit(myUndoableEdit);
		gui.updateUndoGui();
	}
	

	public void copy(boolean rectangle, GraphEdge assoc) {
		GraphNode topic = assoc.getNode1();	
		((GraphPanel) graphClass).copyCluster(rectangle, topic);
	}

	public boolean getRectangle() {
		return rectangle;
	}
	
	public void cut(boolean rectangle, GraphEdge assoc) {
		GraphNode topic = assoc.getNode1();	
		((GraphPanel) graphClass).copyCluster(rectangle, topic);
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

	public Hashtable<Integer,GraphNode> getNodes() {
		return nodes;
	}
	public Hashtable<Integer,GraphEdge> getEdges() {
		return edges;
	}

	public GraphNode getSelectedNode() {
		return selectedTopic;
	}
	public GraphEdge getSelectedEdge() {
		return selectedAssoc;
	}

	public PresentationExtras getControlerExtras() {
		return controlerExtras;
	}

	public LifeCycle getLifeCycle() {
		return lifeCycle;
	}

	public UndoManager getUndoManager() {
		return undoManager;
	}
	
	public TextEditorPanel getEdi() {
		return (TextEditorPanel) editorClass;
	}
	
	public Gui getGui() {
		return gui;
	}
	
	public JTextField getLabelField() {
		return labelField;
	}
	
}

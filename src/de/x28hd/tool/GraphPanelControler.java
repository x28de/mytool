package de.x28hd.tool;
import java.awt.Point;
import java.util.Hashtable;

import javax.swing.JFrame;

interface GraphPanelControler {
	void nodeSelected(GraphNode node);
	void edgeSelected(GraphEdge edge);
	void graphSelected();
	void displayContextMenu(String menuID, int x, int y) ;
	void displayPopup(String msg);
	TextEditorPanel edi = null;
	void addToLabel(String text);
	void setFilename(String filename, int type);
	NewStuff getNSInstance();
	GraphEdge createEdge(GraphNode topic1, GraphNode topic2);
	void setMouseCursor(int type);
	void setDirty(boolean toggle);
	String getNewNodeColor();
	void commit(int type, GraphNode node, GraphEdge edge, Point move);
	void toggleRectanglePresent(boolean on);
	boolean getExtended();
	JFrame getMainWindow();
	boolean startStoring(String storeFilename, boolean anonymized);
	void deleteCluster(boolean rectangle, GraphEdge assoc, boolean auto);
	Hashtable<Integer,GraphNode> getNodes();
	Hashtable<Integer,GraphEdge> getEdges();
	boolean close();
	public GraphNode getSelectedNode();
	public GraphEdge getSelectedEdge();
	public void fixDivider();
	void linkTo(String textToAdd);
	public GraphExtras getGraphExtras();
	public PresentationExtras getControlerExtras();
	public Point getTranslation();
	public GraphPanel getGraphPanel();
	public void setModel(Hashtable<Integer, GraphNode> nodes, 
			Hashtable<Integer, GraphEdge> edges);
	LifeCycle getLifeCycle();
	boolean getRectangle();
	}

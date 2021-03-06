package de.x28hd.tool;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.tree.DefaultTreeModel;

interface GraphPanelControler {
	void nodeSelected(GraphNode node);
	void edgeSelected(GraphEdge edge);
	void graphSelected();
	void displayContextMenu(String menuID, int x, int y) ;
	void displayPopup(String msg);
	TextEditorPanel edi = null;
	void addToLabel(String text);
	void setFilename(String filename, int type);
	void triggerUpdate();
	NewStuff getNSInstance();
	CompositionWindow getCWInstance();
	void finishCompositionMode();
	void manip(int x);
	GraphEdge createEdge(GraphNode topic1, GraphNode topic2);
	void setMouseCursor(int type);
	void setDirty(boolean toggle);
	void updateBounds();
	String getNewNodeColor();
	Rectangle getBounds();
	void setSystemUI(boolean toggle);
	void toggleAltColor(boolean down);
	void setTreeModel(DefaultTreeModel model);
	DefaultTreeModel getTreeModel();
	void setNonTreeEdges(HashSet<GraphEdge> nonTreeEdges);
	HashSet<GraphEdge> getNonTreeEdges();
	void replaceByTree(Hashtable<Integer, GraphNode> nodes,
			Hashtable<Integer, GraphEdge> edges);
	void commit(int type, GraphNode node, GraphEdge edge, Point move);
	void toggleRectanglePresent(boolean on);
	void stopHint();
	void zoom(boolean on);
	boolean getExtended();
	void toggleHyp(int whichWasToggled, boolean auto);
	JFrame getMainWindow();
	void replaceForLayout(Hashtable<Integer,GraphNode> replacingNodes, 
			   Hashtable<Integer,GraphEdge> replacingEdges); 
	boolean startStoring(String storeFilename, boolean anonymized);
	void findHash(String hash);
	void toggleHashes(boolean onOff);
	void deleteCluster(boolean rectangle, GraphEdge assoc, boolean auto);
	Hashtable<Integer,GraphNode> getNodes();
	Hashtable<Integer,GraphEdge> getEdges();
	boolean close();
	public GraphNode getSelectedNode();
	public void fixDivider();
	void linkTo(String textToAdd);
	public GraphExtras getGraphExtras();
	}

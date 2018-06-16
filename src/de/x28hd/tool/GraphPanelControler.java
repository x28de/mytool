package de.x28hd.tool;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Hashtable;

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
	void triggerUpdate(boolean justOneMap);
	NewStuff getNSInstance();
	CompositionWindow getCWInstance();
	void finishCompositionMode();
	void manip(int x);
	void beginTranslation();
	void beginCreatingEdge();
	void beginLongTask();
	void endTask();
	GraphEdge createEdge(GraphNode topic1, GraphNode topic2);
	void setDefaultCursor();
	public void setWaitCursor();
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
	}

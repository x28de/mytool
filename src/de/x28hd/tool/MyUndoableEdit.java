package de.x28hd.tool;

import java.awt.Point;
import java.util.Hashtable;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class MyUndoableEdit implements UndoableEdit {
	String [] presentationNames = {"Delete", "Delete Line", "Move", ""};
	int lastChangeType;
	static final int DELETE_NODE = 0;
	static final int DELETE_EDGE = 1;
	static final int MOVE = 2;
	static final int NONE = 3;
	GraphNode lastNode;
	GraphEdge lastEdge;
	Point lastMove;
	
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	GraphPanel graphPanel;
	Gui gui;

	public MyUndoableEdit(int type, GraphNode node, GraphEdge edge, Point move,
			Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges,
			GraphPanel graphPanel, Gui gui) {
		lastChangeType = type;
		lastNode = node;
		lastEdge = edge;
		lastMove = move;
		this.nodes = nodes;
		this.edges = edges;
		this.graphPanel = graphPanel;
		this.gui = gui;
	}
	public boolean addEdit(UndoableEdit arg0) {
		return false;
	}
	public boolean canRedo() {
		return true;
	}
	public boolean canUndo() {
		return true;
	}
	public void die() {
	}
	public String getPresentationName() {
		String addedInfo = "";
		if (lastNode != null) addedInfo = " \"" + lastNode.getLabel() + "\"";
		return presentationNames[lastChangeType] + addedInfo;
	}
	public String getRedoPresentationName() {
		return "Redo " + getPresentationName();
	}
	public String getUndoPresentationName() {
		return "Undo " + getPresentationName();
	}
	public boolean isSignificant() {
		return true;	// was crucial
	}
	public void redo() throws CannotRedoException {
		if (lastChangeType == DELETE_NODE) {
			GraphNode node = lastNode;
			int id = node.getID();
			nodes.remove(id);
		} else if (lastChangeType == DELETE_EDGE) {
			GraphEdge edge = lastEdge;
			int id = edge.getID();
			edges.remove(id);
		} else if (lastChangeType == MOVE) {
			Point xy = lastNode.getXY();
			xy = new Point(xy.x + lastMove.x, xy.y + lastMove.y);
			lastNode.setXY(xy);
			graphPanel.repaint();
		}
		gui.updateUndoGui();
	}
	public boolean replaceEdit(UndoableEdit anEdit) {
		return false;
	}
	public void undo() throws CannotUndoException {
		if (lastChangeType == DELETE_NODE) {
			GraphNode node = lastNode;
			int id = node.getID();
			nodes.put(id,  node);
		} else if (lastChangeType == DELETE_EDGE) {
			GraphEdge edge = lastEdge;
			int id = edge.getID();
			edges.put(id, edge);
			GraphNode node1 = nodes.get(edge.getN1());
			GraphNode node2 = nodes.get(edge.getN2());
			node1.addEdge(edge);
			node2.addEdge(edge);
		} else if (lastChangeType == MOVE) {
			Point xy = lastNode.getXY();
			xy = new Point(xy.x - lastMove.x, xy.y - lastMove.y);
			lastNode.setXY(xy);
			graphPanel.repaint();
		}
		gui.updateUndoGui();
	}
}


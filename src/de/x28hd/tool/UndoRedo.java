package de.x28hd.tool;

import java.awt.Point;
import java.util.Hashtable;

public class UndoRedo {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	GraphPanelControler controler;
	Gui gui;
	
	int lastChangeType;
	static final int DELETE_NODE = 0;
	static final int DELETE_EDGE = 1;
	static final int MOVE = 2;
	static final int NONE = 3;
	String[] lastChange = {"Delete single item", "Delete single line", "Single move", ""};
	GraphNode lastNode;
	GraphEdge lastEdge;
	Point lastMove = new Point(0, 0);
	
	public UndoRedo(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges,
			GraphPanelControler controler) {
		this.controler = controler;
		this.nodes = nodes;
		this.edges = edges;
		gui = controler.getGuiInstance();
		System.out.println(nodes.size());
	}
	
	public void commit(int type, GraphNode node, GraphEdge edge, Point move) {
		gui.menuItem91.setText("Undo: " + lastChange[type]);
		lastChangeType = type;
		gui.menuItem91.setEnabled(true);
		if (type == DELETE_NODE) {
			lastNode = node;
		} else if (type == DELETE_EDGE) {
			lastEdge = edge;;
		} else if (type == MOVE) {
			lastMove = move;
		} else if (type == NONE) {
			gui.menuItem91.setEnabled(false);
		}
		gui.menuItem92.setText("Redo ");
		gui.menuItem92.setEnabled(false);
	}
	
		public void undo() {
			int type = lastChangeType;
			gui.menuItem92.setText("Redo: " + lastChange[type]);
			if (type == DELETE_NODE) {
				GraphNode node = lastNode;
				int id = node.getID();
				nodes.put(id,  node);
			} else if (type == DELETE_EDGE) {
				GraphEdge edge = lastEdge;
				int id = edge.getID();
				edges.put(id, edge);
			} else if (type == MOVE) {
				GraphNode selectedTopic = controler.getSelectedNode();
				Point xy = selectedTopic.getXY();
				xy = new Point(xy.x - lastMove.x, xy.y - lastMove.y);
				selectedTopic.setXY(xy);
				controler.getMainWindow().repaint();
			}
			gui.menuItem91.setText("Undo ");
			gui.menuItem91.setEnabled(false);
			gui.menuItem92.setText("Redo: " + lastChange[type]);
			gui.menuItem92.setEnabled(true);
		}

		public void redo() {
			int type = lastChangeType;
			if (type == DELETE_NODE) {
				GraphNode node = lastNode;
				int id = node.getID();
				nodes.remove(id);
			} else if (type == DELETE_EDGE) {
				GraphEdge edge = lastEdge;
				int id = edge.getID();
				edges.remove(id);
			} else if (type == MOVE) {
				GraphNode selectedTopic = controler.getSelectedNode();
				Point xy = selectedTopic.getXY();
				xy = new Point(xy.x + lastMove.x, xy.y + lastMove.y);
				selectedTopic.setXY(xy);
				controler.getMainWindow().repaint();
			}
			gui.menuItem92.setText("Redo ");
			gui.menuItem92.setEnabled(false);
			gui.menuItem91.setText("Undo: " + lastChange[type]);
			gui.menuItem91.setEnabled(true);
		}

}

package de.x28hd.tool.importers;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class EdgeList {
	
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	int maxVert = 10;
	int j = 0;
	
	public EdgeList (File file, PresentationService controler) {
		nodes = controler.getNodes();
		edges = controler.getEdges();

		// Make index of existing labels
		Hashtable<String,Integer> label2id = new Hashtable<String,Integer>();
		Enumeration<Integer> nodesEnum = nodes.keys();
		while (nodesEnum.hasMoreElements()) {
			int key = nodesEnum.nextElement();
			GraphNode node = nodes.get(key);
			String label = node.getLabel();
			label2id.put(label, key);
		}
		
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error EL101 " + e);
		}
		Utilities utilities = new Utilities();
		String linesString = utilities.convertStreamToString(fileInputStream);	
		String [] lines = linesString.split("\\r?\\n");	
		
		// Scan input for new nodes
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String [] fields = line.split("\\t");
			for (int f = 0; f < 2; f++) {
				String label = fields[f];
				if (!label2id.containsKey(label)) {
					int id = addNode(label);
					label2id.put(label, id);
				}
			}
		}
		
		// Make edges
		int max = edges.size();
		int edgeNum = max;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String [] fields = line.split("\\t");
			String label1 = fields[0];
			String label2 = fields[1];
			int	n1 = label2id.get(label1);
			int	n2 = label2id.get(label2);
			GraphNode node1 = nodes.get(n1);
			GraphNode node2 = nodes.get(n2);
			edgeNum++;
			GraphEdge edge = new GraphEdge(edgeNum, node1, node2, Color.decode("#d8d8d8"), "");
			edges.put(edgeNum, edge);
			node1.addEdge(edge);
			node2.addEdge(edge);
		}
		
		controler.getMainWindow().repaint();
	}
	
	public int addNode(String label) {
		j++;
		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		int id = newKey(nodes.keySet());
		GraphNode topic = new GraphNode (id, p, Color.decode("#ccdddd"), label, label);	

		nodes.put(id, topic);
		return id;
	}
	public int newKey(Set<Integer> keySet) {	// duplicate in PS; TODO integrate
		int idTest = keySet.size();
		while (keySet.contains(idTest)) idTest++;
		return idTest;
	}
	
}

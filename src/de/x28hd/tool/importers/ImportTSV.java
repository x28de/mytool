package de.x28hd.tool.importers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;

public class ImportTSV implements ActionListener {
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	HashSet<PlannedEdge> plannedEdges = new HashSet<PlannedEdge>(); 
	TreeMap<Integer,GraphNode> usedNodes = new TreeMap<Integer,GraphNode>(); 
	SortedMap<Integer,GraphNode> usedList = (SortedMap<Integer,GraphNode>) usedNodes;
	int maxVert = 10;
	int j = 0;
	int edgesNum = 0;
	int number;
	int skippedEdges = 0;
	PresentationService controler;
	JDialog panel;
	JCheckBox skipBox;
	boolean skip;
	JCheckBox colorBox;
	boolean ignore;
	
	private class PlannedEdge {
		int n1;
		int n2;
		Color color;
		
		private PlannedEdge(int n1, int n2, String colorString) {
			this.n1 = n1;
			this.n2 = n2;
			color = Color.decode(colorString);
		}
		
		private GraphEdge createEdge() {
			if (!nodes.containsKey(n1)) return null;
			GraphNode node1 = nodes.get(n1);
			if (!nodes.containsKey(n2)) return null;
			GraphNode node2 = nodes.get(n2);
			usedList.put(n1, node1);
			usedList.put(n2, node2);
			edgesNum++;
			GraphEdge edge = new GraphEdge(edgesNum, node1, node2, color, "");
			node1.addEdge(edge);
			node2.addEdge(edge);
			return edge;
		}
	}
	
	public ImportTSV (PresentationService c) {
		controler = c;

		// Instructions and options
		panel = new JDialog(controler.getMainWindow(), "Specifications", true);
		panel.setMinimumSize(new Dimension(400, 300));
		panel.setLocation(200, 200);
		panel.setLayout(new BorderLayout());
		
		JLabel top = new JLabel();
		top.setText("<html>For each node and edge we need an input line with three values, separated by the TAB character."
				+ "<br><br>Nodes are specified by one numeric and two text values:"
				+ "<br><pre>ID	Label	Detail</pre><br>"
				+ "Edges are specified by one text character and two numeric values:"
				+ "<br><pre>switch	ID1	ID2</pre>"
				+ "where <tt>switch</tt> is either empty or \"<tt>n</tt>\" to indicate <ul>"
				+ "<li>a positive relationship to be colored in green, "
				+ "<li>or a negative (n) relationship to be colored in red."
				+ "</ul>(This option was inspired by discourse mapping).");
		top.setBorder(new EmptyBorder(10, 10, 10, 10));
		panel.add(top, "North");
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());
		bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		skipBox = new JCheckBox(" Skip unconnected items?", true);
		bottom.add(skipBox, "Center");
		colorBox = new JCheckBox(" Ignore color switch", false);
		bottom.add(colorBox, "West");
		
		JButton continueButton = new JButton("Continue");
		continueButton.addActionListener(this);
		continueButton.setSelected(true);
		bottom.add(continueButton, "East");

		panel.add(bottom, "South");

		panel.setVisible(true);
	}
	
	public void mainPart() {
		panel.dispose();
		FileDialog fd = new FileDialog(controler.getMainWindow());
		fd.setTitle("Select a Tab Separated Values file");
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		File file = new File(filename);

		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error TS101 " + e);
		}
		Utilities utilities = new Utilities();
		String linesString = utilities.convertStreamToString(fileInputStream);	
		String [] lines = linesString.split("\\r?\\n");	
		
		// Scan input for new nodes
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String [] fields = line.split("\\t");
			if (fields.length < 3) {
				controler.displayPopup("Invalid format in this line:\n" + line);
				return;
			}
			int id = -1;
			String label = "";
			String detail = "";
			int n1 = -1;
			int n2 = -1;
			String marked = "";
			String colorString = "#c0c0c0";
			
			if (isInteger(fields[0])) {		// nodes 
				id = Integer.parseInt(fields[0]);
				label = fields[1];
				detail = fields[2];
				addNode(id, label, detail);

			} else {						// edges 
				if (!ignore) {
					marked = fields[0];
					colorString = marked.isEmpty() ? "#bbffbb" : "#ffbbbb";
				}
				if (isInteger(fields[1])) {
					n1 = Integer.parseInt(fields[1]);
				} else {
					controler.displayPopup("Invalid format in this line:\n" + line);
					return;
				}
				if (isInteger(fields[2])) {
					n2 = Integer.parseInt(fields[2]);
				} else {
					controler.displayPopup("Invalid format in this line:\n" + line);
					return;
				}
				
				PlannedEdge pledge = new PlannedEdge(n1, n2, colorString);
				plannedEdges.add(pledge);
			}
		}

		Iterator<PlannedEdge> todoList = plannedEdges.iterator();
		while (todoList.hasNext()) {
			PlannedEdge pledge = todoList.next();
			GraphEdge edge = pledge.createEdge();
			if (edge == null) {
				skippedEdges++;
				continue;
			}
			edges.put(edgesNum, edge);
		}
		if (skippedEdges > 0) controler.displayPopup(skippedEdges + " edges skipped");

		SortedSet<Integer> usedSet = (SortedSet<Integer>) usedList.keySet();
		Iterator<Integer> iter = usedSet.iterator();

		int sequence = 0;
		while (iter.hasNext()) {
			sequence++;
			int i = iter.next();
			GraphNode node = usedList.get(i);
			Point xy = new Point(sequence * (- 20), sequence * 20);
			node.setXY(xy);
		}
		
		// Include unused ?
		
		Enumeration<Integer> allNodeIDs = nodes.keys();
		while (allNodeIDs.hasMoreElements()) {
			int id = allNodeIDs.nextElement();
			if (usedSet.contains(id)) continue;
			if (skip) {
				nodes.remove(id);
				continue;
			}
			j++;
			int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			int x = 40 + (j/maxVert) * 150;
			Point xy = new Point(x, y);
			nodes.get(id).setXY(xy);
		}
		
	    // Pass on
	    
	    String dataString = "";
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error TS108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error TS109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error TS110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
	}
	
	public int addNode(int id, String label, String detail) {
		GraphNode topic = new GraphNode (id, new Point(40, 40), Color.decode("#ccdddd"), label, detail);	
		nodes.put(id, topic);
		return id;
	}
	
	public static boolean isInteger(String string) {
		try {
			Integer.parseInt(string);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		skip = skipBox.isSelected();
		ignore = colorBox.isSelected();
		mainPart();
	}
}

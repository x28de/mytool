package de.x28hd.tool.accessories;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class ListNeighbors implements ActionListener {
	
	PresentationService controler;
	JFrame listFrame = new JFrame();
	
	   public ListNeighbors(PresentationService c) {
		   controler = c;
		   GraphNode node = controler.getSelectedNode();
		   TreeMap<Double,GraphEdge> outgoing = new TreeMap<Double,GraphEdge>();
		   TreeMap<Double,GraphEdge> incoming = new TreeMap<Double,GraphEdge>();
		   HashSet<Double> vertical = new HashSet<Double>();
		   Enumeration<GraphEdge> neighbors = node.getEdges();
		   while (neighbors.hasMoreElements()) {
			   GraphEdge edge = neighbors.nextElement();
			   GraphNode related = node.relatedNode(edge);
			   double y = related.getXY().getY();
			   while (vertical.contains(y)) {
				   y += .0001;
			   }
			   vertical.add(y);
			   GraphNode node1 = edge.getNode1();
			   if (node1.equals(node)) {
				   outgoing.put(y, edge);
			   } else {
				   incoming.put(y, edge);
			   }
		   }
		   SortedMap<Double,GraphEdge> outList = (SortedMap<Double,GraphEdge>) outgoing;
		   SortedMap<Double,GraphEdge> inList = (SortedMap<Double,GraphEdge>) incoming;
		   SortedSet<Double> outSet = (SortedSet<Double>) outList.keySet();
		   SortedSet<Double> inSet = (SortedSet<Double>) inList.keySet();
		   Iterator<Double> outIter = outSet.iterator();
		   Iterator<Double> inIter = inSet.iterator();
		   
		   String list = "<html>Outgoing neighbors:<br><br>";
		   int outCount = 0;
		   while (outIter.hasNext()) {
			   outCount++;
			   Double y = outIter.next();
			   GraphEdge edge = outgoing.get(y);
			   GraphNode related = node.relatedNode(edge);
			   Color color = edge.getColor();
			   int r = color.getRed();
			   int g = color.getGreen();
			   int b = color.getBlue();
			   String colorString = String.format("#%02x%02x%02x", r, g, b);
			   list += "<span style=\"background-color: " + colorString + ";\">--></span> " 
			   + related.getLabel() + "<br>";
		   }
		   if (outCount < 1) list += "<em>(none)</em><br>";
		   
		   list += "<br>Incoming neighbors:<br><br>";
		   int inCount = 0;
		   while (inIter.hasNext()) {
			   inCount++;
			   Double y = inIter.next();
			   GraphEdge edge = incoming.get(y);
			   GraphNode related = node.relatedNode(edge);
			   Color color = edge.getColor();
			   int r = color.getRed();
			   int g = color.getGreen();
			   int b = color.getBlue();
			   String colorString = String.format("#%02x%02x%02x", r, g, b);
			   list += "<span style=\"background-color: " + colorString + ";\">&lt;--</span> " 
			   + related.getLabel() + "<br>";
		   }
		   if (inCount < 1) list += "<em>(none)</em><br>";
		   list += "<br>";
		   
		   listFrame.setTitle(node.getLabel());
		   listFrame.setMinimumSize(new Dimension(300, 250));
		   JLabel textPanel = new JLabel();
		   textPanel.setLayout(new BorderLayout());
		   textPanel.setOpaque(true);
		   textPanel.setBackground(Color.WHITE);
		   textPanel.setBorder(new EmptyBorder(10, 50, 10, 10));
		   
		   textPanel.setText(list);
		   
		   JButton refreshButton = new JButton("Refresh");
		   refreshButton.addActionListener(this);
		   textPanel.add(refreshButton, BorderLayout.SOUTH);
		   
		   listFrame.add(textPanel);
		   listFrame.setVisible(true);
	   }

	public void actionPerformed(ActionEvent e) {
		listFrame.dispose();
		new ListNeighbors(controler);
	}

}

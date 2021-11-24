package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

public class RandomMap implements ActionListener {
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	GraphPanelControler controler;
	String dataString = "";
	int edgesNum = 0;
	Random random;
	int total = 100;
	JSlider slider;
	JDialog panel;
	JCheckBox colorBox;

	public RandomMap(GraphPanelControler controler) {
		this.controler = controler;
		
		random = new Random((new Date()).getTime() + 8432570);
		createSlider();		// after user response, mainPart() is called
	}
	
	public void mainPart() {
		for (int i = 0; i < total; i++) {
			int x = (int) (random.nextDouble() * 800);
			int y = (int) (random.nextDouble() * 600);
			GraphNode node = new GraphNode(i, new Point(x, y), Color.decode("#ccdddd"), i + "", "");
			nodes.put(i,  node);
		}
		
		for (int i = 0; i < total; i++) {
			GraphNode node = nodes.get(i);
			boolean found = false;
			while (!found) {
				int otherID = (int) (random.nextDouble() * total);
				if (otherID == i) continue;
				GraphNode otherNode = nodes.get(otherID);
				GraphEdge edge = new GraphEdge(edgesNum, node, otherNode, Color.decode("#c0c0c0"), "");
				edges.put(edgesNum,  edge);
				edgesNum++;
				node.setDetail("<a href=\"#" + otherID + "\">" + otherID + "</a>");	// hyperhopping just for fun
				found = true;
			}
		}
		
		// pass on
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error RM108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error RM109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error RM110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.toggleHashes(true);
	}
	
	public void createSlider() {
		slider = new JSlider(JSlider.VERTICAL);
		slider.setValue(40);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setMaximum(125);
		slider.setMinimum(20);
		slider.setMajorTickSpacing(10);
		Hashtable<Integer,JComponent> labelDict = new Hashtable<Integer,JComponent>();
		slider.createStandardLabels(125);
		for (int i = 12; i > 1; i--) {
			labelDict.put(new Integer(i * 10), (JComponent) new JLabel((int) Math.pow(i, 2.5) + " items"));
		}
		panel = new JDialog(controler.getMainWindow(), "How many items?", true);
		panel.setMinimumSize(new Dimension(200, 500));
		panel.setLocation(200, 200);
		panel.setLayout(new BorderLayout());
		
		slider.setLabelTable((Dictionary<Integer,JComponent>) labelDict);
		panel.add(slider);
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());
		bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		colorBox = new JCheckBox(" Coloring for ease?", true);
		bottom.add(colorBox, "North");
		
		JButton continueButton = new JButton("OK");
		continueButton.addActionListener(this);
		continueButton.setSelected(true);
		bottom.add(continueButton, "East");

		panel.add(bottom, "South");

		panel.setVisible(true);
	}
	
	public boolean triggerColoring() {
		return colorBox.isSelected();
	}
	
	public void actionPerformed(ActionEvent arg0) {
		total = (int) Math.pow(slider.getValue()/10., 2.5);
		System.out.println(total + " items wanted");
		panel.dispose();
		mainPart();
	}
	
}

package de.x28hd.tool;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class BrainImport {
	
	//	Major fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,String> inputItems = new Hashtable<String,String>();
	Hashtable<String,String> inputItems2 = new Hashtable<String,String>();
	Hashtable<String,Point> itemPositions = new Hashtable<String,Point>();
	
	//	Keys for nodes and edges, incremented in addNode and addEdge
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	Hashtable<String,Integer> edgeID2num = new  Hashtable<String,Integer>();
	int j = -1;
	int edgesNum = 0;
	
	//	Constants
	private static final String XML_ROOT = "BrainData";
	int maxVert = 10;
	GraphPanelControler controler;
	
	public BrainImport(JFrame mainWindow, GraphPanelControler controler) {
		this.controler = controler;
		
//
//		Open XML document
		
//		File file = new File("C:\\Users\\Matthias\\Desktop\\testbrain.xml");
		FileDialog fd = new FileDialog(mainWindow);
		fd.setTitle("Select a CMap CXL file");
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		File file = new File(filename);
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error BI101 " + e);
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error BI102 " + e2 );
		}
		
		try {
			inputXml = db.parse(fileInputStream);
			
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != XML_ROOT) {
				System.out.println("Error BI105, unexpected: " + inputRoot.getTagName() );
				fileInputStream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error BI106 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error BI107 " + e );
		}

//
//		Find input items
		
		NodeList itemContainer = inputXml.getElementsByTagName("Thoughts");
		NodeList itemList = ((Element) itemContainer.item(0)).getElementsByTagName("Thought");
		for (int i = 0; i < itemList.getLength(); i++) {
			Element node = (Element) itemList.item(i);
			Element idElem = (Element) node.getElementsByTagName("guid").item(0);
			String itemID = idElem.getTextContent();
			
			//	Label (actually, "Name")
			Element labelElem = (Element) node.getElementsByTagName("name").item(0);
			String labelString = labelElem.getTextContent();
			inputItems.put(itemID, labelString);
		}
		
//		
//		Process input links
		
		NodeList linkContainer = inputXml.getElementsByTagName("Links");
		NodeList linkList = 
				((Element) linkContainer.item(0)).getElementsByTagName("Link");
		edgesNum = 0;
		
		for (int i = 0; i < linkList.getLength(); i++) {

			Element link = (Element) linkList.item(i);
			Element idElem = (Element) link.getElementsByTagName("guid").item(0);
			String id = idElem.getTextContent();
			Element fromElem = (Element) link.getElementsByTagName("idA").item(0);
			String fromItem = fromElem.getTextContent();
			Element toElem = (Element) link.getElementsByTagName("idB").item(0);
			String toItem = toElem.getTextContent();
			if (!inputID2num.containsKey(fromItem)) addNode(fromItem);
			if (!inputID2num.containsKey(toItem)) addNode(toItem);
			addEdge(fromItem, toItem);
			edgeID2num.put(id, edgesNum);
			System.out.println("edges: " + inputID2num.size());
		}
		
//
//		Process unlinked items
		
		Enumeration<String> itemEnum = inputItems.keys();
		while (itemEnum.hasMoreElements()) {
			String item = itemEnum.nextElement();
			if (!inputID2num.containsKey(item)) {
				addNode(item);
			}
		}
		
//			
//		Pass on the new map
		
		System.out.println("BI Map: " + nodes.size() + " " + edges.size());
		try {
			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error BI108 " + e1);
		} catch (IOException e1) {
			System.out.println("Error BI109 " + e1);
		} catch (SAXException e1) {
			System.out.println("Error BI110 " + e1);
		}
		
		controler.getNSInstance().setInput(dataString, 2);
	}
	
	public void addNode(String nodeRef) { 
		boolean linkingPhrase = (inputItems2.containsKey(nodeRef));
		j++;
		String newNodeColor;
		String newLine = "\r";
		String topicName = ""; 
		if (linkingPhrase) {
			topicName = inputItems2.get(nodeRef);
			newNodeColor = "#eeeeee";
		} else {
			topicName = inputItems.get(nodeRef);
			newNodeColor = "#ccdddd";
		}
		String verbal = topicName;
		topicName = topicName.replace("\r"," ");
		if (topicName.equals(newLine)) topicName = "";
		if (verbal == null || verbal.equals(newLine)) verbal = "";
		if (topicName.isEmpty() && verbal.isEmpty()) return;
		int id = 100 + j;

		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		inputID2num.put(nodeRef, id);
	}
	
	public void addEdge(String fromRef, String toRef) {
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
		edgesNum++;
		GraphNode sourceNode = nodes.get(inputID2num.get(fromRef));
		GraphNode targetNode = nodes.get(inputID2num.get(toRef));
		edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), "");
		edges.put(edgesNum, edge);
	}
}

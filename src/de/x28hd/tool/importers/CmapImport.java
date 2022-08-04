package de.x28hd.tool.importers;

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

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;

public class CmapImport {
	
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
	private static final String XML_ROOT = "cmap";
	int maxVert = 10;
	PresentationService controler;
	
	public CmapImport(JFrame mainWindow, PresentationService controler) {
		this.controler = controler;
		
//
//		Open XML document
		
//		File file = new File("C:\\Users\\Matthias\\Desktop\\emerg.cmap.cxl");
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
			System.out.println("Error CI101 " + e);
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error CI102 " + e2 );
		}
		
		try {
			inputXml = db.parse(fileInputStream);
			
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != XML_ROOT) {
				System.out.println("Error CI105, unexpected: " + inputRoot.getTagName() );
				fileInputStream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error CI106 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error CI107 " + e );
		}
		new CmapImport(inputXml, controler);
	}
	
	public CmapImport(Document inputXml, PresentationService controler) {
		
		NodeList graphContainer = inputXml.getElementsByTagName("map");
		Element graph = (Element) graphContainer.item(0);

//
//		Find input items
		
		NodeList itemContainer = graph.getElementsByTagName("concept-list");
		NodeList itemList = ((Element) itemContainer.item(0)).getElementsByTagName("concept");
		
		for (int i = 0; i < itemList.getLength(); i++) {
			Element node = (Element) itemList.item(i);
			String itemID = node.getAttribute("id");
			
			//	Label
			String labelString = node.getAttribute("label");
			inputItems.put(itemID, labelString);
		}
		
		NodeList itemContainer2 = graph.getElementsByTagName("linking-phrase-list");
		if (itemContainer2.getLength() > 0) {
			NodeList itemList2 = ((Element) itemContainer2.item(0)).getElementsByTagName("linking-phrase");

			for (int i = 0; i < itemList2.getLength(); i++) {
				Element node = (Element) itemList2.item(i);
				String itemID = node.getAttribute("id");

				//	Label
				String labelString = node.getAttribute("label");

				inputItems2.put(itemID, labelString);
			}
		}
		
//
//		Find item properties (here: position)
		
		NodeList propContainer = graph.getElementsByTagName("concept-appearance-list");
		NodeList propList = ((Element) propContainer.item(0)).getElementsByTagName("concept-appearance");
		
		for (int i = 0; i < propList.getLength(); i++) {
			Element node = (Element) propList.item(i);
			String itemID = node.getAttribute("id");
			
			//	Position
			int x = Integer.parseInt(node.getAttribute("x"));
			int y = Integer.parseInt(node.getAttribute("y"));
			itemPositions.put(itemID, new Point(x, y));
		}
		
		NodeList propContainer2 = graph.getElementsByTagName("linking-phrase-appearance-list");
		if (propContainer2.getLength() > 0) {
			NodeList propList2 = ((Element) propContainer2.item(0)).getElementsByTagName("linking-phrase-appearance");

			for (int i = 0; i < propList2.getLength(); i++) {
				Element node = (Element) propList2.item(i);
				String itemID = node.getAttribute("id");

				//	Position
				int x = Integer.parseInt(node.getAttribute("x"));
				int y = Integer.parseInt(node.getAttribute("y"));
				itemPositions.put(itemID, new Point(x, y));
			}
		}
		
//		
//		Process input links
		
		NodeList linkContainer = graph.getElementsByTagName("connection-list");
		NodeList linkList = 
				((Element) linkContainer.item(0)).getElementsByTagName("connection");
		edgesNum = 0;
		
		for (int i = 0; i < linkList.getLength(); i++) {

			Element link = (Element) linkList.item(i);
			String id = link.getAttribute("id");
			String fromItem = link.getAttribute("from-id");
			String toItem = link.getAttribute("to-id");
			if (!inputID2num.containsKey(fromItem)) addNode(fromItem);
			if (!inputID2num.containsKey(toItem)) addNode(toItem);
			addEdge(fromItem, toItem);
			edgeID2num.put(id, edgesNum);
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
//		Find dashed connections
		
		NodeList linkPropContainer = graph.getElementsByTagName("connection-appearance-list");
		if (linkPropContainer.getLength() > 0) {
			NodeList linkPropList = 
					((Element) linkPropContainer.item(0)).getElementsByTagName("connection-appearance");
			for (int i = 0; i < linkPropList.getLength(); i++) {

				Element linkProp = (Element) linkPropList.item(i);
				String id = linkProp.getAttribute("id");
				String style = linkProp.getAttribute("style");
				if (style.equals("dashed")) {
					int edgeID = edgeID2num.get(id);
					GraphEdge edge = edges.get(edgeID);
					edge.setColor("#f0f0f0");	// pale
				}
			}
		}
		
//			
//		Pass on the new map
		
		try {
			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error CI108 " + e1);
		} catch (IOException e1) {
			System.out.println("Error CI109 " + e1);
		} catch (SAXException e1) {
			System.out.println("Error CI110 " + e1);
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
//		if (topicName.isEmpty() && verbal.isEmpty()) return;
		int id = 100 + j;

		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		p = itemPositions.get(nodeRef);
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		if (inputID2num.containsKey(nodeRef)) {
			System.out.println("Error CI113");
			return;
		}
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

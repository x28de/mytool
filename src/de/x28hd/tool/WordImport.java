package de.x28hd.tool;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WordImport {
	
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
	private static final String XML_ROOT = "w:document";
	int maxVert = 10;
	GraphPanelControler controler;
	
	public WordImport(JFrame mainWindow, GraphPanelControler controler) {
		this.controler = controler;
		
//
//		Open XML document
		
//		File file = new File("C:\\Users\\Matthias\\Desktop\\probetext\\word\\document.xml");
		FileDialog fd = new FileDialog(mainWindow);
		fd.setTitle("Select a Word file");
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		File file = new File(filename);
		new WordImport(file, controler);
	}
	
	public WordImport(File file, GraphPanelControler controler) {
		Charset CP850 = Charset.forName("CP850");
		ZipFile zfile = null;
		try {
			zfile = new ZipFile(file,CP850);
			Enumeration<? extends ZipEntry> e = zfile.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				String filename = entry.getName();
				filename = filename.replace('\\', '/');		
				if (!filename.equals("word/document.xml")) {
					continue;
				} else {
					InputStream stream = zfile.getInputStream(entry);
					new WordImport(stream, controler);
				}
			}
		} catch (IOException e1) {
			System.out.println("Error ID111 " + e1);
		}
//		FileInputStream fileInputStream = null;
//		try {
//			fileInputStream = new FileInputStream(file);
//		} catch (FileNotFoundException e) {
//			System.out.println("Error BI101 " + e);
//		}
		
	}
	
	public WordImport(InputStream stream, GraphPanelControler controler) {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error BI102 " + e2 );
		}
		
		try {
			inputXml = db.parse(stream);
			
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != XML_ROOT) {
				System.out.println("Error BI105, unexpected: " + inputRoot.getTagName() );
				stream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error BI106 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error BI107 " + e );
		}

//
//		Find input items
		
		NodeList bodyContainer = inputXml.getElementsByTagName("w:body");
		Element body = (Element) bodyContainer.item(0);
		
		NodeList itemList = body.getElementsByTagName("w:p");
		System.out.println("Items: " + itemList.getLength());
		for (int i = 0; i < itemList.getLength(); i++) {
			Element node = (Element) itemList.item(i);
			//	Text
			NodeList runContainer = node.getElementsByTagName("w:r");
			String textString = "";
			for (int j = 0; j < runContainer.getLength(); j++) {
				Element runElem = (Element) runContainer.item(j);
				System.out.println(runElem.getNodeName());
				NodeList textContainer = runElem.getElementsByTagName("w:t");
				if (textContainer.getLength() > 0) {
					Element textElem = (Element) textContainer.item(0);
					textString = textString + textElem.getTextContent();
					inputItems.put(i + " ", textString);
				}
			}
			textString = i + "\t" + textString + "\n";
			System.out.println(textString);
			dataString = dataString + textString;
		}
		
//			
//		Pass on the new map
		
		System.out.println("BI Map: " + nodes.size() + " " + edges.size());
//		try {
//			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
//		} catch (TransformerConfigurationException e1) {
//			System.out.println("Error BI108 " + e1);
//		} catch (IOException e1) {
//			System.out.println("Error BI109 " + e1);
//		} catch (SAXException e1) {
//			System.out.println("Error BI110 " + e1);
//		}
		
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

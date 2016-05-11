package de.x28hd.tool;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class EnexImport {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	String dataString = "";
	
	private static final String XML_ROOT = "en-export";
	
	GraphPanelControler controler;

	public EnexImport(JFrame mainWindow, GraphPanelControler controler) {
//		File file = new File("C:\\Users\\Matthias\\Desktop\\Evernote.enex");
		controler.displayPopup("May 10, 2016: This is just a Quick and Dirty first attempt.\n" + 
				"Regard it as a Proof Of Concept. Soon more.");
		FileDialog fd = new FileDialog(mainWindow);
		fd.setTitle("Select an Evernote ENEX file");
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		File file = new File(filename);
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error EI101 " + e);
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Transformer transformer = null;
		Document enex = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error EI102 " + e2 );
		}
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e2) {
			System.out.println("Error EI103 " + e2);
		} catch (TransformerFactoryConfigurationError e2) {
			System.out.println("Error EI104 " + e2 );
		}
		try {
			enex = db.parse(fileInputStream);
			Element enexRoot = null;
			enexRoot = enex.getDocumentElement();
			if (enexRoot.getTagName() != XML_ROOT) {
				System.out.println("Error EI105, unexpected: " + enexRoot.getTagName() );
				fileInputStream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error EI106 " + e1);			
		} catch (SAXException e) {
			System.out.println("Error EI107" + e );
		}
		
		int maxVert = 10;
		
		NodeList enexItems = enex.getElementsByTagName("note");
		
		for (int i = 0; i < enexItems.getLength(); i++) {
			Element note = (Element) enexItems.item(i);
			NodeList titleContainer = note.getElementsByTagName("title");
			String title = titleContainer.item(0).getTextContent().toString();
			NodeList contentContainer = note.getElementsByTagName("content");
			String content = contentContainer.item(0).getTextContent().toString();
			System.out.println("item " + i + " " + title + "\nß\t" + content);
			
			// Create node
			
			int j = i + 1;
			String newNodeColor = "#ccdddd";
			String newLine = "\r";
			String topicName = title;
			String verbal = content;
			if (topicName.equals(newLine)) topicName = "";
			if (verbal == null || verbal.equals(newLine)) verbal = "";
			if (topicName.isEmpty() && verbal.isEmpty()) continue;
			int id = 100 + j;
			
			int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			int x = 40 + (j/maxVert) * 150;
			Point p = new Point(x, y);
			GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

			nodes.put(id, topic);

		}
		
		//	Pass on
		
		edges.clear();
		try {
			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error EI108 " + e1);
		} catch (IOException e1) {
			System.out.println("Error EI109 " + e1);
		} catch (SAXException e1) {
			System.out.println("Error EI110 " + e1);
		}
		controler.getNSInstance().setInput(dataString, 2);
	}
}

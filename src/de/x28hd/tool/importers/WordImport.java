package de.x28hd.tool.importers;

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

import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.PresentationService;

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
	PresentationService controler;
	
	public WordImport(JFrame mainWindow, PresentationService controler) {
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
	
	public WordImport(File file, PresentationService controler) {
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
			System.out.println("Error WI111 " + e1);
		}
	}
	
	public WordImport(InputStream stream, PresentationService controler) {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error WI102 " + e2 );
		}
		
		try {
			inputXml = db.parse(stream);
			
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != XML_ROOT) {
				System.out.println("Error WI105, unexpected: " + inputRoot.getTagName() );
				stream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error WI106 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error WI107 " + e );
		}
		
		new WordImport(inputXml, controler);
	}
	
	public WordImport(Document inputXml, PresentationService controler) {

//
//		Find input items
		
		NodeList bodyContainer = inputXml.getElementsByTagName("w:body");
		Element body = (Element) bodyContainer.item(0);
		
		NodeList itemList = body.getElementsByTagName("w:p");
		for (int i = 0; i < itemList.getLength(); i++) {
			Element node = (Element) itemList.item(i);
			//	Text
			NodeList runContainer = node.getElementsByTagName("w:r");
			String textString = "";
			for (int j = 0; j < runContainer.getLength(); j++) {
				Element runElem = (Element) runContainer.item(j);
				NodeList textContainer = runElem.getElementsByTagName("w:t");
				if (textContainer.getLength() > 0) {
					Element textElem = (Element) textContainer.item(0);
					textString = textString + textElem.getTextContent();
					inputItems.put(i + " ", textString);
				}
			}
			textString = i + "\t" + textString + "\n";
			dataString = dataString + textString;
		}
		
		controler.getNSInstance().setInput(dataString, 6);
	}
	
	//	TODO remove old attributes
	
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

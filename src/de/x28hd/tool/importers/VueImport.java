package de.x28hd.tool.importers;

import java.awt.Color;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;

public class VueImport {
	
	//	Major fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,String> inputItems = new Hashtable<String,String>();
	
	//	Keys for nodes and edges, incremented in addNode and addEdge
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	Hashtable<String,Integer> edgeID2num = new  Hashtable<String,Integer>();
	int j = -1;
	int edgesNum = 0;
	
	//	Constants
	public final static String XSI = "http://www.w3.org/2001/XMLSchema-instance"; 
//	public final static String XSI = "www.w3.org/2001/XMLSchema-instance"; 
	int maxVert = 10;
	PresentationService controler;
	
	public VueImport(File file, PresentationService controler) {
		this.controler = controler;
		Reader fileReader = null;
		try {
			fileReader = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
		} catch (FileNotFoundException e) {
			System.out.println("Error VI101 " + e);
		}
		BufferedReader reader = new BufferedReader(fileReader);
		String firstNonCommentLine;
		String line = "";
		for (;;) {
			try {
				reader.mark(2048);
				// Comment from VUE: a single comment line can't be longer than this...
				line = reader.readLine();

			} catch (IOException e) {
				System.out.println("Error VI103 " + e);
			}
			if (line == null) {
				System.out.println("Error VI102 ");
			}
			if (line.startsWith("<!--")) {
//				System.out.println("Comment skipped");
			} else {
				firstNonCommentLine = line;
				break;
			}
		}
//    	System.out.println(firstNonCommentLine);
        // (From VUE source:)
        // Reset the reader to the start of the last line read, which should be the <?xml line,
        try {
			reader.reset();
		} catch (IOException e) {
			System.out.println("Error VI104 " + e);
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error VI105 " + e2 );
		}
		
		try {
			inputXml = db.parse(new InputSource(reader));
		} catch (SAXException | IOException e) {
			System.out.println("Error VI106 " + e );
		}

//
//		Find input items
		
		String type ="";
		NodeList itemList = inputXml.getElementsByTagName("child");
		for (int i = 0; i < itemList.getLength(); i++) {
			Element node = (Element) itemList.item(i);
			String itemID = node.getAttribute("ID");
			type = node.getAttributeNS(XSI, "type");
			
			if (type.equals("node")) {
				String xString = node.getAttribute("x");
				String yString = node.getAttribute("y");
				Double xDouble = Double.valueOf(xString);
				Double yDouble = Double.valueOf(yString);
				int x = xDouble.intValue();
				int y = yDouble.intValue();
				Point xy = new Point(x, y);
				
				//	Label
				String labelString = node.getAttribute("label");
				inputItems.put(itemID, labelString);
				
				//	Color
				String colorString = "";
				NodeList colorContainer1 = node.getElementsByTagName("fillColor");
				if (colorContainer1.getLength() > 0) {
					colorString = colorContainer1.item(0).getTextContent();
				}

				//	Details
				String detail = "";
				NodeList detailContainer = node.getElementsByTagName("notes");
				if (detailContainer.getLength() > 0) detail = detailContainer.item(0).getTextContent();

				addNode(itemID, xy, colorString, detail);
			}
		}
			for (int i = 0; i < itemList.getLength(); i++) {
				Element link = (Element) itemList.item(i);
//				String itemID = node.getAttribute("ID");
				type = link.getAttributeNS(XSI, "type");
			if (type.equals("link")) {
				NodeList id1container = link.getElementsByTagName("ID1");
				String id1 = id1container.item(0).getTextContent();
				NodeList id2container = link.getElementsByTagName("ID2");
				String id2 = id2container.item(0).getTextContent();
				addEdge(id1, id2);
			}
		}
//		
//	Pass on the new map
	
//		edges.clear();
	try {
		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
	} catch (TransformerConfigurationException e1) {
		System.out.println("Error VI108 " + e1);
	} catch (IOException e1) {
		System.out.println("Error VI109 " + e1);
	} catch (SAXException e1) {
		System.out.println("Error VI110 " + e1);
	}
	
	this.controler.getNSInstance().setInput(dataString, 2);
	}
	
	public void addNode(String nodeRef, Point xy, String colorString, String detail) { 
		j++;
		String newNodeColor;
		String newLine = "\r";
		String topicName = ""; 
			topicName = inputItems.get(nodeRef);
			newNodeColor = "#ccdddd";
			if (!colorString.isEmpty()) newNodeColor = colorString;
		String verbal = detail;
		topicName = topicName.replace("\r"," ");
		if (topicName.equals(newLine)) topicName = "";
		if (verbal == null || verbal.equals(newLine)) verbal = "";
		if (topicName.isEmpty() && verbal.isEmpty()) return;
		int id = 100 + j;

//		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
//		int x = 40 + (j/maxVert) * 150;
//		Point p = new Point(x, y);
//		p = itemPositions.get(nodeRef);
		GraphNode topic = new GraphNode (id, xy, Color.decode(newNodeColor), topicName, verbal);	

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

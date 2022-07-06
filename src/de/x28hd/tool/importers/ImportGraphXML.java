package de.x28hd.tool.importers;

import java.awt.Color;
import java.awt.Point;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.GraphPanelControler;
import de.x28hd.tool.exporters.TopicMapStorer;

public class ImportGraphXML {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,GraphNode> input2node = new Hashtable<String,GraphNode>();
	String dataString = "";
	int j = 0;
	int maxVert = 10;
	int edgesNum = 0;
	
	public ImportGraphXML(Document inputXml, GraphPanelControler controler) {
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream("C:\\users\\matthias\\desktop\\tags.xml");
		} catch (FileNotFoundException e) {
			System.out.println("Error IG101 " + e);
		}
		
//		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//		DocumentBuilder db = null;
//		Document inputXml = null;
//		try {
//			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//			db = dbf.newDocumentBuilder();
//		} catch (ParserConfigurationException e2) {
//			System.out.println("Error IG102 " + e2 );
//		}

		try {
//			inputXml = db.parse(fileInputStream);
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != "GraphXML") {
				System.out.println("Error IG105, unexpected: " + inputRoot.getTagName() );
				fileInputStream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error IG106 " + e1 + "\n" + e1.getClass());
			System.out.println("Import failed:\n" + e1);
			return;
//		} catch (SAXException e) {
//			System.out.println("Error IG107 " + e );
		}

		NodeList graphContainer = inputXml.getElementsByTagName("graph");
		Element graph = (Element) graphContainer.item(0);

		NodeList nodesList = graph.getElementsByTagName("node");
		if (nodesList.getLength() <= 0) return;
		for (int i = 0; i < nodesList.getLength(); i++) {
			Element node = (Element) nodesList.item(i);
			String name = ((Element) node).getAttribute("name");
			
			NodeList labelContainer = ((Element) node).getElementsByTagName("label");
			String label = "";
			if (labelContainer.getLength() < 1) {
				label = name;
			} else {
				Element labelElement = (Element) labelContainer.item(0);
				label = labelElement.getTextContent();
			}
			GraphNode gnode = addNode(label);
			input2node.put(name, gnode);
		}
		NodeList edgesList = graph.getElementsByTagName("edge");
		if (edgesList.getLength() <= 0) return;
		for (int i = 0; i < edgesList.getLength(); i++) {
			Element edge = (Element) edgesList.item(i);
			String source = ((Element) edge).getAttribute("source");
			String target = ((Element) edge).getAttribute("target");
			GraphNode node1 = input2node.get(source);
			GraphNode node2 = input2node.get(target);
			addEdge(node1, node2);
		}
		
//
//		pass on
		
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error IG108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error IG109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error IG110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.getControlerExtras().setTreeModel(null);
    	controler.getControlerExtras().setNonTreeEdges(null);
		
	}
	
	public GraphNode addNode(String label) {
		j++;
		int id = 100 + j;

		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode("#ccdddd"), label, "");	

		nodes.put(id, topic);
		return topic;
	}

	public void addEdge(GraphNode node1, GraphNode node2) {
		if (node1 == null || node2 == null) return;
		edgesNum++;
		GraphEdge edge = new GraphEdge(edgesNum, node1, node2, Color.decode("#d8d8d8"), "");
		edges.put(edgesNum, edge);
	}
}

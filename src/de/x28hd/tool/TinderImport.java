package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.util.Hashtable;

import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TinderImport {

	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	String dataString = "";
	Hashtable<String,GraphNode> id2node = new Hashtable<String,GraphNode>();
	Element inputRoot = null;
	int j = 0;
	int edgesNum = 0;
			
	public TinderImport(Document inputXml, GraphPanelControler controler) {
			inputRoot = inputXml.getDocumentElement();
		
		//	Extract nodes 
		NodeList children = inputRoot.getElementsByTagName("item");		
		Node child;
		String id = "";
		Node tinderAttr = null;
		String label = "";
		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			id = ((Element) child).getAttribute("ID");
			NodeList tinderAttrs = ((Element) child).getElementsByTagName("attribute");
			for (int j = 0; j < tinderAttrs.getLength(); j++) {
				tinderAttr = tinderAttrs.item(j);
				String xmlAttr = ((Element) tinderAttr).getAttribute("name");
				if (!xmlAttr.equals("Name")) continue;
				label = ((Element) tinderAttr).getTextContent();
				if (label.isEmpty()) continue;
			}
			String detail = "";
			NodeList xmlNodes = ((Element) child).getElementsByTagName("text");
			for (int k = 0; k < xmlNodes.getLength(); k++) {
				detail = xmlNodes.item(k).getTextContent();
			}
			detail = detail.replace("\n", "<br/>");
			GraphNode node = addNode(label, detail);
			id2node.put(id, node);
		}

		//	Extract links 
		NodeList links = inputRoot.getElementsByTagName("link");
		for (int i = 0; i < links.getLength(); i++) {
			Node link = links.item(i);
			String sourceID = ((Element) link).getAttribute("sourceid");
			String destID = ((Element) link).getAttribute("destid");
			if (!id2node.containsKey(sourceID)) continue;
			GraphNode node1 = id2node.get(sourceID);
			if (!id2node.containsKey(sourceID)) continue;
			GraphNode node2 = id2node.get(destID);
			edgesNum++;
			GraphEdge edge = new GraphEdge(edgesNum, node1, node2, Color.decode("#d8d8d8"), "");
			edges.put(edgesNum,  edge);
		}

		
//
//		pass on
		
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error TBI108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error TBI109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error TBI110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.setTreeModel(null);
    	controler.setNonTreeEdges(null);
	}

	public GraphNode addNode(String label, String detail) { 
		j++;
		int maxVert = 10;
		String newNodeColor;
		String newLine = "\r";
		String topicName = label; 
		newNodeColor = "#ccdddd";
		String verbal = detail;
		topicName = topicName.replace("\r"," ");
		if (topicName.equals(newLine)) topicName = "";
		if (verbal == null || verbal.equals(newLine)) verbal = "";
		if (topicName.isEmpty() && verbal.isEmpty()) return null;
		int id = 100 + j;

		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		return topic;
	}
}

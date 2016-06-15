package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TopicMapLoader {
	Hashtable<Integer, GraphNode> newNodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> newEdges = new Hashtable<Integer, GraphEdge>();
	String dataString = "";
	int topicnum = 0;
	int assocnum = 0;
	Element root;
	boolean readyMap = false;
	int minX = Integer.MAX_VALUE;
	int maxX = Integer.MIN_VALUE;
	int minY = Integer.MAX_VALUE;
	int maxY = Integer.MIN_VALUE;

	public TopicMapLoader(Document doc, GraphPanelControler controler) {
		//
		//	Which type of XML file? 
		String [] knownFormats = {
				"en-export", 
				"(not relevant)", 
				"kgif", 
				"cmap", 
				"BrainData",
				"w:document",
				"topicmap"
				};

		if (doc.hasChildNodes()) {
			root = doc.getDocumentElement();
			
			if (root.getTagName() == "x28map") {
				System.out.println("TL Success: new" );
				readyMap = true;
			} else {
//				boolean known = false;
//				for (int k = 0; k < knownFormats.length; k++) {
//					if (root.getTagName() == knownFormats[k]) {
////						if (compositionMode) controler.getCWInstance().cancel();
//						System.out.println("Format: " + knownFormats[k]);
//						new ImportDirector(k, doc, controler);
//						known = true;
//						return;
//					}
//				}
//				if (!known) {
					System.out.println("TL Failure.");
					return;
//				}
			}
			
			NodeList topics = root.getElementsByTagName("topic");
			NodeList assocs = root.getElementsByTagName("assoc");
			for (int i = 0; i < topics.getLength(); i++) {
				importTopic((Element) topics.item(i));
			}
			for (int i = 0; i < assocs.getLength(); i++) {
				importAssoc((Element) assocs.item(i));
			}
			System.out.println("TL: " + topicnum + " new topics and " + assocnum + " new assocs loaded");
			
			System.out.println("TL recorded " + newNodes.size() + " nodes and " + newEdges.size() + " edges");
			return;
//
//		} else {
//			System.out.println("NS: not XML");
//			
//			{
//				if (inputType != 1) return false;
//				try {
//					stream = new FileInputStream(dataString);
//				} catch (FileNotFoundException e) {
//					System.out.println("NS Error 126 " + e);
//				}
//				dataString = convertStreamToString(stream);
//			}
		}
	}

	//	Detail methods for current map xml format
	//	Nodes & edges are called "topics" and "assocs" to distinguish from xml nodes
	
	private void importTopic(Element topic) {
		GraphNode node;
		topicnum++;

		String id = topic.getAttribute("ID");
		String label = topic.getFirstChild().getTextContent();
		String detail = topic.getLastChild().getTextContent();
		int x = Integer.parseInt(topic.getAttribute("x"));
		int y = Integer.parseInt(topic.getAttribute("y"));
		if (x < minX) minX = x;
		if (x > maxX) maxX = x;
		if (y < minY) minY = y;
		if (y > maxY) maxY = y;
		String color = topic.getAttribute("color");
	
//		System.out.println("NS: id = " + id + ", label = " + label + ", detail has length " + detail.length() +
//				", x = " + x + ", y = " + y + ", color = " + color);

		node = new GraphNode(topicnum, new Point(x,y), Color.decode(color), label, detail);
		newNodes.put(node.getID(), node);
	}

	private void importAssoc(Element assoc) {
		GraphEdge edge;
		assocnum++;

		String detail = assoc.getFirstChild().getTextContent();
		int n1 = Integer.parseInt(assoc.getAttribute("n1"));
		int n2 = Integer.parseInt(assoc.getAttribute("n2"));
		String color = assoc.getAttribute("color");
	
//		System.out.println("NS: detail has length " + detail.length() +
//				", node1 = " + newNodes.get(n1).getLabel() + ", node2 = " + newNodes.get(n2).getLabel() + ", color = " + color);

		edge = new GraphEdge(assocnum, newNodes.get(n1), newNodes.get(n2), Color.decode(color), detail);
		newEdges.put(assocnum, edge);
		newNodes.get(n1).addEdge(edge);
		newNodes.get(n2).addEdge(edge);
	}
}

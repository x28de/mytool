package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.text.html.HTMLEditorKit;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TreeImport {
	
	// Main fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	
	// Auxiliary stuff
	Hashtable<String,String> inputItems = new Hashtable<String,String>();
	Hashtable<String,String> relationships = new Hashtable<String,String>();
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	
	int j = -1;
	int readCount = 0;
	int edgesNum = 0;
	boolean top = true;
	String htmlOut = "";
	
	//	Constants
	int maxVert = 10;
	
	//	Overriden later
	int knownFormat = 10;	//	OPML
	String topNode = "body";
	String nestNode = "outline";
	String labelAttr = "text";
	String idAttr = "";
	
	public TreeImport(Document inputXml, GraphPanelControler controler, int knownFormat) {

		this.knownFormat = knownFormat;
		if (knownFormat == 11) {	//	FreeMind
			topNode = "map";
			nestNode = "node";
			labelAttr = "TEXT";
			idAttr = "ID";
		}
		NodeList graphContainer = inputXml.getElementsByTagName(topNode);
		inputItems.put(topNode, "ROOT");
		addNode(topNode, "");
		Element graph = (Element) graphContainer.item(0);
//		edges.clear();
		
		//	Collect nested nodes
		nest(graph, topNode);
		
		//	Collect relationships
		Enumeration<String> relEnum = relationships.keys();
		while (relEnum.hasMoreElements()) {
			String fromRef = relEnum.nextElement();
			String toRef = relationships.get(fromRef);
			addEdge(fromRef, toRef, true);
		}
		
//		
//	Pass on the new map
	
	System.out.println("TI Map: " + nodes.size() + " " + edges.size());
	try {
		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
	} catch (TransformerConfigurationException e1) {
		System.out.println("Error TI108 " + e1);
	} catch (IOException e1) {
		System.out.println("Error TI109 " + e1);
	} catch (SAXException e1) {
		System.out.println("Error TI110 " + e1);
	}
	
	controler.getNSInstance().setInput(dataString, 2);
	}
	
	public void nest(Node parent, String parentID) {
		
		NodeList children = parent.getChildNodes();
		Node child;
		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			String name = child.getNodeName();
			if (!name.equals(nestNode)) {
				continue;
			}
			
			//	Extract stuff 
			String label = ((Element) child).getAttribute(labelAttr);
			String detail = "";
			String id = "";
			if (knownFormat == 11) {	//	FreeMind
				id = ((Element) child).getAttribute(idAttr);

				//	Notes
				NodeList richContainer = ((Element) child).getElementsByTagName("richcontent");
				if (richContainer.getLength() > 0) detail = richContainer.item(0).getTextContent();
				if (label.isEmpty()) {
					label = filterHTML(detail);
					int len = label.length();
					if (len > 30) label = label.substring(0, 29) + "...";
				}
				
				//	Arrows 
				NodeList arrowCandidates = child.getChildNodes();
				for (int k = 0; k < arrowCandidates.getLength(); k++) {
					Node arrowCandidate = arrowCandidates.item(k);
					String nodeName = arrowCandidate.getNodeName();
					if (nodeName.equals("arrowlink")) {
						String relDesti = ((Element) arrowCandidate).getAttribute("DESTINATION");
						relationships.put(id, relDesti);
					}
				}
			} else {
				id = readCount++ + "";
				detail = ((Element) child).getAttribute("_note");
				detail = detail.replace("\n", " X<br />");
			}
			
			//	add node
			inputItems.put(id, label);
			addNode(id, detail);
			
			//	add link
			addEdge(parentID, id, false);
			
			//	recurse
			nest(child, id);
		}
	}
		
	public void addNode(String nodeRef, String detail) { 
		j++;
		String newNodeColor;
		String newLine = "\r";
		String topicName = ""; 
		topicName = inputItems.get(nodeRef);
		newNodeColor = "#ccdddd";
		String verbal = detail;
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

	public void addEdge(String fromRef, String toRef, boolean xref) {
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
		if (xref) newEdgeColor = "#ffff00";
		edgesNum++;
		if (!inputID2num.containsKey(fromRef)) {
			System.out.println("Error TI101 " + fromRef + ", " + xref);
			return;
		}
		GraphNode sourceNode = nodes.get(inputID2num.get(fromRef));
		if (!inputID2num.containsKey(toRef)) {
			System.out.println("Error TI102 " + toRef);
			return;
		}
		GraphNode targetNode = nodes.get(inputID2num.get(toRef));
		edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), "");
		edges.put(edgesNum, edge);
	}
	
//
//	Accessories to eliminate HTML tags 
//	Duplicate of NewStuff TODO reuse

	private String filterHTML(String html) {
		htmlOut = "";
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				htmlOut = htmlOut + dataString + " ";
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error IM109 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error IM110 " + e3.toString());
		}
		return htmlOut;
	}

	private static class MyHTMLEditorKit extends HTMLEditorKit {
		private static final long serialVersionUID = 7279700400657879527L;

		public Parser getParser() {
			return super.getParser();
		}
	}
}

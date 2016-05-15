package de.x28hd.tool;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DwzImport {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	String dataString = "";
	
	String htmlOut = "";
	boolean ever = false;
	boolean link = false;
	
	private static final String XML_ROOT = "kgif";
	
	GraphPanelControler controler;

	public DwzImport(JFrame mainWindow, GraphPanelControler controler) {
//		File file = new File("C:\\Users\\Matthias\\Desktop\\kgif.xml");
		FileDialog fd = new FileDialog(mainWindow);
		fd.setTitle("Select an DenkWerkZeug KGIF (Knowledge Graph) file");
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		File file = new File(filename);
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error DI101 " + e);
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document dwz = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error DI102 " + e2 );
		}
		
		try {
			dwz = db.parse(fileInputStream);
			
			Element dwzRoot = null;
			dwzRoot = dwz.getDocumentElement();
			if (dwzRoot.getTagName() != XML_ROOT) {
				System.out.println("Error DI105, unexpected: " + dwzRoot.getTagName() );
				fileInputStream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error DI106 " + e1 + "\n" + e1.getClass());
			if (e1.getClass().equals(java.net.UnknownHostException.class))
				controler.displayPopup("Currently, a host is not reachable, and therefore " +
						"the second line of the KGIF file must be removed.");
		} catch (SAXException e) {
			System.out.println("Error DI107 " + e );
		}
		
		int maxVert = 10;
		
		NodeList graphContainer = dwz.getElementsByTagName("graph");

		//	Find DWZ nodes
		NodeList dwzNodes = ((Element) graphContainer.item(0)).getElementsByTagName("node");
		System.out.println("Wieviel gefunden " + dwzNodes.getLength());
		
		Hashtable<String,Integer> dwz2num = new  Hashtable<String,Integer>();
		Hashtable<String,String> typeDict = new Hashtable<String,String>();
		
		for (int i = 0; i < dwzNodes.getLength(); i++) {
			Element node = (Element) dwzNodes.item(i);
			String nodeID = node.getAttribute("id");
			NodeList labelContainer = node.getElementsByTagName("label");
			Element label = (Element) labelContainer.item(0);
			String labelString = label.getTextContent();
			typeDict.put(nodeID, labelString);
			NodeList contentContainer = node.getElementsByTagName("content");
			String content = "";
			int found = contentContainer.getLength();
			if (found > 0) {
				content = contentContainer.item(0).getTextContent().toString();
			}
//			content = filterHTML(content);
			
			// Create MyTool nodes
			int j = i;
			String newNodeColor = "#ccdddd";
			String newLine = "\r";
			String topicName = labelString;
			String verbal = content;
			if (topicName.equals(newLine)) topicName = "";
			if (verbal == null || verbal.equals(newLine)) verbal = "";
			if (!verbal.isEmpty()) newNodeColor = "#ffdddd";
			if (topicName.isEmpty() && verbal.isEmpty()) continue;
			int id = 100 + j;
			
			int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			int x = 40 + (j/maxVert) * 150;
			Point p = new Point(x, y);
			GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	
			

			nodes.put(id, topic);
			dwz2num.put(nodeID, id);
			System.out.println("Merke " + labelString + " " + nodeID + " -> " + id + " ");
		}
		
		//	Rearrange nodes in a circle
//		int nodesTotal = nodes.size();
//
//		Enumeration<GraphNode> nodes2 = nodes.elements();
		int i = 0;
//		while (nodes2.hasMoreElements()) {
//			GraphNode node = nodes2.nextElement();
//			Point p = circleEvenly(i, nodesTotal, new Point(10, 10));
//			node.setXY(p);
//			i++;
//		}
		
		//	Find DWZ links
		
		NodeList dwzLinks = ((Element) graphContainer.item(0)).getElementsByTagName("link");
		System.out.println("Wieviel gefunden " + dwzNodes.getLength());
		int edgesNum = 0;
		
		for (i = 0; i < dwzLinks.getLength(); i++) {

			Element link = (Element) dwzLinks.item(i);
			String linkID = link.getAttribute("id");
			String fromNode = link.getAttribute("from");
			String toNode = link.getAttribute("to");
			String linkType = link.getAttribute("type");
			String linkLabel = typeDict.get(linkType);
			System.out.println(fromNode + " " + linkLabel + " " + toNode);

			// Generate edges
			edgesNum++;
			int sourceNodeNum = dwz2num.get(fromNode);
			int	targetNodeNum = dwz2num.get(toNode);
			GraphNode sourceNode = nodes.get(sourceNodeNum);
			GraphNode targetNode = nodes.get(targetNodeNum);
			GraphEdge edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode("#c0c0c0"), linkLabel);
			edges.put(edgesNum, edge);
			sourceNode.addEdge(edge);
			targetNode.addEdge(edge);
		}
		
		Enumeration<GraphNode> nodes2 = nodes.elements();
		i = 0;
		while (nodes2.hasMoreElements()) {
			GraphNode node = nodes2.nextElement();
			Enumeration<GraphEdge> neighbors = node.getEdges();
			int count = 0;
			while (neighbors.hasMoreElements()) {
				GraphEdge dummy = neighbors.nextElement();
				count++;
				System.out.println("Neighbors: " + count);
			}
			if (count > 3) {
				Point prelim = node.getXY();
				int x = prelim.x;
				int y = prelim.y;
				Point adjusted = new Point(x + 30, y);
				node.setXY(adjusted);
			}
			i++;
		}
		
		
		//	Pass on
		
//		edges.clear();
		try {
			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error DI108 " + e1);
		} catch (IOException e1) {
			System.out.println("Error DI109 " + e1);
		} catch (SAXException e1) {
			System.out.println("Error DI110 " + e1);
		}
		controler.getNSInstance().setInput(dataString, 2);
	}
	
//
//	Accessories to filter HTML tag "en-note"
//	Duplicate of NewStuff TODO reuse

	private String filterHTML(String html) {
		htmlOut = "";
		ever = false;
		link = false;

		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == Tag.A) {
					String address = (String) a.getAttribute(Attribute.HREF);
					link = true;
					System.out.println("Link " + address);
					htmlOut = htmlOut + "<a href=\"" + address + "\">" + address + "</a> ";
				}
			}
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				if (ever && !link) {
					htmlOut = htmlOut + dataString + " ";
				} else {
					System.out.println("DataString: " + dataString);
				}
			}
			public void handleEndTag(HTML.Tag t, int pos) {
				System.out.println("</" + t + "> on pos " + pos);
				if (t.toString() == "a") {
					link = false;
				}
			}
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				System.out.println("Simple <" + t + "> on pos " + pos);
				if (t.toString().equals("en-note")) {
					ever = !ever;
					System.out.println("Switched to " + ever);
				}
				if (t.toString().equals("en-media")) {
					htmlOut = "(Media cannot yet be displayed)";
					System.out.println("Media");
					return;
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error DI109 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error DI110 " + e3.toString());
		}
		return htmlOut;
	}

	private static class MyHTMLEditorKit extends HTMLEditorKit {
		private static final long serialVersionUID = 7279700400657879527L;

		public Parser getParser() {
			return super.getParser();
		}
	}
	
	public Point circleEvenly(int item, int total, Point corner) {
		int radius = total * 5;
		double incr = 360.0 / total;
			double x = radius * Math.cos((incr * item) * (Math.PI / 180));					
			double y = radius * Math.sin((incr * item) * (Math.PI / 180));					
			int xInt = (int) Math.round(x) + corner.x - radius/2;
			int yInt = (int) Math.round(y) + corner.y - radius/2;
		return new Point(xInt, yInt);
	}
}

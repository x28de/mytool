package de.x28hd.tool.importers;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.MyHTMLEditorKit;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;

public class ImportWXR {
	Hashtable<Integer, GraphNode> nodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> edges = new Hashtable<Integer, GraphEdge>();
	PresentationService controler;
	String htmlOut = "";
	int count = 0;
	String url = "";
	Hashtable<String, GraphNode> urls = new Hashtable<String, GraphNode>();
	TreeMap<String, GraphNode> urlMap = new TreeMap<String, GraphNode>();
	SortedMap<String,GraphNode> urlList = (SortedMap<String, GraphNode>) urlMap;
	HashSet<String> uniqEdges = new HashSet<String>();
	int j = 0;
	int edgesNum = 0;
	String blogName;
	boolean defaultPerma = false;
	boolean fallback;

	public ImportWXR(Document inputXml, PresentationService controler, boolean fallback) {
		this.fallback = fallback;

		NodeList itemContainer = inputXml.getElementsByTagName("channel");
		Node blogNode = ((Element) itemContainer.item(0)).getElementsByTagName("link").item(0);
		blogName = blogNode.getTextContent();
		System.out.println(blogName.substring(8));
		NodeList itemList = ((Element) itemContainer.item(0)).getElementsByTagName("item");
		for (int i = 0; i < itemList.getLength(); i++) {
			Element node = (Element) itemList.item(i);
			Element typeElem = (Element) node.getElementsByTagName("wp:post_type").item(0);
			String typeString = typeElem.getTextContent();
			if (!typeString.equals("post")) continue; 
			Element statusElem = (Element) node.getElementsByTagName("wp:status").item(0);
			String statusString = statusElem.getTextContent();
			if (!statusString.equals("publish")) continue; 
			
			Element titleElem = (Element) node.getElementsByTagName("title").item(0);
			String title = titleElem.getTextContent();
			Element urlElem = (Element) node.getElementsByTagName("link").item(0);
			url = urlElem.getTextContent();
			Element contentElem = (Element) node.getElementsByTagName("content:encoded").item(0);
			String content = contentElem.getTextContent();
			filterHTML(content);	// extracts links
			
			GraphNode thisNode = null;
			if (!urls.containsKey(url)) {
				thisNode = addNode(url);
			} else {
				thisNode = urls.get(url);
			}
			String detail = "<a href=\"" + url + "\">" + title + "</a>.";
			thisNode.setDetail(detail);
			thisNode.setLabel(title);
		}
		if (!defaultPerma && !fallback) {
			controler.displayPopup("No permalinks with default format "
					+ "(Day and Name) found; \ncannot identify posts, trying fallback.");
			new ImportWXR(inputXml, controler, true);
			return;
		}

		
		int i = 0;
		SortedSet<String> urlSet = (SortedSet<String>) urlList.keySet();
		Iterator<String> iter = urlSet.iterator();
		while (iter.hasNext()) {
			i++;
			String url = iter.next();
			GraphNode node = urls.get(url);
//			System.out.println(node.getLabel());
			Point xy = new Point(-20 * i, 20 * i);
			node.setXY(xy);
		}
		
		
		// Pass on
	    String dataString = "";
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error IW108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error IW109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error IW110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.getControlerExtras().setTreeModel(null);
    	controler.getControlerExtras().setNonTreeEdges(null);
	}
	
	private String filterHTML(String html) {
		htmlOut = "";
		
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.A) {
					if (!a.isDefined(HTML.Attribute.HREF)) return;

					Object href = ((AttributeSet) a).getAttribute(HTML.Attribute.HREF);
					String h = href.toString();
					h = h.replaceAll("\"", "");
					h = h.replaceAll("http://" + blogName.substring(8), "https://" + blogName.substring(8));
					if (h.startsWith("../")) h = blogName + h.substring(3);
					if (h.contains("#")) return;
					if (!h.endsWith("/") && h.contains(blogName.substring(8))) 
						h = h + "/";
					if (h.startsWith(blogName + "/20")) {
						addLink(url, h);
						defaultPerma = true;
					}
					if (fallback && h.startsWith(blogName)) addLink(url, h);
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error IW128 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error IW129 " + e3.toString());
		}
		return htmlOut;
	}

	public GraphNode addNode(String fullLabel) {
		GraphNode node;
		if (!urls.containsKey(fullLabel)) {
			j++;
			int id = j + 100;
			String label = fullLabel;
			if (label.length() > 44) label = label.substring(44);
			node = new GraphNode(id, new Point(-20 * j, 20 * j), Color.decode("#ccdddd"), 
					label, "<a href=\"" + fullLabel + "\">" + label + ".</a>");
			nodes.put(id, node);
			urls.put(fullLabel, node);
			urlMap.put(fullLabel, node);
		} else {
			node = urls.get(fullLabel);
		}
		return node;
	}
	
	public void addLink(String from, String to) {
		GraphNode node1;
		GraphNode node2;
		node1 = addNode(from);
		node2 = addNode(to);
		edgesNum++;
		
		int n1 = node1.getID();
		int n2 = node2.getID();
		String uniq = (n1 < n2) ? n1 + "-" + n2 : n2 + "-" + n1;
		if (uniqEdges.contains(uniq)) {
//			System.out.println("Duplicate " + node1.getLabel() + " -> " + node2.getLabel());
			return;
		}
		GraphEdge edge = new GraphEdge(edgesNum, node1, node2, Color.decode("#c0c0c0"), "");
		node1.addEdge(edge);
		node2.addEdge(edge);
		edges.put(edgesNum, edge);
		uniqEdges.add(uniq);
	}
}

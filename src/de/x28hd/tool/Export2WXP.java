package de.x28hd.tool;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;


public class Export2WXP {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	String basedir;
	Hashtable<Integer,Integer> nodeids = new Hashtable<Integer,Integer>();;
	int nodenum = 0;
	String commentString = "\nThis should work like genuine WP but was generated from http://x28hd.de/tool/ \n";
	char[] commentChars = commentString.toCharArray();
	
	public Export2WXP(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}
	
	public File createTopicmapFile(String filename) throws IOException, TransformerConfigurationException, SAXException {
		File topicmapFile = new File(filename);
//			System.out.println(topicmapFile.getCanonicalPath());
		FileOutputStream out = new FileOutputStream(topicmapFile);
		exportViewMode(out, filename);
		out.close();
		return topicmapFile;
	}

//	public String createTopicmapString() throws IOException, TransformerConfigurationException, SAXException {
//		ByteArrayOutputStream out = new ByteArrayOutputStream(99999);
//		exportViewMode(out);
//		String topicmapString = out.toString();
//		out.close();
//		return topicmapString;
//	}

	private void exportViewMode(FileOutputStream out, String filename) throws TransformerConfigurationException, SAXException {
		SAXTransformerFactory saxTFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
		TransformerHandler handler = null;
		handler = saxTFactory.newTransformerHandler();
		handler.setResult(new StreamResult(out));
		handler.startDocument();
		handler.comment(commentChars, 0, commentChars.length);
		Hashtable<String, String> rootAttribs = new Hashtable<String, String>();
		rootAttribs.put("version", "2.0");
		rootAttribs.put("xmlns:excerpt", "http://wordpress.org/export/1.2/excerpt/");
		rootAttribs.put("xmlns:content", "http://purl.org/rss/1.0/modules/content/");
		rootAttribs.put("xmlns:wfw", "http://wellformedweb.org/CommentAPI/");
		rootAttribs.put("xmlns:dc", "http://purl.org/dc/elements/1.1/");
		rootAttribs.put("xmlns:wp", "http://wordpress.org/export/1.2/");
		startElement(handler, "rss", rootAttribs);
		startElement(handler, "channel", null);

		textNode(handler, "title", "Dummy Title");
		textNode(handler, "link", "http://127.0.0.1/wordpress");
		textNode(handler, "description", "Just another WordPress site");
		textNode(handler, "pubDate", "Mon, 21 Ju 2014 19:42:13 +0000");
		textNode(handler, "language", "en-US");
		textNode(handler, "wp:wxr_version", "1.2");
		textNode(handler, "wp:base_site_url", "http://127.0.0.1/wordpress");
		textNode(handler, "wp:blog_site_url", "http://127.0.0.1/wordpress");

		startElement(handler, "wp_author", null);
		textNode(handler, "wp_author_id", "1");
		textNode(handler, "wp_author_login", "admin");
		textNode(handler, "wp_author_email", "noreply@example.com");
		startElement(handler, "wp_author_display_name", null);
		characters(handler, "Admin User");
		endElement(handler, "wp_author_display_name");
		startElement(handler, "wp_author_first_name", null);
		characters(handler, "Admin");
		endElement(handler, "wp_author_first_name");
		startElement(handler, "wp_author_last_name", null);
		characters(handler, "Admin");
		endElement(handler, "wp_author_last_name");
		endElement(handler, "wp_author");

		textNode(handler, "generator", "http://wordpress.org/?v=3.7.3");
		
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		exportTopics(nodesEnum, handler, true);
//		Enumeration<GraphEdge> edgesEnum = edges.elements();
//		exportAssociations(edgesEnum, handler, true);

		endElement(handler, "channel");
		endElement(handler, "rss");
		handler.endDocument();	
	}

//	private void exportViewMode(ByteArrayOutputStream out) throws TransformerConfigurationException, SAXException {
//		SAXTransformerFactory saxTFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
//		TransformerHandler handler = null;
//		handler = saxTFactory.newTransformerHandler();
//		handler.setResult(new StreamResult(out));
//		handler.startDocument();
//		startElement(handler, "x28map", null);
//		handler.comment(commentChars, 0, commentChars.length);
//
//		Enumeration<GraphNode> nodesEnum = nodes.elements();
//		exportTopics(nodesEnum, handler, true);
//		Enumeration<GraphEdge> edgesEnum = edges.elements();
//		exportAssociations(edgesEnum, handler, true);
//		endElement(handler, "x28map");
//		handler.endDocument();	
//	}

	public void exportTopics(Enumeration<GraphNode> topics, ContentHandler handler, boolean visible) throws SAXException {
		while (topics.hasMoreElements()) {
			GraphNode topic = topics.nextElement();
			exportTopic(topic, handler, visible);
		}

	}
//	public void exportAssociations(Enumeration<GraphEdge> assocs, ContentHandler handler, boolean visible) throws SAXException {
//		while (assocs.hasMoreElements()) {
//			GraphEdge assoc = assocs.nextElement();
//
//			GraphNode topic1 = assoc.getNode1();
//			if (!nodes.contains(topic1)) {
//				System.out.println("Nicht OK " + topic1.getLabel());
//				continue;
//			} else System.out.println("OK: " + topic1.getLabel());
//			GraphNode topic2 = assoc.getNode2();
//			if (!nodes.contains(topic2)) continue;
//			
//			exportAsssociation(assoc, handler, visible);
//		}
//	}

	private void exportTopic(GraphNode topic, ContentHandler handler, boolean visible) throws SAXException {
	Hashtable<String, String> attribs = new Hashtable<String, String>();

	nodenum++;

	// --- generate item element ---
	startElement(handler, "item", attribs);
	textNode((TransformerHandler) handler, "title",  topic.getLabel());
	startElement(handler, "dc:creator", null);
	characters(handler, "Admin User");
	endElement(handler, "dc:creator");
	startElement(handler, "content:encoded", null);
	characters(handler, topic.getDetail());
	endElement(handler, "content:encoded");
	textNode((TransformerHandler) handler, "wp:post_type",  "post");
	textNode((TransformerHandler) handler, "wp:status",  "publish");
	endElement(handler, "item");
	}

//	public void exportAsssociation(GraphEdge assoc, ContentHandler handler, boolean visible) throws SAXException {
//		Hashtable<String, String> attribs = new Hashtable<String, String>();
//		Color color = assoc.getColor();
//		int r = color.getRed();
//		int g = color.getGreen();
//		int b = color.getBlue();
//		attribs.put("color", String.format("#%02x%02x%02x", r, g, b));
//		int n1 = nodeids.get(assoc.getN1());
//		int n2 = nodeids.get(assoc.getN2());
//		attribs.put("n1", "" + n1);
//		attribs.put("n2", "" + n2);
//
//		startElement(handler, "assoc", attribs);
//		startElement(handler, "detail", null);
//		characters(handler, assoc.getDetail());
//		endElement(handler, "detail");
//
//		endElement(handler, "assoc");
//	}

//
//  Accessories for CDATA, start, and end
	public static void characters(ContentHandler handler, String string) throws SAXException {
		char[] chars = string.toCharArray();
		if (handler instanceof LexicalHandler) {
			((LexicalHandler) handler).startCDATA();
			handler.characters(chars, 0, chars.length);
			((LexicalHandler) handler).endCDATA();
		} else {
			handler.characters(chars, 0, chars.length);
		}
	}

	public static void startElement(ContentHandler handler, String tagName,
			Hashtable attributes) throws SAXException {
		AttributesImpl attrs = new AttributesImpl();
		if (attributes != null) {
			Enumeration attribKeys = attributes.keys();
			while(attribKeys.hasMoreElements()) {
				String key = attribKeys.nextElement().toString();
				String value = attributes.get(key).toString();
				attrs.addAttribute("", "", key, "CDATA", value);
			}
		}
		handler.startElement("", "", tagName, attrs);
	}

	public static void endElement(ContentHandler handler, String tagName) throws SAXException {
		handler.endElement("", "", tagName);
	}
	
	public void textNode(TransformerHandler handler, String tag, String content) throws SAXException {
		startElement(handler, tag, null);
		String textNode = content;
		char[] chars = textNode.toCharArray();
		handler.characters(chars, 0, chars.length);
		endElement(handler, tag);
	}

	public static void createDirectory(File file) {
		File dstDir = file.getParentFile();
		if (dstDir.mkdirs()) {
			System.out.println(">>> document repository has been created: " + dstDir);
		}
	}
	

}

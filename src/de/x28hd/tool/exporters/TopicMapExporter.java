package de.x28hd.tool.exporters;

import java.awt.Color;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class TopicMapExporter {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	String commentString = "\nThis is not for human readers but for http://x28hd.de/tool/ \n";
	char[] commentChars = commentString.toCharArray();
	
	public TopicMapExporter(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}
	
	public void createTopicmapArchive(String zipFilename) {
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(zipFilename);
		} catch (FileNotFoundException e) {
			System.out.println("Error TX127 " + e);
		}
		ZipOutputStream zout = new ZipOutputStream(fout);
		exportViewMode(zout);
		try {
			zout.close();
		} catch (IOException e) {
			System.out.println("Error TX127a " + e);
		}
	}
	
	private void exportViewMode(ZipOutputStream out) {
		try {
			out.putNextEntry(new ZipEntry("savefile.xml"));

			SAXTransformerFactory saxTFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
			TransformerHandler handler = saxTFactory.newTransformerHandler();
			handler.setResult(new StreamResult(out));

			handler.startDocument();
			handler.comment(commentChars, 0, commentChars.length);
			startElement(handler, "topicmap", null);
			
			Enumeration<GraphNode> nodesEnum = nodes.elements();
			exportTopics(nodesEnum, handler);
			Enumeration<GraphEdge> edgesEnum = edges.elements();
			exportAssociations(edgesEnum, handler);
			//
			endElement(handler, "topicmap");
			handler.endDocument();
		} catch (Throwable e) {
			System.out.println("Error TX101 " + e);
		}
	}

	public void exportTopics(Enumeration<GraphNode> topics, ContentHandler handler) throws SAXException {
		while (topics.hasMoreElements()) {
			GraphNode topic = topics.nextElement();
			exportTopic(topic, handler);
		}

	}
	public void exportAssociations(Enumeration<GraphEdge> assocs, ContentHandler handler) throws SAXException {
		while (assocs.hasMoreElements()) {
			GraphEdge assoc = assocs.nextElement();
			exportAsssociation(assoc, handler);
		}
	}

	private void exportTopic(GraphNode topic, ContentHandler handler) throws SAXException {
	Hashtable<String, String> attribs = new Hashtable<String, String>();

	attribs.put("ID", topic.getID() + "");
	attribs.put("visible", "yes");

	Color color = topic.getColor();
	attribs.put("r", new Integer(color.getRed()) + "");
	attribs.put("g", new Integer(color.getGreen()) + "");
	attribs.put("b", new Integer(color.getBlue()) + "");

	// geometry 
	Point p = topic.getXY();
	attribs.put("x", new Integer(p.x) + "");
	attribs.put("y", new Integer(p.y) + "");

	// --- generate topic element ---
	startElement(handler, "topic", attribs);
	startElement(handler, "topname", null);
	startElement(handler, "basename", null);
	characters(handler, topic.getLabel());
	endElement(handler, "basename");
	endElement(handler, "topname");
	startElement(handler, "description", null);
	characters(handler, topic.getDetail());
	endElement(handler, "description");
	endElement(handler, "topic");
	}

	public void exportAsssociation(GraphEdge assoc, ContentHandler handler) throws SAXException {
		Hashtable<String, String> attribs = new Hashtable<String, String>();
		attribs.put("ID", assoc.getID() + "");
		attribs.put("visible", "yes");
		Color color = assoc.getColor();
		attribs.put("r", new Integer(color.getRed()) + "");
		attribs.put("g", new Integer(color.getGreen()) + "");
		attribs.put("b", new Integer(color.getBlue()) + "");
		int n1 = assoc.getN1();
		int n2 = assoc.getN2();
		attribs.put("n1", "" + n1);
		attribs.put("n2", "" + n2);

		startElement(handler, "assoc", attribs);
		startElement(handler, "description", null);
		characters(handler, assoc.getDetail());
		endElement(handler, "description");

		attribs = new Hashtable<String, String>();
		attribs.put("anchrole", "tt-topic1");
		startElement(handler, "assocrl", attribs);
		characters(handler, assoc.getN1() + "");
		endElement(handler, "assocrl");
		attribs.put("anchrole", "tt-topic2");
		startElement(handler, "assocrl", attribs);
		characters(handler, assoc.getN2() + "");
		endElement(handler, "assocrl");
		endElement(handler, "assoc");
	}

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
			Hashtable<String, String> attributes) throws SAXException {
		AttributesImpl attrs = new AttributesImpl();
		if (attributes != null) {
			Enumeration<String> attribKeys = attributes.keys();
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

}
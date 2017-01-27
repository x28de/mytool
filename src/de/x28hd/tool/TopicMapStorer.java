package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;


public class TopicMapStorer {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	String basedir;
	String htmlOut = "";
	Hashtable<Integer,Integer> nodeids = new Hashtable<Integer,Integer>();;
	int nodenum = 0;
	String commentString = "\nThis is not for human readers but for http://x28hd.de/tool/ \n";
	char[] commentChars = commentString.toCharArray();
	boolean anonymized = false;
	
	public TopicMapStorer(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}
	
	public File createTopicmapFile(String filename, boolean special) 
			throws IOException, TransformerConfigurationException, SAXException {
		anonymized = special;
		File topicmapFile = new File(filename);
		FileOutputStream out = new FileOutputStream(topicmapFile);
		exportViewMode(out, filename);
		out.close();
		return topicmapFile;
	}

	public String createTopicmapString() throws IOException, TransformerConfigurationException, SAXException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(99999);
		exportViewMode(out);
		String topicmapString = out.toString();
		out.close();
		return topicmapString;
	}

	private void exportViewMode(FileOutputStream out, String filename) throws TransformerConfigurationException, SAXException {
		SAXTransformerFactory saxTFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
		TransformerHandler handler = null;
		handler = saxTFactory.newTransformerHandler();
		Transformer transformer = handler.getTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
//		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		handler.setResult(new StreamResult(out));
		handler.startDocument();
		handler.comment(commentChars, 0, commentChars.length);
		startElement(handler, "x28map", null);
		
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		exportTopics(nodesEnum, handler, true);
		Enumeration<GraphEdge> edgesEnum = edges.elements();
		exportAssociations(edgesEnum, handler, true);
		endElement(handler, "x28map");
		handler.endDocument();	
	}

	private void exportViewMode(ByteArrayOutputStream out) throws TransformerConfigurationException, SAXException {
		SAXTransformerFactory saxTFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
		TransformerHandler handler = null;
		handler = saxTFactory.newTransformerHandler();
		handler.setResult(new StreamResult(out));
		handler.startDocument();
		startElement(handler, "x28map", null);
		handler.comment(commentChars, 0, commentChars.length);

		Enumeration<GraphNode> nodesEnum = nodes.elements();
		exportTopics(nodesEnum, handler, true);
		Enumeration<GraphEdge> edgesEnum = edges.elements();
		exportAssociations(edgesEnum, handler, true);
		endElement(handler, "x28map");
		handler.endDocument();	
	}

	public void exportTopics(Enumeration<GraphNode> topics, ContentHandler handler, boolean visible) throws SAXException {
		while (topics.hasMoreElements()) {
			GraphNode topic = topics.nextElement();
			exportTopic(topic, handler, visible);
		}

	}
	public void exportAssociations(Enumeration<GraphEdge> assocs, ContentHandler handler, boolean visible) throws SAXException {
		while (assocs.hasMoreElements()) {
			GraphEdge assoc = assocs.nextElement();

			GraphNode topic1 = assoc.getNode1();
			if (!nodes.contains(topic1)) {
				continue;
			}
			GraphNode topic2 = assoc.getNode2();
			if (!nodes.contains(topic2)) continue;
			
			exportAsssociation(assoc, handler, visible);
		}
	}

	private void exportTopic(GraphNode topic, ContentHandler handler, boolean visible) throws SAXException {
	Hashtable<String, String> attribs = new Hashtable<String, String>();

	nodenum++;
	attribs.put("ID", "" + nodenum);
	nodeids.put(topic.getID(), nodenum);

	Color color = topic.getColor();
	int r = color.getRed();
	int g = color.getGreen();
	int b = color.getBlue();
	attribs.put("color", String.format("#%02x%02x%02x", r, g, b));

	// geometry 
	if (visible) {
		Point p = topic.getXY();
		attribs.put("x", "" + p.x);
		attribs.put("y", "" + p.y);
	}

	// --- generate topic element ---
	startElement(handler, "topic", attribs);
	startElement(handler, "label", null);
	String labelString = topic.getLabel();
	if (anonymized) {
		labelString = labelString.replaceAll("[a-z]","x");
		labelString = labelString.replaceAll("[A-Z]","X");
	}
	characters(handler, labelString);
	endElement(handler, "label");
	startElement(handler, "detail", null);
	String detailString = topic.getDetail();
	if (anonymized) detailString = filterHTML(detailString);
	characters(handler, detailString);
	endElement(handler, "detail");
	endElement(handler, "topic");
	}

	public void exportAsssociation(GraphEdge assoc, ContentHandler handler, boolean visible) throws SAXException {
		Hashtable<String, String> attribs = new Hashtable<String, String>();
		Color color = assoc.getColor();
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		attribs.put("color", String.format("#%02x%02x%02x", r, g, b));
		int n1 = nodeids.get(assoc.getN1());
		int n2 = nodeids.get(assoc.getN2());
		attribs.put("n1", "" + n1);
		attribs.put("n2", "" + n2);

		startElement(handler, "assoc", attribs);
		startElement(handler, "detail", null);
		String detailString = assoc.getDetail();
		if (anonymized) detailString = "";
		characters(handler, detailString);
		endElement(handler, "detail");

		endElement(handler, "assoc");
	}

//
//  Accessories for CDATA, start, and end
	public static void characters(ContentHandler handler, String string) throws SAXException {
		if (string == null) return;
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

	public static void createDirectory(File file) {
		File dstDir = file.getParentFile();
		if (dstDir.mkdirs()) {
//			System.out.println(">>> document repository has been created: " + dstDir);
		}
	}
	
	//	Accessory for anonymize
	
	private String filterHTML(String html) {
		htmlOut = "";
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				dataString = dataString.replaceAll("[a-z]","x");
				dataString = dataString.replaceAll("[A-Z]","X");
				htmlOut = htmlOut + dataString + " ";
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t.equals(HTML.Tag.B)) htmlOut = htmlOut + "<b>";
			}
			public void handleEndTag(HTML.Tag t, int pos) {
				if (t.equals(HTML.Tag.B)) htmlOut = htmlOut + "</b>";
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error ZE119 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error ZE120 " + e3.toString());
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
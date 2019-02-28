package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;


public class Export2WXR implements ActionListener{
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	String basedir;
	Hashtable<Integer,Integer> nodeids = new Hashtable<Integer,Integer>();;
	int nodenum = 0;
	String commentString = "\nThis should work like genuine WP but was generated from http://x28hd.de/tool/ \n";
	char[] commentChars = commentString.toCharArray();
	
	JDialog dialog;
	HashSet<Color> catColorsChoice = new HashSet<Color>();
	Hashtable<Color,JCheckBox> colorSelections = new Hashtable<Color,JCheckBox>();
	Hashtable<Color,Integer> colorCounts = new Hashtable<Color,Integer>();
	JButton continueButton = new JButton("Continue");
	HashSet<Color> catColors = new HashSet<Color>();
	Hashtable<GraphNode,String> nicenames = new Hashtable<GraphNode,String>();
	String dateString = "";
	SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	String dir = "";
	
	public Export2WXR(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
		
		// Ask for category colors
		
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		while (nodesEnum.hasMoreElements()) {
			Color color = nodesEnum.nextElement().getColor();
			if (!catColorsChoice.contains(color)) {
				colorCounts.put(color, 1);
			} else {
				int count = colorCounts.get(color);
				count++;
				colorCounts.put(color, count);
			}
			catColorsChoice.add(color);
		}
		dialog = new JDialog();
		dialog.setModal(true);
		dialog.setTitle("Options");
		if (System.getProperty("os.name").startsWith("Windows")) {
			dialog.setMinimumSize(new Dimension(596, 417));
		} else {
			dialog.setMinimumSize(new Dimension(796, 417));
		}
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setLocation(dim.width/2-dialog.getSize().width/2 - 298, 
				dim.height/2-dialog.getSize().height/2 - 209);		
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel panel = new JPanel();
        JScrollPane outerPanel = new JScrollPane(panel);
		panel.setLayout(new GridLayout(0, 1));
		outerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		panel.add(new JLabel("(Optional:) Which color are your categories?"));
		
		Iterator<Color> iter = (Iterator<Color>) catColorsChoice.iterator();
		while (iter.hasNext()) {
			Color color = iter.next();
			JCheckBox box = new JCheckBox("" + colorCounts.get(color));
			colorSelections.put(color, box);
			JPanel outerBox = new JPanel();
			box.setBackground(color);
			outerBox.add(box);
			panel.add(outerBox);
		}
		dialog.add(outerPanel);
		JPanel toolbar = new JPanel(new BorderLayout());
		toolbar.add(continueButton, "East");
		toolbar.setBorder(new EmptyBorder(10, 10, 10, 10));
		continueButton.addActionListener(this);
		dialog.add(toolbar, "South");
		
		dialog.setVisible(true);
	}
	
	public File createTopicmapFile(String filename, String dir) throws IOException, TransformerConfigurationException, SAXException {
		File topicmapFile = new File(filename);
		this.dir = dir;
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
//		SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Date date = new Date();
		while (topics.hasMoreElements()) {
			// Increment date to keep sorting enabled
			Long dateLong = date.getTime();
			dateLong++;
			date = new Date(dateLong);
			dateString = df2.format(date);
			
			GraphNode topic = topics.nextElement();
			Color seeIfCat = topic.getColor();
			if (catColors.contains(seeIfCat)) continue;
			exportTopic(topic, handler, visible);
		}

	}

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
	String det = topic.getDetail();
	det = det.replaceAll("\n", "");
	det = det.replaceAll("<br>", "<br />");	// If detail was touched by edi.getText()
	
	det = det.replaceAll("<html>", "");	// TODO get rid of this Java kind of html encoding
	det = det.replaceAll("</html>", "");
	det = det.replaceAll("<head>", "");
	det = det.replaceAll("</head>", "");
	det = det.replaceAll("<body>", "");
	det = det.replaceAll("</body>", "");
	det = det.replaceAll("^ +", "");
	characters(handler, det);
	endElement(handler, "content:encoded");
	textNode((TransformerHandler) handler, "wp:post_type",  "post");
	textNode((TransformerHandler) handler, "wp:status",  "publish");
	
	if (nicenames.containsKey(topic)) {
	String nicename = nicenames.get(topic);
	nicename = nicename.toLowerCase();
	nicename = nicename.replaceAll(" ", "-");
	attribs.put("nicename", nicename);
	attribs.put("domain", "category");
	startElement(handler, "category", attribs);
	characters(handler, nicename);
	endElement(handler, "category");
	}
	attribs.clear();
	startElement(handler, "wp:post_date", attribs);
	//  Dirty hack tailored to my own use:
	//	Try to look up mod date for items named like files in the same folder as WXR
	File f = new File(dir + File.separator + topic.getLabel());
	if (f.exists()) {
		Long dateLong = f.lastModified();
		dateString = df2.format(dateLong);
	}
	System.out.println(dateString);
	characters(handler, dateString);
	endElement(handler, "wp:post_date");
	endElement(handler, "item");
	
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

	public void actionPerformed(ActionEvent arg0) {
		Iterator<Color> iter = (Iterator<Color>) catColorsChoice.iterator();
		while (iter.hasNext()) {
			Color color = iter.next();
			JCheckBox box = colorSelections.get(color);
			if (box.isSelected()) catColors.add(color);
		}
		Iterator<Color> iter2 = (Iterator<Color>) catColors.iterator();
		while (iter2.hasNext()) {
			Color color = iter2.next();
			System.out.println(color);
		}
		dialog.dispose();
		
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Color color = node.getColor();
			if (catColors.contains(color)) continue;
			Enumeration<GraphEdge> neighbors = node.getEdges();
			while (neighbors.hasMoreElements()) {
				GraphEdge edge = neighbors.nextElement();
				GraphNode otherEnd = node.relatedNode(edge);
				Color otherColor = otherEnd.getColor();
				if (!catColors.contains(otherColor)) continue; // TODO insert hyperlink?
				nicenames.put(node, otherEnd.getLabel());
			}
		}
	}
	

}

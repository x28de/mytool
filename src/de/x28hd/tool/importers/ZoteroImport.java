package de.x28hd.tool.importers;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Hashtable;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML.Attribute;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.MyHTMLEditorKit;
import de.x28hd.tool.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;


public class ZoteroImport {
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	PresentationService controler;
	String dataString = "";
	int j = 0;
	int maxVert = 10;
	int edgeNum = 0;

	String htmlOut = "";
	boolean passThru = false;
	boolean skip = false;
	String zotClass = "";
	int level = 0;
	boolean headerSwitch = false;
	boolean notesSwitch = false;
	boolean attachmentsSwitch = false;
	String headerString = "";
	GraphNode lastMajorNode;
	GraphNode lastMinorNode;

	public ZoteroImport(File file, PresentationService controler) {
		
		InputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (final Exception e) {
			System.out.println("Error ZI102 " + e);
		}
		Utilities utilities = new Utilities();
		String inputString = utilities.convertStreamToString(fileInputStream);	

		filterHTML(inputString);
		
//
//		pass on
		
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error ZI108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error ZI109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error ZI110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.getControlerExtras().setTreeModel(null);
    	controler.getControlerExtras().setNonTreeEdges(null);
	}
	
	public GraphNode addNode(String label, String detail, boolean publication) {
		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 200;
		j++;

		int id = j + 100;
		String colorString = "#ccdddd";
		if (publication) {
			colorString = "#808080";
//			x = x - 10;
		}
		Point p = new Point(x, y);
		if (label.length() > 30) label = label.substring(0, 30);
		GraphNode topic = new GraphNode (id, p, Color.decode(colorString), label, detail);	

		nodes.put(id, topic);
		return topic;
	}
	
	private void filterHTML(String html) {
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.H2) {
					level = 1;
					if (!htmlOut.isEmpty()) {
						lastMajorNode = addNode(headerString, htmlOut, true);
						htmlOut = "";
					}
					passThru = true;
					headerSwitch = true;
				} else if (t == HTML.Tag.H3) {
					level = 2;
					zotClass = (String) a.getAttribute(Attribute.CLASS);
					if (zotClass.equals("notes")) {
						passThru = false;
						skip = true;	// just skip the heading
						lastMajorNode = addNode(headerString, htmlOut, true);
						htmlOut = "";
					} else if (zotClass.equals("attachments")) {
						attachmentsSwitch = true;
					} else if (zotClass.equals("related")) {
						attachmentsSwitch = true;
					}
				} else if (t == HTML.Tag.TITLE) {
					skip = true;
				} else if (t == HTML.Tag.STRONG) {	// "Extracted Annotations"
					skip = true;
				} else if (t == HTML.Tag.P) {
//					notesSwitch = true;
					notesSwitch = false;
				}
				if (t == HTML.Tag.A) {
					if (!attachmentsSwitch) htmlOut += "<" + t + " " + a + ">";
				} else if (t == HTML.Tag.LI || t == HTML.Tag.UL) {
					if (level > 1 && passThru) htmlOut += "<" + t.toString() + ">";
				} else if (passThru) htmlOut += "<" + t.toString() + ">";
			}
			
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				if (!skip && !attachmentsSwitch) htmlOut += dataString;
				if (headerSwitch) headerString = dataString;
			}
			
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.BR) htmlOut += "<" + t.toString() + ">";
			}
			
			public void handleEndTag(HTML.Tag t, int pos) {
				if (t == HTML.Tag.UL) attachmentsSwitch = false;
				if (t == HTML.Tag.UL || t == HTML.Tag.LI) {
					if (level > 1 && passThru) {
						htmlOut += "</" + t.toString() + ">";
					}
				} else if (t == HTML.Tag.H2) {
					headerSwitch = false;
				} else if (t == HTML.Tag.H3) {
					skip = false;
				} else if (t == HTML.Tag.TITLE) {
					skip = false;
				} else if (t == HTML.Tag.STRONG) {
					skip = false;
					notesSwitch = true;
				} else if (t == HTML.Tag.A) {
					if (!attachmentsSwitch) htmlOut += "</" + t + ">";
				} else if (t == HTML.Tag.P) {
					if (!htmlOut.isEmpty()) {
						String label = htmlOut;
						if (label.length() > 30) label = htmlOut.substring(0, 30);
						if (!label.toString().equals("\u00a0")) {	// &nbsp;
							lastMinorNode = addNode(label, htmlOut, false);
							GraphEdge edge = new GraphEdge(edgeNum++, lastMinorNode, lastMajorNode, 
									Color.decode("#f0f0f0"), "");
							edges.put(edgeNum, edge);
						}
						htmlOut = "";
					}
				} else if (t == HTML.Tag.BODY) {
					if (!htmlOut.isEmpty()) {
						lastMajorNode = addNode(headerString, htmlOut, true);
						htmlOut = "";
					}
				} else {
					if (passThru) htmlOut += "</" + t.toString() + ">";
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error ZI128 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error ZI129 " + e3.toString());
		}
	}
}

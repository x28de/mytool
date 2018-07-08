package de.x28hd.tool;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class EnexImport {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	String dataString = "";
	
	String htmlOut = "";
	boolean ever = false;
	boolean link = false;
	
	private static final String XML_ROOT = "en-export";
	
	GraphPanelControler controler;

	public EnexImport(JFrame mainWindow, GraphPanelControler controler) {
//		File file = new File("C:\\Users\\Matthias\\Desktop\\Evernote.enex");
		controler.displayPopup("May 12, 2016: This is just a Quick and Dirty first attempt.\n" + 
				"Regard it as a Proof Of Concept. Maybe soon more.");
		FileDialog fd = new FileDialog(mainWindow);
		fd.setTitle("Select an Evernote ENEX file");
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		File file = new File(filename);
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error EI101 " + e);
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document enex = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error EI102 " + e2 );
		}
		
		try {
			enex = db.parse(fileInputStream);
			Element enexRoot = null;
			enexRoot = enex.getDocumentElement();
			if (enexRoot.getTagName() != XML_ROOT) {
				System.out.println("Error EI105, unexpected: " + enexRoot.getTagName() );
				fileInputStream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error EI106 " + e1);			
		} catch (SAXException e) {
			System.out.println("Error EI107" + e );
		}
		
		new EnexImport(enex, controler);
	}
	
	public EnexImport(Document enex, GraphPanelControler controler) {
		int maxVert = 10;
		
		NodeList enexItems = enex.getElementsByTagName("note");
		
		for (int i = 0; i < enexItems.getLength(); i++) {
			Element note = (Element) enexItems.item(i);
			NodeList titleContainer = note.getElementsByTagName("title");
			String title = titleContainer.item(0).getTextContent().toString();
			NodeList contentContainer = note.getElementsByTagName("content");
			String content = contentContainer.item(0).getTextContent().toString();
			content = filterHTML(content);
			
			// Create node		TODO remove old attributes
			
			int j = i;
			String newNodeColor = "#ccdddd";
			String newLine = "\r";
			String topicName = title;
			String verbal = content;
			if (topicName.equals(newLine)) topicName = "";
			if (verbal == null || verbal.equals(newLine)) verbal = "";
			if (topicName.isEmpty() && verbal.isEmpty()) continue;
			int id = 100 + j;
			
			int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			int x = 40 + (j/maxVert) * 150;
			Point p = new Point(x, y);
			GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

			nodes.put(id, topic);
			
			if (i == 0) dataString = topicName + "\t"+ verbal + "\r\n";
			else dataString += topicName + "\t" + verbal + "\r\n";
		}
		
		//	Pass on
		
		edges.clear();
//		try {
//			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
//		} catch (TransformerConfigurationException e1) {
//			System.out.println("Error EI108 " + e1);
//		} catch (IOException e1) {
//			System.out.println("Error EI109 " + e1);
//		} catch (SAXException e1) {
//			System.out.println("Error EI110 " + e1);
//		}
		controler.getNSInstance().setInput(dataString, 6);
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
					if (address.startsWith("evernote://")) {
						htmlOut = "(Link cannot be reconstructed from export file)" + 
								"<br /" + address + "<br /";
					} else {
						htmlOut = htmlOut + "<a href=\"" + address + "\">" + address + "</a> ";
					}
				}
			}
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				if (ever && !link) {
					htmlOut = htmlOut + dataString + " ";
				} else {
//					System.out.println("DataString: " + dataString);
				}
			}
			public void handleEndTag(HTML.Tag t, int pos) {
				if (t.toString() == "a") {
					link = false;
				}
			}
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t.toString().equals("en-note")) {
					ever = !ever;
				}
				if (t.toString().equals("en-media")) {
					htmlOut = "(Media cannot yet be displayed)";
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
			System.out.println("Error EI109 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error EI110 " + e3.toString());
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

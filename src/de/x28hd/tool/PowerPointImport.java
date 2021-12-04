package de.x28hd.tool;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PowerPointImport {

	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	GraphPanelControler controler;
	String dataString = "";
	
	private static final String XML_ROOT = "p:sld";
	String presoName = "";
	GraphNode titleSlide = null;
	int slideNum = 0;
	int edgesNum = 0;
	
	FileWriter list;

	public PowerPointImport(File file, GraphPanelControler controler) {
		presoName = file.getName();
		
		// Prepare text file output
		String listName = file.getName().replace(".pptx", ".txt");
		String baseDir = "";
		try {
			baseDir = System.getProperty("user.home") + File.separator + "Desktop";
		} catch (Throwable e) {
			System.out.println("Error PI101" + e );
		}
		try {
			list = new FileWriter(baseDir + File.separator + listName);
		} catch (IOException e1) {
			System.out.println("Error PI102 FileWriter failed " + e1);
			e1.printStackTrace();
		} 
		
		// Unzip the PPTX (calls import once per slide)
		ZipFile zfile = null;
		try {
			zfile = new ZipFile(file);
			Enumeration<? extends ZipEntry> e = zfile.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				String filename = entry.getName();
				filename = filename.replace('\\', '/');		
				if (!filename.startsWith("ppt/slides/slide") || !filename.endsWith(".xml")) {
					continue;
				} else {
					String[] nameParts = filename.split("/");
					String number = nameParts[2].replaceAll("\\D", "");
					slideNum = Integer.parseInt(number);
					InputStream stream = zfile.getInputStream(entry);
					
					// One slide
					powerPointImport(stream, controler);
				}
			}
		} catch (IOException e1) {
			System.out.println("Error PI103 " + e1);
		}
		try {
			list.close();
		} catch (IOException e1) {
			System.out.println("Error PI104 FileWriter failed");
			e1.printStackTrace();
		} 
		
		// connect them 
		Enumeration<GraphNode> nodeList = nodes.elements();
		while (nodeList.hasMoreElements()) {
			GraphNode node = nodeList.nextElement();
			if (node.equals(titleSlide)) continue;
			edgesNum++;
			GraphEdge edge = new GraphEdge(edgesNum, node, titleSlide, Color.decode("#c0c0c0"), "");
			edges.put(edgesNum,  edge);
			
		}
		
		// pass on
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error PI105 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error PI106 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error PI107 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
	}

	// Zip entry to XML
	
	public void powerPointImport(InputStream stream, GraphPanelControler controler) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error PI108 " + e2 );
		}
		
		try {
			inputXml = db.parse(stream);
			
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != XML_ROOT) {
				System.out.println("Error PI109, unexpected: " + inputRoot.getTagName() );
				stream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error PI110 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error PI111 " + e );
		}
		
		powerPointImport(inputXml, controler);
	}

	// Extract texts from XML
	
	public void powerPointImport(Document inputXml, GraphPanelControler controler) {

		NodeList slideContainer = inputXml.getElementsByTagName("p:cSld");
		Element slide = (Element) slideContainer.item(0);
		NodeList itemList = slide.getElementsByTagName("p:spTree");
		Element tree = (Element) itemList.item(0);
		NodeList spContainer = tree.getElementsByTagName("p:sp");
		String slideTitle = "";
		String slideString = "";
		boolean first = true;

		for (int j = 0; j < spContainer.getLength(); j++) {
			Element spElem = (Element) spContainer.item(j);
			NodeList bodyContainer = spElem.getElementsByTagName("p:txBody");
			Element sp = (Element) bodyContainer.item(0);

			NodeList runList = sp.getElementsByTagName("a:p");
			for (int k = 0; k < runList.getLength(); k++) {
				String bulletString = "";
				Element node = (Element) runList.item(k);
				NodeList runContainer = node.getElementsByTagName("a:r");
				for (int l = 0; l < runContainer.getLength(); l++) {
					Element runElem = (Element) runContainer.item(l);
					NodeList textContainer = runElem.getElementsByTagName("a:t");
					if (textContainer.getLength() > 0) {
						Element textElem = (Element) textContainer.item(0);
						if (first) {
							slideTitle = slideNum + " " + textElem.getTextContent(); 
							slideString = slideTitle + "<p>";
							try {
								list.write(slideTitle + "\n");
							} catch (IOException e) {
								System.out.println("Error PI112 " + e);
							}
							if (slideNum == 1) slideTitle = presoName;
							first = false;
						} else {
							bulletString += textElem.getTextContent();
						}
					}
				}
				if (!bulletString.trim().isEmpty() && !bulletString.contains("http")) {
					slideString += bulletString.trim() + "<p>";
					try {
						list.write(bulletString + "\n");
					} catch (IOException e) {
						System.out.println("Error PI113 " + e);
					}
				}
			}
		}
		
		
		// Create icons
		
		Point p = new Point(40, 40 + (slideNum * 50));
		GraphNode node = new GraphNode (slideNum, p, Color.decode("#ccdddd"), slideTitle, slideString);	
		nodes.put(slideNum, node);
		if (slideNum == 1) {
			titleSlide = node;
			node.setColor("#bbbbff");
			Point xy = node.getXY();
			node.setXY(new Point(xy.x - 50, xy.y));
		}
	}
	
}

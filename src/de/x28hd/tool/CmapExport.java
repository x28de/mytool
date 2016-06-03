package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CmapExport {
	
	private static final String XML_ROOT = "cmap";
	GraphPanelControler controler;
	boolean success = false;

	public CmapExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
			String storeFilename, GraphPanelControler controler) {
		
		controler.setWaitCursor();
		
		// Initialize output 
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(storeFilename);
		} catch (FileNotFoundException e1) {
			System.out.println("error DE101 " + e1);			}

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Transformer transformer = null;
		Document out = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("error DE102 " + e2 );
		}
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e2) {
			System.out.println("error DE103 " + e2);
		} catch (TransformerFactoryConfigurationError e2) {
			System.out.println("error DE104 " + e2 );
		}

		//
		//	Assemble XML

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		out = db.newDocument();
		Element outRoot = out.createElementNS("http://cmap.ihmc.us/xml/cmap/", XML_ROOT );
		out = outRoot.getOwnerDocument();
		out.appendChild(outRoot);	
//		Element resMeta = out.createElement("res-meta");
//		outRoot.appendChild(resMeta);
		
		//	Header
		Element outMap = out.createElement("map");
		Rectangle bounds = controler.getBounds();
		outMap.setAttribute("width", bounds.width + 100 + "");
		outMap.setAttribute("height", bounds.height + 100 + "");
		outRoot.appendChild(outMap);
		
		//	Concepts
		Element conceptList = out.createElement("concept-list");
		Enumeration<GraphNode> myNodes = nodes.elements();
		while (myNodes.hasMoreElements()) {
			Element concept = out.createElement("concept");
			GraphNode node = myNodes.nextElement();
			int num = node.getID();
			concept.setAttribute("id", num + "");
//			concept.setAttribute("id", stolenIDs[num]);
			String label = node.getLabel();
			label = label.replace("\r", "");
			concept.setAttribute("label", label);
			conceptList.appendChild(concept);
		}
		outMap.appendChild(conceptList);
		
		//	Linking Phrases
		Element linkingPhraseList = out.createElement("linking-phrase-list");
		outMap.appendChild(linkingPhraseList);
		
		//	Connections
		Element connectionList = out.createElement("connection-list");
		Enumeration<GraphEdge> myEdges = edges.elements();
		while (myEdges.hasMoreElements()) {
			Element connection = out.createElement("connection");
			GraphEdge edge = myEdges.nextElement();
			int num = edge.getID();
			connection.setAttribute("id", num + "");
			int n1 = edge.getN1();
			connection.setAttribute("from-id", n1 + "");
//			connection.setAttribute("from-id", stolenIDs[n1]);
			int n2 = edge.getN2();
			connection.setAttribute("to-id", n2 + "");
//			connection.setAttribute("to-id", stolenIDs[n2]);
			connectionList.appendChild(connection);
		}
		outMap.appendChild(connectionList);
		
		//	Concept Appearances
		Element conceptAppearanceList = out.createElement("concept-appearance-list");
		Enumeration<GraphNode> myNodes2 = nodes.elements();
		while (myNodes2.hasMoreElements()) {
			Element conceptAppearance = out.createElement("concept-appearance");
			GraphNode node = myNodes2.nextElement();
			int num = node.getID();
			conceptAppearance.setAttribute("id", num + "");
//			conceptAppearance.setAttribute("id", stolenIDs[num]);
			Point xy = node.getXY();
			xy.translate(- bounds.x, - bounds.y);
			conceptAppearance.setAttribute("x", xy.x + 50 + "");
			conceptAppearance.setAttribute("y", xy.y + 50 + "");
			conceptAppearanceList.appendChild(conceptAppearance);
		}
		outMap.appendChild(conceptAppearanceList);
		
		//	Linking Phrase Appearances
		Element linkingPhraseAppearanceList = out.createElement("linking-phrase-appearance-list");
		outMap.appendChild(linkingPhraseAppearanceList);
		
		//	Connection Appearances
		Element connectionAppearanceList = out.createElement("connection-appearance-list");
		Enumeration<GraphEdge> myEdges2 = edges.elements();
		while (myEdges2.hasMoreElements()) {
			Element connectionAppearance = out.createElement("connection-appearance");
			GraphEdge edge = myEdges2.nextElement();
			Color color = edge.getColor();
			if (color.equals(Color.decode("#f0f0f0")) 
					|| color.equals(Color.decode("#d8d8d8"))) {	//	pale
				int num = edge.getID();
				connectionAppearance.setAttribute("id", num + "");
				connectionAppearance.setAttribute("style", "dashed");
				connectionAppearanceList.appendChild(connectionAppearance);
			}
		}
		outMap.appendChild(connectionAppearanceList);
		
		//
		//	Output 
		try {
			transformer.transform(new DOMSource(out), new StreamResult(output));
		} catch (TransformerException e) {
			System.out.println("error DE109 " + e);
		}
		
		String xml = output.toString();
		xml = xml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", 
						  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		byte[] cdsOut = xml.getBytes();
		success = true;
		try {
			fout.write(cdsOut);
			
			if (!success) controler.displayPopup("Export failed");
			controler.setDefaultCursor();
			fout.close();
		} catch (IOException e) {
			System.out.println("error DE110 " + e);
		}
	}
}

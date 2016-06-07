package de.x28hd.tool;

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

public class BrainExport {
	
	private static final String XML_ROOT = "BrainData";
	GraphPanelControler controler;
	boolean success = false;
	int firstThought = 0;

	public BrainExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
			String storeFilename, GraphPanelControler controler) {
		
		controler.setWaitCursor();
		
		// Initialize output 
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(storeFilename);
		} catch (FileNotFoundException e1) {
			System.out.println("error BE101 " + e1);			}

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Transformer transformer = null;
		Document out = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("error BE102 " + e2 );
		}
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e2) {
			System.out.println("error BE103 " + e2);
		} catch (TransformerFactoryConfigurationError e2) {
			System.out.println("error BE104 " + e2 );
		}

		//
		//	Assemble XML

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		out = db.newDocument();
		Element outMap = out.createElement(XML_ROOT );
		out.appendChild(outMap);	
		
		//	Concepts
		Element conceptList = out.createElement("Thoughts");
		Enumeration<GraphNode> myNodes = nodes.elements();
		
		while (myNodes.hasMoreElements()) {
			Element concept = out.createElement("Thought");
			GraphNode node = myNodes.nextElement();
			
			int num = node.getID();
			firstThought = num;
			Element guid = out.createElement("guid");
			guid.setTextContent("concept-" + num);
			concept.appendChild(guid);
			
			String labelString = node.getLabel();
			labelString = labelString.replace("\r", "");
			Element label = out.createElement("name");
			label.setTextContent(labelString);
			concept.appendChild(label);

			conceptList.appendChild(concept);
		}
		outMap.appendChild(conceptList);
		
		//	Connections
		Element connectionList = out.createElement("Links");
		Enumeration<GraphEdge> myEdges = edges.elements();

		while (myEdges.hasMoreElements()) {
			Element connection = out.createElement("Link");
			GraphEdge edge = myEdges.nextElement();
			int n1 = edge.getN1();
			Element from = out.createElement("idA");
			from.setTextContent("concept-" + n1);
			connection.appendChild(from);
			int n2 = edge.getN2();
			Element to = out.createElement("idB");
			to.setTextContent("concept-" + n2);
			connection.appendChild(to);
			Element dir = out.createElement("dir");
			dir.setTextContent("1");
			connection.appendChild(dir);
			
			connectionList.appendChild(connection);
		}
		outMap.appendChild(connectionList);
		
		//	Entries
		Element entryList = out.createElement("Entries");
		Enumeration<GraphNode> myNodes3 = nodes.elements();
		
		while (myNodes3.hasMoreElements()) {
			Element entry = out.createElement("Entry");
			GraphNode node = myNodes3.nextElement();
			
			Element entryObjects = out.createElement("EntryObjects");
			entry.appendChild(entryObjects);
			
			Element entryObject = out.createElement("EntryObject");
			entryObjects.appendChild(entryObject);
			
			Element objectType = out.createElement("objectType");
			objectType.setTextContent("0");
			entryObject.appendChild(objectType);
			
			Element objectID = out.createElement("objectID");
			int id = node.getID();
			objectID.setTextContent("concept-" + id);
			entryObject.appendChild(objectID);
			
			Element body = out.createElement("body");
			String detail = node.getDetail();
			body.setTextContent(detail);
			entryObject.appendChild(body);
			
			entryList.appendChild(entry);
		}
		outMap.appendChild(entryList);
		
		//
		//	Output 
		try {
			transformer.transform(new DOMSource(out), new StreamResult(output));
		} catch (TransformerException e) {
			System.out.println("error BE109 " + e);
		}
		
		String xml = output.toString();
		xml = xml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", 
						  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		byte[] cdsOut = xml.getBytes();
		success = true;
		try {
			fout.write(cdsOut);
			
			if (!success) controler.displayPopup("Export failed");
			controler.displayPopup("Export file created. Please be cautious \n"
					+ "before importing it into your serious data.\n"
					+ "After importing, you may need to click \n"
					+ "File > Utilities > Analyze Main Thoughts.\n");
			controler.setDefaultCursor();
			fout.close();
		} catch (IOException e) {
			System.out.println("error BE110 " + e);
		}
	}
}

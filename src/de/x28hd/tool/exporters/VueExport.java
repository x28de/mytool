package de.x28hd.tool.exporters;

import java.awt.Color;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.text.html.HTMLEditorKit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.MyHTMLEditorKit;

public class VueExport {
	
	private static final String XML_ROOT = "LW-MAP";
	PresentationService controler;
	boolean success = false;
	String htmlOut = "";
	public final static String XSI = "http://www.w3.org/2001/XMLSchema-instance"; 
	
	public VueExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
			String storeFilename, PresentationService controler) {
		
		// Initialize output 
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(storeFilename);
		} catch (FileNotFoundException e1) {
			System.out.println("error VE101 " + e1);
		}

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = null;
		Transformer transformer = null;
		Document out = null;
		int maxNodeNum = Integer.MIN_VALUE;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("error VE102 " + e2 );
		}
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			//	Otherwise, VUE's file loading will fail (it used mark 2048 to skip beginning)
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");
			} catch (TransformerConfigurationException e2) {
			System.out.println("error VE103 " + e2);
		} catch (TransformerFactoryConfigurationError e2) {
			System.out.println("error VE104 " + e2 );
		}

		//
		//	Assemble XML

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		out = db.newDocument();
		Element outRoot = out.createElement(XML_ROOT );
		outRoot.setAttribute("xmlns:xsi", XSI);
		out.appendChild(outRoot);	
		
//		//	Header
		Element fillColor = out.createElement("fillColor");
		fillColor.setTextContent("#FFFFFF");
		outRoot.appendChild(fillColor);
		
		//	Concepts
		Enumeration<GraphNode> myNodes = nodes.elements();
		while (myNodes.hasMoreElements()) {
			Element concept = out.createElement("child");
			GraphNode node = myNodes.nextElement();
			int num = node.getID();
			if (num > maxNodeNum) maxNodeNum = num;
			concept.setAttribute("ID", num + "");
			concept.setAttribute("xsi:type", "node");
			concept.setAttribute("strokeWidth", "2.0");
//			concept.setAttribute("autoSized", "true");

			Color color = node.getColor();
			String colorString = String.format("#%02x%02x%02x", 
					color.getRed(), color.getGreen(), color.getBlue());
			Element strokeColor = out.createElement("strokeColor");
			strokeColor.setTextContent(colorString);
			concept.appendChild(strokeColor);

			Element shape = out.createElement("shape");
			shape.setAttribute("xsi:type", "roundRect");
			concept.appendChild(shape);

			Point xy = node.getXY();
			concept.setAttribute("x", xy.x + ".0");
			concept.setAttribute("y", xy.y + ".0");
			
			String label = node.getLabel();
			label = label.replace("\r", "");
			concept.setAttribute("label", label);
			
			String detail = node.getDetail();
			detail = detail.replace("<br />", "%nl");
			detail = detail.replace("<br>", "%nl;");
			detail = filterHTML(detail);
			if (detail.isEmpty()) detail = label;
			Element notes = out.createElement("notes");
			notes.setTextContent(detail);
			concept.appendChild(notes);

			outRoot.appendChild(concept);
		}
		
		//	Connections
		Enumeration<GraphEdge> myEdges = edges.elements();
		while (myEdges.hasMoreElements()) {
			Element connection = out.createElement("child");
			GraphEdge edge = myEdges.nextElement();
			int num = edge.getID();
			connection.setAttribute("ID", "" + num + maxNodeNum);
			connection.setAttribute("xsi:type", "link");
			connection.setAttribute("stroke-width", "2.0");
			
			int n1 = edge.getN1();
			Element n1el = out.createElement("ID1");
			n1el.setAttribute("xsi:type", "node");
			n1el.setTextContent(n1 + "");
			connection.appendChild(n1el);
			
			int n2 = edge.getN2();
			Element n2el = out.createElement("ID2");
			n2el.setAttribute("xsi:type", "node");
			n2el.setTextContent(n2 + "");
			connection.appendChild(n2el);
			
			outRoot.appendChild(connection);
		}
		
		//
		//	Output 
		try {
			transformer.transform(new DOMSource(out), new StreamResult(output));
		} catch (TransformerException e) {
			System.out.println("error VE109 " + e);
		}
		
		String xml = output.toString();
		xml = xml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", 
						  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		byte[] cdsOut = xml.getBytes();
		success = true;
		try {
			fout.write(cdsOut);
			
			if (!success) controler.displayPopup("Export failed");
			fout.close();
		} catch (IOException e) {
			System.out.println("error VE110 " + e);
		}
	}
	
//
//	Accessories to eliminate HTML tags 
//	Duplicate of NewStuff TODO reuse

private String filterHTML(String html) {
	htmlOut = "";
	MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
	HTMLEditorKit.Parser parser = null;
	HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
		public void handleText(char[] data, int pos) {
			String dataString = new String(data);
			htmlOut = htmlOut + dataString + " ";
		}
	};
	parser = htmlKit.getParser();
	Reader reader; 
	reader = (Reader) new StringReader(html);
	try {
		parser.parse(reader, cb, true);
	} catch (IOException e2) {
		System.out.println("Error VE109 " + e2);
	}
	try {
		reader.close();
	} catch (IOException e3) {
		System.out.println("Error VE110 " + e3.toString());
	}
	return htmlOut;
}

}

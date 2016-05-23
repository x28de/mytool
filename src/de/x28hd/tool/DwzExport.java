package de.x28hd.tool;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

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
import javax.xml.transform.stream.StreamResult;

import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DwzExport {
	
	//  Major fields
	String outString = "";
	Delta contentDelta = new Delta(0);
	Delta statementDelta = contentDelta;

	//	For RDF
	int maxVertical;
	String rootBody = "";
	String rootItem = "";
	String inboxBody = "";
	String inboxItem = "";
	
	//  For unique CDS uris
	private long lastTime = 0;
	private long count = 0;
	
	//  Long constant strings
	public static final String URI_PREFIX = "urn-x28hd.de-t";

	//	XML string constants
	private static final String XML_ROOT = "kgif";

	String htmlOut = "";
	GraphPanelControler controler;
	boolean success = false;

	
	public DwzExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
			String zipFilename, GraphPanelControler controler)  {

//		Reads stub and adds to it;
//		For CDS, we create XML Documents: stub, contentDeltas & statementDeltas, and merge them
		
//
//		Read stub file
		
		controler.setWaitCursor();
		
		InputStream cdsInputStream = getClass().getResourceAsStream("dwzstub.xml"); 
		
		// Initialize output 
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(zipFilename);
		} catch (FileNotFoundException e1) {
			System.out.println("Error IE101 " + e1);			}

//		
// 		CDS: read stub, write into delta doc, then merge both;  In parallel, add the RDF lines

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Transformer transformer = null;
		Document stub = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error IE102 " + e2 );
		}
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e2) {
			System.out.println("Error IE103 " + e2);
		} catch (TransformerFactoryConfigurationError e2) {
			System.out.println("Error IE104 " + e2 );
		}
		
//
// 		Read CDS stub
		
		try {
			stub = db.parse(cdsInputStream);
			Element stubRoot = null;
			stubRoot = stub.getDocumentElement();
			if (stubRoot.getTagName() != XML_ROOT) {
				System.out.println("Error IE105" );
				fout.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error IE106 " + e1);			
		} catch (SAXException e) {
			System.out.println("Error IE107" + e );
		}

//
// 		Create CDS deltas
		
		// Loop though the MyTool nodes "topics"
		
		Enumeration<Integer> topics0 = nodes.keys();  
		TreeMap<String,Integer> orderMap = new TreeMap<String,Integer>();
		SortedMap<String,Integer> orderList = (SortedMap<String,Integer>) orderMap;
		while (topics0.hasMoreElements()){
			int key = topics0.nextElement();
			GraphNode node = nodes.get(key);
			String sortString = node.getLabel().toUpperCase();
			orderMap.put(sortString, key);
		}
		SortedSet<String> orderSet = (SortedSet<String>) orderList.keySet();
		Iterator<String> ixit = orderSet.iterator(); 
		
		maxVertical = (int) Math.sqrt(nodes.size() * 6);
		
		Hashtable<Integer,String> num2rdf = new Hashtable<Integer,String>();
		Hashtable<Integer,String> num2cds = new Hashtable<Integer,String>();

		while (ixit.hasNext()) {
			String nextLabel = ixit.next();
			int topicID = orderMap.get(nextLabel);
			GraphNode topic = nodes.get(topicID);
			String label = topic.getLabel();
			label = label.replace("\r","");
			String detail = topic.getDetail();
			detail = filterHTML(detail);
			
			String myCdsUri = createUniqueCdsURI().toString();
			addXmlItem(label, myCdsUri, detail);	
			num2cds.put(topicID, myCdsUri);
			
			String myRdfUri = "<urn:imapping:" + UUID.randomUUID().toString() + ">";
			num2rdf.put(topicID, myRdfUri);
		}
		
		//	Loop through edges "assocs"
		
		Enumeration<Integer> assocs = edges.keys();  
		while (assocs.hasMoreElements()){
			int assocID = assocs.nextElement();
			GraphEdge edge = edges.get(assocID);
			int n1 = edge.getN1();
			int n2 = edge.getN2();

			String sourceCds = num2cds.get(n1);
			String targetCds = num2cds.get(n2);
			String linkCds = createUniqueCdsURI().toString();
			addEdgeXml(linkCds, sourceCds, targetCds);
		}

//
// 		Write out the merged tree

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		Document merge = db.newDocument();
		Element mergeRoot = merge.createElement(XML_ROOT);
		merge.appendChild(mergeRoot);	

		Element mergeHeader = merge.createElement("header");
		mergeRoot.appendChild(mergeHeader);
		
		// Don't loop through 5 containers as in iMapping export, but just one container
		// mixing items and links

		int c = 0;

			Element mergeContainer = merge.createElement("graph");
			mergeRoot.appendChild(mergeContainer);

			NodeList stubItems = stub.getElementsByTagName("node");
			for (int i = 0; i < stubItems.getLength(); i++) {
				mergeContainer.appendChild(merge.adoptNode(stubItems.item(i).cloneNode(true)));
			}
			String tagname = "";
			NodeList deltaItems = null;
			System.out.println("For container " + c + " nodes, there are " + stubItems.getLength() + " stub items");

			stubItems = stub.getElementsByTagName("link");
			for (int i = 0; i < stubItems.getLength(); i++) {
				mergeContainer.appendChild(merge.adoptNode(stubItems.item(i).cloneNode(true)));
			}
			tagname = "";
			deltaItems = null;
			System.out.println("For container " + c + " links, there are " + stubItems.getLength() + " stub items");

//			if (c == 0) {
				tagname = "node";
				deltaItems = contentDelta.getTree().getElementsByTagName(tagname);
				for (int i = 0; i < deltaItems.getLength(); i++) {
					NodeList topicsContainer = contentDelta.getRoot().getElementsByTagName(tagname);
					mergeContainer.appendChild(merge.adoptNode(topicsContainer.item(i).cloneNode(true)));
				}
				System.out.println("For container " + c + " nodes, there are " + deltaItems.getLength() + " delta items");
//			} else if (c == 3) {
				tagname = "link";
				deltaItems = statementDelta.getTree().getElementsByTagName(tagname);
				for (int i = 0; i < deltaItems.getLength(); i++) {
					NodeList topicsContainer = statementDelta.getRoot().getElementsByTagName(tagname);
					mergeContainer.appendChild(merge.adoptNode(topicsContainer.item(i).cloneNode(true)));
				}
				System.out.println("For container " + c + " links, there are " + deltaItems.getLength() + " delta items");
//			}
		
		merge.normalize();

		try {
			transformer.transform(new DOMSource(merge), new StreamResult(output));
		} catch (TransformerException e) {
			System.out.println("Error IE109 " + e);
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
			System.out.println("Error IE110 " + e);
		}

	}

	public void addXmlItem(String text, String myCdsUri, String detail) {

		Element item = contentDelta.getTree().createElement("node");
		item.setAttribute("id", myCdsUri);
		contentDelta.getContainer().appendChild(item);

		Element label = contentDelta.getTree().createElement("label");
		label.setAttribute("contentType", "http://purl.org/net/xydra/datatypes#String");
		text = text.replace("\r","");	// "label" is wrong
		text = text.replace("& ","&amp; ");	// TODO find a better way
		text = text.replace("<br>","<br />");	// TODO find a better way
		label.setTextContent(text);
		item.appendChild(label);

		Element content = contentDelta.getTree().createElement("content");
		content.setAttribute("contentType", "text/plain");
		content.setAttribute("parseType", "jspwiki+dwz");
		content.setTextContent(detail);
		item.appendChild(content);
	}
	
	public void addEdgeXml(String linkCds, String sourceCds, String targetCds) {

		Element item = statementDelta.getTree().createElement("link");
		item.setAttribute("id", linkCds);
		item.setAttribute("to", targetCds);
		item.setAttribute("type", "cds-rel-hasRelated");
		item.setAttribute("from", sourceCds);
		Element metaDummy = statementDelta.getTree().createElement("metadata");
		item.appendChild(metaDummy);
		statementDelta.getContainer().appendChild(item);
	}
	
	
	private class Delta {
		
//		int c;
		Document deltaTree = null;
		Element deltaRoot = null;
		Element deltaContainer = null;
		
		private Delta(int c) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = null;
			
			try {
				db = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e2) {
				System.out.println("Error IE102 " + e2 );
			}
			
			deltaTree = db.newDocument();
			deltaRoot = deltaTree.createElement(XML_ROOT);
			deltaTree.appendChild(deltaRoot);
			deltaContainer = deltaTree.createElement("graph");
			deltaRoot.appendChild(deltaContainer);
		}
		
		public Document getTree() {
			return deltaTree;
		}
		public Element getRoot() {
			return deltaRoot;
		}
		public Element getContainer() {
			return deltaContainer;
		}
	}
	
//
//	Accessories to eliminate HTML tags from label
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
		System.out.println("Error IM109 " + e2);
	}
	try {
		reader.close();
	} catch (IOException e3) {
		System.out.println("Error IM110 " + e3.toString());
	}
	return htmlOut;
}

private static class MyHTMLEditorKit extends HTMLEditorKit {
	private static final long serialVersionUID = 7279700400657879527L;

	public Parser getParser() {
		return super.getParser();
	}
}
	
    
//
//	Adapted from org.semanticdesktop.swecr.model.util.MemIDProvider 
    
	public synchronized URIImpl createUniqueCdsURI() {
		TimeZone utc = TimeZone.getTimeZone("UTC");
		Calendar cal = Calendar.getInstance(utc);
		long now = cal.getTimeInMillis();
		if (now > lastTime) {
			// easy
			count = 0;
		} else {
			// disambiguated by count
			count++;
		}
		int month = cal.get(Calendar.MONTH);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		int milli = cal.get(Calendar.MILLISECOND);
		String uriString = cal.get(Calendar.YEAR) + "" + (month < 9 ? "0" + (month + 1) : month + 1) + "" + (day < 10 ? "0" + day : day) + "-" + (hour < 10 ? "0" + hour : hour) + "."
				+ (minute < 10 ? "0" + minute : minute) + "." + (second < 10 ? "0" + second : second) + "." + (milli > 99 ? milli : (milli > 9 ? "0" + milli : "00" + milli)) + "-" + count;

		lastTime = now;

		return new URIImpl(URI_PREFIX + uriString);
	}

}

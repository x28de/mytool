package de.x28hd.tool;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;


public class ImappingExport {
	
	//	For RDF
	String outString = "";

	Hashtable<String,String> rdfItems = new Hashtable<String,String>();
	Hashtable<String,String> bodies = new Hashtable<String,String>();

	String myRdfUri = "";
	String myBodyUri = "";
	
	//  For unique CDS uris
	String myCdsUri = "";
	private long lastTime = 0;
	private long count = 0;
	
	//  Long constant strings
	private static final String IMAPPING_PREFIX = 
			"<http://ont.semanticdesktop.org/ontologies/2007/imapping#";
	private static final String HAS_BODY = IMAPPING_PREFIX + "hasBody>";
	private static final String REPR_CDS = IMAPPING_PREFIX + "representsCdsItem>";
	private static final String RDF_STORE_BODY = IMAPPING_PREFIX + "RdfStoreBody>";
	private static final String RDF_CDS_ITEM = IMAPPING_PREFIX + "RdfCdsItem>";
	private static final String RDF_STORE_ITEM = IMAPPING_PREFIX + "RdfStoreImapItem>";
	private static final String HAS_PARENT = IMAPPING_PREFIX + "hasParent>";
	private static final String CDS_PREFIX = 
			"http://www.semanticdesktop.org/ontologies/2007/09/01/cds#";
	private static final String CDS_ROOT = CDS_PREFIX + "rootItem";
	private static final String HAS_DETAIL = CDS_PREFIX + "hasDetail";
	private static final String XML_ROOT = 
			"org.semanticdesktop.swecr.model.memory.xml.XModel";
	private static final String IS_TYPE = 
			"<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
	private static final String DOUBLE = "^^<http://www.w3.org/2001/XMLSchema#double>";
	public static final String URI_PREFIX = "urn:xam.de#t";
	private static final String CDS_INBOX1 = "<uri:cds:inbox>";
	private static final String CDS_INBOX2 = "urn:xam.de#t20160507-16.14.50.170-0";	 // must match stub

	private static final String RDF_FILENAME = "layout.rdf.nt";
	private static final String CDS_FILENAME = "content.cds.xml";
	
	String rootBody = "";
	String rootItem = "";
	String inboxBody = "";
	String inboxItem = "";
	
	
	public ImappingExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, String zipFilename)  {

//
//		Read stub files
		
		InputStream rdfInputStream = getClass().getResourceAsStream("stub.txt"); 
		InputStream cdsInputStream = getClass().getResourceAsStream("stub.xml"); 
		
		// Initialize output 
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(zipFilename);
		} catch (FileNotFoundException e1) {
			System.out.println("Error IE101 " + e1);			}
		ZipOutputStream zout = new ZipOutputStream(fout);

//
//		Copy RDF stub to outString, and analyse it to find INBOX

		String triplesString = convertStreamToString(rdfInputStream);		
		String [] triples = triplesString.split("\\r?\\n");

		for (int i = 0; i < triples.length; i++) {
			String line = triples[i];
			outString = outString + line + "\r";
			String triple[] = line.split("\\s");
			String predicate = triple[1];

			if (predicate.equals(HAS_BODY)) {
				rdfItems.put(triple[0], triple[2]);

			} else if (predicate.equals(REPR_CDS)) {
				String object = triple[2];
				object = object.substring(1, object.length()-1);
				if (object.equals(CDS_ROOT)) {
					rootBody = triple[0];
//				} else if (object.equals(CDS_INBOX1)) {
//					inboxBody = triple[0];
				} else if (object.equals(CDS_INBOX2)) {
					inboxBody = triple[0];
				}
				int objLen = object.length();
				object = object.substring(1, objLen-1);
				bodies.put(triple[0], object);
			}
		}
				
		//	Search for items that have rootBody or inboxBody
		Enumeration<String> itemList = rdfItems.keys();
		while (itemList.hasMoreElements()) {
			String testItem = itemList.nextElement();
			String testBody = rdfItems.get(testItem);
			if (testBody.equals(rootBody)) {
				rootItem = testItem;
			}
			if (testBody.equals(inboxBody)) {
				inboxItem = testItem;
//				System.out.println("Inbox item found: " + inboxItem);
			}
		}

//		
// 		CDS: read stub, write into delta doc, then merge both;  In parallel, add the RDF lines

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Transformer transformer = null;
		Document stub = null;

//		String [] containerNames = {"contentItems", "nameItems", "relations",
//				"statements", "triples"};
//		String [] itemNames = {"contentitem", "nameitem", "relation",
//				"statement", "triple"};

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
				zout.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error IE106 " + e1);			
		} catch (SAXException e) {
			System.out.println("Error IE107" + e );
		}

//
// 		Create CDS delta

		Document delta = db.newDocument();
		Element deltaRoot = delta.createElement(XML_ROOT);
		delta.appendChild(deltaRoot);

		Hashtable<Integer,String> cdsUris = new Hashtable<Integer,String>();
		
		String [] containerNames = {"contentItems", "nameItems", "relations",
				"statements", "triples"};
		String [] itemNames = {"contentitem", "nameitem", "relation",
				"statement", "triple"};

		// Loop through the 5 containers

		for (int c = 0; c < 5; c++) {
			Element deltaContainer = delta.createElement(containerNames[c]);
			deltaContainer.setAttribute("class", "linked-list");
			deltaRoot.appendChild(deltaContainer);

			if (c == 0) {	// contentItems

				String [] constantElements = {"readonly", "deletable", "changeDate",
						"creationDate", "authorURI", "binary"};
				String [] constantValues = {"false", "true", "1461420246049",
						"1461359537610", "http://imapping.info#author", "false"};

				// Loop though the MyTool items

				int nodeNumber = 0;
				int maxVertical = (int) Math.sqrt(nodes.size() * 6);

				Enumeration<Integer> topics = nodes.keys();  
				while (topics.hasMoreElements()){
					nodeNumber++;
					int topicID = topics.nextElement();
					GraphNode topic = nodes.get(topicID);
					String label = topic.getLabel();

					generateUniques();
					cdsUris.put(topicID,myCdsUri);
					outString = addToRdf(outString, nodeNumber, maxVertical);

					Element item = delta.createElement(itemNames[c]);
					deltaContainer.appendChild(item);

					Element uri = delta.createElement("uri");
					item.appendChild(uri);
					uri.setTextContent(myCdsUri);

					for (int j = 0; j < 6; j++) {
						Element el = delta.createElement(constantElements[j]);
						item.appendChild(el);
						el.setTextContent(constantValues[j]);
					}

					Element content = delta.createElement("content");
					item.appendChild(content);
					label = label.replace("\r","");
					content.setTextContent("<p>" + label + "</p>");

					Element mime = delta.createElement("mimetype");
					item.appendChild(mime);
					mime.setAttribute("class", "org.semanticdesktop.binstore.MimeType");
//					mime.setAttribute("reference", 		//  TODO why not [2] as in sample?
//						"/org.semanticdesktop.swecr.model.memory.xml.XModel/contentItems/contentitem/mimetype");
					Element mimetype = delta.createElement("mimeType");
					mime.appendChild(mimetype);
					mimetype.setTextContent("application/stif+xml");
				}
				
			} else if (c == 3) {	//  Statements
				
				String [] constantElements = {"readonly", "deletable", "changeDate",
						"creationDate", "authorURI", "binary"};
				String [] constantValues = {"false", "true", "1461420246049",
						"1461359537610", "http://imapping.info#author", "false"};
				
				// Loop through MyTool items again

				Enumeration<Integer> topics2 = nodes.keys(); 
				while (topics2.hasMoreElements()){
					
					Element stmt = delta.createElement(itemNames[c]);
					deltaContainer.appendChild(stmt);

					Element uri = delta.createElement("uri");
					stmt.appendChild(uri);
					int topicID = topics2.nextElement();
					myCdsUri = cdsUris.get(topicID);
					uri.setTextContent(createUniqueCdsURI().toString());  // Too fast for object's uri + "-1"

					for (int j = 0; j < 6; j++) {
						Element el = delta.createElement(constantElements[j]);
						stmt.appendChild(el);
						el.setTextContent(constantValues[j]);
					}

					Element s = delta.createElement("s");
					stmt.appendChild(s);
//					s.setTextContent(CDS_ROOT);		//	better ?
					s.setTextContent(CDS_INBOX2);

					Element p = delta.createElement("p");
					stmt.appendChild(p);
					p.setTextContent(HAS_DETAIL);

					Element o = delta.createElement("o");
					stmt.appendChild(o);
					o.setTextContent(myCdsUri);

				}
			}
		}

//
// 		Write out the merged tree

		try {
			zout.putNextEntry(new ZipEntry(CDS_FILENAME));
		} catch (IOException e1) {
			System.out.println("Error IE108 " + e1);	
			}
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		Document merge = db.newDocument();
		Element mergeRoot = merge.createElement(XML_ROOT);
		merge.appendChild(mergeRoot);	

		for (int c = 0; c < 5; c++) {

			Element mergeContainer = merge.createElement(containerNames[c]);
			mergeContainer.setAttribute("class", "linked-list");
			mergeRoot.appendChild(mergeContainer);

			NodeList stubItems = stub.getElementsByTagName(itemNames[c]);
			for (int i = 0; i < stubItems.getLength(); i++) {
				mergeContainer.appendChild(merge.adoptNode(stubItems.item(i).cloneNode(true)));
			}

			NodeList deltaItems = delta.getElementsByTagName(itemNames[c]);

			for (int i = 0; i < deltaItems.getLength(); i++) {
				NodeList topicsContainer = deltaRoot.getElementsByTagName(itemNames[c]);
				mergeContainer.appendChild(merge.adoptNode(topicsContainer.item(i).cloneNode(true)));
			}
		}

		merge.normalize();

		try {
			transformer.transform(new DOMSource(merge), new StreamResult(output));
		} catch (TransformerException e) {
			System.out.println("Error IE109 " + e);
		}

		byte[] cdsOut = output.toByteArray();
		try {
			zout.write(cdsOut);
			byte[] outBytes = outString.getBytes(Charset.forName("UTF-8"));
			zout.putNextEntry(new ZipEntry(RDF_FILENAME));
			zout.write(outBytes);
			zout.close();
		} catch (IOException e) {
			System.out.println("Error IE110 " + e);
		}

	}
	
	public void generateUniques() {
		myCdsUri = createUniqueCdsURI().toString();
		myRdfUri = "<urn:imapping:" + UUID.randomUUID().toString() + ">";
		myBodyUri = "<urn:imapping:" + UUID.randomUUID().toString() + ">";
	}

	public String addToRdf(String outString, int itemNumber, int maxVertical) {
		
		outString = outString + myRdfUri + " " + IS_TYPE + " " + RDF_STORE_ITEM +  " .\r";
		outString = outString + "<" + myCdsUri + "> " + IS_TYPE + " " + RDF_CDS_ITEM +  " .\r";
		outString = outString + myRdfUri + " " + HAS_PARENT + " " + inboxItem +  " .\r";
		outString = outString + myBodyUri + " " + IS_TYPE + " " + RDF_STORE_BODY +  " .\r";
		outString = outString + myRdfUri + " " + HAS_BODY + " " + myBodyUri +  " .\r";
		outString = outString + myBodyUri + " " + REPR_CDS + " <" + myCdsUri +  "> .\r";

		int deltaX = 240 * (itemNumber/maxVertical);
		String xString = Integer.toString(10 + deltaX);
		outString = outString + myRdfUri + " " + IMAPPING_PREFIX + "hasPositionX>" 
				+ " \"" + xString + ".0\"" + DOUBLE + " .\r";
		
		int deltaY = 30 * (itemNumber % maxVertical);	// modulo maxVertical
		String yString = Integer.toString(20 + deltaY);
		outString = outString + myRdfUri + " " + IMAPPING_PREFIX + "hasPositionY>"	
				+ " \"" + yString + ".0\"" + DOUBLE + " .\r";
		
		outString = outString + myRdfUri + " " + IMAPPING_PREFIX + "hasExpansionStatus>" 
				+ " " + IMAPPING_PREFIX + "Collapsed>" + " .\r";
		outString = outString + myRdfUri + " " + IMAPPING_PREFIX + "hasItemScale>" 
				+ " \"0.800000011920929\"" + DOUBLE + " .\r";

		outString = outString + myBodyUri + " " + IMAPPING_PREFIX + "hasHeadHeight>" 
				+ " \"16.0\"" + DOUBLE + " .\r";
		outString = outString + myBodyUri + " " + IMAPPING_PREFIX + "hasBellyWidth>" 
				+ " \"230.0\"" + DOUBLE + " .\r";
		outString = outString + myBodyUri + " " + IMAPPING_PREFIX + "hasBellyHeight>" 
				+ " \"32.0\"" + DOUBLE + " .\r";

		return outString;
	}
	
//
//  Accessories for CDATA, start, and end		// TODO reuse several other copies

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

	
//
// 	Accessory 
//	Duplicate of NewStuff TODO reuse

	private String convertStreamToString(InputStream is, Charset charset) {
    	
        //
        // From http://kodejava.org/how-do-i-convert-inputstream-to-string/
        // ("To convert the InputStream to String we use the
        // Reader.read(char[] buffer) method. We iterate until the
        // Reader return -1 which means there's no more data to
        // read. We use the StringWriter class to produce the string.")
    	
    	if (is != null) {
    		Writer writer = new StringWriter();
    		char[] buffer = new char[1024];
    		Reader reader = null;;
    		
   			reader = new BufferedReader(
   					new InputStreamReader(is, charset));	

    		int n;
    		try {
    			while ((n = reader.read(buffer)) != -1) {
    				writer.write(buffer, 0, n);
    			}
    		} catch (IOException e) {
    			System.out.println("Error IE117 " + e);
    			try {
    				writer.close();
    			} catch (IOException e1) {
    				System.out.println("Error IE118 " + e1);
    			}
    		} finally {
    			try {
    				is.close();
    			} catch (IOException e) {
    				System.out.println("Error IE119 " + e);
    			}
    		}
    		String convertedString = writer.toString();
    		return convertedString;
    	} else {        
    		return "";
    	}
    }
    private String convertStreamToString(InputStream is) {
    	return convertStreamToString(is, Charset.forName("UTF-8"));
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

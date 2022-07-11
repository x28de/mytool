package de.x28hd.tool.exporters;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.text.MutableAttributeSet;
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

import de.x28hd.tool.BranchInfo;
import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.MyHTMLEditorKit;

public class ZknExport {
	
	private static final String XML_ROOT = "zettelkasten";
	PresentationService controler;
	boolean success = false;
	HashSet<GraphEdge> nonTreeEdges; 
	int zettelNumber = 0;
	Hashtable<Integer,Integer> z2id = new Hashtable<Integer,Integer>(); //	Zettel to mynode id
	Hashtable<Integer,Integer> id2z = new Hashtable<Integer,Integer>(); //	Mynode id to zettel
	Hashtable<Integer,String> luh = new Hashtable<Integer,String>();	//	"luhmann" tree ref
	Hashtable<Integer,String> manl = new Hashtable<Integer,String>();	//	manlinks

	Document out = null;
	Element outMap;
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	DefaultTreeModel treeModel = null;
	String htmlOut = "";
	
	public ZknExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
			String zipFilename, PresentationService controler) {
		
		this.nodes = nodes;
		this.edges = edges;
		
//
//		Initialize output
		
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(zipFilename);
		} catch (FileNotFoundException e1) {
			System.out.println("Error ZE101 " + e1);			}
		ZipOutputStream zout = new ZipOutputStream(fout);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Transformer transformer = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("error ZE102 " + e2 );
		}
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e2) {
			System.out.println("error ZE103 " + e2);
		} catch (TransformerFactoryConfigurationError e2) {
			System.out.println("error ZE104 " + e2 );
		}

//
//		Assemble XML

		out = db.newDocument();
		outMap = out.createElement(XML_ROOT );
		out.appendChild(outMap);	
		
		//	Prepare entries

		//	Zettels
		
		treeModel = controler.getControlerExtras().getTreeModel();
		
		if (treeModel != null) {
			nonTreeEdges = controler.getControlerExtras().getNonTreeEdges();
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treeModel.getRoot();

			descendTree(treeNode, 0, "");
			
		} else {	
			
			Enumeration<Integer> myNodes = this.nodes.keys();
			while (myNodes.hasMoreElements()) {
				int topicID = myNodes.nextElement();
				zettelNumber++;
				z2id.put(zettelNumber, topicID);
				id2z.put(topicID, zettelNumber);
			}
		}

		//	Connections
		
		Enumeration<GraphEdge> myEdges = this.edges.elements();

		while (myEdges.hasMoreElements()) {
			GraphEdge edge = myEdges.nextElement();
			if (treeModel != null && !nonTreeEdges.contains(edge)) continue;
			int n1 = edge.getN1();
			int n2 = edge.getN2();
			int zettel1 = id2z.get(n1);
			int zettel2 = id2z.get(n2);
			String newManl = zettel2 + "";
			if (manl.containsKey(zettel1)) {
				String oldManl = manl.get(zettel1);
				newManl = oldManl + "," + newManl;
			}
			manl.put(zettel1, newManl);
			//	Reverse link
			newManl = zettel1 + "";
			if (manl.containsKey(zettel2)) {
				String oldManl = manl.get(zettel2);
				newManl = oldManl + "," + newManl;
			}
			manl.put(zettel2, newManl);
		}
		
		//	Create entries
		for (int zettel = 1; zettel <= z2id.size(); zettel++) {
			addNode(zettel);
		}
		
		//	Include 4 stub files
		
		String[] stubFiles = {"authorFile", "bookmarks", "keywordFile", "metaInformation"};
		String[] stubRoots = {"authors", "bookmarks", "keywords", "metainformation"};
		Document[] stubs = new Document[4];
		
		for (int s = 0; s < 4; s++) {
			stubs[s] = db.newDocument();
			Element stub = stubs[s].createElement(stubRoots[s]);
			if (s == 3) {	//	metainformation
				Element version = stubs[3].createElement("version");
				version.setAttribute("id", "3.8");
				stub.appendChild(version);
				Element description = stubs[3].createElement("description");
				stub.appendChild(description);
				Element attachmentpath = stubs[3].createElement("attachmentpath");
				stub.appendChild(attachmentpath);
				Element imagepath = stubs[3].createElement("imagepath");
				stub.appendChild(imagepath);
			}
			stubs[s].appendChild(stub);	
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try {
				transformer.transform(new DOMSource(stubs[s]), new StreamResult(output));
			} catch (TransformerException e) {
				System.out.println("Error ZE105 " + e);
				controler.displayPopup("Export failed");
			}
			try {
				zout.putNextEntry(new ZipEntry(stubFiles[s] + ".xml"));
			} catch (IOException e) {
				System.out.println("Error ZE106 " + e);
				controler.displayPopup("Export failed");
			}
			byte[] nextOut = output.toByteArray();
			try {
				zout.write(nextOut);
			} catch (IOException e) {
				System.out.println("Error ZE107 " + e);
				controler.displayPopup("Export failed");
			}
		}
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			transformer.transform(new DOMSource(out), new StreamResult(output));
		} catch (TransformerException e) {
			System.out.println("Error ZE109 " + e);
		}
		try {
			zout.putNextEntry(new ZipEntry("zknFile.xml"));
		} catch (IOException e1) {
			System.out.println("Error ZE108 " + e1);	
		}
		byte[] mainOut = output.toByteArray();
		success = true;
		try {
			zout.write(mainOut);
			if (!success) controler.displayPopup("Export failed");
			zout.close();
		} catch (IOException e) {
			System.out.println("Error ZE110 " + e);
		}
	}

	public void descendTree(DefaultMutableTreeNode treeNode, int parentZettel, 
			String indent) {
		indent = indent + "  ";
		BranchInfo info = (BranchInfo) treeNode.getUserObject();
		int topicID = info.getKey();
		if (topicID == BranchInfo.NOZETTEL) return;
		if (topicID != BranchInfo.ROOTZETTEL) zettelNumber++;
		int myZettel = zettelNumber;
		
		if (parentZettel > 0) {
			String newLuh = myZettel + "";
			if (luh.containsKey(parentZettel)) {
				String oldLuh = luh.get(parentZettel);
				newLuh = oldLuh + "," + newLuh;
			}
			luh.put(parentZettel, newLuh);
		}
		
		//	Recursion
		@SuppressWarnings("rawtypes")
		Enumeration children =  treeNode.children();
		while (children.hasMoreElements()) {
			descendTree((DefaultMutableTreeNode) children.nextElement(), myZettel, indent);
		}
		if (topicID != BranchInfo.ROOTZETTEL) {
			z2id.put(myZettel, topicID);
			id2z.put(topicID, myZettel);
		}
	}

	public void addNode(int myZettel) {
		Element concept = out.createElement("zettel");
		int topicID = z2id.get(myZettel);
		GraphNode topic = nodes.get(topicID);
		String labelString = topic.getLabel();
		labelString = labelString.replace("\r","");
		
		concept.setAttribute("zknid", myZettel + "");
		
		concept.setAttribute("ts_edited", "1611052051");
		concept.setAttribute("ts_created", "1611212130");
		
		labelString = topic.getLabel();
		labelString = labelString.replace("\r", "");
		Element label = out.createElement("title");
		label.setTextContent(labelString);
		concept.appendChild(label);

		Element body = out.createElement("content");
		String detailString = topic.getDetail();
		detailString = filterHTML(detailString);
		body.setTextContent(detailString + " ");
		concept.appendChild(body);
		
		Element author = out.createElement("author");
		concept.appendChild(author);
		
		Element keywords = out.createElement("keywords");
		concept.appendChild(keywords);
		
		//	NonTree links
		Element manlinks = out.createElement("manlinks");
		if (manl.containsKey(myZettel)) {
			String manlString = manl.get(myZettel);
			manlinks.setTextContent(manlString);
		}
		concept.appendChild(manlinks);
		
		Element links = out.createElement("links");
		concept.appendChild(links);
		
		Element misc = out.createElement("misc");
		concept.appendChild(misc);
		
		Element luhmann = out.createElement("luhmann");
		if (luh.containsKey(myZettel)) {
			String luhString = luh.get(myZettel);
			luhmann.setTextContent(luhString);
		}
		concept.appendChild(luhmann);
		
		outMap.appendChild(concept);
	}

	private String filterHTML(String html) {
		htmlOut = "";
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				htmlOut = htmlOut + dataString + " ";
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t.equals(HTML.Tag.P)) htmlOut = htmlOut + "[br]";
				if (t.equals(HTML.Tag.B)) htmlOut = htmlOut + "[f]";
			}
			public void handleEndTag(HTML.Tag t, int pos) {
				if (t.equals(HTML.Tag.B)) htmlOut = htmlOut + "[/f]";
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
}

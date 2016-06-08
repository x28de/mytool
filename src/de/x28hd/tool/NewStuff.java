package de.x28hd.tool;
import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.TransferHandler;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class NewStuff {
	
	//	Main fields
	String dataString = "";
	Hashtable<Integer, GraphNode> newNodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> newEdges = new Hashtable<Integer, GraphEdge>();
	Point insertion = null;
	
	//	About caller (PresentationService()) and CompositionWindow()
	private GraphPanelControler controler;
	boolean compositionMode = false;
	boolean firstComposition = true;
	boolean windows = false;
	
	//	Input
	public static DataFlavor htmlSelectionFlavor;
	int inputType = 0;
	InputStream stream = null;
	String advisableFilename = "";
	
	//	Map loading
	boolean readyMap = false;
	int topicnum = 0;
	int assocnum = 0;
	Element root;
	boolean isAssoc = false;
	int nodenum = -1;
	int edgenum = -1;
	String nodesArray [][] = new String [600][5];   // 0 = x, 1 = y, 2 = rgb, 3 = label, 4 = id
	String edgesArray [][] = new String [600][3];    // 0 = n1, 1 = n2, 2 = rgb
	Hashtable<String, Integer> nodeids = new Hashtable<String, Integer>();
	Hashtable<String, Integer> edgeids = new Hashtable<String, Integer>();
	int minX, maxX, minY, maxY;
	
	//	HTML exploitation
	String htmlOut = "";
	boolean listItem = false;
	boolean tableRow = false;
	boolean tableCell = false;
	boolean firstColumn = true;
	String dataStringResort = "";
	
	//	Input file sorting
	Hashtable<Integer,String> byModDates = new Hashtable<Integer,String>();;
	TreeMap<Long,Integer> datesMap = new TreeMap<Long,Integer>();
	SortedMap<Long,Integer> datesList = (SortedMap<Long,Integer>) datesMap;

	
	public NewStuff(final GraphPanelControler controler) {
		System.out.println("NS started");
		this.controler = controler;
		windows = (System.getProperty("os.name").startsWith("Windows"));
	}
	
//
//	Handlers for dropped and pasted stuff
	
//	canImport and importData are called by drop from GraphPanel and CompositionWindow,
//	readClipboard is called from Paste operations in PresentationService and 
//	CompositionWindow. 
	
	public boolean canImport(TransferHandler.TransferSupport support, String diag) {
		support.setDropAction(TransferHandler.COPY);
//        support.setShowDropLocation(true);    // no effect ?
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) && 
			!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			System.out.println("NS dataflavors: " + support.getDataFlavors() + " " + support.getDataFlavors().length);
			DataFlavor[] df = support.getDataFlavors();
			for (int i = 0; i < df.length; i++) {
				System.out.println("NS dataflavors: " + df[i].getHumanPresentableName());
			}
        	return false;
        }
		System.out.println("NS canImport " + diag);
		return true;
	}
	
	public boolean importData(TransferHandler.TransferSupport support, String diag) {
   	 
        if (!canImport(support, diag)) {
           return false;
        }

        Transferable t = support.getTransferable();
		insertion = new Point(support.getDropLocation().getDropPoint().x,
				support.getDropLocation().getDropPoint().y);
		System.out.println(support.getDropLocation().getDropPoint().x + ", " +
				support.getDropLocation().getDropPoint().y);
       
        boolean success = transferTransferable(t);
		if (!success) {
			System.out.println("Error NS120.");
		} else {
			System.out.println("NS: Drop or Paste content received.");
		}
        return success;
	}

	public Transferable readClipboard() {
		controler.setWaitCursor();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		return clipboard.getContents(null); 
	}

//
//	Obtain input in 3 different dataFlavors 
	
	@SuppressWarnings("unchecked")
	public boolean transferTransferable(Transferable content) {
		controler.setWaitCursor();
			
		//  File(s) ?

		if (content.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			List<File> l = null;
			try {
				l = (java.util.List<File>) content.getTransferData(DataFlavor.javaFileListFlavor);
			} catch (UnsupportedFlavorException e1) {
				System.out.println("Error NS109 " + e1);
				return false;
			} catch (IOException e1) {
				System.out.println("Error NS110 " + e1);
				return false;
			}
			// Compare actionPerformed() case Open
			dataString = "";
			byModDates.clear();
			datesMap.clear();
			int fileCount = 0;
			String fn = "";
			for (File f : l) {
				fileCount++;
				System.out.println("NS file(s): " + fileCount + " " + f.getName());
				if (fileCount == 1) {
					fn = f.getAbsolutePath();
					dataString = fn;
					inputType = 1;
					advisableFilename = dataString;
				} else {
					fn = f.getAbsolutePath();
					dataString = dataString + "\r\n" + fn;
					inputType = 4;
				}
				datesMap.put(f.lastModified(), fileCount);
				byModDates.put(fileCount,fn);
			}
			
			// Sort files by modDate
			if (inputType == 4) {
				dataString = "";
				SortedSet<Long> datesSet = (SortedSet<Long>) datesList.keySet();
				System.out.println("fileCount = " + fileCount + ", datesSet.size() = " + datesSet.size());
				Iterator<Long> ixit = datesSet.iterator(); 
				if (datesSet.size() > 0) {
					while (ixit.hasNext()) {
						Long modDate = ixit.next();
						Integer fileIndex = datesList.get(modDate);
						fn = byModDates.get(fileIndex);
						dataString = dataString + "\r\n" + fn;
					}
				}
			}

			System.out.println("NS: was javaFileListFlavor; Inputtype = " + inputType);
			step2();
			return true;
		}

		//  HTML String?

		try {
			htmlSelectionFlavor = new DataFlavor("text/html;charset=utf-8;class=java.io.InputStream");
			// "unicode" was wrong: yielded space after each character, 
			// and HTMLEditorKit.ParserCallback() recognized nothing but body p-implied
		} catch (ClassNotFoundException cle) {
			System.out.println("Error NS111 " + cle);
			return false;
		}
		if (content.isDataFlavorSupported(htmlSelectionFlavor)) {
			try {
				InputStream in = (InputStream) content.getTransferData(htmlSelectionFlavor);
				dataString = convertStreamToString(in);
				in.close();
				dataStringResort = (String) content.getTransferData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException e1) {
				System.out.println("Error NS112 " + e1);
				return false;
			} catch (IOException e1) {
				System.out.println("Error NS113 " + e1);
				return false;
			}
			System.out.println("NS: was htmlSelectionFlavor: \r\n");
			inputType = 3;
			step2();
			return true;
		} 

		//	Plain string ?

		if (content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				dataString = (String) content.getTransferData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException e) {
				System.out.println("Error NS114 " + e);
				return false;
			} catch (IOException e) {
				System.out.println("Error NS115 " + e);
				return false;
			}
			if (!dataString.contains("\n")) System.out.println("NS: was stringFlavor: \r\n" + dataString);
			else System.out.println("NS: was stringFlavor.");
			inputType = 2;
			step2();
			return true;

		// Nothing ?
			
		} else {
			controler.displayPopup("Nothing appropriate found in the Clipboard");
			return false;
		}
	}
	
	// Accessory for html flavored dropping and pasting, and for processSimplefiles

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
//					new InputStreamReader(is, "UTF-8"));
// 					changed to allow windows's exotic Charset.defaultCharset() for simple files
   					new InputStreamReader(is, charset));	

    		int n;
    		try {
    			while ((n = reader.read(buffer)) != -1) {
    				writer.write(buffer, 0, n);
    			}
    		} catch (IOException e) {
    			System.out.println("Error NS117 " + e);
    			try {
    				writer.close();
    			} catch (IOException e1) {
    				System.out.println("Error NS118 " + e1);
    			}
    		} finally {
    			try {
    				is.close();
    			} catch (IOException e) {
    				System.out.println("Error NS119 " + e);
    			}
    		}
    		String convertedString = writer.toString();
    		if (!convertedString.contains("\n")) System.out.println("NS converted: " + convertedString);
    		return convertedString;
    	} else {        
    		return "";
    	}
    }
    private String convertStreamToString(InputStream is) {
    	return convertStreamToString(is, Charset.forName("UTF-8"));
    }
    	
//
//	Process the input, in particular see whether it is a ready map in some xml format.
    
    public void step2() {
    	System.out.println("NS proceeding with inputType = " + inputType);
    	newNodes.clear();
    	newEdges.clear();
    	minX = Integer.MAX_VALUE;
    	maxX = Integer.MIN_VALUE;
    	minY = Integer.MAX_VALUE;
    	maxY = Integer.MIN_VALUE;
    	
		readyMap = processInput();	// opposite is raw stuff
		System.out.println("NS ready map ? " + readyMap);
		System.out.println("NS compositionMode ? " + compositionMode);
//		System.out.println("NS dataString: \r\b" + dataString);
    	if (inputType == 3) dataString = filterHTML(dataString);
    	if (inputType > 3) dataString = processSimpleFiles();
		if (compositionMode) {
			if (firstComposition) {
				firstComposition = false;
			} else {
				readyMap = false;
			}
	    	controler.getCWInstance().insertSnippet(dataString);
		} else step3();
   	
    }

	private boolean processInput() {
		Document doc = null;
		boolean oldFormat = false;
		
		if (!dataString.contains("\n")) {
			System.out.println("NewStuff().processInput(" + dataString + ", " + this.inputType + ") started");
		} else {
			System.out.println("NewStuff().processInput((input), " + this.inputType + ") started");
		}
		
		if (!unpack()) {
			if (this.inputType != 2 && this.inputType != 3) {
				try {
					stream = new FileInputStream(dataString);
					doc = getParsedDocument(stream);
					System.out.println("NS: found doc from fileinputstream");
				} catch (FileNotFoundException e2) {
					System.out.println("Error NS127 "  + e2);
					return false;
				}
			} else {
				doc = getParsedDocument(dataString);
				System.out.println("NS: found doc from string");
			}
		} else {
			doc = getParsedDocument(stream);
			System.out.println("NS: found doc from zip inputstream");
		}
		
		newNodes.clear();
		newEdges.clear();
		
		topicnum = 0;
		assocnum = 0;

		//
		//	Which type of XML file? 

		if (doc.hasChildNodes()) {
			root = doc.getDocumentElement();
			if (root.getTagName() == "x28map") {
				System.out.println("NS Success: new" );
				readyMap = true;
			} else if (root.getTagName() == "topicmap") {
				System.out.println("NS Success: old");
				oldFormat = true;
				readyMap = true;
				System.out.println("NS Starting otherXML ?");
				otherXML();
				System.out.println("NS Finished otherXML ?");
			} else {
				System.out.println("NS Failure.");
				return false;
			}
			
			//	New format
			
			if (!oldFormat) {
				NodeList topics = root.getElementsByTagName("topic");
				NodeList assocs = root.getElementsByTagName("assoc");
				for (int i = 0; i < topics.getLength(); i++) {
					importTopic((Element) topics.item(i));
				}
				for (int i = 0; i < assocs.getLength(); i++) {
					importAssoc((Element) assocs.item(i));
				}
				System.out.println("NS: " + topicnum + " new topics and " + assocnum + " new assocs loaded");
			}
			
			System.out.println("NS.processInput created " + newNodes.size() + " nodes and " + newEdges.size() + " edges");
			return true;

		} else {
			System.out.println("NS: not XML");
			
			{
				if (inputType != 1) return false;
				try {
					stream = new FileInputStream(dataString);
				} catch (FileNotFoundException e) {
					System.out.println("NS Error 126 " + e);
				}
				dataString = convertStreamToString(stream);
			}
			
			return false;
		}
	}

//
//	Auxiliary methods for processInput()
	
	//	Unpack zip files
	
	private boolean unpack() {
		Charset CP850 = Charset.forName("CP850");
		if (inputType != 1) return false;
		File file = new File(dataString);
		boolean success = false;	// success is a single leaf entry
		ZipFile zfile = null;
		String filelist = "";
		int entryCount = 0;
		if (!dataString.contains("\n")) {
			System.out.println("NewStuff().unpack with " + dataString + " and type " + this.inputType + " started");
		} else {
			System.out.println("NewStuff().unpack with (dataString) and type " + this.inputType + " started");
		}
		try {
			zfile = new ZipFile(file,CP850);
			Enumeration<? extends ZipEntry> e = zfile.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				String filename = entry.getName();
				System.out.println("----------- " + filename);
				filename = filename.replace('\\', '/');		
				if (filename.indexOf("icons/") != -1) {	// very old deepamehta2 format
					continue;
				} else	{
					if (entryCount == 0) {
						filelist = filename;	// to avoid leading newline
					} else {
						filelist = filelist + "\r\n" + filename;
					}
					System.out.println("NewStuff.unpack found " + filename + ", hopefully not more");
					stream = zfile.getInputStream(entry);
					entryCount++;
				}
			}
			if (entryCount == 1) {
				success = true;
			} else {
				dataString = filelist;
				inputType = 5;
				success = false;
				advisableFilename = "";
			}
//			zfile.close();
		} catch (ZipException e1) {
			System.out.println("Error NS121 " + e1);
			success = false;
		} catch (IOException err) {
			System.out.println("Error NS122 " + err);
			success = false;
		}
		System.out.println("NewStuff().unpack returned " + success);
		return success;

	}
	
	//	Methods to try if XML 
	
	private Document getParsedDocument(InputStream stream) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = null; 
		try  {
			parser = dbf.newDocumentBuilder(); 
			return parser.parse(stream);
		} catch (Exception e) {
			System.out.println("Error NS124 (getParsedDocument from stream): " + e);
			return parser.newDocument();
		}
	}

	private Document getParsedDocument(String input) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		InputStream stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
		DocumentBuilder parser = null; 
		try  {
			parser = dbf.newDocumentBuilder(); 
			return parser.parse(stream);
		} catch (Exception e) {
			System.out.println("Error NS125 (getParsedDocument from string): " + e);
			return parser.newDocument();
		}
	}

	//	Detail methods for current map xml format
	//	Nodes & edges are called "topics" and "assocs" to distinguish from xml nodes
	
	private void importTopic(Element topic) {
		GraphNode node;
		topicnum++;

		String id = topic.getAttribute("ID");
		String label = topic.getFirstChild().getTextContent();
		String detail = topic.getLastChild().getTextContent();
		int x = Integer.parseInt(topic.getAttribute("x"));
		int y = Integer.parseInt(topic.getAttribute("y"));
		if (x < minX) minX = x;
		if (x > maxX) maxX = x;
		if (y < minY) minY = y;
		if (y > maxY) maxY = y;
		String color = topic.getAttribute("color");
	
//		System.out.println("NS: id = " + id + ", label = " + label + ", detail has length " + detail.length() +
//				", x = " + x + ", y = " + y + ", color = " + color);

		node = new GraphNode(topicnum, new Point(x,y), Color.decode(color), label, detail);
		newNodes.put(node.getID(), node);
	}

	private void importAssoc(Element assoc) {
		GraphEdge edge;
		assocnum++;

		String detail = assoc.getFirstChild().getTextContent();
		int n1 = Integer.parseInt(assoc.getAttribute("n1"));
		int n2 = Integer.parseInt(assoc.getAttribute("n2"));
		String color = assoc.getAttribute("color");
	
//		System.out.println("NS: detail has length " + detail.length() +
//				", node1 = " + newNodes.get(n1).getLabel() + ", node2 = " + newNodes.get(n2).getLabel() + ", color = " + color);

		edge = new GraphEdge(assocnum, newNodes.get(n1), newNodes.get(n2), Color.decode(color), detail);
		newEdges.put(assocnum, edge);
		newNodes.get(n1).addEdge(edge);
		newNodes.get(n2).addEdge(edge);
	}

	//	Old map xml format and others 
	
	private boolean otherXML() {
		NodeList topicsAndAssocs = root.getChildNodes();
		System.out.println("Reading in " + topicsAndAssocs.getLength() + " items");
		
		// Read structure, save in intermediate arrays, and record old ID to new ID mapping
		
		for (int i = 0; i < topicsAndAssocs.getLength(); i++) {	
			Node node = topicsAndAssocs.item(i);
			String topicid ="";
			int r, g, b;
			r = g = b = 0;
			String hex = "";
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				isAssoc = false;
				if (topicsAndAssocs.item(i).getNodeName() == "assoc") {
					isAssoc = true;
				} else if (topicsAndAssocs.item(i).getNodeName() == "topic") {
					isAssoc = false;
				} else continue;
				if (!isAssoc) nodenum++; else edgenum++;

//				System.out.println(i + ": " + topicsAndAssocs.item(i).getNodeName());
//				System.out.println(((Element) topicsAndAssocs.item(i)).getTagName());
				NamedNodeMap attrs = topicsAndAssocs.item(i).getAttributes();
				
				//	Attributes
				
				for (int j = 0; j < attrs.getLength(); j++) {
					if (attrs.item(j).getNodeName().equals("ID")) {
						topicid = attrs.item(j).getNodeValue();
//						System.out.println("j=" + j + ": " + topicid);
						if (!isAssoc) {
							nodeids.put(topicid, nodenum);
						} else {
							edgeids.put(topicid, edgenum);
						}
					}
					if (attrs.item(j).getNodeName().equals("x")) {
						if (!isAssoc) nodesArray[nodenum][0] = attrs.item(j).getNodeValue();
						else edgesArray[edgenum][0] = attrs.item(j).getNodeValue();
					}
					if (attrs.item(j).getNodeName().equals("y")) {
						if (!isAssoc) nodesArray[nodenum][1] = attrs.item(j).getNodeValue();
						else edgesArray[edgenum][1] = attrs.item(j).getNodeValue();
					}
					if (attrs.item(j).getNodeName().equals("r")) {
						r = Integer.parseInt(attrs.item(j).getNodeValue());
					}
					if (attrs.item(j).getNodeName().equals("g")) {
						g = Integer.parseInt(attrs.item(j).getNodeValue());
					}
					if (attrs.item(j).getNodeName().equals("b")) {
						b = Integer.parseInt(attrs.item(j).getNodeValue());
					}
					
					//  Very old (DeepaMehta 2) colors ?
					if (attrs.item(j).getNodeName().equals("color")) {
						if (hex == "") hex = attrs.item(j).getNodeValue().toLowerCase();
					}
					if (attrs.item(j).getNodeName().equals("icon")) {
						String oldIcon = attrs.item(j).getNodeValue();
						if (oldIcon.length() > 12) {
							boolean properHex = true;
							String oldRgb  = oldIcon.substring(3, 9);    //  tt-rrggbb.png
							for (int pos = 0; pos < 6; pos++) {
								if (!oldRgb.substring(pos, pos+1).matches("[0-9a-f]")) {
									properHex = false;
									break;
								}
							}
							if (properHex && hex == "") hex = "#" + oldRgb.toLowerCase();
						}
					}
				} 	//	Next Attribute

				if (hex == "") hex = String.format("#%02x%02x%02x", r, g, b);
					
				if (!isAssoc) nodesArray[nodenum][2] = hex;
				else edgesArray[edgenum][2] = hex;
				
				// Children nested?
				
				if (topicsAndAssocs.item(i).hasChildNodes()) {
//					System.out.println("Has children");
					
					nest(topicsAndAssocs.item(i), "", topicid);
				}

			} else {
				System.out.println(i + " not an Element but: " + node.getNodeType());
			}	//	End of element nodes 

		}
		
		//	Processing the topics

		System.out.println("NS: nodenum = " + (nodenum + 1));
		for (int i = 0; i < nodenum + 1; i++) {
//			System.out.println("---- x = " + nodesArray[i][0] + ", y = " + nodesArray[i][1] + ", rgb = " + nodesArray[i][2] 
//					+ ", label = " + nodesArray[i][3] + ", detail length = " + nodesArray[i][4].length());
			int x = Integer.parseInt(nodesArray[i][0]);
			int y = Integer.parseInt(nodesArray[i][1]);
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
			GraphNode node = new GraphNode(i, 	// ID
					new Point(x, y),
					Color.decode(nodesArray[i][2]), 
					nodesArray[i][3], 	// label
					nodesArray[i][4]);	// detail
//			System.out.println("newNodes contains " + newNodes.size() + " items");
			newNodes.put(i, node);
//			System.out.println("newNodes contains now " + newNodes.get(i) + " as key " + i);
		}

		// Processing the assocs
		
		System.out.println("NS: edgenum = " + (edgenum + 1));
		for (int i = 0; i < edgenum + 1; i++) {
//			System.out.println("---- n1 = " + edgesArray[i][0] + ", n2 = " + edgesArray[i][1] + ", rgb = " + edgesArray[i][2]);
			int n1 = Integer.parseInt(edgesArray[i][0]);
			int n2 = Integer.parseInt(edgesArray[i][1]);
			if (!newNodes.containsKey(n1) || !newNodes.containsKey(n2)) {
				System.out.println("NS: " + i + "-th edge not created");
				continue;	//  edgesArray[][] == -1 signals when input data corrupt
			}
			GraphEdge edge = new GraphEdge(i, newNodes.get(n1), 
										   newNodes.get(n2),
										   Color.decode(edgesArray[i][2]), 
											"");			// detail TODO
//			System.out.println("newEdges contains " + newEdges.size() + " items");
//			edge.setID(i);
			newEdges.put(i, edge);
			newNodes.get(n1).addEdge(edge);
			newNodes.get(n2).addEdge(edge);
//			System.out.println("newEdges contains now " + newEdges.get(i) + " as key " + i);
		}
		readyMap = true;
		return true;
	}
	
	//	Accessory for Reading the XML 
	
	private void nest(Node parent, String indent, String topicid) {
		NodeList children = parent.getChildNodes();
		Node child;
		String detail = "";
		String label = "";
		String parentname = parent.getNodeName();
		String owlDetail = "";

		// By entry type
		
		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			String name = child.getNodeName();
			
			// TODO move elsewhere
			if (child.getNodeType() == Node.TEXT_NODE) {
				if (parentname == "rdfs:comment") {
					owlDetail = owlDetail + "<p>" + child.getNodeValue();
					owlDetail = owlDetail.replaceAll("\n"," ");
//					System.out.println(owlDetail);
				}
			}
			if (name == "#text") continue; 
			
			if (child.getNodeType() == Node.ELEMENT_NODE) {
//				System.out.println(indent + ((Element) child).getNodeName());
				parentname = parentname.substring(parentname.indexOf(":")+1);

				NamedNodeMap attrs = child.getAttributes();
				for (int j = 0; j < attrs.getLength(); j++) {
					Node attr = attrs.item(j);
					if (attr.getNodeName() == "rdf:about") {
						String about = attr.getNodeValue();
						int ind = about.indexOf("#");
						if (ind > -1) about = about.substring(ind + 1);
						owlDetail = owlDetail + "<p><b>" + parentname + ": " + about + "</b>";
					}
					if (attr.getNodeName() == "rdf:resource") {
						String about = attr.getNodeValue();
						int ind = about.indexOf("#");
						if (ind > -1) about = about.substring(ind + 1);
						owlDetail = owlDetail + "<p><b>" + child.getNodeName() + ": " + about + "</b>";
					}
//					System.out.println(owlDetail);
				}
				
				// Empty label 
				if (child.getNodeName() == "basename" && child.getChildNodes().getLength() < 1) {
					label = "";
					nodesArray[nodeids.get(topicid)][3] = label;
				}
			}
			
//			//	CDATA_SECTION_NODE or unusual types
//			if (child.getNodeType() != Node.ELEMENT_NODE) {
//				System.out.println(indent + child.getNodeName());
//			} 
			
			//	Main types
			
			if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
//				System.out.println("Text length of " + topicid + ": " + child.getNodeValue().length());
//				System.out.println(topicid + ": " + child.getNodeValue());
				
				//	Label
				if (parent.getNodeName() == "basename") {
					label = child.getNodeValue().toString();
					nodesArray[nodeids.get(topicid)][3] = label;
				} 
				
				//	Detail
				if (parent.getNodeName() == "description" && !isAssoc) {
					detail = child.getNodeValue().toString();
					nodesArray[nodeids.get(topicid)][4] = detail;
				}
				//	Detail if very old (Deepamehta 2) format, TODO refine or remove
				if (parent.getNodeName() == "property" && !isAssoc) {
					detail = child.getNodeValue().toString();
					nodesArray[nodeids.get(topicid)][4] = detail;
				}
				
				//	Ends of edge
				if (parent.getNodeName() == "assocrl") {
					NamedNodeMap assocattrs = parent.getAttributes();
					int whichend = Integer.parseInt(assocattrs.item(0).getNodeValue().substring(8));
					String end = child.getNodeValue();
					if (nodeids.containsKey(end)) {
						edgesArray[edgeids.get(topicid)][whichend - 1] = nodeids.get(child.getNodeValue()).toString();
					} else {	// Corrupted; key -1 will skip GraphEdge creation 
						System.out.println("NS: End node "+ end + " missing");	
						edgesArray[edgeids.get(topicid)][whichend - 1] = "-1";
					}
				}
			}
			String name2 = child.getNodeName();		
			if (name2 != "owl:Restriction") { 	// stub
//					&& name2 != "rdfs:inverseOf" 
//					&& name2 != "rdfs:disjointWith" 
//					&& name2 != "owl:equivalentClass") {
				nest(child, "  " + indent, topicid);
			}
		}
//		return true;
	}
	
	//	Stuff from html lists

	private String filterHTML(String html) {
		if (inputType != 3) return html;
		listItem = false;
		htmlOut = "";
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleEndTag(HTML.Tag t, int pos) {
//				System.out.println("</" + t + "> on pos " + pos);
				if (t.toString() == "li") {
					listItem = false;
					htmlOut = htmlOut  + "\t\r\n";
				} else if (t.toString() == "tr") {
					tableRow = false;
					htmlOut = htmlOut  + "\r\n";
				} else if (t.toString() == "td") {
					tableCell = false;
					firstColumn = false;
				}

			}
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
//				System.out.println("Data " + dataString + " on pos " + pos);
				if (listItem && !tableCell) htmlOut = htmlOut + dataString;
				if (tableRow && tableCell) {
					if (firstColumn) {
						htmlOut = htmlOut + dataString  + "\t";
						firstColumn = false;
					} else {
						htmlOut = htmlOut + dataString  + "<br />";
					}
				}
			}
			public void handleComment(char[] data, int pos) {
				String dataString = new String(data);
//				System.out.println("<-- " + dataString + " --> on pos " + pos);
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				Enumeration<?> attrs = a.getAttributeNames();
//				System.out.println("<" + t + "> on pos " + pos);
				if (t.toString() == "li") {
					if (listItem) htmlOut = htmlOut  + "\t\r\n";
					listItem = true;
				} else if (t.toString() == "tr") {
					tableRow = true;
					firstColumn = true;
				} else if (t.toString() == "td") {
					tableCell = true;
				}

//				while (attrs.hasMoreElements()) System.out.println("  attr: " +  attrs.nextElement());
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error PS101 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error PS102 " + e3.toString());
		}
		if (htmlOut == "") {
			controler.displayPopup("No list items found in HTML snippet,\r\n"
				+ "using raw input string instead.");
			htmlOut = dataStringResort;
			dataStringResort = "";
		}
		return htmlOut;
	}
    private static class MyHTMLEditorKit extends HTMLEditorKit {
    	private static final long serialVersionUID = 7279700400657879527L;

    	public Parser getParser() {
    		return super.getParser();
    	}
    }
    
    //	Try to make filenames clickable, or even include their content
    private String processSimpleFiles() {
    	String textStr[] = dataString.split("\\r?\\n");
    	String output = "";
    	
    	for (int i = 0; i < textStr.length; i++) {
    		boolean success = false;
        	String shortName = "";
    		String contentString = "";

    		String line = textStr[i];
  			File f = new File(line);
  			
     		if (inputType == 5 || !f.exists() || f.isDirectory()) {

     			//	Just list the filename
     			//	TODO replace by utility (test with Mac)
				shortName = line.replace('\\', '/');	
				if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
       			shortName = shortName.substring(shortName.lastIndexOf("/") + 1);
    			line = shortName + "\t" + line;
    			output = output + line + "\r\n";

     		} else {

     			shortName = f.getName();
					if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
	       			shortName = shortName.substring(shortName.lastIndexOf("/") + 1);
    			String extension = shortName.substring(shortName.lastIndexOf("."));
    			System.out.println(shortName + " " + extension);

    			if (!extension.equals(".txt") && !extension.equals(".htm")) {
    				String esc = "";
    				line = f.toURI().toString();
    				if (windows) line = line.replace("%C2%A0", "%A0"); // For bookmarks containing nbsp
    				line = shortName + "\t" + "<html><body>Open file <a href=\"" + line  + "\">" + shortName + "</a></body></html>";
    				output = output + line + "\r\n";

    			} else {

    				try {
    					stream = new FileInputStream(line);
    				} catch (FileNotFoundException e) {
    					System.out.println("Error NS123 " + e);
    					success = false;
    				}
    				try {
    					InputStream in = (InputStream) stream;
    					if (windows) {
//        					contentString = convertStreamToString(in, Charset.defaultCharset());
        					contentString = convertStreamToString(in, Charset.forName("Cp1252"));
    					} else {
        					contentString = convertStreamToString(in);
    					}
    					in.close();
    					success = true;
    				} catch (IOException e1) {
    					System.out.println("Error NS113 " + e1);
    					success = false;
    				}
    				if (success) {
    					contentString = contentString.replace("\n", "<br />");
    					contentString = contentString.replace("\t", " (TAB) ");  // TODO improve
    					line = shortName + "\t" + contentString;
    					output = output + line + "\r\n";
    				}
    			} 
       		}
    	}
    	dataString = output;
    	return dataString;
    }


	
//
//	DataString or Map are now ready. 
	
//	Create new nodes from the splitted string, and then trigger the caller 
//	to merge these new nodes or the new map with the existing or empty map 
	
	
	private void step3() {
		if (readyMap) {
			newNodes = fetchToCenter(newNodes);
		} else {
			SplitIntoNew splitIntoNew = new SplitIntoNew(controler);
			int newNodesCount = splitIntoNew.separateRecords(dataString);
			System.out.println("NS: " + newNodesCount + " new nodes created");
			splitIntoNew.heuristics(newNodesCount);
			splitIntoNew.createNodes(newNodesCount);	
			newNodes = splitIntoNew.getNodes();
			newEdges = splitIntoNew.getEdges();
			minX = 40;
			minY = 40;
			maxX = 40 + (newNodes.size()/11 + 1) * 150;
			maxY = Math.min(newNodes.size() * 40, 540);
		}
		
		//	Integrate new Nodes into existing graph
		System.out.println("NS calling triggerupdate, new nodes/ edges now: " + newNodes.size() + "/ " + newEdges.size());
		boolean justOneMap = false;
		if (inputType == 1 && readyMap) justOneMap = true;
		controler.triggerUpdate(justOneMap);
		readyMap = false;
	}

	public Hashtable<Integer, GraphNode> fetchToCenter(Hashtable<Integer,GraphNode> nodes) {
		Enumeration<GraphNode> e = nodes.elements();
		System.out.println("NS: Adjust needed? " + minX + " - " + maxX + ", " + minY + " - " + maxY);
		int adjustX = 0, adjustY = 0;
		if (minX < 0 || minX > 960) adjustX = 480 - (maxX + minX)/2;  // TODO determine actual window dimensions
		if (minY < 0 || minY > 580) adjustY = 290 - (maxY + minY)/2;  
		System.out.println("NS: Adjusting " + adjustX + ", " + adjustY);
		while (e.hasMoreElements()) {
			GraphNode node =e.nextElement();
			Point xy = node.getXY();
			xy.translate(adjustX, adjustY);;
		}
		return nodes;
	}
	
//
//	Communication with other classes
	
	public void scoopCompositionWindow(CompositionWindow compositionWindow) {
		dataString = compositionWindow.dataString;
		if (!dataString.contains("\r\n")) System.out.println("Scooped from oldPasteWindow: \r\n" + dataString );
		else System.out.println("CW: Scooped from CompositionWindow.");
		step3();
	}

	public void setCompositionMode(boolean toggle) {
		this.compositionMode = toggle;
	}
	
	public void setInput(String dataString, int inputType) {
		this.dataString = dataString;
		this.inputType = inputType;
		step2();
	}
	
	public String getString() {
		return dataString;
	}
	
	public Hashtable<Integer, GraphNode> getNodes() {
		System.out.println("NS returns new nodes/ edges now: " + newNodes.size() + "/ " + newEdges.size());
		return newNodes;
	}
	public Hashtable<Integer, GraphEdge> getEdges() {
		return newEdges;
	}
	
	public String getAdvisableFilename() {
		System.out.println("advisableFilename = " + advisableFilename);
		return advisableFilename;
	}

	public Point getInsertion() {
		return insertion;
	}
//
//	Misc
//	
//	Temporary map when input routines are not yet functional
	
	public void tmpInit() {

		GraphNode node;
		GraphEdge edge;

		node = new GraphNode(501, new Point(310,220), Color.blue, "Node 1", "Node 1 details");
		newNodes.put(node.getID(), node);
		node = new GraphNode(502, new Point(120,330), Color.blue, "Node 2", "Node 2 details");
		newNodes.put(node.getID(), node);
		node = new GraphNode(503, new Point(500,430), Color.blue, "Node 3", "Node 3 details");
		newNodes.put(node.getID(), node);

		edge = new GraphEdge(1, newNodes.get(501), newNodes.get(502), Color.blue, "Edge 1 details");
		newEdges.put(1, edge);
		newNodes.get(501).addEdge(edge);
		newNodes.get(502).addEdge(edge);
		edge = new GraphEdge(2, newNodes.get(503), newNodes.get(502), Color.blue, "Edge 2 details");
		newEdges.put(2, edge);
		newNodes.get(503).addEdge(edge);
		newNodes.get(502).addEdge(edge);
		
		controler.triggerUpdate(true);

	}
	
//
//	Stubs, abandon ?
	
	public Point roomNeeded() {
		return new Point(maxX - minX + 1, maxY - minY + 1);
	}
	public Point upperleft() {
		return new Point(minX + 1, minY + 1);
	}

}

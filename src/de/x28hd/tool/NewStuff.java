package de.x28hd.tool;

import java.awt.Point;
import java.awt.Rectangle;
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
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	
	//	HTML exploitation
	String htmlOut = "";
	boolean listItem = false;
	boolean tableRow = false;
	boolean tableCell = false;
	boolean firstColumn = true;
	String dataStringResort = "";
	boolean silentlyResort = false;
	
	//	Input file sorting
	Hashtable<Integer,String> byModDates = new Hashtable<Integer,String>();;
	TreeMap<Long,Integer> datesMap = new TreeMap<Long,Integer>();
	SortedMap<Long,Integer> datesList = (SortedMap<Long,Integer>) datesMap;


	
	public NewStuff(final GraphPanelControler controler) {
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
		System.out.println("NS canImport via " + diag);
		return true;
	}
	
	public boolean importData(TransferHandler.TransferSupport support, String diag) {
   	 
        if (!canImport(support, diag)) {
           return false;
        }

        Transferable t = support.getTransferable();
		insertion = new Point(support.getDropLocation().getDropPoint().x,
				support.getDropLocation().getDropPoint().y);
		System.out.println("NS Drop point " + support.getDropLocation().getDropPoint().x + ", " +
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
//	Obtain input in 3 different dataFlavors (file list, HTML, or string)
	
//	Then determine an inputType of the following:
//	1 = file list, single file
//	2 = string (including complete x28maps for dragging or from importers)
//	3 = HTML string
//	4 = file list, multiple files
//	and later:
//	5 = file list, multiple files, from ZIP
//	6 = pre-processed list (fit for Composition Window)
	
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
			if (l.size() == 1) {
				dataString = l.get(0).getAbsolutePath();
				inputType = 1;
				advisableFilename = dataString;
				System.out.println("NS: was javaFileListFlavor; Inputtype = " + inputType);
				interceptZips();
			} else {
				for (File f : l) {
					String fn = f.getAbsolutePath();
					dataString = dataString + "\r\n" + fn;
				}
				inputType = 4;
				readyMap = false;
				System.out.println("NS: was javaFileListFlavor; Inputtype = " + inputType);
				exploitFilelist();
			}
			return true;
		}

		//  HTML String?

		try {
			htmlSelectionFlavor = new DataFlavor("text/html;charset=utf-8;class=java.io.InputStream");
			// "unicode" was wrong: yielded space after each character, 
			// and HTMLEditorKit.ParserCallback() recognized nothing but body p-implied
			
			// Update June 2016: still works from Eclipse but not from Jar. Trying to
			// keep it until advice may come along
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
			inputType = 2;
			step2();
			return true;

		// Nothing ?
			
		} else {
			controler.displayPopup("Nothing appropriate found in the Clipboard");
			return false;
		}
	}
	
	
	public void step2() {
		if (inputType > 2) {
	    	if (inputType == 3) dataString = filterHTML(dataString);
			if (compositionMode) {
		    	controler.getCWInstance().insertSnippet(dataString);
		    	return;
			} 
			SplitIntoNew splitIntoNew = new SplitIntoNew(controler);
			int newNodesCount = splitIntoNew.separateRecords(dataString);
			splitIntoNew.heuristics(newNodesCount);
			splitIntoNew.createNodes(newNodesCount);	
			newNodes = splitIntoNew.getNodes();
			newEdges = splitIntoNew.getEdges();
			step3();
		} else {
			analyzeBlob();
		}
	}
	
	public void step3() {
		if (readyMap) {
			newNodes = fetchToUpperLeft(newNodes);
		}
		boolean existingMap = false;
		if (inputType == 1 && readyMap) existingMap = true;		
		controler.triggerUpdate(existingMap, true);
		readyMap = false;
	}
	
//
//	Detail methods
	
//
//	Get the most out of file lists
	
	public void exploitFilelist() {
		
		// Sort files by modDate
		if (inputType != 5) {
 	    	String textStr[] = dataString.split("\\r?\\n");
			byModDates.clear();
 			datesMap.clear();
 			int fileCount = 0;
 			for (int i = 0; i < textStr.length; i++) {
 				String line = textStr[i];
 				File f = new File(line);
 				if (!f.exists()) continue;
 				datesMap.put(f.lastModified(), fileCount);
 				byModDates.put(fileCount, line);
 				fileCount++;
 			}
 			dataString = "";
 			SortedSet<Long> datesSet = (SortedSet<Long>) datesList.keySet();
 			Iterator<Long> ixit = datesSet.iterator(); 
 			if (datesSet.size() > 0) {
 				while (ixit.hasNext()) {
 					Long modDate = ixit.next();
 					Integer fileIndex = datesList.get(modDate);
 					String fn = byModDates.get(fileIndex);
 					dataString = dataString + "\r\n" + fn;
 				}
 			}
 		}
		
		//	Process simple files
	    //	(Try to make filenames clickable, or even include their content)
    	String output = "";
    	String[] textStr = dataString.split("\\r?\\n");
    	for (int i = 0; i < textStr.length; i++) {
        	String shortName = "";
    		boolean success = false;
    		String contentString = "";
    		String line = textStr[i];
  			File f = new File(line);
  			
     		if (inputType == 5 || !f.exists() || f.isDirectory()) {
     			//	Just list the filename
     			//	TODO replace by utility (test with Mac)
				shortName = line.replace('\\', '/');	
				if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
    			line = shortName + "\t" + line;
    			output = output + line + "\r\n";
     		} else {
    			shortName = f.getName();
					if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
	       			shortName = shortName.substring(shortName.lastIndexOf("/") + 1);
    			String extension = shortName.substring(shortName.lastIndexOf("."));

    			if (!extension.equals(".txt") && !extension.equals(".htm")) {
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
    					System.out.println("Error NS116 " + e1);
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
			dataString = output;
		}

		step2();
	}
	
//
//	Intercept some peculiarities contained in ZIP files
	
	public void interceptZips() {
		Charset CP850 = Charset.forName("CP850");
		File file = new File(dataString);	//	Brute force testing for zip
		ZipFile zfile = null;
		String filelist = "";
		int entryCount = 0;
		try {
			zfile = new ZipFile(file,CP850);
			Enumeration<? extends ZipEntry> e = zfile.entries();
			boolean done = false;
			while (e.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				stream = zfile.getInputStream(entry);
				String filename = entry.getName();
				filename = filename.replace('\\', '/');		
				if (filename.equals("savefile.xml") || filename.startsWith("topicmap-t-")) {
					new ImportDirector(7, stream, controler); 
					done = true;
					break;
				} else if (filename.endsWith("content.cds.xml")) {
					new ImportDirector(1, file, controler); 
					done = true;
					break;
				} else if (filename.equals("word/document.xml")) {
					new ImportDirector(5, stream, controler); 
					done = true;
					break;
				} else	{
					if (entryCount == 0) {
						filelist = filename + "\r\n";	// to avoid leading newline
					} else {
						filelist = filelist + filename + "\r\n";
					}
					entryCount++;
				}
			}
			if (done) {
				zfile.close();
				return;
			}
			if (entryCount == 1) {
				dataString = convertStreamToString(stream);
				inputType = 2;
				advisableFilename = file.getAbsolutePath();
				step2();
			} else {
				dataString = filelist;
				inputType = 5;
				advisableFilename = "";
				exploitFilelist();
			}
//			zfile.close();
		} catch (ZipException e1) {
			System.out.println("Error NS121 (can be ignored) " + e1);
			step2();
		} catch (IOException err) {
			System.out.println("Error NS122 " + err);
			controler.displayPopup("Error NS122 " + err);
		}
	}

//
//	Large single object (as opposed to composed lists)
	
	public void analyzeBlob() {
		boolean hope = true;
		
		//	Try if XML format
		Document doc = null;
		if (inputType == 2) {
			doc = getParsedDocument(dataString);	//	TODO consolidate
		} else if (inputType == 1) {
			try {
				stream = new FileInputStream(dataString);
				doc = getParsedDocument(stream);
			} catch (FileNotFoundException e2) {
				System.out.println("Error NS127 (can be ignored) "  + e2);
				hope = false;
			}
		}
		if (doc.hasChildNodes()) {
			root = doc.getDocumentElement();
			
			//	Try if known XML format
			if (root.getTagName() == "x28map") {
				if (compositionMode) controler.getCWInstance().cancel();
				TopicMapLoader loader = new TopicMapLoader(doc, controler);
				bounds = loader.getBounds();
				System.out.println("Bounds after TL " + bounds);
				readyMap = true;
				newNodes = loader.newNodes;
				newEdges = loader.newEdges;
				step3();
				return;
			}
			String [] knownFormats = {
					"en-export", 
					"(not relevant)", 
					"kgif", 
					"cmap", 
					"BrainData",
					"w:document",
					"(not relevant",
					"topicmap"
					};
			for (int k = 0; k < knownFormats.length; k++) {
				if (root.getTagName() == knownFormats[k]) {
					if (k != 0 && k != 5 && k != 6) {		// Evernote, Word, Endnote
						if (compositionMode) {
							controler.getCWInstance().cancel();
						}
					}
					new ImportDirector(k, doc, controler);
					return;
				}
			}
		} else hope = false;
			
		//	No XML
		//	Endnote is not (yet) recognized, => works only via Wizard 
		if (!hope) {
			if (inputType == 1) {
				String flatFileContent = "";
				try {
					stream = new FileInputStream(dataString);
				} catch (FileNotFoundException e) {
					System.out.println("Error NS126 " + e);
					controler.displayPopup("Error NS126 File not found " + e);
					return;
				}
				flatFileContent = convertStreamToString(stream);
				dataString = flatFileContent;
			}
			inputType = 6;
			step2();
			return;
		}
	}
	
//
//	Stuff from HTML lists

	private String filterHTML(String html) {
		if (inputType != 3) return html;
		listItem = false;
		htmlOut = "";
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleEndTag(HTML.Tag t, int pos) {
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
				if (dataString.contains("w:WordDocument")) {
					silentlyResort = true;	//	Word uses styles instead of list items
				}
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
//				Enumeration<?> attrs = a.getAttributeNames();
				if (t.toString() == "li") {
					if (listItem) htmlOut = htmlOut  + "\t\r\n";
					listItem = true;
				} else if (t.toString() == "tr") {
					tableRow = true;
					firstColumn = true;
				} else if (t.toString() == "td") {
					tableCell = true;
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error NS128 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error NS129 " + e3.toString());
		}
		if (htmlOut == "") {
			if (!silentlyResort) {
				controler.displayPopup("No list items found in HTML snippet,\r\n"
						+ "using raw input string instead.");
			}
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
    
//	Determine upper left visible corner
	public Point determineCorner(Hashtable<Integer,GraphNode> nodes) {

		int maxX = bounds.x + bounds.width;
		int minY = bounds.y;
		int minXtop = maxX - bounds.width/2;
		if (bounds.width < 726) {	//	graphPanel width, 960 window - 232 right pane
			minXtop = maxX - bounds.width/2;
		}
		Enumeration<GraphNode> nodesEnum = newNodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Point xy = node.getXY();
			int x = xy.x;
			int y = xy.y;
			if (y < minY + 100) {
				if (x < minXtop) minXtop = x;
			}
		}
		return new Point(minXtop - 40, minY - 40);
	}

	public Hashtable<Integer, GraphNode> fetchToUpperLeft(Hashtable<Integer,GraphNode> nodes) {
		Point adjust = determineCorner(nodes);
		Enumeration<GraphNode> e = nodes.elements();
		while (e.hasMoreElements()) {
			GraphNode node =e.nextElement();
			Point xy = node.getXY();
			xy.translate(- adjust.x, - adjust.y);;
		}
		return nodes;
	}
	

//
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
    		return convertedString;
    	} else {        
    		return "";
    	}
    }
    private String convertStreamToString(InputStream is) {
    	return convertStreamToString(is, Charset.forName("UTF-8"));
    }
    	
	//	Methods to try if XML 
	
	private Document getParsedDocument(InputStream stream) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = null; 
		try  {
			parser = dbf.newDocumentBuilder(); 
			return parser.parse(stream);
		} catch (Exception e) {
			System.out.println("Error NS124 (in getParsedDocument from stream): " + e);
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
			System.out.println("Error NS125 (in getParsedDocument from string): " + e);
			return parser.newDocument();
		}
	}

//
//	Communication with other classes
    
	public void setInput(String dataString, int inputType) {
		this.dataString = dataString;
		this.inputType = inputType;
		if (inputType == 1) {
			advisableFilename = dataString;
			interceptZips();
		}
		else if (inputType == 4) exploitFilelist();
		else if (inputType == 2) analyzeBlob();
		else step2();
	}
	
	public Hashtable<Integer, GraphNode> getNodes() {
		System.out.println("NS returns new nodes/ edges now: " + newNodes.size() + "/ " + newEdges.size());
		return newNodes;
	}
	public Hashtable<Integer, GraphEdge> getEdges() {
		return newEdges;
	}
	
	public void setCompositionMode(boolean toggle) {
		this.compositionMode = toggle;
	}
	
	public void scoopCompositionWindow(CompositionWindow compositionWindow) {
		dataString = compositionWindow.dataString;
		inputType = 6;
		step2();
	}
	
	public String getAdvisableFilename() {
		return advisableFilename;
	}
	
	public Point getInsertion() {
		return insertion;
	}

}

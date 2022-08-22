package de.x28hd.tool.importers;

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

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.MyHTMLEditorKit;
import de.x28hd.tool.PresentationExtras;
import de.x28hd.tool.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class NewStuff {
	
	
	//	Main fields
	Hashtable<Integer, GraphNode> newNodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> newEdges = new Hashtable<Integer, GraphEdge>();
	Point dropLocation = null;
	
	//	About caller (PresentationService()) and CompositionWindow()
	private PresentationService controler;
	PresentationExtras controlerExtras;
	boolean compositionMode = false;
	
	//	Input
	public static DataFlavor htmlSelectionFlavor;
	String advisableFilename = "";

	
	//	Map loading
	boolean existingMap = false;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	
	//	HTML exploitation
	boolean parseMode = false; 
	boolean dropEncoding = true;
	
//	String htmlOut = "";
//	boolean listItem = false;
//	boolean firstColumn = true;
	String dataStringResort = "";
//	boolean silentlyResort = false;
//	boolean belowHeading = false;
//	boolean htmlNoise = false;
//	boolean structureFound = false;
//	boolean listStructure = false;
	

	
	public NewStuff(final PresentationService controler) {
		this.controler = controler;
	}
	
//
//	Handlers for dropped and pasted stuff
	
//	canImport and importData are called by drop from GraphPanel and CompositionWindow,
//	readClipboard is called from Paste operations in PresentationExtras and 
//	CompositionWindow. 
	
	public boolean canImport(TransferHandler.TransferSupport support, String diag) {
		support.setDropAction(TransferHandler.COPY);
//        support.setShowDropLocation(true);    // no effect ?
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) && 
			!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
//			System.out.println("NS dataflavors: " + support.getDataFlavors() + " " + support.getDataFlavors().length);
			DataFlavor[] df = support.getDataFlavors();
			for (int i = 0; i < df.length; i++) {
				System.out.println("NS dataflavors: " + df[i].getHumanPresentableName());
			}
        	return false;
        }
//		System.out.println("NS canImport via " + diag);
		return true;
	}
	
	public boolean importData(TransferHandler.TransferSupport support, String diag) {
   	 
        if (!canImport(support, diag)) {
           return false;
        }

        Transferable t = support.getTransferable();
		dropLocation = new Point(support.getDropLocation().getDropPoint().x,
				support.getDropLocation().getDropPoint().y);
//		System.out.println("NS Drop point " + support.getDropLocation().getDropPoint().x + ", " +
//				support.getDropLocation().getDropPoint().y);
       
        boolean success = transferTransferable(t);
		if (!success) {
			System.out.println("Error NS120.");
		}
        return success;
	}

	public Transferable readClipboard() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		dropLocation = null;
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
		String dataString = "";			
		Utilities utilities = new Utilities();
		
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
				advisableFilename = dataString;
				System.out.println("NS: was javaFileListFlavor, 1 file");
				
//				interceptZips(dataString);
				new InterceptZips(dataString, controler, dropEncoding, compositionMode);
			} else {
				for (File f : l) {
					String fn = f.getAbsolutePath();
					dataString = dataString + "\r\n" + fn;
				}
				System.out.println("NS: was javaFileListFlavor, > 1 files");
				
//				exploitFilelist(dataString, true);
				new ExploitFilelist(dataString, true, dropEncoding, compositionMode, controler);
			}
			return true;
		}

		//  HTML String?

		if (parseMode && content.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor)) {
			try {
				dataString = (String) content.getTransferData(DataFlavor.fragmentHtmlFlavor);
				dataStringResort = (String) content.getTransferData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException e1) {
				System.out.println("Error NS112a " + e1);
				return false;
			} catch (IOException e1) {
				System.out.println("Error NS113a " + e1);
				return false;
			}
			System.out.println("NS: was fragmentHtmlFlavor: \r\n");
			
//			step2b(dataString, true);
			new Step2b(dataString, true, compositionMode, controler);
			return true;
		} else {
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
				dataString = utilities.convertStreamToString(in);
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
			
//			step2b(dataString, true);
			new Step2b(dataString, true, compositionMode, controler);
			return true;
		} 
		}

		//	Plain string ?

		if (content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				dataString = (String) content.getTransferData(DataFlavor.stringFlavor);
				System.out.println("NS: was stringFlavor: \r\n");
			} catch (UnsupportedFlavorException e) {
				System.out.println("Error NS114 " + e);
				return false;
			} catch (IOException e) {
				System.out.println("Error NS115 " + e);
				return false;
			}
//			analyzeBlob(dataString, false);
			new AnalyzeBlob(dataString, false, compositionMode, controler);
			return true;

		// Nothing ?
			
		} else {
			controler.displayPopup("Nothing appropriate found in the Clipboard");
			return false;
		}
	}
	
//	public void step2b(String dataString, boolean html) {
//	    	if (html) dataString = filterHTML(dataString);
//			if (compositionMode) {
//		    	controlerExtras.getCWInstance().insertSnippet(dataString);
//		    	return;
//			} 
//			SplitIntoNew splitIntoNew = new SplitIntoNew(controler);
////			int newNodesCount = splitIntoNew.separateRecords(dataString);
//			int newNodesCount = splitIntoNew.separateRecords2(dataString);
//			splitIntoNew.heuristics(newNodesCount);
//			splitIntoNew.createNodes(newNodesCount);	
//			newNodes = splitIntoNew.getNodes();
//			newEdges = splitIntoNew.getEdges();
//			step3b();
//	}
	
//	public void step3a() {
//		newNodes = fetchToUpperLeft(newNodes);
//		existingMap = true;	
////		step3b();
//		new Step3b(controler);
//	}
	
//	public void step3b() {
//		controlerExtras.triggerUpdate();
//		dropLocation = null;
//	}
	
//
//	Detail methods
	
//
//	Get the most out of file lists
	
//	public void exploitFilelist(String dataString, boolean withDates) {
//		InputStream stream = null;
//		Utilities utilities = new Utilities();
//		boolean windows = (System.getProperty("os.name").startsWith("Windows"));
//		//	Input file sorting
//		Hashtable<Integer,String> byModDates = new Hashtable<Integer,String>();;
//		TreeMap<Long,Integer> datesMap = new TreeMap<Long,Integer>();
//		SortedMap<Long,Integer> datesList = (SortedMap<Long,Integer>) datesMap;
//		
//		// Sort files by modDate
//		if (withDates) {
// 	    	String textStr[] = dataString.split("\\r?\\n");
//			byModDates.clear();
// 			datesMap.clear();
// 			int fileCount = 0;
// 			for (int i = 0; i < textStr.length; i++) {
// 				String line = textStr[i];
// 				File f = new File(line);
// 				if (!f.exists()) continue;
// 				long modDate = f.lastModified();
// 				while (datesMap.containsKey(modDate)) modDate++;
// 				datesMap.put(modDate, fileCount);
// 				byModDates.put(fileCount, line);
// 				fileCount++;
// 			}
// 			dataString = "";
// 			SortedSet<Long> datesSet = (SortedSet<Long>) datesList.keySet();
// 			Iterator<Long> ixit = datesSet.iterator(); 
// 			if (datesSet.size() > 0) {
// 				while (ixit.hasNext()) {
// 					Long modDate = ixit.next();
// 					Integer fileIndex = datesList.get(modDate);
// 					String fn = byModDates.get(fileIndex);
//// 					dataString = dataString + "\r\n" + fn;
// 					dataString = dataString + fn + "\r\n";
// 				}
// 			}
// 		}
//		
//		//	Process simple files
//	    //	(Try to make filenames clickable, or even include their content)
//    	String output = "";
//    	String[] textStr = dataString.split("\\r?\\n");
//    	for (int i = 0; i < textStr.length; i++) {
//        	String shortName = "";
//    		boolean success = false;
//    		String contentString = "";
//    		String line = textStr[i];
//  			File f = new File(line);
//  			
//     		if (!withDates || !f.exists() || f.isDirectory()) {
//     			//	Just list the filename
//     			//	TODO replace by utility (test with Mac)
//				shortName = line.replace('\\', '/');	
//				if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
//    			line = shortName + "\t" + line;
//    			output = output + line + "\r\n";
//     		} else {
//    			shortName = f.getName();
//					if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
//	       			shortName = shortName.substring(shortName.lastIndexOf("/") + 1);
//    			int extOffset = shortName.lastIndexOf(".");
//    			String extension = "";
//    			if (extOffset > 0) extension = shortName.substring(extOffset);
//
//    			if (!extension.equals(".txt") && !extension.equals(".htm")) {
//    				line = f.toURI().toString();
//    				if (windows) line = line.replace("%C2%A0", "%A0"); // For bookmarks containing nbsp
//    				line = shortName + "\t" + "<html><body>Open file <a href=\"" + line  + "\">" + shortName + "</a></body></html>";
//    				output = output + line + "\r\n";
//
//    			} else {
//
//    				try {
//    					stream = new FileInputStream(line);
//    				} catch (FileNotFoundException e) {
//    					System.out.println("Error NS123 " + e);
//    					success = false;
//    				}
//    				try {
//    					InputStream in = (InputStream) stream;
//    					if (windows) {
//    						if (dropEncoding) {
//    							contentString = utilities.convertStreamToString(in, Charset.forName("UTF-8"));
//    						} else {
//        					contentString = utilities.convertStreamToString(in, Charset.forName("Cp1252"));
//    						}
//						} else {
//        					contentString = utilities.convertStreamToString(in);
//    					}
//    					in.close();
//    					success = true;
//    				} catch (IOException e1) {
//    					System.out.println("Error NS116 " + e1);
//    					success = false;
//    				}
//    				if (success) {
////    					contentString = contentString.replace("\n", "<br />");
//    					contentString = contentString.replaceAll("\\r?\\n", "<br />");
//    					contentString = contentString.replace("\t", " (TAB) ");  // TODO improve
//    					line = shortName + "\t" + contentString;
//    					output = output + line + "\r\n";
//    				}
//    			} 
//     		}
//			dataString = output;
//		}
//
//		step2b(dataString, false);	// html already handled
//	}
	
//
//	Intercept some peculiarities contained in ZIP files, plus folder trees
	
//	public void interceptZips(String dataString) {
//		Utilities utilities = new Utilities();
//		InputStream stream = null;
//		Charset CP850 = Charset.forName("CP850");
//		File file = new File(dataString);	//	Brute force testing for zip
//		if (new File(dataString).isDirectory()) {
//			new ImportDirector(Importer.Filetree, new File(dataString), controler);
//			return;
//		}
//		ZipFile zfile = null;
//		String filelist = "";
//		int entryCount = 0;
//		try {
//			zfile = new ZipFile(file,CP850);
//			Enumeration<? extends ZipEntry> e = zfile.entries();
//			boolean done = false;
//			while (e.hasMoreElements()) {
//				ZipEntry entry = (ZipEntry) e.nextElement();
//				stream = zfile.getInputStream(entry);
//				String filename = entry.getName();
//				filename = filename.replace('\\', '/');		
//				if (filename.equals("savefile.xml") || filename.startsWith("topicmap-t-")) {
//					new ImportDirector(Importer.OldFormat, stream, controler); 
//					done = true;
//					break;
//				} else if (filename.endsWith("content.cds.xml")) {
//					new ImportDirector(Importer.iMapping, file, controler); 
//					done = true;
//					break;
//				} else if (filename.endsWith("zknFile.xml")) {
////					new ImportDirector(13, stream, controler); 
//					new ImportDirector(Importer.Zettelkasten, file, controler); 
//					done = true;
//					break;
//				} else if (filename.equals("word/document.xml")) {
//					new ImportDirector(Importer.Word, stream, controler); 
//					done = true;
//					break;
//				} else if (filename.startsWith("ppt/slides/slide") && filename.endsWith(".xml")) {
//					new ImportDirector(Importer.PPTX, file, controler); 
//					done = true;
//					break;
//				} else if (zfile.getName().endsWith(".h5p") && filename.endsWith("content/content.json")) {
//					new ImportDirector(Importer.H5p, file, controler); 
//					done = true;
//					break;
//				} else	{
////					if (entryCount == 0) {
////						filelist = filename + "\r\n";	// to avoid leading newline
////					} else {
//					filelist = filelist + filename + "\r\n";
////					}
//					entryCount++;
//				}
//			}
//			if (done) {
//				zfile.close();
//				return;
//			}
//			if (entryCount == 1) {
//				dataString = utilities.convertStreamToString(stream);
//				advisableFilename = file.getAbsolutePath();
//				analyzeBlob(dataString, false);
//			} else {
//				dataString = filelist;
//				advisableFilename = "";
//				exploitFilelist(dataString, false);
//			}
////			zfile.close();
//		} catch (ZipException e1) {
////			System.out.println("Error NS121 (can be ignored) " + e1);
//			analyzeBlob(dataString, true);
//		} catch (IOException err) {
//			System.out.println("Error NS122 " + err);
//			controler.displayPopup("Error NS122 " + err);
//		}
//	}

//
//	Large single object (as opposed to composed lists)
	
//	public void analyzeBlob(String dataString, boolean isFile) {
//		InputStream stream = null;
//		Utilities utilities = new Utilities();
//		boolean hope = true;
//		
//		//	Try if XML format
//		Document doc = null;
//		if (!isFile) {
//			doc = getParsedDocument(dataString);	//	TODO consolidate
//		} else if (isFile) {
//			try {
//				stream = new FileInputStream(dataString);
//				doc = getParsedDocument(stream);
//			} catch (FileNotFoundException e2) {
////				System.out.println("Error NS127 (can be ignored) "  + e2);
//				hope = false;
//			}
//		}
//		if (doc.hasChildNodes()) {
//			Element root = doc.getDocumentElement();
//			
//			//	Try if known XML format
//			if (root.getTagName() == "x28map") {
//				if (compositionMode) controlerExtras.getCWInstance().cancel();
//				TopicMapLoader loader = new TopicMapLoader(doc, controler, false);
//				bounds = loader.getBounds();
//				newEdges = loader.newEdges;
//				step3a();
//				return;
//			}
//			Importer[] importers = Importer.getImporters();
//			for (int k = 0; k < importers.length; k++) {
//				if (root.getTagName() == importers[k].getKnownFormat()) {
//					if (k != Importer.Evernote 
//						&& k != Importer.Word 
//						&& k != Importer.Endnote) {
//						if (compositionMode) {
//							controlerExtras.getCWInstance().cancel();
//						}
//					}
//					new ImportDirector(k, doc, controler);
//					return;
//				}
//			}
//			hope = false;
//		} else hope = false;
//			
//		//	No XML
//		//	Endnote is not (yet) recognized, => works only via Wizard 
//		if (!hope) {
//			if (isFile) {
//				//	Analyze extension
//				File file = new File(dataString);
//				String filename = file.getName();
//				String ext = filename.substring(filename.lastIndexOf(".") + 1);
//				ext = ext.toLowerCase();
//				Importer[] importers = Importer.getImporters();
//				for (int k = 0; k < importers.length; k++) {
//					if (importers[k].getExt().equals(ext)) {
//						if (k != Importer.Evernote 
//							&& k != Importer.Word 
//							&& k != Importer.Endnote
//							&& k != Importer.RIS 
//							&& k != Importer.BibTeX) {
//							if (compositionMode) {
//								controlerExtras.getCWInstance().cancel();
//							}
//							if (k == Importer.Tagged) continue;	// txt not via autodiscovery
//							if (k == Importer.EdgeList) continue;	// txt not via autodiscovery
//							if (k == Importer.Roget) continue;	// htm not via autodiscovery
//						}
////						System.out.println("NS:" + ext + ", " + k + ", " + file);
//						new ImportDirector(k, file, controler);
//						return;
//					}
//				}
//				
//				//	Take flat file
//				String flatFileContent = "";
//				try {
//					stream = new FileInputStream(dataString);
//				} catch (FileNotFoundException e) {
//					System.out.println("Error NS126 " + e);
//					controler.displayPopup("Error NS126 File not found " + e);
//					return;
//				}
//				flatFileContent = utilities.convertStreamToString(stream);
//				dataString = flatFileContent;
//			}
////			step2b(dataString, false);
//			new Step2b(dataString, false, compositionMode, controler);
//			return;
//		}
//	}
	
//
//	Stuff from HTML lists

//	private String filterHTML(String html) {
////		System.out.println(html);
//
//		htmlOut = "";
//		listItem = false;
//		belowHeading = false;
//		structureFound = false;
//		silentlyResort = false;
//		listStructure = false;
//		htmlNoise = false;
//		if (System.getProperty("os.name").startsWith("Windows")) htmlNoise = true;
//		if (html.startsWith("Version:")) htmlNoise = true;
//
//		// We distinguish headings and lists. Depending on the first such structure found, 
//		// we either insert a TAB between heading and what follows, or leave each item as 
//		// one line (to let SplitIntoNew decide). Headings within lists are ignored, lists 
//		// under headings are formatted within a single detail field.
//		
//		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
//		HTMLEditorKit.Parser parser = null;
//		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
//			public void handleEndTag(HTML.Tag t, int pos) {
//				if (t.toString() == "li") {
//					listItem = false;
//					if (belowHeading) {
//						htmlOut = htmlOut  + "<br />";
//					} else {
//						htmlOut = htmlOut  + "\r\n";
//					}
//				} else if (t == HTML.Tag.STRONG || t == HTML.Tag.B || t.toString().matches("h\\d")) {
//					if (listStructure) return;
//					belowHeading = true;
//					structureFound = true;
//					htmlOut = htmlOut + "\t";
//				} else if (t == HTML.Tag.P && (belowHeading || !structureFound)) {
//					if (htmlOut.length() > 30) htmlOut = htmlOut + "<br /><br />";
//				} else if (t == HTML.Tag.TABLE && (belowHeading || !structureFound)) {
//					htmlOut = htmlOut + "<br />";
//				}
//
//			}
//			public void handleText(char[] data, int pos) {
//				String dataString = new String(data);
//				if (htmlNoise) return;
//				htmlOut = htmlOut + dataString;
//			}
//			public void handleComment(char[] data, int pos) {
//				String dataString = new String(data);
//				if (dataString.contains("w:WordDocument")) {
//					silentlyResort = true;	//	Word uses styles instead of list items
//					System.out.println("silent");
//				}
//				if (dataString.contains("StartFragment")) {
//					htmlNoise = false;
//				}
//			}
//			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
////				Enumeration<?> attrs = a.getAttributeNames();
//				if (t == HTML.Tag.PRE) {
//					silentlyResort = true;
//				} else if (t.toString() == "li") {
//					if (!htmlNoise) {
//						listItem = true;
//					} else return;
//					if (!structureFound) {
//						htmlOut = htmlOut + "\n";
//						listStructure = true;
//						structureFound = true;
//					}
//					if (belowHeading) htmlOut = htmlOut  + "<br /><br />o ";
//				} else if (t == HTML.Tag.STRONG || t == HTML.Tag.B || t.toString().matches("h\\d")) {
//					if (listItem) return;
//					belowHeading = false;	// start of the heading itself
//					if (!structureFound && htmlOut != "") htmlOut = "\t" + htmlOut;
//					htmlOut = htmlOut + "\n";
//				}
//			}
//			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
//				if (t == HTML.Tag.BR && (belowHeading || !structureFound)) {
//					htmlOut = htmlOut + "<br />";
//				}
//			}
//		};
//		parser = htmlKit.getParser();
//		Reader reader; 
//		reader = (Reader) new StringReader(html);
//		try {
//			parser.parse(reader, cb, true);
//		} catch (IOException e2) {
//			System.out.println("Error NS128 " + e2);
//		}
//		try {
//			reader.close();
//		} catch (IOException e3) {
//			System.out.println("Error NS129 " + e3.toString());
//		}
//		if (!structureFound || silentlyResort) {
//			if (!silentlyResort) {
//				controler.displayPopup("No items oder headings identified in HTML snippet,\r\n"
//						+ "using raw input string instead.");
//			}
//			htmlOut = dataStringResort;
//			dataStringResort = "";
//		}
//		return htmlOut;
//	}
    
//	Determine upper left visible corner
//	public Point determineCorner(Hashtable<Integer,GraphNode> nodes) {
//
//		int maxX = bounds.x + bounds.width;
//		int minY = bounds.y;
//		int minXtop = maxX;
//		if (bounds.width < 726) {	//	graphPanel width, 960 window - 232 right pane
//			minXtop = maxX - bounds.width/2;
//		}
//		Enumeration<GraphNode> nodesEnum = newNodes.elements();
//		while (nodesEnum.hasMoreElements()) {
//			GraphNode node = nodesEnum.nextElement();
//			Point xy = node.getXY();
//			int x = xy.x;
//			int y = xy.y;
//			if (y < minY + 100) {
//				if (x < minXtop) minXtop = x;
//			}
//		}
//		return new Point(minXtop - 40, minY - 40);
//	}

//	public Hashtable<Integer, GraphNode> fetchToUpperLeft(Hashtable<Integer,GraphNode> nodes) {
//		Point adjust = determineCorner(nodes);
//		Enumeration<GraphNode> e = nodes.elements();
//		while (e.hasMoreElements()) {
//			GraphNode node =e.nextElement();
//			Point xy = node.getXY();
//			xy.translate(- adjust.x, - adjust.y);;
//		}
//		return nodes;
//	}
	
//	//	Methods to try if XML 
//	
//	private Document getParsedDocument(InputStream stream) {
//		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//		DocumentBuilder parser = null; 
//		try  {
//			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//			parser = dbf.newDocumentBuilder(); 
//			return parser.parse(stream);
//		} catch (Exception e) {
//			System.out.println("Error NS124 (in getParsedDocument from stream): " + e);
//			return parser.newDocument();
//		}
//	}
//
//	private Document getParsedDocument(String input) {
//		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//		InputStream stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
//		DocumentBuilder parser = null; 
//		try  {
//			parser = dbf.newDocumentBuilder(); 
//			return parser.parse(stream);
//		} catch (Exception e) {
////			System.out.println("Error NS125 (can be ignored): " + e);
//			return parser.newDocument();
//		}
//	}

//
//	Communication with other classes
    
	public void setInput(String dataString, int inputType) {
		// TODO: fit to just 1, 2 and few 6
		if (inputType == 1) {
			advisableFilename = dataString;
//			interceptZips(dataString);
			new InterceptZips(dataString, controler, dropEncoding, compositionMode);
		}
		else if (inputType == 2) {
//			analyzeBlob(dataString, false);
			new AnalyzeBlob(dataString, false, compositionMode, controler);
		}
		else {
//			step2b(dataString, false);
			new Step2b(dataString, false, compositionMode, controler);
		}
	}
	
//	public Hashtable<Integer, GraphNode> getNodes() {
//		return newNodes;
//	}
//	public Hashtable<Integer, GraphEdge> getEdges() {
//		return newEdges;
//	}
	
	public void setCompositionMode(boolean toggle) {
		this.compositionMode = toggle;
	}
	
	public void setParseMode(boolean toggle) {
		this.parseMode = toggle;
	}
	
	public void setDropEncoding(boolean toggle) {
		this.dropEncoding = toggle;
	}
	
	public void scoopCompositionWindow(CompositionWindow compositionWindow) {
		String dataString = compositionWindow.dataString;
//		analyzeBlob(dataString, false);
		new AnalyzeBlob(dataString, false, compositionMode, controler);
	}
	
	public String getAdvisableFilename() {
		return advisableFilename;
	}
	
//	public Point getDropLocation() {
//		return dropLocation;
//	}

//	public boolean isExistingMap() {
//		return existingMap;
//	}
	
	public void init() {
		controlerExtras = controler.getControlerExtras();
	}
}

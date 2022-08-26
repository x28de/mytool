package de.x28hd.tool.importers;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Hashtable;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import de.x28hd.tool.MyHTMLEditorKit;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class Step2b {
	
	Hashtable<Integer, GraphNode> newNodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> newEdges = new Hashtable<Integer, GraphEdge>();
	String dataString;
	PresentationService controler;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	String htmlOut = "";
	boolean listItem = false;
	boolean firstColumn = true;
	String dataStringResort = "";
	boolean silentlyResort = false;
	boolean belowHeading = false;
	boolean htmlNoise = false;
	boolean structureFound = false;
	boolean listStructure = false;
	
	public Step2b(String dataString, PresentationService controler, 
			boolean parseHtml) {
		this.dataString = dataString;
		this.controler = controler;
		boolean compositionMode = controler.getNSInstance().compositionMode;

    	if (parseHtml) dataString = filterHTML(dataString);
		if (compositionMode) {
	    	controler.getControlerExtras().getCWInstance().insertSnippet(dataString);
	    	return;
		} 
		SplitIntoNew splitIntoNew = new SplitIntoNew(controler);
//		int newNodesCount = splitIntoNew.separateRecords(dataString);
		int newNodesCount = splitIntoNew.separateRecords2(dataString);
		splitIntoNew.heuristics(newNodesCount);
		splitIntoNew.createNodes(newNodesCount);	
		newNodes = splitIntoNew.getNodes();
		newEdges = splitIntoNew.getEdges();
		
		new Step3a(controler, newNodes, newEdges, bounds, false);
	}
	
//
//	Stuff from HTML lists
	
	private String filterHTML(String html) {
//		System.out.println(html);

		htmlOut = "";
		listItem = false;
		belowHeading = false;
		structureFound = false;
		silentlyResort = false;
		listStructure = false;
		htmlNoise = false;
		if (System.getProperty("os.name").startsWith("Windows")) htmlNoise = true;
		if (html.startsWith("Version:")) htmlNoise = true;

		// We distinguish headings and lists. Depending on the first such structure found, 
		// we either insert a TAB between heading and what follows, or leave each item as 
		// one line (to let SplitIntoNew decide). Headings within lists are ignored, lists 
		// under headings are formatted within a single detail field.
		
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleEndTag(HTML.Tag t, int pos) {
				if (t.toString() == "li") {
					listItem = false;
					if (belowHeading) {
						htmlOut = htmlOut  + "<br />";
					} else {
						htmlOut = htmlOut  + "\r\n";
					}
				} else if (t == HTML.Tag.STRONG || t == HTML.Tag.B || t.toString().matches("h\\d")) {
					if (listStructure) return;
					belowHeading = true;
					structureFound = true;
					htmlOut = htmlOut + "\t";
				} else if (t == HTML.Tag.P && (belowHeading || !structureFound)) {
					if (htmlOut.length() > 30) htmlOut = htmlOut + "<br /><br />";
				} else if (t == HTML.Tag.TABLE && (belowHeading || !structureFound)) {
					htmlOut = htmlOut + "<br />";
				}

			}
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				if (htmlNoise) return;
				htmlOut = htmlOut + dataString;
			}
			public void handleComment(char[] data, int pos) {
				String dataString = new String(data);
				if (dataString.contains("w:WordDocument")) {
					silentlyResort = true;	//	Word uses styles instead of list items
					System.out.println("silent");
				}
				if (dataString.contains("StartFragment")) {
					htmlNoise = false;
				}
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
//				Enumeration<?> attrs = a.getAttributeNames();
				if (t == HTML.Tag.PRE) {
					silentlyResort = true;
				} else if (t.toString() == "li") {
					if (!htmlNoise) {
						listItem = true;
					} else return;
					if (!structureFound) {
						htmlOut = htmlOut + "\n";
						listStructure = true;
						structureFound = true;
					}
					if (belowHeading) htmlOut = htmlOut  + "<br /><br />o ";
				} else if (t == HTML.Tag.STRONG || t == HTML.Tag.B || t.toString().matches("h\\d")) {
					if (listItem) return;
					belowHeading = false;	// start of the heading itself
					if (!structureFound && htmlOut != "") htmlOut = "\t" + htmlOut;
					htmlOut = htmlOut + "\n";
				}
			}
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.BR && (belowHeading || !structureFound)) {
					htmlOut = htmlOut + "<br />";
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
		if (!structureFound || silentlyResort) {
			if (!silentlyResort) {
				controler.displayPopup("No items oder headings identified in HTML snippet,\r\n"
						+ "using raw input string instead.");
			}
			htmlOut = dataStringResort;
			dataStringResort = "";
		}
		return htmlOut;
	}

}

package de.x28hd.tool.inputs;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Hashtable;

import javax.swing.JOptionPane;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.MyHTMLEditorKit;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class IngestItemlists {
	
	Hashtable<Integer, GraphNode> newNodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> newEdges = new Hashtable<Integer, GraphEdge>();
	PresentationService controler;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	String htmlOut = "";
	boolean htmlNoise = false;
	
	// for filterHTML1
	boolean listItem = false;
	boolean silentlyResort = false;
	boolean belowHeading = false;
	boolean structureFound = false;
	boolean listStructure = false;
	
	// for filterHTML2
	Hashtable<Integer,Integer> olItem = new Hashtable<Integer,Integer>();
	int item = 0;
	Hashtable<Integer,Integer> olParents = new Hashtable<Integer,Integer>();
	int level = 0;
	Hashtable<Integer,Integer> nodeParents = new Hashtable<Integer,Integer>();
	int seq = 0;
	String indent = "";		// to format testing output, then to detect a peculiar error
	boolean recordEmpty = true;
	boolean heading = false;
	boolean ol = false;
	
	// Usual auxiliary fields
	int j = 0;
	int maxVert = 10;
	int edgesNum = 0;
	String [] edgePalette = 	// Copied from Gui()
		{"#d2bbd2", "#bbbbff", "#bbffbb", "#ffff99", "#ffe8aa", "#ffbbbb"};
	
	public IngestItemlists(String dataString, PresentationService controler, 
			boolean parseHtml) {
		this.controler = controler;
		boolean compositionMode = controler.getNSInstance().compositionMode;

		// New: tree, uses filterHTML2 
		if (parseHtml) {
			if (dataString.toLowerCase().contains("<ol") 
					|| dataString.toLowerCase().contains("<ul") 
					|| dataString.toLowerCase().contains("<dl")) {
				Object[] options =  {"No", "Yes"};
				int	response = JOptionPane.showOptionDialog(null,
						"Hierarchical arrangement wanted?\n",
						"Option", JOptionPane.YES_NO_OPTION, 
						JOptionPane.QUESTION_MESSAGE, null, 
						options, options[1]);
				if (response == 1) {
					handleTrees(dataString);
					new InsertMap(controler, newNodes, newEdges, bounds, false);
					return;
				}
			}
		}
		
		// Old, uses filterHTML1, crude as before
		if (parseHtml) dataString = filterHTML1(dataString);
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
		
		new InsertMap(controler, newNodes, newEdges, bounds, false);
	}
	
	public void handleTrees(String dataString) {
		String dataString2 = filterHTML2(dataString);
		dataString = dataString2;
		String [] lines = dataString.split("\\r?\\n");
		int numRec = lines.length;
		for (int i = 0; i < numRec; i++) {
			String line = lines[i];
			int id = i + 1;
			if (!line.contains("\t")) {
				System.out.println("No level: " + line);
				line = "0\t" + line;
			}
			String[] fields = line.split("\\t");
			int level = Integer.parseInt(fields[0]);
			String record = fields[1];
			addNode(record, level);		// should get the above id as its key
			if (nodeParents.containsKey(id)) {
				int parentID = nodeParents.get(id);
				// Diagnostics mitigates that parser is not thread-safe?
				if (!newNodes.containsKey(id)) {
					System.out.println("No child " + id);
					continue;
				}
				if (!newNodes.containsKey(parentID)) {
					System.out.println("No parent " + parentID);
					continue;
				}
				edgesNum++;
				GraphNode node1 = newNodes.get(id);
				GraphNode node2 = newNodes.get(parentID);
				String colorString = edgePalette[level % 6];

				GraphEdge edge = new GraphEdge(edgesNum, node1, node2, Color.decode(colorString), "");
				newEdges.put(edgesNum, edge);
			} else {
				System.out.println(id + " has no parent");
			}
		}
	}
	
	public GraphNode addNode(String record, int level) {
		String label = "";
		j++;
		int id = j;

		int y = 40 + j * 40;
		int x = 40 + level * 150;
		Point p = new Point(x, y);
		int offset = record.indexOf("<br");
		if (offset > 0 && offset < 30) {
			label = record.substring(0, offset);
		} else if (record.length() < 30) {
			label = record;
		} else {
			offset = record.indexOf("(");
			if (offset > 0 && offset < 30) {
				label = record.substring(0, offset);
			} else {
				offset = record.indexOf(",");
				if (offset > 0 && offset < 30) {
					label = record.substring(0, offset);
				} else {
					label = record.substring(0, 30);
				}
			}
		}
		GraphNode topic = new GraphNode (id, p, Color.decode("#ccdddd"), label, record);	
		newNodes.put(id, topic);
		return topic;
	}
	
//
//	Stuff from HTML 
	
	private String filterHTML1(String html) {
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
				if (t == HTML.Tag.P) htmlOut += "<p>";
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
			System.out.println("Error IIL128a " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error IIL129a " + e3.toString());
		}
		if (!structureFound || silentlyResort) {
			if (!silentlyResort) {
				controler.displayPopup("No items oder headings identified in HTML snippet,\r\n"
						+ "using raw input string instead.");
			}
			htmlOut = controler.getNSInstance().getDataStringResort();
		}
		return htmlOut;
	}
		
	private String filterHTML2(String html) {
		htmlOut = "";
		htmlNoise = false;
		if (System.getProperty("os.name").startsWith("Windows")) htmlNoise = true;
		if (html.startsWith("Version:")) htmlNoise = true;
		
		// We copy each line, preceding by its level and a tab character,
		// Also, we record its sequence number seq to store the nodeParent relationship for
		// handleTree() to draw the connector lines.
		// And we store olItem and olParent per level to reconstruct the OL item numbers.

		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;

		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleEndTag(HTML.Tag t, int pos) {
				if (t == HTML.Tag.OL || t == HTML.Tag.UL || t == HTML.Tag.DL) {
					if (indent.length() < 2) {		// unexplainable excessive end tag encountered
						controler.displayPopup("Parsing failed; sorry.\n" +
							"For once, try without any lines that aren't list items.");
					}
					indent = indent.substring(2);
					level--;
				} else if (t == HTML.Tag.LI || t == HTML.Tag.DD) {
					if (!recordEmpty) {
						htmlOut = htmlOut + "\r\n";
						seq++;
						recordEmpty = true;
						int seqParent = olParents.get(level);
						nodeParents.put(seq, seqParent);
					}
				} else if (t.toString().matches("h\\d")) {
					heading = false;
						htmlOut = htmlOut + "</" + t.toString() + ">\r\n";
						seq++;
				}
			}
			public void handleText(char[] data, int pos) {
				int len = data.length;
				if (len <= 0) return;
				String dataString2 = "";
				if (len > 0) dataString2 = new String(data);
				if (htmlNoise) return;
				if (heading) {
					heading = false;
					htmlOut += dataString2;
					return;
				}
				htmlOut = htmlOut + dataString2;
				recordEmpty = false;
			}
			public void handleComment(char[] data, int pos) {
				String dataString2 = new String(data);
				if (dataString2.contains("StartFragment")) {
					htmlNoise = false;
				}
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (htmlNoise) return;
				if (t == HTML.Tag.OL || t == HTML.Tag.UL || t == HTML.Tag.DL) {
					ol = (t == HTML.Tag.OL);
					if (!recordEmpty) {
						htmlOut = htmlOut + "\r\n";
						seq++;
						recordEmpty = true;
						int seqParent = olParents.get(level);
						nodeParents.put(seq, seqParent);
					}
					indent += "  ";
					level++;
					olItem.put(level, 0);
					olParents.put(level, seq);
				} else if (t == HTML.Tag.LI || t == HTML.Tag.DD) {
					item = olItem.get(level);
					item++;
					olItem.put(level, item);
					String itemString = ol ? item + ". " : "";
					htmlOut = htmlOut + level + "\t" + itemString;
				} else if (t.toString().matches("h\\d")) {
					heading = true;
					htmlOut = htmlOut + level + "\t<" + t.toString() + ">";
				}
			}
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.BR) {
					htmlOut = htmlOut + "<br />";
				}
			}
		};
		parser = htmlKit.getParser();
		StringReader reader; 
		reader = new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error IIL128b " + e2);
		}
		reader.close();
		return htmlOut;
	}
}

package de.x28hd.tool.exporters;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.MyHTMLEditorKit;

public class CsvExport {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	String htmlOut;
	String breaks;
	boolean start;
	
	public CsvExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
		String storeFilename, PresentationService controler) {
		new CsvExport(nodes, edges, storeFilename, controler, false);
	}
	public CsvExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
		String storeFilename, PresentationService controler, boolean lines) {
		String newLine = System.getProperty("line.separator");
		
		FileWriter list;
		try {
			list = new FileWriter(storeFilename);
			if (!lines) {
			Enumeration<GraphNode> nodesEnum = nodes.elements();
			while (nodesEnum.hasMoreElements()) {
				GraphNode node = nodesEnum.nextElement();
				String label = node.getLabel();
				label = label.replace("\n", "");
				String detail = node.getDetail();
				detail = detail.replace("\n", "");
				detail = filterHTML(detail);
				list.write(label + "\t" + detail + newLine);
			}
			} else {
				Enumeration<GraphEdge> edgesEnum = edges.elements();
				while (edgesEnum.hasMoreElements()) {
					GraphEdge edge = edgesEnum.nextElement();
					GraphNode node1 = edge.getNode1();
					String label1 = node1.getLabel();
					label1 = label1.replace("\n", "");
					int id1 = node1.getID();
					GraphNode node2 = edge.getNode2();
					String label2 = node2.getLabel();
					label2 = label2.replace("\n", "");
					int id2 = node2.getID();
					list.write(id1 + "\t" + label1 + "\t" + id2 + "\t" + label2 + 
							newLine);
				}
			}
			list.close();
		} catch (IOException e) {
			System.out.println("Error CSV101 " + e);			
		}
	}
	private String filterHTML(String html) {
		htmlOut = "";
		breaks = "";
		start = true;
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				htmlOut = htmlOut + breaks + dataString;
				start = false;
				breaks = "";
			}
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t.equals(HTML.Tag.BR)) {
					if (!start) breaks = breaks + "<br />";
					return;
				}
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t.equals(HTML.Tag.P)) {
					if (!start) breaks = breaks + "<br />";
					return;
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error CSV109 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error CSV110 " + e3.toString());
		}
		return htmlOut;
	}
}

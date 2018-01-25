package de.x28hd.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class CsvExport {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	String htmlOut;
	String breaks;
	boolean start;
	
	public CsvExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
		String storeFilename, GraphPanelControler controler) {
		String newLine = System.getProperty("line.separator");
		
		FileWriter list;
		try {
			list = new FileWriter(storeFilename);
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

	private static class MyHTMLEditorKit extends HTMLEditorKit {
		private static final long serialVersionUID = 7279700400657879527L;

		public Parser getParser() {
			return super.getParser();
		}
	}
}

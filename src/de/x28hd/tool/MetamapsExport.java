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

import org.w3c.dom.Element;

public class MetamapsExport {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	String htmlOut;
	String breaks;
	boolean start;
	
	public MetamapsExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
		String storeFilename, GraphPanelControler controler) {
		String newLine = System.getProperty("line.separator");
		
		FileWriter list;
		try {
			list = new FileWriter(storeFilename);
			list.write("Topics\n" +
					"Id,Name,Metacode,X,Y,Description\n");
//					"Name\n");
			Enumeration<GraphNode> nodesEnum = nodes.elements();
			while (nodesEnum.hasMoreElements()) {
				GraphNode node = nodesEnum.nextElement();
				int id = node.getID();
				String label = node.getLabel();
				label = label.replace("\n", "");
				if (label.isEmpty()) label = "(Unnamed " + id + ")";
				label = textQualifying(label);
				String detail = node.getDetail();
				detail = detail.replace("\n", "");
				detail = filterHTML(detail);
				detail = textQualifying(detail);
				int x = node.getXY().x;
				int y = node.getXY().y;
				list.write(id + "," + label + ",Note," + x + "," + y + "," + 
						detail + newLine);
//				list.write(id + newLine);
			}
			list.write("\nSynapses\n" +
					"Topic1,Topic2,Category,Description\n");
			Enumeration<GraphEdge> myEdges = edges.elements();
			while (myEdges.hasMoreElements()) {
				GraphEdge edge = myEdges.nextElement();
				int n1 = edge.getN1();
				int n2 = edge.getN2();
				list.write(n1 + "," + n2 + ",from-to,\"is using\"" + newLine);
			}
			list.close();
		} catch (IOException e) {
			System.out.println("Error CSV101 " + e);			
		}
	}
	
	public String textQualifying(String in) {
		String out = in;
		if (out.contains(",")) out = "\"" + in + "\"";
		return out;
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

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

import org.w3c.dom.Element;

import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.MyHTMLEditorKit;

public class MetamapsExport {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	String htmlOut;
	String breaks;
	boolean start;
	boolean firstRecord = true;
	
	public MetamapsExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
		String storeFilename, PresentationService controler) {
		String newLine = System.getProperty("line.separator");
		
		// CSV version commented because of current error in Metamaps.cc
		FileWriter list;
		try {
			list = new FileWriter(storeFilename);
//			list.write("Topics\n" +
//					"Id,Name,Metacode,X,Y,Description\n");
			list.write("{\"topics\":[");
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
				x = Math.round(x * (float) 1.5);
				int y = node.getXY().y;
				y = Math.round(y * (float) 1.5);
				if (firstRecord) {
					list.write("{\"id\":" + id + ",\"name\":\"" + label + "\"," +
						"\"metacode\":\"Note\",\"x\":" + x + ",\"y\":" + y + "," +
						"\"description\":\"" +	detail + "\"}");
				} else {
					list.write(",\n{\"id\":" + id + ",\"name\":\"" + label + "\"," +
						"\"metacode\":\"Note\",\"x\":" + x + ",\"y\":" + y + "," +
						"\"description\":\"" +	detail + "\"}");
					
				}
				firstRecord = false;
//				list.write(id + "," + label + ",Note," + x + "," + y + "," + 
//						detail + newLine);
//				list.write(id + newLine);
			}
//			list.write("\nSynapses\n" +
//					"Topic1,Topic2,Category,Description\n");
			list.write("],\"synapses\":[");
			firstRecord = true;
			Enumeration<GraphEdge> myEdges = edges.elements();
			while (myEdges.hasMoreElements()) {
				GraphEdge edge = myEdges.nextElement();
				int n1 = edge.getN1();
				int n2 = edge.getN2();
				if (firstRecord) {
					list.write("{\"topic1\":" + n1 + ",\"topic2\":" + n2 + 
						",\"category\":\"from-to\",\"description\":\"\"}");
				} else {
					list.write(",\n{\"topic1\":" + n1 + ",\"topic2\":" + n2 + 
						",\"category\":\"from-to\",\"description\":\"\"}");
				}
				firstRecord = false;
//				list.write(n1 + "," + n2 + ",from-to,\"is using\"" + newLine);
			}
			list.write("]}");
			list.close();
		} catch (IOException e) {
			System.out.println("Error CSV101 " + e);			
		}
	}
	
	public String textQualifying(String in) {
		String out = in;
//		if (out.contains(",")) out = "\"" + in + "\"";
		out = in.replace("\"", "\\\"");
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
//					if (!start) breaks = breaks + "<br />";
					if (!start) breaks = breaks + "\\n";
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

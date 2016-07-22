package de.x28hd.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

public class CsvExport {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	
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
				String detail = node.getDetail();
				list.write(label + "\t" + detail + newLine);
			}
			list.write("done");
			list.close();
		} catch (IOException e) {
			System.out.println("Error CSV101 " + e);			
		}
	}
}

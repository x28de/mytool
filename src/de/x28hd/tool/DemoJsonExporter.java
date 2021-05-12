package de.x28hd.tool;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DemoJsonExporter {
	public DemoJsonExporter(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges,
			String storeFilename) {
		Hashtable<Integer,Integer> concordance = new Hashtable<Integer,Integer>();
		
		FileWriter file = null;
		try {
			file = new FileWriter(storeFilename);
		} catch (IOException e1) {
			System.out.println("Error DJE101 " + e1);
		}
		
		JSONObject both = null;
		try {
			JSONArray topics = new JSONArray();
			Enumeration<GraphNode> nodeList = nodes.elements();
			int arrayIndex = -1;
			while (nodeList.hasMoreElements()) {
				GraphNode node = nodeList.nextElement();
				JSONObject topic = new JSONObject();
				
				int x = node.getXY().x;
				topic.put("x", x);
				
				int y = node.getXY().y;
				topic.put("y", y);
				
				Color color = node.getColor();
				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();
				String colorString = String.format("#%02x%02x%02x", r, g, b);
				topic.put("color", colorString);
				
				String label = node.getLabel();
				topic.put("label", label);
				
				String detail = node.getDetail();
				topic.put("detail", detail);
				
				topics.put(topic);
				arrayIndex++;
				
				int nodeID = node.getID();
				concordance.put(nodeID, arrayIndex);
			}
			
			JSONArray assocs = new JSONArray();
			Enumeration<GraphEdge> edgeList = edges.elements();
			arrayIndex = -1;
			while (edgeList.hasMoreElements()) {
				GraphEdge edge = edgeList.nextElement();
				JSONObject assoc = new JSONObject();
				
				int node1 = edge.getN1();
				int index1 = concordance.get(node1);
				assoc.put("n1", index1);
				
				int node2 = edge.getN2();
				int index2 = concordance.get(node2);
				assoc.put("n2", index2);
				
				Color color = edge.getColor();
				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();
				String colorString = String.format("#%02x%02x%02x", r, g, b);
				assoc.put("color", colorString);
				
				assocs.put(assoc);
			}
			
			both = new JSONObject();
//			both.put("z comment", "Used with http://condensr.de");
			both.put("nodes", topics);
			both.put("edges", assocs);
		} catch (JSONException e) {
			System.out.println("Error DJE102 " + e);
		}
		
		try {
			file.write(both.toString());
			file.close();
		} catch (IOException e) {
			System.out.println("Error DJE103 " + e);
		}
	}
}

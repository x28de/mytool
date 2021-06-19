package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

import javax.xml.transform.TransformerConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

public class GrsshopperImport {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,Integer> concordance = new Hashtable<String,Integer>();
	int j = 0;
	int maxVert = 10;
	
	public GrsshopperImport(File file, GraphPanelControler controler) {
		String inputString = "";
		FileInputStream fileInputStream = null;
//		File file = new File("C:\\Users\\matthias\\Desktop\\grsshopper-graph.json");
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error GI101 " + e);
		}
		Utilities utilities = new Utilities();
		inputString = utilities.convertStreamToString(fileInputStream);	
		
		try {
			JSONObject all = new JSONObject(inputString);
			
			String tablesString = all.getString("nodes");
			JSONArray links = new JSONArray(tablesString);
			
			for (int i = 0; i < links.length(); i++) {
				String itemstring = links.getString(i);
				JSONObject topic = new JSONObject(itemstring);

				String label = topic.getString("label");
				String linkString = topic.getString("url");
				String detail = "<a href=\"" + linkString + "\">" + label + "</a><p>";
				if (label.length() > 30) label = label.substring(0, 30);
				if (topic.has("description")) detail += topic.getString("description");

				String jsonID = topic.getString("id");
//				jsonID = jsonID.substring(jsonID.indexOf("/") + 1);
//				int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
//				int x = 40 + (j/maxVert) * 200;
				int y = topic.getInt("y") * 50;
				int x = topic.getInt("x") * 20;
				j++;
				Point xy = new Point(x, y);
				Color color = Color.decode("#ccdddd");
				GraphNode node = new GraphNode(i, xy, color, label, detail);
				concordance.put(jsonID, i);
				nodes.put(i, node);
			}
			
			String assocsString = all.getString("edges");
			JSONArray assocs = new JSONArray(assocsString);
			
			for (int i = 0; i < assocs.length(); i++) {
				String assocString = assocs.getString(i);
				JSONObject assoc = new JSONObject(assocString);
				String node1 = assoc.getString("source");
				String node2 = assoc.getString("target");
//				node1 = node1.substring(node1.indexOf("/") + 1);
//				node2 = node2.substring(node2.indexOf("/") + 1);
				Color color = Color.decode("#ccdddd");
				if (!concordance.containsKey(node1)) {
					System.out.println("source not found: " + node1);
					continue;
				}
				int n1 = concordance.get(node1);
				if (!concordance.containsKey(node2)) {
					System.out.println("target not found: " + node2);
					continue;
				}
				int n2 = concordance.get(node2);
				GraphNode gnode1 = nodes.get(n1);
				GraphNode gnode2 = nodes.get(n2);
				GraphEdge edge = new GraphEdge(i, gnode1, gnode2, color, "");
				edges.put(i, edge);
				gnode1.addEdge(edge);
				gnode2.addEdge(edge);
			}
			
		} catch (JSONException e) {
			System.out.println("Error GI101 " + e);
		}
		
	    // Pass on
	    
	    String dataString = "";
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error FE108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error FE109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error FE110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
	}
}

package de.x28hd.tool.importers;

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

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;

public class DemoJsonImport {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	
	public DemoJsonImport(File file, PresentationService controler) {
		String inputString = "";
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error DJI101 " + e);
		}
		Utilities utilities = new Utilities();
		inputString = utilities.convertStreamToString(fileInputStream);	
		
		try {
			JSONObject all = new JSONObject(inputString);
			
			String topicsString = all.getString("nodes");
			JSONArray topics = new JSONArray(topicsString);
			
			for (int i = 0; i < topics.length(); i++) {
				String topicString = topics.getString(i);
				JSONObject topic = new JSONObject(topicString);
				int x = topic.getInt("x");
				int y = topic.getInt("y");
				Point xy = new Point(x, y);
				String colorString = topic.getString("color");
				Color color = Color.decode(colorString);
				String label = topic.getString("label");
				String detail = topic.getString("detail");
				GraphNode node = new GraphNode(i, xy, color, label, detail);
				nodes.put(i, node);
			}
			
			String assocsString = all.getString("edges");
			JSONArray assocs = new JSONArray(assocsString);
			
			for (int i = 0; i < assocs.length(); i++) {
				String assocString = assocs.getString(i);
				JSONObject assoc = new JSONObject(assocString);
				int n1 = assoc.getInt("n1");
				int n2 = assoc.getInt("n2");
				String colorString = assoc.getString("color");
				Color color = Color.decode(colorString);
				GraphNode node1 = nodes.get(n1);
				GraphNode node2 = nodes.get(n2);
				GraphEdge edge = new GraphEdge(i, node1, node2, color, "");
				edges.put(i, edge);
				node1.addEdge(edge);
				node2.addEdge(edge);
			}
			
		} catch (JSONException e) {
			System.out.println("Error DJI101 " + e);
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

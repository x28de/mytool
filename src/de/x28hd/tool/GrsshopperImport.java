package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
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
	GraphPanelControler controler;
	int j = 0;
	int maxVert = 10;

	String report = "";
	String [] direction = {"to", "from"};
	
	public GrsshopperImport(File file, GraphPanelControler controler) {
		this.controler = controler;
		String inputString = "";
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error GI101 " + e);
		}
		Utilities utilities = new Utilities();
		inputString = utilities.convertStreamToString(fileInputStream);	
		
		try {
			JSONObject all = new JSONObject(inputString);
			
			String tablesString = "";
			if (all.has("nodes")) {
				tablesString = all.getString("nodes");
			} else {
				controler.displayPopup("Error GI102 input has no \"nodes\"");
			}
			JSONArray links = new JSONArray(tablesString);
			
			for (int i = 0; i < links.length(); i++) {
				String itemstring = links.getString(i);
				JSONObject topic = new JSONObject(itemstring);

				String label = "";
				if (topic.has("label")) {
					label = topic.getString("label");
				} else {
					warn(i, "label");
				}
				String linkString = "";
				if (topic.has("url")) {
					linkString = topic.getString("url");
				} else {
					warn(i, "url");
				}
				String detail = "<a href=\"" + linkString + "\">" + label + "</a><p>";
				if (label.length() > 30) label = label.substring(0, 30);
				if (topic.has("description")) {
					detail += topic.getString("description") + "<p>";
//				} else {
//					warn(i, "description");
				}

				String jsonID = topic.getString("id");
				// Default positioning
				int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
				int x = 40 + (j/maxVert) * 200;
				j++;
				
				if (topic.has("y")) y = topic.getInt("y") * 50;
				if (topic.has("x")) x = topic.getInt("x") * 20;
				Point xy = new Point(x, y);
				Color color = Color.decode("#ccdddd");
				GraphNode node = new GraphNode(i, xy, color, label, detail);
				concordance.put(jsonID, i);
				nodes.put(i, node);
			}
			
			String assocsString = "";
			if (all.has("edges")) {
				assocsString = all.getString("edges");
			} else {
				controler.displayPopup("Error GI103 input has no \"edges\"");
			}
			JSONArray assocs = new JSONArray(assocsString);
			
			for (int i = 0; i < assocs.length(); i++) {
				String assocString = assocs.getString(i);
				JSONObject assoc = new JSONObject(assocString);
				String node1 = "";
				String node2 = "";
				if (assoc.has("source")) {
					node1 = assoc.getString("source");
				} else {
					warn(i, "source");
					continue;
				}
				if (assoc.has("target")) {
					node2 = assoc.getString("target");
				} else {
					warn(i, "target");
					continue;
				}
				Color color = Color.decode("#ccdddd");
				String type = "notype";
				if (assoc.has("type")) type = assoc.getString("type");
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
				GraphEdge edge = new GraphEdge(i, gnode1, gnode2, color, type);
				edges.put(i, edge);
				gnode1.addEdge(edge);
				gnode2.addEdge(edge);
			}
			
		} catch (JSONException e) {
//			System.out.println("Error GI101 " + e);
			controler.displayPopup("Error GI101 " + e);
		}
		
		// Create hyperlinks
		Enumeration<GraphNode> nodeList = nodes.elements();
		while (nodeList.hasMoreElements()) {
			GraphNode node = nodeList.nextElement();
			Enumeration<GraphEdge> neighbors = node.getEdges();
			while (neighbors.hasMoreElements()) {
				GraphEdge edge = neighbors.nextElement();
				GraphNode otherEnd = node.relatedNode(edge);
				int dir = 0;	// to or from?
				if (otherEnd.equals(edge.getNode1())) dir = 1 - dir;
				String rel = "<br>" + edge.getDetail() + " " + direction[dir] + 
						" <a href=\"#" + otherEnd.getLabel() + "\">" + otherEnd.getLabel() + "</a>";
				String detail = node.getDetail();
				node.setDetail(detail + rel);
			}
		}
		
	    // Pass on
	    
	    String dataString = "";
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error GI108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error GI109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error GI110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	if (!report.isEmpty()) { 
    		controler.displayPopup("Import completed with warnings:\n\n" + report);
    	}
	   	controler.getControlerExtras().toggleHashes(true);
	}
	
	public void warn (int i, String whatsMissing) {
		report += "Element " + i + " doesn't have a \"" + whatsMissing + "\"\n";
	}
}

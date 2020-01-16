package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import javax.xml.transform.TransformerConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

public class ImportRoam {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<Integer,Integer> fromBuffer = new Hashtable<Integer,Integer>();
	Hashtable<Integer,String> toBuffer = new Hashtable<Integer,String>();
	Hashtable<String,Integer> lookup = new Hashtable<String,Integer>();
	HashSet<String> edgesDone = new HashSet<String>(); 
	String dataString = "";
	int j = 0;
	int maxVert = 10;
	int edgesNum = 0;
	
	public ImportRoam(File file, GraphPanelControler controler) {
		FileInputStream fileInputStream = null;
		try {
//			fileInputStream = new FileInputStream("C:\\users\\matthias\\desktop\\x28de.json");
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error IR103 " + e);
		}
		Utilities utilities = new Utilities();
		String inputString = utilities.convertStreamToString(fileInputStream);	
		
		//	Parse JSON
		JSONArray array = null;
		try {
			array = new JSONArray(inputString);
	    	int arrayLen = array.length();
			JSONObject pageObj = null;
			String title = "";
	    	for (int j = 0; j < arrayLen; j++) {
	    		pageObj = array.getJSONObject(j);
				title = (String) pageObj.getString("title");
				
				String out = descendants(pageObj, 0);
				
				//	Find links
    			String rest = out;
    			String detail = "";
    			while (rest.contains("[[")) {
    				int linkStart = rest.indexOf("[[");
    				detail += rest.substring(0, linkStart);
    				rest = rest.substring(linkStart + 2);
    				if (!rest.contains("]]")) continue;
    				int linkEnd = rest.indexOf("]]");
    				String link = rest.substring(0, rest.indexOf("]]"));
    				rest = rest.substring(linkEnd + 2);
    				detail += "<a href=\"#" + link.toLowerCase() + "\">" + link + "</a>";
    				edgesNum++;
    				fromBuffer.put(edgesNum, j);
    				toBuffer.put(edgesNum, link.toLowerCase());
    			}
    			detail += rest;
				addNode(j, title, detail);
	    		lookup.put(title.toLowerCase(), j);
	    	}
				
	    	//
	    	//	Add buffered edges
	    	Enumeration<Integer> edgesToCreate = fromBuffer.keys();
	    	while (edgesToCreate.hasMoreElements()) {
	    		int id = edgesToCreate.nextElement();
	    		int fromID = fromBuffer.get(id);
	    		GraphNode node1 = nodes.get(fromID);
	    		String toID = toBuffer.get(id);
	    		int nodeID = lookup.get(toID);
	    		GraphNode node2 = nodes.get(nodeID);
	    		// Detect duplicates
		    	String n2label = node2.getLabel();
		    	String nodelabel = node1.getLabel();
				String unique = nodelabel.compareToIgnoreCase(n2label) > 0 ? 
						(nodelabel + " -- " + n2label) : (n2label + " -- " + nodelabel);
				if (edgesDone.contains(unique)) {
//					System.out.println("Duplicate link " + unique + " skipped");
					continue;
				}
				edgesDone.add(unique);
	    		addEdge(node1, node2);
	    	}
	    	
		} catch (JSONException e) {
			System.out.println("Error IR101 " + e);
		}
		
//
//		pass on
		
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error IR108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error IR109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error IR110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.setTreeModel(null);
    	controler.setNonTreeEdges(null);
		
		controler.toggleHashes(true);
		controler.fixDivider();
		controler.displayPopup("Import completed.\n"
				+ "Use Advanced > Layouts to arrange your links.\n"
				+ "If you miss something, please contact us.");
	}
	
	public String descendants(JSONObject pageObj, int level) {
		String out = "";
		String[] names = pageObj.getNames(pageObj);
		for (int n = 0; n < names.length; n++) {
			if (names[n].equals("children")) {

				int childrenLen = 0;
				JSONArray children = null;
				try {
					children = pageObj.getJSONArray("children");
					childrenLen = children.length();
					for (int k = 0; k < childrenLen; k++) {
						JSONObject childObj = children.getJSONObject(k);
						String content = childObj.getString("string");
						String outNew = k + ": " + content;
						if (level != 0) {	// save space on top level
							outNew = "<li>" + outNew + "</li>";
						} else {
							outNew += "<br/>";
						}
						out += outNew;
						out += descendants(childObj, level + 1);	//	recursion
					}
				} catch (JSONException e) {
					System.out.println("Error IR102 " + e);
				}
			}
		}
		if (!out.isEmpty() && level != 0) out = "<ul>" + out + "</ul>";
		return out;
	}
	
	public GraphNode addNode(int id, String label, String detail) {
		j++;

		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode("#ccdddd"), label, detail);	

		nodes.put(id, topic);
		return topic;
	}
	public void addEdge(GraphNode node1, GraphNode node2) {
		if (node1 == null || node2 == null) return;
		edgesNum++;
		GraphEdge edge = new GraphEdge(edgesNum, node1, node2, Color.decode("#d8d8d8"), "");
		edges.put(edgesNum, edge);
	}
}

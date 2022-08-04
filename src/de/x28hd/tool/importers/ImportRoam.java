package de.x28hd.tool.importers;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.xml.transform.TransformerConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;

public class ImportRoam {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<Integer,Integer> fromBuffer = new Hashtable<Integer,Integer>();
	Hashtable<Integer,String> toBuffer = new Hashtable<Integer,String>();
	Hashtable<String,Integer> lookup = new Hashtable<String,Integer>();
	HashSet<String> edgesDone = new HashSet<String>(); 
	HashSet<GraphNode> dontPrune = new HashSet<GraphNode>();
	String dataString = "";
	int j = 0;
	int maxVert = 10;
	int edgesNum = 0;
	boolean simplify = false;
	int liCount = 0;
	boolean sizeWarn = false;
	
	public ImportRoam(File file, PresentationService controler) {
		Object[] simplifyOptions =  {"Simplify", "Don't simplify", "Cancel"};
		int	simplifyResponse = JOptionPane.showOptionDialog(null,
			"Do you want to simplify the map\n"
			+ "(omit any lines to single leaves and Log Days) ?",
			"Option", JOptionPane.YES_NO_CANCEL_OPTION, 
			JOptionPane.QUESTION_MESSAGE, null, 
			simplifyOptions, simplifyOptions[0]);  
		if (simplifyResponse == JOptionPane.YES_OPTION) simplify = true;
				
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
				liCount = 0;
				
				String out = descendants(pageObj, 0);
				
				//	Find # and [[ links
				String detail = "";
    			String rest0 = out;
    			out = "";
    			while (rest0.contains(" #")) {
    				int linkStart = rest0.indexOf(" #");
    				out += rest0.substring(0, linkStart);
    				rest0 = rest0.substring(linkStart + 1);
    				String regex = "(#\\w++)(.*?)";
    				Pattern p = Pattern.compile(regex);
    				Matcher m = p.matcher(rest0);
    				if (!m.matches()) continue;
	    			String link = m.group(1).substring(1);
    				out += "<a href=\"#" + link.toLowerCase() + "\">" + link + "</a>";
    				edgesNum++;
    				fromBuffer.put(edgesNum, j);
    				toBuffer.put(edgesNum, link.toLowerCase());
    				rest0 = m.group(2);
    				out += link;
    			}
    			out += rest0;
    			String rest = out;
    			detail = "";
    			while (rest.contains("[[")) {
    				int linkStart = rest.indexOf("[[");
    				detail += rest.substring(0, linkStart);
    				rest = rest.substring(linkStart + 2);
    				if (!rest.contains("]]")) continue;
    				int linkEnd = rest.indexOf("]]");
    				String link = rest.substring(0, rest.indexOf("]]"));
    				if (link.contains("[[")) continue;
    				rest = rest.substring(linkEnd + 2);
    				detail += "<a href=\"#" + link.toLowerCase() + "\">" + link + "</a>";
    				edgesNum++;
    				fromBuffer.put(edgesNum, j);
    				toBuffer.put(edgesNum, link.toLowerCase());
    			}
    			detail += rest;
				GraphNode node = addNode(j, title, detail);
				if (liCount > 500) {
					node.setColor("#000000");
					liCount = 0;
					sizeWarn = true;
				}
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
	    		int nodeID = -1;
	    		if (!lookup.containsKey(toID)) {
	    			if (toID.length() > 30) toID = toID.substring(0, 30);
	    			System.out.println("Problem with " + toID + " linked from " + node1.getLabel());
	    			continue;
	    		} else {
	    			nodeID = lookup.get(toID);
	    		}
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
				title = node1.getLabel();
    			if (title.endsWith("2019") || title.endsWith("2020")) continue;
	    		addEdge(node1, node2);
	    	}
	    	
	    	//
	    	//	Prune single leaves
	    	if (simplify) {
	    	Enumeration<GraphNode> allNodes = nodes.elements();
	    	while (allNodes.hasMoreElements()) {
	    		GraphNode node = allNodes.nextElement();
	    		if (dontPrune.contains(node)) continue;
	    		GraphEdge lastEdge = null;
	    		int count = 0;
	    		Enumeration<GraphEdge> neighbors = node.getEdges();
	    		while (neighbors.hasMoreElements()) {
	    			GraphEdge edge = neighbors.nextElement();
	    			lastEdge = edge;
	    			count++;
	    		}
	    		if (count != 1) {
	    			if (node.getLabel().endsWith("2019") || node.getLabel().endsWith("2020")) node.setColor("#ff0000");
	    			continue;
	    		}
	    		GraphNode otherEnd = node.relatedNode(lastEdge);
	    		otherEnd.removeEdge(lastEdge);
	    		int edgeID = lastEdge.getID();
	    		edges.remove(edgeID);
	    		
	    		String label = node.getLabel();
	    		String detail = otherEnd.getDetail();
	    		detail += "<br/>Pruned: <a href=\"#" + label + "\">" + label + "</a>";
	    		otherEnd.setDetail(detail);
	    		dontPrune.add(otherEnd);
	    		
	    		label = otherEnd.getLabel();
	    		detail = node.getDetail();
	    		detail += "<br/>Pruned at : <a href=\"#" + label + "\">" + label + "</a>";
	    		node.setDetail(detail);
	    	}
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
    	controler.getControlerExtras().setTreeModel(null);
    	controler.getControlerExtras().setNonTreeEdges(null);
		
	   	controler.getControlerExtras().toggleHashes(true);
		controler.fixDivider();
		String msg = "Import completed.\n"
				+ "Use Advanced > Layouts to arrange your links.\n";
		if (sizeWarn) msg += "\nIf you hit the black nodes the map seems to freeze;\n"
				+ "Be patient because these pages are huge.\n"
				+ "Move them with a selection rectangle.";
		controler.displayPopup(msg);
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
						String outNew = content;
						if (level != 0) {	// save space on top level
							outNew = "<li>" + outNew + "</li>";
							liCount++;		// for size warning (coloring the node black)
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
		if (node1 == null || node2 == null || node1.equals(node2)) return;
		edgesNum++;
		GraphEdge edge = new GraphEdge(edgesNum, node1, node2, Color.decode("#d8d8d8"), "");
		edges.put(edgesNum, edge);
		node1.addEdge(edge);
		node2.addEdge(edge);
	}
}

package de.x28hd.tool.importers;

import java.awt.Color;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.xml.transform.TransformerConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.Utilities;
import de.x28hd.tool.exporters.TopicMapStorer;

public class AnnoImport {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	TreeMap<Integer,String> locMap = new TreeMap<Integer,String>();
	Hashtable<Integer,String> threadUsers = new Hashtable<Integer,String>();
	Hashtable<String,Integer> userColors = new Hashtable<String,Integer>();
	String [] palette = {"#d2bbd2", "#bbffbb", 	// purple, green
			"#ffbbbb", "#bbbbff", "#ffe8aa", 	// red, blue, orange
			"#fbefe8", "#fcf0d0", "#fdf1b8", "#e59f63", "#65473c"};  // 5 human skins
	Hashtable<String,String> edgesBuffer = new Hashtable<String,String>();

	int j = 0;
	int nodeID = 100;
	int edgesNum = 0;
	int colorNum = 0;
	
	public AnnoImport(File json, PresentationService controler) {
//		File json = new File("c:\\users\\matthias\\desktop\\hypothesis.json");
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(json);
			
		} catch (FileNotFoundException e) {
			System.out.println("Error AI101 " + e);
		}
		Utilities utilities = new Utilities();
		String pageString = utilities.convertStreamToString(fileInputStream);	
		pageString = pageString.substring(1, pageString.length() - 1); // TODO: parse the [[
		
		JSONObject itemObj = null;
		String user = "";
		int order = 0;
		JSONArray array = null;
		
		try {
			//	Parse JSON
			array = new JSONArray(pageString);
			for (int i = 0; i < array.length(); i++) {
				itemObj = array.getJSONObject(i);
				user = (String) itemObj.get("user");
				String quote = "";
				if (itemObj.has("quote")) quote = (String) itemObj.get("quote");
				String id = (String) itemObj.get("id");
				JSONArray refs = (JSONArray) itemObj.get("refs");
				String refList = "";
				String lastRef = "";
				for (int ii = 0; ii < refs.length(); ii++) {
					lastRef = refs.getString(ii);
//					refList += lastRef + "<br />";
				}
				
				String text = (String) itemObj.get("text");
				JSONArray targets = (JSONArray) itemObj.get("target");
				JSONObject target = (JSONObject) targets.get(0);
				if (!target.has("selector")) {
					
					//	Reply
					addNode(id, "", user + ": <br /><br />" + text +
							"<br /><br /><a href=\"https://hyp.is/" + 
							id + "\">open on https://hyp.is</a>", user);
					order++;
					locMap.put(order, id);
					if (!lastRef.isEmpty()) edgesBuffer.put(id, lastRef);
				} else {
					
					//	Location
					JSONArray selectors = (JSONArray) target.get("selector");
					JSONObject selector = null;
					int start = 0; 
					for (int k = 0; k < selectors.length(); k++) {
						selector = (JSONObject) selectors.get(k);
						String type = (String) selector.get("type");
						if (!type.equals("TextPositionSelector")) continue;
						start = (int) selector.getInt("start");
					}
					colorUsers();
					text = (String) itemObj.get("text");
					String detail = "\"" + quote + "\"<p>" + user + ": <br /><br />" + text;
					addNode(id, start + "", (detail + "<br /><br /><a href=\"https://hyp.is/" + 
							id + "\">open on https://hyp.is</a>"), user);
					order = start * 1000;
					while (locMap.containsKey(order)) order++;
					locMap.put(order, id);
				}
			}
			colorUsers();  // last thread

		} catch (JSONException e) {
			System.out.println("Error AI102 " + e);
		}
		
		//	Sort locations 
		SortedMap<Integer,String> locList = (SortedMap<Integer,String>) locMap;
		SortedSet<Integer> locSet = (SortedSet<Integer>) locList.keySet();
		Iterator<Integer> it = locSet.iterator();
		
		while (it.hasNext()) {
			Integer start = it.next();
			String id = locList.get(start);
			Integer num = inputID2num.get(id);
			GraphNode current = nodes.get(num);
			j++;
			int y = 40 + (j % 10) * 50 + (j/10)*5;
			int x = 40 + (j/10) * 150;
			Point p = new Point(x, y);
			current.setXY(p);
		}

		// Add buffered edges
		Enumeration<String> fromRefs = edgesBuffer.keys();
		while (fromRefs.hasMoreElements()) {
			String fromRef = fromRefs.nextElement();
			String toRef = edgesBuffer.get(fromRef);
			addEdge(fromRef, toRef);
		}
		
		// Pass on
	    String dataString = "";
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error AI108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error AI109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error AI110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.getControlerExtras().setTreeModel(null);
    	controler.getControlerExtras().setNonTreeEdges(null);
	}
	
	public void colorUsers() {
		// color recurring users of previous thread 
		Enumeration<Integer> thread = threadUsers.keys();
		while (thread.hasMoreElements()) {
			int itemID = thread.nextElement();
			String itemUser = threadUsers.get(itemID);
			if (!userColors.containsKey(itemUser)) continue;
			int userColor = userColors.get(itemUser);
			GraphNode node = nodes.get(itemID);
			node.setColor(palette[userColor]);
		}
		threadUsers.clear();
		userColors.clear();
		colorNum = 0;
	}
	
	public void addNode(String refID, String label, String detail, String user) {
		nodeID++;
		Point p = new Point(nodeID, nodeID);	// preliminary
		GraphNode topic = new GraphNode (nodeID, p, Color.decode("#ccdddd"), label, detail);	

		nodes.put(nodeID, topic);
		inputID2num.put(refID, nodeID);
		if (threadUsers.contains(user)) {
			if (!userColors.containsKey(user)){
				userColors.put(user, colorNum);
				colorNum++;
				colorNum = colorNum % 10;
			}
		}
		threadUsers.put(nodeID, user);
	}
	
	public void addEdge(String fromRef, String toRef) {
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
		edgesNum++;
		if (!inputID2num.containsKey(fromRef)) {
			System.out.println("Error AI103 " + fromRef);
			return;
		}
		GraphNode sourceNode = nodes.get(inputID2num.get(fromRef));
		if (!inputID2num.containsKey(toRef)) {
			System.out.println("Error AI104: invalid ref from " + fromRef + " to " + toRef);
			return;
		}
		GraphNode targetNode = nodes.get(inputID2num.get(toRef));
		edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), "");
		edges.put(edgesNum, edge);
	}
	
}

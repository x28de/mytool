package de.x28hd.tool.importers;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.transform.TransformerConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.GraphPanelControler;
import de.x28hd.tool.Utilities;
import de.x28hd.tool.exporters.TopicMapStorer;

public class H5pTextImport {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	String dataString = "";
	int j = -1;
	ZipFile zipFile;
	static final int HOTSPOTS = 0;
	static final int ACCORDION = 1;
	static final int TIMELINE = 2;
	int type = -1;
	
	public H5pTextImport(File file, GraphPanelControler controler) {
		InputStream fileInputStream = null;
		try {
			zipFile = new ZipFile(file);
			final ZipEntry entry = zipFile.getEntry("content/content.json");
			if (entry == null) System.out.println("Error HI101");
			fileInputStream = zipFile.getInputStream(entry);
		} catch (final Exception e) {
			System.out.println("Error HI102 " + e);
		}
		Utilities utilities = new Utilities();
		String inputString = utilities.convertStreamToString(fileInputStream);	
		String diag = inputString.substring(0, 100);
		
		//	Parse JSON
		JSONObject all = null;
		String hotspotsString = "";
		String panelsString = "";
		String timelineString = "";
		JSONArray array = null;
		String color = "#ccdddd";
		int arrayLen = -1;
		JSONObject hotspotObj = null;
		JSONObject panelObj = null;
		JSONObject timelineObj = null;
		
		try {
			all = new JSONObject(inputString);
			if (all.has("hotspots")) {
				hotspotsString = all.getString("hotspots");
				array = new JSONArray(hotspotsString);
				color = all.getString("color");
				type = HOTSPOTS;
			} else if (all.has("panels")) {
				panelsString = all.getString("panels");
				array = new JSONArray(panelsString);
				type = ACCORDION;
			} else if (all.has("timeline")) {
				timelineString = all.getString("timeline");
				timelineObj = new JSONObject(timelineString);
				String datesString = timelineObj.getString("date");
				array = new JSONArray(datesString);
				type = TIMELINE;
			} else {
				controler.displayPopup("<html>We are currently only supporting:"
						+ "<ul><li>ImageHotspots,</li>"
						+ "<li>Timeline,</li>"
						+ "<li>and Accordion</li></ul></html>");
			}
			arrayLen = array.length();
			
		} catch (JSONException e) {
			System.out.println("Error HI103 " + e);
		}

		// HotSpots
		if (type == HOTSPOTS) {
			for (int j = 0; j < arrayLen; j++) {
				String detail = "";
				String label = "";
				String text = "";
				double x = 8; 
				double y = 8;
				try {
					hotspotObj = array.getJSONObject(j);
					JSONObject position = hotspotObj.getJSONObject("position");
					String content = hotspotObj.getString("content");

					JSONArray fractions = new JSONArray(content);
					for (int k = 0; k < fractions.length(); k++) {
						JSONObject item = fractions.getJSONObject(k);
						JSONObject params = item.getJSONObject("params");
						if (params.has("text")) {
							text = params.getString("text");
						} else if (item.has("library")) {
							text = "(" + item.getString("library") + ")";
						}
						detail +=  text;
					}
					x = position.getDouble("x");
					y = position.getDouble("y");
					if (hotspotObj.has("header")) {
						label = hotspotObj.getString("header");
					}

				} catch (JSONException e) {
					System.out.println("Error HI104 " + e);
				}
				addNode((int) x , (int) y, color, label, detail);
			}

		// Accordion
		} else if (type == ACCORDION) {
			for (int j = 0; j < arrayLen; j++) {
				String detail = "";
				String label = "";
				String text = "";
				try {
					panelObj = array.getJSONObject(j);
					String content = panelObj.getString("content");

					JSONObject item = new JSONObject(content);
					JSONObject params = item.getJSONObject("params");
					if (params.has("text")) {
						text = params.getString("text");
					} else if (item.has("library")) {
						text = "(" + item.getString("library") + ")";
					}
					detail +=  text;
					if (panelObj.has("title")) {
						label = panelObj.getString("title");
					}
				} catch (JSONException e) {
					System.out.println("Error HI104 " + e);
				}
				addNode(color, label, detail);
			}
			
		// Timeline
		} else if (type == TIMELINE) {
			JSONObject item = null;
			try {
				for (int j = 0; j < arrayLen; j++) {
					String detail = "";
					String label = "";
					String text = "";
					item = array.getJSONObject(j);
					text = item.getString("text");
					detail +=  text;
					if (item.has("startDate")) {
						label = item.getString("startDate");
					}
					if (item.has("endDate")) {
						label += " - " + item.getString("endDate");
					}
					addNode(color, label, detail);
				}
			} catch (JSONException e) {
				System.out.println("Error HI104 " + e);
			}

		}
		
		
		if (nodes.size() < 1) controler.displayPopup("This "
				+ "cannot be recognized as one of the supported "
				+ "content types' content.json: \n" + diag );
		
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
    	
	}
	
	public GraphNode addNode(int x, int y, String color, String label, String detail) {
		j++;

		int id = j + 100;
		Point p = new Point(x * 5, y * 5);
		GraphNode topic = new GraphNode (id, p, Color.decode(color), label, detail);	

		nodes.put(id, topic);
		return topic;
	}
	public GraphNode addNode(String color, String label, String detail) {
		j++;
		int maxVert = 10;
		int id = j + 100;
		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode(color), label, detail);	

		nodes.put(id, topic);
		return topic;
	}
}

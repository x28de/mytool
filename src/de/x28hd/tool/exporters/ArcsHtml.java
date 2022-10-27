package de.x28hd.tool.exporters;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.MyHTMLEditorKit;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class ArcsHtml {
	TreeMap<Integer,GraphNode> nodesMap = new TreeMap<Integer,GraphNode>(); 
	SortedMap<Integer,GraphNode> nodesList = (SortedMap<Integer,GraphNode>) nodesMap;
	Hashtable<Integer,Integer> concordance = new Hashtable<Integer,Integer>();
	
	String htmlOut = "";
	int paragraphCount = 0;
	FileWriter file = null;
	
	public ArcsHtml(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
			PresentationService controler) {
		controler.displayPopup("Only the vertical order is used;\n"
				+ "only one paragraph per item is used;\n"
				+ "if it does not end in a period (\".\") it is capitalized;\n"
				+ "arrows should point from lower to upper items;\n"
				+ "light red arrow color (\"#ffbbbb\") marks contrast;\n"
				+ "the\"About\" item is not copied.\n\n"
				+ "Ready?"); 
//		
//		FileWriter file = null;
//		try {
//			file = new FileWriter(filename);
//		} catch (IOException e1) {
//			System.out.println("Error AJ101 " + e1);
//		}
		
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			if (node.getLabel().equals("About")) continue;
			int y = node.getXY().y;
			if (nodesMap.containsKey(y)) {
				controler.displayPopup("\"" + nodesMap.get(y).getLabel() + "\" and "
						+ "\"" + node.getLabel() + "\" are not serialized; try again.");
				return;
			}
			nodesMap.put(y, node);
		}
		SortedSet<Integer> nodesSet = (SortedSet<Integer>) nodesList.keySet();
		Iterator<Integer> iter = nodesSet.iterator();
		
		JSONObject both = null;
		JSONArray topics = null;
		JSONArray assocs = null;
		try {
			topics = new JSONArray();
			
			int newID = -1;
			while (iter.hasNext()) {
				int y = iter.next();
				GraphNode node = nodesList.get(y);
				int key = node.getID();
				newID++;
				concordance.put(key, newID);
				String detail = node.getDetail();
				detail = filterHTML(detail);
				JSONObject topic = new JSONObject();
				topic.put("id", newID);
				topic.put("detail", detail);
				topics.put(topic);
			}
		} catch (JSONException e) {
			System.out.println("Error AJ102 " + e);
		}
		
		try {
			assocs = new JSONArray();
			Enumeration<GraphEdge> edgesEnum = edges.elements();
			while (edgesEnum.hasMoreElements()) {
				GraphEdge edge = edgesEnum.nextElement();
				int n1 = edge.getN1();
				int n2 = edge.getN2();
				// double-check direction
				GraphNode node1 = nodes.get(n1);
				GraphNode node2 = nodes.get(n2);
				if (node2.getXY().y >= node1.getXY().y) {
					controler.displayPopup("\"" + node1.getLabel() + "\" -> \"" + node2.getLabel() + 
							"\" seems upside down; try again.");
					return;
				}
				Color color = edge.getColor();
				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();
				String colorString = String.format("#%02x%02x%02x", r, g, b);
				JSONObject assoc = new JSONObject();
				assoc.put("n1", concordance.get(n1));
				assoc.put("n2", concordance.get(n2));
				assoc.put("color", colorString);
				assocs.put(assoc);
//				System.out.println(n1 + " " + n2 + " " + colorString);
			}
		} catch (JSONException e) {
			System.out.println("Error AJ103 " + e);
		}
		
		String filename = controler.getLifeCycle().askForLocation("x28arcs.html");

//		FileWriter file = null;
		try {
			file = new FileWriter(filename);
		} catch (IOException e1) {
			System.out.println("Error AJ101 " + e1);
		}
		
		try {
			both = new JSONObject();
			both.put("nodesInput", topics);
			both.put("edges", assocs);
			addHTML();
			file.write("var map = " + both.toString() + ";");
			addJS();
			file.close();
		} catch (JSONException e) {
			System.out.println("Error AJ104 " + e);
		} catch (IOException e) {
			System.out.println("Error AJ105 " + e);
		}
	}
	
	private String filterHTML(String html) {
		htmlOut = "";
		paragraphCount = 0;
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleText(char[] data, int pos) {
				if (paragraphCount > 1) return;
				String dataString = new String(data);
				htmlOut = htmlOut + dataString;
			}
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t.equals(HTML.Tag.BR)) {
					paragraphCount++;
					return;
				}
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t.equals(HTML.Tag.P)) {
					paragraphCount++;
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
			System.out.println("Error AJ109 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error AJ110 " + e3.toString());
		}
		return htmlOut;
	}

	public void addHTML() {
		try {
			file.write("<!DOCTYPE html>\r\n"
					+ "<body onLoad=\"main()\">\r\n"
					+ "<canvas id=\"myCanvas\">\r\n"
					+ "no canvas\r\n"
					+ "</canvas>\r\n"
					+ "<script>\r\n");
		} catch (IOException e) {
			System.out.println("Error AJ113 " + e);
		}
	}

	public void addJS() {
		try {
			file.write("function main() { \r\n"
					+ "\r\n"
					+ "	var nodes;\r\n"
					+ "	var edges;\r\n"
					+ "	var canvas;\r\n"
					+ "	var context;\r\n"
					+ "	 \r\n"
					+ "	var mousedown = false;\r\n"
					+ "	var modified = false;\r\n"
					+ "	var moving = false;\r\n"
					+ "	\r\n"
					+ "	var selectedNode = -1;\r\n"
					+ "\r\n"
					+ "	var lastX = 0; \r\n"
					+ "	var lastY = 0; \r\n"
					+ "	var translatedX = 0; \r\n"
					+ "	var translatedY = 0;\r\n"
					+ "		\r\n"
					+ "	init();\r\n"
					+ "\r\n"
					+ "	newStuff(map);\r\n"
					+ "\r\n"
					+ "		\r\n"
					+ "	function init() {\r\n"
					+ "		document.head.innerHTML = \r\n"
					+ "			'<meta charset=\"UTF-8\"><title>Arcs</title>';\r\n"
					+ "\r\n"
					+ "		canvas = document.getElementById(\"myCanvas\"); \r\n"
					+ "		canvas.width = window.innerWidth;\r\n"
					+ "		canvas.height = window.innerHeight;\r\n"
					+ "		\r\n"
					+ "		context = canvas.getContext('2d');\r\n"
					+ "\r\n"
					+ "		nodes = [];\r\n"
					+ "		edges = [];\r\n"
					+ "		\r\n"
					+ "		draw();\r\n"
					+ "\r\n"
					+ "	if (window.PointerEvent) { \r\n"
					+ "		canvas.addEventListener('pointerdown', down); \r\n"
					+ "		canvas.addEventListener('pointermove', move); \r\n"
					+ "		canvas.addEventListener('pointerup', up); \r\n"
					+ "	} else if (window.TouchEvent) { \r\n"
					+ "		canvas.addEventListener('touchstart', down); \r\n"
					+ "		canvas.addEventListener('touchmove', move); \r\n"
					+ "		canvas.addEventListener('touchend', up); \r\n"
					+ "   	} else { \r\n"
					+ "		canvas.addEventListener('mousedown', down); \r\n"
					+ "		canvas.addEventListener('mousemove', move); \r\n"
					+ "		canvas.addEventListener('mouseup', up); \r\n"
					+ "	} \r\n"
					+ "		\r\n"
					+ "		canvas.addEventListener('contextmenu', rightClick, false);\r\n"
					+ "	}\r\n"
					+ "							    		\r\n"
					+ "	function down(evt) {\r\n"
					+ "		lastX = evt.offsetX; \r\n"
					+ "		lastY = evt.offsetY; \r\n"
					+ "		if (evt.altKey || evt.which == 2) {\r\n"
					+ "			modified = true;\r\n"
					+ "		} else modified = false;\r\n"
					+ "		mousedown = true;\r\n"
					+ "		draw(evt);\r\n"
					+ "    }\r\n"
					+ "    		\r\n"
					+ "	function move(evt){ \r\n"
					+ "		evt.preventDefault();\r\n"
					+ "		if (!mousedown) return;\r\n"
					+ "		var deltaX = evt.offsetX - lastX; \r\n"
					+ "		var deltaY = evt.offsetY - lastY; \r\n"
					+ "		if (mousedown && !modified) {\r\n"
					+ "			translatedX += deltaX; \r\n"
					+ "			translatedY += deltaY; \r\n"
					+ "			context.translate(deltaX, deltaY); \r\n"
					+ "		}\r\n"
					+ "		moving = true;\r\n"
					+ "		lastX = evt.offsetX; \r\n"
					+ "		lastY = evt.offsetY; \r\n"
					+ "		draw(evt); \r\n"
					+ "	} \r\n"
					+ "\r\n"
					+ "	function up(evt){ \r\n"
					+ "		if (!moving) selectedNode = findClicked(evt);\r\n"
					+ "		moving = false;\r\n"
					+ "		mousedown = false;\r\n"
					+ "		modified = false;\r\n"
					+ "		draw(evt);\r\n"
					+ "	}  \r\n"
					+ "    		\r\n"
					+ "//\r\n"
					+ "//	Core functions\r\n"
					+ "				    		\r\n"
					+ "	function draw(evt) { \r\n"
					+ "		context.clearRect(-translatedX, -translatedY, canvas.width, canvas.height); \r\n"
					+ "		if (edges.length > 0) {\r\n"
					+ "			for (var i = 0; i < edges.length; i++) { \r\n"
					+ "				context.strokeStyle = edges[i].color; \r\n"
					+ "				context.lineWidth = 3;\r\n"
					+ "				context.beginPath(); \r\n"
					+ "				p1 = nodes[edges[i].n1].x;\r\n"
					+ "				p2 = nodes[edges[i].n2].y;\r\n"
					+ "				delta = p2 - p1;\r\n"
					+ "				radius = Math.sqrt(delta * delta + delta * delta) / 2.;\r\n"
					+ "				center = (p2 + p1) / 2.;\r\n"
					+ "				counterClock = (edges[i].color == \"#bbffbb\");\r\n"
					+ "				if (edges[i].n1 == selectedNode || \r\n"
					+ "						edges[i].n2 == selectedNode) context.lineWidth = 10;\r\n"
					+ "				context.arc(center, center, radius, -.75 * Math.PI, .25 * Math.PI, counterClock);\r\n"
					+ "				context.stroke(); \r\n"
					+ "				context.lineWidth = 3;\r\n"
					+ "			} \r\n"
					+ "		}\r\n"
					+ "		for (var i = 0; i < nodes.length; i++) { \r\n"
					+ "			var sentence = nodes[i].detail;\r\n"
					+ "			context.beginPath(); \r\n"
					+ "			context.arc(nodes[i].x, nodes[i].y, 5, 0, 2 * Math.PI); \r\n"
					+ "			if (sentence.endsWith(\".\")) {\r\n"
					+ "				context.fillStyle = \"#555555\"; \r\n"
					+ "				context.arc(nodes[i].x, nodes[i].y, 5, 0, 2 * Math.PI); \r\n"
					+ "				context.fill(); \r\n"
					+ "				context.font = \"14px Arial\";\r\n"
					+ "			} else {		// author\r\n"
					+ "				context.strokeStyle = \"#cccccc\";\r\n"
					+ "				context.arc(nodes[i].x, nodes[i].y, 5, 0, 2 * Math.PI); \r\n"
					+ "				context.stroke(); \r\n"
					+ "				sentence = sentence.toUpperCase();\r\n"
					+ "				context.font = \"bold 14px Arial\";\r\n"
					+ "			}\r\n"
					+ "			context.fillStyle = \"#000000\"; \r\n"
					+ "			context.fillText(sentence, nodes[i].x + 12, nodes[i].y + 5); \r\n"
					+ "		} \r\n"
					+ "		context.fillText(\"Adapted from @denizcemonduygu.\", 100, window.innerHeight - 120);\r\n"
					+ "		if (map.alternateUrl) context.fillText(\"Rightclick for alternate format\", 100, window.innerHeight - 100);\r\n"
					+ "	} \r\n"
					+ "\r\n"
					+ "	function findClicked(evt) {\r\n"
					+ "		node = -1;\r\n"
					+ "		x = evt.offsetX - translatedX; \r\n"
					+ "		y = evt.offsetY - translatedY; \r\n"
					+ "		for (var i = 0; i < nodes.length; i++) { \r\n"
					+ "			if (x - nodes[i].x > 0 && Math.abs(y - nodes[i].y) < 11) { \r\n"
					+ "				node = i;\r\n"
					+ "			}\r\n"
					+ "		}\r\n"
					+ "		return node;\r\n"
					+ "	}\r\n"
					+ "	\r\n"
					+ "//\r\n"
					+ "//	Process the input stuff \"map\"\r\n"
					+ "\r\n"
					+ "	function newStuff(map) {\r\n"
					+ "		var nodesInput = map.nodesInput;\r\n"
					+ "		j = 0;\r\n"
					+ "		for (var i = 0; i < nodesInput.length; i++) {\r\n"
					+ "if (!nodesInput[i]) console.log(i);\r\n"
					+ "			j++;\r\n"
					+ "			if (!nodesInput[i].detail.endsWith(\".\")) j++;\r\n"
					+ "			id = nodesInput[i].id;\r\n"
					+ "			nodes[id] = {\"id\": nodesInput[i].id, x: j * 20, y: j * 20, color: '#ccdddd', \r\n"
					+ "					detail: nodesInput[i].detail};\r\n"
					+ "		}\r\n"
					+ "		edges = map.edges;\r\n"
					+ "		draw();\r\n"
					+ "	}\r\n"
					+ "\r\n"
					+ "	function rightClick(e) {\r\n"
					+ "		e.preventDefault();\r\n"
					+ "		location.assign(map.alternateUrl);\r\n"
					+ "	}\r\n"
					+ "}\r\n"
					+ "</script>\r\n"
					+ "</body>\r\n"
					+ "<style>\r\n"
					+ "canvas {touch-action: none;}\r\n"
					+ "</style>\r\n"
					);
		} catch (IOException e) {
			System.out.println("Error AJ112 " + e);
		}
	}
}

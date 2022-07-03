package de.x28hd.tool;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

public class MakeHTML {
	
	FileWriter list = null;
	
	public MakeHTML(boolean graphOnly, Hashtable<Integer,GraphNode> nodesIn, 
			Hashtable<Integer,GraphEdge> edgesIn, String filename, GraphPanelControler controler) {
		if (nodesIn.size() > 3000 || edgesIn.size() > 3000) {
			controler.displayPopup("Sorry, current size limit is 3000 nodes/ edges");
			return;  	//	TODO get rid of limitation
		}
		String nodesOut [][] = new String [3000][5];   // 0 = x, 1 = y, 2 = rgb, 3 = label, 4 = id
		String edgesOut [][] = new String [3000][3];   // 0 = n1, 1 = n2, 2 = rgb
		// TODO eliminate detour via topicid
		Hashtable<Integer, Integer> nodeids = new Hashtable<Integer, Integer>();
		Hashtable<Integer, Integer> edgeids = new Hashtable<Integer, Integer>();
		Rectangle bounds = controler.getControlerExtras().getBounds();
//		System.out.println(bounds.x + " - " + bounds.width + " x " + bounds.y + " - " + bounds.height);
		int width = bounds.width + 150;
		int height = bounds.height + 60;
		if (!graphOnly) {
			width = 860;
			height = 580;
		} 

	try {
		list = new FileWriter(filename);
		list.write("<html> \r\n"
				+ "<!DOCTYPE html>\r\n<meta charset=\"utf-8\"> \r\n"
				+ "\r\n"

				+ "<style> \r\n"
				+ "div#demo { \r\n"
				+ "margin-left: 870px; \r\n"
				+ "} \r\n"
//				+ "div#brand { \r\n"
//				+ "margin-left: 10px; margin-top: 510px; font-size: 75%;\r\n"
//				+ "} \r\n"
				+ "canvas { \r\n"
				+ "display: block; border: 1px solid #000; float: left; \r\n"
				+ "touch-action: none; \r\n" 
				+ "} \r\n"
				+ "</style>\r\n"
				+ "\r\n"
				
				+ "</head> \r\n"
				
				+ "<body onLoad=\"main()\";> \r\n\n"
				+ "<canvas id=\"myCanvas\" width=\"" + width + "\" height=\"" + height + "\"> \r\n"
				+ "Your browser does not support the HTML5 canvas tag. \r\n"
				+ "</canvas>\r\n");
		if (!graphOnly) {
				list.write(
				  "<div id=\"demo\"><em>Drag the clipping to pan the map. <br />&nbsp;"
				+ "<br />Click a circle on "
				+ "the left pane to see details in the right pane. </em> \r\n"
				+ "\r\n&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />" 
				+ "<em>&nbsp;Powered by <a href=\"http://condensr.de\">Condensr.de</a></em></div>"
				
				);
		}
	} catch (IOException e1) {
		System.out.println("Error MH101 " + e1);
	} 

	int nodenum = -1;
	int edgenum = -1;
	int i2 = -1;
	int r, g, b;
	r = g = b = 0;
	int topicid = 0;
	String colorString = "";
	Enumeration<GraphNode> topics = nodesIn.elements();
	Enumeration<GraphEdge> assocs = edgesIn.elements();
	
//
//	Collect nodes

	while (topics.hasMoreElements()) {
		i2++;
		GraphNode node = topics.nextElement();
		nodenum++;
		topicid = node.getID();
		nodeids.put(topicid, nodenum);
		Point xy = node.getXY();
		nodesOut[nodenum][0] = xy.x - bounds.x + 30 + "";
		nodesOut[nodenum][1] = xy.y - bounds.y + 30 + "";
		r = node.getColor().getRed();
		g = node.getColor().getGreen();
		b = node.getColor().getBlue();
		colorString = String.format("#%02x%02x%02x", r, g, b);
		nodesOut[nodenum][2] = colorString;
		String label = node.getLabel();
		label = label.replace("\n", "");
//		label = label.replace("\r", "");
		label = label.replace("'", "&apos;");
		nodesOut[nodeids.get(topicid)][3] = label;
		nodesOut[nodeids.get(topicid)][4] = topicid + "";
		try {
			list.write("\r\n<div id=\"" + topicid + "\" style=\"display: none;\">" + 
					node.getDetail() + "</div>");
		} catch (IOException e1) {
			System.out.println("Error MH102 " + e1);
		}
	}

//
//	Collect edges
	
	while (assocs.hasMoreElements()) {
		i2++;
		GraphEdge edge = assocs.nextElement();
		edgenum++;
		topicid = edge.getID();
		edgeids.put(topicid, edgenum);
		edgesOut[edgeids.get(topicid)][0] = nodeids.get(edge.getN1()).toString();
		edgesOut[edgeids.get(topicid)][1] = nodeids.get(edge.getN2()).toString();
		r = edge.getColor().getRed();
		g = edge.getColor().getGreen();
		b = edge.getColor().getBlue();
		colorString = String.format("#%02x%02x%02x", r, g, b);
		edgesOut[edgenum][2] = colorString;
	}

//
//	Output nodes

	try {
		list.write("<script> \r\n"
				+ "function main() { \r\n"
				+ "var nodes = [ \r\n");
	} catch (IOException e1) {
		System.out.println("Error MH103 " + e1);
	}

	for (int i = 0; i < nodenum + 1; i++) {
//		System.out.println("---- x = " + nodesOut[i][0] + ", y = " + nodesOut[i][1] + ", rgb = " + nodesOut[i][2] 
//				+ ", label = " + nodesOut[i][3] + ", id = " + nodesOut[i][4]);
		String comma = ", ";
		if (i == nodenum) comma = "";
		try {
			list.write("{x: " + nodesOut[i][0] + ", y: " + nodesOut[i][1] + ", rgb: '" + nodesOut[i][2] 
					+ "', label: '" + nodesOut[i][3] + "', id: '" + nodesOut[i][4] + "'}" + comma + " \r\n");
		} catch (IOException e1) {
			System.out.println("Error MH104 " + e1);
		}
	}

//
//	Output edges
	
	try {
		list.write("]; \r\n"
				+ "var edges = [ \r\n");
	} catch (IOException e1) {
		System.out.println("Error MH105 " + e1);
	}

	for (int i = 0; i < edgenum + 1; i++) {
//		System.out.println("---- n1 = " + edgesOut[i][0] + ", n2 = " + edgesOut[i][1] + ", rgb = " + edgesOut[i][2]);
		String comma = ", ";
		if (i == edgenum) comma = "";
		try {
			list.write("{n1: " + edgesOut[i][0] + ", n2: " + edgesOut[i][1] + ", rgb: '" + edgesOut[i][2] + "'}" + comma + " \r\n");			
		} catch (IOException e1) {
			System.out.println("Error MH106 " + e1);
		}
	}

	try {
		list.write("]; \r\n"
		+ "\r\n"
		
		+ "var can = document.getElementById(\"myCanvas\"), \r\n"
		+ "ctx = can.getContext('2d'), \r\n"
		+ "dragging = false, \r\n"
		+ "lastX = 0, \r\n"
		+ "lastY = 0, \r\n"
		+ "translatedX = 0, \r\n"
		+ "translatedY = 0; \r\n"
		+ "ctx.font = \"12px Arial\"; \r\n"
		+ "ctx.lineWidth = 2; \r\n"
		+ "\r\n"
		
		+ "can.addEventListener('click', function(e) { \r\n"
		+ "var evt = e || event; \r\n"
		+ "absoluteX = evt.pageX - translatedX - 9; \r\n"
		+ "absoluteY = evt.pageY - translatedY - 15; \r\n"
		+ "findClicked(absoluteX, absoluteY); \r\n"
		+ "}); \r\n"
		+ "\r\n"
		
		+ "function down(e){ \r\n"
		+ "var evt = e || event; \r\n"
		+ "dragging = true, \r\n"
		+ "lastX = evt.pageX; \r\n"
		+ "lastY = evt.pageY; \r\n"
		+ "} \r\n"
		+ "\r\n"
		
		+ "function move(e){ \r\n"
		+ "var evt = e || event; \r\n"
		+ "if (dragging){ \r\n"
		+ "var deltaX = evt.pageX - lastX; \r\n"
		+ "var deltaY = evt.pageY - lastY; \r\n"
		+ "translatedX += deltaX; \r\n"
		+ "translatedY += deltaY; \r\n"
		+ "ctx.translate(deltaX, deltaY); \r\n"
		+ "lastX = evt.pageX; \r\n"
		+ "lastY = evt.pageY; \r\n"
		+ "draw(); // redraw \r\n"
		+ "} \r\n"
		+ "} \r\n"
		+ "\r\n"
		
		+ "function up(e){ \r\n"
		+ "dragging = false; \r\n"
		+ "} \r\n"
		+ "\r\n"

		+ "if (window.PointerEvent) { \r\n"
		+ "can.addEventListener('pointerdown', down); \r\n"
		+ "can.addEventListener('pointermove', move); \r\n"
		+ "can.addEventListener('pointerup', up); \r\n"
		+ "} else { \r\n"
		+ "can.addEventListener('mousedown', down); \r\n"
		+ "can.addEventListener('mousemove', move); \r\n"
		+ "can.addEventListener('mouseup', up); \r\n"
		+ "} \r\n"
		+ "\r\n"
		
		+ "function draw() { \r\n"
		+ "ctx.clearRect(-translatedX, -translatedY, " + width + ", " + height + "); \r\n"
		+ "for (var i = 0; i < edges.length; i++) { \r\n"
		+ "ctx.strokeStyle = edges[i].rgb; \r\n"
		+ "ctx.beginPath(); \r\n"
		+ "ctx.moveTo(nodes[edges[i].n1].x, nodes[edges[i].n1].y); \r\n"
		+ "ctx.lineTo(nodes[edges[i].n2].x, nodes[edges[i].n2].y); \r\n"
		+ "ctx.stroke(); \r\n"
		+ "} \r\n"
		+ "for (var i = 0; i < nodes.length; i++) { \r\n"
		+ "ctx.beginPath(); \r\n"
		+ "ctx.fillStyle = nodes[i].rgb; \r\n"
		+ "ctx.arc(nodes[i].x, nodes[i].y, 9, 0, 2 * Math.PI); \r\n"
		+ "ctx.fill(); \r\n"
		+ "ctx.fillStyle = \"#000000\"; \r\n"
		+ "ctx.fillText(nodes[i].label, nodes[i].x - 9, nodes[i].y + 23); \r\n"
		+ "} \r\n"
		+ "} \r\n"
		+ "\r\n"
		
		+ "function findClicked(x, y) { \r\n"
		+ "for (var i = 0; i < nodes.length; i++) { \r\n"
		+ "if (Math.abs(x - nodes[i].x) < 11 && Math.abs(y - nodes[i].y) < 11) { \r\n"
		+ "draw(); \r\n"
		+ "ctx.strokeStyle = \"#ff0000\"; \r\n"
		+ "ctx.strokeRect(nodes[i].x - 11, nodes[i].y - 11, 22, 22); \r\n"
		+ "myFunction(nodes[i].id); \r\n"
		+ "} \r\n"
		+ "} \r\n"
		+ "} \r\n"
		+ "\r\n"
		
		+ "draw(); \r\n"
		+ "} \r\n"
		
//		+ "function myDraw(x1, y1, x2, y2) { \r\n"
//		+ "var can = document.getElementById(\"myCanvas\"), \r\n"
//		+ "ctx = can.getContext('2d'); \r\n"
//		+ "ctx.moveTo(x1, y1); \r\n"
//		+ "ctx.lineTo(x2, y2); \r\n"
//		+ "ctx.stroke(); \r\n"
//		+ "} \r\n"
		
		+ "function myFunction(detail) { \r\n"
		+ "var x = document.getElementById(detail).innerHTML; \r\n"
		+ "document.getElementById(\"demo\").innerHTML = x; \r\n"
		+ "} \r\n"
		
		+ "</script> \r\n"

		+ "</body></html> \r\n");
		list.close();

		String q = "\"";
		if(graphOnly) q = "";
		controler.displayPopup("Snapshot " + q + "printed" + q + " into " + filename);
		
	} catch (IOException e1) {
		System.out.println("Error MH107 " + e1);
	} 
	}
}

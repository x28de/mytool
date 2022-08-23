package de.x28hd.tool.importers;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Hashtable;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class Assembly {
	PresentationService controler;
	String dataString = "";
	Hashtable<Integer, GraphNode> nodes;
	Hashtable<Integer, GraphEdge> edges;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	Point dropLocation = null;
	String advisableFilename = "";
	boolean dropEncoding = true;
	boolean compositionMode = false;
	boolean withDates = true;
	boolean isHtml = false;
	boolean isFile = false;
	boolean existingMap = false;

	public Assembly(String d, PresentationService c) {
		dataString = d;
		controler = c;
		dropLocation = controler.getNSInstance().dropLocation;
		compositionMode = controler.getNSInstance().compositionMode;
		dropEncoding = controler.getNSInstance().dropEncoding;
	}
}

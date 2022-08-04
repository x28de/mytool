package de.x28hd.tool.core;

public class Selection {

	public int mode;
	static final int SELECTED_NONE = 1;
	public static final int SELECTED_TOPIC = 2;
	static final int SELECTED_ASSOCIATION = 3;
	public static final int SELECTED_TOPICMAP  = 4;

	public GraphNode topic;	//	\ max one
	GraphEdge assoc;	//	/ is set

	Selection() {
		mode = SELECTED_NONE;
	}
}

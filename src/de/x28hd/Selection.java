package de.x28hd;

class Selection {

	int mode;
	static final int SELECTED_NONE = 1;
	static final int SELECTED_TOPIC = 2;
	static final int SELECTED_ASSOCIATION = 3;
	static final int SELECTED_TOPICMAP  = 4;

	GraphNode topic;	//	\ max one
	GraphEdge assoc;	//	/ is set

	Selection() {
		mode = SELECTED_NONE;
	}
}

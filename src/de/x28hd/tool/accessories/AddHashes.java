package de.x28hd.tool.accessories;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class AddHashes {
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	PresentationService controler;

	public AddHashes(Hashtable<Integer,GraphNode> nodes,
			Hashtable<Integer,GraphEdge> edges, PresentationService controler) {
		this.controler = controler;
		nodes = controler.getNodes();
		edges = controler.getEdges();
    	controler.getControlerExtras().toggleHashes(true);
		
		Enumeration<GraphNode> nodeList = nodes.elements();
		while (nodeList.hasMoreElements()) {
			String add = "<p>See also:</p><p>";
			GraphNode node = nodeList.nextElement();
			if (controler.getSelectedNode().equals(node)) controler.displayPopup(
					"The selected item '" + node.getLabel() + "' cannot be changed");
			
			TreeSet<String> labelList = new TreeSet<String>();
			SortedSet<String> labelSet = (SortedSet<String>) labelList;
			Enumeration<GraphEdge> neighbors = node.getEdges();
			String anchor = "";
			while (neighbors.hasMoreElements()) {
				GraphEdge edge = neighbors.nextElement();
				GraphNode node1 = edge.getNode1();
				GraphNode node2 = edge.getNode2();
				if (node1.equals(node)) {
					anchor = node2.getLabel();
				} else {
					anchor = node1.getLabel();
				}
				labelList.add(anchor);
			}
			Iterator<String> iter = labelSet.iterator();
			while (iter.hasNext()) {
				anchor = iter.next();
				add += "<a href=\"#" + anchor + "\">" + anchor + "</a><br />";
			}
			String detail = node.getDetail();
			if (detail.contains("</body>")) {
				detail = detail.replace("</body>", add + "</body>");
			} else {
				detail += add;
			}
			node.setDetail(detail);
		}
	}

}

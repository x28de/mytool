package de.x28hd.tool.core;

import java.awt.Color;
import java.awt.Point;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

public class DataCore {
	public Hashtable<Integer, GraphNode> nodes = new Hashtable<Integer, GraphNode>();
	public Hashtable<Integer, GraphEdge> edges = new Hashtable<Integer, GraphEdge>();

	Object dataSource;
	public PresentationCore controlerCore;
	protected boolean dumbCaller;
	
	int maxVert = 10;
	int j = 0;
	boolean init = true;

	public DataCore(Object caller) {
		dumbCaller = (caller.getClass() == PresentationCore.class);
		controlerCore = (PresentationCore) caller;
		if (dumbCaller) createExample();
	}
	
	public void useData(Object dataSource) {
		this.dataSource = dataSource;
		if (!(dataSource instanceof MapItems)) {
			System.out.println("Only the MapItems interface is implemented so far.");
			return;
		}
		Collection<Object> linkedList = ((MapItems) dataSource).getList();
		Iterator<Object> iterator = linkedList.iterator();
		while (iterator.hasNext()) {
			j++;
			int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			int x = 40 + (j/maxVert) * 150;
			Point p = new Point(x, y);
			int id = j + 100;	// TODO use newKey
			String label = iterator.next().toString();
			
			// TODO integrate this with SplitIntoNew
			String detail = "";
			if (label.contains("\t")) {
				String [] columns = label.split("\\t");
				detail = columns[1];
				label = columns[0];
			}
			int len = label.length();
			if (len > 30) {
				detail = label;
				label = label.substring(0, 30);
			}
			GraphNode node = new GraphNode(id, p, Color.decode("#ccdddd"), label, detail);
			nodes.put(id, node);
		}
	}
	
	private void createExample() {
		GraphNode n1 = new GraphNode(1,new Point(40, 40), Color.RED, "Item 1", "Example text");
		GraphNode n2 = new GraphNode(2,new Point(140, 40), Color.GREEN, "Item 2", "Example text");
		nodes.put(1, n1);
		nodes.put(2, n2);
		GraphEdge edge = new GraphEdge(1, n1, n2, Color.YELLOW, "");
		edges.put(1, edge);
		n1.addEdge(edge);
		n2.addEdge(edge);
	}
}

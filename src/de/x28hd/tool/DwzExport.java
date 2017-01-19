package de.x28hd.tool;

import java.util.Hashtable;

public class DwzExport {
	
	
	public DwzExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges, 
			String zipFilename, GraphPanelControler controler)  {
		new LimitationMessage();
	}
}

package de.x28hd.tool;

// Documentation (see http://x28hd.de/tool/devel/index.htm for updates)

// Core of the data model are the two Hashtables nodes and edges

// Synopsis of GraphNode/ GraphEdge fields
// 
// 1. id (node and edge)
// in constructor,
// redundant (same as key in Hashtable)
// get/set by getID() / setID()
// loaded/ stored (new format): nodeid as attribute, edgeid not in xml
// imported (old format) as attribute
// 
// 2. xy (node only)
// in constructor,
// not redundant 
// get/set by getXY() / setXY()
// loaded/ stored (new format): as attributes x, y
// imported (old format) as attributes x, y
// 
// 3. node1 / node2 (edge only)
// in constructor
// not redundant,
// get by getNode1/2, no set 
// not loaded/ stored (new format), see n1, n2 instead
// not imported (old format), see n1, n2 instead
// 
// 4. color (node and edge)
// in constructor,
// not redundant,
// get/set by getColor(Color) / setColor (String)
// loaded/ stored (new format): as attribute (String)
// imported (old format) as attributes r, g, b
// 
// 5. label (node)
// in constructor
// not redundant,
// get/set by getLabel / setLabel 
// loaded/ stored (new format) as cdata
// imported (old format) as topname>\<basename>\cdata
// 
// 6. detail (node and edge)
// in constructor
// not redundant,
// get/set by getDetail / setDetail 
// loaded/ stored (new format) as cdata
// imported (old format) as <description>\cdata
// 
// 7. n1 / n2 (edge only)
// not in constructor
// redundant, see node1 / node2
// get by getN1/2, no set
// loaded/ stored (new format) as attributes
// imported (old format) as <assocrl anchrole=tt-topic[12]>\cdata
// 
// 8. associations (node only)
// not in constructor
// redundant, see egdes
// get by getEdges, relatedNode; set by addEdge, removeEdge
// not loaded/ stored (new format)
// not imported (old format)
// 


import javax.swing.JApplet;

public class MyTool extends JApplet {
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MyTool");
		new MyTool().initApplication(args);
	}
	
	private void initApplication(String[] args) {
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MyTool");
		try {
			PresentationService ps = new PresentationService(false);
			new Thread(ps).start();
			if (args.length >0) ps.setFilename(args[0], 0);
		} catch (Throwable e) {
			System.out.println("Error initApplication " + e);
			e.printStackTrace();
		}
	}
}

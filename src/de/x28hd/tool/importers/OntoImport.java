package de.x28hd.tool.importers;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.BranchInfo;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;
import de.x28hd.tool.layouts.DAG;
import edu.uci.ics.jung.graph.util.Pair;

public class OntoImport {
	
	// Warning: this is very raw. It serves my own needs and you can try yours.
	
	// Standard Condensr.de fields
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	int j = 0;
	int maxVert = 10;
	String dataString = "";
	int edgesNum = 0;
	PresentationService controler;

	// input-related
	HashSet<String> suppressedTypes = new HashSet<String>();
	static String [] suppress = {"owl:Class", "owl:ObjectProperty", 
			"owl:DatatypeProperty", "rdf:Description",
			"Class", "ObjectProperty", "FunctionalProperty", "owl:Ontology", 
			"NamedIndividual",
			"DatatypeProperty", "Description"};
	static String [] rels = {"rdf:type", 
			"rdfs:subClassOf", "rdfs:subPropertyOf", "skos:broader",
			"rdfs:domain", "schema:domainIncludes", 
			"rdfs:range", "schema:rangeIncludes"};
	static int [] relTypes = {0, 1, 1, 1, 2, 2, 3, 3};
	HashSet<String> relsSet = new HashSet<String>();
	HashSet<Integer> props = new HashSet<Integer>();
	Hashtable<String,GraphNode> lookup = new Hashtable<String,GraphNode>(); 
	HashSet<String> uniqEdges = new HashSet<String>();
	boolean errorsNoted = false;
	HashSet<String> complaints = new HashSet<String>();

	// buffer
	Hashtable<GraphNode,String> comments = new Hashtable<GraphNode,String>();
	HashSet<Pair<GraphNode>> domainLinks = new HashSet<Pair<GraphNode>>(); 
	HashSet<Pair<GraphNode>> rangeLinks = new HashSet<Pair<GraphNode>>(); 
	HashSet<Pair<GraphNode>> typeLinks = new HashSet<Pair<GraphNode>>(); 
	
	// output-related
	boolean simple;	
	Hashtable<GraphNode,GraphNode> parents = new Hashtable<GraphNode,GraphNode>();
	HashSet<GraphNode> dependantNodes = new HashSet<GraphNode>();
	DefaultMutableTreeNode top;
	String htmlList = "";
	int maxY = 40;
	
	public OntoImport(Document inputXml, PresentationService controler) {
		this.controler = controler;
		// Preparation
		for (int s = 0; s < suppress.length; s++) {
			suppressedTypes.add(suppress[s]);
		}
		for (int r = 0; r < rels.length; r++) {
			relsSet.add(rels[r]);
		}
		Object[] options = {"Yes, simple", "No, not all links"};
		int response = JOptionPane.showOptionDialog(controler.getMainWindow(),
				"<html>Do we have <b>simple</b> data? Then we can try to paint all links,"
				+ "<br>and also generate an HTML text file."
				+ "<br>Otherwise we will just paint the tree data and a few extra links.</html>",
				"Option",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]);
		simple = response == 0;
		
		// Explore what comes in through the fog :-)
		Element root = inputXml.getDocumentElement();
		processChildren(root);
		
//
//		Output
		
		// Edges tree and hypertext, part 1: all except ObjectPropertys
		if (simple) {
			top = new DefaultMutableTreeNode(new BranchInfo(0, "ROOT"));
			buildTree(top, "", true);
			outputTree(top, "");
		}	// else later in DAG layout
		
		// Check off dependent nodes for finding the tops of isolated groups
		Enumeration<GraphNode> nodelist = nodes.elements();
		while (nodelist.hasMoreElements()) {
			GraphNode node = nodelist.nextElement();
			Enumeration<GraphEdge> neighbors = node.getEdges();
			while (neighbors.hasMoreElements()) {
				GraphEdge edge = neighbors.nextElement();
				GraphNode target = edge.getNode2();
				if (target.equals(node)) dependantNodes.add(node);
			}
		}
		
		// Add more edges: 
		// - all if simple == true
		// - or selected ones just to connect isolated groups
		moreEdges(domainLinks, "#ffff99", true);
		moreEdges(rangeLinks, "#ffe8aa", false);
		
		// Edges and hypertext, part 2: just the ObjectProperty links
		if (simple) {
			top = new DefaultMutableTreeNode(new BranchInfo(0, "ROOT"));
			htmlList += "<h3>Linkage (the Ontology Triples)</h3>";
			buildTree(top, "", false);
			outputTree(top, "");
			
			for (int i = 0; i < 20; i++) {htmlList += "<p>&nbsp;</p>";}  // guarantee targeting
			FileWriter list;
			try {
				list = new FileWriter(System.getProperty("user.home") + 
						File.separator + "Desktop" + File.separator + "x28list.htm");
				list.write(htmlList);
				list.close();
			} catch (IOException e) {
				System.out.println("Error OI102 " + e);			
			}
		}

		if (!simple) {
			moreEdges(typeLinks, "#d2bbd2", false);
			new DAG(nodes, edges, controler);		// dumb tree layout
		}

		// Pass on
		try {
			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error OI108 " + e1);
		} catch (IOException e1) {
			System.out.println("Error OI109 " + e1);
		} catch (SAXException e1) {
			System.out.println("Error OI110 " + e1);
		}
		
		controler.getNSInstance().setInput(dataString, 2);
		controler.getControlerExtras().toggleHashes(true);
		
		if (errorsNoted) {
			controler.displayPopup("rdf:nodeID etc. not yet supported");
		}
	}
	
	public void processChildren(Element root) {
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			if (!child.hasAttributes()) {
				complain(child.getNodeName(), " has no attributes");
				continue;
			}
			
			// Identification via rdf:about
			NamedNodeMap attrmap = child.getAttributes();
			Node aboutNode = attrmap.getNamedItem("rdf:about");
			if (aboutNode == null) {
				errorsNoted = true;		// notify at end
				continue;
			}
			String about = aboutNode.getNodeValue();
			String shortened = shorten(about);
			String type = child.getNodeName();	// Rarer way; ignore? 
			if (type.equals("owl:Ontology")) shortened = "About";
			GraphNode thisNode = addNode(shortened, type, "#ccdddd");
			if (!simple && !type.isEmpty()) {
				considerLink("rdfs:type", 0, type, thisNode);  // Purple lines
			}
			
			// Gather the main info, mostly rdf:resource
			if (!child.hasChildNodes()) {
				complain(child.getNodeName(), " " + shortened + " has no children");
				continue;
			}
			NodeList grandChildren = child.getChildNodes();
			for (int k = 0; k < grandChildren.getLength(); k++) {
				Node grandChild = grandChildren.item(k);
				if (grandChild.getNodeType() != Node.ELEMENT_NODE) continue; // often a \n
				NamedNodeMap attrmap2 = grandChild.getAttributes();
				int relType = -1;
				for (int r = 0; r < rels.length; r++) {
					if (grandChild.getNodeName().equals(rels[r])) {
						relType = relTypes[r];
					}
				}
				
				// link info is recorded for considerText ('display') and considerLink,
				// other info just for considerText
				
				Node resourceNode = attrmap2.getNamedItem("rdf:resource");
				String display = "";
				if (resourceNode != null) {
					display = shorten(resourceNode.getNodeValue());
					
					// record link
//					System.out.println(thisNode.getLabel() + ": " + grandChild.getNodeName() +
//							" (" + relType + ") " + display);
					considerLink(grandChild.getNodeName(), relType, display, thisNode);
				} else {
					// Unsupported complexity?
					Node idNode = attrmap2.getNamedItem("rdf:nodeID"); 
					boolean unsupported = false;
					if (idNode != null) {
						unsupported = true;
						errorsNoted = true;
					}
					if (relType >= 0) {
						// info needed; buried further down? 
						if (!parseCollection((Element) child, relType, about, thisNode)) {
							if (!unsupported) {
								System.out.println(grandChild.getNodeName() + " of " 
										+ thisNode.getLabel() + " has no resource");
								break;
							}
						}
					}
					display = grandChild.getTextContent();
					if (display.isEmpty() && !unsupported) {
						complain(grandChild.getNodeName()  + " of " + thisNode.getLabel(), 
							" has no 'resource' or textContent");
					}
					considerText(grandChild, display, thisNode);
				}
			}
		}
	}
	
	public void considerLink(String property, int relType, String resource,
			GraphNode thisNode) {
		if (suppressedTypes.contains(resource)) return;
		GraphNode otherNode = addNode(resource, "", "#ccdddd");
		if (relType == 0) {		// type
				typeLinks.add(new Pair<GraphNode>(thisNode, otherNode));
		} else if (relType == 1) {	// subXxxxOf
			if (simple) {
				parents.put(thisNode, otherNode);
				addEdge(thisNode, otherNode, "#c0c0c0");	// no dependantNodes, avoid unreadable
			} else {
				addEdge(otherNode, thisNode, "#c0c0c0");
			}
		} else if (relType == 2) {	// domain
			registerLink(thisNode, otherNode, domainLinks, true);
		} else if (relType == 3) {	// range
			registerLink(thisNode, otherNode, rangeLinks, false);
		}
		String currentDetail = thisNode.getDetail();
		String fragment = otherNode.getLabel();
		if (simple && property.equals("rdfs:domain")) return;
		if (simple && property.equals("rdfs:range")) return;
		if (!simple) thisNode.setDetail(currentDetail + "<br>" + property + ": "
				+ "<a href=\"#" + fragment + "\">" + fragment + "</a>");
		return;
	}

	public void considerText(Node child, String display, GraphNode thisNode) {
		String oldDetail = thisNode.getDetail();
		int colon = Math.max(child.getNodeName().indexOf(":"), 0);
		String suffix = child.getNodeName().substring(colon + 1);
		String prefix = child.getNodeName().substring(0, colon + 1);
		if (!simple) thisNode.setDetail(oldDetail + "<p>" + prefix + "<b>" + suffix + ": </b>"  
				+ display + "</p>");
		if (child.getNodeName().equals("rdfs:comment")) {
			if (simple) {
				comments.put(thisNode, display);	// for different format in later hypertext  
				String currentDetail = thisNode.getDetail();
				if (!currentDetail.isEmpty()) currentDetail = "<br>" + currentDetail;
				thisNode.setDetail("<i>" + display + "</i>" + currentDetail);
			}
		}
	}
	
	public void registerLink(GraphNode thisNode, GraphNode otherNode, 
			HashSet<Pair<GraphNode>> table, boolean reverse) {
		thisNode.setColor("#ffe8aa");
		table.add(new Pair<GraphNode> (thisNode, otherNode));
		if (!reverse) {
			updateDetails(otherNode, thisNode);
		} else {
			updateDetails(thisNode, otherNode);
		}
	}
	
	public void updateDetails(GraphNode thisNode, GraphNode otherNode) {
		String currentDetail = otherNode.getDetail();
		String fragment = thisNode.getLabel();
		String fragment2 = fragment;
		if (simple) {
			fragment2 = prettyfy(fragment);
			if (!currentDetail.isEmpty()) currentDetail += "<br>";
			otherNode.setDetail(currentDetail 
					+ "<a href=\"#" + fragment2 + "\">" + fragment2 + "</a>");
		}
	}

	public void moreEdges(HashSet<Pair<GraphNode>> table, String color,
			boolean reverse) {
		// if simple == true selectEdges() selects all !
		Iterator<Pair<GraphNode>> list = table.iterator();
		while (list.hasNext()) {
			Pair<GraphNode> pair = list.next();
			GraphNode propNode = pair.getFirst();
			GraphNode otherNode = pair.getSecond(); 
			
			if (reverse) {
				selectEdges(otherNode, propNode, color);
			} else {
				selectEdges(propNode, otherNode, color);
			}
		}
	}
	
	public void selectEdges(GraphNode one, GraphNode two, String color) {
		if (simple) {
			addEdge(one, two, color);
			return;
		}
		// Edge for this link only if not yet among the target nodes 
		if (!dependantNodes.contains(one)) {
			addEdge(one, two, color);
			dependantNodes.add(one);
		}
		if (!dependantNodes.contains(two)) {
			addEdge(one, two, color);
			dependantNodes.add(two);
		}
	}

	public void buildTree(DefaultMutableTreeNode parent, String indent, boolean properties) {
		if (parent == top) {
			Enumeration<Integer> nodeList = nodes.keys();
			while (nodeList.hasMoreElements()) {
				int id = nodeList.nextElement();
				if (props.contains(id) == properties) continue;
				GraphNode child = nodes.get(id);
				if (!parents.containsKey(child)) {
					DefaultMutableTreeNode lowerNode = 
							new DefaultMutableTreeNode(new BranchInfo(id, child.getLabel()));
					parent.add(lowerNode);
					buildTree(lowerNode, indent + "  ", properties);
				}
			}
			return;
		}
		GraphNode graphNode;
		Enumeration<Integer> nodeList2 = nodes.keys();
		while (nodeList2.hasMoreElements()) {
			int id = nodeList2.nextElement();
			if (props.contains(id) == properties) continue;
			GraphNode child = nodes.get(id);
			BranchInfo info = (BranchInfo) parent.getUserObject();
			graphNode = nodes.get(info.getKey());
			if (!parents.containsKey(child)) continue;
			if (parents.get(child).equals(graphNode)) {
				DefaultMutableTreeNode lowerNode = 
						new DefaultMutableTreeNode(new BranchInfo(id, child.getLabel()));
				parent.add(lowerNode);
				buildTree(lowerNode, indent + "  ", properties);		// recursion
			} else {
				continue;
			}
		}
	}
	
	public int outputTree(DefaultMutableTreeNode parent, String indent) {
		int lvl = parent.getLevel();
		int leftX = 40 + lvl * 180;
		int newX = leftX;
		int newY = maxY;
		TreeMap<String,DefaultMutableTreeNode> levelMap = 
			new TreeMap<String,DefaultMutableTreeNode>(); 
		Enumeration<DefaultMutableTreeNode> children = parent.children();
		boolean onlyLeaves = true;
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = children.nextElement();
			BranchInfo info = (BranchInfo) child.getUserObject();
			levelMap.put(info.toString(), child);
			if (!child.isLeaf()) onlyLeaves = false;
		}
		SortedMap<String,DefaultMutableTreeNode> levelList = 
			(SortedMap<String,DefaultMutableTreeNode>) levelMap;
		SortedSet<String> levelSet = (SortedSet<String>) levelList.keySet();
		Iterator<String> iter = levelSet.iterator();
		boolean first = true;
		int siblings = parent.getLeafCount();
		int sibling = 0;
		while (iter.hasNext()) {
			sibling++;
			String item = iter.next();
			DefaultMutableTreeNode treeNode = levelList.get(item);
			BranchInfo info = (BranchInfo) treeNode.getUserObject();
			int id = info.getKey();
//			System.out.println(indent + nodes.get(id).getLabel());
			
			GraphNode thisNode = nodes.get(id);
			int advanceY = 40;
			if (onlyLeaves) {
				newX = leftX + 30 * (siblings - sibling);
				advanceY = 20;
			} else {
				newX = leftX;
			}
			if (first) {
				newY = maxY;
			} else {
				newY = maxY + advanceY;
			}
			thisNode.setXY(new Point(newX, newY));
			first = false;
			maxY = newY;
			htmlList += "<li><a name=\"" + prettyfy(thisNode.getLabel()) + "\"></a>"; 
			htmlList += prettyfy(thisNode.getLabel());
			if (!props.contains(thisNode.getID())) {
				htmlList += "\n<br />" + thisNode.getDetail();
			} else {
				String owlComment = comments.get(thisNode);
				if (owlComment == null) {
					owlComment = ":<br />";
				} else {
					owlComment = " (<i>" + owlComment+ "</i>):<br />";
				}
				htmlList += owlComment;
				if (owlComment != null) owlComment += " (<i>" + owlComment + "</i>):<br />";
				printSentence(thisNode);
			}
			htmlList += "</li>\n"; 
			htmlList += "<ul>";
			
			outputTree(treeNode, indent + "  ");	// recursion
			htmlList += "</ul>\n";
			thisNode.setLabel(prettyfy(thisNode.getLabel()));
		}
		return parent.getLeafCount();
	}
	
//
//	Standard tasks
	
	public GraphNode addNode(String label, String type, String color) {
		if (lookup.containsKey(label)) return lookup.get(label);
		j++;
		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		int id = 100 + j;
		if (type.equals("owl:ObjectProperty")) {
			props.add(id);
//			label += ":";
		}
		GraphNode topic = new GraphNode (id, p, Color.decode(color), label, "");	
		nodes.put(id, topic);
		lookup.put(label, topic);
		return topic;
	}
	
	public void addEdge(GraphNode node1, GraphNode node2, String color) {
		edgesNum++;
		int n1 = node1.getID();
		int n2 = node2.getID();
		String uniq = (n1 < n2) ? n1 + "-" + n2 : n2 + "-" + n1;
		if (uniqEdges.contains(uniq)) {
			System.out.println("Duplicate " + node1.getLabel() + " -> " + node2.getLabel());
			return;
		}
		GraphEdge edge = new GraphEdge(edgesNum, node1, node2, Color.decode(color), "");
		node1.addEdge(edge);
		node2.addEdge(edge);
		edges.put(edgesNum, edge);
		uniqEdges.add(uniq);
	}

	
//
//	Special tasks
	
	public boolean parseCollection(Element el, int relType, String about, 
			GraphNode thisNode) {
		boolean success = false;
		if (!el.hasChildNodes()) return false;
		NodeList children = el.getChildNodes();	
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			if (!relsSet.contains(child.getNodeName())) continue;
			NodeList children2 = child.getChildNodes();	
			for (int k = 0; k < children2.getLength(); k++) {
				Node child2 = children2.item(k);
				if (child2.getNodeName() != "owl:Class") continue;
				NodeList children3 = child2.getChildNodes();
				for (int l = 0; l < children3.getLength(); l++) {
					Node child3 = children3.item(l);
					if (child3.getNodeName() != "owl:unionOf") continue;
					NodeList children4 = child3.getChildNodes();
					for (int m = 0; m < children4.getLength(); m++) {
						Node child4 = children4.item(m);
						if (child4.getNodeName() != "rdf:Description") continue;
						NamedNodeMap attrmap = child4.getAttributes();
						Node aboutNode = attrmap.getNamedItem("rdf:about");
						if (aboutNode == null) continue;
						String about2 = aboutNode.getNodeValue();
						String shortened = shorten(about2);
						considerLink(child.getNodeName(), relType, shortened, thisNode);
//						System.out.println(relType + " " + shortened + " -> " + thisNode.getLabel());
						success = true;
					}
				}
			}
		}
		return success;
	}
	
	public void printSentence(GraphNode node) {		// triple S, P, O
		HashSet<String> subjects = new HashSet<String>();
		HashSet<String> objects = new HashSet<String>();
		Enumeration<GraphEdge> neighbors = node.getEdges();
		while (neighbors.hasMoreElements()) {

			GraphEdge edge = neighbors.nextElement();
			GraphNode node1 = edge.getNode1();
			GraphNode node2 = edge.getNode2();
			if (node1.equals(node)) objects.add(node2.getLabel());
			if (node2.equals(node)) subjects.add(node1.getLabel());
		}
		Iterator<String> subs = subjects.iterator();
		while (subs.hasNext()) {
			String subsnext = subs.next();
			Iterator<String> obs = objects.iterator();
			while (obs.hasNext()) {
				String halfSentence = "<a name=\"" + node.getLabel() + "\" href=\""
						+ "#" + subsnext + "\">" + subsnext + "</a>";
				halfSentence += " -- " + prettyfy(node.getLabel()) + " -- ";
				String obsnext = obs.next();
				String sentence = halfSentence + "<a href=\""
						+ "#" + obsnext + "\">" + obsnext + "</a><br>\n";
				htmlList += sentence;
			}
		}
	}

	public void restNest(Element parent, String indent) {
		if (parent.hasAttributes()) {
			NamedNodeMap amap = parent.getAttributes();
			for (int i = 0; i < amap.getLength(); i++) {
				String attr = "" + amap.item(i);
				if (attr.contains("#")) attr = "\"" + attr.substring(attr.indexOf("#") + 1);
				System.out.println(indent + amap.item(i).getNodeName() + " " + attr);
			}
		}
		if (!parent.hasChildNodes()) return;
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				System.out.println(indent + child.getNodeName());
				restNest((Element) child, indent + "  ");
			} else if (child.getNodeType() == Node.TEXT_NODE) {
				String content = child.getTextContent().trim();
				if (!content.isEmpty()) System.out.println(indent + content);
			}
		}
	}
	
//
//	Accessories
	
	public String shorten(String longURI) {
		int offset = longURI.indexOf("#");
		if (offset > 0) {
			String suffix = longURI.substring(offset + 1);
			if (suffix.length() > 0) {
				return suffix;
			}
		}
		offset = longURI.lastIndexOf("/");
		if (offset > 0) {
			return longURI.substring(offset + 1);
		} else {
			return longURI;
		}
	}

	public String prettyfy(String in) {
		String out = in.substring(in.indexOf("_") + 1);
		out = out.replaceAll("_"," ");
		return out;
	}
	

	public void complain(String who, String what) {
		if (complaints.contains(who)) return;
		System.out.println(who + what);
		complaints.add(who);
	}
}

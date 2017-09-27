package de.x28hd.tool;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GedcomImport {
	
	//	Major fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,String> inputItems = new Hashtable<String,String>();
	Hashtable<String,String> inputItems2 = new Hashtable<String,String>();
	Hashtable<String,String> inputNotes = new Hashtable<String,String>();
	Hashtable<String,String> discoveredVia = new Hashtable<String,String>(); // "parents" didn't fit
	HashSet<String> done = new HashSet<String>();
	Hashtable<String,Boolean> discoveryPointsBack = new Hashtable<String,Boolean>();
	Hashtable<Integer,Integer> colFill = new Hashtable<Integer,Integer>();
	Hashtable<String,String> marriages = new Hashtable<String,String>();
	
	//	Keys for nodes and edges, incremented in addNode and addEdge
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	Hashtable<Integer,String> num2inputID = new  Hashtable<Integer,String>();
	int j = -1;
	int edgesNum = 0;
	
	// For topology
    DefaultMutableTreeNode treeNode = null;
    DefaultMutableTreeNode topNode = null;
    DefaultMutableTreeNode newNode = null;
    String topID = "";
    Boolean first = true;
    DefaultTreeModel model = null;
	
	//	Constants
	private static final String XML_ROOT = "GEDCOM";
	int maxVert = 10;
	GraphPanelControler controler;
	
	public GedcomImport(JFrame mainWindow, GraphPanelControler controler) {	// TODO remove 
		this.controler = controler;
		
//
//		Open XML document
		
		FileDialog fd = new FileDialog(mainWindow);
		fd.setTitle("Select a Gedcom XML file");
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		File file = new File(filename);
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error GI101 " + e);
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error GI102 " + e2 );
		}
		
		try {
			inputXml = db.parse(fileInputStream);
			
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != XML_ROOT) {
				System.out.println("Error GI105, unexpected: " + inputRoot.getTagName() );
				fileInputStream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error GI106 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error GI107 " + e );
		}
		
		new GedcomImport(inputXml, controler);

	}
	
	public GedcomImport(Document inputXml, GraphPanelControler controler) {
		
//
//		Find input items

		NodeList itemList = inputXml.getElementsByTagName("IndividualRec");
		for (int i = 0; i < itemList.getLength(); i++) {
			Element node = (Element) itemList.item(i);
			String itemID = node.getAttribute("Id");
			Element idElem = (Element) node.getElementsByTagName("IndivName").item(0);
			NodeList partList = idElem.getElementsByTagName("NamePart");
			String gn = "";
			String gnFull = "";
			String sn = "";
			for (int p = 0; p < partList.getLength(); p++) {
				String partType = ((Element) partList.item(p)).getAttribute("Type");
				String part = partList.item(p).getTextContent();
				if (partType.equals("given name")) {
					gnFull = part;
					int bangOffset = gnFull.indexOf("!");
					if (bangOffset > 0) {
						gn = gnFull.substring(0, bangOffset);
					} else {
						gn = gnFull;
					}
				} else {
					sn = part;
				}
			}
			String labelString = gn;
			inputItems.put(itemID, labelString);
			String detailString = itemID + " " + gnFull + " " + sn;
			inputNotes.put(itemID, detailString);
		}
		
//
//		Process individuals' items	
		
		Enumeration<String> itemEnum = inputItems.keys();
		while (itemEnum.hasMoreElements()) {
			String item = itemEnum.nextElement();
			addNode(item);
		}
		
//
//		Get event dates for individuals above or marriages below
		
		NodeList eventList = inputXml.getElementsByTagName("EventRec");
		for (int i = 0; i < eventList.getLength(); i++) {
			Element node = (Element) eventList.item(i);
			String itemID = node.getAttribute("Id");
			
			NodeList dateList = node.getElementsByTagName("Date");
			String dateFull = "";
			String year = "";
			if (dateList.getLength() > 0) {
				Element dateElem = (Element) dateList.item(0);
				dateFull = dateElem.getTextContent();
				dateFull = dateFull.trim();
				String[] dateParts = dateFull.split(" ");
				year = dateParts[dateParts.length - 1];
			}
			String eventType = ((Element) eventList.item(i)).getAttribute("Type");
			
			if (eventType.equals("marriage")) {
				marriages.put(itemID, year);
			} else {
				Element link = (Element) node.getElementsByTagName("Link").item(0);
				String toItem = link.getAttribute("Ref");
				if (!inputID2num.containsKey(toItem)) {
					System.out.println("Error GI120 Individual missing: " + toItem);
					continue;
				}
				int nodeNum = inputID2num.get(toItem);
				GraphNode graphNode = nodes.get(nodeNum);
				String details = graphNode.getDetail();
				details = details + "<p>" + eventType + " " + year;
				graphNode.setDetail(details);
			}
		}

//		
//		Record input links
		
		NodeList famList = inputXml.getElementsByTagName("FamilyRec");
		for (int i = 0; i < famList.getLength(); i++) {
			Element node = (Element) famList.item(i);
			String itemID = node.getAttribute("Id");
			
			String year = "";
			NodeList marrList = node.getElementsByTagName("BasedOn");
			if (marrList.getLength() > 0) {
				Element idElem = (Element) marrList.item(0);
				Element link = (Element) idElem.getElementsByTagName("Link").item(0);
				String toItem = link.getAttribute("Ref");
				if (!marriages.containsKey(toItem)) {
					System.out.println("Error GI121 marriage missing " + toItem);
					continue;
				}
				year = marriages.get(toItem);
//				System.out.println("Event " + itemID + ", Marriage = " + toItem + " year " + year);
			}
			String labelString = year;
			
			inputItems.put(itemID, labelString);
			inputItems2.put(itemID, labelString);
			String detailString = itemID;
			inputNotes.put(itemID, detailString);
			addNode(itemID);
			
			if (first) {
			    treeNode = new DefaultMutableTreeNode(new BranchInfo(1, itemID));
			    topNode = treeNode;
				model = new DefaultTreeModel(topNode);
				topID = itemID;
				first = false;
			}
			
			NodeList fathList = node.getElementsByTagName("HusbFath");
			if (fathList.getLength() > 0) {
				Element idElem = (Element) fathList.item(0);
				Element link = (Element) idElem.getElementsByTagName("Link").item(0);
				String toItem = link.getAttribute("Ref");
				addEdge(toItem, itemID);
			}
			
			NodeList mothList = node.getElementsByTagName("WifeMoth");
			if (mothList.getLength() > 0) {
				Element idElem = (Element) mothList.item(0);
				Element link = (Element) idElem.getElementsByTagName("Link").item(0);
				String toItem = link.getAttribute("Ref");
				addEdge(toItem, itemID);
			}
			
			NodeList childList = node.getElementsByTagName("Child");
			for (int c = 0; c < childList.getLength(); c++) {
				Element idElem = (Element) childList.item(c);
				Element link = (Element) idElem.getElementsByTagName("Link").item(0);
				String toItem = link.getAttribute("Ref");
				addEdge(itemID, toItem);
				
			}
		}

//
//		Create discovery tree
//		(this tree does NOT represent the ancestral lineage but just 
//		the discovery sequence starting from an arbitrary starting point)
		
		int nodeID = inputID2num.get(topID);
		GraphNode node = nodes.get(nodeID);
		System.out.println("Starting. First node is " + node.getDetail());
		
		connectFamilies(node);
		gatherRelatives(topID, topNode);
		
		// traverse the discovery tree
		colFill.put(1,0);	// initialize odd cases
		colFill.put(-1,0);
		growGraph(topNode, "", 0, 1);
		
//			
//		Pass on the new map
		
		try {
			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error GI108 " + e1);
		} catch (IOException e1) {
			System.out.println("Error GI109 " + e1);
		} catch (SAXException e1) {
			System.out.println("Error GI110 " + e1);
		}
		
		controler.getNSInstance().setInput(dataString, 2);
	}
	
	public void addNode(String nodeRef) { 
		boolean linkingPhrase = (inputItems2.containsKey(nodeRef));	// denotes a marriage here
		j++;
		String newNodeColor;
		String newLine = "\r";
		String topicName = ""; 
		String verbal = "";
		if (linkingPhrase) {
			topicName = inputItems2.get(nodeRef);
			newNodeColor = "#eeeeee";
			verbal = inputNotes.get(nodeRef);
		} else {
			topicName = inputItems.get(nodeRef);
			verbal = inputNotes.get(nodeRef);
			newNodeColor = "#ccdddd";
		}
		topicName = topicName.replace("\r"," ");
		if (topicName.equals(newLine)) topicName = "";
		if (verbal == null || verbal.equals(newLine)) verbal = "";
		if (topicName.isEmpty() && verbal.isEmpty()) return;
		int id = 100 + j;

		Point p = new Point(40, 40);
		
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		inputID2num.put(nodeRef, id);
		num2inputID.put(id, nodeRef);
	}
	
	public void addEdge(String fromRef, String toRef) {
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
		if (!inputID2num.containsKey(fromRef) || !inputID2num.containsKey(toRef)) {
			System.out.println(fromRef + " -> " + toRef);
			return;
		}
		edgesNum++;
		GraphNode sourceNode = nodes.get(inputID2num.get(fromRef));
		GraphNode targetNode = nodes.get(inputID2num.get(toRef));
		edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), "");
		edges.put(edgesNum, edge);
		sourceNode.addEdge(edge);
		targetNode.addEdge(edge);
	}

	public void connectFamilies(GraphNode node) {
		int nodeNum = node.getID();
		String nodeID = num2inputID.get(nodeNum);
		Enumeration<GraphEdge> neighbors = node.getEdges();
		while (neighbors.hasMoreElements()) {
			GraphEdge edge = neighbors.nextElement();
			GraphNode otherEnd = node.relatedNode(edge);
			int otherEndNum = otherEnd.getID();
			boolean back = false;
			int n1 = edge.getN1();
			if (otherEndNum == n1){
				back = true;
			}
			String otherEndID = num2inputID.get(otherEndNum);
			if (done.contains(otherEndID)) continue;
			if (discoveredVia.containsKey(otherEndID)) continue;
			if (discoveredVia.containsKey(nodeID) && discoveredVia.get(nodeID).equals(otherEndID)) continue;
			discoveredVia.put(otherEndID, nodeID);
			
			int edgeID = edge.getID();
//			if (back) {
//				String edgeColor = "#ff0000";
//				edge.setColor(edgeColor);
//			}
			discoveryPointsBack.put(otherEndID, back);
			
			done.add(otherEndID);
			connectFamilies(otherEnd);
		}
	}
	
	public void gatherRelatives(String parent, DefaultMutableTreeNode treeNode) {
		Enumeration<String> parentsEnum = discoveredVia.keys();
		while (parentsEnum.hasMoreElements()) {
			String testChild = parentsEnum.nextElement();
			String testParent = discoveredVia.get(testChild);
			if (!testParent.equals(parent)) {
				continue;
			} else {
				int hori = 1;
				if (discoveryPointsBack.get(testChild)) hori = -1;
				newNode = new DefaultMutableTreeNode(new BranchInfo(hori, testChild));
				treeNode.add(newNode);
				gatherRelatives(testChild, newNode);
			}
		}
	}

	public void growGraph(DefaultMutableTreeNode nestNode, String indent, int myCol, int hori) {
		
		int newCol  = 0;
		
		String myID = nestNode.getUserObject().toString();
		int nodeNum = inputID2num.get(myID);
		GraphNode node = nodes.get(nodeNum);
		int depth = nestNode.getDepth();
		
		//	Add current node to its column
		Point drawLoc = addToFill(myCol, hori, depth);
		node.setXY(drawLoc);
		
		// Recursively open the nested subtrees
		
		Enumeration<DefaultMutableTreeNode> treeChildren = nestNode.children();
		
		//	Prepare for reordering the current level of children
		Hashtable<String,DefaultMutableTreeNode> childrenMap = 
				new Hashtable<String,DefaultMutableTreeNode>();
		TreeMap<Double,String> orderMap = new TreeMap<Double,String>();
		SortedMap<Double,String> orderList = (SortedMap<Double,String>) orderMap;
		double disambig = 0.001;

		//	Unordered list
		while (treeChildren.hasMoreElements()) {
			DefaultMutableTreeNode child = treeChildren.nextElement();
			String itemID = child.getUserObject().toString();
			childrenMap.put(itemID, child);
			int weight = child.getLeafCount();
//			int weight = child.getDepth();
//			int weight = child.getChildCount();
			double dweight = weight;
			if (orderMap.containsKey(dweight)) {
				dweight = dweight + disambig;
				disambig = disambig + 0.001;
			}
			orderMap.put(dweight, itemID);
        }
		
		// Ordered list
		SortedSet<Double> orderSet = (SortedSet<Double>) orderList.keySet();
		Iterator<Double> ixit = orderSet.iterator(); 
		while (ixit.hasNext()) {
        	double rankNum = ixit.next();
        	int weight = (int) rankNum;
        	String itemID = orderMap.get(rankNum);
			DefaultMutableTreeNode child = childrenMap.get(itemID);
		
			BranchInfo nodeRef = (BranchInfo) child.getUserObject();
			hori = nodeRef.branchKey; 	// the term "key" is abused here; it is +- 1
			
			//	just for diag
			int nodeNum2 = inputID2num.get(itemID);
			GraphNode node2 = nodes.get(nodeNum2);
			String diag = node2.getLabel();
//			if (weight > 1) node2.setLabel(diag + " " + weight);
//			System.out.println(indent + itemID + " " + diag);

			newCol = myCol + hori;
			growGraph(child, indent + " ", newCol, hori);	// the recursion
		}
	}
	
	public Point addToFill(int col, int hori, int depth) {
		if (!colFill.containsKey(col)) colFill.put(col, 0);
		int fill = colFill.get(col);
		
		int testCol = col;
		while (colFill.containsKey(testCol)) {
			int testFill = colFill.get(testCol);
			if (testFill > fill) fill = testFill;
			if (Math.abs(testCol - col) >= depth) break;
			if (hori > 0) {
				testCol++;
			} else {
				testCol--;
			}
		}
		fill++;
		int minCol = col - hori;
		if (!colFill.containsKey(minCol)) colFill.put(minCol,0);
		int min = colFill.get(minCol);
		if (fill < min) fill = min;
		colFill.put(col, fill);
		
		int x = col * 150;
		int y = fill * 50;
		return new Point(x, y);
	}
}

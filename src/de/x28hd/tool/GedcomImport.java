package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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
	Hashtable<String,String> places = new Hashtable<String,String>();
	HashSet<Integer> nonTreeEdges = new HashSet<Integer>();
	
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
	int maxVert = 10;
	GraphPanelControler controler;
	
	public GedcomImport(Document inputXml, GraphPanelControler controler) {

		if (!inputXml.getXmlEncoding().equals("UTF-8") && 
				!System.getProperty("file.encoding").equals("UTF-8")) {
			controler.displayPopup("<html>Warning: Umlauts may be distorted."
					+ "<br>Please start the program with"
					+ "<br><b>java -Dfile.encoding=UTF-8 -jar </b><em>path</em><b>\\dm.jar</b>" 
					+ "<br>from a command prompt or .bat file.</html>");
		}
		
//
//		Find input items

		NodeList itemList = inputXml.getElementsByTagName("IndividualRec");
		for (int i = 0; i < itemList.getLength(); i++) {
			Element node = (Element) itemList.item(i);
			String itemID = node.getAttribute("Id");
			String gn = "";
			String gnFull = "";
			String sn = "";
			
			NodeList IndivList = node.getElementsByTagName("IndivName");
		if (IndivList.getLength() > 0) {
			if (first) {
			    treeNode = new DefaultMutableTreeNode(new BranchInfo(1, itemID));
			    topNode = treeNode;
				model = new DefaultTreeModel(topNode);
				topID = itemID;
				first = false;
			}
			Element idElem = (Element) node.getElementsByTagName("IndivName").item(0);
			NodeList partList = idElem.getElementsByTagName("NamePart");
			if (partList.getLength() <= 0) {
				gn = idElem.getTextContent();
				System.out.println(itemID + " " + gn);
			} else {
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
			}
		}
			String pinfo = "";
			NodeList pinfoList = node.getElementsByTagName("PersInfo");
			for (int p = 0; p < pinfoList.getLength(); p++) {
				Element pinfoElem = (Element) pinfoList.item(p);
				NodeList infoList = pinfoElem.getElementsByTagName("Information");
				if (infoList.getLength() > 0) {
					pinfo = pinfo + infoList.item(0).getTextContent() + "<br />" ;
				}
			}
			String note = "";
			NodeList noteList = node.getElementsByTagName("Note");
			for (int n = 0; n < noteList.getLength(); n++) {
				Element noteElem = (Element) noteList.item(n);
				note = note + noteElem.getTextContent() + "<br />";
				note = note.replaceAll("\r", "<br />");
			}
			String labelString = gn;
			inputItems.put(itemID, labelString);
			gnFull = gnFull.replace("!", "");
			String detailString = gnFull + " " + sn + "<p>" + pinfo + note;
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
//		Get event dates & places for individuals above or marriages below
		
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
			
			NodeList placeList = node.getElementsByTagName("Place");
			String place = "";
			if (placeList.getLength() > 0) {
				Element placeElem = (Element) placeList.item(0);
				NodeList nameList = placeElem.getElementsByTagName("PlaceName");
				if (nameList.getLength() > 0) {
					Element nameElem = (Element) nameList.item(0);
					place = nameElem.getTextContent();
					place = place.trim();
				}
			}
			
			String eventType = ((Element) eventList.item(i)).getAttribute("Type");
			
			if (eventType.equals("marriage")) {
				marriages.put(itemID, year);
				places.put(itemID, place);
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
				details = details + "<br />" + eventType + " " + year + " " + place;
				graphNode.setDetail(details);
			}
		}

//		
//		Record input links and marriages
		
		NodeList famList = inputXml.getElementsByTagName("FamilyRec");
		for (int i = 0; i < famList.getLength(); i++) {
			Element node = (Element) famList.item(i);
			String itemID = node.getAttribute("Id");
			
			String year = "";
			String place = "";
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
				place = places.get(toItem);
//				System.out.println("Event " + itemID + ", Marriage = " + toItem + " year " + year);
			}
			String labelString = year;
			
			inputItems.put(itemID, labelString);
			inputItems2.put(itemID, labelString);
			String prep = "";
			if (!place.isEmpty()) prep = "in";
			String detailString = year + " " + prep + " " + place;
			inputNotes.put(itemID, detailString);
			addNode(itemID);
			
			NodeList childList = node.getElementsByTagName("Child");
//			if (childList.getLength() <= 0) continue;
			for (int c = 0; c < childList.getLength(); c++) {
				Element idElem = (Element) childList.item(c);
				Element link = (Element) idElem.getElementsByTagName("Link").item(0);
				String toItem = link.getAttribute("Ref");
				addEdge(itemID, toItem);
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
			
		}

//
//		Create discovery tree
//		(this tree does NOT represent the ancestral lineage but just 
//		the discovery sequence starting from an arbitrary starting point)
		
		int nodeID = inputID2num.get(topID);
		GraphNode node = nodes.get(nodeID);
		System.out.println("Starting. First node is: " + node.getLabel() + " (" + topID + ")" );
		done.add(topID);
		
		connectFamilies(node);
		gatherRelatives(topID, topNode);
		
		// traverse the discovery tree
		colFill.put(1,0);	// initialize odd cases
		colFill.put(-1,0);
		growGraph(topNode, "", 0, 1);
		
		// make cross-links pale
		Enumeration<Integer> edgeList2 = edges.keys();
		while (edgeList2.hasMoreElements()) {
			int id = edgeList2.nextElement();
			GraphEdge edge = edges.get(id);
			if (nonTreeEdges.contains(id)) edge.setColor("#f0f0f0");
		}
		
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
		String previousTrail = discoveredVia.get(nodeID);

//
//		Any neighbor
		
		Enumeration<GraphEdge> neighbors = node.getEdges();
		while (neighbors.hasMoreElements()) {
			GraphEdge edge = neighbors.nextElement();
			GraphNode otherEnd = node.relatedNode(edge);
			
			// If the arrow points away from the neighbor, the discovery direction is "back"
			int otherEndNum = otherEnd.getID();
			boolean back = false;
			int n1 = edge.getN1();
			if (otherEndNum == n1){
				back = true;
			}
			
//
// Analyze "otherEnd" to detect cross links
			
			String otherEndID = num2inputID.get(otherEndNum);
			boolean xref = false;
			
			if (otherEndID.equals(previousTrail)) continue;  // trivial case
			if (done.contains(otherEndID)) xref = true;
			else if (discoveredVia.containsKey(otherEndID)) xref= true;
			else if (discoveredVia.containsKey(nodeID) && discoveredVia.get(nodeID).equals(otherEndID)) {
				System.out.println("Error GI122: Odd case: " + nodeID + " -> " + otherEndID);
				continue;
			}
			
			//	Cross link found: swap discovery hierarchy 
			
			if (xref) {
				String currentTrail = discoveredVia.get(otherEndID);
				if (currentTrail != nodeID) {	// TODO maybe just topID ?
					
					// Family node XOR link back, this means a children link and should not  
					// be a cross link. Swap it with a marriage link  
					
					if (inputItems2.containsKey(nodeID) != back) {
						
						// Find the edge to be recolored
						Enumeration<GraphEdge> neighbors2 = node.getEdges();
						int wantedEdgeID = -1;
						while (neighbors2.hasMoreElements()) {
							GraphEdge edge2 = neighbors2.nextElement();
							GraphNode otherEnd2 = node.relatedNode(edge2);
							int previousNum = inputID2num.get(previousTrail);
							GraphNode previousNode = nodes.get(previousNum);
							if (otherEnd2 != previousNode) continue;
							wantedEdgeID = edge2.getID();
						}
						nonTreeEdges.add(wantedEdgeID);
						
						// Re-specify discovery order
						discoveredVia.put(nodeID, otherEndID);
						discoveryPointsBack.put(nodeID, back);
						done.add(otherEndID);
						continue;

					// is already a marriage link, just re-color it
					} else {
						nonTreeEdges.add(edge.getID());
						continue;
					}
				}
			}
			
			// Normal tree links
			discoveredVia.put(otherEndID, nodeID);
			discoveryPointsBack.put(otherEndID, back);
			done.add(otherEndID);
//			if (!xref) connectFamilies(otherEnd);
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

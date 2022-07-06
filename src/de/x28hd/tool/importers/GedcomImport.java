package de.x28hd.tool.importers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.x28hd.tool.BranchInfo;
import de.x28hd.tool.GraphEdge;
import de.x28hd.tool.GraphNode;
import de.x28hd.tool.GraphPanelControler;
import de.x28hd.tool.exporters.TopicMapStorer;

public class GedcomImport implements ListSelectionListener, ActionListener {
	
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
	TreeMap<String,String> namePicker = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
	Hashtable<String,String> famNames = new Hashtable<String,String>();
	
	//	Keys for nodes and edges, incremented in addNode and addEdge
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	Hashtable<Integer,String> num2inputID = new  Hashtable<Integer,String>();
	int j = -1;
	int edgesNum = 0;
	
	// For topology
	Hashtable<String,String> nextUp = new Hashtable<String,String>();
	Hashtable<String,String> nextDown = new Hashtable<String,String>();
	HashSet<Integer> nonTreeEdges = new HashSet<Integer>();
    HashSet<String> ancestors = new HashSet<String>(); 
    DefaultMutableTreeNode treeNode = null;
    DefaultMutableTreeNode topNode = null;
    DefaultMutableTreeNode newNode = null;
    String topID = "";
    String topPick;
    Boolean first = true;
    DefaultTreeModel model = null;
    int shiftedOutliers = 1;
    
    //	Accessories for user selections
    int scope = 3;
    JFrame frame;
	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			discovery();
		}
	};
    JList<Item> list;
    class Item {
    	private String id;
    	private String description;
    	public Item(String id, String description) {
    		this.id = id;
    		this.description = description;
    	}
    	public String getId() {
    		return id;
    	}
    	public String getDescription() {
    		return description;
    	}
    	public String toString() {
    		return description;
    	}
    }
    JButton continueButton;
	boolean transit = false;
	JCheckBox transitBox = null;
	Item topItem;
	JLabel instruction2;
	boolean shift = false;
	JCheckBox shiftBox = null;
	
	//	Constants
	int maxVert = 10;
	GraphPanelControler controler;
	
	public GedcomImport(Document inputXml, GraphPanelControler controler) {
		this.controler = controler;
        controler.getControlerExtras().stopHint();	//	to keep the frame in front

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
//					model = new DefaultTreeModel(topNode);
					topID = itemID;
					first = false;
					topItem = new Item(itemID, sn + ", " + gn);
				}
				Element idElem = (Element) node.getElementsByTagName("IndivName").item(0);
				NodeList partList = idElem.getElementsByTagName("NamePart");
				if (partList.getLength() <= 0) {
					gn = idElem.getTextContent();
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
							famNames.put(itemID, sn);
						}
					}
				}
			}
			String pickString = sn + ", " + gn;
			if (namePicker.containsKey(pickString)) pickString = pickString + " (" + itemID + ")";
			namePicker.put(pickString, itemID);

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
			for (int c = 0; c < childList.getLength(); c++) {
				Element idElem = (Element) childList.item(c);
				Element link = (Element) idElem.getElementsByTagName("Link").item(0);
				String toItem = link.getAttribute("Ref");
				addEdge(itemID, toItem);
				if (!nextUp.containsKey(toItem)) nextUp.put(toItem, itemID);
			} 
		
			NodeList fathList = node.getElementsByTagName("HusbFath");
			if (fathList.getLength() > 0) {
				Element idElem = (Element) fathList.item(0);
				Element link = (Element) idElem.getElementsByTagName("Link").item(0);
				String toItem = link.getAttribute("Ref");
				addEdge(toItem, itemID);
				if (!nextDown.containsKey(toItem)) nextDown.put(toItem, itemID);
				if (famNames.containsKey(toItem) && labelString.compareTo("1990") < 0) {
					labelString = famNames.get(toItem) + " " + labelString;
					int num = inputID2num.get(itemID);
					GraphNode gnode = nodes.get(num);
					gnode.setLabel(labelString);
				}
			}
			
			NodeList mothList = node.getElementsByTagName("WifeMoth");
			if (mothList.getLength() > 0) {
				Element idElem = (Element) mothList.item(0);
				Element link = (Element) idElem.getElementsByTagName("Link").item(0);
				String toItem = link.getAttribute("Ref");
				addEdge(toItem, itemID);
				if (!nextDown.containsKey(toItem)) nextDown.put(toItem, itemID);
			}
		}
		
		SortedMap<String,String> nameSorter = (SortedMap<String,String>) namePicker;
		SortedSet<String> nameSet = (SortedSet<String>) nameSorter.keySet();
		Iterator<String> it = nameSet.iterator();
		DefaultListModel<Item> listModel = new DefaultListModel<Item>();
		while (it.hasNext()) {
			String pickName = it.next();
			String pickID = namePicker.get(pickName);
			Item item = new Item(pickID, pickName);
			if (pickID == topID) topItem = item;
			listModel.addElement(item);
		}
        frame = new JFrame("Import options");
        frame.setLocation(100, 170);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(myWindowAdapter);
		frame.setLayout(new BorderLayout());
		
        list = new JList<Item>(listModel);
        list.setSelectedValue(topItem, true);
        list.addListSelectionListener(this);
        list.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.add(new JScrollPane(list));
        
        JPanel topbar = new JPanel();
        topbar.setLayout(new BorderLayout());
		topbar.setBorder(new EmptyBorder(10, 10, 10, 10));
		JLabel instruction1 = new JLabel("<html><body>" +
			    "Start with <b>" + topItem.description + 
			    "</b> &nbsp; or select another &nbsp;<em>birthname, givenname</em></body></html>");
		topbar.add(instruction1, "West");
		frame.add(topbar,"North");

		JPanel toolbar = new JPanel();
        toolbar.setLayout(new BorderLayout());
		toolbar.setBorder(new EmptyBorder(10, 10, 10, 10));
		instruction2 = new JLabel("<html><body>" +
	    "Which relatives of <b>" + topItem.description + 
	    "</b> should be included?</body></html>");
		toolbar.add(instruction2, "North");
        JPanel buttons = new JPanel();
        buttons.setLayout(new BorderLayout());
        continueButton = new JButton("Continue");
        continueButton.addActionListener(this);
        continueButton.setSelected(true);
		JButton cancelButton = new JButton("Cancel");
	    cancelButton.addActionListener(this);
        buttons.add(continueButton, "East");
		buttons.add(cancelButton, "West");
		toolbar.add(buttons,"East");
		
		JPanel radioPanel = new JPanel(new FlowLayout());
		ButtonGroup buttonGroup = new ButtonGroup();
		
		String[] radioCaptions = {"Just ancestors", "Ancestors plus descendants", 
				"Descendants of all ancestors", "All"};
		for (int i = 0; i < 4; i++) {
			JRadioButton radio = new JRadioButton(radioCaptions[i]);
			radio.setActionCommand(i + "");
			radio.addActionListener(this);
			if (i == 3) radio.setSelected(true);
			buttonGroup.add(radio);
			radioPanel.add(radio);
		}
		
		JPanel options = new JPanel(new BorderLayout());
		options.add(radioPanel, "North");
		shiftBox = new JCheckBox("Improve view of shifted generations (e.g. for history by dynasties) ?");
//		shiftBox.setActionCommand("shift");
		options.add(shiftBox, "South");
		toolbar.add(options, "West");

		frame.add(toolbar,"South");
        frame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
        frame.setMinimumSize(new Dimension(596, 418));
        frame.setVisible(true);
	}
	
//
//	Create discovery tree
//	(this tree does NOT represent the ancestral lineage but just 
//	the discovery sequence starting from an arbitrary starting point)
		
	public void discovery() {

		int nodeID = inputID2num.get(topID);
		GraphNode node = nodes.get(nodeID);
		System.out.println("Starting. First node is: " + node.getLabel() + " (" + topID + ")" );
		done.add(topID);
		ancestors.add(topID);
		discoveredVia.put(topID,  "");
		
		if (scope == 3) {	//	All
			connectFamiliesAll(node);
			gatherRelatives(topID, topNode);
		} else {
			
			//	Up: find ancestors and build the upward part of the discovery tree
			connectFamiliesUp(node);
			gatherRelatives(topID, topNode);

			//	Down: find descendants of all ancestors
			if (scope > 0) { 

				if (scope == 1) { //  Just my topID's descendants
					int nodeNum = inputID2num.get(topID);
					GraphNode gnode = nodes.get(nodeNum);
					connectFamiliesDown(gnode, false);
				} else { 
					Enumeration<TreeNode> ancestorTree = topNode.breadthFirstEnumeration();
					while (ancestorTree.hasMoreElements()) {
						TreeNode nestNode = ancestorTree.nextElement();
						String myID = ((DefaultMutableTreeNode) nestNode).getUserObject().toString();
						int nodeNum = inputID2num.get(myID);
						GraphNode gnode = nodes.get(nodeNum);
						connectFamiliesDown(gnode, false);
					}
				}

				//	Clear the up tree and start one with up and down
				topNode.removeAllChildren();
				gatherRelatives2(topID, topNode);
			}

			//	Delete all unused nodes
			Enumeration<Integer> pruneCheckList = nodes.keys();
			while (pruneCheckList.hasMoreElements()) {
				int testNum = pruneCheckList.nextElement();
				String testID = num2inputID.get(testNum);
				if (!discoveredVia.containsKey(testID)) {
					GraphNode node2 = nodes.get(testNum);
					Enumeration<GraphEdge> edgeList = node2.getEdges();
					while (edgeList.hasMoreElements()) {
						GraphEdge edge = edgeList.nextElement();
						int edgeID = edge.getID();
						GraphNode otherEnd2 = node2.relatedNode(edge);
						otherEnd2.removeEdge(edge);
						edges.remove(edgeID);
					}
					nodes.remove(testNum);
				}
			}
		}
		
		// traverse the discovery tree
		colFill.put(1,0);	// initialize odd cases
		colFill.put(-1,0);
		growGraph(topNode, "", 0, 1);
		
		// final touches:
		// make cross-links pale
		// and shift superficially visible generation gaps
		Enumeration<Integer> edgeList2 = edges.keys();
		while (edgeList2.hasMoreElements()) {
			int id = edgeList2.nextElement();
			GraphEdge edge = edges.get(id);
			if (nonTreeEdges.contains(id)) edge.setColor("#f0f0f0");
			if (shift) shiftRight(edge);
		}
		if (shift) {
			while (shiftedOutliers > 0) {
				shiftedOutliers = 0;
				Enumeration<Integer> edgeList3 = edges.keys();
				while (edgeList3.hasMoreElements()) {
					int id = edgeList3.nextElement();
					GraphEdge edge = edges.get(id);
					shiftOutliers(edge);
				}
			}
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
	
	private void shiftRight(GraphEdge edge) {
		GraphNode node1 = edge.getNode1();
		GraphNode node2 = edge.getNode2();
		Point p = node2.getXY();
		int x1 = node1.getXY().x;
		int x2 = p.x;
		int diff = x2 - x1;
		if (diff > 0) return;
		p.translate(300, 0);
		Enumeration<GraphEdge> neighbors = node2.getEdges();
		while(neighbors.hasMoreElements()) {
			GraphEdge edge2 = neighbors.nextElement();
			shiftRight(edge2);		// recursive
		}
	}
	
	private boolean shiftOutliers(GraphEdge edge) {
		boolean allowed = true;
		GraphNode node1 = edge.getNode1();
		GraphNode node2 = edge.getNode2();
		Point p = node1.getXY();
		int x1 = p.x;
		int x2 = node2.getXY().x;
		int diff = x2 - x1;
		if (diff <= 150) return true;	// allowed but not necessary
		Enumeration<GraphEdge> neighbors = node1.getEdges();
		while(neighbors.hasMoreElements()) {
			GraphEdge edge2 = neighbors.nextElement();
			if (edge2 == edge) continue;	// this edge was just processed
			GraphNode node3 = edge2.getNode1();
			if (node3 == node1) return false;	// another branch? shift too complicated
			
			allowed = allowed && shiftOutliers(edge2);		// recursive
		}
		if (allowed) {
			p.translate(diff - 150, 0);
			shiftOutliers(edge);
			shiftedOutliers++;
		}
		return allowed;
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

		Point p = new Point(40, -40);
		
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

	public void connectFamiliesAll(GraphNode node) {
		
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
			connectFamiliesAll(otherEnd);
		}
	}
	
	public void connectFamiliesUp(GraphNode node) {
		int nodeNum = node.getID();
		String nodeID = num2inputID.get(nodeNum);
		
		if (ancestors.contains(nodeID)) {
			Enumeration<GraphEdge> edgeList2 = edges.elements();
			while (edgeList2.hasMoreElements()) {
				GraphEdge edge = edgeList2.nextElement();
				int n1 = edge.getN1();
				GraphNode node1 = edge.getNode1();
				GraphNode node2 = edge.getNode2();
				if (node != node2) continue;
				String testUp = num2inputID.get(n1);
				if (!discoveredVia.containsKey(testUp)) {
					discoveredVia.put(testUp, nodeID);
					discoveryPointsBack.put(testUp, true);
				} else continue;
				ancestors.add(testUp);
				connectFamiliesUp(node1);	// recurse
			}
		}
	}
	
	public void connectFamiliesDown(GraphNode node, boolean recursive) {
		int nodeNum = node.getID();
		String nodeID = num2inputID.get(nodeNum);
		
		Enumeration<GraphEdge> edgeList1 = edges.elements();
		while (edgeList1.hasMoreElements()) {
			GraphEdge edge = edgeList1.nextElement();
			int n2 = edge.getN2();
			GraphNode node1 = edge.getNode1();
			GraphNode node2 = edge.getNode2();
			if (node != node1) continue;
			String testDown = num2inputID.get(n2);
			if (!discoveredVia.containsKey(testDown)) {
				discoveredVia.put(testDown, nodeID);
				discoveryPointsBack.put(testDown, false);
			} else continue;
			connectFamiliesDown(node2, true);	// recurse
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

	public void gatherRelatives2(String parent, DefaultMutableTreeNode treeNode) {
		Enumeration<String> parentsEnum = discoveredVia.keys();
		while (parentsEnum.hasMoreElements()) {
			String testChild = parentsEnum.nextElement();
			String testParent = discoveredVia.get(testChild);
			if (!testParent.equals(parent)) {
				continue;
			} else {
				int hori = 1;
				if (discoveryPointsBack.get(testChild)) {
					hori = -1;
					if (!ancestors.contains(parent)) continue;
				}
				newNode = new DefaultMutableTreeNode(new BranchInfo(hori, testChild));
				treeNode.add(newNode);
				gatherRelatives2(testChild, newNode);
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
		
		Enumeration<TreeNode> treeChildren = nestNode.children();
		
		//	Prepare for reordering the current level of children
		Hashtable<String,DefaultMutableTreeNode> childrenMap = 
				new Hashtable<String,DefaultMutableTreeNode>();
		TreeMap<Double,String> orderMap = new TreeMap<Double,String>();
		SortedMap<Double,String> orderList = (SortedMap<Double,String>) orderMap;
		double disambig = 0.001;

		//	Unordered list
		while (treeChildren.hasMoreElements()) {
			TreeNode child = treeChildren.nextElement();
			String itemID = ((DefaultMutableTreeNode) child).getUserObject().toString();
			childrenMap.put(itemID, (DefaultMutableTreeNode) child);
			int weight = ((DefaultMutableTreeNode) child).getLeafCount();
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

	public void valueChanged(ListSelectionEvent arg0) {
		Item selected = (Item) list.getSelectedValue();
		String itemID = selected.getId();
		treeNode = new DefaultMutableTreeNode(new BranchInfo(1, itemID));
		topNode = treeNode;
		model = new DefaultTreeModel(topNode);
		topID = itemID;
		topItem = selected;
		instruction2.setText("<html><body>" +
			    "Which relatives of <b>" + topItem.description + 
			    "</b> should be included?</body></html>");
		frame.repaint();
	}

	public void actionPerformed(ActionEvent arg0) {
		String command = arg0.getActionCommand();
		if (command == "Cancel") {
			transit = false;
		} else if (command == "Continue") {
			shift = shiftBox.isSelected();
	        frame.setVisible(false);
	        frame.dispose();
	        discovery();
	        return;
		}
		scope = -1;
	    for (int i = 0; i < 4; i++) {
	    	if (command.equals(i + "")) scope = i;
	    }
	}
}

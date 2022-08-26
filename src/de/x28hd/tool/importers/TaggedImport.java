package de.x28hd.tool.importers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;
import edu.uci.ics.jung.graph.util.Pair;  

public class TaggedImport implements ActionListener, Comparator<HashSet<String>> {
	
	// Standard mytool fields
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	PresentationService controler;
	int j = 0;
	int maxVert = 10;
	String dataString = "";
	int edgesNum = 0;
	
	// For main part
	HashSet<String> subset = null;
	HashSet<HashSet<String>> subsets = new HashSet<HashSet<String>>();
	Hashtable<String,HashSet<String>> item2subset = new Hashtable<String,HashSet<String>>(); 
	TreeSet<HashSet<String>> subsetsBySize = new TreeSet<HashSet<String>>(this);
	
	Hashtable<HashSet<String>,HashSet<String>> subset2itemGroup = 
			new Hashtable<HashSet<String>,HashSet<String>>(); 
	
	Hashtable<HashSet<String>,GraphNode> subset2node = new Hashtable<HashSet<String>,GraphNode>();
	Hashtable<String,GraphNode> label2node = new Hashtable<String,GraphNode>();
	HashSet<GraphNode> singles = new HashSet<GraphNode>();
	Hashtable<String,String> catsLong = new Hashtable<String,String>();
	String[] records;
	String contentString = "";
	String suspendList = "";
	boolean fuse = false;
	boolean hide = false;
	boolean suspend = false;
	boolean suppress = false;
	
	Hashtable<String,HashSet<String>> catUnits = new Hashtable<String,HashSet<String>>();
	Hashtable<Pair<String>,String> linkDetails = new Hashtable<Pair<String>,String>();
	Hashtable<Pair<String>,Integer> linkStrengths = new Hashtable<Pair<String>,Integer>();
	
	JDialog dialog;
	JCheckBox fuseBox;
	JCheckBox hideBox;
	JCheckBox suppressBox;
	JCheckBox suspendBox;
	
	// Category combination and item group, used to create node/s
	public class CombiUnit {	
		int type = COMBI;
		static final int CAT = 0;	// items belonging to a single category only
		static final int COMBI = 1;	// several items belonging to a certain combination of categories, fused
		static final int BUNCH = 2;	// several items belonging to a certain combination of categories, not fused
		static final int UNIQUE = 3;	// single item belonging to a (representing a) unique combination of categories
		String [] colorStrings = {"#ff0000", "#ffdddd", "#ddccdd", "#ccdddd"};
		HashSet<String> mySubset;
		HashSet<String> itemGroup = new HashSet<String>();
		String label = "";
		String links = "";
		String detail = "";
		public CombiUnit(HashSet<String> subset) {
			mySubset = subset;
		}
		public void processItems() {
			Iterator<String> cats = mySubset.iterator();
			int catCount = 0;
			// category subsets
			while (cats.hasNext()) {
				catCount++;
				String cat = cats.next();
				label += cat + " ";
				if (!catsLong.containsKey(cat)) catsLong.put(cat, cat);	// fallback
				String catLong = catsLong.get(cat);
				if (fuse) {
					links += "<b>" + catLong + "</b><br/>";
				} else {
					links += "<a href=\"#" + catLong + "\">" + catLong + "</a><br/>";
				}
			}
			label = label.trim();
			if (catCount <= 1) {	// pure category
				type = CombiUnit.CAT;
				if (catsLong.containsKey(label)) label = catsLong.get(label);
			}
			// item groups
			Iterator<String> groupItems = itemGroup.iterator();
			int itemCount = 0;
			while (groupItems.hasNext()) {
				itemCount++;
				detail += groupItems.next() + "<br/>";
			}
			if (catCount > 1) {
				if (itemCount <= 1) {	// category combination with just 1 item
					label = detail;
					detail = links;
					label = label.replace("<br/>", "");
					type = CombiUnit.UNIQUE;
				} else {				// arbitrary category combination
					if (fuse) {
						detail = links + "<br/>" + detail;
						type = CombiUnit.COMBI;
					} else {
						detail += links;
						type = CombiUnit.BUNCH;
					}
				}
			}
		}
		public void createNodes() {
			if (hide && type != CombiUnit.CAT) return;
			if (type != CombiUnit.BUNCH) {
				GraphNode node = createNode(label, detail);
				subset2node.put(mySubset, node);
			} else {
				Iterator<String> groupItems = itemGroup.iterator();
				while (groupItems.hasNext()) {
					String label = groupItems.next();
					String detail = label;
					createNode(label, detail);
				}
			}
		}
		public GraphNode createNode(String label, String detail) {
			GraphNode node = null;
			if (label2node.containsKey(label)) {
				node = label2node.get(label); 
			} else {
				String colorString = colorStrings[type];
				node = addNode(label, detail, colorString);
			}
			if (type == CombiUnit.UNIQUE) singles.add(node);
			label2node.put(label, node);
			return node;
		}
		public HashSet<String> getItemGroup() {
			return itemGroup;
		}
	}
		
	Hashtable<HashSet<String>,CombiUnit> combiUnits = new Hashtable<HashSet<String>,CombiUnit>();

	public TaggedImport(File file, PresentationService controler) {
		this.controler = controler;

		//	Read the input
		FileInputStream fileInputStream = null;
		Utilities utilities = new Utilities();
		String contentString = "";
		try {
			fileInputStream = new FileInputStream(file);
			InputStream in = (InputStream) fileInputStream;
				contentString = utilities.convertStreamToString(in);
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error TGI101 " + e);
		} catch (IOException e) {
			System.out.println("Error TGI102 " + e);
		}
		
		new TaggedImport(contentString, controler);
	}
	
	public TaggedImport(String contentString, PresentationService controler) {
		this.controler = controler;

		records = contentString.split("\\n");
		System.out.println(records.length + " records read");

//
//		For each item, consider the list of categories ('subset') to which the item belongs
		
		for (int i = 0; i < records.length; i++) {
			String line = records[i].trim();
			
			if (!line.contains("\t")) continue;
			String[] fields = line.split("\\t");
			String item = fields[0];
			// TODO report doubles
			String cat = fields[1];

			// Some initialization along the way
			
			if (item.startsWith(cat) && !cat.equals(item)) {
				catsLong.put(cat, item);	// Long category titles appended to inputs
				continue;
			}
			
			// Guarantee category nodes even if no items belong exclusively to them,
			// and, along the way, prepare for hide option 
			HashSet<String> allItemsOfCat;
			if (!catUnits.containsKey(cat)) {
				allItemsOfCat = new HashSet<String>();
				catUnits.put(cat, allItemsOfCat);
				subset = new HashSet<String>();
				subset.add(cat);
				CombiUnit combiUnit = new CombiUnit(subset);
				combiUnits.put(subset, combiUnit);
				subsetsBySize.add(subset);
			} else {
				allItemsOfCat = catUnits.get(cat);
			}
			allItemsOfCat.add(item);

			
			// Main work starts here
			
			if (item2subset.containsKey(item)) {
				subset = item2subset.get(item);
				if (!subset.contains(cat)) {
					subset.add(cat); 
				}
			} else {
				subset = new HashSet<String>();
				subset.add(cat);
				item2subset.put(item, subset);
			}
		}
		
//
//		Determine the unique subsets
		
		Enumeration<String> items = item2subset.keys();
		while (items.hasMoreElements()) {
			String item = items.nextElement();
			subset = item2subset.get(item);
			CombiUnit combiUnit;
			if (!combiUnits.containsKey(subset)) {
				combiUnit = new CombiUnit(subset);
				combiUnits.put(subset, combiUnit);
				subsetsBySize.add(subset);
			} else {
				combiUnit = combiUnits.get(subset);
			}
			
			// and assemble the group of items sharing a given category subset
			HashSet<String> itemGroup = combiUnit.getItemGroup();
			if (!itemGroup.contains(item)) {
				itemGroup.add(item);
			}
		}
		
		askForSimplify();
	}
	
	public void mainPart2() {
		
		// Weed out if suppress or suspend option was specified
		if (suppress || suspend) {		// note that the hide option has separate suppress processing
			Enumeration<HashSet<String>> iter0 = combiUnits.keys();
			while (iter0.hasMoreElements()) {
				subset = iter0.nextElement();
				HashSet<String> itemGroup = combiUnits.get(subset).getItemGroup();
				if ((suppress && itemGroup.size() == 1)
						|| (suspend && subset.size() > 3)) {
					String items = combiUnits.get(subset).getItemGroup().toString();
					Iterator<String> suppIter = subset.iterator();
					while (suppIter.hasNext()) {
						String cat = suppIter.next();
						String left = "";
						if (!catsLong.containsKey(cat)) {
							left = cat;
						} else {
							left = catsLong.get(cat);
						}
						String line = left + "\t" + items + "\n";
						suspendList += line;
					}
					combiUnits.remove(subset);
					subsetsBySize.remove(subset);
				}
			}
		}
		
		//
		// Create nodes via the CombiUnit for each subset enumerating its item group
		
		Enumeration<HashSet<String>> iter1 = combiUnits.keys();
		while (iter1.hasMoreElements()) {
			subset = iter1.nextElement();
			CombiUnit combiUnit = combiUnits.get(subset);
			combiUnit.processItems();

			combiUnit.createNodes();
		}
		
		//
		// Create the edges
		
		Enumeration<HashSet<String>> iter2 = combiUnits.keys();
		while (iter2.hasMoreElements()) {
			subset = iter2.nextElement();
			HashSet<String> relRepresented = new HashSet<String>();
			Iterator<HashSet<String>> iter2a = subsetsBySize.iterator();

			if (!fuse && !hide) {
				
				// connect item nodes to their category
				
				if (subset.size() == 1) continue;	// no categories at this end
				CombiUnit combiUnit = combiUnits.get(subset);
				HashSet<String> itemGroup = combiUnit.getItemGroup();
				
				Iterator<String> groupItems = itemGroup.iterator();
				while (groupItems.hasNext()) {
					String item = groupItems.next();
					GraphNode node1 = label2node.get(item);
					Iterator<String> iter3 = subset.iterator();
					while (iter3.hasNext()) {
						GraphNode node2;
						String cat = iter3.next();
						cat = catsLong.get(cat);
						if (label2node.containsKey(cat)) {
						node2 = label2node.get(cat);
						} else {
							System.out.println("Error TGI107 " + cat + "\t" + subset);
							continue;
						}
						addEdge(node1, node2);
					}
				}
				
			} else if (fuse) {
				
				// Find subset relationships and create an edge for each one
				
				while (iter2a.hasNext()) {
					HashSet<String> testSubset = iter2a.next();
					if (testSubset.equals(subset)) continue;
					if (subset.size() <= testSubset.size()) continue;
					if (subset.containsAll(testSubset)) {
						Iterator<String> iter3 = testSubset.iterator();
						boolean done = true;
						while (iter3.hasNext()) {
							String member = iter3.next();
							if (relRepresented.contains(member)) continue;
							relRepresented.add(member);
							done = false;
						}
						if (done && testSubset.size() == 1) continue;
						GraphNode node1 = null;
						GraphNode node2 = null;
						node1 = subset2node.get(subset);
						node2 = subset2node.get(testSubset);
						addEdge(node1, node2);
					}
				}
			}
		}
		
		if (hide) hiding();	
		
		// Add hyperlinks in the detail pane
		
		Enumeration<GraphEdge> allEdges = edges.elements();
		while (allEdges.hasMoreElements()) {
			GraphEdge edge = allEdges.nextElement();
			GraphNode node1 = edge.getNode1();
			GraphNode node2 = edge.getNode2();
			if (!singles.contains(node1)) {
				String otherEnd = node2.getLabel();
				String thisDetail = node1.getDetail() + "<br/><a href=\"#" + otherEnd + "\">" + otherEnd + "</a>";
				node1.setDetail(thisDetail);
			}
			if (!singles.contains(node2)) {
				String otherEnd = node1.getLabel();
				String thisDetail = node2.getDetail() + "<br/><a href=\"#" + otherEnd + "\">" + otherEnd + "</a>";
				node2.setDetail(thisDetail);
			}
		}
		
		System.out.println(nodes.size() + " nodes added");
	   	controler.getControlerExtras().toggleHashes(true);

//
//		pass on
		
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error TGI108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error TGI109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error TGI110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.getControlerExtras().setTreeModel(null);
    	controler.getControlerExtras().setNonTreeEdges(null);
    	
    	if (suspendList.isEmpty()) return;
		FileWriter list;
		try {
			list = new FileWriter(System.getProperty("user.home") + 
					File.separator + "Desktop" + File.separator + "x28list.txt");
			list.write(suspendList);
			list.close();
		} catch (IOException e) {
			System.out.println("Error RG102 " + e);			
		}
	}
	
//
//	Other major methods
	
	public GraphNode addNode(String label, String detail, String colorString) {
			j++;
			int id = 100 + j;

			int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			int x = 40 + (j/maxVert) * 150;
			Point p = new Point(x, y);
			GraphNode topic = new GraphNode (id, p, Color.decode(colorString), label, detail);	

			nodes.put(id, topic);
			return topic;
	}
	
	public void addEdge(GraphNode node1, GraphNode node2) {
		addEdge(node1, node2, "");
	}
	public void addEdge(GraphNode node1, GraphNode node2, String detail) {
		edgesNum++;
		GraphEdge edge = new GraphEdge(edgesNum, node1, node2, Color.decode("#d8d8d8"), detail);
		edges.put(edgesNum,  edge);
	}

	public void hiding() {
		// I tried to integrate this into the subset/ itemGroup logic but that was less transparent
		controler.displayPopup("<html><b>Note:</b> For once, lines have now info in the detail pane.\n"
				+ "This rarely used functionality may be withdrawn some day.");
		Enumeration<String> cats = catUnits.keys();	// loop through categories
		while (cats.hasMoreElements()) {
			String currentCat = cats.nextElement();
			GraphNode node1 = null;
			String label = currentCat;
			if (catsLong.containsKey(currentCat)) label = catsLong.get(currentCat);
			if (label2node.containsKey(label)) {
				node1 = label2node.get(label); 
			} else {
				System.out.println("Error TGI105 " + label);
				continue;
			}
			Enumeration<String> cats2 = catUnits.keys();
			HashSet<String> set1 = catUnits.get(currentCat);
			while (cats2.hasMoreElements()) {			// loop through later categories
				String cat2 = cats2.nextElement();
				if (cat2.compareTo(currentCat) <= 0) continue;
				Pair<String> catCombi = new Pair<String>(currentCat, cat2);
				HashSet<String> set2 = catUnits.get(cat2);
				Iterator<String> items1 = set1.iterator();	// loop through items
				while (items1.hasNext()) {
					String testItem = items1.next();
					if (set2.contains(testItem)) {
						String details = "";
						int linkStrength = 0;
						if (linkDetails.containsKey(catCombi)) {
							details = linkDetails.get(catCombi);
							linkStrength = linkStrengths.get(catCombi);
						}
						details += testItem + "<br/>";
						linkStrength++;
						linkDetails.put(catCombi, details);
						linkStrengths.put(catCombi, linkStrength);
					}
				}
				GraphNode node2 = null;
				String label2 = cat2;
				if (catsLong.containsKey(cat2)) label2 = catsLong.get(cat2);
				if (label2node.containsKey(label2)) {
					node2 = label2node.get(label2); 
				} else {
					System.out.println("Error TGI106 " + label2);
					continue;
				}
				if (linkDetails.containsKey(catCombi)) {
					int strength = linkStrengths.get(catCombi);
					if (suppress && strength < 2) continue;
					String detail = linkDetails.get(catCombi);
					addEdge(node1, node2, detail);
				}
			}
		}
	}

	// UI accessories
	
	public void askForSimplify() {
		
			// Ask for simplify option
			dialog = new JDialog(controler.getMainWindow(), "Option");
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			dialog.setMinimumSize(new Dimension(338, 246));
			dialog.setLocation(dim.width/2-dialog.getSize().width/2 - 169, 
					dim.height/2-dialog.getSize().height/2 - 113);		
			dialog.setLayout(new BorderLayout());
			
			// Stats for decision
			Enumeration<HashSet<String>> iter0 = combiUnits.keys();
			
			int catCount = 0;
			int itemCount = 0;
			int catSum = 0;
			int itemSum = 0;
			int subsetCount = 0;
			while (iter0.hasMoreElements()) {
				subset = iter0.nextElement();
				catCount = subset.size();
				CombiUnit combiUnit = combiUnits.get(subset);
				itemCount = combiUnit.getItemGroup().size();
				if (catCount > 1) {
					subsetCount++;
					catSum += catCount;
					itemSum += itemCount;
				}
			}
			String avgCats = String.format("%.2f", ((float) catSum)/subsetCount);
			String avgItems = String.format("%.2f", ((float) itemSum)/subsetCount);
			
			String question = "<html>We have " + subsetCount + " combinations of the " + catsLong.size() + " categories,<br/>"
					+ "containing an average of " + avgCats + " categories and<br/>"
					+ avgItems + " items of the " + itemSum + " items that belong to multiple<br/>"
					+ "categories.<br/><br/>"
					+ "To simplify the map, you may<br/><br/>";
	
			JLabel info = new JLabel(question);
			info.setBorder(new EmptyBorder(10, 20, 10, 20));
			dialog.add(info, BorderLayout.NORTH);
			JPanel optionLines = new JPanel();
			optionLines.setLayout(new BoxLayout(optionLines, BoxLayout.Y_AXIS));
			
			JPanel fuseInfo = new JPanel();
			fuseInfo.setLayout(new BorderLayout());
			fuseBox = new JCheckBox("<html><b>fuse</b> all items of a given combination into a single icon;");
			fuseBox.setActionCommand("fuse");
			fuseBox.addActionListener(this);
			fuseInfo.add(fuseBox, BorderLayout.WEST);
			optionLines.add(fuseInfo);

			JPanel hideInfo = new JPanel();
			hideInfo.setLayout(new BorderLayout());
			hideBox = new JCheckBox("<html><b>hide</b> the shared items into the line details;");
			hideBox.setActionCommand("hide");
			hideBox.addActionListener(this);
			hideInfo.add(hideBox, BorderLayout.WEST);
			optionLines.add(hideInfo);

			JPanel suspendInfo = new JPanel();
			suspendInfo.setLayout(new BorderLayout());
			suspendBox = new JCheckBox("<html><b>suspend</b> the largest combinations (> 3, into a file);");
			suspendInfo.add(suspendBox, BorderLayout.WEST);
			optionLines.add(suspendInfo);

			JPanel suppressInfo = new JPanel();
			suppressInfo.setLayout(new BorderLayout());
			suppressBox = new JCheckBox("<html><b>suppress</b> combinations shared by less than 2 items.");
			suppressInfo.add(suppressBox, BorderLayout.WEST);
			optionLines.add(suppressInfo);

			JPanel bottom = new JPanel();
			bottom.setLayout(new BorderLayout());
			JButton nextButton = new JButton("Next >");
			nextButton.setActionCommand("simplify");
			nextButton.addActionListener(this);
			bottom.add(nextButton, BorderLayout.EAST);
			optionLines.add(bottom);
			
			dialog.add(optionLines, BorderLayout.SOUTH);
			dialog.setVisible(true);
		}

	// Accessories for interfaces
	
	public void actionPerformed(ActionEvent arg0) {
			if (arg0.getActionCommand().equals("fuse")) {
	    	fuse = fuseBox.isSelected();
	    	hideBox.setSelected(hideBox.isSelected() && !fuse);
	    	hideBox.setEnabled(!fuse);
	    	dialog.repaint();
	    } else if (arg0.getActionCommand().equals("hide")) {
	    	hide = hideBox.isSelected();
	    	fuseBox.setSelected(fuseBox.isSelected() && !hide);
	    	fuseBox.setEnabled(!hide);
	    	dialog.repaint();
	    } else if (arg0.getActionCommand().equals("simplify")) {
	    	suspend = suspendBox.isSelected();
	    	suppress = suppressBox.isSelected();
	    	dialog.dispose();
	    	System.out.println("Fuse " + fuse + ", hide " + hide + ", suspend " + suspend + ", suppress " + suppress);
	    	mainPart2();
	    }
	}

	public int compare(HashSet<String> arg0, HashSet<String> arg1) {
		int sizeComp = - Integer.compare(arg0.size(), arg1.size());
		if (sizeComp == 0) sizeComp = arg0.toString().compareTo(arg1.toString());
		return sizeComp;
	}
	
}

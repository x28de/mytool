package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.io.Reader;
import java.io.StringReader;
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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

public class TaggedImport implements TreeSelectionListener, ActionListener, Comparator<HashSet<String>> {
	
	// Standard mytool fields
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	GraphPanelControler controler;
	int j = 0;
	int maxVert = 10;
	String dataString = "";
	int edgesNum = 0;
	
	// For main part
	HashSet<String> subset = null;
	HashSet<HashSet<String>> subsets = new HashSet<HashSet<String>>();
	Hashtable<String,HashSet<String>> item2subset = new Hashtable<String,HashSet<String>>(); 
	TreeSet<HashSet<String>> subsetsBySize = new TreeSet<HashSet<String>>(this);
	
	HashSet<String> itemGroup = null;
	Hashtable<HashSet<String>,HashSet<String>> subset2itemGroup = 
			new Hashtable<HashSet<String>,HashSet<String>>(); 
	
	Hashtable<HashSet<String>,GraphNode> subset2node = new Hashtable<HashSet<String>,GraphNode>();
	Hashtable<String,GraphNode> label2node = new Hashtable<String,GraphNode>();
	HashSet<GraphNode> singles = new HashSet<GraphNode>();
	Hashtable<String,String> catsLong = new Hashtable<String,String>();
	String[] records;
	String contentString = "";
	boolean fuse = false;
	
	// For preparation parts
	Hashtable<Integer,String> superCatNames = new Hashtable<Integer,String>();
	Hashtable<Integer,Integer> superCatLevels = new Hashtable<Integer,Integer>();
	Hashtable<Integer,HashSet<String>> catSets = new Hashtable<Integer,HashSet<String>>();
	int superCatNum = -1;
	boolean started = false;
	String htmlOut = "";
	boolean itemSwitch = false;		// must be fields because of Parser Callback use
	boolean catSwitch = false;
	boolean superCatSwitch = false;
	boolean nameSwitch = false;
	boolean separatorSwitch = false;
	String itemString = "";
	String catString = "";
	String superCatString = "";
	String catNumber = "";
	int count = 0;
	String previousItem = "";
	String previousCat = "";
	String catNames = "";
	
	// For the UI
	JTree tree;
    DefaultMutableTreeNode top = null;
    DefaultTreeModel model = null;
    DefaultMutableTreeNode[] superiors = new DefaultMutableTreeNode[4]; 
    Hashtable<Integer,Boolean> selected = new Hashtable<Integer,Boolean>();
    HashSet<String> wanted = new HashSet<String>();
	String[] partsOfSpeech = {"N.", "V.", "Adj.", "Adv.", "Phr."};
    JFrame frame;
    JPanel innerFrame;
	JFileChooser chooser = new JFileChooser();
	JDialog dialog;
    JCheckBox[] posBoxes;
	JCheckBox fuseBox;
	
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
				node = addNode(label, detail, colorStrings[type]);
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

	
	//	Special constructor for optional preparation from Roget files (import type 'Roget')
	
	public TaggedImport(GraphPanelControler controler) {
		this.controler = controler;
	
		// Preparation 1
		String contentString = askForFile("body", controler);
		filterHTML1(contentString);	// also fills tables that are used in the options dialog
		askForSelections();			// -> preparation2()
	}
	
	public void preparation2() {
		contentString = askForFile("index", controler);
		contentString = contentString.replaceAll("&nbsp;", "");
		
	    // show progress
		dialog = new JDialog(controler.getMainWindow(), "Processing the list...");
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setMinimumSize(new Dimension(338, 226));
		dialog.setLocation(dim.width/2-dialog.getSize().width/2 - 169, 
				dim.height/2-dialog.getSize().height/2 - 113);		
		dialog.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel info = new JLabel("Processing the list...");
		dialog.add(info);
		dialog.setVisible(true);
		
		// takes time
		contentString = filterHTML2(contentString) + "\n" + catNames;
		dialog.dispose();
		// Save a copy
		FileWriter list;
		try {
			list = new FileWriter(System.getProperty("user.home") + 
					File.separator + "Desktop" + File.separator + "x28list.txt");
			list.write(contentString);
			list.close();
		} catch (IOException e) {
			System.out.println("Error TGI12 " + e);			
		}

		mainPart(contentString);
	}

	//
	//	General constructor for import type 'Tagged'
	
	public TaggedImport(File file, GraphPanelControler controler) {
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
		
		mainPart(contentString);
	}
	
	
	public void mainPart(String contentString) {
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
			if (cat.startsWith(item) && !cat.equals(item)) {
				GraphNode node = addNode(item, cat, "#ff0000");	// Long category detail texts 
				label2node.put(item, node);
				continue;
			}
			
			
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
			itemGroup = combiUnit.getItemGroup();
			if (!itemGroup.contains(item)) {
				itemGroup.add(item);
			}
		}
		
		askForSimplify();
	}
	
	public void mainPart2() {
		
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

			if (!fuse) {
				
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
		controler.toggleHashes(true);

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
    	controler.setTreeModel(null);
    	controler.setNonTreeEdges(null);
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
		edgesNum++;
		GraphEdge edge = new GraphEdge(edgesNum, node1, node2, Color.decode("#d8d8d8"), "");
		edges.put(edgesNum,  edge);
	}

	//
	// For preparation part 1: processing Roget's body file which contains:
	//
	// <center>supercat</center>
	// <b>#catnum</b> <b>catname</b> 
	// <b>catnum catpos</b> item ... item 
	// <b>catnum catpos</b> item ... item 

	private void filterHTML1(String html) {
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.CENTER) {
					superCatSwitch = true;
					catSwitch = false;
				} else if (t == HTML.Tag.B) {
					catSwitch = true;
				}
			}
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				if (dataString.contains("CLASS")) started = true;
				if (catSwitch) catString += dataString;
				if (superCatSwitch && started) superCatString += dataString;
			}
			public void handleEndTag(HTML.Tag t, int pos) {

				// Super categories
				if (t == HTML.Tag.CENTER) {
					superCatSwitch = false;
					if (!started) return;

					//	Record the hierarchy
					superCatNum++;
					superCatNames.put(superCatNum, superCatString);
					if (superCatString.startsWith("CLASS")) {
						superCatLevels.put(superCatNum, 1);
					} else if (superCatString.startsWith("DIVISION")) {
						superCatLevels.put(superCatNum, 2);
					} else if (superCatString.startsWith("SECTION")) {
						superCatLevels.put(superCatNum, 3);
					} else {
						superCatLevels.put(superCatNum, 4);
					}

					superCatString = "";

					//	Categories
				} else if (t == HTML.Tag.B) {
					catSwitch = false;
					if (!started) return;
					count++;
					if (nameSwitch) {
						catNumber = catNumber.substring(1, catNumber.length() -1);
						catsLong.put(catNumber, catNumber + " " + catString);
						nameSwitch = false;
					}
					if (catString.startsWith("#")) {
						nameSwitch = true;
						catNumber = catString;
						HashSet<String> catSet;
						if (!catSets.containsKey(superCatNum)) {
							catSet = new HashSet<String>();
							catSets.put(superCatNum, catSet);
						} 
						catSet = catSets.get(superCatNum);
						catSet.add(catString);
					}
					catString = "";
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error TGI128 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error TGI129 " + e3.toString());
		}
	}

	//
	// For preparation step 2: process Roget's index file which contains:
	//
	//	<b>item</b> <i>cat</i> ... <i>cat</i>
	
	private String filterHTML2(String html) {
		htmlOut = "";
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.B) {
					itemSwitch = true;
				} else if (t == HTML.Tag.I) {
					catSwitch = true;
				} else if (t == HTML.Tag.CENTER) {
					separatorSwitch = true;
				}
			}
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				dataString = dataString.replace("\r", "");
				if (separatorSwitch) return;
				if (itemSwitch) itemString += dataString;
				if (catSwitch) catString += dataString;
			}
			public void handleEndTag(HTML.Tag t, int pos) {
				
				//	Items
				if (t == HTML.Tag.B) {
					itemSwitch = false;
					itemString = itemString.replaceAll(":$", "");
					count++;
					previousItem = itemString;
					
				//	Categories
				} else if (t == HTML.Tag.I) {
					catSwitch = false;
					
					String catNum = catString.replaceAll("(.*)( [0-9]+[ab]? )(.*)", "$2");
													// e.g.: "politics 737b N."
					catNum = catNum.trim();
					catString = catString.replaceAll("(.*)( [0-9]+[ab]? )(.*)", "$2$3");
					catString = catString.replaceAll("^-", "");
					catString = catString.replaceAll(" ", "");
					if (!wanted.contains("#" + catString)) {
						catString = "";
						itemString = "";
						return;
					}
					
					count++;
					if (count % 90 == 0)
						dialog.setTitle("Loaded " + count + " items ...");
					
					if (itemString.isEmpty()) {
						htmlOut += "\n" + previousItem;
					} else {
						htmlOut += "\n" + itemString;
					}
					htmlOut += "\t" +catNum;
					catString = "";
					itemString = "";
					
				} else if (t == HTML.Tag.CENTER) {
					separatorSwitch = false;
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error TGI128b " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error TGI129b " + e3.toString());
		}
		return htmlOut;
	}
    
	private static class MyHTMLEditorKit extends HTMLEditorKit {
    	private static final long serialVersionUID = 7279700400657879527L;

    	public Parser getParser() {
    		return super.getParser();
    	}
    }
    
//
//	UI accessories
    
	public String askForFile(String which, GraphPanelControler controler) {
		File file = null;
		String baseDir = "";
		try {
			baseDir = System.getProperty("user.home") + File.separator + "Desktop";
		} catch (Throwable e) {
			System.out.println("Error TGI105" + e );
		}
		File b = new File(baseDir);
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(b);
		chooser.setDialogTitle("Open the downloaded http://www.gutenberg.org/files/10681/10681-h-" + which + "-pos.htm");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(".htm (the downloaded " + which + " file)", "htm");
        chooser.setFileFilter(filter); 
        JFrame mainWindow = controler.getMainWindow();
	    int returnVal = chooser.showOpenDialog(mainWindow);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	 file = chooser.getSelectedFile();
	    }
		FileInputStream fileInputStream = null;
		Utilities utilities = new Utilities();
		String contentString = "";
		try {
			fileInputStream = new FileInputStream(file);
			InputStream in = (InputStream) fileInputStream;
				contentString = utilities.convertStreamToString(in);
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error TGI101b " + e);
		} catch (IOException e) {
			System.out.println("Error TGI102b " + e);
		}
		
	    return contentString;
	}
	
	public void askForSelections() {
		frame = new JFrame("Selection");
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2-frame.getSize().width/2 - 298, 
				dim.height/2-frame.getSize().height/2 - 209);	
		innerFrame = new JPanel(new BorderLayout());
		innerFrame.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(new BranchInfo(0, "Select some [sub]sections"));
		superiors[0] = top;
		boolean divisionPresent = false;
		for (int i = 0; i < superCatNames.size(); i++) {
			selected.put(i, false);  // initializing
			int level = superCatLevels.get(i);
			int previousLevel = level - 1;
			if (level == 2) divisionPresent = true;
			if (level == 1 && divisionPresent) divisionPresent = false;
			if (level == 3  && !divisionPresent) previousLevel = previousLevel - 1;
			DefaultMutableTreeNode superior = superiors[previousLevel];
			DefaultMutableTreeNode branch = new DefaultMutableTreeNode(new BranchInfo(i, superCatNames.get(i)));
            superior.add(branch);
            if (level < 4) superiors[level] = branch;
		}
	    model = new DefaultTreeModel(top);
	    tree = new JTree(model);
	    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	    tree.addTreeSelectionListener(this);
	    
		JScrollPane scrollPane = new JScrollPane(tree);
		innerFrame.add(scrollPane, BorderLayout.NORTH);
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new FlowLayout());
		String[] posNames = {"Nouns", "Verbs", "Adjectives", "Adverbs", "Phrases"};
		posBoxes = new JCheckBox[5];
		for (int i = 0; i < 5; i++) {
			posBoxes[i] = new JCheckBox(partsOfSpeech[i]);
			posBoxes[i].setToolTipText(posNames[i]);
			bottom.add(posBoxes[i]);
		}
		JButton nextButton = new JButton("Next >");
		nextButton.addActionListener(this);
		bottom.add(nextButton);
		innerFrame.add(bottom, BorderLayout.SOUTH);
		
		frame.add(innerFrame);
		if (System.getProperty("os.name").startsWith("Windows")) {
			frame.setMinimumSize(new Dimension(596, 417));
		} else {
			frame.setMinimumSize(new Dimension(796, 417));
		}
		frame.setVisible(true);
	}
	
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
	    if (arg0.getActionCommand().equals("Next >")) {
	    	boolean noCatSelected = true;
	    	catNames = "";
	    	for (int i = 0; i < superCatNames.size(); i++) {
	    		if (!selected.get(i)) continue;
	    		noCatSelected = false;
	    		if (!catSets.containsKey(i)) continue;
	    		HashSet<String> catSet = catSets.get(i);
	    		Iterator<String> iter = catSet.iterator();
	    		while (iter.hasNext()) {
	    			String cat = iter.next();
	    			String catNum = cat.substring(1, cat.length() - 1);
	    			String catLong = catsLong.get(catNum);
	    			catNames += catLong + "\t" + catNum + "\n";
	    			boolean noPosSelected = true;
	    			for (int j = 0; j < 5; j++) {
	    				String catPOS = cat.replace(".", "");
	    				if (posBoxes[j].isSelected()) {
	    					catPOS += partsOfSpeech[j];
	    					wanted.add(catPOS);
	    					noPosSelected = false;
	    				}
	    			}
	    			if (noPosSelected) {
	    				controler.displayPopup("No part of speech selected, try again.");
	    				return;
	    			}
	    		}
	    	}
	    	if (noCatSelected) {
	    		controler.displayPopup("No sections selected, try again.");
	    		frame.dispose();
	    		return;
	    	}
	    	catsLong.clear();
	    	frame.dispose();
	    	preparation2();
	    	
	    } else if (arg0.getActionCommand().equals("fuse")) {
	    	fuse = fuseBox.isSelected();
	    } else if (arg0.getActionCommand().equals("simplify")) {
	    	dialog.dispose();
	    	mainPart2();
	    }
	}

	public void valueChanged(TreeSelectionEvent e) {
		TreePath[] paths = e.getPaths();

		for (int i = 0; i < paths.length; i++) {
			TreePath selectedPath = paths[i];
			Object o = selectedPath.getLastPathComponent();
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) o;
			toggleSelection(selectedNode, e.isAddedPath(i));
			
			// TODO subsections?
		}
	}
	public void toggleSelection(DefaultMutableTreeNode selectedNode, boolean fluct) {
		BranchInfo branch = (BranchInfo) selectedNode.getUserObject();
		int keyOfSel = branch.getKey();
		if (selected.containsKey(keyOfSel)) {
			boolean currentSetting = selected.get(keyOfSel);
			selected.put(keyOfSel, !currentSetting);
			Enumeration<DefaultMutableTreeNode> children = selectedNode.children();
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
				toggleSelection(child, fluct);		// recursion
			}
		} else {
			System.out.println("Error TGI120 " + keyOfSel);
			frame.dispose();
		}
	}

	public int compare(HashSet<String> arg0, HashSet<String> arg1) {
		int sizeComp = - Integer.compare(arg0.size(), arg1.size());
		if (sizeComp == 0) sizeComp = arg0.toString().compareTo(arg1.toString());
		return sizeComp;
	}
	
}

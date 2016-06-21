package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ImappingImport implements TreeSelectionListener, ActionListener {
	
	GraphPanelControler controler;
	String dataString = "";
	
	Hashtable<String,String> rdfItems = new Hashtable<String,String>();
	Hashtable<String,String> parents = new Hashtable<String,String>();
	Hashtable<String,String> bodies = new Hashtable<String,String>();
	Hashtable<String,String> contents = new Hashtable<String,String>();

	Hashtable<String,String> arrows = new Hashtable<String,String>();
	Hashtable<String,String> arrowheads = new Hashtable<String,String>();
	Hashtable<String,String> arrowtails = new Hashtable<String,String>();
	Hashtable<String,String> arrowkeys = new Hashtable<String,String>();
	Hashtable<String,String> arrownames = new Hashtable<String,String>();

	Hashtable<String,Integer> childrenCounts = new Hashtable<String,Integer>();
	Hashtable<String,Boolean> selected = new Hashtable<String,Boolean>();
	TreeMap<Integer,String> orderMap = new TreeMap<Integer,String>();
	SortedMap<Integer,String> orderList = (SortedMap<Integer,String>) orderMap;
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,Integer> uri2num = new Hashtable<String,Integer>();
	Hashtable<String,String> fusedContent = new Hashtable<String,String>();
	
	private static final String RDF_FILENAME = "layout.rdf.nt";
	private static final String CDS_FILENAME = "content.cds.xml";

	private static final String HAS_BODY = 
			"<http://ont.semanticdesktop.org/ontologies/2007/imapping#hasBody>";
	private static final String REPR_CDS = 
			"<http://ont.semanticdesktop.org/ontologies/2007/imapping#representsCdsItem>";
	private static final String HAS_PARENT = 
			"<http://ont.semanticdesktop.org/ontologies/2007/imapping#hasParent>";
	private static final String CDS_ROOT = 
			"<http://www.semanticdesktop.org/ontologies/2007/09/01/cds#rootItem>";
	private static final String REPR_STMT = 
			"<http://ont.semanticdesktop.org/ontologies/2007/imapping#representsCdsStatement>";
	private static final String LINKS_FROM = 
			"<http://ont.semanticdesktop.org/ontologies/2007/imapping#linksFrom>";
	private static final String LINKS_TO = 
			"<http://ont.semanticdesktop.org/ontologies/2007/imapping#linksTo>";

	private ZipFile zipFile;
	private InputStream rdfInputStream;
	private InputStream cdsInputStream;
	
	String rdfRoot = "<urn:imapping/root>";
	String rootBody = "";
	int topicnum = 0;
	int assocnum = 0;
	String htmlOut = "";

	JTree tree;
	boolean noSelectionMade = true;
	int order;
	JFrame frame;

	int x;
	int y;
	int maxVert = 10;
	int j = 0;
	int edgesNum = 0;
	
	boolean success = false;

//
//	Accessories for UI
	
	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			noSelectionMade = true;
			processChildren();
			controler.getNSInstance().setInput(dataString, 2);
		}
	};
	
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand().equals("Cancel")) {
			noSelectionMade = true;
		}
		frame.dispose();
		processChildren();
		if (!success) failed();
		controler.getNSInstance().setInput(dataString, 2);
	}

	public ImappingImport(JFrame mainWindow, GraphPanelControler controler) {
		
		this.controler = controler;

//
//		Read inputs
		
		File file = new File("C:\\Users\\Matthias\\Desktop\\introduction.iMap");
		FileDialog fd = new FileDialog(mainWindow);
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		file = new File(filename);
		
		new ImappingImport(file, controler);
	}
	
	public ImappingImport(File file, GraphPanelControler controler) {
		this.controler = controler;
		try {
			zipFile = new ZipFile(file);
			final ZipEntry rdfEntry = zipFile.getEntry(RDF_FILENAME);
			final ZipEntry cdsEntry = zipFile.getEntry(CDS_FILENAME);
			if (rdfEntry == null) System.out.println("Error IM101");
			if (cdsEntry == null) System.out.println("Error IM101");
			rdfInputStream = zipFile.getInputStream(rdfEntry);
			cdsInputStream = zipFile.getInputStream(cdsEntry);
		} catch (final Exception e) {
			System.out.println("Error IM102 " + e);
		}
		
//
//		RDF -- for graphical hierarchy
		String triplesString = convertStreamToString(rdfInputStream);		
		String [] triples = triplesString.split("\\r?\\n");
//		String [] triples = triplesString.split("\\r");

		for (int i = 0; i < triples.length; i++) {
			String line = triples[i];
			String triple[] = line.split("\\s");
			if (triple.length < 3) {
				failed();
				return;
			}
			String predicate = triple[1];
				
			if (predicate.equals(HAS_BODY)) {
				rdfItems.put(triple[0], triple[2]);
		
			} else if (predicate.equals(REPR_CDS)) {
				String object = triple[2];
				if (object.equals(CDS_ROOT)) {
					rootBody = triple[0];
				}
				int objLen = object.length();
				object = object.substring(1, objLen-1);
				bodies.put(triple[0], object);
				
			} else if (predicate.equals(HAS_PARENT)) {
				parents.put(triple[0], triple[2]);

				int childrenCount = 1;
				if (childrenCounts.containsKey(triple[2])) {
					childrenCount = childrenCounts.get(triple[2]);
					childrenCount++;
				}
				childrenCounts.put(triple[2], childrenCount);
				
			} else if (predicate.equals(REPR_STMT)) {
				String object = triple[2];
				int objLen = object.length();
				object = object.substring(1, objLen-1);
				arrows.put(triple[0], object);
				
			} else if (predicate.equals(LINKS_TO)) {
				arrowheads.put(triple[0], triple[2]);
				
			} else if (predicate.equals(LINKS_FROM)) {
				arrowtails.put(triple[0], triple[2]);
				
			}
		}
		
//
//		CDS -- for Texts and Arrow Names
		Document doc = null;
		doc = getParsedDocument(cdsInputStream);
		try {
			zipFile.close();
		} catch (IOException e) {
			System.out.println("Error IM103 " + e);
		}
		
		Element xmlRoot = null;
		
		if (doc.hasChildNodes()) {
			xmlRoot = doc.getDocumentElement();
			if (xmlRoot.getTagName() != "org.semanticdesktop.swecr.model.memory.xml.XModel") {
				System.out.println("Error IM104" );
				return;
			} else {
				NodeList topicsContainer = xmlRoot.getElementsByTagName("contentItems");
				NodeList specialContainer = xmlRoot.getElementsByTagName("nameItems");
				
				NodeList topics = ((Element) topicsContainer.item(0)).getElementsByTagName("contentitem");
				NodeList specials = ((Element) specialContainer.item(0)).getElementsByTagName("nameitem");

				for (int i = 0; i < topics.getLength(); i++) {
					importTopic((Element) topics.item(i));
				}
				for (int i = 0; i < specials.getLength(); i++) {
					importSpecial((Element) specials.item(i));
				}
			}
			
			
			//	Search for RDF root
			Enumeration<String> itemsEnum = rdfItems.keys();
			while (itemsEnum.hasMoreElements()) {
				String itemKey = itemsEnum.nextElement();
				String body = rdfItems.get(itemKey);
				if (body == null) continue;
				if (body.equals(rootBody)) {
					rdfRoot = itemKey;
					System.out.println("Root item found: " + itemKey);
				}
			}

			//	Initialize selection to false 
			Enumeration<String> allItems = rdfItems.keys();
			while (allItems.hasMoreElements()) {
				selected.put(allItems.nextElement(), false);
			}
			
			//	Show tree
		    DefaultMutableTreeNode top = 
		    		new DefaultMutableTreeNode(new BranchInfo(rdfRoot, "All"));
		    createSelectionNodes(top);
		    
		    DefaultTreeModel model = new DefaultTreeModel(top);
		    tree = new JTree(model);
		    
		    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		    tree.addTreeSelectionListener(this);
		    order = 0;
		    
	        frame = new JFrame("Pick a collection?");
	        frame.setLocation(100, 170);
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);	// closing triggers further processing
			frame.addWindowListener(myWindowAdapter);
			frame.setLayout(new BorderLayout());
	        frame.add(new JScrollPane(tree));

	        JPanel toolbar = new JPanel();
	        toolbar.setLayout(new BorderLayout());
	        JButton continueButton = new JButton("Continue");
	        continueButton.addActionListener(this);
	        continueButton.setSelected(true);
			JButton cancelButton = new JButton("Cancel");
		    cancelButton.addActionListener(this);
		    
		    String okLocation = "East";
		    String cancelLocation = "West";
		    String multSel = "CTRL";
			if (System.getProperty("os.name").equals("Mac OS X")) {
		    	okLocation = "East";
		    	cancelLocation = "West";
		    	multSel = "CMD";
		    }
			JLabel instruction = new JLabel("<html><body>" +
		    "You may restrict your import. " +
			"Do do so, select one or more branches. <br />" + 
		    "Specify multiple selections as usual by holding " + multSel + 
		    " or Shift while clicking. <br />&nbsp;<br />");
	        toolbar.add(continueButton, okLocation);
			toolbar.add(cancelButton, cancelLocation);
			
			toolbar.add(instruction, "North");
			toolbar.setBorder(new EmptyBorder(10, 10, 10, 10));
	        frame.add(toolbar,"South");
	        frame.pack();
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
//	        frame.setMinimumSize(new Dimension(400, 300));
	        frame.setMinimumSize(new Dimension(596, 418));

	        frame.setVisible(true);

//	        
//			Read in the cross links
	        
			if (xmlRoot.getTagName() != "org.semanticdesktop.swecr.model.memory.xml.XModel") {
				System.out.println("Error IM104" );
				return;
			} else {
				NodeList assocsContainer = xmlRoot.getElementsByTagName("statements");
				NodeList relationsContainer = xmlRoot.getElementsByTagName("relations");
				
				NodeList assocs = ((Element) assocsContainer.item(0)).getElementsByTagName("statement");
				NodeList rels = ((Element) relationsContainer.item(0)).getElementsByTagName("relation");

				for (int i = 0; i < assocs.getLength(); i++) {
					importAssoc((Element) assocs.item(i));
				}
				System.out.println("Relations count " + rels.getLength());
				for (int i = 0; i < rels.getLength(); i++) {
					importRel((Element) rels.item(i));
				}
				System.out.println("IM: " + topicnum + " new topics and " + assocnum + " new assocs read");
				
			}
			
		}

	}

//
//	Major work 
//	Is started when user closes the selection tree
	
	public void processChildren() {
		
//
//		Determine which single children should be fused with their parent
		
		// Loop through children
		Enumeration<String> childrenEnum = parents.keys();
		while (childrenEnum.hasMoreElements()) {
			String childItemKey = childrenEnum.nextElement();
//			if (!selected.get(childItemKey) && !noSelectionMade) continue;
			String childContent = fetchItem(childItemKey);
			String parentItemKey = parents.get(childItemKey);
			String parentContent = fetchItem(parentItemKey);
			
			boolean fuse = true;
			
			if (!childrenCounts.containsKey(parentItemKey)) {
				System.out.println("Error IM115 " + parentContent);
				continue;
			} else if (childrenCounts.get(parentItemKey) > 1) {		// is not a single child
				fuse = false;
			}
			
			if (childrenCounts.containsKey(childItemKey)) {		// is not a leaf entry
				fuse = false;
			}
			
			if (parentContent.length() > 30) {		// detail field is occupied by parent'd long text
				fuse = false;
			}
			
			if (childContent.length() <= 30) {		// probably, detail fields are not desired
				fuse = false;
			}
			
//			fuse = false;	//	disable fusing 
		
			if (fuse) {
				fusedContent.put(parentItemKey, childContent); 
			} else {
				if (fusedContent.containsKey(childItemKey)) {
					continue;	// Content is already there"
				} else {
					fusedContent.put(childItemKey, childContent); 
				}
			}
		}
		
//
//		Generate new map nodes
//		Copied from SplitIntoNew; TODO reuse
		
		// Add the Root node (was not enumerated in parents)
		if (selected.get(rdfRoot) || noSelectionMade) {
			Point rootPoint = new Point(40 + (j/maxVert) * 150, 40 + (j % maxVert) * 50 + (j/maxVert)*5);
			GraphNode rootNode = new GraphNode (j + 100, rootPoint, Color.decode("#ccdddd"), "ROOT", "(Root's detail here");
			nodes.put(j + 100, rootNode);
			uri2num.put(rdfRoot, j + 100);
			j++;
		}
		

		SortedSet<Integer> orderSet = (SortedSet<Integer>) orderList.keySet();
		Iterator<Integer> ixit = orderSet.iterator(); 

		while (ixit.hasNext()) {
			int nextnum = ixit.next();
			String nodeRef = orderMap.get(nextnum);
			if (!selected.get(nodeRef) && !noSelectionMade) continue;
			if (!fusedContent.containsKey(nodeRef)) continue;

			String label = fetchItem(nodeRef);
			String detail = fusedContent.get(nodeRef);
			
			label = filterHTML(label);
			if (label.length() > 25) label = label.substring(0, 25) + " ..."; 
			
//			String newNodeColor = controler.getNewNodeColor();	// TODO 
			String newNodeColor = "#ccdddd";
			String newLine = "\r";
			String topicName = label;
			String verbal = detail;
			if (topicName.equals(newLine)) topicName = "";
			if (verbal == null || verbal.equals(newLine)) verbal = "";
			if (topicName.isEmpty() && verbal.isEmpty()) continue;
			int id = 100 + j;
			
			y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			x = 40 + (j/maxVert) * 150;
			if (childrenCounts.containsKey(nodeRef)) {
				if (childrenCounts.get(nodeRef) > 3) x = x + 30;
			}
			Point p = new Point(x, y);
			GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

			nodes.put(id, topic);
			uri2num.put(nodeRef, id);
			j++;
		}
		
//
//		Generate edges
		
		Enumeration<String> childrenEnum2 = fusedContent.keys();
		while (childrenEnum2.hasMoreElements()) {
			String sourceNodeUri = childrenEnum2.nextElement();
			if (!uri2num.containsKey(sourceNodeUri)) {
				System.out.println("skipping child (not included in selection): " + sourceNodeUri);
				continue;
			}
			else {
				int sourceNodeNum = uri2num.get(sourceNodeUri);
				if (!parents.containsKey(sourceNodeUri)) {
					System.out.println("Error IM114 " + sourceNodeUri);
					continue;
				}
				String targetNodeUri = parents.get(sourceNodeUri);
				int targetNodeNum = 0;
				if (!uri2num.containsKey(targetNodeUri)) {
					System.out.println("skipping parent (not included in selection) " + targetNodeUri + " ( <- " + sourceNodeUri + ")");
					continue;
				}
				else {
					edgesNum++;
					targetNodeNum = uri2num.get(targetNodeUri);
					GraphNode sourceNode = nodes.get(sourceNodeNum);
					GraphNode targetNode = nodes.get(targetNodeNum);
					GraphEdge edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode("#c0c0c0"), "hasParent");
					edges.put(edgesNum,  edge);
				}
			}
		}
		
		//	Arrows other than hasParent
		Enumeration<String> arrowsEnum = arrows.keys();
		while (arrowsEnum.hasMoreElements()) {
			String arrowKey = arrowsEnum.nextElement();
			String arrowName = fetchArrow(arrowKey);
			
			String arrowTail = arrowtails.get(arrowKey);
			String tailName = fetchItem(arrowTail);
			String sourceNodeUri = arrowTail;

			String arrowHead = arrowheads.get(arrowKey);
			String headName = fetchItem(arrowHead);
			String targetNodeUri = arrowHead;

			if (!fusedContent.containsKey(sourceNodeUri)) {
				if (!parents.containsKey(sourceNodeUri)) {
					System.out.println("Error IM131 " + tailName);
					continue;
				} else {
					sourceNodeUri = parents.get(sourceNodeUri);
				}
			}
			if (!uri2num.containsKey(sourceNodeUri)) {
				if (selected.get(targetNodeUri)) {
					createRelatedNode(sourceNodeUri);
				} else continue;
			}
			int sourceNodeNum = uri2num.get(sourceNodeUri);
			
			if (!fusedContent.containsKey(targetNodeUri)) {
				if (!parents.containsKey(targetNodeUri)) {
					System.out.println("Error IM132 " + headName);
					continue;
				} else {
					targetNodeUri = parents.get(targetNodeUri);
				}
			}
			if (!uri2num.containsKey(targetNodeUri)) {
				if (selected.get(sourceNodeUri)) {
					createRelatedNode(targetNodeUri);
				} else continue;
			}
			int targetNodeNum = uri2num.get(targetNodeUri);
			
			edgesNum++;
			GraphNode sourceNode = nodes.get(sourceNodeNum);
			GraphNode targetNode = nodes.get(targetNodeNum);
			GraphEdge edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode("#ffff00"), arrowName);
			edges.put(edgesNum,  edge);
//			System.out.println(tailName + " " + arrowKey + " " + arrowName + " " + headName);
		}
//
//		Pass on nodes and edges 
		
		try {
			dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error IM105 " + e1);
		} catch (IOException e1) {
			System.out.println("Error IM106 " + e1);
		} catch (SAXException e1) {
			System.out.println("Error IM107 " + e1);
		}
		success = true;
	}
	
//
//	Repeated substantial subroutines
	
	public void importTopic(Element element) {
		topicnum++;
		String content = element.getElementsByTagName("content").item(0).getTextContent();
		String key = element.getElementsByTagName("uri").item(0).getTextContent();
		contents.put(key, content);
	}

	public void importSpecial(Element element) {
		topicnum++;
		String content = element.getElementsByTagName("name").item(0).getTextContent();
		String key = element.getElementsByTagName("uri").item(0).getTextContent();
//		System.out.println("special, content = " + content + ", uri = " + key);
		contents.put(key, content);
	}

	public void importAssoc(Element element) {
		assocnum++;
		String key = element.getElementsByTagName("uri").item(0).getTextContent();
//		String subject = element.getElementsByTagName("s").item(0).getTextContent();
		String predicate = element.getElementsByTagName("p").item(0).getTextContent();
//		String object = element.getElementsByTagName("o").item(0).getTextContent();
		if (predicate.equals("http://www.semanticdesktop.org/ontologies/2007/09/01/cds#hasDetail")) {
			return;
		} else if (predicate.equals("http://www.semanticdesktop.org/ontologies/2007/09/01/cds#hasSubtype")) {
			return;
		} else if (predicate.equals("http://www.semanticdesktop.org/ontologies/2007/09/01/cds#isSubtypeOf")) {
			return;
			// TODO maybe fetch author and skip if this was system
		} else {
//			System.out.println("IM predicate: " + predicate);
			arrowkeys.put(key, predicate);
		}
	}

	public void importRel(Element element) {
		assocnum++;
		String key = element.getElementsByTagName("uri").item(0).getTextContent();
		String name = element.getElementsByTagName("name").item(0).getTextContent();
//		System.out.println("IM relation " + key + " " + name);
		arrownames.put(key, name);
	}
	
	
	public String fetchItem(String itemKey) {
			String bodyKey = rdfItems.get(itemKey);
			if (bodyKey == null) {
				System.out.println("Error IM111 " + itemKey);
				return "";
			}
			String cdsKey = bodies.get(bodyKey);
			if (cdsKey == null) {
				System.out.println("Error IM112 " + itemKey + "\n\t-> " + bodyKey);
				return "";
			}
			String cdsContent = contents.get(cdsKey);
			if (cdsContent == null) {
				System.out.println("Error IM113 " + itemKey + "\n\t-> " + bodyKey + "\n\t\t- > " + cdsKey);
				return "";
			}
			return cdsContent;
	}
	
	public void failed() {
		controler.displayPopup("Import failed.");		
	}
	
	public String fetchArrow(String arrowKey) {
		String cdsKey = arrows.get(arrowKey);
		if (cdsKey == null) {
			System.out.println("Error IM121 " + arrowKey);
			return "";
		}
		String pKey = arrowkeys.get(cdsKey);
		if (pKey == null) {
			System.out.println("Error IM122 " + arrowKey + "\n\t-> " + cdsKey);
			return "";
		}
		String arrowname = arrownames.get(pKey);
		if (arrowname == null) {
			System.out.println("Error IM123 " + arrowKey + "\n\t-> " + cdsKey + "\n\t\t- > " + pKey);
			return "";
		}
		return arrowname;
	}
	
//
//	Accessories for branch selection
    
    private void createSelectionNodes(DefaultMutableTreeNode top) {
        DefaultMutableTreeNode branch = null;
        BranchInfo categoryInfo = (BranchInfo) top.getUserObject();
        String parentKey = categoryInfo.getKey();
        String childKey = "";
        String branchLabel = "";
        
        Enumeration<String> children = parents.keys();
        while (children.hasMoreElements()) {
        	childKey = children.nextElement();
        	String testParent = parents.get(childKey);
        	if (testParent.equals(parentKey)) {
        		branchLabel = fetchItem(childKey);
    			branchLabel = filterHTML(branchLabel);
    			if (branchLabel.length() > 25) branchLabel = branchLabel.substring(0, 25) + " ..."; 
                branch = new DefaultMutableTreeNode(new BranchInfo(childKey, branchLabel));
                top.add(branch);
        		order++;
        		orderMap.put(order, childKey);
        		
                createSelectionNodes(branch);	// recursive
        	}
        }
    }
    
    private class BranchInfo {
        public String branchKey;
        public String branchLabel;
 
        public BranchInfo(String branchKey, String branchLabel) {
        	this.branchKey = branchKey;
        	this.branchLabel = branchLabel;
            
            if (branchLabel == null) {
                System.err.println("Error IM140 Couldn't find info for " + branchKey);
            }
        }
 
        public String getKey() {
            return branchKey;
        }
        
        public String toString() {
            return branchLabel;
        }
    }

	public void valueChanged(TreeSelectionEvent arg0) {
		TreePath[] paths = arg0.getPaths();

		System.out.println("\n");
		for (int i = 0; i < paths.length; i++) {
			TreePath selectedPath = paths[i];
			Object o = selectedPath.getLastPathComponent();
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) o;
			toggleSelection(selectedNode, arg0.isAddedPath(i));
		}
	}

	public void toggleSelection(DefaultMutableTreeNode selectedNode, boolean fluct) {
		noSelectionMade = false;
		String fluctText = "removed";
		if (fluct) fluctText = "added";
		BranchInfo branch = (BranchInfo) selectedNode.getUserObject();
		String keyOfSel = branch.getKey();
//		System.out.println(keyOfSel + " (" + branch + ") " + fluctText);
		if (selected.containsKey(keyOfSel)) {
			boolean currentSetting = selected.get(keyOfSel);
			selected.put(keyOfSel, !currentSetting);
		} else {
			System.out.println("Error IM120 " + keyOfSel);
			if (!success) failed();
			frame.dispose();
		}

		@SuppressWarnings("rawtypes")
		Enumeration children =  selectedNode.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
			toggleSelection(child, fluct);
		}
	}
	
	public void createRelatedNode(String nodeRef) {
		String label = fetchItem(nodeRef);
		String detail = fusedContent.get(nodeRef);
		
		label = filterHTML(label);
		if (label.length() > 25) label = label.substring(0, 25) + " ..."; 
		
//		String newNodeColor = controler.getNewNodeColor();	// TODO 
		String newNodeColor = "#eeeeee";
		String newLine = "\r";
		String topicName = label;
		String verbal = detail;
		if (topicName.equals(newLine)) topicName = "";
		if (verbal == null || verbal.equals(newLine)) verbal = "";
		int id = 100 + j;
		
		y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		x = 40 + (j/maxVert) * 150;
		if (childrenCounts.containsKey(nodeRef)) {
			if (childrenCounts.get(nodeRef) > 3) x = x + 30;
		}
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		uri2num.put(nodeRef, id);
		j++;

		if (nodeRef.equals(rdfRoot)) return;
		String parentNodeUri = parents.get(nodeRef); 
		int targetNodeNum;
		if (!uri2num.containsKey(parentNodeUri)) {
			createRelatedNode(parentNodeUri);
		}
		targetNodeNum = uri2num.get(parentNodeUri);
		edgesNum++;
		GraphNode sourceNode = nodes.get(id);
		GraphNode targetNode = nodes.get(targetNodeNum);
		GraphEdge edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode("#e0e0e0"), "hasParent");
		edges.put(edgesNum,  edge);
	}
	
//
//		Accessory for XML 
// 		Duplicate of NewStuff TODO reuse
	
	private Document getParsedDocument(InputStream stream) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = null; 
		try  {
			parser = dbf.newDocumentBuilder(); 
			return parser.parse(stream);
		} catch (Exception e) {
			System.out.println("Error IM108 (getParsedDocument from stream): " + e);
			return parser.newDocument();
		}
	}
	
//
//		Accessories to eliminate HTML tags from label
//		Duplicate of NewStuff TODO reuse

	private String filterHTML(String html) {
		htmlOut = "";
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				htmlOut = htmlOut + dataString + " ";
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error IM109 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error IM110 " + e3.toString());
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
// 	Accessory 
//	Duplicate of NewStuff TODO reuse

	private String convertStreamToString(InputStream is, Charset charset) {
    	
        //
        // From http://kodejava.org/how-do-i-convert-inputstream-to-string/
        // ("To convert the InputStream to String we use the
        // Reader.read(char[] buffer) method. We iterate until the
        // Reader return -1 which means there's no more data to
        // read. We use the StringWriter class to produce the string.")
    	
    	if (is != null) {
    		Writer writer = new StringWriter();
    		char[] buffer = new char[1024];
    		Reader reader = null;;
    		
   			reader = new BufferedReader(
   					new InputStreamReader(is, charset));	

    		int n;
    		try {
    			while ((n = reader.read(buffer)) != -1) {
    				writer.write(buffer, 0, n);
    			}
    		} catch (IOException e) {
    			System.out.println("Error IM117 " + e);
    			try {
    				writer.close();
    			} catch (IOException e1) {
    				System.out.println("Error IM118 " + e1);
    			}
    		} finally {
    			try {
    				is.close();
    			} catch (IOException e) {
    				System.out.println("Error IM119 " + e);
    			}
    		}
    		String convertedString = writer.toString();
    		return convertedString;
    	} else {        
    		return "";
    	}
    }
    private String convertStreamToString(InputStream is) {
    	return convertStreamToString(is, Charset.forName("UTF-8"));
    }

}

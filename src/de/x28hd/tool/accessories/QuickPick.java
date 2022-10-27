package de.x28hd.tool.accessories;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class QuickPick implements TreeSelectionListener, ActionListener {
	
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	PresentationService controler;
	
	TreeMap<Integer,String> inputMap = new TreeMap<Integer,String>();
	Hashtable<Integer,String> ids2name = new Hashtable<Integer,String>();
	
	TreeSet<String> namesList = new TreeSet<String>();
	SortedSet<String> namesSet = (SortedSet<String>) namesList;

	TreeSet<String> letterList = new TreeSet<String>();
	SortedSet<String> letterSet = (SortedSet<String>) letterList;

	Hashtable<String,DefaultMutableTreeNode> tops = new Hashtable<String,DefaultMutableTreeNode>();
	Hashtable<String,DefaultMutableTreeNode> authorNodes = new Hashtable<String,DefaultMutableTreeNode>();

	public static String[][] extraNames = {
			{"Leucippus & Democritus", "Leucippus & Democritus"},
			{"Stoics (Zeno of Citium et al)", "Stoics"},
			{"Timon of Phlius", "Timon"},
			{"Zeno of Elea", "Zeno"}
	};
	
	boolean source = true;
	JCheckBox sourceButton = null;
	boolean reject = false;
	JCheckBox rejectButton = null;
	JFrame treeFrame = null;
	int sourceID;
	String sourceLabel = "";
	String outList = "target\trej\tsource\tcomment\r\n";
	String htmlOut = "";
	int paragraphCount = 0;
	
	public QuickPick(Hashtable<Integer,GraphNode> n, Hashtable<Integer,GraphEdge> e, 
			PresentationService c) {
		nodes = n;
		edges = e;
		controler = c;
		
		// Scan nodes for prefix 
		
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			String label = node.getLabel();
			String [] fields = label.split(": ");
			String prefix = label;
			if (fields.length > 0) prefix = fields[0];
			
			String shortStmt = node.getDetail();
			shortStmt = filterHTML(shortStmt);
			int id = node.getID();
			inputMap.put(id, shortStmt);
			ids2name.put(id, prefix);
			namesList.add(prefix);
		}
		
		// Author navigation	// TODO rename author to prefix
		Iterator<String> authors = namesList.iterator();
		while (authors.hasNext()) {
			String author = authors.next();
			String firstLetter = author.substring(0, 1).toUpperCase();
			if (!tops.containsKey(firstLetter)) {
				DefaultMutableTreeNode top = new DefaultMutableTreeNode(firstLetter);
				tops.put(firstLetter, top);
				letterList.add(firstLetter);
			}
			DefaultMutableTreeNode top = tops.get(firstLetter);
			
			if (!authorNodes.containsKey(author)) {
				DefaultMutableTreeNode authorNode = new DefaultMutableTreeNode(author);
				authorNodes.put(author, authorNode);
				top.add(authorNode);
			}
		}
		
		// Items (in getID() order)
		SortedSet<Integer> inputSet = (SortedSet<Integer>) inputMap.keySet();
		Iterator<Integer> iter = inputSet.iterator();
		
		while(iter.hasNext()) {
			int id = iter.next();
			String label = inputMap.get(id);
			if (label.length() > 70) label = label.substring(0, 70);
			String author = ids2name.get(id);
			DefaultMutableTreeNode authorNode = authorNodes.get(author);
			DefaultMutableTreeNode item = new DefaultMutableTreeNode(new BranchInfo(id, label));
			authorNode.add(item);
		}

		// UI 
		JFrame frame = new JFrame("First letter");
		frame.setMinimumSize(new Dimension(500, 400));
		frame.setLayout(new BorderLayout());
		JPanel alphabet = new JPanel();
		Iterator<String> letterIter = letterSet.iterator();
		while (letterIter.hasNext()) {
			String letter = letterIter.next();
			JButton letterButton = new JButton(letter);
			letterButton.setMinimumSize(new Dimension(10, 10));
			letterButton.addActionListener(this);
			alphabet.add(letterButton);
		}
		frame.add(alphabet);
		
		// Switches
		JPanel bottom = new JPanel();	// TODO rename
		sourceButton = new JCheckBox("New Source?");
		sourceButton.setSelected(source);
		sourceButton.addActionListener(this);
		bottom.add(sourceButton);
		rejectButton = new JCheckBox("Rejecting?");
		rejectButton.setSelected(false);
		bottom.add(rejectButton);
		frame.add(bottom, BorderLayout.NORTH);
		
		frame.setVisible(true);
	}
	
	public String lastName(String fullName) {
		for (int i = 0; i < extraNames.length; i++) {
			if (extraNames[i][0].equals(fullName)) {
				return extraNames[i][1];
			}
		}
		String namePart[] = fullName.split(" ");
		return namePart[namePart.length - 1];
	}

	public void valueChanged(TreeSelectionEvent e) {
		TreePath[] paths = e.getPaths();

		for (int i = 0; i < paths.length; i++) {
			// get ID
			TreePath selectedPath = paths[i];
			Object o = selectedPath.getLastPathComponent();
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) o;
			if (!e.isAddedPath(i)) continue;
			BranchInfo info = (BranchInfo) selectedNode.getUserObject();
			int nodeID = info.getKey();
			
			// prepare output
			String detail = inputMap.get(nodeID);
			String shortStmt = detail;
			if (shortStmt.length() > 20) shortStmt = shortStmt.substring(0, 20);
			String author = ids2name.get(nodeID);
			String label = lastName(author) + ": " + shortStmt;
			String rejectMarker = reject ? "n" : "";
			String colorString = reject ? "#ff0000" : "#bbffbb";
			if (source) {
				sourceID = nodeID;
				sourceLabel = label;
				sourceButton.setSelected(false);
				source = false;
			} else {
				outList += nodeID + "\t" + rejectMarker + "\t" + sourceID + 
						"\t(" + label + " <- " + sourceLabel + ")\r\n";
				sourceButton.setSelected(true);
				source = true;
				treeFrame.dispose();
				GraphNode sourceNode = nodes.get(sourceID);
				GraphNode targetNode = nodes.get(nodeID);
				int edgeNum = edges.size();
				while (edges.containsKey(edgeNum)) edgeNum++;
				GraphEdge edge = new GraphEdge(edgeNum, sourceNode, targetNode, Color.decode(colorString), "");
				edges.put(edgeNum,  edge);
				sourceNode.addEdge(edge);
				targetNode.addEdge(edge);
			}
			out();
			rejectButton.setSelected(false);
			reject = false;
		}
	}
	
	public void out() {
		FileWriter list;
		try {
			list = new FileWriter(System.getProperty("user.home") + 
					File.separator + "Desktop" + File.separator + "x28log.txt");
			list.write(outList);
			list.close();
		} catch (IOException e) {
			System.out.println("Error RG102 " + e);			
		}
		
	}

	private String filterHTML(String html) {
		htmlOut = "";
		paragraphCount = 0;
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleText(char[] data, int pos) {
				if (paragraphCount > 1) return;
				String dataString = new String(data);
				htmlOut = htmlOut + dataString;
			}
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t.equals(HTML.Tag.BR)) {
					paragraphCount++;
					return;
				}
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t.equals(HTML.Tag.P)) {
					paragraphCount++;
					return;
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error AJ109 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error AJ110 " + e3.toString());
		}
		return htmlOut;
	}
	public void actionPerformed(ActionEvent arg0) {
		String command = arg0.getActionCommand();
		
		if (command.equals("New Source?")) {
			source = sourceButton.isSelected();
			return;
		}
		reject = rejectButton.isSelected();
		
		DefaultTreeModel model = new DefaultTreeModel(tops.get(command));
	    JTree tree = new JTree(model);
	    tree.addTreeSelectionListener(this);
	    
	    String windowTitle = source ? "Source" : "Target";
		treeFrame = new JFrame(windowTitle);
		treeFrame.setMinimumSize(new Dimension(500, 400));
		if (source) {
			treeFrame.setLocation(510, 0);
		} else {
			treeFrame.setLocation(510, 200);
		}
		
	    JPanel innerPanel = new JPanel();
	    innerPanel.setBackground(Color.WHITE);
	    innerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
	    innerPanel.add(tree);
	    treeFrame.add(innerPanel);
	    treeFrame.setVisible(true);
	}
}

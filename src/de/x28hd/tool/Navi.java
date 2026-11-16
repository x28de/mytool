package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import de.x28hd.tool.accessories.BranchInfo;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.layouts.TreeMapLayout.MyBranchInfo;

public class Navi implements TreeSelectionListener {
	Hashtable<Integer, GraphNode> nodes; 
	Hashtable<Integer, GraphEdge> edges;	
	PresentationService controler;
	GraphPanel graphPanel;

	public Navi(Hashtable<Integer, GraphNode> n, Hashtable<Integer, GraphEdge> e, PresentationService c) {
		nodes = n;
		edges = e;
		controler = c;
		graphPanel = c.getGraphPanel();
		
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(new BranchInfo(0, "ROOT"));
		
		Object[] options = new GraphNode[3];
		Enumeration<GraphNode> nodeList = nodes.elements();
		int i = -1;
		while (nodeList.hasMoreElements()) {
			i++;
			GraphNode nextNode = nodeList.nextElement();
			options[i] = nextNode;
			DefaultMutableTreeNode child = new DefaultMutableTreeNode(new BranchInfo(nextNode.getID(), nextNode.getLabel()));
			top.add(child);
			if (i >= 2) break;
		}
//		setSystemUI();
		JFrame treeFrame = new JFrame();
		treeFrame.setLayout(new BorderLayout());
//		renderer = new DefaultTreeCellRenderer();
//		Icon myLeaf = renderer.getOpenIcon();
//		UIManager.put("Tree.leafIcon", myLeaf);		// leaf entries here are folders, too
		JPanel innerPanel = new JPanel();
	    innerPanel.setBackground(Color.WHITE);
	    innerPanel.setBorder(new EmptyBorder(35, 10, 10, 10));
	    innerPanel.setLayout(new BorderLayout());

		DefaultTreeModel model = new DefaultTreeModel(top);
		JTree tree = new JTree(model);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
	    tree.setVisibleRowCount(55);
	    JScrollPane scrollPane = new JScrollPane(tree);
		innerPanel.add(scrollPane, "North");
		JLabel caption = new JLabel("tree");
		innerPanel.add(caption, "South");
		treeFrame.add(innerPanel);
		treeFrame.setMaximumSize(new Dimension(300, 300));
//		c.getMainWindow().add(treeFrame);
//		c.getMainWindow().repaint();
		treeFrame.setVisible(true);
		
//		int response = JOptionPane.showOptionDialog(c.getMainWindow(), "Choose", "Picking Dialog", 
//				JOptionPane.YES_NO_OPTION, 
//				JOptionPane.QUESTION_MESSAGE, null, 
//				options, options[2]);
//		GraphNode selectedNode = (GraphNode) options[response];
//		graphPanel.nodeSelected(selectedNode);

	}

	public void valueChanged(TreeSelectionEvent arg0) {
		TreePath path = arg0.getPath();
		TreePath selectedPath = path;
		Object o = selectedPath.getLastPathComponent();
		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) o;
		BranchInfo info = (BranchInfo) selectedNode.getUserObject();
		int key = info.getKey();
		GraphNode graphNode = nodes.get(key);
//		highLight = r;
		graphPanel.nodeSelected(graphNode);
//		diagramPanel.repaint();
	}
}

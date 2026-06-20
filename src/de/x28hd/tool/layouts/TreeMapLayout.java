package de.x28hd.tool.layouts;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.json.JSONArray;

import de.x28hd.tool.importers.TreeMapCounting;

public class TreeMapLayout implements TreeSelectionListener, MouseListener {
	TreePath selPath = null;

	JFrame mainWindow;
	JComponent diagramPanel;
	Rectangle highLight = null;
	
	DefaultMutableTreeNode countersTop;
	TreeMapCounting caller;
	JPanel treeFrame;
	DefaultTreeModel treeModel;
	JTree tree;
	DefaultTreeCellRenderer renderer;

	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		}
	};
		
	public TreeMapLayout() {
	}

	public void setTop (DefaultMutableTreeNode top) {
		countersTop = top;
		File file = null;
		mainPart(file);
	}
	
	public TreeMapLayout(File file) {
		mainPart(file);
	}
	public void mainPart(File file) {
		
		// Tree diagram
		
		mainWindow = new JFrame("Diagram Window");
		mainWindow.addWindowListener(myWindowAdapter);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int w = dim.width - 80;
		int h = dim.height - 80;
		Container contentPane = mainWindow.getContentPane();
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		splitPane.setOneTouchExpandable(true);
		int x = Math.round((float) (dim.width * .75)) - 40 + 8;
		splitPane.setResizeWeight(.8);
		splitPane.setDividerSize(8);

		contentPane.add(splitPane);

		if (file != null) countersTop = collectCounts(file, "");	// filetree down, treenodes up
		
		// Alternative:
//		GenericTree genericTree = new GenericTree(this);
//		JSONArray dirList = genericTree.jsonStruc(file);	// folder
//		JSONArray dirList = genericTree.dotNotated(file);	// text file
//		countersTop = genericTree.collectJSON(dirList, "");
		// Testing
//		genericTree.fetchCounts(countersTop, "");	// treenodes down for testing; indent for testing
		
		diagramPanel = new JComponent() {
			public void paint(Graphics g) {
				paintRect(g, countersTop, 40, 40, x - 100, h - 120, "");
				if (highLight != null) paintHighLight(g, highLight);
			}
		};
		diagramPanel.addMouseListener(this);

		mainWindow.setSize(new Dimension(w, h));
		mainWindow.setVisible(true);
		splitPane.setLeftComponent(diagramPanel);
		treeFrame = showTree(countersTop);
		splitPane.setRightComponent(treeFrame);
		splitPane.setDividerLocation(x);
		splitPane.repaint();
	}
	
	public class MyBranchInfo {
		int total;
		String label;
		Rectangle rectangle;
		public MyBranchInfo(int t, String l) {
			total = t;
			label = l + "     " + t;
		}
		public int getTotal() {return this.total;}
		public String toString() {
			return this.label;
		}
		public Rectangle getRectangle() {
			return rectangle;
		}
		public void setRectangle(int x, int y, int w, int h) {
			this.rectangle = new Rectangle(x, y, w, h);
		}
	}
	
	
	public DefaultMutableTreeNode collectCounts(File file, String indent) {
		File[] dirList = file.listFiles();
		DefaultMutableTreeNode constructed = null;

		// Buffers for sorting
		TreeMap<Double,DefaultMutableTreeNode> bufferMap = new TreeMap<Double,DefaultMutableTreeNode>(Collections.reverseOrder());
		SortedMap<Double,DefaultMutableTreeNode> bufferList = (SortedMap<Double,DefaultMutableTreeNode>) bufferMap;

		int leafCount = 0;
		int folderCount = 0;
		double disambig = .00001;
		int sum = 0;

		if (dirList == null) {
			System.out.println("Error with Dirlist in " + file.getName());
			return null;
		} 

		// Scan the folder
		for (File f : dirList) {
			if (f.isHidden()) continue;
			if (f.isDirectory()) {
				folderCount++;
				DefaultMutableTreeNode child = collectCounts(f, indent + "  ");
				MyBranchInfo info = (MyBranchInfo) child.getUserObject();
				int total = info.getTotal();
				sum += total;
				
				double uniq = total + 0.;
				while (bufferMap.containsKey(uniq)) uniq += disambig;
				bufferMap.put(uniq, child);
			} else {
				leafCount++;
			}
		}

		// pseudo folder of leaf files
		if (leafCount > 0) {
			if (folderCount > 0) {
				double uniq = leafCount + .0;
				while (bufferMap.containsKey(uniq)) uniq += disambig;
				MyBranchInfo myBranchInfo = new MyBranchInfo(leafCount, file.getName() + " files");
				DefaultMutableTreeNode pseudoFolder = new DefaultMutableTreeNode(myBranchInfo);
				bufferMap.put(uniq, pseudoFolder);
			} else {
				MyBranchInfo myBranchInfo = new MyBranchInfo(leafCount, file.getName());
				DefaultMutableTreeNode pseudoFolder = new DefaultMutableTreeNode(myBranchInfo);
				return pseudoFolder;
			}
		}
		
		// sort by totals
		SortedSet<Double> bufferSet = (SortedSet<Double>) bufferList.keySet();
		Iterator<Double> iter = bufferSet.iterator();

		// add child nodes
		MyBranchInfo constructedInfo = new MyBranchInfo(sum + leafCount, file.getName());
		constructed = new DefaultMutableTreeNode(constructedInfo);
		while (iter.hasNext()) {
			double sortedNode = iter.next();
			DefaultMutableTreeNode treeNode = bufferMap.get(sortedNode);
			constructed.add(treeNode);
		}
		return constructed;
	}
	
//
//	Graphics
	
	public void paintRect(Graphics g, DefaultMutableTreeNode treeNode, int x0, int y0, int w, int h, String indent) {
		if (treeNode.isLeaf()) return;
		
		int len =treeNode.getChildCount();
		int[] rawData = new int[len];
		Enumeration<DefaultMutableTreeNode> children2 = treeNode.children();
		int j = 0;
		while (children2.hasMoreElements()) {
			DefaultMutableTreeNode child = children2.nextElement();
			MyBranchInfo info = (MyBranchInfo) child.getUserObject();
			int raw = info.getTotal();
			rawData[j] = raw;
			j++;
		}
		
		double sum = 0.;	// it's all just integers but used for many divisions
		double halfWay = 0.;
		int wrap = Math.min(pixel(len * .1), len - 1);
		for (int i = 0; i < len; i++) {
			sum += rawData[i];
			if (i == wrap) halfWay = sum;
		}
		double upper = halfWay / sum * 100.;			// percent 
		
		int x;
		int y = 0;
		int width;
		int height = pixel(halfWay / sum * h);
		
		double accuRaw = 0.;							// accumulated
		double ratio = w / (upper * 10.);				// pixels per percent

		int i = 0;
		Enumeration<DefaultMutableTreeNode> children = treeNode.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = children.nextElement();
			MyBranchInfo childInfo = (MyBranchInfo) child.getUserObject();
			x = pixel(accuRaw / sum * 100. * 10. * ratio);
			width = pixel(rawData[i] / sum * 100. * 10. * ratio);
			accuRaw += rawData[i];

			g.drawRect(x + x0, y + y0, width, height);
			paintRect(g, child, x + x0, y + y0, width, height, indent + "  ");				// recursion
			((MyBranchInfo) childInfo).setRectangle(x + x0, y + y0, width, height);
			
			if (i == wrap) {		// switch to lower half
				y += height;
				height = pixel((sum - halfWay)/sum * h);
				ratio = (x + width) / ((100. - upper) * 10.);		// new ratio
				accuRaw = 0.;
			}
			i++;
		}
	}
	
	public void paintHighLight(Graphics g, Rectangle r) {
		((Graphics2D) g).setStroke(new BasicStroke(3));
		g.setColor(Color.RED);
		g.drawRect(r.x, r.y, r.width, r.height);
	}
	
//
//	Accessories
	
	public int pixel(double in) {
		float floatNum = (float) in;
		return Math.round(floatNum);
	}

	public JPanel showTree(DefaultMutableTreeNode top) {
		setSystemUI();
		treeFrame = new JPanel();
		treeFrame.setLayout(new BorderLayout());
		renderer = new DefaultTreeCellRenderer();
		Icon myLeaf = renderer.getOpenIcon();
		UIManager.put("Tree.leafIcon", myLeaf);		// leaf entries here are folders, too
		JPanel innerPanel = new JPanel();
	    innerPanel.setBackground(Color.WHITE);
	    innerPanel.setBorder(new EmptyBorder(35, 10, 10, 10));
	    innerPanel.setLayout(new BorderLayout());

		DefaultTreeModel model = new DefaultTreeModel(top);
		tree = new JTree(model);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
	    tree.setVisibleRowCount(55);
	    JScrollPane scrollPane = new JScrollPane(tree);
		innerPanel.add(scrollPane, "North");
		treeFrame.add(innerPanel);
		return treeFrame;
	}
	
	public void selectNode(Point xy) {
		Enumeration<TreeNode> checkList = countersTop.breadthFirstEnumeration();
		while (checkList.hasMoreElements()) {
			DefaultMutableTreeNode tNode = (DefaultMutableTreeNode) checkList.nextElement();
			if (tNode.equals(countersTop)) continue;
			MyBranchInfo info = (MyBranchInfo) tNode.getUserObject();
			Rectangle folder = info.getRectangle();
			if (!folder.contains(xy)) continue;
			TreeNode[] treeNode = tNode.getPath();
			TreePath treePath = new TreePath(treeNode);
			tree.scrollPathToVisible(treePath);
			tree.addSelectionPath(treePath);
			treeFrame.requestFocus();
		}
	}
	
	public void setSystemUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e2) {
			System.out.println("Error PS104 " + e2);
		} catch (ClassNotFoundException e2) {
			System.out.println("Error PS105" + e2);
		} catch (InstantiationException e2) {
			System.out.println("Error PS106 " + e2);
		} catch (IllegalAccessException e2) {
			System.out.println("Error PS107 " + e2);
		}  
	}
	
	public void valueChanged(TreeSelectionEvent arg0) {

		TreePath path = arg0.getPath();
		TreePath selectedPath = path;
		Object o = selectedPath.getLastPathComponent();
		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) o;
		MyBranchInfo info = (MyBranchInfo) selectedNode.getUserObject();
		Rectangle r = info.getRectangle();
		highLight = r;
		diagramPanel.repaint();
	}

	public void mouseClicked(MouseEvent arg0) {
		selectNode(new Point(arg0.getX(), arg0.getY()));
	}
	public void mouseEntered(MouseEvent arg0) {
	}
	public void mouseExited(MouseEvent arg0) {
	}
	public void mousePressed(MouseEvent arg0) {
	}
	public void mouseReleased(MouseEvent arg0) {
	}
}

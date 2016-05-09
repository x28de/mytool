package de.x28hd.tool;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JMenuItem;
import javax.swing.TransferHandler;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

class GraphPanel extends JDesktopPane  {

	private GraphPanelControler controler;
	JComponent graphPanel;
	Hashtable<Integer, GraphNode> nodes;
	Hashtable<Integer, GraphEdge> edges;
    JMenuItem menuItem = new JMenuItem("Paste new Input");
//    Point insertion = new Point(300, 300);
	private Vector currentBunch;
	private int currentBunchIndex;
	private Selection selection;
	boolean labelUpdate = false;
	Font font = new Font("monospace", Font.PLAIN, 12);
	NewStuff newStuff = null;
	
	//	Drag accessories
	int bundleDelay = 0;
	boolean bundleInProgress = false;
	String myTransferable = "";

	
	// directly from jri
	private boolean dragInProgress;			// true if dragged before released
	private boolean translateInProgress;	// \
	private boolean moveInProgress;			// | max one is set
	private boolean edgeInProgress;			// |
	private boolean clusterInProgress;		// /
	//
	private Hashtable cluster;				// cluster of associated nodes
	private GraphNode targetNode;			// used while edgeInProgress
	private int ex, ey;						// used while edgeInProgress
	private int mX, mY;						// last mouse position
	private Point translation;
	//
	Rectangle bounds;
	private int width, height;
	private Image topImage;
	private Image bottomImage;
	private Image leftImage;
	private Image rightImage;
	final static int BORDER_IMAGE_WIDTH = 84;  
	final static int BORDER_IMAGE_HEIGHT = 12;
	boolean x28PresoSizedMode = false;
	boolean borderOrientation = false;
	boolean antiAliasing = true;
    
//
//  Accessories for drop ( same as in ComposeWindow)
	    
    private TransferHandler handler = new TransferHandler() {
		private static final long serialVersionUID = 1L;

		public boolean canImport(TransferHandler.TransferSupport support) {
			return newStuff.canImport(support, "GP");
		}

		public boolean importData(TransferHandler.TransferSupport support) {
			controler.beginLongTask();
			return newStuff.importData(support, "GP");
	    	 
		}
	};
	
//
//  Accessories for drag
	
	public class MyTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 1L;
		
		public Transferable createTransferable(JComponent c) {
			System.out.println("GP: Transferable created ");
			return new StringSelection(myTransferable);
		}
	    public int getSourceActions(JComponent c) {
	        return TransferHandler.COPY;
	    }
		protected void exportDone(JComponent c, Transferable data, int action) {
			bundleInProgress = false;
			clusterInProgress = false;
			System.out.println("GP: Drag & Drop Done " + action);
		}
	}


	private static final long serialVersionUID = 1L;

	GraphPanel(final GraphPanelControler controler) {
		this.controler = controler;
		this.translation = new Point(0, 0);
		newStuff = controler.getNSInstance();
		setLayout(null);
		selection = new Selection();
		
		width = this.getWidth() + 1;
		height = this.getHeight() + 1;
		bounds = new Rectangle(height/2, width/2, 0, 0);
		topImage = getImage("up.gif");
		bottomImage = getImage("down.gif");
		leftImage = getImage("left.gif");
		rightImage = getImage("right.gif");
		
		graphPanel = new JComponent() {

//
//			Main graphics activity

			public void paint(Graphics g) { 
				if (borderOrientation) paintBorderImages(g);
				if (antiAliasing) {
					((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				}
				g.translate(translation.x, translation.y);
				paintEdges(g);
				paintNodes(g);
			}
			private void paintNodes(Graphics g) {
				Enumeration<GraphNode> e = nodes.elements();
				while (e.hasMoreElements()) {
					try {
						GraphNode node = e.nextElement();
						paintNode(node, g);
					} catch (Throwable t) {
						System.out.println("GP: Error paintnodes " + t);	
					}
				}
			}
			private void paintEdges(Graphics g) {
				Enumeration<GraphEdge> e = edges.elements();
				while (e.hasMoreElements()) {
					GraphEdge edge = e.nextElement();
					paintEdge(edge, g);
				}
				if (edgeInProgress) {
					Point p = selection.topic.getXY();
					g.setColor(Color.gray.darker());
					paintLine(g, p.x, p.y, ex - translation.x, ey - translation.y, true);
				}
			} 
			private static final long serialVersionUID = 1L;
			private void paintBorderImages(Graphics g) {
				if (showTopImage()) {
					g.drawImage(topImage, (width - BORDER_IMAGE_WIDTH) / 2, 0, this);
				}
				if (showBottomImage()) {
					g.drawImage(bottomImage, (width - BORDER_IMAGE_WIDTH) / 2, height -
						BORDER_IMAGE_HEIGHT, this);
				}
				if (showLeftImage()) {
					g.drawImage(leftImage, 0, (height - BORDER_IMAGE_WIDTH) / 2, this);
				}
				if (showRightImage()) {
					g.drawImage(rightImage, width - BORDER_IMAGE_HEIGHT, (height -
						BORDER_IMAGE_WIDTH) / 2, this);
				}
			}
		};

//
//		Accessories for Mouse (press, release, drag) and window resizing

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				thisPanelPressed(e);
			}
			public void mouseReleased(MouseEvent e) {
				thisPanelReleased(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if (isSpecial(e)) {
				}
				thisPanelDragged(e);
			}
		});

		addComponentListener(new ComponentAdapter() {       // this was crucial
			public void componentResized(ComponentEvent e) {
				Dimension s = getSize();
				graphPanel.setSize(s);
				// update border images
				width = s.width;
				height = s.height;
				repaint();
			}
		});

		setToolTipText("");		//	turns getToolTipText on
		graphPanel.setTransferHandler(handler);
		add(graphPanel);
	}

//
//	Paint methods

	void setModel(Hashtable<Integer, GraphNode> nodes, Hashtable<Integer, GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}

	private void paintNode(GraphNode node, Graphics g) {
		g.setColor(node.getColor());
		Point p = node.getXY();
		int iconWidth = 18;
		int iconHeight = 18;
		if (x28PresoSizedMode) {
			iconWidth = 28;
			iconHeight = 28;
		}
//		g.fillOval(p.x - 9, p.y - 9, 18, 18);
		g.fillOval(p.x - iconWidth/2, p.y - iconHeight/2, iconWidth, iconHeight);

		if (node == selection.topic && selection.mode == Selection.SELECTED_TOPIC) {
			g.setColor(Color.red);
//			g.drawRect(p.x - 11, p.y - 11, 21, 21);
//			g.drawRect(p.x - 12, p.y - 12, 23, 23);
			g.drawRect(p.x - iconWidth/2 - 2, p.y - iconWidth/2 - 2, iconWidth + 3, iconHeight + 3);
			g.drawRect(p.x - iconWidth/2 - 3, p.y - iconWidth/2 - 3, iconWidth + 5, iconHeight + 5);
		}
		
		if (node.getLabel() != null) {
			g.setColor(Color.black);
			Font x28PresoSized = new Font(font.getName(), font.getStyle(), 30);
			if (!x28PresoSizedMode) {
				g.setFont(font); 
				g.drawString(node.getLabel(), p.x - 9, p.y + 9 + font.getSize() + 2);
			} else {
				g.setFont(x28PresoSized);
				//  TODO why is font-size in windows 10 so strange?
				g.drawString(node.getLabel(), p.x - 14, p.y + 28 + font.getSize() + 2);
			}
		}
	}

	private void paintEdge(GraphEdge edge, Graphics g) {
		Point from = nodes.get(edge.getN1()).getXY();
		Point to = nodes.get(edge.getN2()).getXY();

		g.setColor(edge.getColor());

		paintLine(g, from.x, from.y, to.x, to.y, true);
		if (!edge.getDetail().isEmpty()) {
			g.fillOval(from.x -2 + (to.x - from.x)/2, 
					from.y - 2 + (to.y - from.y)/2, 5, 5);
		}
		if (edge == selection.assoc && selection.mode == Selection.SELECTED_ASSOCIATION) {
			g.setColor(Color.red);
			g.drawLine(from.x - 2, from.y - 2, to.x - 2, to.y - 2);
			g.drawLine(from.x + 3, from.y - 2, to.x + 3, to.y - 2);
			g.drawLine(from.x - 2, from.y + 3, to.x - 2, to.y + 3);
			g.drawLine(from.x + 2, from.y + 3, to.x + 2, to.y + 3);
		}
	}

	// Directly copied from J�rg Richter's DeepaMehta 2	(like much of this class)
	public static void paintLine(Graphics g, int x1, int y1, int x2, int y2,
			boolean hasDirection) {
		if (hasDirection) {
			g.drawLine(x1, y1 - 1, x2, y2);
			g.drawLine(x1 + 1, y1 - 1, x2, y2);
			g.drawLine(x1 + 2, y1, x2, y2);
			g.drawLine(x1 + 2, y1 + 1, x2, y2);
			g.drawLine(x1 + 1, y1 + 2, x2, y2);
			g.drawLine(x1, y1 + 2, x2, y2);
			g.drawLine(x1 - 1, y1 + 1, x2, y2);
			g.drawLine(x1 - 1, y1, x2, y2);
		} else {
			g.drawLine(x1, y1, x2, y2);
			g.drawLine(x1 + 1, y1, x2 + 1, y2);
			g.drawLine(x1, y1 + 1, x2, y2 + 1);
			g.drawLine(x1 + 1, y1 + 1, x2 + 1, y2 + 1);
		}
	}
	
	private boolean showTopImage() {
		return !isEmpty() && bounds.y + translation.y < 0;
	}

	private boolean showBottomImage() {
		return !isEmpty() && bounds.y + bounds.height + translation.y > height;
	}

	private boolean showLeftImage() {
		return !isEmpty() && bounds.x + translation.x < 0;
	}

	private boolean showRightImage() {
		return !isEmpty() && bounds.x + bounds.width + translation.x > width;
	}
	
	private boolean isEmpty() {
		return nodes.size() == 0;
	}
	
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
	}
	
	public Image getImage(String imagefile) {
		URL imgURL = getClass().getResource(imagefile);
		ImageIcon ii;
		Image img = null;
		if (imgURL == null) {
			controler.displayPopup("Image " + imagefile + " not loaded");
		} else {
			ii = new ImageIcon(imgURL);
			img = ii.getImage();
		}
		return img;
	}


//
//	Find methods and classes   
//	TODO decide if to reactivate the click on label possibility 
	
	private Vector findAllNodes(int x, int y) {
		x -= translation.x;
		y -= translation.y;
		//
		Vector foundNodes = new Vector();
		Enumeration e = nodes.elements();
		while (e.hasMoreElements()) {
			GraphNode node = (GraphNode) e.nextElement();
			Point p = node.getXY();
			int iconWidth = 18;     
			int iconHeight = 18;
			int iw2 = iconWidth / 2;
			int ih2 = iconHeight / 2;
//			iw2 = 18;
//			ih2 = 18;
			// check if inside icon
//			if (Math.abs(x - p.x) <= iw2 && Math.abs(y - p.y) <= ih2) {
			if (Math.abs(x - p.x) <= iw2 && Math.abs(y - p.y - 4) <= 15) {
				foundNodes.addElement(node);
			}
		}
		return foundNodes;
	}

	private boolean edgeHit(Point p1, Point p2, Point hit) {
		// original implementation
		/*
		int x = hit.x;
		int y = hit.y;
		xMin = Math.min(p1.x, p2.x) - 2;
		xMax = Math.max(p1.x, p2.x) + 2;
		yMin = Math.min(p1.y, p2.y) - 2;
		yMax = Math.max(p1.y, p2.y) + 2;
		return (x >= xMin && x <= xMax && y >= yMin && y <= yMax);
		*/
		// a, b, and c should be considered as geometric vectors with components x, y
		Point a = new Point(hit.x - p1.x, hit.y - p1.y);  // vector from p1 to hit
		Point b = new Point(p2.x  - p1.x, p2.y  - p1.y);  // vector from p1 to p2
		float ab_scalarproduct = (float) a.x * b.x + a.y * b.y;  // scalar product of a and b
		float b_length_square = (float) b.x * b.x + b.y * b.y;  // 
		if (b_length_square == 0) {
			return false;
		}		
		float x = ab_scalarproduct / b_length_square;
		Point c = new Point((int) (x * b.x) - a.x, (int) (x * b.y) - a.y);
		int c_length_square = c.x * c.x + c.y * c.y;		
		return 0 < x && x < 1 && c_length_square <= 25;
	}

	//	TODO try to eliminate
	private GraphNode findNode(int x, int y, boolean selectNextInBunch) {
		Vector bunch = findAllNodes(x, y);
		//
		if (bunch.equals(currentBunch)) {
			if (selectNextInBunch && currentBunch.size() > 0) {
				currentBunchIndex = (currentBunchIndex + 1) % currentBunch.size();
				// ### System.out.println(">>> same bunch (" + currentBunch.size() + " nodes) ==> new index is " + currentBunchIndex);
			}
		} else {
			currentBunch = bunch;
			currentBunchIndex = 0;
			// ### System.out.println(">>> new bunch (" + bunch.size() + " nodes) ==> begin at 0");
		}
		//
		return currentBunch.size() > 0 ? (GraphNode) currentBunch.elementAt(currentBunchIndex) : null;
	}

	private GraphEdge findEdge(int x, int y) {
		x -= translation.x;
		y -= translation.y;
		Enumeration e = edges.elements();
		GraphEdge edge = null;
		GraphNode node1, node2;
		Point p1, p2;
		// --- phase 1: collect edge candidates based on bounding rectangle ---
		Vector candidates = new Vector();
		while (e.hasMoreElements()) {
			edge = (GraphEdge) e.nextElement();
			node1 = edge.getNode1();
			node2 = edge.getNode2();
			if (node1 == null || node2 == null) {
				// Note: this is an error condition and has been already reported
				continue;
			}
			p1 = node1.getXY();
			p2 = node2.getXY();
			if (edgeHit(p1, p2, new Point(x,y))) {
				candidates.addElement(edge);
			}
		}
		int candCount = candidates.size();
		if (candCount == 0) {
			return null;
		}
		if (candCount == 1) {
			// ### this is a workaround to enable selection of horizontal resp. vertical
			// edges
			return (GraphEdge) candidates.firstElement();
		}
		// --- phase 2: determine the nearest edge ---
		e = candidates.elements();
		float dist;
		float minDist = 100;
		GraphEdge nearestEdge = null;
		while (e.hasMoreElements()) {
			edge = (GraphEdge) e.nextElement();
			p1 = edge.getNode1().getXY();
			p2 = edge.getNode2().getXY();
			dist = Math.abs(Math.abs(((x - p1.x) / (float) (y - p1.y)) /
									 ((p2.x - p1.x) / (float) (p2.y - p1.y))) - 1);
			if (dist < minDist) {
				minDist = dist;
				nearestEdge = edge;
			}
		}
		return minDist < 0.3 ? nearestEdge : null;
	}
	
	public String getToolTipText(MouseEvent evt) {
		int x = evt.getX();
		int y = evt.getY();
//		Vector foundNodes = findAllNodes(x, y);
//		int count = foundNodes.size();
//		if (count == 0) {
			if (findEdge(x,y) != null) {
				GraphEdge hoveredEdge = findEdge(x,y);
				GraphNode topic1 = hoveredEdge.getNode1();	
				GraphNode topic2 = hoveredEdge.getNode2();
					String topicName1 = topic1.getLabel();
					String topicName2 = topic2.getLabel();
				return topicName1 + " -- " + topicName2;
			}
			return null;
	}
	
	// Find clusters
	private Hashtable<Integer,GraphNode> createNodeCluster(GraphNode node) {
		Hashtable<Integer,GraphNode> cluster = new Hashtable<Integer,GraphNode>();
		if (node == null) {
			return cluster;		// already reported
		}
		// start recursion
		nodeCluster(node, cluster);
		return cluster;
	}
	private void nodeCluster(GraphNode node, Hashtable<Integer,GraphNode> cluster) {

		if (node == null) {
			return;		// already reported
		}

		int id = node.getID();
		if (cluster.get(id) == null) {
			cluster.put(id, node);
			Enumeration<GraphEdge> e = node.getEdges();
			GraphEdge edge;
			while (e.hasMoreElements()) {
				edge = (GraphEdge) e.nextElement();
				nodeCluster(node.relatedNode(edge), cluster);
			}
		}
	}

//
//		Clicking

		public void thisPanelPressed(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			mX = x;
			mY = y;

			GraphNode foundNode = findNode(x, y, !isSpecial(e));
			GraphNode node = foundNode != null ? foundNode : null;
			GraphEdge edge = foundNode == null ? findEdge(x, y) : null;
			// Note: an edge is only be searched if no node was found resp. the node was CLICKED_ON_NAME
			if (node != null && edge != null) {
				// Note: an edge dominates a CLICKED_ON_NAME node
				edgeClicked(edge, e);
			} else if (edge != null) {		//  swapped (x28)
				edgeClicked(edge, e);
			} else if (node != null) {
				nodeClicked(node, e);
			} else {
				graphClicked(e);
			}
		}
		
		private void thisPanelDragged(MouseEvent e) {

			if (moveInProgress || clusterInProgress || translateInProgress) {
				//	moveInProgess was set in thisPanelClicked ! 
				if (!dragInProgress) {
					controler.beginTranslation();
					dragInProgress = true;
				}
				int x = e.getX();
				int y = e.getY();
				int dx = x - mX;
				int dy = y - mY;
				mX = x;
				mY = y;
				if (moveInProgress) {
					translateNode(selection.topic, dx, dy);
					controler.updateBounds();

				} else if (clusterInProgress) {
					if (isSpecial(e)) {
						bundleForDrag(e);
					}  
					translateCluster(dx, dy);
					controler.updateBounds();

				} else {
					translateGraph(dx, dy);
				}
				repaint();

			} else if (edgeInProgress) {
				
				if (!dragInProgress) {
					controler.beginCreatingEdge();
					dragInProgress = true;
				}
				
				ex = e.getX();
				ey = e.getY();
//				FoundNode foundNode = findNode(ex, ey, false);
				GraphNode foundNode = findNode(ex, ey, false);
//				targetNode = foundNode != null ? foundNode.node : null;
				
				targetNode = foundNode;
				if (targetNode != null) {
					Point p = targetNode.getXY();
					ex = p.x + translation.x;
					ey = p.y + translation.y;
				}
				repaint();
			}
		}
		
		private void thisPanelReleased(MouseEvent e) {
			if (moveInProgress) {
				moveInProgress = false;
				if (dragInProgress) {
					controler.endTask();
					controler.setDirty(true);
					dragInProgress = false;
				}
			} else if (clusterInProgress) {
				clusterInProgress = false;
				if (dragInProgress) {
					controler.endTask();
					controler.setDirty(true);
					dragInProgress = false;
				}
			} else if (translateInProgress) {
				translateInProgress = false;
				if (dragInProgress) {
					controler.endTask();
					dragInProgress = false;
				}
			} else if (edgeInProgress) {
				edgeInProgress = false;
				//
				controler.endTask();
				dragInProgress = false;
				//
				if (targetNode != null && targetNode != selection.topic) {
					GraphNode node1 = selection.topic;
					GraphNode node2 = targetNode;
					controler.createEdge(node1, node2);
				}	
				repaint();		
			}
		}
		
		private void nodeClicked(GraphNode node, MouseEvent e) {
			System.out.println("GP: Node clicked " + node.getLabel());
			nodeSelected(node);	
			int x = e.getX();
			int y = e.getY();
			if (e.getClickCount() == 2) {		// double clicked
			} else if (e.isAltDown()) {			// alt modifier is pressed -- start creating an edge
				edgeInProgress = true;
				targetNode = null;
				ex = x;
				ey = y;
			} else if (isPopupTrigger(e)) {		// right-click -- show node context menu
				controler.displayContextMenu("node", e.getX(), e.getY());
			} else {							// default -- start moving a node
				moveInProgress = true;
			}
		}

		private void edgeClicked(GraphEdge edge, MouseEvent e) {
			System.out.println("GP: Edge clicked ");
			edgeSelected(edge);
			if (isPopupTrigger(e)) {		// right-click -- show edge context menu
				controler.displayContextMenu("edge", e.getX(), e.getY());
			}
				cluster = createNodeCluster(edge.getNode1());	// ### create later
					clusterInProgress = true;
		}

		private void graphClicked(MouseEvent e) {
			System.out.println("GP: Graph clicked ");
			graphSelected();	
			if (isPopupTrigger(e)) {	// right-click -- show graph context menu
				controler.displayContextMenu("graph", e.getX(), e.getY());
			} else {	// default -- start moving the graph
				translateInProgress = true;
			}
		}

//
//		Selection processing

		//	TODO eliminate this tmp thing
		public Selection getSelectionInstance() {
			return selection;
		}
		
		public void nodeSelected(GraphNode node) {
			if (node != selection.topic && !labelUpdate) {
				controler.nodeSelected(node);
				repaint();
				selection.mode = Selection.SELECTED_TOPIC;
				selection.topic = node;
				selection.assoc = null;
			}
		}

		private void edgeSelected(GraphEdge edge) {
			if (edge != selection.assoc) {
				controler.edgeSelected(edge);
				repaint();
				selection.mode = Selection.SELECTED_ASSOCIATION;
				selection.assoc = edge;
				selection.topic = null;
			}
		}
		
		private void graphSelected() {
			if (selection.mode != Selection.SELECTED_TOPICMAP) {
				repaint();
				selection.mode = Selection.SELECTED_TOPICMAP;
				selection.topic = null;
				selection.assoc = null;
				controler.graphSelected();
			}
		}
		
		void labelUpdateToggle(boolean toggle) {
			labelUpdate = toggle;
		}

//
//		Dragging
	
	public void translateGraph(int x, int y) {
		translation.x += x;
		translation.y += y;
	}

	private void translateNode(GraphNode node, int x, int y) {
		node.getXY().translate(x, y);
	}

	private void translateCluster(int x, int y) {
		Enumeration e = cluster.elements();
		while (e.hasMoreElements()) {
			GraphNode node = (GraphNode) e.nextElement();
			translateNode(node, x, y);
		}
	}
	
	Point getTranslation() {
		return translation;
	}

	public void bundleForDrag(MouseEvent e) {
		if (bundleDelay > 10) {
			myTransferable = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<x28map></x28map>";
			try {
				myTransferable = new TopicMapStorer(cluster, edges).createTopicmapString();
			} catch (TransformerConfigurationException e1) {
				System.out.println("Error GP102 " + e);
			} catch (IOException e1) {
				System.out.println("Error GP103 " + e);
			} catch (SAXException e1) {
				System.out.println("Error GP104 " + e);
			}
			bundleInProgress = true;
			MyTransferHandler t = new MyTransferHandler();
			graphPanel.setTransferHandler(t);
			t.exportAsDrag(graphPanel, e, TransferHandler.COPY);  // This uses my above myTransferable
			bundleDelay = 0;
			return;
		} else {
			if (!bundleInProgress) bundleDelay++;
			return;
		}
	}
		
//
//		Right-clicking etc.
		
		private boolean isSpecial(MouseEvent e) {
			return e.getClickCount() == 2 || e.isAltDown() || isPopupTrigger(e);
		}
		private boolean isPopupTrigger(MouseEvent e) {
			if (e.isPopupTrigger()) {
				return true;
			} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
				return true;
			}
			return false;
		}

//
//		Misc
		
		public void togglePreso() {
			x28PresoSizedMode = ! x28PresoSizedMode;
			repaint();
		}

		public void toggleBorders() {
			borderOrientation = !borderOrientation;
			repaint();
		}

		public void toggleAntiAliasing() {
			antiAliasing = !antiAliasing;
			repaint();
		}


}
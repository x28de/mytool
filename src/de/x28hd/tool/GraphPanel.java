package de.x28hd.tool;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

import de.x28hd.tool.core.GraphCore;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.core.Selection;
import de.x28hd.tool.exporters.TopicMapStorer;
import de.x28hd.tool.inputs.NewStuff;

public class GraphPanel extends GraphCore  {

	PresentationService controler;
	JComponent graphPanel;
	GraphExtras graphExtras;
	public NewStuff newStuff = null;
	
	private boolean rectangleInProgress;
	private boolean rectangleGrowing;
	Rectangle rectangle;
	Point rectangleMark;
	HashSet<GraphNode> rectangleSet = new HashSet<GraphNode>();
	
	//	Drag accessories
	int bundleDelay = 0;
	boolean bundleInProgress = false;
	String myTransferable = "";
	boolean simulatedAltDown = false;
	boolean tabletMode = false;

	// directly from jri
	public Hashtable<Integer,GraphNode> cluster;	// cluster of associated nodes
	private boolean clusterInProgress;		// /
	//
	Rectangle bounds;
	boolean x28PresoSizedMode = false;
	boolean indexCards = true;
	boolean antiAliasing = true;
	boolean enableClusterCopy = false;
    
//
//  Accessories for drag and drop 
	    
    private MyTransferHandler handler = new MyTransferHandler();
	
	public class MyTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 1L;
		
	//  For drag/ copy
		public Transferable createTransferable(JComponent c) {
			return new StringSelection(myTransferable);
		}
	    public int getSourceActions(JComponent c) {
	        return TransferHandler.COPY;
	    }
		protected void exportDone(JComponent c, Transferable data, int action) {
			bundleInProgress = false;
			clusterInProgress = false; 
			toggleAlt(false);
		}
		
//  	For drop ( same as in ComposeWindow)
		public boolean canImport(TransferHandler.TransferSupport support) {
			return newStuff.canImport(support, "GP");
		}

		public boolean importData(TransferHandler.TransferSupport support) {
			return newStuff.importData(support, "GP");
		}
	}

	JComponent graphComponent = new JComponent() {

//
//		Main graphics activity

		public void paint(Graphics g) { 
			graphExtras.paintHints(g);	// borders and jumping arrows, if set
			if (antiAliasing) {
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
			}
			g.translate(translation.x, translation.y);
			paintEdges(g);
			paintNodes(g);
		}
		private static final long serialVersionUID = 1L;
	};

	private static final long serialVersionUID = 1L;

	protected GraphPanel(Object caller) {
		super(caller);
		controler = (PresentationService) caller;

		graphExtras = new GraphExtras(this);
		this.translation = new Point(0, 0);
		setLayout(null);
		
		graphPanel = graphComponent;

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				thisPanelDragged(e);
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent arg0) {
				translateGraph(0, -arg0.getUnitsToScroll() * 20);
				repaint();
			}
		});
		setToolTipText("");		//	turns getToolTipText on
		setTransferHandler(handler);
		
		if (System.getProperty("os.name").equals("Linux")) toggleAntiAliasing();	// TODO raus
	}

//
//	Paint methods

	public void paintNodes(Graphics g) {
		super.paintNodes(g);
		if (rectangleInProgress) paintRect(g);
	}
	private void paintRect(Graphics g) {
		if (rectangleGrowing) {
			int x = rectangleMark.x;
			int y = rectangleMark.y;
			Point rectangleDot = new Point(ex, ey);
			int w = rectangleDot.x - x;
			int h = rectangleDot.y - y;
			if (w < 0) {
				x = rectangleDot.x;
				w = 0 - w;
			}
			if (h < 0) {
				y = rectangleDot.y;
				h = 0 - h;
			}
			g.setColor(Color.RED);
			g.drawRect(x, y, w, h);
			rectangle.setBounds(x, y, w, h);
		} else {
			g.setColor(Color.RED);
			g.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
		}
	}

	public void paintNode(GraphNode node, Graphics g) {
		if (indexCards && !x28PresoSizedMode) {
			super.paintNode(node, g);
			return;
		}
		// TODO where to put the sizes?
		g.setColor(node.getColor());
		Point p = node.getXY();
		int iconWidth = 18;
		int iconHeight = 18;
		if (x28PresoSizedMode) {
			iconWidth += 10;
			iconHeight += 10;
		}
		if (indexCards) {
			iconWidth += 3;
			iconHeight -= 1;
			g.fillRect(p.x - iconWidth/2, p.y - iconHeight/2, iconWidth, iconHeight);
		} else {
			g.fillOval(p.x - iconWidth/2, p.y - iconHeight/2, iconWidth, iconHeight);
		}

		if (node == selection.topic && selection.mode == Selection.SELECTED_TOPIC) {
			g.setColor(Color.red);
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

	public boolean isEmpty() {
		return nodes.size() == 0;
	}
	
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
		graphExtras.setBounds(bounds);
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
	public Hashtable<Integer,GraphNode> createNodeCluster(GraphNode node) {
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
	
	public Hashtable<Integer,GraphNode> createNodeRectangle() {
		Hashtable<Integer,GraphNode> cluster = new Hashtable<Integer,GraphNode>();
		Iterator<GraphNode> rectangleNodes = rectangleSet.iterator();
		while (rectangleNodes.hasNext()) {
			GraphNode node = rectangleNodes.next();
			int id = node.getID();
			cluster.put(id, node);
		}
		return cluster;
	}
	void nodeRectangle(boolean on) {
		rectangleSet.clear();
		rectangleInProgress = on;
		controler.toggleRectanglePresent(on);
		if (!on) return;
		Enumeration<GraphNode> nodesEnum = nodes.elements();
		while (nodesEnum.hasMoreElements()) {
			GraphNode node = nodesEnum.nextElement();
			Point xy = node.getXY();
			if (!rectangle.contains(xy)) continue;
			rectangleSet.add(node);
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
		
		public void thisPanelDragged(MouseEvent e) {
			if (dumbCaller) {
				super.thisPanelDragged(e);
				return;
			}
			//	Intercept ALT-Drag on Graph 
			if (selection.mode == Selection.SELECTED_TOPICMAP && (isSpecial(e) || simulatedAltDown)) {
				translateInProgress = false;
				if (!rectangleInProgress) {
					int x = e.getX();
					int y = e.getY();
					rectangleMark = new Point(x - translation.x, y - translation.y);
					rectangleGrowing = true;
					rectangleInProgress = true;
					rectangle = new Rectangle();	// grows in paintRect
				} else if (rectangleGrowing) {
					ex = e.getX() - translation.x;
					ey = e.getY() - translation.y;
					repaint();
				} else if (!rectangleGrowing) {	// new rectangle starting?
					int x = e.getX() - translation.x;
					int y = e.getY() - translation.y;
					if (!rectangle.contains(new Point(x, y))) {
						rectangleInProgress = false;
						nodeRectangle(false);
						repaint();
					}
				}
			} else if (rectangleInProgress && //	drag node/ edge outside rectangle ?
					selection.mode != Selection.SELECTED_TOPICMAP) {
				int x = e.getX() - translation.x;
				int y = e.getY() - translation.y;
				if (!rectangle.contains(new Point(x, y))) {	
					rectangleInProgress = false;
					nodeRectangle(false);
					repaint();
				}
				
			}

			//	Main processing
			if (moveInProgress || clusterInProgress || translateInProgress) {
				//	moveInProgess was set in thisPanelClicked ! 
				if (!dragInProgress) {
					controler.setMouseCursor(Cursor.HAND_CURSOR);
					dragInProgress = true;
				}
				int x = e.getX();
				int y = e.getY();
				int dx = x - mX;
				int dy = y - mY;
				mX = x;
				mY = y;
				if (moveInProgress) {
					if (rectangleInProgress ) {	
						translateRectangle(dx, dy);
					} else {
						translateNode(selection.topic, dx, dy);
					}
					controler.getControlerExtras().updateBounds();

				} else if (clusterInProgress) {
					if (enableClusterCopy && (isSpecial(e) || simulatedAltDown)) {
						bundleForDrag(e);
					}  
					if (rectangleInProgress ) {	//	TODO check if necessary
						translateRectangle(dx, dy);
					} else {
						translateCluster(dx, dy);
					}
					controler.getControlerExtras().updateBounds();

				} else {
					if (rectangleInProgress && 
							rectangle.contains(new Point(e.getX() - translation.x, e.getY() - translation.y))) {
						translateRectangle(dx, dy);
					} else {
						translateGraph(dx, dy);
					}
				}
				repaint();

			} else if (edgeInProgress) {
				
				if (!dragInProgress) {
					controler.setMouseCursor(Cursor.CROSSHAIR_CURSOR);
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
		
		public void thisPanelReleased(MouseEvent e) {
			if (dumbCaller) {
				super.thisPanelReleased(e);
				return;
			}
			if (moveInProgress) {
				moveInProgress = false; 
				int dx = e.getX() - lastPoint.x;
				int dy = e.getY() - lastPoint.y;
				if (dx != 0 && dy != 0) {	// Moved?
					controler.commit(2, selection.topic, null, new Point(dx, dy));
				}
				if (dragInProgress) {
					controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
					controler.setDirty(true);
					dragInProgress = false;
				}
			} else if (clusterInProgress) {
				clusterInProgress = false;
				if (dragInProgress) {
					controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
					controler.setDirty(true);
					dragInProgress = false;
				}
			} else if (translateInProgress) {
				translateInProgress = false;
				if (dragInProgress) {
					controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
					dragInProgress = false;
				} else if (rectangleInProgress) {
					int x = e.getX() - translation.x;
					int y = e.getY() - translation.y;
					if (rectangle.contains(new Point(x, y)) && !rectangleGrowing) {
						rectangleInProgress = false;
						nodeRectangle(false);
						repaint();
					}
				}
			} else if (edgeInProgress) {
				edgeInProgress = false;
				toggleAlt(false);
				//
				controler.setMouseCursor(Cursor.DEFAULT_CURSOR);
				dragInProgress = false;
				//
				if (targetNode != null && targetNode != selection.topic) {
					GraphNode node1 = selection.topic;
					GraphNode node2 = targetNode;
					controler.createEdge(node1, node2);
				}	
				repaint();		
			}
			if (rectangleGrowing) {	
				rectangleInProgress = true;
				nodeRectangle(true);
				rectangleGrowing = false;
				toggleAlt(false);
			}
		}
		
		public void nodeClicked(GraphNode node, MouseEvent e) {
			
			nodeSelected(node);	
			int x = e.getX();
			int y = e.getY();
			if (!dumbCaller) {
				if (e.getClickCount() == 2) {		// double clicked
					toggleAlt(true);
					return;
				} else if (isPopupTrigger(e)) {		// right-click -- show node context menu
					controler.displayContextMenu("node", e.getX(), e.getY());
					return;
				}
			}
			if (e.isAltDown() || SwingUtilities.isMiddleMouseButton(e) || simulatedAltDown) {
				edgeInProgress = true;
				targetNode = null;
				ex = x;
				ey = y;
			} else {							// default -- start moving a node
				moveInProgress = true;
				lastPoint = new Point(x, y);
			}
		}

		public void edgeClicked(GraphEdge edge, MouseEvent e) {
			edgeSelected(edge);
			if (dumbCaller) return;
			if (e.getClickCount() == 2) {		// double clicked
				toggleAlt(true);
			} else if (isPopupTrigger(e)) {		// right-click -- show edge context menu
				controler.displayContextMenu("edge", e.getX(), e.getY());
			}
				cluster = createNodeCluster(edge.getNode1());	// ### create later
					clusterInProgress = true;
		}

		public void graphClicked(MouseEvent e) {
			graphSelected();	
			if (dumbCaller) {
				translateInProgress = true;
				return;
			}
			if (isPopupTrigger(e)) {	// right-click -- show graph context menu
					controler.displayContextMenu("graph", e.getX(), e.getY());
			} else {	// default -- start moving the graph
				translateInProgress = true;
			}
			if (!simulatedAltDown) toggleAlt(false);
		}

		void labelUpdateToggle(boolean toggle) {
			labelUpdate = toggle;
		}
		
//
//		Selection processing

//
//		Dragging
	
	private void translateRectangle(int x, int y) {
		rectangle.translate(x,  y);
		Iterator<GraphNode> rectangleNodes = rectangleSet.iterator();
		
		while (rectangleNodes.hasNext()) {
			GraphNode node = rectangleNodes.next();
			node.getXY().translate(x, y);
		}
	}

	private void translateCluster(int x, int y) {
		Enumeration<GraphNode> e = cluster.elements();
		while (e.hasMoreElements()) {
			GraphNode node = (GraphNode) e.nextElement();
			translateNode(node, x, y);
		}
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
	
	// Variant of drag bundle: copy
		
	public void copyCluster(boolean rectangle, GraphNode topic) {
		Hashtable<Integer,GraphNode> cluster = new Hashtable<Integer,GraphNode>();
		if (rectangle) {
			cluster = createNodeRectangle();
		} else {
			cluster = createNodeCluster(topic);
		}
		myTransferable = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<x28map></x28map>";
		try {
			myTransferable = new TopicMapStorer(cluster, edges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error GP102a " + e1);
		} catch (IOException e1) {
			System.out.println("Error GP103a " + e1);
		} catch (SAXException e1) {
			System.out.println("Error GP104a " + e1);
		}
		MyTransferHandler t = new MyTransferHandler();
		graphPanel.setTransferHandler(t);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		t.exportToClipboard(graphPanel, clipboard, TransferHandler.COPY);  // This uses my above myTransferable
		return;
	}
		
//
//		Misc
		
		public void togglePreso() {
			x28PresoSizedMode = ! x28PresoSizedMode;
			repaint();
		}

		public void toggleCards(boolean onoff) {
			indexCards = onoff;
			repaint();
		}

		//	Toggle the simulatedAltDown state
		//	It is switched on by double-clicking a node or edge,
		//	it is switched off after its purpose is fulfilled 
		//	(edge creation or bundle exporting) or by clicking the canvas, 
		//	and can also be toggled from a "button" in presentation mode
		
		public void toggleAlt(boolean down) {
			simulatedAltDown = down;
			controler.getControlerExtras().toggleAltColor(down);
		}

		public void toggleTablet(boolean toggle) {
			tabletMode = toggle;
		}

		public void toggleBorders() {
			graphExtras.toggleBorders();
		}

		public void toggleClusterCopy() {
			enableClusterCopy = !enableClusterCopy;
			System.out.println("ClusterCopy ? " + enableClusterCopy);
		}

		public void toggleAntiAliasing() {
			antiAliasing = !antiAliasing;
			repaint();
		}
		
		Point getTranslation() {
			return translation;
		}
		
		public void setSize(int size) {
			font = new Font(font.getName(), font.getStyle(), size);
			repaint();
		}

		public void jumpingArrow(boolean clueless) {
			graphExtras.jumpingArrow(clueless);
		}
		
		public GraphExtras getExtras() {
			return graphExtras;
		}

		public void init() {
			if (dumbCaller) return;	// newStuff later
			newStuff = controler.getNSInstance();
		}
		
		public void normalize() {
			Enumeration<GraphNode> todoList = nodes.elements();
			while (todoList.hasMoreElements()) {
				GraphNode node = todoList.nextElement();
				Point p = node.getXY();
				p.translate(40 - bounds.x, 40 - bounds.y);
				node.setXY(p);
			}
			translation = new Point(0, 0);
		}
}
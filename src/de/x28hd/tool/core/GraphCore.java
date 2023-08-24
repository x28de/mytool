package de.x28hd.tool.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.SwingUtilities;

/** The visual map container in the left pane */
public class GraphCore extends JDesktopPane {
	private static final long serialVersionUID = 1L;
	
	private JComponent graphPanel;
	/** The controller class */
	public PresentationCore controlerCore;
	protected boolean dumbCaller;	// to disable things temporarily
	protected boolean labelUpdate = false;
	
	protected Hashtable<Integer, GraphNode> nodes;
	protected Hashtable<Integer, GraphEdge> edges;
	
	protected Font font = new Font("monospace", Font.PLAIN, 12);
	public Selection selection;
	
	// directly from jri
	protected boolean dragInProgress;			// true if dragged before released
	protected boolean translateInProgress;		// \
	protected boolean moveInProgress;			// | max one is set
	protected boolean edgeInProgress;			// |
	
	private Vector<GraphNode> currentBunch;
	private int currentBunchIndex;
	
	/** used while edgeInProgress */
	public GraphNode targetNode;					// used while edgeInProgress

	/** used while edgeInProgress */
	protected int ex, ey;						// used while edgeInProgress (& rectangleGrowing)
	/** last mouse position */
	protected int mX, mY;						// last mouse position
	/** panned distances x and y */
	protected Point translation;
	protected Point lastPoint = new Point(0, 0);
	
	private JComponent graphComponent = new JComponent() {
		public void paint(Graphics g) { 
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
			g.translate(translation.x, translation.y);
			paintEdges(g);
			paintNodes(g);
		}
		private static final long serialVersionUID = 1L;
	};
	
	protected GraphCore(Object caller) {
		dumbCaller = (caller.getClass() == PresentationCore.class);
		controlerCore = (PresentationCore) caller;
		
		graphPanel = graphComponent;
		add(graphPanel);
		
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
				thisPanelDragged(e);
			}
		});
		addComponentListener(new ComponentAdapter() {       // this was crucial
			public void componentResized(ComponentEvent e) {
				Dimension s = getSize();
				graphPanel.setSize(s);
			}
		});
		selection = new Selection();
		this.translation = new Point(0, 0);
	}

//
//	Paint methods

	/** Called from the JComponent's paint() 
	 * @param g the JComponent's Graphics object to draw on */
	protected void paintNodes(Graphics g) {
		Enumeration<GraphNode> e = nodes.elements();
		while (e.hasMoreElements()) {
			try {
				GraphNode node = e.nextElement();
				paintNode(node, g);
			} catch (Throwable t) {
				System.out.println("GC: Error paintnodes " + t);	
			}
		}
	}
	/** Called from the JComponent's paint() 
	 * @param g the JComponent's Graphics object to draw on */
	protected void paintEdges(Graphics g) {
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

	protected void paintNode(GraphNode node, Graphics g) {
		g.setColor(node.getColor());
		Point p = node.getXY();
		int iconWidth = 21;
		int iconHeight = 17;
		g.fillRect(p.x - iconWidth/2, p.y - iconHeight/2, iconWidth, iconHeight);
		
		if (node == selection.topic && selection.mode == Selection.SELECTED_TOPIC) {
			g.setColor(Color.red);
			g.drawRect(p.x - iconWidth/2 - 2, p.y - iconWidth/2 - 2, iconWidth + 3, iconHeight + 3);
			g.drawRect(p.x - iconWidth/2 - 3, p.y - iconWidth/2 - 3, iconWidth + 5, iconHeight + 5);
		}
		
		if (node.getLabel() != null) {
			g.setColor(Color.black);
				g.setFont(font); 
				g.drawString(node.getLabel(), p.x - 9, p.y + 9 + font.getSize() + 2);
		}
	}
	
	protected void paintEdge(GraphEdge edge, Graphics g) {
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
	
	// Directly copied from Joerg Richter's DeepaMehta 2	(like much of this class)
	protected static void paintLine(Graphics g, int x1, int y1, int x2, int y2,
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
	
	/** Defines the items and lines 
	 * @param nodes the items (icons and text)
	 * @param edges the connector lines */
	public void setModel(Hashtable<Integer, GraphNode> nodes, Hashtable<Integer, GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}
	
//
//	Clicking

	protected void thisPanelPressed(MouseEvent e) {
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
	
	protected void thisPanelDragged(MouseEvent e) {
		if (moveInProgress || translateInProgress) {
			dragInProgress = true;
			int x = e.getX();
			int y = e.getY();
			int dx = x - mX;
			int dy = y - mY;
			mX = x;
			mY = y;
			if (moveInProgress) translateNode(selection.topic, dx, dy);
			else translateGraph(dx, dy);
			repaint();
		} else if (edgeInProgress) {
			dragInProgress = true;
			ex = e.getX();
			ey = e.getY();
			GraphNode foundNode = findNode(ex, ey, false);
			targetNode = foundNode;
			if (targetNode != null) {
				Point p = targetNode.getXY();
				ex = p.x + translation.x;
				ey = p.y + translation.y;
			}
			repaint();
		}
	}
	
	protected void thisPanelReleased(MouseEvent e) {
		if (moveInProgress) {
			moveInProgress = false; 
			dragInProgress = false;
		} else if (translateInProgress) {
			translateInProgress = false;
			dragInProgress = false;
		} else if (edgeInProgress) {
			edgeInProgress = false;
			dragInProgress = false;
			if (targetNode != null && targetNode != selection.topic) {
				GraphNode node1 = selection.topic;
				GraphNode node2 = targetNode;
				controlerCore.createEdge(node1, node2);
			}	
			repaint();		
		}
	}
	
	private void nodeClicked(GraphNode node, MouseEvent e) {
		nodeSelected(node);	
		int x = e.getX();
		int y = e.getY();
		if (e.isAltDown() || SwingUtilities.isMiddleMouseButton(e)) {
			edgeInProgress = true;
			targetNode = null;
			ex = x;
			ey = y;
		} else {							// default -- start moving a node
			moveInProgress = true;
			lastPoint = new Point(x, y);
		}
	}

	private void edgeClicked(GraphEdge edge, MouseEvent e) {
		edgeSelected(edge);
	}

	private void graphClicked(MouseEvent e) {
		graphSelected();	
		translateInProgress = true;
	}
	
//
//	Selecting

	
	public void nodeSelected(GraphNode node) {
		if (node != selection.topic) {
			selection.mode = Selection.SELECTED_TOPIC;
			controlerCore.nodeSelected(node);
			selection.topic = node;
			selection.assoc = null;
			repaint();
		}
	}

	protected void edgeSelected(GraphEdge edge) {
		if (edge != selection.assoc) {
			selection.mode = Selection.SELECTED_ASSOCIATION;
			controlerCore.edgeSelected(edge);
			selection.assoc = edge;
			selection.topic = null;
			repaint();
		}
	}
	
	/** Triggered via graphClicked() and thisPanelPressed() from MouseAdapter */
	public void graphSelected() {
		if (selection.mode != Selection.SELECTED_TOPICMAP) {
			selection.mode = Selection.SELECTED_TOPICMAP;
			selection.topic = null;
			selection.assoc = null;
			controlerCore.graphSelected();
			repaint();
		}
	}

//
//	Dragging

	/** Pan the map 
	 * @param x the horizontal move in pixels
	 * @param y the vertital move in pixels */
	public void translateGraph(int x, int y) {
		translation.x += x;
		translation.y += y;
	}

	/** Move an icon 
	 * @param node the icon's item
	 * @param x the horizontal move in pixels
	 * @param y the vertital move in pixels */
	protected void translateNode(GraphNode node, int x, int y) {
		node.getXY().translate(x, y);
	}
	
//
//	Find methods and classes   
//	TODO decide if to reactivate the click on label possibility 
	
	private Vector<GraphNode> findAllNodes(int x, int y) {
		x -= translation.x;
		y -= translation.y;
		//
		Vector<GraphNode> foundNodes = new Vector<GraphNode>();
		Enumeration<GraphNode> e = nodes.elements();
		while (e.hasMoreElements()) {
			GraphNode node = (GraphNode) e.nextElement();
			Point p = node.getXY();
			int iconWidth = 18;     
			int iw2 = iconWidth / 2;
			// check if inside icon
			if (Math.abs(x - p.x) <= iw2 && Math.abs(y - p.y - 4) <= 15) {
				foundNodes.addElement(node);
			}
		}
		return foundNodes;
	}

	private boolean edgeHit(Point p1, Point p2, Point hit) {
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
	public GraphNode findNode(int x, int y, boolean selectNextInBunch) {
		Vector<GraphNode> bunch = findAllNodes(x, y);
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

	protected GraphEdge findEdge(int x, int y) {
		x -= translation.x;
		y -= translation.y;
		Enumeration<GraphEdge> e = edges.elements();
		GraphEdge edge = null;
		GraphNode node1, node2;
		Point p1, p2;
		// --- phase 1: collect edge candidates based on bounding rectangle ---
		Vector<GraphEdge> candidates = new Vector<GraphEdge>();
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

//
//	Right-clicking etc.
	
	protected boolean isSpecial(MouseEvent e) {
		return e.getClickCount() == 2 || e.isAltDown() || isPopupTrigger(e) || 
				SwingUtilities.isMiddleMouseButton(e);
	}
	protected boolean isPopupTrigger(MouseEvent e) {
		if (e.isPopupTrigger()) {
			return true;
		} else if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0) {
			return true;
		}
		return false;
	}
	
	//	TODO eliminate this tmp thing
	protected Selection getSelectionInstance() {
		return selection;
	}
	public void setSize(int size) {	// distinguish from JComponent setSize()
		font = new Font(font.getName(), font.getStyle(), size);
		repaint();
	}
	public void init() {
		
	}
}

package de.x28hd.tool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;

public class GraphCore extends JDesktopPane {
	JComponent graphPanel;
	public PresentationService controler;
	public PresentationCore controCore;
	boolean dumbCaller;	// to disable things temporarily
	
	Hashtable<Integer, GraphNode> nodes;
	Hashtable<Integer, GraphEdge> edges;
	
	Font font = new Font("monospace", Font.PLAIN, 12);
	public Selection selection;
	
	// directly from jri
	private boolean dragInProgress;			// true if dragged before released
	private boolean translateInProgress;	// \
	private boolean moveInProgress;			// | max one is set
	private boolean edgeInProgress;			// |
	private boolean clusterInProgress;		// /
	
	public int ex, ey;						// used while edgeInProgress (& rectangleGrowing)
	public int mX, mY;						// last mouse position
	public Point translation;
	
	JComponent graphComponent = new JComponent() {
		public void paint(Graphics g) { 
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
			g.translate(translation.x, translation.y);
			paintEdges(g);
			paintNodes(g);
		}
		private static final long serialVersionUID = 1L;
	};
	
	public GraphCore(Object caller) {
		dumbCaller = !(caller instanceof PresentationService);
		controCore = (PresentationCore) caller;
		if (!dumbCaller) controler = (PresentationService) caller;
		showDiag();
		
		graphPanel = graphComponent;
		add(graphPanel);
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

	public void paintNodes(Graphics g) {
		Enumeration<GraphNode> e = nodes.elements();
		while (e.hasMoreElements()) {
			try {
				GraphNode node = e.nextElement();
				paintNode(node, g);
			} catch (Throwable t) {
				System.out.println("GP: Error paintnodes " + t);	
			}
		}
//		if (rectangleInProgress) paintRect(g);
	}
	public void paintEdges(Graphics g) {
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

	public void paintNode(GraphNode node, Graphics g) {
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
	
	public void paintEdge(GraphEdge edge, Graphics g) {
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
	void setModel(Hashtable<Integer, GraphNode> nodes, Hashtable<Integer, GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
		System.out.println(nodes.size());
	}
	
	Point getTranslation() {
		return translation;
	}
	//	TODO eliminate this tmp thing
	public Selection getSelectionInstance() {
		return selection;
	}
	public void setSize(int size) {	// distinguish from JComponent setSize()
		font = new Font(font.getName(), font.getStyle(), size);
		repaint();
	}
	public void showDiag() {
		System.out.println("GP from dumb? " + dumbCaller);
	}
}

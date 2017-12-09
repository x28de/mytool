package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JComponent;

public class GraphPanelZoom extends JComponent {
	private static final long serialVersionUID = 1L;
	JComponent graphPanelZoom;
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
//	private Point translation = new Point(0, 0);
	private Point translation;
	int zoomFactor = 100;
	int mX;;
	int mY;

	public GraphPanelZoom(Point transl) {
		translation = transl;
		
		graphPanelZoom = new JComponent() {
			public void paint(Graphics g) { 
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(getForeground());
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
						System.out.println("GPZ: Error paintnodes " + t);	
					}
				}
			}
			private void paintEdges(Graphics g) {
				Enumeration<GraphEdge> e = edges.elements();
				while (e.hasMoreElements()) {
					GraphEdge edge = e.nextElement();
					paintEdge(edge, g);
				}
			} 
			private static final long serialVersionUID = 1L;
		};
		setLayout(new BorderLayout());
		add(graphPanelZoom);
		setBackground(Color.WHITE);
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				rememberPos(e);
			}
			public void mouseReleased(MouseEvent e) {
				rememberPos(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				thisPanelDragged(e);
			}
		});
	}
	
	private void paintNode(GraphNode node, Graphics g) {
		g.setColor(node.getColor());
		Point p = node.getXY();
		int iconWidth = (18 * zoomFactor)/100;
		int iconHeight = (18 * zoomFactor)/ 100;
		p = new Point((p.x * zoomFactor)/100, (p.y * zoomFactor) / 100);
		g.fillOval(p.x - iconWidth/2, p.y - iconHeight/2, iconWidth, iconHeight);

	}

	private void paintEdge(GraphEdge edge, Graphics g) {
		Point from = nodes.get(edge.getN1()).getXY();
		from = new Point((from.x * zoomFactor)/100, (from.y * zoomFactor) / 100);

		Point to = nodes.get(edge.getN2()).getXY();
		to = new Point((to.x * zoomFactor)/100, (to.y * zoomFactor) / 100);

		g.setColor(edge.getColor());

		paintLine(g, from.x, from.y, to.x, to.y, true);
	}

	// Directly copied from Jörg Richter's DeepaMehta 2	(like much of this class)
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
	
	public void zoom(int factor) {
		zoomFactor = factor;
		repaint();
	}
	
	void setModel(Hashtable<Integer, GraphNode> nodes, Hashtable<Integer, GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
		graphPanelZoom.repaint();
	}

	public void rememberPos(MouseEvent e) {
		mX = e.getX();
		mY = e.getY();
	}
	public void thisPanelDragged(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			int dx = x - mX;
			int dy = y - mY;
			mX = x;
			mY = y;
			translateGraph(dx, dy);
			repaint();
	}
	
	public void translateGraph(int x, int y) {
		translation.x += x;
		translation.y += y;
	}
}

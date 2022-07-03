package de.x28hd.tool;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GraphPanelZoom extends JComponent implements ChangeListener, ItemListener, ActionListener {
	private static final long serialVersionUID = 1L;
	JComponent graphPanelZoom;
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	private Point translation;
	GraphPanelControler controler;
	int zoomFactor = 100;
	int mX;
	int mY;
	Font font = new Font("monospace", Font.PLAIN, 12);
	JSlider slider;
	final static float dash1[] = {10.0f};
    final static BasicStroke dashed =
        new BasicStroke(1.0f,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f, dash1, 0.0f);
    JCheckBox markerBox;

	public GraphPanelZoom(Point transl, GraphPanelControler controler) {
		this.controler = controler;
		translation = transl;
		
		graphPanelZoom = new JComponent() {
			public void paint(Graphics g) { 
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
				//	crosshair
				if (markerBox.isSelected()) {
				g.setColor(Color.RED);
				g.drawLine(getWidth()/2 - 3,  getHeight()/2, getWidth()/2 + 3,  getHeight()/2);
				g.drawLine(getWidth()/2,  getHeight()/2 - 3, getWidth()/2,  getHeight()/2 + 3);
				((Graphics2D) g).setStroke(dashed);
				g.drawRect(getWidth()/2 * (100 - zoomFactor)/100, 
						getHeight()/2 * (100 - zoomFactor)/100, 
						getWidth() * zoomFactor/100 - 1,
						getHeight() * zoomFactor/100 - 1);
				((Graphics2D) g).setStroke(new BasicStroke());
				}
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
						System.out.println("Error GPZ101 paintNodes " + t);	
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
				mX = e.getX();
				mY = e.getY();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				thisPanelDragged(e);
			}
		});
	}
	
	//	Methods for repaint
	
	private void paintNode(GraphNode node, Graphics g) {
		g.setColor(node.getColor());
		Point p = node.getXY();
		int iconWidth = (18 * zoomFactor)/100;
		int iconHeight = (18 * zoomFactor)/ 100;
		
		p = zoomPoint(p);
		
		g.fillOval(p.x - iconWidth/2, p.y - iconHeight/2, iconWidth, iconHeight);
		
		if (node.getLabel() != null && zoomFactor > 25) {
			g.setColor(Color.black);
			g.setFont(font); 
			g.drawString(node.getLabel(), p.x - 9, p.y + 9 + font.getSize() + 2);
		}
	}

	private void paintEdge(GraphEdge edge, Graphics g) {
		Point from = nodes.get(edge.getN1()).getXY();
		from = zoomPoint(from);

		Point to = nodes.get(edge.getN2()).getXY();
		to = zoomPoint(to);

		g.setColor(edge.getColor());

		paintLine(g, from.x, from.y, to.x, to.y, true);
	}

	public Point zoomPoint(Point p) {
		p = new Point(((p.x - getWidth()/2 + translation.x) * 
				zoomFactor) / 100 + getWidth()/2 - translation.x, 
				((p.y - getHeight()/2 + translation.y) * 
				zoomFactor) / 100 + getHeight()/2 - translation.y);
		return p;
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
	
	//	Preparations
	
	public JPanel createSlider() {
		slider = new JSlider(JSlider.VERTICAL);
		slider.setValue(slider.getMaximum());
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setMaximum(125);
		slider.setMajorTickSpacing(10);
		Hashtable<Integer,JComponent> labelDict = new Hashtable<Integer,JComponent>();
		slider.createStandardLabels(125);
		for (int i = 1; i < 13; i++) {
			labelDict.put(new Integer(i * 10), (JComponent) new JLabel(i * 10 + " %"));
		}
		labelDict.put(new Integer(100), (JComponent) new JLabel("<html><b>100 %</b></html>"));
		slider.setLabelTable((Dictionary<Integer,JComponent>) labelDict);
		slider.addChangeListener(this);
		slider.updateUI();
		
		JButton backButton = new JButton("Return");
		backButton.setToolTipText("Return to normal size and functionality, panned to the marked area");
		backButton.addActionListener(this);
		backButton.setSelected(true);
		
		JPanel top = new JPanel();
		top.setLayout(new BorderLayout());
		top.add(backButton, "East");
		
		markerBox = new JCheckBox (" Mark the targeted area", true);
		markerBox.addItemListener(this);

		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());
		bottom.add(markerBox, "West");
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		panel.add(slider);
		panel.add(top, "North");
		panel.add(bottom, "South");
		return panel;
	}
	
	void setModel(Hashtable<Integer, GraphNode> nodes, Hashtable<Integer, GraphEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
		graphPanelZoom.repaint();
	}

	//	Main operations
	
	public void stateChanged(ChangeEvent arg0) {
		zoomFactor = slider.getValue();
		font = new Font("monospace", Font.PLAIN, (12 * zoomFactor)/ 100);
		repaint();
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
		translation.x += (x * 100) / zoomFactor;
		translation.y += (y * 100) / zoomFactor;
	}

	public void actionPerformed(ActionEvent arg0) {
		controler.getControlerExtras().zoom(false);
	}

	public void itemStateChanged(ItemEvent arg0) {
		repaint();
	}
}

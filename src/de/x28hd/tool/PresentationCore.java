package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class PresentationCore implements Runnable {

	JEditorPane edi = new JEditorPane();
	GraphPanel graphPanel;
	
	// Main fields
	Hashtable<Integer, GraphNode> nodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> edges = new Hashtable<Integer, GraphEdge>();
	
	// User Interface
	public JFrame mainWindow;
	public Container cp;	// Content Pane
	JPanel labelBox = null;
	JTextField labelField = null;
	
	JSplitPane splitPane = null;
	JPanel rightPanel = null;
	Selection selection = null;
	
	public void run() {
		createGraphPanel();
		initialize("Mein Window");
		graphPanel.setSize(12);
		mainWindow.setVisible(true);
	}
	
	public void createGraphPanel() {
		GraphNode n1 = new GraphNode(1,new Point(40, 40), Color.RED, "n1", "");
		GraphNode n2 = new GraphNode(2,new Point(140, 40), Color.GREEN, "n2", "");
		nodes.put(1, n1);
		nodes.put(2, n2);
		graphPanel = new GraphPanel(this);
	}
	
	public void initialize(String title) {
		graphPanel.setModel(nodes, edges);
		selection = graphPanel.getSelectionInstance();	//	TODO eliminate again
		splitPane = createMainGUI();
		createMainWindow(title);
		graphSelected();
	}

	public JSplitPane createMainGUI() {
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(960 - 232);
		splitPane.setResizeWeight(.8);
		splitPane.setDividerSize(8);

		graphPanel.setBackground(Color.WHITE);
		
		splitPane.setLeftComponent(graphPanel);

		rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		
		labelBox = new JPanel();
		labelBox.setLayout(new BorderLayout());
		labelBox.add(new JLabel("Label", JLabel.CENTER));
		labelBox.setToolTipText("Short text that also appears on the map. To see it there, click the map.");
		labelField = new JTextField();
		labelBox.add(labelField,"South");
		Dimension dim = new Dimension(1400,150);
		labelBox.setMaximumSize(dim);

		rightPanel.add(labelBox,"North");
		
		JPanel detailBox = new JPanel();
		detailBox.setLayout(new BoxLayout(detailBox, BoxLayout.Y_AXIS));
		JLabel detailBoxLabel = new JLabel("Detail", JLabel.CENTER);
		detailBoxLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		detailBox.add(detailBoxLabel);
		detailBox.setToolTipText("More text about the selected item, always at your fingertips.");
		detailBox.add(edi,"South");
		rightPanel.add(detailBox,"South");

		splitPane.setRightComponent(rightPanel);
		splitPane.repaint();
		
		return splitPane;
	}

	public void createMainWindow(String title) {
		mainWindow = new JFrame(title) {
			private static final long serialVersionUID = 1L;
		};
		
		mainWindow.setLocation(0, 30);
		mainWindow.setSize(960, 580);
		mainWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		mainWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		cp = mainWindow.getContentPane();
		cp.add(splitPane);
	}
	
	public void graphSelected() {
		graphPanel.grabFocus();		//  was crucial
	}
	
	public boolean close() {
		mainWindow.dispose();
		System.out.println("(tmp): Closed");
		System.exit(0);
		return true;

	}
}

package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class PresentationCore implements Runnable {

	TextEditorPanel edi = new TextEditorPanel(this);
	GraphCore graphObject = new GraphCore(this);

	
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
	
	// Placeholders
	GraphNode dummyNode = new GraphNode(-1, null, null, null, null);
	GraphNode selectedTopic = dummyNode;		// TODO integrate into Selection()
	GraphEdge dummyEdge = new GraphEdge(-1, dummyNode, dummyNode, null, null);
	GraphEdge selectedAssoc = dummyEdge;
	
	public void run() {
		createGraphPanel();
		initialize("Simple Window");
		graphObject.setSize(12);
		mainWindow.setVisible(true);
	}
	
	public void createGraphPanel() {
		// tmp data
		GraphNode n1 = new GraphNode(1,new Point(40, 40), Color.RED, "n1", "n1txt");
		GraphNode n2 = new GraphNode(2,new Point(140, 40), Color.GREEN, "n2", "n2txt");
		nodes.put(1, n1);
		nodes.put(2, n2);
		GraphEdge edge = new GraphEdge(1, n1, n2, Color.YELLOW, "edgetxt");
		edges.put(1, edge);
		n1.addEdge(edge);
		n2.addEdge(edge);
		
		if (this instanceof PresentationService) graphObject = new GraphPanel(this);
	}
	
	public void initialize(String title) {
		graphObject.setModel(nodes, edges);
		selection = graphObject.getSelectionInstance();	//	TODO eliminate again
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

		graphObject.setBackground(Color.WHITE);
		
		splitPane.setLeftComponent(graphObject);

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
	
//	Selection processing

	public void deselect(GraphNode node) {
		if (!node.equals(dummyNode)) {
			node.setLabel(labelField.getText());
			labelField.setText("");
			node.setDetail(edi.getText());
			edi.tracking(false);
			edi.setText("");
		}
	}

	public void deselect(GraphEdge edge) {
		if (!edge.equals(dummyEdge)) {
			String det = edi.getText();
			if (det.length() > 59) edge.setDetail(det);
			edi.setText("");
		}
	}	
	
	public void nodeSelected(GraphNode node) {
		deselect(selectedTopic);
		deselect(selectedAssoc);
		selectedAssoc = dummyEdge;
		selectedTopic = node;
		String labelText = selectedTopic.getLabel();
		labelField.setText(labelText);
		
		edi.setText((selectedTopic).getDetail());
		edi.tracking(true);
		edi.setDirty(false);
		edi.getTextComponent().requestFocus();
		edi.repaint();
	}

	public void edgeSelected(GraphEdge edge) {
		deselect(selectedAssoc);
		deselect(selectedTopic);
		selectedTopic = dummyNode;
		selectedAssoc = edge;
		edi.setText(selectedAssoc.getDetail());
		edi.getTextComponent().setCaretPosition(0);
		edi.repaint();
	}
	
	public void graphSelected() {
		deselect(selectedTopic);
		selectedTopic = dummyNode;
		deselect(selectedAssoc);
		selectedAssoc = dummyEdge;
		edi.setText("");
		graphObject.grabFocus();		//  was crucial
	}
	
	public GraphEdge createEdge(GraphNode topic1, GraphNode topic2) {
		if (topic1 != null && topic2 != null) {
			int newId = newKey(edges.keySet());
			GraphEdge assoc = new GraphEdge(newId, topic1, topic2, Color.decode("#c0c0c0"), "");  // nicht 239
			assoc.setID(newId);
			edges.put(newId, assoc);
			topic1.addEdge(assoc);
			topic2.addEdge(assoc);
			return assoc;
		} else {
			return null;
		}
	}
	
	public int newKey(Set<Integer> keySet) {
		int idTest = keySet.size();
		while (keySet.contains(idTest)) idTest++;
		return idTest;
	}
	
	public void addToLabel(String textToAdd) {
		if (selectedTopic == dummyNode) return;
		String oldText = labelField.getText();
		String newText = oldText + " " + textToAdd;
		labelField.setText(newText);
		GraphNode justUpdated = selectedTopic;
		graphSelected();
		nodeSelected(justUpdated);
		mainWindow.repaint();   // this was crucial
	}
	
	public boolean close() {
		mainWindow.dispose();
		System.out.println("(tmp): Closed");
		System.exit(0);
		return true;

	}
}

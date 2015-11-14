package de.x28hd;

import java.awt.Color;
import java.awt.Point;
import java.util.Hashtable;

import javax.swing.JOptionPane;


public class SplitIntoNew {
	private GraphPanelControler controler;

	Hashtable<Integer, GraphNode> nodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> edges = new Hashtable<Integer, GraphEdge>();
	String [][] columns = new String [500][2];
	int [] colons = new int [500];
	int [] commas = new int [500];
	int [] blanks = new int [500];

	
	public SplitIntoNew(final GraphPanelControler controler) {
		this.controler = controler;
		
	}
	
	int separateRecords(String rawText) {
		System.out.println("SplitIntoNew.separateRecords started");
		StringBuffer recordBuffer;
		String testForNewLine;
//		String newLine = System.getProperty("line.separator");
		String newLine= "\n";
		int newLineLength = newLine.length();
//		System.out.println(">" + newLine + "< (" + newLineLength + ")");
		char testForTab;
		char tabChar = "\t".charAt(0);
		char commaChar = ",".charAt(0);
		rawText = rawText.concat(newLine).concat(newLine);
		int len = rawText.length();

//
//		split rows and find tabs and commas along the way
//		TODO convert to text.split()

		int i = 0;
		int numRec = 0;
		commas[0] = -1;
		int col = 0;
		recordBuffer = new StringBuffer();
		int offsetOfRecord = 0 - newLineLength;
		boolean cont = true;
		while (cont) {
			int j = i + newLineLength;
			testForNewLine = rawText.substring(i, j);
			if (testForNewLine.equals(newLine)) {
				columns[numRec][col] = recordBuffer.toString();	
				recordBuffer = new StringBuffer();
				if (col != 0 || !columns[numRec][0].isEmpty()) numRec++;
				commas[numRec] = -1;
				if (numRec == 499) {
					cont = false;
					JOptionPane.showMessageDialog(null,"Truncated to 500 items, sorry\n");
				}
				col = 0;
				offsetOfRecord = i + newLineLength;
				i = j;
			} else {
				testForTab = rawText.charAt(i);
				if (testForTab != tabChar) {
					recordBuffer.append(testForTab);
					int posOfFirstComma = commas[numRec];
					if (posOfFirstComma == -1 && testForTab == commaChar) {
						posOfFirstComma = i - offsetOfRecord;
						commas[numRec] = posOfFirstComma;
					}
					i++;
				} else {
					columns[numRec][col] = recordBuffer.toString();	
					recordBuffer = new StringBuffer();
					col = 1;
					i++;
				}
			}
			if (i >= len-1) {
				cont = false;
				continue;
			}
		}
		if (numRec > 499) numRec = 500;
		return numRec;
	}

//	
//	find out if it is a reference list
	
	void heuristics(int numRec) {
		boolean noTabs = true;
		for (int i = 0; i <= numRec; i++) {
			if (columns[i][1] != null) {
				noTabs = false;
			}
		}
		boolean manyEarlyCommas = false;
		int earlyCommasCount = 0;
		for (int i = 0; i <= numRec; i++) {
			int commaPos = commas[i];
			if (commaPos > -1 && commaPos < 30) {
				earlyCommasCount++;
			} else {
			}
		}

		double commaPercentage = (double) earlyCommasCount/numRec;
		if (commaPercentage > .8) manyEarlyCommas = true;

		if (noTabs && manyEarlyCommas) {
			for (int i = 0; i <= numRec; i++) {
				String first = columns[i][0]; 
				columns[i][1] = first; 
				if (first == null) continue;
				int commaPos = commas[i];
				if (commaPos == -1) commaPos = first.length();
				if (commaPos > 25) commaPos = 25;
				columns[i][0] = first.substring(0, commaPos);
			}
		} else if (noTabs) {
			for (int i = 0; i <= numRec; i++) {
				String first = columns[i][0]; 
				columns[i][1] = first; 
				if (first == null) continue;
				if (first.length() > 30) {
					columns[i][0] = String.valueOf(i+1);
				}
			}
		}
	}
	
//
//	Turn rows into nodes	
	
	void createNodes(int newNodes) {
		System.out.println("SplitIntoNew.createNodes started");
		String newNodeColor = controler.getNewNodeColor();
//		String newLine = System.getProperty("line.separator");
		String newLine = "\r";
		int x;
		int y;
		int maxVert = (int) Math.sqrt(newNodes/3); // 3x more horizontal distance
		if (maxVert < 10) maxVert = 10;
		int j = 0;
		for (int i = 0; i < newNodes; i++) {
			String topicName = columns[i][0];
			String verbal = columns[i][1];
			System.out.println(verbal);
			if (topicName.equals(newLine)) topicName = "";
			if (verbal == null || verbal.equals(newLine)) verbal = "";
			if (topicName.isEmpty() && verbal.isEmpty()) continue;
			int id = 100 + j;
			y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
			x = 40 + (j/maxVert) * 150;
			Point p = new Point(x, y);
			GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	
			nodes.put(id, topic);
			j++;
		}
		edges.clear();

//		controler.scoopNewNodes(this);
	}

	public Hashtable<Integer, GraphNode> getNodes() {
		System.out.println("SplitIntoNew returning " + nodes.size() + " nodes");
		return nodes;
	}
	public Hashtable<Integer, GraphEdge> getEdges() {
		System.out.println("SplitIntoNew returning " + edges.size() + " edges");
		return edges;
	}

}

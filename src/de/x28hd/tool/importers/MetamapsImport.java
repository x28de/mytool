package de.x28hd.tool.importers;
import java.awt.Color;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Hashtable;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;
import de.x28hd.tool.exporters.TopicMapStorer;

public class MetamapsImport {
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	String dataString = "";
	
	int j = -1;
	int maxVert = 10;
	String newNodeColor = "#ccdddd";
	int edgesNum = 0;

	
	public MetamapsImport (File file, PresentationService controler) {
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error MI101 " + e);
		}
		Utilities utilities = new Utilities();
		String linesString = utilities.convertStreamToString(fileInputStream);		
		String [] lines = linesString.split("\\r?\\n");	
		int startTopics = Integer.MAX_VALUE;
		int startSynapses = Integer.MAX_VALUE;
		
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.isEmpty()) continue;
			
			String [] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
			// credit to Bart Kiers at SO for the regex
			if (line.equals("Topics")) startTopics = i + 2; 
			if (line.equals("Synapses")) {
				startTopics = Integer.MAX_VALUE;
				startSynapses = i + 2;
			}
			if (i >= startTopics) {
				if (fields.length < 6) {
					controler.displayPopup("Invalid CSV format:\n" + line);
					return;
				};
				int x = Integer.parseInt(fields[3]);
				int y = Integer.parseInt(fields[4]);
				addNode(fields[0], fields[1], x, y, fields[5]);
				
			} else if (i >= startSynapses) {
				addEdge(fields[0], fields[1]);
			}
			continue;
		}
		
		// 	pass on
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error MI108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error MI109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error MI110 " + e1);
    	}
		
    	controler.getNSInstance().setInput(dataString, 2);
	}
	
	public void addNode(String nodeRef, String name, int x, int y, String detail) { 
		j++;
		String newNodeColor;
		String newLine = "\r";
		String topicName = name; 
		newNodeColor = "#ccdddd";
		String verbal = detail;
		if (verbal.startsWith("\"")) verbal = verbal.substring(1);
		if (verbal.endsWith("\"")) verbal = verbal.substring(0, verbal.length() - 1);
//		TODO improve and cater to embedded "s
		topicName = topicName.replace("\r"," ");
		if (topicName.equals(newLine)) topicName = "";
		if (verbal == null || verbal.equals(newLine)) verbal = "";
		if (topicName.isEmpty() && verbal.isEmpty()) return;
		int id = 100 + j;

		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		inputID2num.put(nodeRef, id);
	}

	public void addEdge(String fromRef, String toRef) {
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
		edgesNum++;
		if (!inputID2num.containsKey(fromRef)) {
			System.out.println("Error MI103 " + fromRef + ", ");
			return;
		}
		GraphNode sourceNode = nodes.get(inputID2num.get(fromRef));
		if (!inputID2num.containsKey(toRef)) {
			System.out.println("Error MI104 " + toRef);
			return;
		}
		GraphNode targetNode = nodes.get(inputID2num.get(toRef));
		edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), "");
		edges.put(edgesNum, edge);
	}
	
	
}

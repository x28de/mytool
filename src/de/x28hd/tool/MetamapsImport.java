package de.x28hd.tool;
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

public class MetamapsImport {
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	String dataString = "";
	
	int j = -1;
	int maxVert = 10;
	String newNodeColor = "#ccdddd";
	int edgesNum = 0;

	
	public MetamapsImport (File file, GraphPanelControler controler) {
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error MI101 " + e);
		}
		String linesString = convertStreamToString(fileInputStream);		
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
	
	
//
// 	Accessory 
//	Duplicate of NewStuff TODO reuse

	private String convertStreamToString(InputStream is, Charset charset) {
    	
        //
        // From http://kodejava.org/how-do-i-convert-inputstream-to-string/
        // ("To convert the InputStream to String we use the
        // Reader.read(char[] buffer) method. We iterate until the
        // Reader return -1 which means there's no more data to
        // read. We use the StringWriter class to produce the string.")
    	
    	if (is != null) {
    		Writer writer = new StringWriter();
    		char[] buffer = new char[1024];
    		Reader reader = null;;
    		
   			reader = new BufferedReader(
   					new InputStreamReader(is, charset));	

    		int n;
    		try {
    			while ((n = reader.read(buffer)) != -1) {
    				writer.write(buffer, 0, n);
    			}
    		} catch (IOException e) {
    			System.out.println("Error MI117 " + e);
    			try {
    				writer.close();
    			} catch (IOException e1) {
    				System.out.println("Error MI118 " + e1);
    			}
    		} finally {
    			try {
    				is.close();
    			} catch (IOException e) {
    				System.out.println("Error MI119 " + e);
    			}
    		}
    		String convertedString = writer.toString();
    		return convertedString;
    	} else {        
    		return "";
    	}
    }
    private String convertStreamToString(InputStream is) {
    	return convertStreamToString(is, Charset.forName("UTF-8"));
    }
}

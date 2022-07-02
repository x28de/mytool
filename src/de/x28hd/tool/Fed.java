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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.transform.TransformerConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

public class Fed {

//	Imports the page name structure of a local Federated Wiki
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,Integer> lookup = new Hashtable<String,Integer>();
	int nodeCount = 0;
	int edgeCount = 0;
	Color nodeColor = Color.decode("#ccdddd");
	Color edgeColor = Color.decode("#c0c0c0");
	HashSet<String> edgesDone = new HashSet<String>(); 
	GraphNode node = null;

	public Fed(GraphPanelControler controler) {
		JFrame mainWindow = controler.getMainWindow();
		String pageDir = "";
		try {
			pageDir = System.getProperty("user.home") + 
					File.separator + ".wiki" + File.separator + "pages";
		} catch (Throwable e) {
			System.out.println("Error FE103" + e );
		}
		File fedDir = new File(pageDir);

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(fedDir);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setSelectedFile(fedDir);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
        		"(Select your wiki \"pages\" folder)", "folder");
        chooser.setFileFilter(filter);        
	    int returnVal = chooser.showOpenDialog(mainWindow);
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	    	fedDir = chooser.getSelectedFile();
	    }
		File[] pages = fedDir.listFiles();
	    int len = pages.length;
	    
	    //	Read the pages
	    for (int i = 0; i < len; i++) {
	    	System.out.println(pages[i].getName());
			FileInputStream fileInputStream = null;
			try {
				fileInputStream = new FileInputStream(pages[i]);
			} catch (FileNotFoundException e) {
				System.out.println("Error FE101 " + e);
			}
			String pageString = convertStreamToString(fileInputStream);	
			
			JSONObject pageObj = null;
			String title = "";
			JSONObject storyObj = null;
			JSONArray array = null;
			JSONObject paraObj = null;
			JSONArray paraArray = null;
			String story = "";
			JSONObject siteObj = null;
			
			try {
				//	Sitemap
				if (pages[i].getName().equals("sitemap.json")) {
					array = new JSONArray(pageString);
			    	int arrayLen = array.length();
			    	for (int j = 0; j < arrayLen; j++) {
			    		siteObj = array.getJSONObject(j);
				    	String synopsis = (String) siteObj.getString("synopsis");
				    	String slug = (String) siteObj.getString("slug");
				    	node = addNode(j,slug);
				    	node.setDetail(synopsis);
				    	nodes.put(j,  node);
				    	System.out.println(slug);
			    	}
			    	continue;
				} else {
					
				//	Page JSON
				pageObj = new JSONObject(pageString);
		    	title = (String) pageObj.get("title");
		    	node = null;
		    	if (!lookup.containsKey(title.toLowerCase())) {
		    		node = addNode(nodeCount, title.toLowerCase()); 
		    		nodes.put(nodeCount, node);
		    		lookup.put(title.toLowerCase(), nodeCount);
		    		nodeCount++;
		    	} else {
		    		int nodeID = lookup.get(title.toLowerCase());
		    		node = nodes.get(nodeID);
		    	}
		    	
		    	//	Paragraphs
		    	array = (JSONArray) pageObj.get("story");
		    	int arrayLen = array.length();
		    	storyObj = new JSONObject(array.get(0));
    			String detail = "";
	    		detail += "<body style=\"font-family: Verdana; "
	    				+ "padding: 20px; line-height: 1.3;"
	    				+ "max-width: 490px;\">\n"
	    				+ "<h1>&#9640; "
	    				+ node.getLabel() + "</h1>";
		    	for (int j = 0; j < arrayLen; j++) {
		    		paraObj = array.getJSONObject(j);
		    		
		    		//	Get text
		    		String[] names = paraObj.getNames(paraObj);
		    		for (int k = 0; k < names.length; k++) {
		    			if (!names[k].equals("text")) continue;
		    			String text = (String) paraObj.get("text");
		    			
		    			// Get hyperlinks
		    			if (text.contains("<h3>")) {
		    				text += "</h3>";
		    			} else {
		    				text += "<p>";
		    			}
		    			String rest0 = text;
		    			text = "";
		    			while (rest0.contains("[http")) {
//		    			if (rest0.contains("[http")) {
		    				int linkStart = rest0.indexOf("[http");
		    				text += rest0.substring(0, linkStart);
		    				rest0 = rest0.substring(linkStart + 1);
//		    				String regex = "(http\\S++) (\\w++)\\].*?";
		    				String regex = "(http\\S++) (\\w++)\\](.*?)";
		    				Pattern p = Pattern.compile(regex);
		    				Matcher m = p.matcher(rest0);
		    				if (!m.matches()) {
		    					System.out.println(regex + " " + m.toMatchResult() +  "\n no match: " + rest0);
		    					continue;
		    				}
			    			String link = "<a style=\"text-decoration: none;\" href=\"" + m.group(1) + "\">" + m.group(2) + "</a>";
			    			System.out.println("link = " + link);
//		    				rest0 = rest0.replaceFirst(regex, "$3");
		    				rest0 = m.group(3);
		    				text += link;
		    			}
		    			text += rest0;
		    			String rest = text;
		    			while (rest.contains("[[")) {
		    				int linkStart = rest.indexOf("[[");
		    				detail += rest.substring(0, linkStart);
		    				rest = rest.substring(linkStart + 2);
		    				if (!rest.contains("]]")) continue;
		    				int linkEnd = rest.indexOf("]]");
		    				String link = rest.substring(0, rest.indexOf("]]"));
		    				rest = rest.substring(linkEnd + 2);
		    				detail += "<a style=\"text-decoration: none;\" href=\"#" + link.toLowerCase() + "\">" + link + "</a>";
		    				
		    				int linkedID = -1;
		    		    	if (!lookup.containsKey(link.toLowerCase())) {
		    		    		GraphNode node2 = addNode(nodeCount, link.toLowerCase()); 
		    		    		nodes.put(nodeCount, node2);
		    		    		linkedID = nodeCount;
		    		    		lookup.put(link.toLowerCase(), linkedID);
		    		    		nodeCount++;
		    		    	} else {
		    		    		linkedID = lookup.get(link.toLowerCase());
		    		    	}
		    		    	GraphNode n2 = nodes.get(linkedID);
		    		    	String n2label = n2.getLabel();
		    		    	String nodelabel = node.getLabel();
		    				String unique = nodelabel.compareToIgnoreCase(n2label) > 0 ? 
		    						(nodelabel + " -- " + n2label) : (n2label + " -- " + nodelabel);
		    				if (edgesDone.contains(unique)) {
		    					System.out.println("Duplicate link " + unique + " skipped");
		    					continue;
		    				}
		    				edgesDone.add(unique);
		    		    	GraphEdge edge = new GraphEdge(edgeCount, node, n2, nodeColor, "");
		    		    	edges.put(edgeCount, edge);
		    		    	edgeCount++;
		    				System.out.println(node.getLabel() + "  -> " + link);
		    			}
		    			detail += rest;
		    		}
		    		detail = detail.replace("<p><h3>", "<h3>");
	    			node.setDetail(detail);
		    	}
				}
			} catch (JSONException e) {
				System.out.println("Error FE102 " + e);
				continue;
			}
	    }
	    
	    // Pass on
	    
	    Enumeration<GraphNode> nodeList = nodes.elements();
	    while (nodeList.hasMoreElements()) {
	    	node = nodeList.nextElement();
	    	if (node.getDetail().isEmpty()) node.setColor("#eeeeee");
	    }
	    
	    String dataString = "";
    	try {
    		dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
    	} catch (TransformerConfigurationException e1) {
    		System.out.println("Error FE108 " + e1);
    	} catch (IOException e1) {
    		System.out.println("Error FE109 " + e1);
    	} catch (SAXException e1) {
    		System.out.println("Error FE110 " + e1);
    	}
    	controler.getNSInstance().setInput(dataString, 2);
    	controler.getControlerExtras().setTreeModel(null);
    	controler.getControlerExtras().setNonTreeEdges(null);
    	
	   	controler.getControlerExtras().toggleHashes(true);
		controler.fixDivider();
	}
	
	public GraphNode addNode(int id, String label) {
		int x = 150 * (id / 10);
		int y = 40 * (id % 10);
		return new GraphNode(id, new Point(x, y), edgeColor, label, "");
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

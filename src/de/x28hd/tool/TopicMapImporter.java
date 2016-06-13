package de.x28hd.tool;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TopicMapImporter {		//	OLD map format !
	
	//	Major fields
	String dataString = "";
	Hashtable<Integer,GraphNode> newNodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> newEdges = new Hashtable<Integer,GraphEdge>();
	Hashtable<String,String> inputItems = new Hashtable<String,String>();
	Hashtable<String,String> inputItems2 = new Hashtable<String,String>();
	Hashtable<String,Point> itemPositions = new Hashtable<String,Point>();
	
	//	Keys for nodes and edges, incremented in addNode and addEdge
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	Hashtable<String,Integer> edgeID2num = new  Hashtable<String,Integer>();
	int j = -1;
	int edgesNum = 0;
	
	//	Constants
	private static final String XML_ROOT = "topicmap";
	int maxVert = 10;
	GraphPanelControler controler;
	
	//	Map loading
	boolean readyMap = false;
	int topicnum = 0;
	int assocnum = 0;
	Element root;
	boolean isAssoc = false;
	int nodenum = -1;
	int edgenum = -1;
	String nodesArray [][] = new String [600][5];   // 0 = x, 1 = y, 2 = rgb, 3 = label, 4 = id
	String edgesArray [][] = new String [600][3];    // 0 = n1, 1 = n2, 2 = rgb
	Hashtable<String, Integer> nodeids = new Hashtable<String, Integer>();
	Hashtable<String, Integer> edgeids = new Hashtable<String, Integer>();
	int minX, maxX, minY, maxY;
	
	public TopicMapImporter(JFrame mainWindow, GraphPanelControler controler) {
		this.controler = controler;
		
//
//		Open XML document
		
//		File file = new File("C:\\Users\\Matthias\\Desktop\\probetext\\word\\document.xml");
		FileDialog fd = new FileDialog(mainWindow);
		fd.setTitle("Select an old Topicmap file");
		fd.setMode(FileDialog.LOAD);
		fd.setVisible(true);
		String filename = fd.getDirectory() + File.separator + fd.getFile();
		File file = new File(filename);
		new TopicMapImporter(file, controler);
	}
	
	public TopicMapImporter(File file, GraphPanelControler controler) {
		Charset CP850 = Charset.forName("CP850");
		ZipFile zfile = null;
		try {
			zfile = new ZipFile(file,CP850);
			Enumeration<? extends ZipEntry> e = zfile.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				String filename = entry.getName();
				filename = filename.replace('\\', '/');		
				if (!filename.equals("savefile.xml") 
					&& !filename.startsWith("topicmap-t-")) {
					continue;
				} else {
					InputStream stream = zfile.getInputStream(entry);
					new TopicMapImporter(stream, controler);
				}
			}
		} catch (IOException e1) {
			System.out.println("Error ID111 " + e1);
		}
	}
	
	public TopicMapImporter(InputStream stream, GraphPanelControler controler) {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error BI102 " + e2 );
		}
		
		try {
			inputXml = db.parse(stream);
			
			root = inputXml.getDocumentElement();
			if (root.getTagName() != XML_ROOT) {
				System.out.println("Error BI105, unexpected: " + root.getTagName() );
				stream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error BI106 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error BI107 " + e );
		}
		new TopicMapImporter(inputXml, controler);
	}
	
	public TopicMapImporter(Document inputXml, GraphPanelControler controler) {

		Element root = inputXml.getDocumentElement();

		NodeList topicsAndAssocs = root.getChildNodes();
		System.out.println("Reading in " + topicsAndAssocs.getLength() + " items");
		
		// Read structure, save in intermediate arrays, and record old ID to new ID mapping
		
		for (int i = 0; i < topicsAndAssocs.getLength(); i++) {	
			Node node = topicsAndAssocs.item(i);
			String topicid ="";
			int r, g, b;
			r = g = b = 0;
			String hex = "";
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				isAssoc = false;
				if (topicsAndAssocs.item(i).getNodeName() == "assoc") {
					isAssoc = true;
				} else if (topicsAndAssocs.item(i).getNodeName() == "topic") {
					isAssoc = false;
				} else continue;
				if (!isAssoc) nodenum++; else edgenum++;

//				System.out.println(i + ": " + topicsAndAssocs.item(i).getNodeName());
//				System.out.println(((Element) topicsAndAssocs.item(i)).getTagName());
				NamedNodeMap attrs = topicsAndAssocs.item(i).getAttributes();
				
				//	Attributes
				
				for (int j = 0; j < attrs.getLength(); j++) {
					if (attrs.item(j).getNodeName().equals("ID")) {
						topicid = attrs.item(j).getNodeValue();
//						System.out.println("j=" + j + ": " + topicid);
						if (!isAssoc) {
							nodeids.put(topicid, nodenum);
						} else {
							edgeids.put(topicid, edgenum);
						}
					}
					if (attrs.item(j).getNodeName().equals("x")) {
						if (!isAssoc) nodesArray[nodenum][0] = attrs.item(j).getNodeValue();
						else edgesArray[edgenum][0] = attrs.item(j).getNodeValue();
					}
					if (attrs.item(j).getNodeName().equals("y")) {
						if (!isAssoc) nodesArray[nodenum][1] = attrs.item(j).getNodeValue();
						else edgesArray[edgenum][1] = attrs.item(j).getNodeValue();
					}
					if (attrs.item(j).getNodeName().equals("r")) {
						r = Integer.parseInt(attrs.item(j).getNodeValue());
					}
					if (attrs.item(j).getNodeName().equals("g")) {
						g = Integer.parseInt(attrs.item(j).getNodeValue());
					}
					if (attrs.item(j).getNodeName().equals("b")) {
						b = Integer.parseInt(attrs.item(j).getNodeValue());
					}
					
					//  Very old (DeepaMehta 2) colors ?
					if (attrs.item(j).getNodeName().equals("color")) {
						if (hex == "") hex = attrs.item(j).getNodeValue().toLowerCase();
					}
					if (attrs.item(j).getNodeName().equals("icon")) {
						String oldIcon = attrs.item(j).getNodeValue();
						if (oldIcon.length() > 12) {
							boolean properHex = true;
							String oldRgb  = oldIcon.substring(3, 9);    //  tt-rrggbb.png
							for (int pos = 0; pos < 6; pos++) {
								if (!oldRgb.substring(pos, pos+1).matches("[0-9a-f]")) {
									properHex = false;
									break;
								}
							}
							if (properHex && hex == "") hex = "#" + oldRgb.toLowerCase();
						}
					}
				} 	//	Next Attribute

				if (hex == "") hex = String.format("#%02x%02x%02x", r, g, b);
					
				if (!isAssoc) nodesArray[nodenum][2] = hex;
				else edgesArray[edgenum][2] = hex;
				
				// Children nested?
				
				if (topicsAndAssocs.item(i).hasChildNodes()) {
//					System.out.println("Has children");
					
					nest(topicsAndAssocs.item(i), "", topicid);
				}

			} else {
				System.out.println(i + " not an Element but: " + node.getNodeType());
			}	//	End of element nodes 

		}
		
		//	Processing the topics

		System.out.println("NS: nodenum = " + (nodenum + 1));
		for (int i = 0; i < nodenum + 1; i++) {
//			System.out.println("---- x = " + nodesArray[i][0] + ", y = " + nodesArray[i][1] + ", rgb = " + nodesArray[i][2] 
//					+ ", label = " + nodesArray[i][3] + ", detail length = " + nodesArray[i][4].length());
			int x = Integer.parseInt(nodesArray[i][0]);
			int y = Integer.parseInt(nodesArray[i][1]);
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
			GraphNode node = new GraphNode(i, 	// ID
					new Point(x, y),
					Color.decode(nodesArray[i][2]), 
					nodesArray[i][3], 	// label
					nodesArray[i][4]);	// detail
//			System.out.println("newNodes contains " + newNodes.size() + " items");
			newNodes.put(i, node);
//			System.out.println("newNodes contains now " + newNodes.get(i) + " as key " + i);
		}

		// Processing the assocs
		
		System.out.println("NS: edgenum = " + (edgenum + 1));
		for (int i = 0; i < edgenum + 1; i++) {
//			System.out.println("---- n1 = " + edgesArray[i][0] + ", n2 = " + edgesArray[i][1] + ", rgb = " + edgesArray[i][2]);
			int n1 = Integer.parseInt(edgesArray[i][0]);
			int n2 = Integer.parseInt(edgesArray[i][1]);
			if (!newNodes.containsKey(n1) || !newNodes.containsKey(n2)) {
				System.out.println("NS: " + i + "-th edge not created");
				continue;	//  edgesArray[][] == -1 signals when input data corrupt
			}
			GraphEdge edge = new GraphEdge(i, newNodes.get(n1), 
										   newNodes.get(n2),
										   Color.decode(edgesArray[i][2]), 
											"");			// detail TODO
//			System.out.println("newEdges contains " + newEdges.size() + " items");
//			edge.setID(i);
			newEdges.put(i, edge);
			newNodes.get(n1).addEdge(edge);
			newNodes.get(n2).addEdge(edge);
//			System.out.println("newEdges contains now " + newEdges.get(i) + " as key " + i);
		}
		
		
//		
//		Pass on the new map
	
		System.out.println("CI Map: " + newNodes.size() + " " + newEdges.size());
		try {
			dataString = new TopicMapStorer(newNodes, newEdges).createTopicmapString();
		} catch (TransformerConfigurationException e1) {
			System.out.println("Error CI108 " + e1);
		} catch (IOException e1) {
			System.out.println("Error CI109 " + e1);
		} catch (SAXException e1) {
			System.out.println("Error CI110 " + e1);
		}

		controler.getNSInstance().setInput(dataString, 2);
	}
	
//
//	Accessory for Reading the XML 
	
	private void nest(Node parent, String indent, String topicid) {
		NodeList children = parent.getChildNodes();
		Node child;
		String detail = "";
		String label = "";
		String parentname = parent.getNodeName();
		String owlDetail = "";

		// By entry type
		
		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			String name = child.getNodeName();
			
			// TODO move elsewhere
			if (child.getNodeType() == Node.TEXT_NODE) {
				if (parentname == "rdfs:comment") {
					owlDetail = owlDetail + "<p>" + child.getNodeValue();
					owlDetail = owlDetail.replaceAll("\n"," ");
//					System.out.println(owlDetail);
				}
			}
			if (name == "#text") continue; 
			
			if (child.getNodeType() == Node.ELEMENT_NODE) {
//				System.out.println(indent + ((Element) child).getNodeName());
				parentname = parentname.substring(parentname.indexOf(":")+1);

				NamedNodeMap attrs = child.getAttributes();
				for (int j = 0; j < attrs.getLength(); j++) {
					Node attr = attrs.item(j);
					if (attr.getNodeName() == "rdf:about") {
						String about = attr.getNodeValue();
						int ind = about.indexOf("#");
						if (ind > -1) about = about.substring(ind + 1);
						owlDetail = owlDetail + "<p><b>" + parentname + ": " + about + "</b>";
					}
					if (attr.getNodeName() == "rdf:resource") {
						String about = attr.getNodeValue();
						int ind = about.indexOf("#");
						if (ind > -1) about = about.substring(ind + 1);
						owlDetail = owlDetail + "<p><b>" + child.getNodeName() + ": " + about + "</b>";
					}
//					System.out.println(owlDetail);
				}
				
				// Empty label 
				if (child.getNodeName() == "basename" && child.getChildNodes().getLength() < 1) {
					label = "";
					nodesArray[nodeids.get(topicid)][3] = label;
				}
			}
			
//			//	CDATA_SECTION_NODE or unusual types
//			if (child.getNodeType() != Node.ELEMENT_NODE) {
//				System.out.println(indent + child.getNodeName());
//			} 
			
			//	Main types
			
			if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
//				System.out.println("Text length of " + topicid + ": " + child.getNodeValue().length());
//				System.out.println(topicid + ": " + child.getNodeValue());
				
				//	Label
				if (parent.getNodeName() == "basename") {
					label = child.getNodeValue().toString();
					nodesArray[nodeids.get(topicid)][3] = label;
				} 
				
				//	Detail
				if (parent.getNodeName() == "description" && !isAssoc) {
					detail = child.getNodeValue().toString();
					nodesArray[nodeids.get(topicid)][4] = detail;
				}
				//	Detail if very old (Deepamehta 2) format, TODO refine or remove
				if (parent.getNodeName() == "property" && !isAssoc) {
					detail = child.getNodeValue().toString();
					nodesArray[nodeids.get(topicid)][4] = detail;
				}
				
				//	Ends of edge
				if (parent.getNodeName() == "assocrl") {
					NamedNodeMap assocattrs = parent.getAttributes();
					int whichend = Integer.parseInt(assocattrs.item(0).getNodeValue().substring(8));
					String end = child.getNodeValue();
					if (nodeids.containsKey(end)) {
						edgesArray[edgeids.get(topicid)][whichend - 1] = nodeids.get(child.getNodeValue()).toString();
					} else {	// Corrupted; key -1 will skip GraphEdge creation 
						System.out.println("NS: End node "+ end + " missing");	
						edgesArray[edgeids.get(topicid)][whichend - 1] = "-1";
					}
				}
			}
			String name2 = child.getNodeName();		
			if (name2 != "owl:Restriction") { 	// stub
//					&& name2 != "rdfs:inverseOf" 
//					&& name2 != "rdfs:disjointWith" 
//					&& name2 != "owl:equivalentClass") {
				nest(child, "  " + indent, topicid);
			}
		}
//		return true;
	}

}

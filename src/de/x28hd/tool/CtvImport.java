package de.x28hd.tool;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.sql.Statement; 
import java.util.Hashtable;

import javax.xml.transform.TransformerConfigurationException;

import org.sqlite.SQLiteConfig;
import org.xml.sax.SAXException;

public class CtvImport {
	
	//	Major fields
	String dataString = "";
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	Hashtable<Integer,GraphEdge> edges = new Hashtable<Integer,GraphEdge>();
	
	//	DB related (from sample program)
    private static Connection connection; 
    static { 
        try { 
            Class.forName("org.sqlite.JDBC"); 
        } catch (ClassNotFoundException e) { 
        	System.out.println("Error CVI101 " + e);
        } 
    } 
    String[][] table = {{"ReferenceAuthor", "ReferenceCategory", "ReferenceKeyword",
    	"KnowledgeItemCategory", "KnowledgeItemKeyword"},
    	{"Person", "Category", "Keyword", "Category", "Keyword", "Reference", "KnowledgeItem"}};
    String[][] field = {{"PersonID", "CategoryID", "KeywordID", "CategoryID", "KeywordID"},
		{"LastName", "Name", "Name", "Name", "Name", "ShortTitle", "CoreStatement"}};
    
	//	Keys for nodes and edges, incremented in addNode and addEdge
	Hashtable<String,Integer> inputID2num = new  Hashtable<String,Integer>();
	int j = 0;
	int edgesNum = 0;
	
	//	Constants
	int maxVert = 10;
	GraphPanelControler controler;
	String filename;

	
    public CtvImport(File file, GraphPanelControler controler){ 
    	this.controler = controler;
        try {
        	filename = file.getCanonicalPath();
            } catch (IOException e) {
            	System.out.println("Error CVI102 " + e);
		} 
        initDBConnection(); 
        handleCitaviDB(); 
    } 
     
//
//	Initialize
    
    private void initDBConnection() { 
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true); 
        try { 
            if (connection != null) 
                return; 
            System.out.println("Creating Connection to Database... " + filename); 
            connection = DriverManager.getConnection("jdbc:sqlite:" + filename, config.toProperties()); 
            if (!connection.isClosed()) 
                System.out.println("...Connection established " + connection.toString()); 
        } catch (SQLException e) { 
        	throw new RuntimeException(e); 
        } 

        Runtime.getRuntime().addShutdownHook(new Thread() { 
            public void run() { 
                try { 
                    if (!connection.isClosed() && connection != null) { 
                    	System.out.println("Trying to close Connection to Database");                         connection.close(); 
                        if (connection.isClosed()) 
                            System.out.println("Connection to Database closed"); 
                    } 
                } catch (SQLException e) { 
                	System.out.println("Error CVI103 " + e);
                } 
            } 
        }); 
    } 
    
//
//	Main work
    
    private void handleCitaviDB() { 
        try { 
            Statement stmt = connection.createStatement(); 
            
            //	Collect multiples
            
            for (int w = 0; w < 3; w++) { 
            	String sqlString = 
            		"SELECT " + table[0][w] + "." + field[0][w] + 
            		", Count(\"\") AS Ausdr1" +
            		" FROM " + table[0][w] +
            		" GROUP BY " + table[0][w] + "." + field[0][w] +
            		" HAVING (((Count(\"\"))>1))"; 
            		// (Authors, Categories or Keywords that occur with 
            		//	multiple publications)
            	ResultSet rs = stmt.executeQuery(sqlString);
            	while (rs.next()) { 
            		String id = rs.getString(field[0][w]); 
            		addNode(id, w);
            	}
            	rs.close(); 
            
            	//	Add publications and connect them
            	sqlString = "SELECT " + table[0][w] + "." + field[0][w] + ", " +
            		table[0][w] + ".ReferenceID FROM " + table[0][w] + ";";
            		//	(All Authors, Categories, Keywords of all Publications)
            	rs = stmt.executeQuery(sqlString);
            	while (rs.next()) { 
            		String multID = rs.getString(field[0][w]); 
            		if (!inputID2num.containsKey(multID)) continue;
            		String pubID = rs.getString("ReferenceID");
            		if (!inputID2num.containsKey(pubID)) {
            			addNode(pubID, 5);
            		}
            		addEdge(multID, pubID);
            	}
            	rs.close(); 
            }
            
            //	Process publications that are linked
            Statement stmt2 = connection.createStatement(); 
            String sqlString2 = "SELECT ActiveEntityID, PassiveEntityID FROM EntityLInk;";
        	ResultSet rs2 = stmt2.executeQuery(sqlString2);
        	while (rs2.next()) { 
        		String actID = rs2.getString("ActiveEntityID"); 
        		if (!inputID2num.containsKey(actID)) {
        			addNode(actID, 5);
        		}
        		String passID = rs2.getString("PassiveEntityID"); 
        		if (!inputID2num.containsKey(passID)) {
        			addNode(passID, 5);
        		}
        		addEdge(actID, passID);
        	}
            rs2.close();
            
            //	Process ideas that have categories or keywords
            
            for (int w = 3; w < 5; w++) { 
            	String sqlString = 
            		"SELECT " + table[0][w] + "." + field[0][w] +
            		", " + table[0][w] + ".KnowledgeItemID FROM " + table[0][w] + ";";
            		//	(All Categories and Keywords of all Ideas)
            	ResultSet rs = stmt.executeQuery(sqlString);
            	while (rs.next()) { 
            		String multID = rs.getString(field[0][w]); 
            		if (!inputID2num.containsKey(multID)) {
            			addNode(multID, w);
            		}
            		String ideaID = rs.getString("KnowledgeItemID"); 
            		if (!inputID2num.containsKey(ideaID)) {
            			addNode(ideaID, 6);
            		}
            		addEdge(multID, ideaID);
            	}
            	rs.close(); 
            }
            
            //	Process ideas linking to publications, and all stand-alone ideas
            String sqlString = "SELECT ID, CoreStatement, ReferenceID FROM KnowledgeItem";
        	ResultSet rs = stmt.executeQuery(sqlString);
        	while (rs.next()) { 
        		String ideaID = rs.getString("ID");
        		if (!inputID2num.containsKey(ideaID)) {
        			addNode(ideaID, 6);
        		}
        		String pubID = rs.getString("ReferenceID");
        		if (pubID == null) continue;
        		if (!inputID2num.containsKey(pubID)) {
        			addNode(pubID, 5);
        		}
        		addEdge(ideaID, pubID);
        	}
            
            connection.close(); 
            
        } catch (SQLException e) { 
        	System.out.println("Error CVI104 " + e);
        } 

        
        //	pass on
        try {
        	dataString = new TopicMapStorer(nodes, edges).createTopicmapString();
        } catch (TransformerConfigurationException e1) {
        	System.out.println("Error CVI108 " + e1);
        } catch (IOException e1) {
        	System.out.println("Error CVI109 " + e1);
        } catch (SAXException e1) {
        	System.out.println("Error CVI110 " + e1);
        }
        controler.getNSInstance().setInput(dataString, 2);
    } 
    
//
//	Detail methods
    
	public void addNode(String nodeRef, int tables) { 
		String newNodeColor = "";
		String newLine = "\r";
		String topicName = ""; 
		String verbal = "";
		
        Statement stmt;
		String more = "";
		if (tables == 5) more = ", Reference.Title";
		if (tables == 6) more = ", KnowledgeItem.Text";
		try {
			stmt = connection.createStatement();
			int w = tables;	// which table
			String sqlString = "SELECT " + table[1][w]+ "." + field[1][w] + more +
        		" FROM " + table[1][w] + " WHERE (((" +
        		table[1][w] + ".ID) = \"" + nodeRef + "\"));"; 
			ResultSet rs = stmt.executeQuery(sqlString);
			while (rs.next()) { 
				try {
					String shorty = rs.getString(field[1][w]); 
					if (shorty == null && tables == 5) {
						verbal = rs.getString("Title");
						topicName = verbal;
					} else if (tables == 6) {
						verbal = rs.getString("Text");
						topicName = shorty;
						if (verbal == null) verbal = shorty; 
					} else {
						topicName = shorty;
						verbal = shorty;
					}
				} catch (SQLException e) {
					System.out.println("Error CVI106 " + e);
				} 
			}
		} catch (SQLException e) {
			System.out.println("Error CVI107 " + e);
		} 
        
		if (tables < 6) {
	        newNodeColor = "#ccdddd";
		} else if (tables == 6) {
			newNodeColor = "#ffff99";
		}
		
		int len = topicName.length();
		if (len > 39) {
			len = 30;
			topicName = topicName.substring(0, len);
		} 

		topicName = topicName.replace("\r"," ");
		if (topicName.equals(newLine)) topicName = "";
		if (verbal == null || verbal.equals(newLine)) verbal = "";
		if (topicName.isEmpty() && verbal.isEmpty()) return;
		int id = 100 + j;

		int y = 40 + (j % maxVert) * 50 + (j/maxVert)*5;
		int x = 40 + (j/maxVert) * 150;
		Point p = new Point(x, y);
		GraphNode topic = new GraphNode (id, p, Color.decode(newNodeColor), topicName, verbal);	

		nodes.put(id, topic);
		inputID2num.put(nodeRef, id);
		j++;
	}
	
	public void addEdge(String fromRef, String toRef) {
		GraphEdge edge = null;
		String newEdgeColor = "#c0c0c0";
		edgesNum++;
		GraphNode sourceNode = nodes.get(inputID2num.get(fromRef));
		GraphNode targetNode = nodes.get(inputID2num.get(toRef));
		edge = new GraphEdge(edgesNum, sourceNode, targetNode, Color.decode(newEdgeColor), "");
		edges.put(edgesNum, edge);
	}

}

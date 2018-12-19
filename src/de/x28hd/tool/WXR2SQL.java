package de.x28hd.tool;

import java.awt.FileDialog;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class WXR2SQL {
	String filename = "";
	String dirname = "";
	FileInputStream in = null;
	FileWriter list = null;
	boolean item = false;
	boolean title = false;
	boolean postType = false;
	boolean excerpt = false;
	boolean category = false;
	boolean content = false;
	boolean postDate = false;
	boolean post = true;
	int itemNum = 0;
	Hashtable<Integer,String> titles = new Hashtable<Integer,String>();
	Hashtable<Integer,String> excerpts = new Hashtable<Integer,String>();
	Hashtable<Integer,String> categories = new Hashtable<Integer,String>();
	Hashtable<Integer,String> dates = new Hashtable<Integer,String>();
	Hashtable<Integer,String> contents = new Hashtable<Integer,String>();
	TreeMap<String,Integer> datesMap = new TreeMap<String,Integer>();
	HashSet<String> catSet = new HashSet<String>();
	
	public  WXR2SQL(JFrame mainWindow) {
		
		String msg = "Use at your own risk.\n\n"
				+ "It does not copy attachments, comments etc.;\n"
				+ "It drops the tables x28text and x28cat if exist.";
		JOptionPane.showMessageDialog(mainWindow, msg);
		
		// Get XML file
		
		String fs = System.getProperty("file.separator");
		FileDialog fd = new FileDialog(mainWindow);
		fd.setVisible(true);
		dirname = fd.getDirectory();
		filename = dirname + fs + fd.getFile();
		
		File wxrFile = new File(filename);
		try {
			in = new FileInputStream(wxrFile);
		} catch (FileNotFoundException e) {
			System.out.println("Error WX101 " + e);
			return;
		}
		
		// Parse (i.e., let the methods of MyHandler be called)
		
		SAXParser parser = null;
		try {
			parser = SAXParserFactory.newInstance().newSAXParser();
		} catch (ParserConfigurationException e) {
			System.out.println("Error WX105 " + e);
			return;
		} catch (SAXException e) {
			System.out.println("Error WX106 " + e);
			return;
		}
		DefaultHandler handler = new MyHandler();
		try {
			parser.parse(in, handler);
		} catch (SAXException e) {
			System.out.println("Error WX107 " + e);
			return;
		} catch (IOException e) {
			System.out.println("Error WX108 " + e);
			return;
		}
		
		closeReader();
		
		// Output
		
		filename = dirname + fs + "grsshopper_x28text.sql";
		try {
			list = new FileWriter(filename);
		} catch (IOException e) {
			System.out.println("Error WX109 " + e);
			return;
		}
		
		SortedMap<String,Integer> datesList = (SortedMap) datesMap; 
		SortedSet<String> datesSet = (SortedSet) datesList.keySet();
		Iterator<String> it = datesSet.iterator();

		String valueString = "";
		int recordNum = 1;
		while (it.hasNext()) {
			String dateString = it.next();
			int key = datesList.get(dateString);
			String excerptString = excerpts.get(key);
			if (excerptString == null) excerptString ="(no excerpt)";
			String contentString = "<![CDATA[" + contents.get(key) + "]]>";
			String catString = categories.get(key);
			if (catString == null) catString ="Uncategorized";
			String titleString = titles.get(key);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = null;
			long epoch = 0L;
			try {
				date = sdf.parse(dateString);
				epoch = date.getTime()/1000;
			} catch (ParseException e) {
				System.out.println("Error WX113 " + e);
				return;
			}

			if (recordNum > 1) valueString += ",";
			valueString += "(" + recordNum + ",'" + epoch + "',NULL,'" + catString + "','" 
			+ titleString + "','" + excerptString + "','" + contentString + "')";
			recordNum++;
		}
		
		try {
			list.write("DROP TABLE IF EXISTS `x28text`;\n" +
					"CREATE TABLE `x28text` (\n" +
					"  `x28text_id` int(11) NOT NULL AUTO_INCREMENT,\n" +
					"  `x28text_crdate` int(15) DEFAULT NULL,\n" +
					"  `x28text_creator` int(15) DEFAULT NULL,\n" +
					"  `x28text_cat` text,\n" +
					"  `x28text_title` text,\n" +
					"  `x28text_excerpt` text,\n" +
					"  `x28text_content` text,\n" +
					"  PRIMARY KEY (`x28text_id`)\n" +
					") ENGINE=MyISAM AUTO_INCREMENT=1526 DEFAULT CHARSET=utf8;\n" +
					"LOCK TABLES `x28text` WRITE;\n");
			list.write("INSERT INTO x28text VALUES " + valueString + ";\n");
			list.write("UNLOCK TABLES;\n");
		} catch (IOException e) {
			System.out.println("Error WX110 " + e);
			return;
		}
		closeWriter();
		
		// Output 2
		
		filename = dirname + fs + "grsshopper_x28cat.sql";
		try {
			list = new FileWriter(filename);
		} catch (IOException e) {
			System.out.println("Error WX109a " + e);
			return;
		}
		
		Iterator<String> it2 = catSet.iterator();

		valueString = "";
		recordNum = 1;
		while (it2.hasNext()) {
			String catString = it2.next();
			if (recordNum > 1) valueString += ",";
			valueString += "(" + recordNum + ",NULL,NULL,'" + catString + "')";
			recordNum++;
		}
		try {
			list.write("DROP TABLE IF EXISTS `x28cat`;\n" +
					"CREATE TABLE `x28cat` (\n" +
					"  `x28cat_id` int(11) NOT NULL AUTO_INCREMENT,\n" +
					"  `x28cat_crdate` int(15) DEFAULT NULL,\n" +
					"  `x28cat_creator` int(15) DEFAULT NULL,\n" +
					"  `x28cat_cat` text,\n" +
					"  PRIMARY KEY (`x28cat_id`)\n" +
					") ENGINE=MyISAM AUTO_INCREMENT=1526 DEFAULT CHARSET=utf8;\n" +
					"LOCK TABLES `x28cat` WRITE;\n");
			list.write("INSERT INTO x28cat VALUES " + valueString + ";\n");
			list.write("UNLOCK TABLES;\n");
		} catch (IOException e) {
			System.out.println("Error WX110 " + e);
			return;
		}
		closeWriter();
		System.exit(0);
	}
	
	public void closeReader() {
		try {
			in.close();
		} catch (IOException e) {
			System.out.println("Error WX111 " + e);
			return;
		}
	}
	
	public void closeWriter() {
		try {
			list.close();
		} catch (IOException e) {
			System.out.println("Error WX112 " + e);
			return;
		}
	}
	
	public class MyHandler extends DefaultHandler {
		public void startDocument() {
			System.out.println("Start");
		}
		public void endDocument() {
			System.out.println("End");
		}
		public void startElement(String uri, String localName, 
				String qName, Attributes attributes) {
			if (qName.equals("item")) {
				item = true;
				itemNum++;
			} else if (item && qName.equals("wp:post_type")) {
				postType = true;
			} else if (item && qName.equals("title")) {
				title = true;
			} else if (item && qName.equals("excerpt:encoded")) {
				excerpt = true;
			} else if (qName.equals("category")) {
				category = true;
			} else if (qName.equals("wp:post_date")) {
				postDate = true;
			} else if (qName.equals("content:encoded")) {
				content = true;
			}
		}
		
		public void endElement(String uri, String localName, String qName) {
			if (qName.equals("item")) {
				item = false;
			} else if (qName.equals("wp:post_type")) {
				postType = false;
			} else if (qName.equals("title")) {
				title = false;
			} else if (qName.equals("excerpt:encoded")) {
				excerpt = false;
			} else if (qName.equals("category")) {
				category = false;
			} else if (qName.equals("wp:post_date")) {
				postDate = false;
			} else if (qName.equals("content:encoded")) {
				content = false;
			}
		}
		public void characters(char[] ch, int start, int length) {
			if (postType) {
				String type = new String(ch, start, length);
				post = (type.equals("post"));
			} else if (title && post) {
				String titleString = new String(ch, start, length);
				titleString = exotReplace(titleString);
				titleString = titleString.replaceAll("'", "''");
				titles.put(itemNum, titleString);
			} else if (excerpt && post) {
				String excerptString = new String(ch, start, length).trim();
				excerptString = excerptString.replaceAll("\n", "<br />");
				excerptString = exotReplace(excerptString);
				excerptString = excerptString.replaceAll("'", "''");
				excerpts.put(itemNum, excerptString);
			} else if (category && post) {
				String catString = new String(ch, start, length).trim();
				categories.put(itemNum, catString);
				catSet.add(catString);
			} else if (postDate && post) {
				String dateString = new String(ch, start, length);
				dates.put(itemNum, dateString);
				datesMap.put(dateString, itemNum);
			} else if (content && post) {
				String contentString = new String(ch, start, length);
				contentString = contentString.replaceAll("'", "''");
				contentString = contentString.replaceAll("\n", "<p>");
				contents.put(itemNum, contentString);
			}
		}
		
		// No idea what this is
		public void processingInstruction(String target, String data) {
			System.out.println("processingInstruction: " + target + " " + data);
		}
		public void ignorableWhitespace(char[] ch, int start, int length) {
			System.out.println("ignorableWhitespace: " + new String(ch, start, length));
		}
	}
	
	public String exotReplace(String string) {
		string = string.replaceAll("\u201c", "\"");	// left double quotation mark
		string = string.replaceAll("\u201d", "\"");	// right double quotation mark
		string = string.replaceAll("\u2019", "'");	// right single quotation mark
		string = string.replaceAll("\u2026", "..."); // horizontal ellipsis
		return string;
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		new WXR2SQL(frame);
	}
}

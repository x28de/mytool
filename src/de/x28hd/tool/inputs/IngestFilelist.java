package de.x28hd.tool.inputs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.Utilities;

//
//	Get the most out of file lists

public class IngestFilelist {
	String dataString;
	PresentationService controler;
	
	public IngestFilelist(String dataString, PresentationService controler, 
			boolean withDates) {
		this.dataString = dataString;
		this.controler = controler;
		boolean dropEncoding = controler.getNSInstance().dropEncoding;
		
		InputStream stream = null;
		Utilities utilities = new Utilities();
		boolean windows = (System.getProperty("os.name").startsWith("Windows"));
		//	Input file sorting
		Hashtable<Integer,String> byModDates = new Hashtable<Integer,String>();;
		TreeMap<Long,Integer> datesMap = new TreeMap<Long,Integer>();
		SortedMap<Long,Integer> datesList = (SortedMap<Long,Integer>) datesMap;
		
		// Sort files by modDate
		if (withDates) {
 	    	String textStr[] = dataString.split("\\r?\\n");
			byModDates.clear();
 			datesMap.clear();
 			int fileCount = 0;
 			for (int i = 0; i < textStr.length; i++) {
 				String line = textStr[i];
 				File f = new File(line);
 				if (!f.exists()) continue;
 				long modDate = f.lastModified();
 				while (datesMap.containsKey(modDate)) modDate++;
 				datesMap.put(modDate, fileCount);
 				byModDates.put(fileCount, line);
 				fileCount++;
 			}
 			dataString = "";
 			SortedSet<Long> datesSet = (SortedSet<Long>) datesList.keySet();
 			Iterator<Long> ixit = datesSet.iterator(); 
 			if (datesSet.size() > 0) {
 				while (ixit.hasNext()) {
 					Long modDate = ixit.next();
 					Integer fileIndex = datesList.get(modDate);
 					String fn = byModDates.get(fileIndex);
 					dataString = dataString + fn + "\r\n";
 				}
 			}
 		}
		
		//	Process simple files
	    //	(Try to make filenames clickable, or even include their content)
    	String output = "";
    	String[] textStr = dataString.split("\\r?\\n");
    	for (int i = 0; i < textStr.length; i++) {
        	String shortName = "";
    		boolean success = false;
    		String contentString = "";
    		String line = textStr[i];
  			File f = new File(line);
  			
     		if (!withDates || !f.exists() || f.isDirectory()) {
     			//	Just list the filename
     			//	TODO replace by utility (test with Mac)
				shortName = line.replace('\\', '/');	
				if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
    			line = shortName + "\t" + line;
    			output = output + line + "\r\n";
     		} else {
    			shortName = f.getName();
					if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
	       			shortName = shortName.substring(shortName.lastIndexOf("/") + 1);
    			int extOffset = shortName.lastIndexOf(".");
    			String extension = "";
    			if (extOffset > 0) extension = shortName.substring(extOffset);

    			if (!extension.equals(".txt") && !extension.equals(".htm")) {
    				line = f.toURI().toString();
    				if (windows) line = line.replace("%C2%A0", "%A0"); // For bookmarks containing nbsp
    				line = shortName + "\t" + "<html><body>Open file <a href=\"" + line  + "\">" + shortName + "</a></body></html>";
    				output = output + line + "\r\n";

    			} else {

    				try {
    					stream = new FileInputStream(line);
    				} catch (FileNotFoundException e) {
    					System.out.println("Error NS123 " + e);
    					success = false;
    				}
    				try {
    					InputStream in = (InputStream) stream;
    					if (windows) {
    						if (dropEncoding) {
    							contentString = utilities.convertStreamToString(in, Charset.forName("UTF-8"));
    						} else {
        					contentString = utilities.convertStreamToString(in, Charset.forName("Cp1252"));
    						}
						} else {
        					contentString = utilities.convertStreamToString(in);
    					}
    					in.close();
    					success = true;
    				} catch (IOException e1) {
    					System.out.println("Error NS116 " + e1);
    					success = false;
    				}
    				if (success) {
    					contentString = contentString.replaceAll("\\r?\\n", "<br />");
    					contentString = contentString.replace("\t", " (TAB) ");  // TODO improve
    					line = shortName + "\t" + contentString;
    					output = output + line + "\r\n";
    				}
    			} 
     		}
			dataString = output;
		}

		new IngestItemlists(dataString, controler, false);
		
	}
}

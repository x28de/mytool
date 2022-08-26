package de.x28hd.tool.importers;

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

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.Utilities;
import de.x28hd.tool.core.GraphNode;

public class EnwImport {
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	String dataString = "";
	int maxVert = 10;
	int j = 0;
	String newNodeColor = "#ccdddd";
	
	public EnwImport (File file, PresentationService controler) {
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error ENW101 " + e);
		}
		Utilities utilities = new Utilities();
		String linesString = utilities.convertStreamToString(fileInputStream);		
		String [] lines = linesString.split("\\r?\\n");	
		int separator = 0; 
		String out = "";
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.isEmpty()) {
				separator++;
				if (separator < 3) {
					out = out + "\n";
					dataString = dataString + out;
					separator = 0;
					out = "";
				}
			} else out = out + line + "<br />";
		}
		out = out + "\n";
		dataString = dataString + out;
		controler.getNSInstance().setInput(dataString, 6);
	}
}

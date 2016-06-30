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

public class EnwImport {
	
	Hashtable<Integer,GraphNode> nodes = new Hashtable<Integer,GraphNode>();
	String dataString = "";
	int maxVert = 10;
	int j = 0;
	String newNodeColor = "#ccdddd";
	
	public EnwImport (File file, GraphPanelControler controler) {
		System.out.println("Endnote");
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error ENW101 " + e);
		}
		String linesString = convertStreamToString(fileInputStream);		
		String [] lines = linesString.split("\\r?\\n");	
		int separator = 0; 
		String out = "";
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			System.out.println(line);
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
		controler.getNSInstance().setInput(dataString, 6);
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
    			System.out.println("Error IM117 " + e);
    			try {
    				writer.close();
    			} catch (IOException e1) {
    				System.out.println("Error IM118 " + e1);
    			}
    		} finally {
    			try {
    				is.close();
    			} catch (IOException e) {
    				System.out.println("Error IM119 " + e);
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

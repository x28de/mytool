package de.x28hd.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.StyledEditorKit;

public class Utilities {
	
//
//	Not much yet, will grow during consolidation
	
	static public String getShortname(String longFilename) {
		String shortName = longFilename.replace('\\', '/');	
		if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
		shortName = shortName.substring(shortName.lastIndexOf("/") + 1);
		return shortName;
	}

	//
//	Context menu

	public static JPopupMenu showContextMenu() {
		JPopupMenu menu = new JPopupMenu();

		Action cutAction = new StyledEditorKit.CutAction();
		String cutActionCommand = (String) cutAction.getValue(Action.ACTION_COMMAND_KEY);	
		JMenuItem cutItem = new JMenuItem();
		cutItem.setActionCommand(cutActionCommand);
		cutItem.addActionListener(cutAction);
		cutItem.setText("Cut");
		menu.add(cutItem);

		Action copyAction = new StyledEditorKit.CopyAction();
		String copyActionCommand = (String) copyAction.getValue(Action.ACTION_COMMAND_KEY);	
		JMenuItem copyItem = new JMenuItem();
		copyItem.setActionCommand(copyActionCommand);
		copyItem.addActionListener(copyAction);
		copyItem.setText("Copy");
		menu.add(copyItem);

		Action pasteAction = new StyledEditorKit.PasteAction();
		String pasteActionCommand = (String) pasteAction.getValue(Action.ACTION_COMMAND_KEY);	
		JMenuItem pasteItem = new JMenuItem();
		pasteItem.setActionCommand(pasteActionCommand);
		pasteItem.addActionListener(pasteAction);
		pasteItem.setText("Paste");
		menu.add(pasteItem);

		return menu;
	}
	
	public static void displayLayoutWarning(GraphPanelControler controler, boolean recolor) {
		String msg = "<html>After the Auto-Layout, the map may appear empty.<br>"
				+ "Then zoom out via <b>Advanced > Zoom the map...</b> to pan.";
		if (recolor) msg = msg + "<br>To undo the recoloring, uncheck <b>Advanced > "
				+ "<br>Layout > Centrality Heatmap</b>";
		msg = msg + "</html>";
		controler.displayPopup(msg);
	}
	
	//	Accessories
	
	public String convertStreamToString(InputStream is, Charset charset) {
    	
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
//					new InputStreamReader(is, "UTF-8"));
// 					changed to allow windows's exotic Charset.defaultCharset() for simple files
   					new InputStreamReader(is, charset));	

    		int n;
    		try {
    			while ((n = reader.read(buffer)) != -1) {
    				writer.write(buffer, 0, n);
    			}
    		} catch (IOException e) {
    			System.out.println("Error NS117 " + e);
    			try {
    				writer.close();
    			} catch (IOException e1) {
    				System.out.println("Error NS118 " + e1);
    			}
    		} finally {
    			try {
    				is.close();
    			} catch (IOException e) {
    				System.out.println("Error NS119 " + e);
    			}
    		}
    		String convertedString = writer.toString();
    		return convertedString;
    	} else {        
    		return "";
    	}
    }
    public String convertStreamToString(InputStream is) {
    	return convertStreamToString(is, Charset.forName("UTF-8"));
    }
    	
	
}

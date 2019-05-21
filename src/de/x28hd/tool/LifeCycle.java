package de.x28hd.tool;

import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFrame;

public class LifeCycle {
	
//
//	(About loading and saving)
	
	String filename = "";
	boolean dirty = false;
	boolean loaded = false;
	String mainWindowTitle = "Main Window";
	
	// Added later:
	GraphPanelControler controler;
	JFrame mainWindow;
	String baseDir;
	String confirmedFilename = "";
	String lastHTMLFilename = "";
	


	public LifeCycle() {
		//	more parameters added later by add() 
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
		if (!filename.isEmpty()) mainWindowTitle = Utilities.getShortname(filename);
	}
	public String getFilename() {
		return filename;
	}
	public String getMainWindowTitle() {
		return mainWindowTitle;
	}
	
	public void setDirty(boolean toggle) {
		dirty = toggle;
	}
	public boolean isDirty() {
		return dirty;
	}

	public void setLoaded(boolean toggle) {
		loaded = toggle;
	}
	public boolean isLoaded() {
		return loaded;
	}
	
	
	public void add(GraphPanelControler controler, String baseDir) {
		this.controler = controler;
		this.baseDir = baseDir;
		mainWindow = controler.getMainWindow();
	}
	
	public void resetFilename(String filename) {
		setFilename(filename);
		if (filename.isEmpty()) return;
		mainWindowTitle = Utilities.getShortname(filename);
		mainWindow.setTitle(mainWindowTitle);
	}

	public void setConfirmedFilename(String confirmedFilename) {
		this.confirmedFilename = confirmedFilename;
	}
	public String getConfirmedFilename() {
		return confirmedFilename;
	}
	public String getLastHTMLFilename() {
		return lastHTMLFilename;
	}
	public boolean askForFilename(String extension) {
		FileDialog fd = new FileDialog(mainWindow, "Specify filename", FileDialog.SAVE);
		String newName; 
		int offs = filename.lastIndexOf(".");
		if (filename.isEmpty() || offs < 0) {
			newName = baseDir + File.separator + "storefile." + extension;
		} else if (!filename.substring(offs + 1).equals(extension)) {
			newName = filename.substring(0, offs) + "." + extension;
		} else {
			newName = filename + "." + extension;  // don't suggest to overwrite
		} 
		if (System.getProperty("os.name").equals("Mac OS X")) {
			
			newName = newName.substring(newName.lastIndexOf(File.separator) + 1);
		}
		fd.setFile(newName);
		fd.setDirectory(baseDir);
		fd.setVisible(true);
		if (fd.getFile() == null) {	//	File dialog aborted.");
			return false; 
		}
		newName = fd.getDirectory() + fd.getFile();
		if (extension == "xml") {
			confirmedFilename = newName;
			resetFilename(newName);
			mainWindow.repaint();
		} else if (extension == "htm") {
			lastHTMLFilename = newName;
		} else {
			System.out.println("Error PS121b");
			return false;
		}
		return true;
	}
}

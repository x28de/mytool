package de.x28hd.tool;

import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

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
			if (!newName.endsWith(".xml")) newName += ".xml";  // TODO more elegant with JFileChooser ?
			confirmedFilename = newName;
			resetFilename(newName);
			mainWindow.repaint();
		} else if (extension == "htm") {
			lastHTMLFilename = newName;
		} else {
			System.out.println("Error LC121b");
			return false;
		}
		return true;
	}
	
	public String askForLocation(String suggestion) {
		String storeFilename = "";
		FileDialog fd = new FileDialog(mainWindow, "Specify filename", FileDialog.SAVE);
		fd.setFile(suggestion); 
		fd.setVisible(true);
		if (fd.getFile() != null) {
			storeFilename = fd.getFile();
			storeFilename = fd.getDirectory() + fd.getFile();
		}
		return storeFilename;
	}
	
	public void save() {
		if (confirmedFilename.isEmpty()) {
			if (askForFilename("xml")) {
				if (controler.startStoring(confirmedFilename, false)) {
					setDirty(false);
				}
			}
		} else {
			if (controler.startStoring(confirmedFilename, false)) {
				setDirty(false);
			}
		}
	}
	public void saveAs() {
		if (askForFilename("xml")) {
			if (controler.startStoring(confirmedFilename, false)) {
				setDirty(false);
			}
		}
	}
	
	public String open() {
		FileDialog fd = new FileDialog(mainWindow);
		fd.setMode(FileDialog.LOAD);
		fd.setDirectory(baseDir);
		fd.setVisible(true);
		return fd.getDirectory() + fd.getFile();
	}
	
	public boolean close() {
		Object[] closeOptions =  {"Save", "Discard changes", "Cancel"};
		int closeResponse = JOptionPane.YES_OPTION;
		if (isDirty()) {
			closeResponse = JOptionPane.showOptionDialog(null,
					"Do you want to save your changes?\n",
					"Warning", JOptionPane.YES_NO_CANCEL_OPTION, 
					JOptionPane.WARNING_MESSAGE, null, 
					closeOptions, closeOptions[0]);  
			if (closeResponse == JOptionPane.CANCEL_OPTION ||
					closeResponse == JOptionPane.CLOSED_OPTION) {
				return false;
			} else if (closeResponse != JOptionPane.NO_OPTION) {
				if (getConfirmedFilename().isEmpty()) {
					if (askForFilename("xml")) {
						if (!controler.startStoring(getConfirmedFilename(), false)) {
							return false;
						}
					} else {
						return false;
					}
				} else {
					if (!controler.startStoring(getConfirmedFilename(), false)) {
						return false;
					}
				}
			}
		}
		mainWindow.dispose();
		System.out.println("LC: Closed");
		System.exit(0);
		return true;
	}
}

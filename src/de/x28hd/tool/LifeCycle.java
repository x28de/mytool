package de.x28hd.tool;

import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFrame;

public class LifeCycle {
	boolean dirty = false;
	boolean loaded = false;
	GraphPanelControler controler;
	String baseDir;
	
	public LifeCycle(GraphPanelControler controler, String baseDir) {
		this.controler = controler;
		this.baseDir = baseDir;
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
}

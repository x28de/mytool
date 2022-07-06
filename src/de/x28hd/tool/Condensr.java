package de.x28hd.tool;

import javax.swing.JApplet;

// using components of de.deepamehta under GPL

public class Condensr extends JApplet {
	private static final long serialVersionUID = 1L;

	//	is the main class for DMG build, otherwise this is still MyTool
	
	public static void main(String[] args) {
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MyTool");
		new Condensr().initApplication(args);
	}
	
	private void initApplication(String[] args) {
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MyTool");
		try {
			GraphPanelControler ps = new GraphPanelControler(false);
			new Thread(ps).start();
			if (args.length >0) ps.setFilename(args[0], 0);
		} catch (Throwable e) {
			System.out.println("Error initApplication " + e);
			e.printStackTrace();
		}
	}
}

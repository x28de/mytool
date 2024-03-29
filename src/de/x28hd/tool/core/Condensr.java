package de.x28hd.tool.core;

import java.awt.Panel;

import de.x28hd.tool.PresentationService;

// using components of de.deepamehta under GPL

public class Condensr extends Panel {
	private static final long serialVersionUID = 1L;

	//	is the main class for DMG build, otherwise this is still MyTool
	
	public static void main(String[] args) {
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MyTool");
		new Condensr().initApplication(args);
	}
	
	private void initApplication(String[] args) {
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MyTool");
		try {
			PresentationService ps = new PresentationService(false);
//			PresentationCore ps = new PresentationCore();
			new Thread(ps).start();
			if (args.length >0) ps.setFilename(args[0], 0);
		} catch (Throwable e) {
			System.out.println("Error initApplication " + e);
			e.printStackTrace();
		}
	}
}

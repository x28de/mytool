package de.x28hd.tool;

public class AboutBuild {
	
	public String about =  " ******** Provisional BANNER ********* " +
			"\r\n This is My Tool, Release 35 Build 1" + 
			"\r\n running on Java version " + System.getProperty("java.version") +
			"\r\n on " + System.getProperty("os.name") + " version " + System.getProperty("os.version") +
			" (os.arch = " + System.getProperty("os.arch") + ")" +
			"\r\n using components of de.deepamehta " + 
			"\r\n and edu.uci.ics.jung under GPL" +
			"\r\n ******** Provisional BANNER ********* ";
	
	public String getAbout() {
		return about;
	}
}

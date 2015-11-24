package de.x28hd;

public class AboutBuild {
	public String about =  " ******** Provisional BANNER ********* " +
			"\r\n This is My Tool, Release 18 Build 6" + 
			"\r\n running on Java version " + System.getProperty("java.version") +
			"\r\n using components of de.deepamehta " + 
			"\r\n and edu.uci.ics.jung under GPL" +
			"\r\n ******** Provisional BANNER ********* ";
	
	public String getAbout() {
		return about;
	}
}

package de.x28hd.tool;

public class AboutBuild {
	String extIndicator = "Basic";
	public String about;
	
	public AboutBuild(boolean extended) {
		if (extended) extIndicator = "Extended";
	
	about =  " ******** Provisional BANNER ********* " +
			"\r\n " + 
			"\r\n This is Condensr Release 44 Build 4 " + extIndicator +
			"\r\n running on Java version " + System.getProperty("java.version") +
			"\r\n on " + System.getProperty("os.name") + " version " + System.getProperty("os.version") +
			" (os.arch = " + System.getProperty("os.arch") + ")" +
			"\r\n file.encoding = " + System.getProperty("file.encoding") +
			"\r\n using components of de.deepamehta, " + 
			"\r\n edu.uci.ics.jung and org.sqlite under GPL" +
			"\r\n " + 
			"\r\n ******** Provisional BANNER ********* ";
	}
	
	public String getAbout() {
		return about;
	}
	
}

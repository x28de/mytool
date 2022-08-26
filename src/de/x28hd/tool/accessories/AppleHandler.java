package de.x28hd.tool.accessories;

import javax.swing.JOptionPane;

import com.apple.eawt.*;
import com.apple.eawt.AppEvent.AppReOpenedEvent;

import de.x28hd.tool.PresentationService;

public class AppleHandler implements com.apple.eawt.OpenFilesHandler, com.apple.eawt.AboutHandler, com.apple.eawt.PreferencesHandler, com.apple.eawt.QuitHandler,
com.apple.eawt.OpenURIHandler, AppReOpenedListener {

	com.apple.eawt.Application app = com.apple.eawt.Application.getApplication();
	PresentationService ps;

	public AppleHandler(PresentationService ps) {
		app.setAboutHandler(this);
		app.setPreferencesHandler(this);
		app.setQuitHandler(this);
		app.setOpenFileHandler(this);
		app.setOpenURIHandler(this);
		app.disableSuddenTermination();    // no effect, since QuitStrategy is gone and QuitHandler must be used

		this.ps = ps;
	}

public void displayPopup(String msg) {
	JOptionPane.showMessageDialog(ps.mainWindow,msg);
}


public void openFiles(com.apple.eawt.AppEvent.OpenFilesEvent ofe) {
	displayPopup("Open Files Event");    // no effect   
}

public void handleAbout(com.apple.eawt.AppEvent.AboutEvent ae) {
	displayPopup(ps.about);
}

public void handlePreferences(com.apple.eawt.AppEvent.PreferencesEvent pe) {
	displayPopup(ps.preferences);
}

public void handleQuitRequestWith(com.apple.eawt.AppEvent.QuitEvent qe, com.apple.eawt.QuitResponse qr) {
	
	boolean closeOK = ps.close();
    		if (!closeOK) {
    			qr.cancelQuit();
    		} else {
    			qr.performQuit();
    		}
}

public void openURI(com.apple.eawt.AppEvent.OpenURIEvent oue) {
	displayPopup("OpenURIEvent");		// no effect
}

public void appReOpened(AppReOpenedEvent arg0) {
		displayPopup("BANNER \r\n My Tool, Release 17a \r\n is starting...");  // futile
	}
	
}
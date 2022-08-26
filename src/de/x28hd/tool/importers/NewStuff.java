package de.x28hd.tool.importers;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;

import javax.swing.TransferHandler;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.PresentationExtras;
import de.x28hd.tool.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

public class NewStuff {
	
	
	//	Main fields
	Hashtable<Integer, GraphNode> newNodes = new Hashtable<Integer, GraphNode>();
	Hashtable<Integer, GraphEdge> newEdges = new Hashtable<Integer, GraphEdge>();
	Point dropLocation = null;
	
	//	About caller (PresentationService()) and CompositionWindow()
	private PresentationService controler;
	PresentationExtras controlerExtras;
	boolean compositionMode = false;
	
	//	Input
	public static DataFlavor htmlSelectionFlavor;
	String advisableFilename = "";
	
	//	Map loading
	boolean existingMap = false;
	Rectangle bounds = new Rectangle(2, 2, 2, 2);
	
	//	HTML exploitation
	boolean parseMode = false; 
	boolean dropEncoding = true;
	
	String dataStringResort = "";

	
	public NewStuff(final PresentationService controler) {
		this.controler = controler;
	}
	
//
//	Handlers for dropped and pasted stuff
	
//	canImport and importData are called by drop from GraphPanel and CompositionWindow,
//	readClipboard is called from Paste operations in PresentationExtras and 
//	CompositionWindow. 
	
	public boolean canImport(TransferHandler.TransferSupport support, String diag) {
		support.setDropAction(TransferHandler.COPY);
//        support.setShowDropLocation(true);    // no effect ?
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) && 
			!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
//			System.out.println("NS dataflavors: " + support.getDataFlavors() + " " + support.getDataFlavors().length);
			DataFlavor[] df = support.getDataFlavors();
			for (int i = 0; i < df.length; i++) {
				System.out.println("NS dataflavors: " + df[i].getHumanPresentableName());
			}
        	return false;
        }
//		System.out.println("NS canImport via " + diag);
		return true;
	}
	
	public boolean importData(TransferHandler.TransferSupport support, String diag) {
   	 
        if (!canImport(support, diag)) {
           return false;
        }

        Transferable t = support.getTransferable();
		dropLocation = new Point(support.getDropLocation().getDropPoint().x,
				support.getDropLocation().getDropPoint().y);
//		System.out.println("NS Drop point " + support.getDropLocation().getDropPoint().x + ", " +
//				support.getDropLocation().getDropPoint().y);
       
        boolean success = transferTransferable(t);
		if (!success) {
			System.out.println("Error NS120.");
		}
        return success;
	}

	public Transferable readClipboard() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		dropLocation = null;
		return clipboard.getContents(null); 
	}

//
//	Obtain input in 3 different dataFlavors (file list, HTML, or string)
	
//	Then, 4 major ingest classes contribute to the result (preliminary names):
//	InterceptZips - brute force test if zipped, and known types auto-detected
//	ExploitFilelist - multiple files of html or plain text or just their names
//	AnalyzeBlob - brute force test if XML, and known types auto-detected, most notably ready map snippets
//	Step2b - splitting a string and optionally parsing HTML
//
//	Step3a calls triggerUpdate in the controler which fetches the new nodes & edges from the assembly 
	
// (Previous description involved inputTypes)
//	Then determine an inputType of the following:
//	1 = file list, single file
//	2 = string (including complete x28maps for dragging or from importers)
//	3 = HTML string
//	4 = file list, multiple files
//	and later:
//	5 = file list, multiple files, from ZIP
//	6 = pre-processed list (fit for Composition Window)
	
	@SuppressWarnings("unchecked")
	public boolean transferTransferable(Transferable content) {
		String dataString = "";			
		Utilities utilities = new Utilities();
		
		//  File(s) ?

		if (content.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			List<File> l = null;
			try {
				l = (java.util.List<File>) content.getTransferData(DataFlavor.javaFileListFlavor);
			} catch (UnsupportedFlavorException e1) {
				System.out.println("Error NS109 " + e1);
				return false;
			} catch (IOException e1) {
				System.out.println("Error NS110 " + e1);
				return false;
			}
			if (l.size() == 1) {
				dataString = l.get(0).getAbsolutePath();
//				System.out.println("NS: was javaFileListFlavor, 1 file");
				
				controler.getNSInstance().setAdvisableFilename(dataString);
				File file = l.get(0);
				new InterceptZips(file, controler);
			} else {
				for (File f : l) {
					String fn = f.getAbsolutePath();
					dataString = dataString + "\r\n" + fn;
				}
//				System.out.println("NS: was javaFileListFlavor, > 1 files");
				new ExploitFilelist(dataString, controler, true);
			}
			return true;
		}

		//  HTML String?

		if (parseMode && content.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor)) {
			try {
				dataString = (String) content.getTransferData(DataFlavor.fragmentHtmlFlavor);
				dataStringResort = (String) content.getTransferData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException e1) {
				System.out.println("Error NS112a " + e1);
				return false;
			} catch (IOException e1) {
				System.out.println("Error NS113a " + e1);
				return false;
			}
//			System.out.println("NS: was fragmentHtmlFlavor: \r\n");
			new Step2b(dataString, controler, true);
			return true;
		} else {
		try {
			htmlSelectionFlavor = new DataFlavor("text/html;charset=utf-8;class=java.io.InputStream");
			// "unicode" was wrong: yielded space after each character, 
			// and HTMLEditorKit.ParserCallback() recognized nothing but body p-implied
			
			// Update June 2016: still works from Eclipse but not from Jar. Trying to
			// keep it until advice may come along
		} catch (ClassNotFoundException cle) {
			System.out.println("Error NS111 " + cle);
			return false;
		}
		if (content.isDataFlavorSupported(htmlSelectionFlavor)) {
			try {
				InputStream in = (InputStream) content.getTransferData(htmlSelectionFlavor);
				dataString = utilities.convertStreamToString(in);
				in.close();
				dataStringResort = (String) content.getTransferData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException e1) {
				System.out.println("Error NS112 " + e1);
				return false;
			} catch (IOException e1) {
				System.out.println("Error NS113 " + e1);
				return false;
			}
//			System.out.println("NS: was htmlSelectionFlavor: \r\n");
			new Step2b(dataString, controler, true);
			return true;
		} 
		}

		//	Plain string ?

		if (content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				dataString = (String) content.getTransferData(DataFlavor.stringFlavor);
//				System.out.println("NS: was stringFlavor: \r\n");
			} catch (UnsupportedFlavorException e) {
				System.out.println("Error NS114 " + e);
				return false;
			} catch (IOException e) {
				System.out.println("Error NS115 " + e);
				return false;
			}

			new AnalyzeBlob(dataString, controler);
			return true;

		// Nothing ?
			
		} else {
			controler.displayPopup("Nothing appropriate found in the Clipboard");
			return false;
		}
	}
	

//
//	Communication with other classes
    
	public void setInput(String dataString, int inputType) {
		// TODO: fit to just 1, 2 and few 6
		if (inputType == 1) {

			controler.getNSInstance().setAdvisableFilename(dataString);
			new InterceptZips(new File(dataString), controler);
		}
		else if (inputType == 2) {

			new AnalyzeBlob(dataString, controler);
		}
		else {

			new Step2b(dataString, controler, false);
		}
	}
	
//	public Hashtable<Integer, GraphNode> getNodes() {	// now in Step3b !
	
	public void setCompositionMode(boolean toggle) {
		this.compositionMode = toggle;
	}
	
	public void setParseMode(boolean toggle) {
		this.parseMode = toggle;
	}
	
	public void setDropEncoding(boolean toggle) {
		this.dropEncoding = toggle;
	}
	
	public void scoopCompositionWindow(CompositionWindow compositionWindow) {
		String dataString = compositionWindow.dataString;
		new AnalyzeBlob(dataString, controler);
	}
	
	public void setAdvisableFilename(String a) {
		advisableFilename = a;
	}

	public String getAdvisableFilename() {
		return advisableFilename;
	}
	
	public Point getDropLocation() {
		return dropLocation;
	}
	
	public void init() {
		controlerExtras = controler.getControlerExtras();
	}
}

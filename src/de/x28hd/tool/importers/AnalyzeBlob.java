package de.x28hd.tool.importers;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.Utilities;
import de.x28hd.tool.core.GraphEdge;
import de.x28hd.tool.core.GraphNode;

//
//Large single object (as opposed to composed lists)

public class AnalyzeBlob {
	PresentationService controler;
	boolean compositionMode;
	Utilities utilities = new Utilities();
	
	public AnalyzeBlob(String dataString, PresentationService controler) {
		this.controler = controler;
		compositionMode = controler.getNSInstance().compositionMode;
		Document doc = null;
		Utilities utilities = new Utilities();
		doc = utilities.getParsedDocument(dataString);	//	TODO consolidate
		
		boolean xml = checkXML(doc);
		if (!xml) {
			new Step2b(dataString, controler, false);
			return;
		}
	}
	
	public AnalyzeBlob(File file, PresentationService controler) {
		this.controler = controler;
		compositionMode = controler.getNSInstance().compositionMode;
		
		InputStream stream = null;
		boolean xml = true;
		Document doc = null;
		try {
			stream = new FileInputStream(file);
			doc = utilities.getParsedDocument(stream);
		} catch (FileNotFoundException e2) {
			xml = false;
		}
		
		xml = checkXML(doc);
		
		if (!xml) {
			boolean knownExtension = checkExtension(file, stream);
			if (knownExtension) return;
			
			//	Take flat file
			String flatFileContent = "";
			try {
				stream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				System.out.println("Error NS126 " + e);
				controler.displayPopup("Error NS126 File not found " + e);
				return;
			}
			flatFileContent = utilities.convertStreamToString(stream);
			String dataString = flatFileContent;
			
			System.out.println(dataString);
			new Step2b(dataString, controler, false);
			return;
		}
	}
	
	public boolean checkXML(Document doc) {
		if (doc.hasChildNodes()) {
			Element root = doc.getDocumentElement();
			
			//	Try if known XML format
			if (root.getTagName() == "x28map") {
				if (compositionMode) controler.getControlerExtras().getCWInstance().cancel();
				TopicMapLoader loader = new TopicMapLoader(doc, controler, false);
				Hashtable<Integer, GraphNode> nodes = loader.newNodes;
				Hashtable<Integer, GraphEdge> edges = loader.newEdges;
				Rectangle bounds = loader.getBounds();
				new Step3a(controler, nodes, edges, bounds, true);
				return true;
			}
			Importer[] importers = Importer.getImporters();
			for (int k = 0; k < importers.length; k++) {
				if (root.getTagName() == importers[k].getKnownFormat()) {
					if (k != Importer.Evernote 
						&& k != Importer.Word 
						&& k != Importer.Endnote) {
						if (compositionMode) {
							controler.getControlerExtras().getCWInstance().cancel();
						}
					}
					new ImportDirector(k, doc, controler);
					return true;
				}
			}
			return false;
		} else return false;
	}
	
	public boolean checkExtension(File file, InputStream stream) {
		String filename = file.getName();
		String ext = filename.substring(filename.lastIndexOf(".") + 1);
		ext = ext.toLowerCase();
		Importer[] importers = Importer.getImporters();
		for (int k = 0; k < importers.length; k++) {
			if (k == Importer.Tagged) continue;	// txt not via autodiscovery
			if (k == Importer.EdgeList) continue;	// txt not via autodiscovery
			if (k == Importer.Roget) continue;	// htm not via autodiscovery
			if (importers[k].getExt().equals(ext)) {
				if (k != Importer.Evernote 
						&& k != Importer.Word 
						&& k != Importer.Endnote
						&& k != Importer.RIS 
						&& k != Importer.BibTeX) {
					if (compositionMode) {
						controler.getControlerExtras().getCWInstance().cancel();
					}
				}
				new ImportDirector(k, file, controler);
				return true;
			}
		}
		return false;
	}
}

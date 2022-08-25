package de.x28hd.tool.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.Utilities;

//
//Large single object (as opposed to composed lists)

public class AnalyzeBlob {
	PresentationService controler;
	boolean compositionMode;
	Utilities utilities = new Utilities();
	
	public AnalyzeBlob(Assembly assembly, String dataString) {
		this.controler = assembly.controler;
		this.compositionMode = assembly.compositionMode;
		Document doc = null;
		Utilities utilities = new Utilities();
		doc = utilities.getParsedDocument(dataString);	//	TODO consolidate
		
		boolean xml = checkXML(assembly, doc);
		if (!xml) {
			assembly.dataString = dataString;
			new Step2b(assembly);
			return;
		}
	}
	
	public AnalyzeBlob(Assembly assembly, File file) {
		String dataString = assembly.dataString;
		this.controler = assembly.controler;
		this.compositionMode = assembly.compositionMode;
		
		InputStream stream = null;
		boolean xml = true;
		Document doc = null;
		try {
			stream = new FileInputStream(dataString);
			doc = utilities.getParsedDocument(stream);
		} catch (FileNotFoundException e2) {
			xml = false;
		}
		
		xml = checkXML(assembly, doc);
		
		if (!xml) {
			boolean knownExtension = checkExtension(dataString, stream);
			if (knownExtension) return;
			
			//	Take flat file
			String flatFileContent = "";
			try {
				stream = new FileInputStream(dataString);
			} catch (FileNotFoundException e) {
				System.out.println("Error NS126 " + e);
				controler.displayPopup("Error NS126 File not found " + e);
				return;
			}
			flatFileContent = utilities.convertStreamToString(stream);
			dataString = flatFileContent;
			
			assembly.dataString = dataString;
			System.out.println(dataString);
			new Step2b(assembly);
			return;
		}
	}
	
	public boolean checkXML(Assembly assembly, Document doc) {
		if (doc.hasChildNodes()) {
			PresentationService controler = assembly.controler;
			compositionMode = assembly.compositionMode;
			Element root = doc.getDocumentElement();
			
			//	Try if known XML format
			if (root.getTagName() == "x28map") {
				if (compositionMode) controler.getControlerExtras().getCWInstance().cancel();
				TopicMapLoader loader = new TopicMapLoader(doc, controler, false);
				assembly.nodes = loader.newNodes;
				assembly.edges = loader.newEdges;
				assembly.bounds = loader.getBounds();
				assembly.existingMap = true;
				new Step3a(assembly);
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
	
	public boolean checkExtension(String dataString, InputStream stream) {
		File file = new File(dataString);
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

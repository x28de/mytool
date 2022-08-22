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

public class AnalyzeBlob {
	
	public AnalyzeBlob(String dataString, boolean isFile, boolean compositionMode, 
			PresentationService controler) {
		System.out.println("AnalyzeBlob for " + dataString);
		InputStream stream = null;
		Utilities utilities = new Utilities();
		boolean hope = true;
		
		//	Try if XML format
		Document doc = null;
		if (!isFile) {
			doc = utilities.getParsedDocument(dataString);	//	TODO consolidate
		} else if (isFile) {
			try {
				stream = new FileInputStream(dataString);
				doc = utilities.getParsedDocument(stream);
			} catch (FileNotFoundException e2) {
//				System.out.println("Error NS127 (can be ignored) "  + e2);
				hope = false;
			}
		}
		if (doc.hasChildNodes()) {
			Element root = doc.getDocumentElement();
			
			//	Try if known XML format
			System.out.println(root.getTagName());
			if (root.getTagName() == "x28map") {
				if (compositionMode) controler.getControlerExtras().getCWInstance().cancel();
				TopicMapLoader loader = new TopicMapLoader(doc, controler, false);
				Rectangle bounds = loader.getBounds();
				Hashtable<Integer, GraphEdge> newEdges = loader.newEdges;
				Hashtable<Integer, GraphNode> newNodes = loader.newNodes;
				boolean existingMap = true;
//				step3a();
				new Step3a(newNodes, newEdges, bounds, existingMap, controler);
//				newNodes = fetchToUpperLeft(newNodes);
//				existingMap = true;	
////				step3b();
//				new Step3b(controler);
				return;
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
					return;
				}
			}
			hope = false;
		} else hope = false;
			
		//	No XML
		//	Endnote is not (yet) recognized, => works only via Wizard 
		if (!hope) {
			if (isFile) {
				//	Analyze extension
				File file = new File(dataString);
				String filename = file.getName();
				String ext = filename.substring(filename.lastIndexOf(".") + 1);
				ext = ext.toLowerCase();
				Importer[] importers = Importer.getImporters();
				for (int k = 0; k < importers.length; k++) {
					if (importers[k].getExt().equals(ext)) {
						if (k != Importer.Evernote 
							&& k != Importer.Word 
							&& k != Importer.Endnote
							&& k != Importer.RIS 
							&& k != Importer.BibTeX) {
							if (compositionMode) {
								controler.getControlerExtras().getCWInstance().cancel();
							}
							if (k == Importer.Tagged) continue;	// txt not via autodiscovery
							if (k == Importer.EdgeList) continue;	// txt not via autodiscovery
							if (k == Importer.Roget) continue;	// htm not via autodiscovery
						}
//						System.out.println("NS:" + ext + ", " + k + ", " + file);
						new ImportDirector(k, file, controler);
						return;
					}
				}
				
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
			}
			
//			step2b(dataString, false);
			System.out.println("AnalyzeBlob for " + dataString + " (end)");
			new Step2b(dataString, false, compositionMode, controler);
			return;
		}

	}

}

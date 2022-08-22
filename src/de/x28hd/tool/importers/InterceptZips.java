package de.x28hd.tool.importers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.Utilities;

public class InterceptZips {
	
	public InterceptZips(String dataString, PresentationService controler, boolean dropEncoding, boolean compositionMode) {
		System.out.println("InterceptZips for " + dataString);
		Utilities utilities = new Utilities();
		InputStream stream = null;
		Charset CP850 = Charset.forName("CP850");
		File file = new File(dataString);	//	Brute force testing for zip
		if (new File(dataString).isDirectory()) {
			new ImportDirector(Importer.Filetree, new File(dataString), controler);
			return;
		}
		ZipFile zfile = null;
		String filelist = "";
		int entryCount = 0;
		try {
			zfile = new ZipFile(file,CP850);
			Enumeration<? extends ZipEntry> e = zfile.entries();
			boolean done = false;
			while (e.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				stream = zfile.getInputStream(entry);
				String filename = entry.getName();
				filename = filename.replace('\\', '/');		
				if (filename.equals("savefile.xml") || filename.startsWith("topicmap-t-")) {
					new ImportDirector(Importer.OldFormat, stream, controler); 
					done = true;
					break;
				} else if (filename.endsWith("content.cds.xml")) {
					new ImportDirector(Importer.iMapping, file, controler); 
					done = true;
					break;
				} else if (filename.endsWith("zknFile.xml")) {
//					new ImportDirector(13, stream, controler); 
					new ImportDirector(Importer.Zettelkasten, file, controler); 
					done = true;
					break;
				} else if (filename.equals("word/document.xml")) {
					new ImportDirector(Importer.Word, stream, controler); 
					done = true;
					break;
				} else if (filename.startsWith("ppt/slides/slide") && filename.endsWith(".xml")) {
					new ImportDirector(Importer.PPTX, file, controler); 
					done = true;
					break;
				} else if (zfile.getName().endsWith(".h5p") && filename.endsWith("content/content.json")) {
					new ImportDirector(Importer.H5p, file, controler); 
					done = true;
					break;
				} else	{
//					if (entryCount == 0) {
//						filelist = filename + "\r\n";	// to avoid leading newline
//					} else {
					filelist = filelist + filename + "\r\n";
//					}
					entryCount++;
				}
			}
			if (done) {
				zfile.close();
				return;
			}
			if (entryCount == 1) {
				dataString = utilities.convertStreamToString(stream);
//				advisableFilename = file.getAbsolutePath();
				
//				analyzeBlob(dataString, false);
				new AnalyzeBlob(dataString, false, compositionMode, controler);
			} else {
				dataString = filelist;
//				advisableFilename = "";
				
//				exploitFilelist(dataString, false);
				new ExploitFilelist(dataString, false, dropEncoding, compositionMode, controler);
			}
//			zfile.close();
		} catch (ZipException e1) {
//			System.out.println("Error NS121 (can be ignored) " + e1);
			
//			analyzeBlob(dataString, true);
			new AnalyzeBlob(dataString, true, compositionMode, controler);
		} catch (IOException err) {
			System.out.println("Error NS122 " + err);
			controler.displayPopup("Error NS122 " + err);
		}
		
	}

}

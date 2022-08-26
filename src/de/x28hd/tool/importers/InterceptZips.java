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

//
//	Intercept some peculiarities contained in ZIP files, plus folder trees

public class InterceptZips {
	PresentationService controler;
	
	public InterceptZips(File file, PresentationService controler) {
		this.controler = controler;
		
		Utilities utilities = new Utilities();
		InputStream stream = null;
		Charset CP850 = Charset.forName("CP850");
		if (file.isDirectory()) {
			new ImportDirector(Importer.Filetree, file, controler);
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
					filelist = filelist + filename + "\r\n";
					entryCount++;
				}
			}
			if (done) {
				zfile.close();
				return;
			}
			if (entryCount == 1) {
				String dataString = utilities.convertStreamToString(stream);
				controler.getNSInstance().setAdvisableFilename(file.getAbsolutePath());
				new AnalyzeBlob(dataString, controler);
			} else {
				String dataString = filelist;
				new ExploitFilelist(dataString, controler, false);
			}
		} catch (ZipException e1) {
			
			// Important normal case, since Zip is autodetected by brute force
			new AnalyzeBlob(file, controler);
			
		} catch (IOException err) {
			System.out.println("Error NS122 " + err);
			controler.displayPopup("Error NS122 " + err);
		}
		
	}

}

package de.x28hd.tool;

public class Importer {
	int id;
	String importType;
	String knownFormat;
	String ext;
	String extDescription;
	String longDescription;
	
	public Importer(int id, String importType, String knownFormat, String ext,
			String extDescription, String longDescription) {
		this.id	= id;
		this.importType = importType;
		this.knownFormat = knownFormat;
		this.ext = ext;
		this.extDescription = extDescription;
		this.longDescription = longDescription;
	}
    	
    public int getID() {
    	return id;
    };
    public String getImportType() {
    	return importType;
    };
    public String getKnownFormat() {
    	return knownFormat;
    };
    public String getExt() {
    	return ext;
    };
    public String getExtDescription() {
    	return extDescription;
    };
    public String getLongDescription() {
    	return longDescription;
    };

    public static final Importer[] getImporters() {
		return importers;
	}

	static final int Evernote = 0; 
	static final int iMapping = 1; 
	static final int DWZ = 2;
	static final int Cmap = 3; 
	static final int TheBrain = 4;
	static final int Word = 5;
	static final int Endnote = 6;
	static final int Citavi = 7;
	static final int VUE = 8;
	static final int RIS = 9;
	static final int BibTeX = 10;
	static final int FreeMind = 11;
	static final int OPML = 12;
	static final int Zettelkasten = 13;
	static final int Metamaps = 14;
	static final int Gedcom = 15;
	static final int FedWiki = 16;
	static final int Hypothesis = 17;
	static final int OldFormat = 18;
	static final int Filetree = 19;
	static final int Sitemap = 20;
	static final int x28tree = 21;
	
	static final Importer[] importers = {
	new Importer(Importer.Evernote, "Evernote", "en-export", "enex", "enex (Evernote Export file)", 
			"<html>If you have an \"ENEX\" export file exported from the Evernote note taking application</html>)"),
	new Importer(Importer.iMapping, "iMapping", "(not relevant)", "iMap", "iMap (iMapping file)", 
			"<html>A map from the <a href=\"http://imapping.info\">iMapping.info</a> think tool application</html>"),
	new Importer(Importer.DWZ, "DWZ", "kgif", "xml", "xml (DenkWerkZeug KGIF file)", 
			"<html>If you have a \"KGIF\" Knowledge Graph Interchange Format file exported from the <br>"
			+ "<a href=\"http://denkwerkzeug.org\">DenkWerkZeug.org</a> think tool application</html>"),
	new Importer(Importer.Cmap, "Cmap", "cmap", "cxl", "cxl (Cmap CXL file)", 
			"<html>If you have a \"CXL\" export file exported from the CmapTools concept mapping application</html>"),
	new Importer(Importer.TheBrain, "TheBrain", "BrainData", "xml", "xml (TheBrain \"Brain XML\" file)",
			"<html>If you have a \"Brain XML\" file exported from the TheBrain note management application</html>"),
	new Importer(Importer.Word, "Word", "w:document", "docx", "docx (Word Document)", 
			"<html>A Microsoft Word Document (we take the plain text from each paragraph)</html>"),
	new Importer(Importer.Endnote, "Endnote", "(not relevant)", "enw", "enw (Endnote Tagged Import Format)", 
			"<html>If you have an \"Endnote Tagged Import Format\" file exported (we just split it up)</html>"),
	new Importer(Importer.Citavi, "Citavi *)", "(not relevant)", "ctv4", "ctv4 (Citavi 4 Project File)", 
			"<html>A Citavi project file (we extract the core knowledge network) <br>"
			+ "*) = Extended version only</html>"),
	new Importer(Importer.VUE, "VUE", "LW-MAP", "vue", "vue (VUE map file)", 
			"<html>A map file from the VUE (Visual Understanding Environment application</html>"),
	new Importer(Importer.RIS, "RIS", "(not relevant)", "ris", "ris (Research Information System file)", 
			"<html>If you have a \"Research Information System\" file exported (we just split it up)</html>"),
	new Importer(Importer.BibTeX, "BibTeX", "(not relevant)", "bib", "bib (BibTeX file)", 
			"<html>If you have a \"BibTeX\" file exported (we just split it up)</html>"),
	new Importer(Importer.FreeMind, "FreeMind", "map", "mm", "mm (FreeMind file)", 
			"<html>A map file created by the \"FreeMind\" mindmap application or imported into it</html>"),
	new Importer(Importer.OPML, "OPML", "opml", "opml", "opml (Outline file)", 
			"<html>An outline file in the \"OPML\" format. Notes (e.g from Scrivener) are supported.</html>"),
	new Importer(Importer.Zettelkasten, "Zettelkasten", "zettelkasten", "zkn3", "zkn3 (ZKN3 export file)", 
			"<html>If you have a \"ZKN3\" file exported from the Luhmann-inspired notes application.</html>"),
	new Importer(Importer.Metamaps, "Metamaps", "(not relevant)", "csv", "csv (Metamaps export file)", 
			"<html>If you have a CSV file exported from the Metamaps.cc application.</html>"),
	new Importer(Importer.Gedcom, "Gedcom", "(not relevant)", "xml", "xml (Gedcom XML file)", 
			"<html>A genealogical Gedcom XML 6.0 file</html>"),
	new Importer(Importer.FedWiki, "FedWiki", "(not relevant)", "none", "none (invisible)", 
			"<html>Page names and structure from a local Smallest Federated Wiki</html>"),
	new Importer(Importer.Hypothesis, "Hypothes.is", "GEDCOM", "json", "json (Hypothes.is export file)", 
			"<html>If you have a JSON file exported from Hypothes.is, see "
			+ "<a href=\"https://jonudell.info/h/\">jonudell.info/h/</a>.</html>"),
	new Importer(Importer.OldFormat, "(Old Format)", "topicmap", "zip", "zip (Zipped XML Document)", 
			"Old versions of this tool and its precursor DeepaMehta"),
	new Importer(Importer.Filetree, "(Filetree)", "(not relevant)", "none", "none (invisible)", 
			"Folder paths (invisible)"),
	new Importer(Importer.Sitemap, "(Sitemap)", "urlset", "xml", "xml (Sitemap)", 
			"Sitemap (invisible)"),
	new Importer(Importer.x28tree, "(x28tree)", "x28tree", "xml", "xml (x28tree)",
			"x28tree (invisible)")
	};
}
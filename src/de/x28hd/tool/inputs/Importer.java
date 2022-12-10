package de.x28hd.tool.inputs;

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

	public static final int Evernote = 0; 
	public static final int iMapping = 1; 
	public static final int DWZ = 2;
	public static final int Cmap = 3; 
	public static final int TheBrain = 4;
	public static final int Word = 5;
	public static final int Endnote = 6;
	public static final int Citavi = 7;
	public static final int VUE = 8;
	public static final int RIS = 9;
	public static final int BibTeX = 10;
	public static final int FreeMind = 11;
	public static final int OPML = 12;
	public static final int Zettelkasten = 13;
	public static final int Luhmann = 14;
	public static final int Metamaps = 15;
	public static final int Gedcom = 16;
	public static final int FedWiki = 17;
	public static final int Hypothesis = 18;
	public static final int Tagged = 19;
	public static final int Roget = 20;
	public static final int Tinder = 21;
	public static final int GraphXML = 22;
	public static final int Roam = 23;
	public static final int H5p = 24;
	public static final int EdgeList = 25;
	public static final int MapTSV = 26;
	public static final int DemoJSON = 27;
	public static final int Grsshopper = 28;
	public static final int Zotero = 29;
	public static final int PPTX = 30;
	public static final int OldFormat = 31;
	public static final int Filetree = 32;
	public static final int Sitemap = 33;
	public static final int x28tree = 34;
	
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
	new Importer(Importer.Citavi, "Citavi *)", "(not relevant)", "ctv6", "ctv6 (Citavi 6 Project File)", 
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
	new Importer(Importer.Luhmann, "Luhmann", "(not relevant)", "none", "none (invisible)", 
			"<html>If you have Zettels downloaded from the " + 
			"<a href=\"https://niklas-luhmann-archiv.de\">niklas-luhmann-archiv.de</a></html>"),
	new Importer(Importer.Metamaps, "Metamaps", "(not relevant)", "csv", "csv (Metamaps export file)", 
			"<html>If you have a CSV file exported from the Metamaps.cc application.</html>"),
	new Importer(Importer.Gedcom, "Gedcom", "GEDCOM", "xml", "xml (Gedcom XML file)", 
			"<html>A genealogical Gedcom XML 6.0 file</html>"),
	new Importer(Importer.FedWiki, "FedWiki", "(not relevant)", "none", "none (invisible)", 
			"<html>Page names and structure from a local Smallest Federated Wiki</html>"),
	new Importer(Importer.Hypothesis, "Hypothes.is", "(not relevant)", "json", "json (Hypothes.is export file)", 
			"<html>If you have a JSON file exported from Hypothes.is, see "
			+ "<a href=\"https://jonudell.info/h/\">jonudell.info/h/</a>.</html>"),
	new Importer(Importer.Tagged, "Tagged list", "(not relavant)", "txt", "txt (item TAB tag)",
			"If you have a list of items with tags, separated by TAB"),
	new Importer(Importer.Roget, "Thesaurus tags", "(not relavant)", "htm", "htm (10681-h-body-pos.htm)",
			"If you have files downloaded from Roget's or Dornseiff's Thesaurus"),
	new Importer(Importer.Tinder, "TinderBox", "tinderbox", "tbx", "tbx (TinderBox file)",
			"A TinderBox file (we import without adornment, just raw text and links)"),
	new Importer(Importer.GraphXML, "GraphXML", "GraphXML", "xml", "xml (GraphXML file)",
			"If you have a map encoded in the XML-Based Graph Description Format"),
	new Importer(Importer.Roam, "Roam", "roam", "json", "json (Roam export file)",
			"If you have a JSON file exported from Roamresearch"),
	new Importer(Importer.H5p, "H5p", "H5P", "h5p", "h5p (H5P reuse file)",
			"If you have an H5P file containing Image Hotspots, Accordion, or Timeline"),
	new Importer(Importer.EdgeList, "EdgeList", "(not relavant)", "txt", "txt (tab separated)",
			"If you have a TXT file listing the edges in the format: Source Label TAB Target Label"),
	new Importer(Importer.MapTSV, "MapTSV", "(not relavant)", "txt", "txt (tab separated)",
			"For specifying nodes and edges, each in 3 columns. Starts with instructions"),
	new Importer(Importer.DemoJSON, "DemoJSON", "(not relavant)", "json", "json (Demo export)",
			"If you have a JSON file exported from a certain experimental demo"),
	new Importer(Importer.Grsshopper, "Grsshopper", "(not relavant)", "json", "json (from gRSShopper)",
			"If you have a JSON file from gRSShopper"),
	new Importer(Importer.Zotero, "Zotero", "(not relavant)", "html", "html (Zotero Report)",
			"If you have a Zotero Report file, after extracting notes with the ZotFile plugin"),
	new Importer(Importer.PPTX, "PowerPoint", "(not relavant)", "pptx", "pptx (PowerPoint presentation)",
			"A PowerPoint presentation (drop several presentations to get an overview)"),
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

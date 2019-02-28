package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class ImportDirector implements ActionListener {
    Runnable fileChooserMac = new Runnable() {

		@Override 
        public void run() {
            fd = new JFileChooser(System.getProperty("user.home") + File.separator + "Desktop");
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
            		extDescription[importType], extension[importType]);
    		if (System.getProperty("os.name").startsWith("Windows")) {
    			Action details = fd.getActionMap().get("viewTypeDetails");
    			details.actionPerformed(null);
    		}
            fd.setFileFilter(filter);        
            fd.setApproveButtonText("<html><b>Select</b></html>");	// Don't know how to default
    		fd.setDialogType(FileDialog.LOAD);
    		frame.remove(scrollPane);
    		continueButton.setEnabled(false);
            frame.add(fd);
            frame.pack();
            fd.addActionListener(ImportDirector.this);
        }
    };
    
	GraphPanelControler controler;
	JFrame frame = null;
	JFileChooser fd = null;
	int knownFormat = -1;
	JPanel radioPanel = null;
    JPanel descriptionsPanel;
    JScrollPane scrollPane = null;
	JPanel innerFrame = null;
	int importType = 0;
	JButton continueButton = new JButton("Next >");
//	boolean lastStep = false;	// needed if more steps for layout erc
	boolean saxError = false;
	
	
	String [] importTypes = {
			"Evernote", 
			"iMapping", 
			"DWZ", 
			"Cmap", 
			"TheBrain",
			"Word",
			"Endnote",
			"Citavi *)",
			"VUE",
			"RIS",
			"BibTeX",
			"FreeMind",
			"OPML",
			"Zettelkasten",
			"Metamaps",
			"Gedcom",
			"FedWiki",
			"Hypothes.is",
			"(Old Format)",
			"(Filetree)",
			"(Sitemap)",
			"(x28tree)"
			};
	String [] knownFormats = {
			"en-export", 
			"(not relevant)", 
			"kgif", 
			"cmap", 
			"BrainData",
			"w:document",
			"(not relevant)",
			"(not relevant)",
			"LW-MAP",
			"(not relevant)",
			"(not relevant)",
			"map",
			"opml",
			"zettelkasten",
			"(not relevant)",
			"(not relevant)",
			"(not relevant)",
			"GEDCOM",
			"topicmap",
			"(not relevant)",
			"urlset",
			"x28tree",
			};
	String [] extension = {
			"enex", 
			"iMap", 
			"xml", 
			"cxl", 
			"xml",
			"docx", 
			"enw",
			"ctv4",
			"vue",
			"ris",
			"bib",
			"mm",
			"opml",
			"zkn3",
			"csv",
			"xml",
			"none",
			"json",
			"zip",
			"none",
			"xml",
			"xml"
			};
	String [] extDescription = {
			"enex (Evernote Export file)", 
			"iMap (iMapping file)", 
			"xml (DenkWerkZeug KGIF file)", 
			"cxl (Cmap CXL file)", 
			"xml (TheBrain \"Brain XML\" file)",
			"docx (Word Document)",
			"enw (Endnote Tagged Import Format)",
			"ctv4 (Citavi 4 Project File)",
			"vue (VUE map file)",
			"ris (Research Information System file)",
			"bib (BibTeX file)",
			"mm (FreeMind file)",
			"opml (Outline file)",
			"zkn3 (ZKN3 export file)",
			"csv (Metamaps export file)",
			"xml (Gedcom XML file)",
			"none (invisible)",
			"json (Hypothes.is export file)",
			"zip (Zipped XML Document)",
			"none (invisible)",
			"xml (Sitemap)",
			"xml (x28tree)"
			};
	String [] longDescription = {
			"<html>If you have an \"ENEX\" export file exported from the Evernote note taking application</html>)", 
			"<html>A map from the <a href=\"http://imapping.info\">iMapping.info</a> think tool application</html>", 
			"<html>If you have a \"KGIF\" Knowledge Graph Interchange Format file exported from the <br><a href=\"http://denkwerkzeug.org\">DenkWerkZeug.org</a> think tool application</html>", 
			"<html>If you have a \"CXL\" export file exported from the CmapTools concept mapping application</html>", 
			"<html>If you have a \"Brain XML\" file exported from the TheBrain note management application</html>",
			"<html>A Microsoft Word Document (we take the plain text from each paragraph)</html>",
			"<html>If you have an \"Endnote Tagged Import Format\" file exported (we just split it up)</html>",
			"<html>A Citavi project file (we extract the core knowledge network) <br>*) = Extended version only</html>",
			"<html>A map file from the VUE (Visual Understanding Environment application</html>",
			"<html>If you have a \"Research Information System\" file exported (we just split it up)</html>",
			"<html>If you have a \"BibTeX\" file exported (we just split it up)</html>",
			"<html>A map file created by the \"FreeMind\" mindmap application or imported into it</html>",
			"<html>An outline file in the \"OPML\" format. Notes (e.g from Scrivener) are supported.</html>",
			"<html>If you have a \"ZKN3\" file exported from the Luhmann-inspired notes application.</html>",
			"<html>If you have a CSV file exported from the Metamaps.cc application.</html>",
			"<html>A genealogical Gedcom XML 6.0 file</html>",
			"<html>Page names and structure from a local Smallest Federated Wiki</html>",
			"<html>If you have a JSON file exported from Hypothes.is, see <a href=\"https://jonudell.info/h/\">jonudell.info/h/</a>.</html>",
			"Old versions of this tool and its precursor DeepaMehta",
			"Folder paths (invisible)",
			"Sitemap (invisible)",
			"x28tree (invisible)"
			};
	
	//	Nothing given => Launch wizard
	public ImportDirector(GraphPanelControler controler) {
		this.controler = controler;
			launchWizard();
		}
	
	//	XML given
	public ImportDirector(int knownFormat, Document doc, GraphPanelControler controler) {
		this.controler = controler;
		this.knownFormat = knownFormat;
//		if (this.knownFormat < 0) {
//			launchWizard();
//		} else if (this.knownFormat == 0) {
		if (this.knownFormat == 0) {
			new EnexImport(doc, controler);
		} else if (this.knownFormat == 2) {
			new DwzImport(doc, controler);
		} else if (this.knownFormat == 3) {
			new CmapImport(doc, controler);
		} else if (this.knownFormat == 4) {
			new BrainImport(doc, controler);
		} else if (this.knownFormat == 15) {
			new GedcomImport(doc, controler);
		} else if (this.knownFormat == 11 || this.knownFormat == 12 || 
				this.knownFormat == 20 || this.knownFormat == 21) {	
				// (freemind, opml, sitemap, x28tree)
			new TreeImport(doc, controler, this.knownFormat);
		}
	}

	//	File given
	public ImportDirector(int knownFormat, File file, GraphPanelControler controler) {
		this.controler = controler;
		this.knownFormat = knownFormat;
		if (this.knownFormat == 1) {
			new ImappingImport(file, controler);
		} else if (this.knownFormat == 6) {
			new EnwImport(file, controler);
		} else if (this.knownFormat == 7) {
			new CtvImport(file, controler);
		} else if (this.knownFormat == 8) {
			new VueImport(file, controler);
		} else if (this.knownFormat == 9) {
			new EnwImport(file, controler);
		} else if (this.knownFormat == 10) {
			new EnwImport(file, controler);
//		} else if (this.knownFormat == 13) {
//			new ZknImport(file, controler);
			} else if (this.knownFormat == 13) {
			new ZknImport(file, controler);
		} else if (this.knownFormat == 14) {	//	neds different method
			new MetamapsImport(file, controler);
		} else if (this.knownFormat == 19) { 
			new TreeImport(file, controler, 19);
		} else {
			controler.displayPopup("Format autodiscovery failed.\nPlease try the Input Wizard." +
					"\nOr contact support@x28hd.de");
		}
	}

	//	Zip Input stream given
	public ImportDirector(int knownFormat, InputStream stream, GraphPanelControler controler) {
		this.controler = controler;
		this.knownFormat = knownFormat;
		if (this.knownFormat == 5 || this.knownFormat == 13 || this.knownFormat == 18) {
			step4(stream);
		}
	}

//
//	Select import format
	
	public void launchWizard() {
		frame = new JFrame("Import Wizard");
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2-frame.getSize().width/2 - 298, 
				dim.height/2-frame.getSize().height/2 - 209);		
		innerFrame = new JPanel(new BorderLayout());
//		innerFrame.setMaximumSize(new Dimension(300, 900));
		
		ButtonGroup buttonGroup = new ButtonGroup();
        radioPanel = new JPanel(new GridLayout(0, 1));
		radioPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		radioPanel.add(new JLabel("<html>Choose a format:"));

		for (int i = 0; i < importTypes.length - 4; i++) {
			JRadioButton radio = new JRadioButton(importTypes[i]);
			radio.setActionCommand("type-" + i);
			radio.addActionListener(this);
			buttonGroup.add(radio);
	        radioPanel.add(radio);
		}
		radioPanel.add(new JLabel(" "));
		innerFrame.add(radioPanel, BorderLayout.WEST);
		
        descriptionsPanel = new JPanel(new GridLayout(0, 1));
		descriptionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		descriptionsPanel.add(new JLabel(" "));
		for (int i = 0; i < importTypes.length - 4; i++) {
			JLabel descr = new JLabel();
	        descr.setText(longDescription[i]);
	        descriptionsPanel.add(descr);
	        }
//		descriptionsPanel.add(new JLabel("<html><em>Note:</em><br>You can also drag or paste files directly into our windows. <br>" +
//		  "Also text snippets from other applications can be pasted here, <br>" +
//		  "and from some applications you can drag and drop snippets directly, <br>" +
//		  "e.g. from editors like Word or Wordpad or from browsers (Internet Explorer <br>" +
//		  "if protected mode is disabled). Even map snippets from this application <br>" +
//		  "(press Alt + drag or middle mouse button + drag). </html>"));
		descriptionsPanel.add(new JLabel("<html><b>Note:</b> You can also drag most files directly into our windows and paste text into them. Also <br>" +
		  "try to <em>drag</em> text snippets from other applications, or even <em>map</em> snippets from our windows."));
		innerFrame.add(descriptionsPanel, BorderLayout.EAST);
		scrollPane = new JScrollPane(innerFrame);
		
		JToolBar continueBar = new JToolBar();
		continueBar.setLayout(new BorderLayout());
		continueBar.setBorder(new EmptyBorder(10, 10, 10, 10));
		continueBar.setBackground(Color.WHITE);
		continueButton.addActionListener(this);
		continueButton.setEnabled(false);
		continueBar.add(continueButton, BorderLayout.EAST);
		JButton cancelButton = new JButton("Cancel");
		continueBar.add(cancelButton, BorderLayout.WEST);
		cancelButton.addActionListener(this);
		frame.add(scrollPane);
		frame.add(continueBar, BorderLayout.SOUTH);
		if (System.getProperty("os.name").startsWith("Windows")) {
			frame.setMinimumSize(new Dimension(596, 417));
		} else {
			frame.setMinimumSize(new Dimension(796, 417));
		}
        frame.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent action) {
		
		//	Cancel
	    if (action.getActionCommand().equals("Cancel")) {
	        frame.setVisible(false);
	        frame.dispose();
	    }
		
		//	Import type choice
	    for (int i = 0; i < importTypes.length - 3; i++) {
	    	if (action.getActionCommand().equals("type-" + i)) {
			if (i == 7 && !((PresentationService) controler).extended) {
				new LimitationMessage();
		        frame.setVisible(false);
		        frame.dispose();
			} else if (i == 16) {
				new Fed(controler);
				frame.dispose();
				return;
			}
//	    		System.out.println("Type: " + i);
	    		knownFormat = i;
	    		continueButton.setEnabled(true);
//	    		step2(knownFormat);
	    	}
	    }
	    //	File chooser response
	    if (action.getActionCommand().equals("CancelSelection")) {
	        frame.setVisible(false);
	        frame.dispose();
	    }
	    if (action.getActionCommand().equals("ApproveSelection")) {
    		continueButton.setEnabled(true);
//    		lastStep = true;
//	    }
//	    
//	    if (action.getActionCommand().equals("Next >") && lastStep) {
//			String filename = fd.getSelectedFile().getPath() + File.separator + fd.getSelectedFile().getName();
			String filename = fd.getSelectedFile().getName();
			if (knownFormat == 1) {
				new ImappingImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 5) {
				new WordImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 6) {
				new EnwImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 7) {
				new CtvImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 8) {
				new VueImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 9) {
				new EnwImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 10) {
				new EnwImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 13) {
				new ZknImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 14) {
				new MetamapsImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 17) {
				new AnnoImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 18) {
				new TopicMapImporter(fd.getSelectedFile(), controler);
			} else {
				step3(fd.getSelectedFile());
			}
	        frame.setVisible(false);
	        frame.dispose();
	    }
	    
	    if (action.getActionCommand().equals("Next >")) {
    		step2(knownFormat);
	    }

	}

//
//	Open file chooser
	
	public void step2(int importType) {
		this.importType = knownFormat;
		SwingUtilities.invokeLater(fileChooserMac);
	}
	
//
//	Open file
	
	public void step3(File file) {
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error ID101 " + e);
		}
		step4(fileInputStream);
	}
	
//
//	Launch dedicated importer class

	public void step4(InputStream stream) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error ID102 " + e2 );
		}
		
		try {
			inputXml = db.parse(stream);
			
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != knownFormats[knownFormat]) {
				System.out.println("Error ID105, unexpected: " + inputRoot.getTagName() );
				stream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error ID106 " + e1 + "\n" + e1.getClass());
			controler.displayPopup("Import failed:\n" + e1);
			return;
		} catch (SAXException e) {
			System.out.println("Error ID107 " + e );
			saxError = true;
		}
		if (knownFormat == 0) {
			new EnexImport(inputXml, controler);
		} else if (knownFormat == 2) {
			if (saxError) {
				controler.displayPopup("The KGIF file has an XML error; \n " +
						"probably the second line must be removed.");
			} else {
			new DwzImport(inputXml, controler);
			}
		} else if (knownFormat == 3) {
			new CmapImport(inputXml, controler);
		} else if (knownFormat == 4) {
			new BrainImport(inputXml, controler);
		} else if (knownFormat == 5) {
			new WordImport(inputXml, controler);
		} else if (knownFormat == 11) {
			new TreeImport(inputXml, controler, 11);
		} else if (knownFormat == 12) {
			new TreeImport(inputXml, controler, 12);
//		} else if (knownFormat == 13) {
//			new ZknImport(inputXml, controler, 13);
		} else if (knownFormat == 15) {
			new GedcomImport(inputXml, controler);
		} else if (knownFormat == 18) {
			new TopicMapImporter(inputXml, controler);
		} else if (knownFormat == 20) {
			new TreeImport(inputXml, controler, 20);
		} else if (knownFormat == 21) {
			new TreeImport(inputXml, controler, 21);
		}
	}
	
}

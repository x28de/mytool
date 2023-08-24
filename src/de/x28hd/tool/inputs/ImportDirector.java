package de.x28hd.tool.inputs;

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

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.accessories.LimitationMessage;
import de.x28hd.tool.importers.AnnoImport;
import de.x28hd.tool.importers.BrainImport;
import de.x28hd.tool.importers.CmapImport;
import de.x28hd.tool.importers.CtvImport;
import de.x28hd.tool.importers.DemoJsonImport;
import de.x28hd.tool.importers.DwzImport;
import de.x28hd.tool.importers.EdgeList;
import de.x28hd.tool.importers.EnexImport;
import de.x28hd.tool.importers.EnwImport;
import de.x28hd.tool.importers.Fed;
import de.x28hd.tool.importers.GedcomImport;
import de.x28hd.tool.importers.GrsshopperImport;
import de.x28hd.tool.importers.H5pTextImport;
import de.x28hd.tool.importers.ImappingImport;
import de.x28hd.tool.importers.ImportGraphXML;
import de.x28hd.tool.importers.ImportRoam;
import de.x28hd.tool.importers.ImportTSV;
import de.x28hd.tool.importers.ImportWXR;
import de.x28hd.tool.importers.LuhmannImport;
import de.x28hd.tool.importers.MetamapsImport;
import de.x28hd.tool.importers.OntoImport;
import de.x28hd.tool.importers.PowerPointImport;
import de.x28hd.tool.importers.RogetImport;
import de.x28hd.tool.importers.TaggedImport;
import de.x28hd.tool.importers.TinderImport;
import de.x28hd.tool.importers.TopicMapImporter;
import de.x28hd.tool.importers.TreeImport;
import de.x28hd.tool.importers.VueImport;
import de.x28hd.tool.importers.WordImport;
import de.x28hd.tool.importers.ZknImport;
import de.x28hd.tool.importers.ZoteroImport;

public class ImportDirector implements ActionListener {
    Runnable fileChooserMac = new Runnable() {

		@Override 
        public void run() {
            fd = new JFileChooser(System.getProperty("user.home") + File.separator + "Desktop");
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
            		importers[importType].getExtDescription(), 
            		importers[importType].getExt());
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
    
	PresentationService controler;
	JFrame frame = null;
	JFileChooser fd = null;
	Importer[] importers = Importer.getImporters();
	int knownFormat = -1;
	JPanel radioPanel = null;
    JPanel descriptionsPanel;
    JScrollPane scrollPane = null;
	JPanel innerFrame = null;
	int importType = 0;
	JButton continueButton = new JButton("Next >");
	boolean saxError = false;
	
	
	//	Nothing given => Launch wizard
	public ImportDirector(PresentationService controler) {
		this.controler = controler;
			launchWizard();
//			new LuhmannImport(controler);
		}
	
	//	XML given
	public ImportDirector(int knownFormat, Document doc, PresentationService controler) {
		this.controler = controler;
		this.knownFormat = knownFormat;
		if (this.knownFormat == Importer.Evernote) {
			new EnexImport(doc, controler);
		} else if (this.knownFormat == Importer.DWZ) {
			new DwzImport(doc, controler);
		} else if (this.knownFormat == Importer.Cmap) {
			new CmapImport(doc, controler);
		} else if (this.knownFormat == Importer.TheBrain) {
			new BrainImport(doc, controler);
		} else if (this.knownFormat == Importer.Gedcom) {
			new GedcomImport(doc, controler);
		} else if (this.knownFormat == Importer.FreeMind 
				|| this.knownFormat == Importer.OPML 
				|| this.knownFormat == Importer.Sitemap 
				|| this.knownFormat == Importer.x28tree) {	
			new TreeImport(doc, controler, this.knownFormat);
		} else if (this.knownFormat == Importer.Tinder) {
			new TinderImport(doc, controler);
		} else if (this.knownFormat == Importer.GraphXML) {
			new ImportGraphXML(doc, controler);
		} else if (this.knownFormat == Importer.WXR) {
			new ImportWXR(doc, controler, false);
		} else if (this.knownFormat == Importer.Onto) {
			new OntoImport(doc, controler);
		}
	}

	//	File given
	public ImportDirector(int knownFormat, File file, PresentationService controler) {
		this.controler = controler;
		this.knownFormat = knownFormat;
		if (this.knownFormat == Importer.iMapping) {
			new ImappingImport(file, controler);
		} else if (this.knownFormat == Importer.Endnote) {
			new EnwImport(file, controler);
		} else if (this.knownFormat == Importer.Citavi) {
			new CtvImport(file, controler);
		} else if (this.knownFormat == Importer.VUE) {
			new VueImport(file, controler);
		} else if (this.knownFormat == Importer.RIS) {
			new EnwImport(file, controler);
		} else if (this.knownFormat == Importer.BibTeX) {
			new EnwImport(file, controler);
		} else if (this.knownFormat == Importer.Zettelkasten) {
			new ZknImport(file, controler);
		} else if (this.knownFormat == Importer.Metamaps) {	//	needs different method
			new MetamapsImport(file, controler);
		} else if (this.knownFormat == Importer.Filetree) { 
			new TreeImport(file, controler, Importer.Filetree);
		} else if (this.knownFormat == Importer.H5p) { 
			new H5pTextImport(file, controler);
		} else if (this.knownFormat == Importer.Zotero) { 
			new ZoteroImport(file, controler);
		} else if (this.knownFormat == Importer.PPTX) { 
			new PowerPointImport(file, controler);
		} else {
			controler.displayPopup("Format autodiscovery failed.\nPlease try the Input Wizard." +
					"\nOr contact support@x28hd.de");
		}
	}

	//	Zip Input stream given
	public ImportDirector(int knownFormat, InputStream stream, PresentationService controler) {
		this.controler = controler;
		this.knownFormat = knownFormat;
		if (this.knownFormat == Importer.Word 
				|| this.knownFormat == Importer.Zettelkasten 
				|| this.knownFormat == Importer.OldFormat) {
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
		
		ButtonGroup buttonGroup = new ButtonGroup();
        radioPanel = new JPanel(new GridLayout(0, 1));
		radioPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		radioPanel.add(new JLabel("<html>Choose a format:"));

		for (int i = 0; i < importers.length - 4; i++) {
			JRadioButton radio = new JRadioButton(importers[i].getImportType());
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
		for (int i = 0; i < importers.length - 4; i++) {
			JLabel descr = new JLabel();
	        descr.setText(importers[i].getLongDescription());
	        descriptionsPanel.add(descr);
	        }
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
	    for (int i = 0; i < importers.length - 3; i++) {
	    	if (action.getActionCommand().equals("type-" + i)) {
			if (i == Importer.Citavi && !controler.extended) {
				new LimitationMessage();
		        frame.setVisible(false);
		        frame.dispose();
			} else if (i == Importer.FedWiki) {
				frame.dispose();
				new Fed(controler);
				return;
			} else if (i == Importer.Luhmann) {
				frame.dispose();
				new LuhmannImport(controler);
				return;
			} else if (i == Importer.Roget) {
				frame.dispose();
				new RogetImport(controler);
				return;
			} else if (i == Importer.MapTSV) {
				frame.dispose();
				new ImportTSV(controler);
				return;
			}
//	    		System.out.println("Type: " + i);
	    		knownFormat = i;
	    		continueButton.setEnabled(true);
	    	}
	    }
	    //	File chooser response
	    if (action.getActionCommand().equals("CancelSelection")) {
	        frame.setVisible(false);
	        frame.dispose();
	    }
	    if (action.getActionCommand().equals("ApproveSelection")) {
            frame.setVisible(false);
            frame.dispose();
 			if (knownFormat == Importer.iMapping) {
				new ImappingImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Word) {
				new WordImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Endnote) {
				new EnwImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Citavi) {
				new CtvImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.VUE) {
				new VueImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.RIS) {
				new EnwImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.BibTeX) {
				new EnwImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Zettelkasten) {
				new ZknImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Metamaps) {
				new MetamapsImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Hypothesis) {
				new AnnoImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Tagged) {
				new TaggedImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Roam) {
				new ImportRoam(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.OldFormat) {
				new TopicMapImporter(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.H5p) {
				new H5pTextImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.EdgeList) {
				new EdgeList(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.DemoJSON) {
				new DemoJsonImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Zotero) {
				new ZoteroImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.PPTX) {
				new PowerPointImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == Importer.Grsshopper) {
				new GrsshopperImport(fd.getSelectedFile(), controler);
			} else {
				step3(fd.getSelectedFile());
			}
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
			if (inputRoot.getTagName() != importers[knownFormat].getKnownFormat()) {
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
		
		if (knownFormat == Importer.Evernote) {
			new EnexImport(inputXml, controler);
		} else if (knownFormat == Importer.DWZ) {
			if (saxError) {
				controler.displayPopup("The KGIF file has an XML error; \n " +
						"probably the second line must be removed.");
			} else {
			new DwzImport(inputXml, controler);
			}
		} else if (knownFormat == Importer.Cmap) {
			new CmapImport(inputXml, controler);
		} else if (knownFormat == Importer.TheBrain) {
			new BrainImport(inputXml, controler);
		} else if (knownFormat == Importer.Word) {
			new WordImport(inputXml, controler);
		} else if (knownFormat == Importer.FreeMind) {
			new TreeImport(inputXml, controler, Importer.FreeMind);
		} else if (knownFormat == Importer.OPML) {
			new TreeImport(inputXml, controler, Importer.OPML);
		} else if (knownFormat == Importer.Gedcom) {
			new GedcomImport(inputXml, controler);
		} else if (this.knownFormat == Importer.Tinder) {
			new TinderImport(inputXml, controler);
		} else if (this.knownFormat == Importer.GraphXML) {
			new ImportGraphXML(inputXml, controler);
		} else if (this.knownFormat == Importer.WXR) {
			new ImportWXR(inputXml, controler, false);
		} else if (this.knownFormat == Importer.Onto) {
			new OntoImport(inputXml, controler);
		} else if (knownFormat == Importer.OldFormat) {
			new TopicMapImporter(inputXml, controler);
		} else if (knownFormat == Importer.Sitemap) {
			new TreeImport(inputXml, controler, Importer.Sitemap);
		} else if (knownFormat == Importer.x28tree) {
			new TreeImport(inputXml, controler, Importer.x28tree);
		}
	}
}

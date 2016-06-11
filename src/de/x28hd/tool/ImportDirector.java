package de.x28hd.tool;

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
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class ImportDirector implements ActionListener {
	
	GraphPanelControler controler;
	JFrame frame = null;
	JFileChooser fd = null;
	int knownFormat = -1;
	JPanel radioPanel = null;
	
	String [] importTypes = {
			"Evernote", 
			"iMapping", 
			"DWZ", 
			"Cmap", 
			"TheBrain",
			"Word",
			};
	String [] knownFormats = {
			"en-export", 
			"(not relevant)", 
			"kgif", 
			"cmap", 
			"BrainData",
			"w:document"
			};
	String [] extension = {
			"enex", 
			"iMap", 
			"xml", 
			"cxl", 
			"xml",
			"docx"
			};
	String [] extDescription = {
			"enex (Evernote Export file)", 
			"iMap (iMapping file)", 
			"xml (DenkWerkZeug KGIF file)", 
			"cxl (Cmap CXL file)", 
			"xml (TheBrain \"Brain XML\" file)",
			"docx (Word Document)"
			};
	
	public ImportDirector(GraphPanelControler controler) {
		this.controler = controler;
			launchWizard();
		}
	
	public ImportDirector(int knownFormat, Document doc, GraphPanelControler controler) {
		this.controler = controler;
		this.knownFormat = knownFormat;
		if (this.knownFormat < 0) {
			launchWizard();
		} else if (this.knownFormat == 0) {
			new EnexImport(doc, controler);
		} else if (this.knownFormat == 2) {
			new DwzImport(doc, controler);
		} else if (this.knownFormat == 3) {
			new CmapImport(doc, controler);
		} else if (this.knownFormat == 4) {
			new BrainImport(doc, controler);
		}
	}

	public ImportDirector(int knownFormat, File file, GraphPanelControler controler) {
		this.controler = controler;
		this.knownFormat = knownFormat;
		if (this.knownFormat == 1) {
			new ImappingImport(file, controler);
		}
	}

	public ImportDirector(int knownFormat, InputStream stream, GraphPanelControler controler) {
		this.controler = controler;
		this.knownFormat = knownFormat;
		if (this.knownFormat == 5) {
			new WordImport(stream, controler);
		}
	}

//
//	Select import format
	
	public void launchWizard() {
//		this.knownFormat = "cmap";
		frame = new JFrame("Import Wizard");
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2-frame.getSize().width/2 - 298, 
				dim.height/2-frame.getSize().height/2 - 209);		
		
		ButtonGroup buttonGroup = new ButtonGroup();
        radioPanel = new JPanel(new GridLayout(0, 1));
		for (int i = 0; i < importTypes.length; i++) {
			JRadioButton radio = new JRadioButton(importTypes[i]);
			radio.setActionCommand("type-" + i);
			radio.addActionListener(this);
			buttonGroup.add(radio);
	        radioPanel.add(radio);
		}
		frame.add(radioPanel);
        frame.setMinimumSize(new Dimension(596, 417));
        frame.setVisible(true);
	}
	
	
	public void actionPerformed(ActionEvent action) {
		//	Import type choice
	    for (int i = 0; i < importTypes.length; i++) {
	    	if (action.getActionCommand().equals("type-" + i)) {
	    		System.out.println("Type: " + i);
	    		knownFormat = i;
	    		step2(knownFormat);
	    	}
	    }
	    //	File chooser response
	    if (action.getActionCommand().equals("CancelSelection")) {
	        System.out.printf("CancelSelection\n");
	        frame.setVisible(false);
	        frame.dispose();
	    }
	    if (action.getActionCommand().equals("ApproveSelection")) {
	        System.out.printf("ApproveSelection\n");

//			String filename = fd.getSelectedFile().getPath() + File.separator + fd.getSelectedFile().getName();
			String filename = fd.getSelectedFile().getName();
			System.out.println(filename);
			if (knownFormat == 1) {
				new ImappingImport(fd.getSelectedFile(), controler);
			} else if (knownFormat == 5) {
				new WordImport(fd.getSelectedFile(), controler);
			} else {
				step3(fd.getSelectedFile());
			}
	        frame.setVisible(false);
	        frame.dispose();
	    }
	}

//
//	Open file chooser
	
	public void step2(int importType) {
		
        fd = new JFileChooser(System.getProperty("user.home") + File.separator + "Desktop");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
        		extDescription[importType], extension[importType]);
        fd.setFileFilter(filter);        
        Action details = fd.getActionMap().get("viewTypeDetails");
        fd.setApproveButtonText("Next");
        details.actionPerformed(null);
		fd.setDialogType(FileDialog.LOAD);
		frame.remove(radioPanel);
        frame.add(fd);
        frame.pack();
        fd.addActionListener(this);
	}

//
//	Launch dedicated importer class

	public void step3(File file) {
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("Error ID101 " + e);
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document inputXml = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e2) {
			System.out.println("Error ID102 " + e2 );
		}
		
		try {
			inputXml = db.parse(fileInputStream);
			
			Element inputRoot = null;
			inputRoot = inputXml.getDocumentElement();
			if (inputRoot.getTagName() != knownFormats[knownFormat]) {
				System.out.println("Error ID105, unexpected: " + inputRoot.getTagName() );
				fileInputStream.close();
				return;
			} 
		} catch (IOException e1) {
			System.out.println("Error ID106 " + e1 + "\n" + e1.getClass());
		} catch (SAXException e) {
			System.out.println("Error ID107 " + e );
		}
		if (knownFormat == 0) {
			new EnexImport(inputXml, controler);
		} else if (knownFormat == 2) {
			new DwzImport(inputXml, controler);
		} else if (knownFormat == 3) {
			new CmapImport(inputXml, controler);
		} else if (knownFormat == 4) {
			new BrainImport(inputXml, controler);
		}
	}
	
}

package de.x28hd.tool.accessories;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;

import de.x28hd.tool.PresentationService;
import de.x28hd.tool.importers.TreeImport;
import de.x28hd.tool.inputs.Importer;

public class UrlCare extends SwingWorker<Void, Void> implements PropertyChangeListener, ActionListener {
	PresentationService controler;
	File file;
	String homeDir = "";
	
	// For progress bar
	JFrame progressFrame;
	JProgressBar progressBar;
	int monitor = 0;
	int myProgress = 0;
	int alternate = -1;
	JDialog ppanel;
	
	// For reading
	Utilities utilities = new Utilities();
	InputStream stream = null;
	String indent = "";
	
	// For recording
	DefaultMutableTreeNode top;
	int readCount = 0;
	Hashtable<Integer,Boolean> ifDir = new Hashtable<Integer,Boolean>();
	Hashtable<Integer,String> targets = new Hashtable<Integer,String>();
	
	// For options

	JDialog panel;

	String hostString;
	JTextField altHost;

	String extension;
	boolean mac;
	static String website = ".WEBSITE";
	boolean altSwitch;
	JTextField altExt;

	JTextField altDir;
	String resultDir;

	
	public UrlCare(PresentationService c) {
		controler = c;
		mac = System.getProperty("os.name").equals("Mac OS X") ? true : false;
		extension = mac ? ".webloc" : ".URL";
		
		hostString = "twitter.com";
		homeDir = System.getProperty("user.home") + "/Desktop";
		resultDir = homeDir + "/x28dir";
		resultDir = new File(resultDir).getAbsolutePath();
		
//
//		Show options
	    
		panel = new JDialog(controler.getMainWindow(), "Specifications", true);
		panel.setMinimumSize(new Dimension(400, 400));
		panel.setLocation(200, 200);
		panel.setLayout(new BorderLayout());
		
		JLabel top = new JLabel();
		top.setText("<html>This utility will search your file tree " 
				+ "for URL files that contain a given string "
				+ "such as <tt>twitter.com</tt>."
				+ "<br>"
				+ "<br>These URLs are then copied into <tt>.htm</tt> files "
				+ "within a second file tree (<tt>x28dir</tt> on your Desktop) "
				+ "such that you can <ul>"
				+ "<li>open the URL while the site is still accessible, "
				+ "<li>edit the .htm file to copy text or addresses mentioned, "
				+ "<li>or try to recover the URL from archive.org later."
				+ "</ul>"
				+ "<br>You may respecify some values below.");
		top.setBorder(new EmptyBorder(10, 10, 10, 10));
		panel.add(top, "North");
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new GridLayout(6, 2));
		bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
		ButtonGroup buttonGroup = new ButtonGroup();
		
		// Host string
		bottom.add(new JLabel("String contained:"));
		altHost = new JTextField("twitter.com");
		bottom.add(altHost);
		
		// URL file extension
		bottom.add(new JLabel("Extension:"));
		JRadioButton defaultExt = new JRadioButton(extension, true);
		defaultExt.setActionCommand(extension);
		defaultExt.addActionListener(this);
		buttonGroup.add(defaultExt);
		bottom.add(defaultExt);
		if (!mac) {
			bottom.add(new JLabel(" "));
			JRadioButton websiteButton = new JRadioButton(website);
			websiteButton.setActionCommand(website);
			websiteButton.addActionListener(this);
			buttonGroup.add(websiteButton);
			bottom.add(websiteButton);
		}
		bottom.add(new JLabel(" "));
		JRadioButton other = new JRadioButton("Other:");
		other.setActionCommand("other");
		other.addActionListener(this);
		buttonGroup.add(other);
		bottom.add(other);
		bottom.add(new JLabel(" "));
		altExt = new JTextField("");
		altExt.setEnabled(false);
		bottom.add(altExt);
		
		// Result directory
		bottom.add(new JLabel("Result folder:"));
		altDir = new JTextField(resultDir);
		bottom.add(altDir);
		
		// Finish
		JButton continueButton = new JButton("Continue");
		continueButton.addActionListener(this);
		continueButton.setSelected(true);
		panel.add(bottom, "Center");
		panel.add(continueButton, "South");
		panel.setVisible(true);
	}
	
	public void firstPart() {

//
//		Ask for start folder
		
		JFileChooser fd = new JFileChooser();
		fd.setDialogTitle("Select start folder for search");
		if (!mac) {
			Action details = fd.getActionMap().get("viewTypeDetails");
			details.actionPerformed(null);
		} else {
			System.setProperty("apple.awt.fileDialogForDirectories", "true"); 
		}
		fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fd.setCurrentDirectory(new File(homeDir));
		fd.setVisible(true);
	    int returnVal = fd.showOpenDialog(controler.getMainWindow());
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	file = fd.getSelectedFile();
	    } else return;

//
//		Show process bar
		
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		JPanel ppanel = new JPanel();
		ppanel.add(progressBar);

		progressFrame = new JFrame("Loading ...");
		progressFrame.setLocation(100, 170);
		progressFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progressFrame.addWindowListener(myWindowAdapter);
		progressFrame.setLayout(new BorderLayout());
		progressFrame.add(ppanel, BorderLayout.PAGE_START);

		progressFrame.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		progressFrame.setLocation(dim.width/2 - 298, dim.height/2 - 209);		
		progressFrame.setMinimumSize(new Dimension(596, 418));

		progressFrame.setVisible(true);
		controler.getControlerExtras().stopHint();

		addPropertyChangeListener(this);	//	updates progress bar when setProgress()
		execute();							//	calls loadStuff() via doInBackground()
	}
	
	public void loadStuff() {
		
		top = new DefaultMutableTreeNode(new BranchInfo(readCount, ""));
		ifDir.put(readCount, true);
		myProgress = 5;
		setProgress(myProgress);
		fileTreeWalk(file, "", top);
	}
	
	public boolean fileTreeWalk(File file, String indent, DefaultMutableTreeNode parentInTree) {
		readCount++;
		// Show progress
		monitor++;
		if (monitor % 250 > 248) {
			if (myProgress < 97) {
				myProgress = myProgress + (int) (.3 * (100 - myProgress));
			} else {	// keep the propertyChange firing
				myProgress = 98 + alternate;
				alternate = -alternate;
			}
			setProgress(myProgress);
		}
		
		// Traverse folders and files
		boolean found = false;
		DefaultMutableTreeNode branch;
		File[] dirList = file.listFiles();
		if (dirList != null) {
			branch = new DefaultMutableTreeNode(new BranchInfo(readCount, file.getName()));
			ifDir.put(readCount, true);
			for (File f : dirList) {
				if (f.isHidden()) continue;
				
				// recurse
				if (fileTreeWalk(f, indent + "  ", branch)) {
					found = true;
				}
			}
			System.out.println(indent + file.getName());
			if (found) {
				parentInTree.add(branch);
				return true;
			}
		} else {		// leaf entries
			String longFilename = file.toURI().toString();
			String shortName = Utilities.getShortname(longFilename);
			int extOffset = shortName.lastIndexOf(".");
			String foundExtension = "";
			if (extOffset > 0) foundExtension = shortName.substring(extOffset);

			String contentString = "";
			if (!foundExtension.toUpperCase().equals(extension.toUpperCase())) return false;
			try {
				stream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				System.out.println("Error UC123 " + e);
			}
			try {
				InputStream in = (InputStream) stream;
				contentString = utilities.convertStreamToString(in, Charset.forName("UTF-8"));
				in.close();
			} catch (IOException e1) {
				System.out.println("Error UC116 " + e1);
			}
			
			// Check for URLs
			String [] lines = contentString.split("\\r?\\n");
			if (lines.length < 2) return false;
			boolean urlPresent = false;
			int urlLine = -1;
			for (int i = 0; i < lines.length; i++) {
				if (!mac && !lines[i].startsWith("URL=")) continue;
				if (mac && !lines[i].contains("<key>URL</key>")) continue;
				urlPresent = true;
				urlLine = i;
			}
			if (!urlPresent) return false;
			String urlString = !mac ? lines[urlLine].substring(4) : lines[urlLine + 1];
			if (!urlString.contains(hostString)) return false;
			
			// Record target URL
			found = true;
			if (mac) {
				urlString = urlString.replace("^*", "");
				urlString = urlString.replace("<string>", "");
				urlString = urlString.replace("</string>", "");
			}
			branch = new DefaultMutableTreeNode(new BranchInfo(readCount, file.getName()));
			ifDir.put(readCount, false);
			targets.put(readCount, urlString);
			parentInTree.add(branch);
		}
		return found;
	}
	
	public void secondPart() {
		resultsTree(top, resultDir);
		// Show on map
		new TreeImport(new File(resultDir), controler, Importer.Filetree, true);
		controler.getControlerExtras().toggleHyp(1, true);
	}

	public void resultsTree(DefaultMutableTreeNode node, String parentPath) {
		BranchInfo info = (BranchInfo) node.getUserObject();
		String path = "";
		int key = info.getKey();
		boolean isDir = ifDir.get(key);
		path = parentPath +	"/" + node.getUserObject().toString();
		if (isDir) {
			new File(path).mkdirs();
		} else {
			writeFile(path, key);
		}
		Enumeration<DefaultMutableTreeNode> children = node.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = children.nextElement();

			// recurse
			resultsTree(child, path);
		}
	}
	
	public void writeFile(String filename, int key) {
		String ext = extension.substring(1);
		if (!extension.equals(website)) {
			filename = filename.replaceAll("\\.(?i)" + ext + "$", ".htm");
		} else {
			filename = filename.replaceAll("\\.(?i)WEBSITE$", ".URL");
		}
		
		File newfile = new File(filename);
		try {
			FileWriter writer = new FileWriter(newfile);
			String urlString = targets.get(key);
			if (!extension.equals(website)) {
				writer.write("<a href=\"" + urlString + "\">" + urlString + "</a>\n<pre>\n</pre>\n");
			} else {
				writer.write("[InternetShortcut]\nURL=" + urlString + "\n");
			}
			writer.close();
		} catch (IOException e) {
			System.out.println("Error UC101 " + e);
		}
	}
	

	// Accessories for progress bar
	
	public Void doInBackground() {
		setProgress(0);
		try {
		loadStuff();
		} catch (Exception ex)  {
			System.out.println("Error UC113 " + ex);
		}
		return null;
	}
	public void done() {
		progressFrame.dispose();
		secondPart();
	}

	private WindowAdapter myWindowAdapter = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			loadStuff();
		}
	};
	
	public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
            progressBar.setString(monitor + "");
        } 
	}

	// Processing the options
	
	public void actionPerformed(ActionEvent arg0) {
		System.out.println(arg0.getActionCommand());
		if (arg0.getActionCommand().equals("other")) {
			altExt.setEnabled(true);
			altSwitch = true;
			return;
		} else if (arg0.getActionCommand().equals(website)) {
			extension = website;
			return;
		}
		hostString = altHost.getText();
		if (altSwitch) extension = altExt.getText();
		resultDir = altDir.getText();
		System.out.println(extension + " " + hostString + " " + resultDir + "\n");
		panel.dispose();
		firstPart();
	}
}

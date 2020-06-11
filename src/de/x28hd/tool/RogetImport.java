package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class RogetImport implements TreeSelectionListener, ActionListener, HyperlinkListener {
	
	GraphPanelControler controler;
	Hashtable<String,String> catsLong = new Hashtable<String,String>();
	String[] records;
	String contentString = "";
	
	// For preparation parts
	Hashtable<Integer,String> superCatNames = new Hashtable<Integer,String>();
	Hashtable<Integer,Integer> superCatLevels = new Hashtable<Integer,Integer>();
	Hashtable<Integer,HashSet<String>> catSets = new Hashtable<Integer,HashSet<String>>();
	int superCatNum = -1;
	boolean started = false;
	String htmlOut = "";
	boolean itemSwitch = false;		// must be fields because of Parser Callback use
	boolean catSwitch = false;
	boolean superCatSwitch = false;
	boolean nameSwitch = false;
	boolean separatorSwitch = false;
	String itemString = "";
	String catString = "";
	String superCatString = "";
	String catNumber = "";
	int count = 0;
	String previousItem = "";
	String previousCat = "";
	String catNames = "";
	int posCount = 5;
	
	// For the UI
	JTree tree;
    DefaultMutableTreeNode top = null;
    DefaultTreeModel model = null;
    DefaultMutableTreeNode[] superiors = new DefaultMutableTreeNode[4]; 
    Hashtable<Integer,Boolean> selected = new Hashtable<Integer,Boolean>();
    HashSet<String> wanted = new HashSet<String>();
	String[] partsOfSpeech = {"N.", "V.", "Adj.", "Adv.", "Phr."};
    JFrame frame;
    JPanel innerFrame;
	JFileChooser chooser = new JFileChooser();
	JDialog dialog;
    JCheckBox[] posBoxes;
    boolean dornseiff = false;
    Hashtable<String,String> deChapterTitles = new Hashtable<String,String>();
    String[] dePartsOfSpeech = {"Substantive", "Verben", "Adjektive"};
	
	public RogetImport(GraphPanelControler controler) {
		this.controler = controler;

		// Switch point if Dornseiff (DE) instead of Roget (EN);
		askForLanguage();
	}
	
	public void preparation1() {
		
		// Preparation 1
		contentString = askForFile("body", controler);
		if (!dornseiff) {
		filterHTML1(contentString);	// also fills tables that are used in the options dialog
		} else {
			dornseiffParse1(contentString);
		}
		askForSelections();			// -> preparation2()
	}
	
	public void preparation2() {
		if (!dornseiff) {
		contentString = askForFile("index", controler);
		}
		contentString = contentString.replaceAll("&nbsp;", "");
		
	    // show progress
		dialog = new JDialog(controler.getMainWindow(), "Processing the list...");
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setMinimumSize(new Dimension(338, 226));
		dialog.setLocation(dim.width/2-dialog.getSize().width/2 - 169, 
				dim.height/2-dialog.getSize().height/2 - 113);		
		dialog.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel info = new JLabel("Processing the list...");
		dialog.add(info);
		dialog.setVisible(true);
		
		// takes time
		if (!dornseiff) {
		contentString = filterHTML2(contentString) + "\n" + catNames;
		} else {
			contentString = dornseiffParse2(contentString);
		}
		dialog.dispose();
		// Save a copy
		FileWriter list;
		try {
			list = new FileWriter(System.getProperty("user.home") + 
					File.separator + "Desktop" + File.separator + "x28list.txt");
			list.write(contentString);
			list.close();
		} catch (IOException e) {
			System.out.println("Error RG102 " + e);			
		}

		new TaggedImport(contentString, controler);
	}

	//
	// For preparation part 1: processing Roget's body file which contains:
	//
	// <center>supercat</center>
	// <b>#catnum</b> <b>catname</b> 
	// <b>catnum catpos</b> item ... item 
	// <b>catnum catpos</b> item ... item 

	private void filterHTML1(String html) {
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.CENTER) {
					superCatSwitch = true;
					catSwitch = false;
				} else if (t == HTML.Tag.B) {
					catSwitch = true;
				}
			}
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				if (dataString.contains("CLASS")) started = true;
				if (catSwitch) catString += dataString;
				if (superCatSwitch && started) superCatString += dataString;
			}
			public void handleEndTag(HTML.Tag t, int pos) {

				// Super categories
				if (t == HTML.Tag.CENTER) {
					superCatSwitch = false;
					if (!started) return;

					//	Record the hierarchy
					superCatNum++;
					superCatNames.put(superCatNum, superCatString);
					if (superCatString.startsWith("CLASS")) {
						superCatLevels.put(superCatNum, 1);
					} else if (superCatString.startsWith("DIVISION")) {
						superCatLevels.put(superCatNum, 2);
					} else if (superCatString.startsWith("SECTION")) {
						superCatLevels.put(superCatNum, 3);
					} else {
						superCatLevels.put(superCatNum, 4);
					}

					superCatString = "";

					//	Categories
				} else if (t == HTML.Tag.B) {
					catSwitch = false;
					if (!started) return;
					count++;
					if (nameSwitch) {
						catNumber = catNumber.substring(1, catNumber.length() -1);
						catsLong.put(catNumber, catNumber + " " + catString);
						nameSwitch = false;
					}
					if (catString.startsWith("#")) {
						nameSwitch = true;
						catNumber = catString;
						HashSet<String> catSet;
						if (!catSets.containsKey(superCatNum)) {
							catSet = new HashSet<String>();
							catSets.put(superCatNum, catSet);
						} 
						catSet = catSets.get(superCatNum);
						catSet.add(catString);
					}
					catString = "";
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error RG128 " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error RG129 " + e3.toString());
		}
	}

	//
	// For preparation step 2: process Roget's index file which contains:
	//
	//	<b>item</b> <i>cat</i> ... <i>cat</i>
	
	private String filterHTML2(String html) {
		htmlOut = "";
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.B) {
					itemSwitch = true;
				} else if (t == HTML.Tag.I) {
					catSwitch = true;
				} else if (t == HTML.Tag.CENTER) {
					separatorSwitch = true;
				}
			}
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				dataString = dataString.replace("\r", "");
				if (separatorSwitch) return;
				if (itemSwitch) itemString += dataString;
				if (catSwitch) catString += dataString;
			}
			public void handleEndTag(HTML.Tag t, int pos) {
				
				//	Items
				if (t == HTML.Tag.B) {
					itemSwitch = false;
					itemString = itemString.replaceAll(":$", "");
					count++;
					previousItem = itemString;
					
				//	Categories
				} else if (t == HTML.Tag.I) {
					catSwitch = false;
					
					String catNum = catString.replaceAll("(.*)( [0-9]+[ab]? )(.*)", "$2");
													// e.g.: "politics 737b N."
					catNum = catNum.trim();
					catString = catString.replaceAll("(.*)( [0-9]+[ab]? )(.*)", "$2$3");
					catString = catString.replaceAll("^-", "");
					catString = catString.replaceAll(" ", "");
					if (!wanted.contains("#" + catString)) {
						catString = "";
						itemString = "";
						return;
					}
					
					count++;
					if (count % 90 == 0)
						dialog.setTitle("Loaded " + count + " items ...");
					
					if (itemString.isEmpty()) {
						htmlOut += "\n" + previousItem;
					} else {
						htmlOut += "\n" + itemString;
					}
					htmlOut += "\t" +catNum;
					catString = "";
					itemString = "";
					
				} else if (t == HTML.Tag.CENTER) {
					separatorSwitch = false;
				}
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error RG128b " + e2);
		}
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error RG129b " + e3.toString());
		}
		return htmlOut;
	}
    
	private static class MyHTMLEditorKit extends HTMLEditorKit {
    	private static final long serialVersionUID = 7279700400657879527L;

    	public Parser getParser() {
    		return super.getParser();
    	}
    }
    
	public void dornseiffParse1(String contentString) {
		deChapterTitles.put("05", "05 Wesen, Beziehung, Geschehnis");
		deChapterTitles.put("09", "09 Wollen und Handeln");
		deChapterTitles.put("10", "10 Fuehlen, Charakrereigenschaften");
		deChapterTitles.put("11", "11 Das Denken");
		records = contentString.split("\\n");
		System.out.println(records.length + " records read");
		String previousSuperCat = "";
		for (int i = 0; i < records.length; i++) {
			String line = records[i].trim();
			
			if (!line.contains("\t")) continue;
			String[] fields = line.split("\\t");
			String item = fields[0];
			String cat = fields[1];
			if (item.startsWith(cat)) {
				String superCat = cat.substring(0, 2);
				if (!superCat.equals(previousSuperCat)) {
					superCatNum++;
					String superCatName = deChapterTitles.get(superCat);
					superCatNames.put(superCatNum, superCatName);
					superCatLevels.put(superCatNum, 1);
					previousSuperCat = superCat;
				}
				superCatNum++;
				superCatNames.put(superCatNum, item);
				superCatLevels.put(superCatNum, 2);
				catNumber = cat;
				HashSet<String> catSet;
				if (!catSets.containsKey(superCatNum)) {
					catSet = new HashSet<String>();
					catSets.put(superCatNum, catSet);
				} 
				catSet = catSets.get(superCatNum);
				catSet.add(cat);
			}
			if (fields.length < 3) {
				System.out.println(item);
				continue;
			}
			String attr = fields[2];
		}
	}
	public String dornseiffParse2(String contentString) {
		String filtered = "";
		records = contentString.split("\\n");
		for (int i = 0; i < records.length; i++) {
			String line = records[i].trim();
			
			if (!line.contains("\t")) continue;
			String[] fields = line.split("\\t");
			String item = fields[0];
			String cat = fields[1];
			String attr = fields[2];
			if (item.startsWith(cat)) {
				String superCat = cat.substring(0, 2);
				line = item + "\t" + cat + "\r\n";
				filtered += line;
			}
//			if (attr.equals("s")) attr = "N.";
//			if (attr.equals("v")) attr = "V.";
//			if (attr.equals("a")) attr = "Adj.";
			if (!wanted.contains(cat + attr)) continue;
			line = item + "\t" + cat + "\r\n";
			filtered += line;
		}
		return filtered;
	}

//
//	UI accessories
    
	public void askForLanguage() {
		dornseiff = false;
		frame = new JFrame("Choice");
		if (System.getProperty("os.name").startsWith("Windows")) {
			frame.setMinimumSize(new Dimension(596, 417));
		} else {
			frame.setMinimumSize(new Dimension(796, 417));
		}
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2-frame.getSize().width/2, 
				dim.height/2-frame.getSize().height/2);		
		innerFrame = new JPanel(new BorderLayout());
		innerFrame.setBorder(new EmptyBorder(10, 10, 10, 10));
		JEditorPane info = new JEditorPane();
		info.setContentType("text/html");
		info.addHyperlinkListener(this);
		info.setEditable(false);
		info.setBackground(Color.decode("#eeeeee"));
		info.setText("<html><body><font face = \"Segoe UI\">"
				+ "Which demo do you want to see?<br /><br />"
				+ "You will need some downloaded files:<br />"
				+ "<li>For the English samples:<br />"
				+ "<a href=\"http://www.gutenberg.org/files/10681/10681-h-body.htm\">http://www.gutenberg.org/files/10681/10681-h-body.htm</a> and"
				+ "<br /><a href=\"http://www.gutenberg.org/files/10681/10681-h-index.htm\">http://www.gutenberg.org/files/10681/10681-h-index.htm</a>)"
				+ "<br /><br /><li>For the German samples:<br />"
				+ "<a href=\"http://www.x28.privat.t-online.de/dornseiff-demo/\">http://www.x28.privat.t-online.de/dornseiff-demo/demo.txt</a>)"
				+ "</html>");
		innerFrame.add(info, BorderLayout.NORTH);
		
		JPanel buttons = new JPanel(new FlowLayout());
		
		JButton enButton = new JButton("English");
		enButton.addActionListener(this);
		enButton.setSelected(true);
		buttons.add(enButton);
		
		JButton deButton = new JButton("German");
		deButton.addActionListener(this);
		buttons.add(deButton);
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		buttons.add(cancelButton);
		
		innerFrame.add(buttons, BorderLayout.SOUTH);
		
		frame.add(innerFrame);
		frame.setVisible(true);
	}
	
	public String askForFile(String which, GraphPanelControler controler) {
		File file = null;
		String baseDir = "";
		try {
			baseDir = System.getProperty("user.home") + File.separator + "Desktop";
		} catch (Throwable e) {
			System.out.println("Error RG105" + e );
		}
		File b = new File(baseDir);
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(b);
		if (!dornseiff) {
		chooser.setDialogTitle("Open the downloaded http://www.gutenberg.org/files/10681/10681-h-" + which + "-pos.htm");
		} else {
			chooser.setDialogTitle("Open the downloaded http://www.x28.privat.t-online.de/dornseiff-demo/demo.txt");
		}
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = null;
		if (!dornseiff) {
        filter = new FileNameExtensionFilter(".htm (the downloaded " + which + " file)", "htm");
		} else {
			filter = new FileNameExtensionFilter(".txt (the downloaded demo.txt file)", "txt");
		}
        chooser.setFileFilter(filter); 
        JFrame mainWindow = controler.getMainWindow();
	    int returnVal = chooser.showOpenDialog(mainWindow);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	 file = chooser.getSelectedFile();
	    }
		FileInputStream fileInputStream = null;
		Utilities utilities = new Utilities();
		String contentString = "";
		try {
			fileInputStream = new FileInputStream(file);
			InputStream in = (InputStream) fileInputStream;
				contentString = utilities.convertStreamToString(in);
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error RG101b " + e);
		} catch (IOException e) {
			System.out.println("Error RG102b " + e);
		}
		
	    return contentString;
	}
	
	public void askForSelections() {
		frame = new JFrame("Selection");
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2-frame.getSize().width/2 - 298, 
				dim.height/2-frame.getSize().height/2 - 209);	
		innerFrame = new JPanel(new BorderLayout());
		innerFrame.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(new BranchInfo(0, "Select some [sub]sections"));
		superiors[0] = top;
		boolean divisionPresent = false;
		for (int i = 0; i < superCatNames.size(); i++) {
			selected.put(i, false);  // initializing
			int level = superCatLevels.get(i);
			int previousLevel = level - 1;
			if (level == 2) divisionPresent = true;
			if (level == 1 && divisionPresent) divisionPresent = false;
			if (level == 3  && !divisionPresent) previousLevel = previousLevel - 1;
			DefaultMutableTreeNode superior = superiors[previousLevel];
			DefaultMutableTreeNode branch = new DefaultMutableTreeNode(new BranchInfo(i, superCatNames.get(i)));
            superior.add(branch);
            if (level < 4) superiors[level] = branch;
		}
	    model = new DefaultTreeModel(top);
	    tree = new JTree(model);
	    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	    tree.addTreeSelectionListener(this);
	    
		JScrollPane scrollPane = new JScrollPane(tree);
		innerFrame.add(scrollPane, BorderLayout.NORTH);
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new FlowLayout());
		String[] posNames = {"Nouns", "Verbs", "Adjectives", "Adverbs", "Phrases"};
		posBoxes = new JCheckBox[5];
		for (int i = 0; i < posCount; i++) {
			posBoxes[i] = new JCheckBox(partsOfSpeech[i]);
			if (!dornseiff) {
			posBoxes[i].setToolTipText(posNames[i]);
			} else {
				posBoxes[i].setText(dePartsOfSpeech[i]);
			}
			bottom.add(posBoxes[i]);
		}
		JButton nextButton = new JButton("Next >");
		nextButton.addActionListener(this);
		bottom.add(nextButton);
		innerFrame.add(bottom, BorderLayout.SOUTH);
		
		frame.add(innerFrame);
		if (System.getProperty("os.name").startsWith("Windows")) {
			frame.setMinimumSize(new Dimension(596, 417));
		} else {
			frame.setMinimumSize(new Dimension(796, 417));
		}
		frame.setVisible(true);
	}
	

	// Accessories for interfaces
	
	public void actionPerformed(ActionEvent arg0) {
	    if (arg0.getActionCommand().equals("Next >")) {
	    	boolean noCatSelected = true;
	    	catNames = "";
	    	for (int i = 0; i < superCatNames.size(); i++) {
	    		if (!selected.get(i)) continue;
	    		noCatSelected = false;
	    		if (!catSets.containsKey(i)) continue;
	    		HashSet<String> catSet = catSets.get(i);
	    		Iterator<String> iter = catSet.iterator();
	    		while (iter.hasNext()) {
	    			String cat = iter.next();
	    			if (!dornseiff) {
	    			String catNum = cat.substring(1, cat.length() - 1);
	    			String catLong = catsLong.get(catNum);
	    			catNames += catLong + "\t" + catNum + "\n";
	    			}
	    			boolean noPosSelected = true;
	    			for (int j = 0; j < posCount; j++) {
	    				String catPOS = cat;
	    				if (!dornseiff) catPOS = cat.replace(".", "");
	    				if (posBoxes[j].isSelected()) {
	    					if (!dornseiff) {
	    					catPOS += partsOfSpeech[j];
	    					} else {
	    						catPOS += dePartsOfSpeech[j].substring(0, 1).toLowerCase();
	    					}
	    					wanted.add(catPOS);
	    					noPosSelected = false;
	    				}
	    			}
	    			if (noPosSelected) {
	    				controler.displayPopup("No part of speech selected, try again.");
	    				return;
	    			}
	    		}
	    	}
	    	if (noCatSelected) {
	    		controler.displayPopup("No sections selected, try again.");
	    		frame.dispose();
	    		return;
	    	}
	System.out.println(wanted);
	    	catsLong.clear();
	    	frame.dispose();
	    	preparation2();
	    } else {
			String cmd = arg0.getActionCommand();
			if (cmd.equals("German")) {
				dornseiff = true;
				posCount = 3;
			}
			frame.dispose();
			if (cmd.equals("Cancel")) return;
			preparation1();
	    }
	}

	public void valueChanged(TreeSelectionEvent e) {
		TreePath[] paths = e.getPaths();

		for (int i = 0; i < paths.length; i++) {
			TreePath selectedPath = paths[i];
			Object o = selectedPath.getLastPathComponent();
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) o;
			toggleSelection(selectedNode, e.isAddedPath(i));
			
			// TODO subsections?
		}
	}
	public void toggleSelection(DefaultMutableTreeNode selectedNode, boolean fluct) {
		BranchInfo branch = (BranchInfo) selectedNode.getUserObject();
		int keyOfSel = branch.getKey();
		if (selected.containsKey(keyOfSel)) {
			boolean currentSetting = selected.get(keyOfSel);
			selected.put(keyOfSel, !currentSetting);
			Enumeration<DefaultMutableTreeNode> children = selectedNode.children();
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
				toggleSelection(child, fluct);		// recursion
			}
		} else {
			System.out.println("Error RG120 " + keyOfSel);
			frame.dispose();
		}
	}

	public void hyperlinkUpdate(HyperlinkEvent arg0) {
		HyperlinkEvent.EventType type = arg0.getEventType();
		final URL url = arg0.getURL();
		if (type == HyperlinkEvent.EventType.ENTERED) {
		} else if (type == HyperlinkEvent.EventType.ACTIVATED) {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(url.toString()));
				} catch (IOException e) {
				} catch (URISyntaxException e) {
				}
			}	
		}
	}

}

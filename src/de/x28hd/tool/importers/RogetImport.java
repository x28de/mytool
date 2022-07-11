package de.x28hd.tool.importers;

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

import de.x28hd.tool.BranchInfo;
import de.x28hd.tool.PresentationService;
import de.x28hd.tool.MyHTMLEditorKit;
import de.x28hd.tool.Utilities;

public class RogetImport implements TreeSelectionListener, ActionListener, HyperlinkListener {
	
	PresentationService controler;
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
    Hashtable<String,String> sentiments = new Hashtable<String,String>();
    JCheckBox[] sentiBoxes;
    String[] sentiTexts = {"negatives", "positives", "0"};
    String singleChapter = "initial";
    
	public RogetImport(PresentationService controler) {
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
		if (singleChapter.isEmpty()) controler.displayPopup("You selected multiple main groups.\n"
				+ "This is supported but you might be disappointed.");

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
    
	public HashSet<String> fillSenti(int whichSide) {
		HashSet<String> left = new HashSet<String>();
		HashSet<String> right = new HashSet<String>();
	    // inelegant but easier than file juggling
	    final String leftColumn = ""
	    		+ "1,3,5,7,9,13,16,17,19,21,23,25,27,31,33,35,37,39,41,43,46,48,50,52,54,56,58,60,62,64,"
	    		+ "66,69,72,76,78,80,82,87,90,93,96,98,100,102,106,108,108a,110,112,114,116,118,121,123,"
	    		+ "125,127,129,132,132a,134,136,138,140,142,144,147,149,151,153,155,157,159,161,164,166,"
	    		+ "168,171,173,175,178,180,184,186,188,190,192,194,196,198,200,202,204,206,208,210,212,"
	    		+ "214,216,220,223,225,227,234,236,238,240,242,245,247,250,253,255,260,262,264,266,268,"
	    		+ "272,274,276,278,280,282,284,286,288,290,292,294,296,298,300,303,305,307,309,312,316,"
	    		+ "319,321,323,325,327,331,333,335,337,339,341,343,345,348,350,352,354,357,359,364,366,"
	    		+ "368,370,373,375,377,380,382,384,386,388,390,394,396,398,400,402,404,406,408,411,413,"
	    		+ "418,420,423,425,428,430,432,434,436,438,441,446,448,450,451,453,455,457,459,461,464,"
	    		+ "465,467,470,472,474,476,478,480,480a,482,484,486,488,490,492,494,496,498,500,502,505,"
	    		+ "507,516,518,522,525,527,527a,529,532,535,537,540,543,547,551,554,562,564,567,570,572,"
	    		+ "574,576,578,580,582,584,586,588,590,592,597,600,602,604,604a,606,609,611,613,615,618,"
	    		+ "620,622,628,637,639,642,644,646,648,650,652,654,656,658,660,662,664,666,673,677,680,"
	    		+ "682,684,686,688,698,700,702,704,706,708,710,713,716,718,720,722,729,731,734,737,737a,"
	    		+ "737b,739,742,745,748,750,753,755,760,763,765,772,775,777,781,784,787,789,795,803,805,"
	    		+ "807,809,812,812a,814,816,818,822,825,827,829,831,834,836,838,840,842,845,847,847a,850,"
	    		+ "858,861,863,865,870,873,875,878,880,885,888,890,892,894,897,903,906,910,912,914,916,"
	    		+ "918,922,924,926,928,931,933,935,937,939,942,944,946,948,950,953,956,958,960,963,970,"
	    		+ "973,977,979,981,983a,985,987,990,996";
	    final String rightColumn = ""
	    		+ "2,4,6,8,10,14,16a,18,20,22,24,26,28,32,34,36,38,40,42,44,47,49,51,53,55,57,59,61,63,"
	    		+ "65,67,70,73,77,79,81,83,88,91,94,97,99,100a,103,107,109,111,113,115,117,119,122,124,"
	    		+ "126,128,130,133,135,137,139,141,143,145,148,150,152,154,156,158,160,162,165,167,169,"
	    		+ "172,174,175a,179,180a,181,182,185,187,189,191,193,195,197,199,201,203,205,207,209,211,"
	    		+ "213,215,216a,221,224,226,228,235,237,239,241,243,246,248,251,252,254,256,261,263,265,"
	    		+ "267,269,273,275,277,279,281,283,285,287,289,291,293,295,297,299,301,304,306,308,310,"
	    		+ "313,317,320,322,324,326,328,332,334,336,338,340,342,344,346,349,351,353,355,358,360,"
	    		+ "365,367,369,371,374,376,378,381,383,385,387,388a,391,395,397,399,401,403,405,407,408a,"
	    		+ "412,414,419,421,422,424,426,426a,429,431,433,435,437,439,442,447,449,450a,452,454,456,"
	    		+ "458,460,462,464a,465a,468,471,473,475,477,479,481,483,485,487,489,491,493,495,497,499,"
	    		+ "501,503,506,508,517,519,523,526,528,530,533,536,538,539,541,544,548,552,555,563,565,568,"
	    		+ "571,573,575,577,579,581,583,585,587,589,591,593,598,601,603,605,607,609a,610,612,614,"
	    		+ "615a,616,619,621,623,629,638,640,643,645,647,649,651,653,655,657,659,661,663,665,667,"
	    		+ "674,678,679,681,683,685,687,689,699,701,703,705,707,709,711,714,717,719,721,723,730,"
	    		+ "732,735,738,740,743,746,749,751,754,756,761,764,766,773,776,777a,782,785,788,790,796,"
	    		+ "804,806,808,810,812b,813,815,817,819,823,826,828,830,832,833,835,837,839,841,843,846,"
	    		+ "848,849,851,859,860,862,864,866,867,868,869,871,874,876,879,881,886,889,891,893,895,"
	    		+ "898,904,905,907,911,913,914a,917,919,923,925,927,927a,929,932,934,936,938,940,943,945,"
	    		+ "947,949,951,954,957,959,961,964,971,972,974,978,980,982,984,986,988,991,997";
	 	if (whichSide == 0) {
	 		String [] lefts = leftColumn.split(",");
	 		for (int i = 0; i < lefts.length; i++) left.add(lefts[i]);
	 		return left;
	 	} else {
	 		String [] rights = rightColumn.split(",");
	 		for (int i = 0; i < rights.length; i++) right.add(rights[i]);
	 		return right;
	 	}
	}
    
	public void dornseiffParse1(String contentString) {
		deChapterTitles.put("05", "05 Wesen, Beziehung, Geschehnis");
		deChapterTitles.put("09", "09 Wollen und Handeln");
		deChapterTitles.put("10", "10 Fuehlen, Affekte, Charaktereigenschaften");
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
			String attr = fields[2];
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
				sentiments.put(cat, attr);
			}
			if (fields.length < 3) {
				System.out.println(item);
				continue;
			}
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
				if (!singleChapter.isEmpty()) {
					if (!cat.startsWith(singleChapter)) continue;
					line = item.substring(3) + "\t" + cat.substring(3) + "\r\n";
				} else {
				line = item + "\t" + cat + "\r\n";
				}
				filtered += line;
			}

//			if (attr.equals("s")) attr = "N.";
//			if (attr.equals("v")) attr = "V.";
//			if (attr.equals("a")) attr = "Adj.";
			if (!wanted.contains(cat + attr)) continue;
			if (!singleChapter.isEmpty()) {
				line = item + "\t" + cat.substring(3) + "\r\n";
			} else {
			line = item + "\t" + cat + "\r\n";
			}
			filtered += line;
		}
		return filtered;
	}
	
	public void sentiFilter(String mood, boolean reverse) {
		Enumeration<TreeNode> checkList = top.breadthFirstEnumeration();
		while (checkList.hasMoreElements()) {
			DefaultMutableTreeNode tNode = (DefaultMutableTreeNode) checkList.nextElement();
			
			//	Reset manual selections
			TreeNode[] treeNode = tNode.getPath();
			TreePath treePath = new TreePath(treeNode);
			TreePath parent = treePath.getParentPath();
			if (!tree.isPathSelected(parent)) continue;
			tree.removeSelectionPath(treePath);
			if (reverse) continue;
			
			//  Determine automatic selections
			BranchInfo info = (BranchInfo) tNode.getUserObject();
			int catNumber = info.getKey();
			String catName = superCatNames.get(catNumber);
			int level = superCatLevels.get(catNumber);
			if (level < 2) continue;
			catName = catName.substring(0, 5);
			if (!sentiments.containsKey(catName)) {
				System.out.println("Error RG106 " + catName);
				return;
			}
			String senti = sentiments.get(catName);
			if (!senti.equals(mood)) {
				tree.addSelectionPath(treePath);
			}
		}
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
				+ "<br /><a href=\"http://www.gutenberg.org/files/10681/10681-h-index.htm\">http://www.gutenberg.org/files/10681/10681-h-index.htm</a>"
				+ "<br /><br /><li>For the German samples:<br />"
				+ "<a href=\"http://www.x28.privat.t-online.de/dornseiff-demo/\">http://www.x28.privat.t-online.de/dornseiff-demo/from-leipzig.txt</a>"
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
	
	public String askForFile(String which, PresentationService controler) {
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
		
		top = new DefaultMutableTreeNode(new BranchInfo(0, "Select some [sub]sections"));
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
	    tree.setVisibleRowCount(tree.getVisibleRowCount() - 2);
	    
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
		innerFrame.add(bottom, BorderLayout.CENTER);

		JPanel veryBottom = new JPanel();
		veryBottom.setLayout(new FlowLayout());
		sentiBoxes = new JCheckBox[2];
	    if (!dornseiff) {
	    	sentiTexts[0] = "negatives/ right column";
	    	sentiTexts[1] = "positives/ left column";
	    }
		for (int i = 0; i < 2; i++) {
			sentiBoxes[i] = new JCheckBox("All but " + sentiTexts[i]);
			sentiBoxes[i].setActionCommand(sentiTexts[i].substring(0, 1));
			if (dornseiff) sentiBoxes[i].addActionListener(this);
			veryBottom.add(sentiBoxes[i]);
		}
		
		
		JButton nextButton = new JButton("Next >");
		nextButton.addActionListener(this);
		veryBottom.add(nextButton);
		innerFrame.add(veryBottom, BorderLayout.SOUTH);
		
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
			HashSet<String> left = fillSenti(0);
			HashSet<String> right = fillSenti(1);
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
	    				if (sentiBoxes[0].isSelected() && right.contains(catNum)) continue;
	    				if (sentiBoxes[1].isSelected() && left.contains(catNum)) continue;
	    			String catLong = catsLong.get(catNum);
	    			catNames += catLong + "\t" + catNum + "\n";
    				} else {
    					String chapter = cat.substring(0, 2);
    					if (!chapter.equals(singleChapter)) {
    						if (singleChapter == "initial") singleChapter = chapter;
    						else singleChapter = "";
    					}
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
	    	catsLong.clear();
	    	frame.dispose();
	    	preparation2();
	    } else {
			String cmd = arg0.getActionCommand();
			if (cmd.equals("German")) {
				dornseiff = true;
				posCount = 3;
			}
			if (cmd.equals("n")) {
				sentiFilter(cmd, !sentiBoxes[0].isSelected());
				return;
			} else if (cmd.equals("p")) {
				sentiFilter(cmd, !sentiBoxes[1].isSelected());
				return;
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
			toggleSelection(selectedNode, e.isAddedPath(i), false);
			
		}
	}
	public void toggleSelection(DefaultMutableTreeNode selectedNode, boolean fluct, boolean cascaded) {
		BranchInfo branch = (BranchInfo) selectedNode.getUserObject();
		int keyOfSel = branch.getKey();
		if (selected.containsKey(keyOfSel)) {
			boolean currentSetting = selected.get(keyOfSel);
			if (!cascaded) {
			selected.put(keyOfSel, !currentSetting);
			} else {				// for visible marking
				TreeNode[] treeNode = selectedNode.getPath();
				TreePath treePath = new TreePath(treeNode);
				if (fluct) {
					tree.addSelectionPath(treePath);
				} else {
					tree.removeSelectionPath(treePath);
				}
			}
			Enumeration<TreeNode> children = selectedNode.children();
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
				toggleSelection(child, fluct, true);		// recursion
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

package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.json.JSONException;
import org.json.JSONWriter;

public class H5pExport implements HyperlinkListener, ActionListener {
	Hashtable<Integer,GraphNode> nodes;
	Hashtable<Integer,GraphEdge> edges;
	GraphPanelControler controler;
	
	TreeMap<String,Integer> alphaMap = new TreeMap<String,Integer>();
	SortedMap<String,Integer> alphaList = (SortedMap<String,Integer>) alphaMap; 

	JFrame frame = new JFrame("Option");
	JCheckBox glossaryHotspots = null;
	static boolean simple = false;	// additional content type not wanted
	static boolean large = false;	// labels not yet well displayable   
	static boolean empty = false;	// for future when connectors are in H5P

	JFileChooser chooser = null;
	String storeFilename = "";
	String title = "";

	Point translation = null;

	public H5pExport(Hashtable<Integer,GraphNode> nodes, Hashtable<Integer,GraphEdge> edges,
			GraphPanelControler controler) {
		this.nodes = nodes;
		this.edges = edges;
		this.controler = controler;
		
		askForOptions();
	}
	
	public void main() {

//
//		Preparations

		if (askForLocation().isEmpty()) return;

		// Sort nodes (alphabetically, for a start)
		// Along the way, determine the most frequent node color, and the x and y range

		Hashtable<Color,Integer> colors = new Hashtable<Color,Integer>();
		Enumeration<Integer> nodeIDs = nodes.keys();
		int disambig = 0;
		while (nodeIDs.hasMoreElements()) {
			int nodeID = nodeIDs.nextElement();
			GraphNode node = nodes.get(nodeID);
			String sortLabel = node.getLabel().toLowerCase();
			while (alphaMap.containsKey(sortLabel)) sortLabel = sortLabel + disambig++;
			alphaMap.put(sortLabel, nodeID);
			
			Color color = node.getColor();
			int count = 0;
			if (colors.containsKey(color)) {
				count = colors.get(color);
				count++;
			}
			colors.put(color, count);	
		}
		SortedSet<String> alphaSet = (SortedSet<String>) alphaList.keySet();
		Iterator<String> nodesIterator = alphaSet.iterator();
		
		// Output naming
		File zipFile = new File(storeFilename);
		if (!storeFilename.endsWith(".h5p")) storeFilename += ".h5p";
		title = zipFile.getName();
		String contentDir = "content";
		String imagesDir = contentDir + "/images";
		
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(zipFile);
		} catch (FileNotFoundException e1) {
			System.out.println("Error HE103 " + e1);
		}
		ZipOutputStream zipOut = new ZipOutputStream(fileOutputStream, Charset.forName("UTF-8"));
		
//		
//		Create background image
		
		GraphExtras graphExtras = controler.getGraphExtras();
		controler.getControlerExtras().updateBounds();

		BufferedImage bufferedImage = graphExtras.snapShot();

		Rectangle bounds = controler.getControlerExtras().getBounds();
		translation = new Point(bounds.x - 40, bounds.y - 40);
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		if (empty) {
			bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
			Graphics2D g = bufferedImage.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
		}
    	
    	ZipEntry imageEntry = new ZipEntry(imagesDir + "/back.jpg");
		try {
			zipOut.putNextEntry(imageEntry);
			ImageIO.write(bufferedImage, "jpg", zipOut);
    		zipOut.closeEntry();
		} catch (IOException e1) {
			System.out.println("Error HE104 " + e1);
		}
		
    	// Determine top ranking color
    	Enumeration<Color> colorList = colors.keys();
    	int max = Integer.MIN_VALUE;
    	Color topColor = Color.decode("#ccdddd");
    	while (colorList.hasMoreElements()) {
    		Color color = colorList.nextElement();
    		int count = colors.get(color);
    		if (count > max) {
    			max = count;
    			topColor = color;
    		}
    	}
		int r = topColor.getRed();
		int gr = topColor.getGreen();
		int b = topColor.getBlue();
    	String colorString = String.format("#%02x%02x%02x", r, gr, b);

//
//		Create content.json
    	
    	ZipEntry contentEntry = new ZipEntry(contentDir + "/content.json");
    	try {
    		zipOut.putNextEntry(contentEntry);
    		StringWriter appendable = new StringWriter();;
    		JSONWriter writer = new JSONWriter(appendable);

    		writeJSON(writer, nodesIterator, colorString, width, height);
    		
			zipOut.write(appendable.toString().getBytes("UTF-8"));
			zipOut.closeEntry();

//
//			Create h5p.json
    	
	    	ZipEntry h5pEntry = new ZipEntry("h5p.json");
	    	zipOut.putNextEntry(h5pEntry);
	    	zipOut.write(h5pJsonString().getBytes("UTF-8"));
	    	zipOut.closeEntry();
			zipOut.close();
		} catch (IOException e1) {
			System.out.println("Error HE107 " + e1);
		}
	}
	
	public void askForOptions() {
		if (System.getProperty("os.name").startsWith("Windows")) {
			frame.setMinimumSize(new Dimension(596, 227));
		} else {
			frame.setMinimumSize(new Dimension(796, 227));
		}
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2-frame.getSize().width/2, 
				dim.height/2-frame.getSize().height/2);		
		JPanel innerFrame = new JPanel(new BorderLayout());
		innerFrame.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		JLabel info = new JLabel();
		info.setText("<html><body>"
				+ "<h4>Content Types to be used</h4>"
				+ "<span style = \"font-size: 1.2em;\">"
				+ "Instead of traditional popups, you may opt for "
				+ "our format where an unintrusive detail pane "
				+ "will preserve the context.</span></body></html>");
		innerFrame.add(info, BorderLayout.NORTH);

		JPanel boxesPanel = new JPanel(new GridLayout(0, 1));
		boxesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		JCheckBox imageHotspots = new JCheckBox("H5P.ImageHotspots-1.8 ");
		imageHotspots.setSelected(true);
		imageHotspots.setEnabled(false);
		boxesPanel.add(imageHotspots);
		
		glossaryHotspots = new JCheckBox("H5P.GlossaryHotspots-1.0");
		glossaryHotspots.setSelected(false);
		boxesPanel.add(glossaryHotspots);
		innerFrame.add(boxesPanel, BorderLayout.WEST);

		JPanel descriptionsPanel = new JPanel(new GridLayout(0, 1));
		descriptionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
 		JEditorPane descr1 = new JEditorPane();
		descr1.setContentType("text/html");
		descr1.addHyperlinkListener(this);
		descr1.setEditable(false);
		descr1.setBackground(Color.decode("#eeeeee"));
        descr1.setText("<html><font face = \"Segoe UI\">"
        		+ "(available via Add New > Get)");
        descriptionsPanel.add(descr1);
        
 		JEditorPane descr2 = new JEditorPane();
		descr2.setContentType("text/html");
		descr2.addHyperlinkListener(this);
		descr2.setEditable(false);
		descr2.setBackground(Color.decode("#eeeeee"));
		descr2.setMinimumSize(new Dimension(50, 20));;
        descr2.setText("<html><font face = \"Segoe UI\"> (download from "
        		+ "<a href=\"http://www.x28.privat.t-online.de/h5p/H5P.GlossaryHotspots-1.0.zip\">http://www.x28.privat.t-online.de/h5p/</a>)");
        descriptionsPanel.add(descr2);
		innerFrame.add(descriptionsPanel, BorderLayout.EAST);
		
		JPanel buttons = new JPanel(new BorderLayout());
		buttons.setBorder(new EmptyBorder(15, 10, 0, 10));
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		buttons.add(cancelButton, BorderLayout.WEST);
		
		JButton nextButton = new JButton("Next >");
		nextButton.addActionListener(this);
		nextButton.setSelected(true);
		buttons.add(nextButton, BorderLayout.EAST);
		
		innerFrame.add(buttons, BorderLayout.SOUTH);
		frame.add(innerFrame);
		frame.setVisible(true);
	}

	public String askForLocation() {
		FileDialog fd = new FileDialog(controler.getMainWindow(), "Specify filename", FileDialog.SAVE);
		fd.setFile("zipfolder.h5p"); 
		fd.setVisible(true);
		if (fd.getFile() != null) {
			storeFilename = fd.getFile();
			storeFilename = fd.getDirectory() + fd.getFile();
		}
		return storeFilename;
	}

	
//
//	JSON formatting
	
	public final String h5pJsonString() {
		String addLib = "{\"machineName\":\"H5P.GlossaryHotspots\",\"majorVersion\":\"1\",\"minorVersion\":\"0\"},";
		String mainLib = "H5P.GlossaryHotspots";
		if (simple) {
			mainLib = "H5P.ImageHotspots";
			addLib = "";
		}
		final String out = "{\"title\":\"" + title + "\",\"language\":\"en\","
				+ "\"mainLibrary\":\"" + mainLib + "\",\"embedTypes\":[\"div\"],\"license\":\"U\","
				+ "\"preloadedDependencies\":["
				+ "{\"machineName\":\"H5P.Text\",\"majorVersion\":\"1\",\"minorVersion\":\"1\"},"
				+ "{\"machineName\":\"H5P.Video\",\"majorVersion\":\"1\",\"minorVersion\":\"5\"},"
				+ "{\"machineName\":\"flowplayer\",\"majorVersion\":\"1\",\"minorVersion\":\"0\"},"
				+ "{\"machineName\":\"H5P.Image\",\"majorVersion\":\"1\",\"minorVersion\":\"1\"},"
				+ "{\"machineName\":\"H5P.ImageHotspots\",\"majorVersion\":\"1\",\"minorVersion\":\"8\"},"
				+ addLib
				+ "{\"machineName\":\"FontAwesome\",\"majorVersion\":\"4\",\"minorVersion\":\"5\"},"
				+ "{\"machineName\":\"H5P.Transition\",\"majorVersion\":\"1\",\"minorVersion\":\"0\"}]}";
		return out;
	}
	
	public void writeJSON(JSONWriter w, Iterator<String> nodesIterator, String colorString, 
			int width, int height) {
    	try {
    		w.object();
			w.key("iconType");
			w.value("icon");
			w.key("icon");
			w.value("plus");
			w.key("color");
			w.value(colorString);
			
			w.key("hotspots");
			w.array();
			
			// JSON array of hotspots
			while (nodesIterator.hasNext()) {
				String sortLabel = nodesIterator.next();
				int id = alphaMap.get(sortLabel);
				GraphNode node = nodes.get(id);
				
				w.object();
				  w.key("position");
				  w.object();
				    w.key("x");
				    double x = ((node.getXY().x + translation.x) * 100.)/width;
				    w.value(x);
				    w.key("y");
				    double y = ((node.getXY().y + translation.y) * 100.)/height;
				    w.value(y);
				  w.endObject();
				  w.key("alwaysFullscreen");
				  w.value(!simple);

				  w.key("content");
				  w.array(); // we have just 1 content fraction
				    w.object();
				      w.key("params");
				      w.object();
				        w.key("text");
				        w.value(node.getDetail());
				      w.endObject(); // end params
				      w.key("library");
				      w.value("H5P.Text 1.1");
				      w.key("subContentId");
				      w.value(UUID.randomUUID());
				      w.key("metadata");
				      w.object();
				        w.key("contentType");
				        w.value("Text");
				        w.key("license");
				        w.value("U");
				        w.key("title");
				        w.value("Untitled Text");
			    	  w.endObject(); // end metadata
				    w.endObject(); // end fraction
				  w.endArray();
				  w.key("header");
				  w.value(node.getLabel());

				w.endObject();  // end single hotspot
			}
			
			// JSON rest
			w.endArray();
			w.key("hotspotNumberLabel");
			w.value("Hotspot #num");
			w.key("closeButtonLabel");
			w.value("Close");
			
			w.key("backgroundImageAltText");
			String alttext = "Screenshot of map with labels and connectors";
			if (empty) alttext = "empty canvas";
			w.value(alttext);
			
			w.key("image");
			w.object();
			w.key("path");
			w.value("images/back.jpg");
			w.key("mime");
			w.value("image/jpeg");
			w.key("copyright");
			  w.object();
		      w.key("license");
		      w.value("U");
		      w.endObject();
			w.key("width");
			w.value(width);
			w.key("height");
			w.value(height);
			w.endObject();  // end image
			w.endObject();
	    	
		} catch (JSONException e) {
			System.out.println("Error HE102 " + e);
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
					System.out.println("Error HE108 " + e);
				} catch (URISyntaxException e) {
					System.out.println("Error HE109 " + e);
				}
			}	
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		simple = !glossaryHotspots.isSelected();
		int width = controler.getControlerExtras().getBounds().width;
		if (simple && width > 700) controler.displayPopup("Warning: "
				+ "Large maps (more than 700 px wide) don't look good yet\n"
				+ "because the original marker icons are too large.\n"
				+ "You can still see the functionality, but here we\n"
				+ "recommend the new content type GlossaryHospots.");
		frame.dispose();
		if (arg0.getActionCommand().equals("Cancel")) return;
		main();
	}
}

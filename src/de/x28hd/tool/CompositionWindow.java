package de.x28hd.tool;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

public class CompositionWindow implements ActionListener, DocumentListener {

//
//	Accessories for drop & paste (same as in GraphPanel)

	private TransferHandler handler = new TransferHandler() {

		private static final long serialVersionUID = 1L;

		public boolean canImport(TransferHandler.TransferSupport support) {
			if (!support.isDrop()) {		//  CTRL+V -- why here?
				Transferable t = newStuff.readClipboard();
				newStuff.transferTransferable(t);
				return false;
			}
			return newStuff.canImport(support, "CW");
		}

		public boolean importData(TransferHandler.TransferSupport support) {
	        if (!canImport(support)) {
	            return false;
	         }
			return newStuff.importData(support, "CW");
		}
	};

	String dataString = "";

	private GraphPanelControler controler;
	NewStuff newStuff = null;
	JFrame compositionWindow;
	private JTextPane textDisplay;
	AbstractDocument doc;
	int caretPos = 0;
	
	JPanel toolbar;	
	JButton pasteButton;
	StyledEditorKit.PasteAction pasteAction = new StyledEditorKit.PasteAction();
	JButton continueButton;
	JTextArea or = new JTextArea("   or   ");
	JTextArea or2 = new JTextArea("        ");
	JTextArea dropFile  = new JTextArea(" \n        Drop File       \n           here\n ");
	JButton cancelButton;
	int shortcutMask;
	
	JPanel advancedToolbar = null;
	JCheckBox advancedBox = null;
	boolean closeReading = false;
	JCheckBox closeReadingBox = null;
	boolean veryCloseReading = false;
	JCheckBox veryCloseReadingBox = null;
	String p = "§§";
	
	public CompositionWindow(final GraphPanelControler controler, int zoomedSize) {
		this.controler = controler;
		newStuff = controler.getNSInstance();
		shortcutMask = ActionEvent.CTRL_MASK;
		if (System.getProperty("os.name").equals("Mac OS X")) shortcutMask = ActionEvent.META_MASK;

//
//		Window
		
		compositionWindow = new JFrame("Paste, drop or type input here ...");
		compositionWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				cancel();
			}
		});
		compositionWindow.setSize(960 -300, 580 +100);
		compositionWindow.setLayout(new BorderLayout());

		// TODO textarea control space for drop file
		controler.setSystemUI(false);

//
//		Text pane

		JScrollPane scroll = new JScrollPane();
		textDisplay = new JTextPane();
		scroll.getViewport().add(textDisplay);

		StyledDocument styledDoc = textDisplay.getStyledDocument();
		if (styledDoc instanceof AbstractDocument) {
			doc = (AbstractDocument)styledDoc;
		} else {
			System.err.println("Error CW105."); // "Text pane's document isn't an AbstractDocument!"
			System.exit(-1);
		}
		textDisplay.setEditable(true);
		textDisplay.setCaretPosition(0);
		textDisplay.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true);
		textDisplay.setFont(new Font(Font.DIALOG, Font.PLAIN, zoomedSize + 2));

//
//		Buttons
		
		JPanel outerToolbar = new JPanel(new BorderLayout());
		advancedToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		toolbar = new JPanel();

		pasteButton = new JButton("<html><body><center>Press here<br />to Paste</center></body></html>");
		pasteButton.setMnemonic(KeyEvent.VK_V);
		pasteButton.setToolTipText("<html>Press this button to Paste from Clipboard<br />" +
				"-- Tab-separated text lines are interpreted as label + detail;<br />" +
				"-- short lines are used as labels;<br />" +
				"-- longer lines will be numbered, instead.</html>");
		pasteButton.setActionCommand("Paste2");
		//  TODO Rightclick context menuitem paste2

		dropFile.setToolTipText("Drag & Drop an Existing File here");
		or.setBackground(new Color(232, 232, 232));
		or2.setBackground(new Color(232, 232, 232));
		continueButton = new JButton("Continue");
		
		cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
	    cancelButton.addActionListener(this);
	    // TODO cancel via Escape key
		
		toolbar.add(pasteButton);
		toolbar.add(or);
		toolbar.add(dropFile);
		toolbar.add(or2);
		Font font = toolbar.getFont();
		toolbar.setFont(new Font(font.getName(), font.getStyle(), font.getSize() + 25));

		continueButton.setEnabled(false);
		toolbar.add(continueButton);
		toolbar.add(cancelButton);
		
		advancedBox = new JCheckBox("Show Advanced Options");
		advancedBox.setActionCommand("advanced");
		advancedBox.setToolTipText("Support close reading");
		advancedBox.addActionListener(this);
		advancedToolbar.add(advancedBox);
		closeReadingBox = new JCheckBox("Split by double line-break");
		closeReadingBox.setToolTipText("Create paragraphs manually; See also: Advanced > Parsing dropped HTML");
		veryCloseReadingBox = new JCheckBox("Split by period");
		veryCloseReadingBox.setToolTipText("Handle single sentences");
		outerToolbar.add(advancedToolbar, "North");
		outerToolbar.add(toolbar, "South");

//
//		Put it all together

		compositionWindow.add(scroll,"Center");
		compositionWindow.add(outerToolbar,"South");
		Point whereOnScreen = new Point(300,50);
		compositionWindow.setLocation(whereOnScreen);
		compositionWindow.setVisible(true);
		
//
//		Main work
		
//		Receive input per drop & paste and insert it into text doc
		
		pasteButton.addActionListener(this);
		dropFile.setTransferHandler(handler);
		textDisplay.setTransferHandler(handler);	// tacitly expected
		
		// Listen to changes and enable the Continue button
		doc.addDocumentListener(this);

		// Continue: tell controler to scoop the content
		continueButton.addActionListener(this);

	}

//
//	NewStuff() sends snippets here
	
	public void insertSnippet(String dataSnippet) {
//		if (dataSnippet.contains("\r\n")) System.out.println("CW: Dropped or pasted to insert: " + dataSnippet);
//		else System.out.println("CW: Dropped or pasted to insert.");
		try {
			caretPos = textDisplay.getCaretPosition();
			doc.insertString(caretPos, dataSnippet + "\n", null);
			caretPos = caretPos + dataSnippet.length() + 1;
			textDisplay.setCaretPosition(caretPos);
		} catch (BadLocationException e) {
			System.out.println("Error CW106 " + " " + e);
		}
		
	}

	public void cancel() {
		controler.finishCompositionMode();
		close();
	}
	
	private void close() {
		compositionWindow.dispose();
	}

//
// Accessory for Text Pane: Waiting for input text that is typed in or inserted from paste or drop 

	public void insertUpdate(DocumentEvent e) {
		continueButton.setEnabled(true);
		caretPos = textDisplay.getCaretPosition();
	} 	
	public void removeUpdate(DocumentEvent e) {
	}
	public void changedUpdate(DocumentEvent e) {
	}

//
//	Process Paste or Continue
			
	public void actionPerformed(ActionEvent a) {
		if (a.getActionCommand().equals("Paste2")) {
			Transferable t = newStuff.readClipboard();
			newStuff.transferTransferable(t);

		} else if (a.getActionCommand().equals("Continue")) {

			// send assembled insert to caller
			dataString = textDisplay.getText();
			if (veryCloseReadingBox.isSelected()) {
				dataString = veryCloseRead(dataString);
			} else if (closeReadingBox.isSelected()) {
				dataString = closeRead(dataString);
			}

			controler.finishCompositionMode();
			newStuff.scoopCompositionWindow(this);
			close();

		} else if (a.getActionCommand().equals("cancel")) {
			cancel();

		} else if (a.getActionCommand().equals("advanced")) {
			if (advancedBox.isSelected()) {
				advancedToolbar.remove(advancedBox);
				JLabel advancedLabel = new JLabel(" Advanced Options: ");
				advancedToolbar.add(advancedLabel, "West");
				advancedToolbar.setVisible(false);
				advancedToolbar.add(closeReadingBox, "Center");
				advancedToolbar.add(veryCloseReadingBox, "East");
				advancedToolbar.setVisible(true);
				return;
			}

		} else System.out.println("Error CW103 " + a.getActionCommand().toString());
	}

	public String closeRead(String dataString2) {
		dataString2 = dataString2.replaceAll("\\r?\\n\\r?\\n", p);
		dataString2 = dataString2.replaceAll("\\r?\\n", "<br>");
		dataString2 = dataString2.replaceAll(p, "\r\n");
		return dataString2;
	}

	public String veryCloseRead(String dataString2) {
		dataString2 = dataString2.replaceAll("\\r?\\n", p);
		dataString2 = dataString2.replaceAll(p + p, p);
		dataString2 = dataString2.replaceAll("\\. ", ".\r\n");
		
		String[] sentences = dataString2.split("\r\n");
		dataString2 = "";
		boolean abbr = false;
		for (int i = 0; i < sentences.length; i++) {
			if (sentences[i].length() < 5 && !sentences[i].equals(" ")) { 	
				dataString2 = dataString2.substring(0, dataString2.length() - 1) + ". " + sentences[i];
				abbr = true; // probably an abbreviation
				continue;
			} else {
				if (abbr) {
					dataString2 = dataString2 + " " + sentences[i];
					abbr = false;
				} else {
					dataString2 = dataString2 + "\r\n" + sentences[i];
				}
			}
		}
		dataString2 = dataString2.replaceAll("\\? ", "?\r\n");
		dataString2 = dataString2.replaceAll("\\! ", "!\r\n");
		dataString2 = dataString2.replaceAll(p, "\n \n");
		return dataString2;
	}
}

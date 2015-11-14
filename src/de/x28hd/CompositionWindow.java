package de.x28hd;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
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
		
	public CompositionWindow(final GraphPanelControler controler) {
		this.controler = controler;
		newStuff = controler.getNSInstance();
		System.out.println("CW calls NS: " + newStuff.getString());

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

//
//		Buttons
		
		toolbar = new JPanel();

		pasteButton = new JButton("<html><body><center>Press here<br />to Paste</center></body></html>");
		pasteButton.setToolTipText("Press this button to Paste from Clipboard");
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

//
//		Put it all together

		compositionWindow.add(scroll,"Center");
		compositionWindow.add(toolbar,"South");
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
		if (dataSnippet.contains("\r\n")) System.out.println("CW: Dropped or pasted to insert: " + dataSnippet);
		else System.out.println("CW: Dropped or pasted to insert.");
		try {
			caretPos = textDisplay.getCaretPosition();
			doc.insertString(caretPos, dataSnippet, null);
			caretPos = caretPos + dataSnippet.length();
			textDisplay.setCaretPosition(caretPos);
		} catch (BadLocationException e) {
			System.out.println("Error CW106 " + " " + e);
		}
		
	}

	private void cancel() {
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
			newStuff.scoopCompositionWindow(this);
			controler.finishCompositionMode();
			close();

		} else if (a.getActionCommand().equals("cancel")) {
			System.out.println("CW: Cancel pressed");
			cancel();

		} else System.out.println("Error CW103 " + a.getActionCommand().toString());
	}
}

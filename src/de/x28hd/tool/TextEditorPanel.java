package de.x28hd.tool;


import java.awt.Desktop;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import de.x28hd.tool.accessories.MyHTMLEditorKit;
import de.x28hd.tool.core.TextEditorCore;

public class TextEditorPanel extends TextEditorCore implements ActionListener, UndoableEditListener, HyperlinkListener {

	public PresentationService controler;
	UndoManager undoManager = new UndoManager();
	MyMouseMotionAdapter myMouseMotionAdapter = new MyMouseMotionAdapter();
	boolean isDirty = false;
	boolean editableOrClickable = true; //  hyperlinks disabled
	boolean hashesEnabled = false;
	boolean tablet = false;
	private static final long serialVersionUID = 1L;
	int offset = -1;
	boolean bundleInProgress = false;
	boolean dragFake = false;
	String myTransferable = "";
	String htmlOut = "";
	
//
//  Accessories
	public class MyTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 1L;
		
	//  For drag/ copy
		public Transferable createTransferable(JComponent c) {
			if (!dragFake) {
				String blubb = editorPane.getText();
				blubb = filterHTML(blubb);
				myTransferable = blubb;
			} else myTransferable = textToAdd + "\nDisclosure:\t"
					+ "Sorry, this  was a fake mockup and won't work here normally. "
					+ "But from a good browser it WILL work !";
			return new StringSelection(myTransferable);
		}
	    public int getSourceActions(JComponent c) {
	        return TransferHandler.COPY;
	    }
		protected void exportDone(JComponent c, Transferable data, int action) {
			bundleInProgress = false;
			if (dragFake) {
				editorPane.removeMouseMotionListener(myMouseMotionAdapter);
				editorPane.addCaretListener(myCaretAdapter);
				selDot = 0;
				dragFake = false;
			}
		}
	}	
	
	
	// standard action events (e.g., for Context Menu clicks)
	
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand().equals("copyAsList")) copyAsList();
		if (arg0.getActionCommand().equals("linkTo")) linkTo();
		if (arg0.getActionCommand().equals("undo")) undoManager.undo();
		if (arg0.getActionCommand().equals("redo")) undoManager.redo();
	}

	// hyperlinks (only if editable = false)

	public void hyperlinkUpdate(HyperlinkEvent arg0) {
		HyperlinkEvent.EventType type = arg0.getEventType();
		final URL url = arg0.getURL();
		String localAddr = "";
		String urlString = arg0.getDescription();
		if (urlString.startsWith("#")) localAddr = urlString.substring(1);
		if (type == HyperlinkEvent.EventType.ENTERED) {
		} else if (type == HyperlinkEvent.EventType.ACTIVATED) {
			if (Desktop.isDesktopSupported()) {
				if (hashesEnabled) {
					if (!localAddr.isEmpty() && url == null) {
						controler.getControlerExtras().findHash(localAddr);
						return;
					}
				}
				try {
					if (url != null) {
						Desktop.getDesktop().browse(new URI(url.toString()));
					} else {
						Desktop.getDesktop().browse(new URI(urlString));
					}
				} catch (IOException e) {
					controler.displayPopup("File problem \r\n<html><tt>" + url.toString() + "</tt></html>\r\n" + e.getMessage());
				} catch (URISyntaxException e) {
					controler.displayPopup("Syntax error \r\n<html><tt>" + url.toString() + "</tt></html>\r\n " + e);
				}
			}	
			// Switch back from CTRL+Click mode
			if (editableOrClickable) editorPane.setEditable(true);	
		}
	}

	public TextEditorPanel(Object caller) {
		super(caller);
		controler = (PresentationService) caller;
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e) {
			System.out.println("Error TE104a " + e);
		} catch (ClassNotFoundException e) {
			System.out.println("Error TE105" + e);
		} catch (InstantiationException e) {
			System.out.println("Error TE106 " + e);
		} catch (IllegalAccessException e) {
			System.out.println("Error TE107 " + e);
		}  
		editorPane.addHyperlinkListener(this);

//
//		Accessory for Mouse: for Context Menu 
		
		editorPane.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0) {
					int x = e.getX();
					int y = e.getY();
					showContextMenu(x, y);
				}
				if (e.isControlDown()) editorPane.setEditable(false);
			}
			//	TODO: make conditional
			public void mouseReleased(MouseEvent e) {
				if (tablet && e.getClickCount() > 1) {		// double clicked
		            doubleclickReselect();
				}
				if (dragFake) selDot = 0;
			}
		});
	}

	class MyMouseMotionAdapter implements MouseMotionListener {
		public void mouseDragged(MouseEvent e) {
			if (!dragFake) return;
			MyTransferHandler t = new MyTransferHandler();
			editorPane.setTransferHandler(t);
			t.exportAsDrag(editorPane, e, TransferHandler.COPY);
		}
		public void mouseMoved(MouseEvent arg0) {
		}
	};
	
	public void processClicks() {
		super.processClicks();
		if (!textToAdd.isEmpty() && dragFake) {
			//	enable drag
			editorPane.removeCaretListener(myCaretAdapter);
			editorPane.addMouseMotionListener(myMouseMotionAdapter);
		}
	}

//
//		Context menu

	public void showContextMenu(int x, int y) {
		JPopupMenu menu = new JPopupMenu();

		Action cutAction = new StyledEditorKit.CutAction();
		String cutActionCommand = (String) cutAction.getValue(Action.ACTION_COMMAND_KEY);	
		JMenuItem cutItem = new JMenuItem();
		cutItem.setActionCommand(cutActionCommand);
		cutItem.addActionListener(cutAction);
		cutItem.setText("Cut");
		menu.add(cutItem);
		
		Action copyAction = new StyledEditorKit.CopyAction();
		String copyActionCommand = (String) copyAction.getValue(Action.ACTION_COMMAND_KEY);	
		JMenuItem copyItem = new JMenuItem();
		copyItem.setActionCommand(copyActionCommand);
		copyItem.addActionListener(copyAction);
		copyItem.setText("Copy");
		menu.add(copyItem);

		Action pasteAction = new StyledEditorKit.PasteAction();
		String pasteActionCommand = (String) pasteAction.getValue(Action.ACTION_COMMAND_KEY);	
		JMenuItem pasteItem = new JMenuItem();
		pasteItem.setActionCommand(pasteActionCommand);
		pasteItem.addActionListener(pasteAction);
		pasteItem.setText("Paste");
		menu.add(pasteItem);
		
		menu.addSeparator();
		
		JMenuItem undoItem = new JMenuItem();
		undoItem.setActionCommand("undo");
		undoItem.addActionListener(this);
		undoItem.setEnabled(undoManager.canUndo());
		undoItem.setText(undoManager.getUndoPresentationName());
		menu.add(undoItem);
		
		JMenuItem redoItem = new JMenuItem();
		redoItem.setActionCommand("redo");
		redoItem.addActionListener(this);
		redoItem.setEnabled(undoManager.canRedo());
		redoItem.setText(undoManager.getRedoPresentationName());
		menu.add(redoItem);
		
		if (!dumbCaller) {
		
		menu.addSeparator();
		
		JMenuItem copyListItem = new JMenuItem();
		copyListItem.setActionCommand("copyAsList");
		copyListItem.addActionListener(this);
		copyListItem.setText("Copy As List");
		menu.add(copyListItem);
		
		JMenuItem linktoItem = new JMenuItem();
		linktoItem.setActionCommand("linkTo");
		linktoItem.addActionListener(this);
		linktoItem.setText("Create connected Item");
		menu.add(linktoItem);
		}
		
		menu.show(this, x, y);
	}

	public void setDirty(boolean toggle) {
		if (isDirty != toggle) {
			if (toggle && !dumbCaller) {
				controler.setDirty(toggle);
			}
			isDirty = toggle;
		}
	}
	
	boolean isDirty() {
		return isDirty;
	}
	

//	
//	Accessory for double-click
//	(On the Surface Pro tablet, double-clicking selects only from beginning to dot? 
//  2022-06-19 not tested any more (my device has broken); feedback welcome
    public void doubleclickReselect() {
    	
    	boolean proceed = true;
    	String inspect = "";
    	Pattern regexPattern = Pattern.compile("[\\p{IsPunctuation}$+<=>^`|~\\p{IsWhite_Space}]");
    	CharSequence inspectedSeq = "";
    	Matcher matcher = null;
    	while (proceed) {
    		try {
    			inspect = doc.getText(myDot, 1);
    		} catch (BadLocationException e1) {
    			System.out.println("Error TE104 BadLocationException");
    			System.exit(0);
    		}
    		inspectedSeq = (CharSequence) inspect;
    		matcher = regexPattern.matcher(inspectedSeq);
    		if (matcher.matches()) {
    			proceed = false;
    		}  else {
    			myDot++;
    		}
    		editorPane.setSelectionEnd(myDot);
    	}
    }

//
//	Accessory for copy as list
    
    public void copyAsList() {
		bundleInProgress = true;
		MyTransferHandler t = new MyTransferHandler();
		this.setTransferHandler(t);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		t.exportToClipboard(this, clipboard, TransferHandler.COPY);  // This uses my above myTransferable
    }

	private String filterHTML(String html) {
		htmlOut = "";
		
		MyHTMLEditorKit htmlKit = new MyHTMLEditorKit();
		HTMLEditorKit.Parser parser = null;
		HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
			public void handleText(char[] data, int pos) {
				String dataString = new String(data);
				htmlOut = htmlOut + dataString;
			}
			public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.BR ) {
					htmlOut = htmlOut + "\n";
				}
			}
			public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
				if (t == HTML.Tag.P ) htmlOut = htmlOut + "\n";
			}
			public void handleEndTag(HTML.Tag t, int pos) {
				if (t == HTML.Tag.P ) htmlOut = htmlOut + "\n";
			}
		};
		parser = htmlKit.getParser();
		Reader reader; 
		reader = (Reader) new StringReader(html);
		try {
			parser.parse(reader, cb, true);
		} catch (IOException e2) {
			System.out.println("Error TE128 " + e2);
		}
		
		try {
			reader.close();
		} catch (IOException e3) {
			System.out.println("Error TE129 " + e3.toString());
		}
		return htmlOut;
	}

//
//	Accessory for demo trick
    public void setFake() {
    	dragFake = true;
    }
    
//
//	Miscellaneous Accessories
	
	public void setSize(int size) {
		if (System.getProperty("os.name").equals("Mac OS X")) size = size + 2;
		editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		editorPane.setFont(new Font("Serif", Font.PLAIN, size + 2));
		repaint();
	}

	public void toggleHyp() {
		editableOrClickable = !editableOrClickable;
		editorPane.setEditable(editableOrClickable);
	}
	
	public void toggleTablet(boolean onOff) {
		tablet = onOff;
	}
	
	public JTextComponent getTextComponent() {
		return (JTextComponent) editorPane;
	}

	public void toggleHashes(boolean onOff) {
		hashesEnabled = onOff;
	}
	
	public void linkTo() {
		String clickText = textToAdd;
		String before = editorPane.getText();
		String after = before.replace(clickText, "<a href=\"#" + clickText + "\"><u>" + clickText + "</u></a>");
		if (after.contentEquals(before)) {
			controler.displayPopup("Hyperlink to '" + clickText + "' was not created;\n"
					+ "special characters don't work yet; sorry.");
		}	// no idea how to tame JEditorPane's strange HTML storing
		editorPane.setText(after);
		controler.getControlerExtras().linkTo(clickText);
	}
	
	public void tracking(boolean onOff) {
		if (onOff) {
			undoManager.discardAllEdits();
			doc.addUndoableEditListener(this);
		} else {
			doc.removeUndoableEditListener(this);
		}
	}

	public void undoableEditHappened(UndoableEditEvent arg0) {
		UndoableEdit undoableEdit = arg0.getEdit();
		undoManager.addEdit(undoableEdit);
		setDirty(true);
	}
}

package de.x28hd.tool;


import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledEditorKit;
import javax.swing.SwingUtilities;

public class TextEditorPanel extends JPanel implements ActionListener, DocumentListener, HyperlinkListener {

	GraphPanelControler controler;
	private JTextComponent textComponent;
	DefaultStyledDocument doc = null;
	private JScrollPane scrollPane;
	JPanel toolbar;
	StyledEditorKit.BoldAction boldAction = new StyledEditorKit.BoldAction();
	String textToAdd = "";
	CaretListener myCaretAdapter;
	int selMark;
	int selDot;
	int myMark;
	int myDot;
	boolean isDirty = false;
	boolean editableOrClickable = true; //  hyperlinks disabled
	boolean tablet = false;
	private static final long serialVersionUID = 1L;
	int loopdetector = 0;

	int offset = -1;
	String EndOfLineStringProperty = "NOCH NIX";
	String peekEndOfLineStringProperty;
	boolean breakIsTrailing;
	
//
//	Accessory for saving line breaks permanently in the HTML (find them in
//	insertUdate). The challenge is that, somewhere, additional \r or \n
//	get in, followed by 4 white spaces. 
//	TODO find a better way, perhaps by analyzing the doc tree structure 
	
    Runnable insertBreak = new Runnable() {
        public void run() {
			String text = textComponent.getText();
			int pos = textComponent.getCaretPosition();
//			int savedPos = pos;
			String headText = "";
			String tailText = "";
			int headOffset = text.indexOf("<body>");
			int tailOffset = text.indexOf("</body>");
			if (headOffset > -1) {
				headOffset = headOffset + 7;
				headText = text.substring(0, headOffset);
			} else {
				headOffset = 0;
			}
			String middleText = text.substring(headOffset, tailOffset);
			String fixedText = middleText.replace(EndOfLineStringProperty + "    ", "");
			fixedText = fixedText.replace(EndOfLineStringProperty, "<br />");
			fixedText = headText + fixedText + tailText;
//			System.out.println(headOffset + " " + tailOffset + " " + 
//					"middle: \r\n" + middleText + "\r\nfixed: \r\n" + fixedText);
			setText(fixedText);
//			System.out.println("breakIsTrailing ? " + breakIsTrailing);
			if (breakIsTrailing) {
				textComponent.setCaretPosition(pos);
			} else {
				textComponent.setCaretPosition(pos - 1);
			}
			repaint();
		}
    };       
	
//
//  Accessories
	
	// standard action events (e.g., for Context Menu clicks)
	
	public void actionPerformed(ActionEvent arg0) {
//		System.out.println("Action " + arg0.getActionCommand() + " performed");
	}

	// hyperlinks (only if editable = false)

	public void hyperlinkUpdate(HyperlinkEvent arg0) {
		HyperlinkEvent.EventType type = arg0.getEventType();
		final URL url = arg0.getURL();
		if (type == HyperlinkEvent.EventType.ENTERED) {
		} else if (type == HyperlinkEvent.EventType.ACTIVATED) {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(url.toString()));
				} catch (IOException e) {
					controler.displayPopup("File problem \r\n<html><tt>" + url.toString() + "</tt></html>\r\n" + e.getMessage());
				} catch (URISyntaxException e) {
					controler.displayPopup("Syntax error \r\n<html><tt>" + url.toString() + "</tt></html>\r\n " + e);
				}
			}	
		}
	}

	// special

	class BoldSpecialActionAdapter implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			if (a.getActionCommand().equals("AddToLabel")) {
				boldAction.actionPerformed(a);
				controler.addToLabel(textToAdd);
			}
		}
	}

	public TextEditorPanel(GraphPanelControler controler) {
		
		this.controler = controler;
		setLayout(new BorderLayout());
		textComponent = new JTextPane();
		
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

		((JEditorPane) textComponent).setContentType("text/html");
		((JEditorPane) textComponent).addHyperlinkListener(this);
		((JEditorPane) textComponent).addCaretListener(myCaretAdapter);
		doc = (DefaultStyledDocument) textComponent.getDocument();
		doc.addDocumentListener(this);

		textComponent.setEditable(true);		// ### false is required for hyperlinks to work
		
		scrollPane = new JScrollPane(textComponent);
		add(scrollPane);

//
//		Toolbar for Bold, Italic, Underline and Bold Special
		
		toolbar = new JPanel();

		// Bold: Action is a field because it is tapped in BoldSpecialActionAdapter
		String boldActionCommand = (String) boldAction.getValue(Action.ACTION_COMMAND_KEY);	
		JButton boldButton = new JButton("<html><body><b>B</b></body></html>");
		boldButton.setToolTipText("Bold");
		boldButton.setActionCommand(boldActionCommand);
		boldButton.addActionListener(boldAction);
		toolbar.add(boldButton);
		
		// Italic & Underline as expected
    	Action italicAction = new StyledEditorKit.ItalicAction();
		String italicActionCommand = (String) italicAction.getValue(Action.ACTION_COMMAND_KEY);	
		JButton italicButton = new JButton("<html><body><i>I</i></body></html>");
		italicButton.setToolTipText("Italic");
		italicButton.setActionCommand(italicActionCommand);
		italicButton.addActionListener(italicAction);
		toolbar.add(italicButton);

		Action underlineAction = new StyledEditorKit.UnderlineAction();
		String underlineActionCommand = (String) underlineAction.getValue(Action.ACTION_COMMAND_KEY);
		JButton underlineButton = new JButton("<html><body><u>U</u></body></html>");
		underlineButton.setToolTipText("Underline");
		underlineButton.setActionCommand(underlineActionCommand);
		underlineButton.addActionListener(underlineAction);
		toolbar.add(underlineButton);

		// Bold special: "AddToLabel"
		BoldSpecialActionAdapter boldSpecialActionListener = new BoldSpecialActionAdapter();
		String boldSpecialActionCommand = "AddToLabel";

		JButton boldSpecialButton = new JButton("<html><body><b>B+</b></body></html>");
		boldSpecialButton.setToolTipText("Bold Special: bold and add the marked text to the item's label above and on the map");
		boldSpecialButton.setActionCommand(boldSpecialActionCommand);
		boldSpecialButton.addActionListener(boldSpecialActionListener);
		toolbar.add(boldSpecialButton);
		
		add(toolbar, BorderLayout.SOUTH);

//
//		Accessory for Mouse: for Context Menu 
		
		textComponent.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
					int x = e.getX();
					int y = e.getY();
					showContextMenu(x, y);
				}
			}
			//	TODO: make conditional
			public void mouseReleased(MouseEvent e) {
				if (tablet && e.getClickCount() > 1) {		// double clicked
		            doubleclickReselect();
				}
			}
		});

		myCaretAdapter = new MyCaretAdapter();
		textComponent.addCaretListener(myCaretAdapter);
	}
	
	class MyCaretAdapter implements CaretListener {

		public void caretUpdate(CaretEvent e) {
	          myDot = e.getDot();
	          myMark = e.getMark();
			selMark = e.getMark();
			selDot = e.getDot();
//			System.out.println("Dot = " + selDot + ", Mark = " + selMark );
			processClicks();
		}
	}

	public void processClicks() {
		int selectedLen = selDot - selMark;
		if (selectedLen < 0) {
			selectedLen = - selectedLen;
			selMark = selDot;
		}
		try {
			textToAdd = textComponent.getText(selMark, selectedLen);
		} catch (BadLocationException e1) {
			System.out.println("Error TE103 " + e1);
			textToAdd = "";
		}
		if (!textToAdd.isEmpty()) {
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
		
		menu.show(this, x, y);
	}

	// document changes 
	public void changedUpdate(DocumentEvent e) {
		setDirty(true);
	}

	public void insertUpdate(DocumentEvent e) {
		if (loopdetector > 100) {
//			System.out.println("Very strange behavior caused by this text");
			return;		//	TODO understand
		}
		loopdetector++;
		offset = e.getOffset();
		int length = e.getDocument().getLength();
//		System.out.println("insert, offset = " + offset + ", length = " + length);
		String insText1 = null;
		String insText2 = null;
		try {
			insText1 = doc.getText(offset, 1);
//			System.out.println("insText1 = >" + insText1 + "<");
		} catch (BadLocationException e1) {
			System.out.println("Error TE102a " + e);
		}
		try {
			insText2 = doc.getText(offset, 2);
//			System.out.println("insText2 = >" + insText2 + "<");
		} catch (BadLocationException e1) {
			System.out.println("Error TE102b " + e);
		}
		if (offset > 0 && offset < length) {
			if (insText2.startsWith("\n")) {
				breakIsTrailing = false;
//				System.out.println("Success!");
				if (insText2.equals("\n\n")) {
//					System.out.println("at the end");
					breakIsTrailing = true;
				}
				SwingUtilities.invokeLater(insertBreak);
			} else  {
//				System.out.println("Failure, string = -->" + insText1 + "<--");
			}
		}
		
		setDirty(true);
	}

	public void removeUpdate(DocumentEvent e) {	
		setDirty(true);
	}

	public void setDirty(boolean toggle) {
		if (isDirty != toggle) {
			if (toggle) {
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
    		textComponent.setSelectionEnd(myDot);
    	}
    }

//
//	Main Methods
	
	public void setText(String text) {
		textComponent.setCaretPosition(selDot);
		textComponent.setText(text);
		EndOfLineStringProperty = doc.getProperty(DefaultEditorKit.EndOfLineStringProperty).toString();
	}
	
	public String getText() {
		loopdetector = 0;
		return textComponent.getText();
	}
	
	public void setSize(int size) {
		if (System.getProperty("os.name").equals("Mac OS X")) size = size + 2;
		((JEditorPane) textComponent).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		((JEditorPane) textComponent).setFont(new Font("Serif", Font.PLAIN, size + 2));
		repaint();
	}
	
	public void toggleHyp() {
		editableOrClickable = !editableOrClickable;
		textComponent.setEditable(editableOrClickable);
	}
	
	public void toggleTablet(boolean onOff) {
		tablet = onOff;
	}
	
	
	public JTextComponent getTextComponent() {
		return textComponent;
	}

}

package de.x28hd.tool;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

public class TextEditorCore extends JPanel {
	
	public PresentationCore controCore;
	boolean dumbCaller;	// to disable things temporarily
	
	public JEditorPane editorPane; 
	StyledDocument doc = null;
	
	public JScrollPane scrollPane;
	JPanel toolbar;
	StyledEditorKit.BoldAction boldAction = new StyledEditorKit.BoldAction();
	CaretListener myCaretAdapter;
	
	int selMark;
	int selDot;
	int myMark;
	int myDot;
	String textToAdd = "";
	String EndOfLineStringProperty = "(nothing yet)";
	private static final long serialVersionUID = 1L;

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
	
	class BoldSpecialActionAdapter implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			if (a.getActionCommand().equals("AddToLabel")) {
				boldAction.actionPerformed(a);
				controCore.addToLabel(textToAdd);
			}
		}
	}

	public TextEditorCore(Object caller) {
		dumbCaller = (caller.getClass() == PresentationCore.class);
		controCore = (PresentationCore) caller;
		
		setLayout(new BorderLayout());
		editorPane = new JEditorPane();
		editorPane.getEditorKit().createDefaultDocument();
		editorPane.setContentType("text/html");
		editorPane.setText("<body><p style=\"margin-top: 0\">t</p></body>");
		doc = (StyledDocument) editorPane.getDocument();
		
		editorPane.setEditable(true);		// ### false is required for hyperlinks to work
		
		scrollPane = new JScrollPane(editorPane);
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
		
		myCaretAdapter = new MyCaretAdapter();
		editorPane.addCaretListener(myCaretAdapter);
	}
	
//
//	Main Methods
	
	public void setText(String text) {
		EndOfLineStringProperty = doc.getProperty(DefaultEditorKit.EndOfLineStringProperty).toString();
		editorPane.setCaretPosition(selDot);
		text = unwrap(text);
		if (!text.startsWith("    <p") && !text.startsWith("<p")) {
			text = "<p style=\"margin-top: 0\">" + text + "</p>"; 
		}
		editorPane.setText(text);
	}
	
	public String getText() {
		return editorPane.getText();
	}

	public void processClicks() {
		int selectedLen = selDot - selMark;
		if (selectedLen < 0) {
			selectedLen = - selectedLen;
			selMark = selDot;
		}
		try {
			textToAdd = editorPane.getText(selMark, selectedLen);
		} catch (BadLocationException e1) {
			System.out.println("Error TE103 " + e1);
			textToAdd = "";
		}
	}

	public String unwrap(String text) {
		// TODO create a regex
		if (text.startsWith("\t<html>")) text = text.substring(7);	// NewStuff simple files
		if (text.startsWith("<html>" + EndOfLineStringProperty)) text = text.substring(7);
		if (text.startsWith("<html>")) text = text.substring(6);	// TreeImport
		if (text.endsWith("</html>" + EndOfLineStringProperty)) text = text.substring(0, text.length() - 8);
		if (text.startsWith("  <head>" + EndOfLineStringProperty)) text = text.substring(9);
		if (text.startsWith("    " + EndOfLineStringProperty)) text = text.substring(5);
		if (text.startsWith("  </head>" + EndOfLineStringProperty)) text = text.substring(10);
		if (text.startsWith(EndOfLineStringProperty + "  </head>" + EndOfLineStringProperty)) text = text.substring(11);
		if (text.startsWith("  <body>" + EndOfLineStringProperty)) text = text.substring(9);
		if (text.endsWith("  </body>" + EndOfLineStringProperty)) text = text.substring(0, text.length() - 10);
		return text;
	}
}

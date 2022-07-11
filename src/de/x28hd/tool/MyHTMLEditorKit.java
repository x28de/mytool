package de.x28hd.tool;

import javax.swing.text.html.HTMLEditorKit;

public class MyHTMLEditorKit extends HTMLEditorKit {
	private static final long serialVersionUID = 7279700400657879527L;

	public Parser getParser() {
		return super.getParser();
	}
}

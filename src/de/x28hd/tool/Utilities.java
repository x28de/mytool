package de.x28hd.tool;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.StyledEditorKit;

public class Utilities {
	
//
//	Not much yet, will grow during consolidation
	
	static public String getShortname(String longFilename) {
		String shortName = longFilename.replace('\\', '/');	
		if (shortName.endsWith("/")) shortName = shortName.substring(0, shortName.length() - 1);
		shortName = shortName.substring(shortName.lastIndexOf("/") + 1);
		return shortName;
	}

	//
//	Context menu

	public static JPopupMenu showContextMenu() {
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

		return menu;
	}
}

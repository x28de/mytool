package de.x28hd.tool;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class LimitationMessage implements HyperlinkListener {

	public LimitationMessage() {
		JFrame frame = new JFrame();
		frame.setMinimumSize(new Dimension(300,200));

		JEditorPane pane = new JEditorPane();
		pane.setContentType("text/html");
		pane.addHyperlinkListener(this);
		pane.setEditable(false);
		pane.setBackground(Color.decode("#eeeeee"));
		pane.setText("<html><body><font face = \"Segoe UI\">Due to their large size, " +
				"some functiions are <br />only available in the <i>extended</i> version" +
				"<br />which can be downloaded for free from <br />" +
				"<a href=\"http://x28hd.de/tool/extended/\">http://x28hd.de/tool/extended/</a></body></html>");
		Object[] array = {pane};
		JOptionPane opane = new JOptionPane(array, JOptionPane.INFORMATION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION, null);
		JDialog dialog = opane.createDialog(pane, "Limitation");
		dialog.setContentPane(opane);
		dialog.setVisible(true);
	}

	@Override
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

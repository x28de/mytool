package de.x28hd.tool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

public class GraphExtras {
	GraphPanel graphPanel;
	boolean jumpingArrow = false;
	boolean borderOrientation = false;
	boolean showHints = false;
	private Image topImage;
	private Image bottomImage;
	private Image leftImage;
	private Image rightImage;
	final static int BORDER_IMAGE_WIDTH = 84;  
	final static int BORDER_IMAGE_HEIGHT = 12;
	Rectangle bounds;
	public int width, height;
	Font font = new Font("monospace", Font.PLAIN, 12);
	boolean part1 = true;
	int ticks = 0;

	public GraphExtras(final GraphPanel graphPanel) {
		
		this.graphPanel = graphPanel;
//		width = ((JComponent) graphPanel).getWidth() + 1;
//		height = ((JComponent) graphPanel).getHeight() + 1;
//		bounds = new Rectangle(height/2, width/2, 0, 0);
		topImage = getImage("up.gif");
		bottomImage = getImage("down.gif");
		leftImage = getImage("left.gif");
		rightImage = getImage("right.gif");
	}
	
	public void paintHints(Graphics g) {
		if (!showHints) return;
		if (jumpingArrow) paintJumpingArrow(g);
		if (!borderOrientation) return;
		if (showTopImage()) {
			g.drawImage(topImage, (width - BORDER_IMAGE_WIDTH) / 2, 0, graphPanel);
		}
		if (showBottomImage()) {
			g.drawImage(bottomImage, (width - BORDER_IMAGE_WIDTH) / 2, height -
				BORDER_IMAGE_HEIGHT, graphPanel);
		}
		if (showLeftImage()) {
			g.drawImage(leftImage, 0, (height - BORDER_IMAGE_WIDTH) / 2, graphPanel);
		}
		if (showRightImage()) {
			g.drawImage(rightImage, width - BORDER_IMAGE_HEIGHT, (height -
				BORDER_IMAGE_WIDTH) / 2, graphPanel);
		}
	}
	public void paintJumpingArrow(Graphics g) {
		g.setFont(font); 
	if (part1) {
		float vel = 5;
		float grav = .5f;
		int y = 100;
		for (int time = 0; time < ticks - 180; time++) {
			if (y <= 0) grav = -grav;
			vel = vel + grav;
			if (y <= 0) vel = -vel;
			y = y - (int) vel;
			if (ticks > 200) break;
		}
		vel = 5;
		grav = .5F;
		for (int time = 0; time < ticks - 200; time++) {
			if (y <= 0) grav = -grav;
			vel = vel + grav;
			if (y <= 0) vel = -vel;
			y = y - (int) vel;
			if (ticks > 220) break;
		}
//		System.out.println(ticks + " " + y);
		int x = 85;
		if (System.getProperty("os.name").equals("Mac OS X")) x = 241;
		int[] xPoints = {x, x + 40, x + 28, x + 28, x - 28, x - 28, x - 40};
		int[] yPoints = {y, y + 25, y + 25, y + 65, y + 65, y + 25, y + 25};
//		g.setColor(Color.GRAY);
		g.drawPolygon(xPoints, yPoints, 7);
		g.setColor(Color.GRAY);
		g.drawString("Insert", x - 17, y + 32);
		g.drawString("some", x - 17, y + 45);
		g.drawString("items ?", x - 17, y + 58);
	} else {
		float vel = 20;
		float grav = 1.5f;
		int x = width - 300;
		int y = 170;
		for (int time = 0; time < ticks - 80; time++) {
			vel = vel + grav;
			x = x + (int) vel;
			y = y - (int) (.33 * vel);
			if (ticks > 100) break;
		}
		vel = 20;
		grav = 1.5F;
		for (int time = 0; time < ticks - 100; time++) {
			vel = vel + grav;
			x = x + (int) vel;
			y = y - (int) (.33 * vel);
			if (ticks > 120) break;
		}
		int[] xPoints = {x, x - 30, x - 31, x - 120, x - 124, x - 33, x - 38};
		int[] yPoints = {y, y + 26, y + 16, y + 45, y + 35, y + 6, y - 4};
		g.setColor(Color.GRAY);
		if (ticks < 210) { 
			if (x <= width + 30) g.drawPolygon(xPoints, yPoints, 7);
//			g.setColor(Color.BLACK);
			g.setColor(Color.GRAY);
			g.setFont(font);
			g.drawString("Then", width - 380, 160);
			if (ticks > 100) { 
				g.drawString("let your eyes DART at " +
				"the details corner !", width - 380, 210 + 20);
			}
		}
	}
	}
	public void jumpingArrow(boolean clueless) {
		if (!clueless) ticks = 401;
		if (ticks > 400) {
			ticks = 0;
			part1 = !part1;
			jumpingArrow = false;
			showHints = borderOrientation || jumpingArrow;
			graphPanel.repaint();
			return;
		}
		ticks++;
		int wait = 180;
		if (!part1) wait = 80;
		if (ticks < wait) {
//			System.out.println("Waiting");
			jumpingArrow = false;
			showHints = borderOrientation || jumpingArrow;
			return;
		} else {
			jumpingArrow = true;
			showHints = borderOrientation || jumpingArrow;
			graphPanel.repaint();
		}
	}
	
	public void toggleBorders() {
		borderOrientation = !borderOrientation;
		showHints = borderOrientation || jumpingArrow;
		graphPanel.repaint();
	}
	
	public boolean getShowHints() {
		return showHints;
	}

	public Image getImage(String imagefile) {
			URL imgURL = getClass().getResource(imagefile);
			ImageIcon ii;
			Image img = null;
			if (imgURL == null) {
				imgURL = getClass().getClassLoader().getResource(imagefile);
			}
			if (imgURL == null) {
				System.out.println("Image " + imagefile + " not loaded");
	//			controler.displayPopup("Image " + imagefile + " not loaded");
			} else {
				ii = new ImageIcon(imgURL);
				img = ii.getImage();
			}
			return img;
		}

	private boolean showTopImage() {
		return !isEmpty() && bounds.y + graphPanel.getTranslation().y < 0;
	}

	private boolean showBottomImage() {
		return !isEmpty() && bounds.y + bounds.height + graphPanel.getTranslation().y > height;
	}

	private boolean showLeftImage() {
		return !isEmpty() && bounds.x + graphPanel.getTranslation().x < 0;
	}

	private boolean showRightImage() {
		return !isEmpty() && bounds.x + bounds.width + graphPanel.getTranslation().x > width;
	}

	private boolean isEmpty() {
		//	TODO decide where
		return graphPanel.isEmpty();
//		return nodes.size() == 0;
	}
	
	
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
	}
	public void setDimension(Dimension s) {
		this.width = s.width;
		this.height = s.height;
	}
	
	public BufferedImage snapShot() {
		graphPanel.graphSelected();
		graphPanel.normalize();
		Dimension s = graphPanel.getSize();		// Fix for H5P export
		setDimension(s);
		int imgWidth = Math.min(bounds.width + 200, width);
		int imgHeight = Math.min(bounds.height + 200, height);
		BufferedImage bufferedImage = new BufferedImage(imgWidth, 
				imgHeight, BufferedImage.TYPE_BYTE_INDEXED);
		Graphics2D g = bufferedImage.createGraphics();
		graphPanel.print(g);
		return bufferedImage;
	}

}

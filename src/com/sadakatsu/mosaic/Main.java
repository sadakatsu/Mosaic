package com.sadakatsu.mosaic;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.sadakatsu.mosaic.MosaicData.SplitType;
import com.sadakatsu.mosaic.gui.MosaicApplication;
import com.sadakatsu.mosaic.renderer.AbstractMosaicRenderer;
import com.sadakatsu.mosaic.renderer.BasicDebugRenderer;
import com.sadakatsu.mosaic.renderer.BasicRenderer;
import com.sadakatsu.mosaic.renderer.FilledWireframeRenderer;

public class Main {
	public static void main(String[] args) throws Exception {
		//*
		SwingUtilities.invokeLater(
			new Runnable() {
				@Override
				public void run() {
					new MosaicApplication();
				}
			}
		);
		//*/
		
		/*
		try {
			BufferedImage image = ImageIO.read(
				new File("waterLiliesAndJapaneseBridge.jpg") // "starryNight.jpg")
			);
			
			MosaicData data = MosaicData.load(image, 0.75);
			System.out.println(data.getPolygonCount(SplitType.BOTH));
			
			AbstractMosaicRenderer renderer;
			BufferedImage result;
			
			renderer = new BasicRenderer();
			result = renderer.render(
				data,
				SplitType.BOTH,
				data.getPolygonCount(SplitType.BOTH)
			);
			ImageIO.write(result, "PNG", new File("results/basic.png"));
			
			renderer = new FilledWireframeRenderer();
			result = renderer.render(
				data,
				SplitType.BOTH,
				data.getPolygonCount(SplitType.BOTH)
			);
			ImageIO.write(result, "PNG", new File("results/filledWireframe.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		//*/
		
		/*
		BufferedImage image = new BufferedImage(12, 10, BufferedImage.TYPE_3BYTE_BGR);
		java.awt.Graphics g = image.getGraphics();
		
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(0, 0, 12, 10);
		
		g.setColor(java.awt.Color.BLACK);
		java.awt.Polygon p = new java.awt.Polygon();
		p.addPoint(3, 1);
		p.addPoint(7 + 1, 1);
		p.addPoint(10 + 1, 4);
		p.addPoint(10, 7);
		p.addPoint(11, 8);
		p.addPoint(5, 8);
		p.addPoint(1, 4);
		p.addPoint(1, 3);
		
		g.fillPolygon(p);
		
		ImageIO.write(image, "PNG", new File("results/test.png"));
		//*/
	}
}

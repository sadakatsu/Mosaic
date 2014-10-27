package com.sadakatsu.mosaic.renderer;

import java.awt.Graphics;

import javax.swing.JPanel;

import com.sadakatsu.mosaic.MosaicData;
import com.sadakatsu.mosaic.MosaicData.SplitType;
import com.sadakatsu.mosaic.Region;

public class BasicDebugRenderer extends AbstractMosaicRenderer {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	/*--------------------------- Types and Enums ----------------------------*/
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	/*--------------------------- Types and Enums ----------------------------*/
	/*----------------------------- Constructors -----------------------------*/
	public BasicDebugRenderer(WhenDone hook) {
		super(hook);
	}
	
	/*------------------------------ Interface -------------------------------*/
	@Override
	public RendererControlPanel getControls() {
		return null;
	}
	
	@Override
	public RenderWorker buildThread(
		MosaicData data,
		SplitType method,
		int count
	) {
		return new RenderWorker(data, method, count) {
			@Override
			protected void performRender() {
				Graphics graphics = image.getGraphics();
				
				for (Region region : data.getRegions(method, count)) {
					if (isStopped()) {
						break;
					}
					
					region.drawOnto(graphics);
				}
			}
		};
	}
	
	@Override
	public String toString() {
		return "Debug";
	}
	
	/*------------------------------- Helpers --------------------------------*/
}
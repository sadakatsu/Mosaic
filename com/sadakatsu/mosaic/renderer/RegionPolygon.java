package com.sadakatsu.mosaic.renderer;

import java.awt.Polygon;

import com.sadakatsu.mosaic.Region;

public class RegionPolygon extends Polygon {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	private static final long serialVersionUID = 3872639748914539249L;
	
	/*--------------------------- Types and Enums ----------------------------*/
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	/*--------------------------- Types and Enums ----------------------------*/
	/*----------------------------- Constructors -----------------------------*/
	public RegionPolygon(Region region) {
		int bottom = region.getBottom();
		int ll = region.getLL();
		int lr = region.getLR();
		int right = region.getRight();
		int ul = region.getUL();
		int ur = region.getUR();
		int x = region.getX();
		int y = region.getY();
		
		int[] xes = {
			x + ul,
			right - ur + 1,
			right + 1,
			right + 1,
			right - lr,
			x + ll + 1,
			x,
			x
		};
		
		int[] yes = {
			y,
			y,
			y + ur,
			bottom - lr,
			bottom + 1,
			bottom + 1,
			bottom - ll,
			y + ul
		};
		
		npoints = 0;
		xpoints = new int[9];
		ypoints = new int[9];
		for (int i = 0; i < xes.length; ++i) {
			if (
				i == 0 ||
				xpoints[npoints - 1] != xes[i] ||
				ypoints[npoints - 1] != yes[i]
			) {
				addPoint(xes[i], yes[i]);
			}
		}
	}
	
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
}

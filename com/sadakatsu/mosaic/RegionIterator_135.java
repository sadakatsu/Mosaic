package com.sadakatsu.mosaic;

import java.awt.Point;
import java.awt.image.BufferedImage;

import com.sadakatsu.util.Pair;

public class RegionIterator_135 implements RegionIterator {
	/******************************** INSTANCE ********************************/
	/***************************** Private Fields *****************************/
	private BufferedImage image;
	
	private int leftX;
	private int leftY;
	private int rightX;
	private int rightY;
	
	private Region bottom;
	private Region slice;
	private Region source;
	private Region top;
	
	/****************************** Constructors ******************************/
	public RegionIterator_135(Region toIterate, BufferedImage reference) {
		image = reference;
		
		leftX = 0;
		leftY = toIterate.getHeight() - toIterate.getLL() - 1;
		rightX = toIterate.getLL();
		rightY = toIterate.getHeight() - 1;
		
		bottom = new Region();
		slice = new Region();
		source = toIterate;
		top = new Region(toIterate);
	}
	
	/**************************** Observer Methods ****************************/
	@Override
	public boolean hasNext() {
		return rightY > source.getUR();
	}
	
	/************************** Transformer Methods ***************************/
	@Override
	public Pair<Region, Region> next() {
		// Figure out the dimensions and data for the slice.
		Pair<Point, Point> corners = getSliceCorners();
		Point left = corners.getFirst();
		Point right = corners.getSecond();
		
		int d = right.x - left.x;
		
		// Load the slice.
		try {
			slice.loadFrom(
				image,
				source.getX() + left.x,
				source.getY() + left.y,
				d + 1,
				d + 1,
				0,
				d,
				d,
				0,
				Region.Orientation._135
			);
		} catch (InvalidDimensionsException e1) {
			e1.printStackTrace();
			throw new CatastrophicError();
		}
		
		// Update the two Regions to reflect this step of the iteration.
		try {
			bottom.add(slice);
			top.subtract(slice);
		} catch (InvalidDimensionsException | NotASliceException e) {
			e.printStackTrace();
			throw new CatastrophicError();
		}
		
		// Update the next slice's initial indices.
		incrementIndices();
		
		// Return the two Regions for this step of the iteration.
		return Pair.make(new Region(bottom), new Region(top));
	}
	
	/***************************** Helper Methods *****************************/
	private Pair<Point, Point> getSliceCorners() {
		int rightNegativeX = source.getWidth() - rightX - 1;
		int rightNegativeY = source.getHeight() - rightY - 1;
		
		int x1 = leftX;
		int y1 = leftY;
		int x2 = rightX;
		int y2 = rightY;
		
		if (leftY + leftX < source.getUL()) {
			int diff = source.getUL() - leftX - leftY;
			int steps = diff / 2 + diff % 2;
			x1 += steps;
			y1 += steps;
		}
		
		if (rightNegativeX + rightNegativeY < source.getLR()) {
			int diff = source.getLR() - rightNegativeX - rightNegativeY;
			int steps = diff / 2 + diff % 2;
			x2 -= steps;
			y2 -= steps;
		}
		
		return Pair.make(new Point(x1, y1), new Point(x2, y2));
	}
	
	private void incrementIndices() {
		boolean perform = true;
		
		while (perform) {
			if (leftY == 0) {
				leftX++;
			} else {
				leftY--;
			}
			
			if (rightX == source.getWidth() - 1) {
				rightY--;
			} else {
				rightX++;
			}
			
			Pair<Point, Point> corners = getSliceCorners();
			Point left = corners.getFirst();
			Point right = corners.getSecond();
			
			perform = (hasNext() && left.x > right.x);
		}
	}
}

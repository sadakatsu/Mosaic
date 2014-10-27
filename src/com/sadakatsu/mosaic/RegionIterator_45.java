package com.sadakatsu.mosaic;

import java.awt.Point;
import java.awt.image.BufferedImage;

import com.sadakatsu.util.Pair;

public class RegionIterator_45 implements RegionIterator {
	/******************************** INSTANCE ********************************/
	/***************************** Private Fields *****************************/
	private BufferedImage image;
	
	private int leftX;
	private int leftY;
	private int rightX;
	private int rightY;
	
	private Region top;
	private Region bottom;
	private Region slice;
	private Region source;
	
	/****************************** Constructors ******************************/
	public RegionIterator_45(Region toIterate, BufferedImage reference) {
		image = reference;
		top = new Region();
		bottom = new Region(toIterate);
		slice = new Region();
		source = toIterate;
		
		leftX = 0;
		leftY = source.getUL();
		rightX = source.getUL();
		rightY = 0;
	}
	
	/**************************** Observer Methods ****************************/
	@Override
	public boolean hasNext() {
		return source.getWidth() - leftX > source.getLR() + 1;
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
				source.getY() + right.y,
				d + 1,
				d + 1,
				d,
				0,
				0,
				d,
				Region.Orientation._45
			);
		} catch (InvalidDimensionsException e1) {
			e1.printStackTrace();
			throw new CatastrophicError();
		}
		
		// Update the two Regions to reflect this step of the iteration.
		try {
			top.add(slice);
			bottom.subtract(slice);
		} catch (InvalidDimensionsException | NotASliceException e) {
			e.printStackTrace();
			throw new CatastrophicError();
		}
		
		// Update the next slice's initial indices.
		incrementIndices();
		
		// Return the two Regions for this step of the iteration.
		return Pair.make(new Region(top), new Region(bottom));
	}
	
	/***************************** Helper Methods *****************************/
	private Pair<Point, Point> getSliceCorners() {
		int leftNegativeY = source.getHeight() - leftY - 1;
		int rightNegativeX = source.getWidth() - rightX - 1;
		
		int x1 = leftX;
		int y1 = leftY;
		int x2 = rightX;
		int y2 = rightY;
		
		if (leftNegativeY + leftX < source.getLL()) {
			int diff = source.getLL() - leftNegativeY - leftX;
			int steps = diff / 2 + diff % 2;
			x1 += steps;
			y1 -= steps;
		}
		
		if (rightNegativeX + rightY < source.getUR()) {
			int diff = source.getUR() - rightNegativeX - rightY;
			int steps = diff / 2 + diff % 2;
			x2 -= steps;
			y2 += steps;
		}
		
		return Pair.make(new Point(x1, y1), new Point(x2, y2));
	}
	
	private void incrementIndices() {
		boolean perform = true;
		
		while (perform) {
			if (leftY == source.getHeight() - 1) {
				leftX++;
			} else {
				leftY++;
			}
			
			if (rightX == source.getWidth() - 1) {
				rightY++;
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

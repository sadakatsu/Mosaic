package com.sadakatsu.mosaic;

import java.awt.image.BufferedImage;

import com.sadakatsu.util.Pair;

public class RegionIterator_90 implements RegionIterator {
	/******************************** INSTANCE ********************************/
	/***************************** Private Fields *****************************/
	private BufferedImage image;
	
	private int i = 0;
	
	private Region left;
	private Region right;
	private Region slice;
	private Region source;
	
	/****************************** Constructors ******************************/
	public RegionIterator_90(Region toIterate, BufferedImage reference) {
		image = reference;
		left = new Region();
		right = new Region(toIterate);
		slice = new Region();
		source = toIterate;
	}
	
	/**************************** Observer Methods ****************************/
	@Override
	public boolean hasNext() {
		return i < source.getWidth() - 1;
	}

	/************************** Transformer Methods ***************************/
	@Override
	public Pair<Region, Region> next() {
		int minY = source.getY();
		int maxY = source.getY() + source.getHeight();
		int ni = source.getWidth() - i - 1;
		
		if (i < source.getUL()) {
			minY += source.getUL() - i;
		} else if (ni < source.getUR()) {
			minY += source.getUR() - ni;
		}
		
		if (i < source.getLL()) {
			maxY -= source.getLL() - i;
		} else if (ni < source.getLR()) {
			maxY -= source.getLR() - ni;
		}
		
		try {
			slice.loadFrom(
				image,
				i + source.getX(),
				minY,
				1,
				maxY - minY,
				0,
				0,
				0,
				0,
				Region.Orientation._90
			);
		} catch (InvalidDimensionsException e1) {
			e1.printStackTrace();
			throw new CatastrophicError();
		}
		
		try {
			left.add(slice);
			right.subtract(slice);
		} catch (InvalidDimensionsException | NotASliceException e) {
			e.printStackTrace();
			throw new CatastrophicError();
		}
		
		
		i++;
		return Pair.make(new Region(left), new Region(right));
	}
}

package com.sadakatsu.mosaic;

import java.awt.image.BufferedImage;

import com.sadakatsu.util.Pair;

public class RegionIterator_0 implements RegionIterator {
	/******************************** INSTANCE ********************************/
	/***************************** Private Fields *****************************/
	private BufferedImage image;
	
	private int j = 0;
	
	private Region top;
	private Region bottom;
	private Region slice;
	private Region source;
	
	/****************************** Constructors ******************************/
	public RegionIterator_0(Region toIterate, BufferedImage reference) {
		image = reference;
		top = new Region();
		bottom = new Region(toIterate);
		slice = new Region();
		source = toIterate;
	}
	
	/**************************** Observer Methods ****************************/
	@Override
	public boolean hasNext() {
		return j < source.getHeight() - 1;
	}

	/************************** Transformer Methods ***************************/
	@Override
	public Pair<Region, Region> next() {
		int minX = source.getX();
		int maxX = source.getX() + source.getWidth();
		int nj = source.getHeight() - j - 1;
		
		if (j < source.getUL()) {
			minX += source.getUL() - j;
		} else if (nj < source.getLL()) {
			minX += source.getLL() - nj;
		}
		
		if (j < source.getUR()) {
			maxX -= source.getUR() - j;
		} else if (nj < source.getLR()) {
			maxX -= source.getLR() - nj;
		}
		
		try {
			slice.loadFrom(
				image,
				minX,
				j + source.getY(),
				maxX - minX,
				1,
				0,
				0,
				0,
				0,
				Region.Orientation._0
			);
		} catch (InvalidDimensionsException e1) {
			e1.printStackTrace();
			throw new CatastrophicError();
		}
		
		try {
			top.add(slice);
			bottom.subtract(slice);
		} catch (InvalidDimensionsException | NotASliceException e) {
			e.printStackTrace();
			throw new CatastrophicError();
		}
		
		
		j++;
		return Pair.make(new Region(top), new Region(bottom));
	}
}

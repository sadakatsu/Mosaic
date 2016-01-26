package com.sadakatsu.mosaic;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

public class Region {
	/******************************** INSTANCE ********************************/
	/***************************** Private Fields *****************************/
	private boolean varianceCalculated = false;
	
	private double variance = 0;
	
	private int hashCode = 0;
	private int height = 0;
	private int ll = 0;
	private int lr = 0;
	private int ul = 0;
	private int ur = 0;
	private int width = 0;
	private int x = 0;
	private int y = 0;
	
	private long area = 0;
	
	private long sumRGB[] = {0, 0, 0};
	private long sumRGBProducts[] = {0, 0, 0, 0, 0, 0};
	
	private Orientation orientation = null;
	
	// DEBUG:
	// private Region before = null;
	
	/****************************** Constructors ******************************/
	public Region() {}
	
	public Region(Region that) {
		copy(that);
	}
	
	/**************************** Observer Methods ****************************/
	@Override
	public boolean equals(Object other) {
		boolean result = false;
		
		if (other instanceof Region) {
			Region that = (Region) other;
			result = (
				this.x == that.x &&
				this.y == that.y &&
				this.width == that.width &&
				this.height == that.height &&
				this.ul == that.ul &&
				this.ur == that.ur &&
				this.ll == that.ll &&
				this.lr == that.lr
			);
			
			for (int i = 0; result && i < sumRGBProducts.length; ++i) {
				if (i < sumRGB.length) {
					result = (this.sumRGB[i] == that.sumRGB[i]);
				}
				
				if (result) {
					result = (this.sumRGBProducts[i] == that.sumRGBProducts[i]);
				}
			}
		}
		
		return result;
	}
	
	public Color getColor() {
		double scale = 1. / (area * 255);
		float r = (float) (sumRGB[0] * scale);
		float g = (float) (sumRGB[1] * scale);
		float b = (float) (sumRGB[2] * scale);
		return new Color(r, g, b);
	}
	
	public double getGrayscale() {
		// There is a more precise formula, but I have chosen to use this one
		// due to its relative simplicity in calculating component-wise
		// variance.
		Color color = getColor();
		return (
			color.getRed() * RED +
			color.getGreen() * GREEN +
			color.getBlue() * BLUE
		);
	}
	
	public double getVariance() {
		if (!varianceCalculated) {
			calculateVariance();
		}
		
		return variance;
	}
	
	public int getBottom() {
		return y + height - 1;
	}

	public int getHeight() {
		return height;
	}
	
	public int getLL() {
		return ll;
	}
	
	public int getLR() {
		return lr;
	}
	
	public int getRight() {
		return x + width - 1;
	}
	
	public int getUL() {
		return ul;
	}
	
	public int getUR() {
		return ur;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getWidth() {
		return width;
	}
	
	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = ul;
			hashCode = hashCode * 31 + ur;
			hashCode = hashCode * 31 + ll;
			hashCode = hashCode * 31 + lr;
			hashCode = hashCode * 31 + x;
			hashCode = hashCode * 31 + y;
			hashCode = hashCode * 31 + width;
			hashCode = hashCode * 31 + height;
			for (int i = 0; i < sumRGBProducts.length; ++i) {
				hashCode = hashCode * 31 + (int) sumRGBProducts[i];
			}
			for (int i = 0; i < sumRGB.length; ++i) {
				hashCode = hashCode * 31 + (int) sumRGB[i];
			}
		}
		return hashCode;
	}
	
	public long getArea() {
		return area;
	}
	
	public Polygon getPolygon() {
		int[] xes = {
			x + ul,
			getRight() - ur,
			getRight(),
			getRight(),
			getRight() - lr,
			x + ll,
			x,
			x
		};
		int[] yes = {
			y,
			y,
			y + ur,
			getBottom() - lr,
			getBottom(),
			getBottom(),
			getBottom() - ll,
			y + ul
		};
		
		int[] keptXes = new int[8];
		int[] keptYes = new int[8];
		int length = 0;
		for (int i = 0; i < 8; ++i) {
			if (
				length == 0 ||
				keptXes[length - 1] != xes[i] ||
				keptYes[length - 1] != yes[i]
			) {
				keptXes[length] = xes[i];
				keptYes[length] = yes[i];
				length++;
			}
		}
		
		return new Polygon(keptXes, keptYes, length);
	}
	
	@Override
	public String toString() {
		return String.format(
			"{ (%d, %d), %d x %d - (%d : %d : %d : %d) = %d | %s | %f }",
				x,
				y,
				width,
				height,
				ul,
				ur,
				ll,
				lr,
				area,
				getColor(),
				getVariance()
		);
	}
	
	// DEBUG:
	public void drawOnto(Graphics graphics) {
		graphics.setColor(getColor());
		forEach(
			(i, j) -> {
				graphics.fillRect(x + i, y + j, 1, 1);
			}
		);
	}
	
	/************************** Transformer Methods ***************************/
	public void add(Region slice)
	throws NotASliceException, InvalidDimensionsException {
		// DEBUG:
		// before = new Region(this);
		
		if (slice.orientation == null) {
			throw new NotASliceException();
		} else if (area == 0) {
			copy(slice);
			orientation = null;
			return;
		} else if (slice.orientation == Orientation._0) {
			addSlice0(slice);
		} else if (slice.orientation == Orientation._45) {
			addSlice45(slice);
		} else if (slice.orientation == Orientation._90) {
			addSlice90(slice);
		} else {
			addSlice135(slice);
		}
		
		addPixelData(slice);
		
		validateDimensions(width, height, ul, ur, ll, lr);
	}
	
	public void copy(Region that) {
		this.area = that.area;
		this.height = that.height;
		this.ll = that.ll;
		this.lr = that.lr;
		this.orientation = that.orientation;
		this.ul = that.ul;
		this.ur = that.ur;
		this.variance = that.variance;
		this.varianceCalculated = that.varianceCalculated;
		this.width = that.width;
		this.x = that.x;
		this.y = that.y;
		
		for (int i = 0; i < sumRGBProducts.length; ++i) {
			if (i < sumRGB.length) {
				this.sumRGB[i] = that.sumRGB[i];
			}
			
			this.sumRGBProducts[i] = that.sumRGBProducts[i];
		}
	}
	
	public void loadFrom(BufferedImage image) {
		try {
			loadFrom(
				image,
				0,
				0,
				image.getWidth(),
				image.getHeight(),
				0,
				0,
				0,
				0,
				null
			);
		} catch (InvalidDimensionsException e) {
			throw new CatastrophicError(e);
		}
	}

	public void loadFrom(
		BufferedImage image,
		int x,
		int y,
		int width,
		int height,
		int ul,
		int ur,
		int ll,
		int lr,
		Orientation orientation
	) throws InvalidDimensionsException {
		validateDimensions(width, height, ul, ur, ll, lr);
		
		reset();
		
		this.height = height;
		this.ll = ll;
		this.lr = lr;
		this.orientation = orientation;
		this.ul = ul;
		this.ur = ur;
		this.width = width;
		this.x = x;
		this.y = y;
		
		forEach(
			(i, j) -> {
				Color color = getColorAt(image, i + x, j + y);
				long r = color.getRed();
				long g = color.getGreen();
				long b = color.getBlue();
				
				sumRGB[0] += r;
				sumRGB[1] += g;
				sumRGB[2] += b;
				
				sumRGBProducts[0] += r * r;
				sumRGBProducts[1] += r * g;
				sumRGBProducts[2] += r * b;
				sumRGBProducts[3] += g * g;
				sumRGBProducts[4] += g * b;
				sumRGBProducts[5] += b * b;
			}
		);
		
		calculateArea();
		this.varianceCalculated = false;
	}
	
	public void reset() {
		area = 0;
		height = 0;
		ll = 0;
		lr = 0;
		orientation = null;
		ul = 0;
		ur = 0;
		variance = 0;
		varianceCalculated = false;
		width = 0;
		x = 0;
		y = 0;
		
		for (int i = 0; i < sumRGBProducts.length; ++i) {
			if (i < sumRGB.length) {
				sumRGB[i] = 0;
			}
			
			sumRGBProducts[i] = 0;
		}
	}
	
	public void subtract(Region slice)
	throws NotASliceException, InvalidDimensionsException {
		// DEBUG:
		// before = new Region(this);
		
		if (slice.orientation == Orientation._0) {
			subtractSlice0(slice);
		} else if (slice.orientation == Orientation._45) {
			subtractSlice45(slice);
		} else if (slice.orientation == Orientation._90) {
			subtractSlice90(slice);
		} else if (slice.orientation == Orientation._135) {
			subtractSlice135(slice);
		} else {
			throw new NotASliceException();
		}
		
		subtractPixelData(slice);
		
		validateDimensions(width, height, ul, ur, ll, lr);
	}
	
	/***************************** Helper Methods *****************************/
	private Color getColorAt(BufferedImage image, int x, int y) {
		try {
			return new Color(image.getRGB(x, y));
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private int calculateCornerCutArea(int length) {
		return length * (length + 1) / 2;
	}
	
	private void addPixelData(Region slice) {
		// DEBUG:
		/*
		calculateArea();
		if (area != before.getArea() + slice.getArea()) {
			System.out.format(
				"ERROR: Areas do not add up:\n\t  %s\n\t+ %s\n\t= %s\n",
					before,
					slice,
					this
			);
			throw new CatastrophicError();
		}
		//*/
		
		// NOT DEBUG:
		area += slice.area;
		
		
		for (int i = 0; i < sumRGBProducts.length; ++i) {
			if (i < sumRGB.length) {
				sumRGB[i] += slice.sumRGB[i];
			}
			
			sumRGBProducts[i] += slice.sumRGBProducts[i];
		}
	}
	
	private void addSlice0(Region slice) {
		int thisRight = getRight();
		int thatRight = slice.getRight();
		
		if (x == slice.x) {
			if (thisRight < thatRight) {
				ur++;
				width++;
			} else if (thisRight > thatRight) {
				lr++;
			}
		} else if (x > slice.x) {
			if (thisRight < thatRight) {
				ur++;
				width += 2;
			} else {
				if (thisRight > thatRight) {
					lr++;
				}
				
				width++;
			}
			
			ul++;
			x = slice.x;
		} else {
			if (thisRight < thatRight) {
				ur++;
				width++;
			} else if (thisRight > thatRight) {
				lr++;
			}
			
			ll++;
		}
		
		height++;
	}
	
	private void addSlice45(Region slice) {
		// It is possible to categorize all the outcomes of diagonal iteration
		// by comparing the Region's X, Y, bottom-edge, and right-edge coords
		// against the slice's.  While there are sixteen different possibilities
		// using these states, only seven of them are legal.  Using a succint
		// notation for all the comparison results and their necessary changes,
		// we get:
		//
		// Xs, Ys, Rs, Bs : ~
		// Xs, Ys, Rs, Bd : ~
		// Xs, Ys, Rd, Bs : ~
		// Xs, Ys, Rd, Bd : W, H, LR increment.
		// Xs, Yd, Rs, Bs : ~
		// Xs, Yd, Rs, Bd : H increment.
		// Xs, Yd, Rd, Bs : ~
		// Xs, Yd, Rd, Bd : ~
		// Xd, Ys, Rs, Bs : ~
		// Xd, Ys, Rs, Bd : ~
		// Xd, Ys, Rd, Bs : W increment.
		// Xd, Ys, Rd, Bd : ~
		// Xd, Yd, Rs, Bs : LR decrement.
		// Xd, Yd, Rs, Bd : H, LL increment.
		// Xd, Yd, Rd, Bs : W, UR increment.
		// Xd, Yd, Rd, Bd : W, H, UR, LL, LR increment.
		//
		// NOTE: There is a special case for adding a single diagonal pixel
		// above and to the right of the Region that occurs when iterating over
		// a 135-degree slice.  Technically, this is a "Xd, Yd, Rd, Bd" case,
		// but it should NOT increment the UR-corner.  To account for this, the
		// code has this handled as a special case.  However, to clearly explain
		// the reasoning of this algorithm, only the general case is explained
		// below.
		//
		// Each of the legal outcomes has one or more changes that need to be
		// made to the Region's dimensions to account for adding the slice.
		// Each of these changes occurs iff specific comparison results are
		// obtained.  In addition, these actions are only mutually exclusive
		// when their comparison results are mutually exclusive.  Consider the
		// actions and their comparison conditions below:
		//
		// - LR decrement: bottom-edge and right-edge coords are the same
		// - LR increment: bottom-edge and right-edge coords are different
		// - H increment: bottom-edge coords are different
		// - LL increment: bottom-edge and X coords are different
		// - W increment: right-edge coords are different
		// - UR increment: right-edge and Y coords are different
		//
		// The decision tree written in this method uses this logic to combine
		// actions whenever their conditions hold, and therefore results in the
		// exact actions shown in the first listing.
		 
		boolean bDiff = (getBottom() != slice.getBottom());
		boolean rDiff = (getRight() != slice.getRight());
		boolean xDiff = (x != slice.x);
		boolean yDiff = (y != slice.y);
		
		if (!(bDiff || rDiff)) {
			lr--;
		} else {
			if (bDiff) {
				height++;
				
				if (xDiff) {
					ll++;
				}
			}
			
			if (rDiff) {
				width++;
				
				if (yDiff) {
					ur++;
				}
			}
			
			if (bDiff && rDiff && slice.getLR() > 0) {
				lr++;
			}
		}
	}
	
	private void addSlice90(Region slice) {
		int thisBottom = getBottom();
		int thatBottom = slice.getBottom();
		
		if (y == slice.y) {
			if (thisBottom < thatBottom) {
				height++;
				ll++;
			} else if (thisBottom > thatBottom) {
				lr++;
			}
		} else if (y > slice.y) {
			if (thisBottom < thatBottom) {
				height += 2;
				ll++;
			} else {
				if (thisBottom > thatBottom) {
					lr++;
				}
				
				height++;
			}
			
			ul++;
			y = slice.y;
		} else {
			if (thisBottom < thatBottom) {
				height++;
				ll++;
			} else if (thisBottom > thatBottom) {
				lr++;
			}
			
			ur++;
		}
		
		width++;
	}
	
	private void addSlice135(Region slice) {
		// It is possible to categorize all the outcomes of diagonal iteration
		// by comparing the Region's X, Y, bottom-edge, and right-edge coords
		// against the slice's.  While there are sixteen different possibilities
		// using these states, only seven of them are legal.  Using a succint
		// notation for all the comparison results and their necessary changes,
		// we get:
		//
		// Xs, Ys, Rs, Bs : ~
		// Xs, Ys, Rs, Bd : ~
		// Xs, Ys, Rd, Bs : ~
		// Xs, Ys, Rd, Bd : ~
		// Xs, Yd, Rs, Bs : ~
		// Xs, Yd, Rs, Bd : Increment H.  Decrement Y.
		// Xs, Yd, Rd, Bs : Increment W, H, UR.  Decrement Y.
		// Xs, Yd, Rd, Bd : ~
		// Xd, Ys, Rs, Bs : ~
		// Xd, Ys, Rs, Bd : Decrement UR.
		// Xd, Ys, Rd, Bs : Increment W.
		// Xd, Ys, Rd, Bd : Increment W, LR.
		// Xd, Yd, Rs, Bs : ~
		// Xd, Yd, Rs, Bd : Increment H, UL.  Decrement Y.
		// Xd, Yd, Rd, Bs : ~
		// Xd, Yd, Rd, Bd : Increment W, H, UL, UR, LR.  Decrement Y.
		//
		// NOTE: There is a special case for adding a single diagonal pixel
		// above and to the right of the Region that occurs when iterating over
		// a 45-degree slice.  Technically, this is a "Xd, Yd, Rd, Bd" case, but
		// it should NOT increment the UR-corner.  To account for this, the code
		// has this handled as a special case.  However, to clearly explain the
		// reasoning of this algorithm, only the general case is explained
		// below.
		//
		// Each of the legal outcomes has one or more changes that need to be
		// made to the Region's dimensions to account for adding the slice.
		// Each of these changes occurs iff specific comparison results are
		// obtained.  In addition, these actions are only mutually exclusive
		// when their comparison results are mutually exclusive.  Consider the
		// actions and their comparison conditions below:
		//
		// - Y decrement and H increment: Y coords are different
		// - W increment: right-edge coords are different
		// - UR increment: Y and right-edge coords are different
		// - UR decrement: Y and right-edge coords are same
		// - UL increment: X and Y coords are different
		// - LR increment: bottom-edge and right-edge coords are different
		// 
		//
		// The decision tree written in this method uses this logic to combine
		// actions whenever their conditions hold, and therefore results in the
		// exact actions shown in the first listing.
		 
		boolean bDiff = (getBottom() != slice.getBottom());
		boolean rDiff = (getRight() != slice.getRight());
		boolean xDiff = (x != slice.x);
		boolean yDiff = (y != slice.y);
		
		if (!(rDiff || yDiff)) {
			ur--;
		} else {
			if (yDiff) {
				height++;
				y--;
				
				if (rDiff && slice.getUR() > 0) {
					ur++;
				}
				
				if (xDiff) {
					ul++;
				}
			}
			
			if (rDiff) {
				width++;
				
				if (bDiff) {
					lr++;
				}
			}
		}
	}
	
	private void calculateArea() {
		long overall = width * height;
		long ulCut = calculateCornerCutArea(ul);
		long urCut = calculateCornerCutArea(ur);
		long llCut = calculateCornerCutArea(ll);
		long lrCut = calculateCornerCutArea(lr);
		
		area = overall - ulCut - urCut - llCut - lrCut;
		
		/*
		area = (
			width * height -
			calculateCornerCutArea(ul) -
			calculateCornerCutArea(ur) -
			calculateCornerCutArea(ll) -
			calculateCornerCutArea(lr)
		);
		//*/
	}
	
	private void calculateVariance() {
		double n = 1. / area;
		
		long r = sumRGB[0];
		long g = sumRGB[1];
		long b = sumRGB[2];
		
		long rr = sumRGBProducts[0];
		long rg = sumRGBProducts[1];
		long rb = sumRGBProducts[2];
		long gg = sumRGBProducts[3];
		long gb = sumRGBProducts[4];
		long bb = sumRGBProducts[5];
		
		variance = n * (
			RR * rr + RG * rg + RB * rb + GG * gg + GB * gb + BB * bb -
			n * (
				RR * r * r +
				RG * r * g +
				RB * r * b +
				GG * g * g +
				GB * g * b +
				BB * b * b
			)
		);
	}
	
	private void forEach(PerPixel algorithm) {
		for (int j = 0; j < height; ++j) {
			int nj = height - 1 - j;
			
			int minX;
			if (j < ul) {
				minX = ul - j;
			} else if (nj < ll) {
				minX = ll - nj;
			} else {
				minX = 0;
			}
			
			int maxX = width;
			if (j < ur) {
				maxX -= ur - j;
			} else if (nj < lr) {
				maxX -= lr - nj;
			}
			
			for (int i = minX; i < maxX; ++i) {
				algorithm.perform(i, j);
			}
		}
	}
	
	private void subtractPixelData(Region slice) {
		// DEBUG:
		/*
		calculateArea();
		if (area != before.getArea() - slice.getArea()) {
			System.out.format(
				"ERROR: Areas do not add up:\n\t  %s\n\t- %s\n\t= %s\n",
					before,
					slice,
					this
			);
			throw new CatastrophicError();
		}
		//*/
		
		// NOT DEBUG:
		area -= slice.getArea();
		
		for (int i = 0; i < sumRGBProducts.length; ++i) {
			if (i < sumRGB.length) {
				sumRGB[i] -= slice.sumRGB[i];
			}
			
			sumRGBProducts[i] -= slice.sumRGBProducts[i];
		}
	}
	
	private void subtractSlice0(Region slice) {
		if (height - ll == 1) {
			x += 1;
			width -= 1;
			ll--;
		}
		
		if (height - lr == 1) {
			width -= 1;
			lr--;
		}
		
		height -= 1;
		y += 1;
		
		if (ul > 0) {
			ul--;
		}
		
		if (ur > 0) {
			ur--;
		}
	}
	
	private void subtractSlice45(Region slice) {
		int originalWidth = width;
		
		boolean changedLeft = false;
		if (slice.height + ll == height) {
			changedLeft = true;
			width--;
			x++;
			if (ll > 0) {
				ll--;
			}
		}
		
		boolean changedTop = false;
		if (slice.width + ur == originalWidth) {
			changedTop = true;
			height--;
			y++;
			if (ur > 0) {
				ur--;
			}
		}
		
		if (changedLeft && changedTop) {
			if (ul > 0) {
				ul--;
			}
		} else if (!(changedLeft || changedTop)) {
			ul++;
		}
	}
	
	private void subtractSlice90(Region slice) {
		if (width - ur == 1) {
			y += 1;
			height -= 1;
			ur--;
		}
		
		if (width - lr == 1) {
			height -= 1;
			lr--;
		}
		
		width -= 1;
		x += 1;
		
		if (ul > 0) {
			ul--;
		}
		
		if (ll > 0) {
			ll--;
		}
	}
	
	private void subtractSlice135(Region slice) {
		int originalWidth = width;
		
		boolean changedLeft = false;
		if (slice.height + ul == height) {
			changedLeft = true;
			width--;
			x++;
			if (ul > 0) {
				ul--;
			}
		}
		
		boolean changedBottom = false;
		if (slice.width + lr == originalWidth) {
			changedBottom = true;
			height--;
			if (lr > 0) {
				lr--;
			}
		}
		
		if (changedBottom && changedLeft) {
			if (ll > 0) {
				ll--;
			}
		} else if (!(changedBottom || changedLeft)) {
			ll++;
		}
	}
	
	private void validateDimensions(
		int width,
		int height,
		int ul,
		int ur,
		int ll,
		int lr
	) throws InvalidDimensionsException {
		if (
			width < 0 ||
			height < 0 ||
			ll < 0 ||
			lr < 0 ||
			ul < 0 ||
			ur < 0 ||
			width - ul - ur < 1 ||
			width - ll - lr < 1 ||
			height - ul - ll < 1 ||
			height - ur - lr < 0
		) {
			throw new InvalidDimensionsException(width, height, ul, ur, ll, lr);
		}
	}
	
	/********************************* CLASS **********************************/
	/*************************** Private Constants ****************************/
	private final double BLUE = 0.0722;
	private final double GREEN = 0.7512;
	private final double RED = 0.2126;
	
	private final double RR = RED * RED;
	private final double RG = RED * GREEN * 2;
	private final double RB = RED * BLUE * 2;
	private final double GG = GREEN * GREEN;
	private final double GB = GREEN * BLUE * 2;
	private final double BB = BLUE * BLUE;
	
	/************************* Public Internal Types **************************/
	public static enum Orientation { _0, _45, _90, _135 };
	
	public static long getRecommendedMax(BufferedImage image) {
		return getRecommendedMax(image.getWidth(), image.getHeight());
	}
	
	public static long getRecommendedMax(int width, int height) {
		return ((long) width * (long) height * 3L) / 35L;
	}
	
	public static long getRecommendedMax(Region region) {
		return getRecommendedMax(region.getWidth(), region.getHeight());
	}
}

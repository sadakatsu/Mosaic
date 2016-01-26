package com.sadakatsu.mosaic;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import javax.swing.SwingWorker;

import com.sadakatsu.util.Pair;

public class MosaicData {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	/*--------------------------- Types and Enums ----------------------------*/
	public static enum SplitType {
		STRAIGHT (0),
		DIAGONAL (1),
		BOTH(2);
		
		private static final SplitType[] ordered = {
			SplitType.STRAIGHT,
			SplitType.DIAGONAL,
			SplitType.BOTH
		};
		
		public static SplitType getNext(SplitType type) {
			return (
				type == null || type.index + 1 >= ordered.length ?
					null :
					ordered[type.index + 1]
			);
		}
		
		public final int index;
		
		private SplitType(int index) {
			this.index = index;
		}
	}
	
	public static class InvalidImage extends IllegalArgumentException {
		private static final long serialVersionUID = -1622560971301894853L;

		public InvalidImage(BufferedImage image) {
			super(
				String.format(
					"The passed image must not be null and be at least 1x1 " +
					"pixel.  Instead, it was %s.",
						(
							image == null ?
								"null" :
								String.format(
									"%dx%d",
										image.getWidth(),
										image.getHeight()
								)
						)
				)
			);
		}
	}
	
	public static class InvalidProximity extends IllegalArgumentException {
		private static final long serialVersionUID = -6552906736546951143L;

		public InvalidProximity(double proximity) {
			super(
				String.format(
					"A proximity must be in the range [0, 1]; received %f.",
						proximity
				)
			);
		}
	}
	
	public static class InvalidSplitType extends IllegalArgumentException {
		private static final long serialVersionUID = 1066671612272474620L;

		public InvalidSplitType() {
			super("A SplitType may not be null.");
		}
	}
	
	public static class InvalidThreshold extends IllegalArgumentException {
		private static final long serialVersionUID = 3298212270119622034L;

		public InvalidThreshold(Double threshold) {
			super(
				String.format(
					"The passed threshold must be null or in the range [0, 1]" +
					".  Instead, it is %s.",
						threshold
				)
			);
		}
	}
	
	public static class Loader extends SwingWorker<MosaicData, LoadProgress> {
		private BufferedImage image;
		private Double threshold;
		private ProgressProcessor lambda;
		
		private Loader(BufferedImage image) {
			this(image, null, null);
		}
		
		private Loader(BufferedImage image, ProgressProcessor processMethod) {
			this(image, null, processMethod);
		}
		
		private Loader(BufferedImage image, Double threshold) {
			this(image, threshold, null);
		}
		
		private Loader(
			BufferedImage image,
			Double threshold,
			ProgressProcessor processMethod
		) {
			this.image = image;
			this.lambda = processMethod;
			this.threshold = threshold;
		}
		
		@Override
		protected MosaicData doInBackground() throws Exception {
			MosaicData data = new MosaicData(image, threshold);
			while (!(data.isStopped() || data.isComplete())) {
				data.processNext();
				if (!data.isStopped()) {
					publish(new LoadProgress(data));
				}
			}
			return (data.isStopped() ? null : data);
		}

		@Override
		protected void process(List<LoadProgress> chunks) {
			if (lambda != null) {
				lambda.process(chunks.get(chunks.size() - 1));
			}
		}
	}
	
	public static class LoadProgress {
		private boolean done;
		private boolean[] complete;
		private double[] proximity;
		private int[] loaded;
		private int[] max;
		
		private LoadProgress(MosaicData data) {
			complete = new boolean[3];
			done = data.isComplete();
			loaded = Arrays.copyOf(data.regionCount, 3);
			max = new int[3];
			proximity = new double[3];
			
			for (int i = 0; i < 3; ++i) {
				complete[i] = (data.method == null || data.method.index > i);
				try {
					proximity[i] = data.calculateProximity(
						data.distance[i][loaded[i] - 1]
					);
				} catch (NullPointerException e) {
					proximity[i] = 0;
				}
				max[i] = (complete[i] ? loaded[i] : data.maxRegions);
			}
		}
		
		public boolean isComplete() {
			return done;
		}
		
		public boolean isComplete(SplitType method) {
			validateMethod(method);
			return complete[method.index];
		}
		
		public double getProximity(SplitType method) {
			validateMethod(method);
			return proximity[method.index];
		}
		
		public int getLimit(SplitType method) {
			validateMethod(method);
			return max[method.index];
		}
		
		public int getLoaded(SplitType method) {
			validateMethod(method);
			return loaded[method.index];
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("<< ");
			for (SplitType method : SplitType.ordered) {
				sb.append(method);
				sb.append(": ");
				sb.append(loaded[method.index]);
				sb.append(" / ");
				sb.append(max[method.index]);
				sb.append(" (");
				sb.append(proximity[method.index] * 100);
				sb.append("%) ");
				sb.append((complete[method.index] ? "+" : "-"));
				if (method != SplitType.BOTH) {
					sb.append(",");
				}
				sb.append(" ");
			}
			sb.append(">>");
			return sb.toString();
		}
		
		private void validateMethod(SplitType method) {
			if (method == null) {
				throw new IllegalArgumentException("Pass a non-null method.");
			}
		}
	}
	
	@FunctionalInterface
	public static interface ProgressProcessor {
		public abstract void process(LoadProgress progress);
	}
	
	private static class Lifetime {
		public Integer end;
		public Integer start;
		
		public Lifetime(int start) {
			this.start = start;
			end = null;
		}
		
		@Override
		public String toString() {
			return String.format("(%s -> %s)", start, end);
		}
	}
	
	private static class Split implements Comparable<Split> {
		private double score = Double.POSITIVE_INFINITY;
		private long areaDifference = Long.MAX_VALUE;
		private Pair<Region, Region> parts = null;
		
		public Split(Pair<Region, Region> parts) {
			this.parts = parts;
			Region first = parts.getFirst();
			Region second = parts.getSecond();
			score = (
				first.getVariance() * first.getArea() +
				second.getVariance() * second.getArea()
			);
			areaDifference = Math.abs(first.getArea() - second.getArea());
		}

		@Override
		public int compareTo(Split that) {
			int result = (
				that == null ?
					-1 :
					Double.compare(score, that.score)
			);
			if (result == 0) {
				result = Long.compare(areaDifference, that.areaDifference);
			}
			return result;
		}
		
		public Pair<Region, Region> getParts() {
			return parts;
		}
	}
	
	/*------------------------------ Interface -------------------------------*/
	public static Loader getLoader(BufferedImage image) {
		return new Loader(image);
	}
	
	public static Loader getLoader(
		BufferedImage image,
		Double threshold
	) {
		return new Loader(image, threshold);
	}
	
	public static Loader getLoader(
		BufferedImage image,
		ProgressProcessor processMethod
	) {
		return new Loader(image, processMethod);
	}
	
	public static Loader getLoader(
		BufferedImage image,
		Double threshold,
		ProgressProcessor processMethod
	) {
		return new Loader(image, threshold, processMethod);
	}
	
	public static MosaicData load(BufferedImage image) {
		return load(image, null);
	}
	
	public static MosaicData load(BufferedImage image, Double threshold) {
		MosaicData data = new MosaicData(image, threshold);
		while (!data.isComplete()) {
			data.processNext();
		}
		return data;
	}
	
	/*------------------------------- Helpers --------------------------------*/
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	private boolean stopped;
	
	private BufferedImage image;
	
	private Double threshold;
	
	private double[][] distance;
	
	private HashMap<Region, Lifetime>[] history;
	
	private int maxRegions;
	private int height;
	private int width;
	
	private int[] regionCount;
	
	private PriorityQueue<Region> queue;
	
	private Region first;
	
	private SplitType method;
	
	/*--------------------------- Types and Enums ----------------------------*/
	/*----------------------------- Constructors -----------------------------*/
	private MosaicData(BufferedImage image) {
		this(image, null);
	}
	
	private MosaicData(BufferedImage image, Double threshold) {
		setImageIfValid(image);
		setThresholdIfValid(threshold);
		prepareFields();
	}
	
	/*------------------------------ Interface -------------------------------*/
	public double getBestProximity(SplitType method) {
		validateMethod(method);
		int last = distance[method.index].length - 1;
		return calculateProximity(distance[method.index][last]);
	}

	public double getProximity(SplitType method, int count) {
		validateMethod(method);
		validateCount(method, count);
		return calculateProximity(distance[method.index][count - 1]);
	}
	
	public int getCount(SplitType method, double proximity) {
		validateMethod(method);
		validateProximity(proximity);
		
		int candidate = regionCount[method.index] - 1;
		int low = 0;
		int high = candidate - 1;
		
		while (low <= high) {
			int mid = (low + high) >> 1;
			double midP = calculateProximity(distance[method.index][mid]);
			if (midP < proximity) {
				low = mid + 1;
			} else {
				candidate = mid;
				high = mid - 1;
			}
		}
		
		return candidate + 1;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getPolygonCount(SplitType method) {
		validateMethod(method);
		return regionCount[method.index];
	}
	
	public int getWidth() {
		return width;
	}
	
	public List<Region> getRegions(SplitType method, double proximity) {
		validateMethod(method);
		return getRegions(method, getCount(method, proximity));
	}
	
	public List<Region> getRegions(SplitType method, int count) {
		validateMethod(method);
		validateCount(method, count);
		
		List<Region> regions = new ArrayList<>(count);
		
		history[method.index].forEach(
			(region, lifetime) -> {
				if (
					lifetime.start <= count &&
					(lifetime.end == null || lifetime.end > count)
				) {
					regions.add(region);
				}
			}
		);
		
		return regions;
	}
	
	/*------------------------------- Helpers --------------------------------*/
	private boolean isCurrentMethodFinished() {
		return (
			regionCount[method.index] == maxRegions ||
			queue.isEmpty() ||
			threshold != null &&
				calculateProximity(
					distance[method.index][regionCount[method.index] - 1]
				) >= threshold
		);
	}
	
	private boolean isComplete() {
		return (method == null);
	}
	
	private boolean isStopped() {
		if (!stopped && Thread.interrupted()) {
			stopped = true;
		}
		
		return stopped;
	}
	
	private double calculateProximity(double distance) {
		double furthest = calculcateProximityContribution(first);
		return (
			furthest < Double.MIN_NORMAL ?
				1 :
				(furthest - distance) / furthest
		);
	}
	
	private double calculcateProximityContribution(Region region) {
		return region.getArea() * region.getVariance();
	}
	
	private RegionIterator[] getIterators(Region region) {
		RegionIterator[] iterators = new RegionIterator[
			(method == SplitType.BOTH ? 4 : 2)
		];
		if (method != SplitType.DIAGONAL) {
			iterators[0] = new RegionIterator_0(region, image);
			iterators[1] = new RegionIterator_90(region, image);
		}
		if (method != SplitType.STRAIGHT) {
			int i = (method == SplitType.DIAGONAL ? 0 : 2);
			iterators[i] = new RegionIterator_45(region, image);
			iterators[i + 1] = new RegionIterator_135(region, image);
		}
		return iterators;
	}
	
	private Split split(Region region) {
		RegionIterator[] iterators = getIterators(region);
		Split best = null;
		for (RegionIterator iterator : iterators) {
			while (!isStopped() && iterator.hasNext()) {
				Split current = new Split(iterator.next());
				if (current.compareTo(best) < 0) {
					best = current;
				}
			}
			if (isStopped()) {
				break;
			}
		}
		return (isStopped() ? null : best);
	}
	
	private void addRegionFromLastSplit(Region region) {
		int n = regionCount[method.index];
		history[method.index].put(region, new Lifetime(n + 1));
		
		double contribution = calculcateProximityContribution(region);
		if (contribution >= Double.MIN_NORMAL) {
			distance[method.index][n] += contribution;
			queue.add(region);
		}
	}
	
	private void performNextSplit() {
		if (isStopped() || isComplete() || isCurrentMethodFinished()) {
			return;
		}
		
		int n = regionCount[method.index];
		
		Region region = queue.remove();
		history[method.index].get(region).end = n + 1;
		distance[method.index][n] = (
			distance[method.index][n - 1] -
			calculcateProximityContribution(region)
		);
		
		Split split = split(region);
		if (split != null) {
			Pair<Region, Region> parts = split.getParts();
			addRegionFromLastSplit(parts.getFirst());
			addRegionFromLastSplit(parts.getSecond());
			regionCount[method.index]++;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void prepareFields() {
		distance = new double[3][];
		
		history = (HashMap<Region, Lifetime>[]) new HashMap<?, ?>[3];
		
		height = image.getHeight();
		width = image.getWidth();
		// TODO: It is not safe to cast this long to an int.  Look into fixing
		// all the code that uses maxRegions.
		maxRegions = (int) Region.getRecommendedMax(width, height);
		
		regionCount = new int[3];
		
		queue = new PriorityQueue<>(
			(a, b) -> {
				Double x = a.getVariance() * a.getArea();
				Double y = b.getVariance() * b.getArea();
				int result = y.compareTo(x);
				if (result == 0) {
					result = Long.valueOf(
						b.getArea()).compareTo(a.getArea()
					);
				}
				
				return result;
			}
		);
		
		method = SplitType.STRAIGHT;
		
		first = new Region();
		try {
			first.loadFrom(image, 0, 0, width, height, 0, 0, 0, 0, null);
		} catch (InvalidDimensionsException e) {
			throw new CatastrophicError(e);
		}
	}
	
	private void processNext() {
		if (isStopped() || isComplete()) {
			return;
		}
		
		prepareSplitProcessIfNew();
		performNextSplit();
		prepareNextCall();
	}
	
	private void prepareNextCall() {
		if (!isStopped() && isCurrentMethodFinished()) {
			if (regionCount[method.index] < maxRegions) {
				distance[method.index] = Arrays.copyOf(
					distance[method.index],
					regionCount[method.index]
				);
			}
			
			method = SplitType.getNext(method);
			if (method == null) {
				queue = null;
			} else {
				queue.clear();
			}
		}
	}
	
	private void prepareSplitProcess() {
		history[method.index] = new HashMap<>();
		history[method.index].put(first, new Lifetime(1));
		
		queue.add(first);
		
		distance[method.index] = new double[maxRegions];
		distance[method.index][0] = calculcateProximityContribution(first);
		
		regionCount[method.index] = 1;
	}
	
	private void prepareSplitProcessIfNew() {
		if (method != null && regionCount[method.index] == 0) {
			prepareSplitProcess();
		}
	}
	
	private void setImageIfValid(BufferedImage image) {
		if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
			throw new InvalidImage(image);
		} else {
			this.image = image;
		}
	}
	
	private void setThresholdIfValid(Double threshold) {
		if (
			threshold != null && (
				threshold.doubleValue() < 0 ||
				threshold.doubleValue() > 1
			)
		) {
			throw new InvalidThreshold(threshold);
		} else {
			this.threshold = threshold;
		}
	}
	
	private void validateCount(SplitType method, int count) {
		if (count < 0 || count > regionCount[method.index]) {
			throw new ArrayIndexOutOfBoundsException();
		}
	}
	
	private void validateMethod(SplitType method) {
		if (method == null) {
			throw new InvalidSplitType();
		}
	}
	
	private void validateProximity(double proximity) {
		if (proximity < 0 || proximity > 1) {
			throw new InvalidProximity(proximity);
		}
	}
}

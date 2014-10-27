package com.sadakatsu.mosaic.gui;

import javax.swing.BoundedRangeModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sadakatsu.util.NumberComparator;

public class BoundedNumberSpinnerModel {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	private static final Double ZERO_D = Double.valueOf(0);
	
	private static final Integer ZERO_I = Integer.valueOf(0);
	
	/*--------------------------- Types and Enums ----------------------------*/
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	private boolean decimalMode = false;
	private boolean sliderFired = false;
	
	private double doubleLast = Double.NaN;
	private double doubleMaximum = 1;
	private double doubleMinimum = 0;
	private double doubleStepSize = 0.05;
	private double doubleValue = 0.5;
	
	private int intLast = Integer.MIN_VALUE;
	private int intMaximum = 100;
	private int intMinimum = 0;
	private int intStepSize = 1;
	private int intValue = 50;
	
	private InternalSliderModel sliderModel;
	
	private InternalSpinnerModel spinnerModel;
	
	/*--------------------------- Types and Enums ----------------------------*/
	private class InternalSliderModel
	extends CanChange
	implements BoundedRangeModel {
		private final BoundedNumberSpinnerModel reference;
		
		private boolean isAdjusting;
		
		public InternalSliderModel(BoundedNumberSpinnerModel reference) {
			this.reference = reference;
			isAdjusting = false;
		}
		
		@Override
		public int getExtent() {
			return 0;
		}
		
		@Override
		public boolean getValueIsAdjusting() {
			return isAdjusting;
		}

		@Override
		public int getMaximum() {
			return reference.getScale();
		}
		
		@Override
		public int getMinimum() {
			return 0;
		}
		
		@Override
		public int getValue() {
			return reference.getSliderValue();
		}
		
		@Override
		public void setExtent(int newExtent) {
			throw new UnsupportedOperationException(
				"Changes should be made through the BoundedNumberSpinnerModel" +
				" method interface."
			);
		}
		
		@Override
		public void setMaximum(int newMaximum) {
			throw new UnsupportedOperationException(
				"Changes should be made through the BoundedNumberSpinnerModel" +
				" method interface."
			);
		}

		@Override
		public void setMinimum(int newMinimum) {
			throw new UnsupportedOperationException(
				"Changes should be made through the BoundedNumberSpinnerModel" +
				" method interface."
			);
		}
		
		@Override
		public void setRangeProperties(
			int value,
			int extent,
			int min,
			int max,
			boolean adjusting
		) {
			throw new UnsupportedOperationException(
				"Changes should be made through the BoundedNumberSpinnerModel" +
				" method interface."
			);
		}

		@Override
		public void setValue(int newValue) {
			reference.setValue(convertToValue(newValue));
		}

		@Override
		public void setValueIsAdjusting(boolean b) {
			if (b != isAdjusting) {
				isAdjusting = b;
				fireStateChanged();
			}
		}
	}
	
	private class InternalSpinnerModel extends SpinnerNumberModel {
		private static final long serialVersionUID = -3940671571031289595L;
		
		private final BoundedNumberSpinnerModel reference;
		
		public InternalSpinnerModel(BoundedNumberSpinnerModel reference) {
			this.reference = reference;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Comparable<? extends Number> getMaximum() {
			return (Comparable<? extends Number>) reference.getMaximum();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Comparable<? extends Number> getMinimum() {
			return (Comparable<? extends Number>) reference.getMinimum();
		}
		
		@Override
		public Number getStepSize() {
			return reference.getStepSize();
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public void setMaximum(Comparable maximum) {
			throw new UnsupportedOperationException(
				"Liskov principle violated unsuccessfully..."
			);
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public void setMinimum(Comparable minimum) {
			throw new UnsupportedOperationException(
				"Liskov principle violated unsuccessfully..."
			);
		}
		
		@Override
		public Number getNumber() {
			return reference.getValue();
		}
		
		@Override
		public Object getValue() {
			return reference.getValue();
		}
		
		@Override
		public Object getNextValue() {
			return reference.getNextValue();
		}
		
		@Override
		public Object getPreviousValue() {
			return reference.getPreviousValue();
		}

		@Override
		public void setStepSize(Number stepSize) {
			throw new UnsupportedOperationException(
				"Liskov principle violated unsuccessfully..."
			);
		}

		@Override
		public void setValue(Object value) {
			if (!(value instanceof Number)) {
				throw new IllegalArgumentException(
					"This SpinnerModel can only accept Numbers."
				);
			}
			reference.setValue((Number) value);
		}
		
		@Override
		protected void fireStateChanged() {
			ChangeEvent changeEvent = new ChangeEvent(this);
			Object[] listeners = listenerList.getListenerList();
			
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == ChangeListener.class) {
					((ChangeListener) listeners[i + 1]).stateChanged(
						changeEvent
					);
				}
			}
		}
	}
	
	/*----------------------------- Constructors -----------------------------*/
	public BoundedNumberSpinnerModel() {
		decimalMode = false;
		sliderModel = new InternalSliderModel(this);
		spinnerModel = new InternalSpinnerModel(this);
	}
	
	public BoundedNumberSpinnerModel(boolean decimalMode) {
		this.decimalMode = decimalMode;
		sliderModel = new InternalSliderModel(this);
		spinnerModel = new InternalSpinnerModel(this);
	}
	
	public BoundedNumberSpinnerModel(
		double minimum,
		double maximum,
		double stepSize
	) {
		this(minimum, maximum, stepSize, (minimum + maximum) / 2);
	}
	
	public BoundedNumberSpinnerModel(
		double minimum,
		double maximum,
		double stepSize,
		double value
	) {
		this();
		update(minimum, maximum, stepSize);
		setValue(value);
	}
	
	public BoundedNumberSpinnerModel(
		int minimum,
		int maximum,
		int stepSize
	) {
		this(minimum, maximum, stepSize, (minimum + maximum) / 2);
	}
	
	public BoundedNumberSpinnerModel(
		int minimum,
		int maximum,
		int stepSize,
		int value
	) {
		this();
		update(minimum, maximum, stepSize);
		setValue(value);
	}
	
	/*------------------------------ Interface -------------------------------*/
	public BoundedRangeModel getSliderModel() {
		return sliderModel;
	}
	
	public Number getLastValue() {
		return (decimalMode ? (Number) ((Double) doubleLast) : intLast);
	}

	public Number getMaximum() {
		return (decimalMode ? (Number) ((Double) doubleMaximum) : intMaximum);
	}
	
	public Number getMinimum() {
		return (decimalMode ? (Number) ((Double) doubleMinimum) : intMinimum);
	}
	
	public Number getStepSize() {
		return (decimalMode ? (Number) ((Double) doubleStepSize) : intStepSize);
	}
	
	public Number getValue() {
		return (decimalMode ? (Number) ((Double) doubleValue) : intValue);
	}
	
	public Number getNextValue() {
		int value = getSliderValue();
		return (value == getScale() ? null : convertToValue(value + 1));
	}
	
	public Number getPreviousValue() {
		int value = getSliderValue();
		return (value == 0 ? null : convertToValue(value - 1));
	}
	
	public SpinnerModel getSpinnerModel() {
		return spinnerModel;
	}
	
	public void setValue(Number value) {
		// Get the linked models' old values.
		int oldSlider = sliderModel.getValue();
		Number oldSpinner = (Number) spinnerModel.getValue();
		
		// Ensure that the value is normalized before setting 
		if (decimalMode) {
			doubleLast = doubleValue;
			doubleValue = normalizeDouble(value.doubleValue());
		} else {
			intLast = intValue;
			intValue = normalizeInt(value.intValue());
		}
		
		// Fire change events if the linked models changed because of this
		// change.
		if (oldSlider != sliderModel.getValue()) {
			sliderModel.fireStateChanged();
			sliderFired = true;
		}
		
		if (!oldSpinner.equals((Number) spinnerModel.getValue())) {
			spinnerModel.fireStateChanged();
		}
	}
	
	public void update(double maximum) {
		update(0, maximum, 1);
	}
	
	public void update(double minimum, double maximum) {
		update(minimum, maximum, 1);
	}
	
	public void update(double minimum, double maximum, double stepSize) {
		update((Double) minimum, (Double) maximum, (Double) stepSize);
	}
	
	public void update(int maximum) {
		update(0, maximum, 1);
	}
	
	public void update(int minimum, int maximum) {
		update(minimum, maximum, 1);
	}
	
	public void update(int minimum, int maximum, int stepSize) {
		update((Integer) minimum, (Integer) maximum, (Integer) stepSize);
	}
	
	public void update(Number minimum, Number maximum, Number stepSize) {
		// Save the slider's current max value.  Set the "slider fired" state to
		// "false" so setValue() can reset it if the value changes.
		int sliderMax = sliderModel.getMaximum();
		sliderFired = false;
		
		// Change this model's range.
		updateInternals(minimum, maximum, stepSize);
		
		// Fire events if the linked models' states changed because of this
		// model's changes.
		if (!(sliderFired || sliderMax == sliderModel.getMaximum())) {
			sliderModel.fireStateChanged();
		}
	}
	
	/*------------------------------- Helpers --------------------------------*/
	private double normalizeDouble(double n) {
		int v = convertToSlider(n);
		v = Math.min(getScale(), Math.max(0, v));
		return (Double) convertToValue(v);
	}
	
	private int convertToSlider(Number n) {
		int toReturn;
		if (decimalMode) {
			toReturn = (int) Math.round(
				(n.doubleValue() - doubleMinimum) / doubleStepSize
			);
		} else {
			toReturn = (n.intValue() - intMinimum) / intStepSize;
		}
		return toReturn;
	}
	
	private Number convertToValue(int sliderValue) {
		if (decimalMode) {
			return sliderValue * doubleStepSize + doubleMinimum;
		} else {
			return sliderValue * intStepSize + intMinimum;
		}
	}
	
	private int getSliderValue() {
		return convertToSlider(getValue());
	}
	
	private int getScale() {
		return convertToSlider(decimalMode ? doubleMaximum : intMaximum);
	}
	
	private int normalizeInt(int n) {
		int v = convertToSlider(n);
		v = Math.min(getScale(), Math.max(0, v));
		return (Integer) convertToValue(v);
	}
	
	private void updateInternals(
		Number minimum,
		Number maximum,
		Number stepSize
	) {
		Number zero = ((stepSize instanceof Double) ? (Number) ZERO_D : ZERO_I);
		
		if (
			NumberComparator.performComparison(minimum, maximum) > 0 ||
			NumberComparator.performComparison(stepSize, zero) <= 0
		) {
			throw new IllegalArgumentException(
				"The maximum must be greater than or equal to the minimum, " +
				"and the step size must be greater than zero."
			);
		}
		
		if (minimum instanceof Double) {
			decimalMode = true;
			
			doubleMinimum = minimum.doubleValue();
			doubleStepSize = stepSize.doubleValue();
			doubleMaximum = (Double) convertToValue(
				convertToSlider(maximum.doubleValue())
			);
			setValue(doubleValue);
		} else {
			decimalMode = false;
			
			intMinimum = minimum.intValue();
			intStepSize = stepSize.intValue();
			intMaximum = (Integer) convertToValue(
				convertToSlider(maximum.intValue())
			);
			
			setValue(intValue);
		}
	}
}

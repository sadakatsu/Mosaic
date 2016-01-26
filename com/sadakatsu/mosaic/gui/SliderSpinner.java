package com.sadakatsu.mosaic.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SliderSpinner extends JComponent {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	private static final long serialVersionUID = -3843337207161969463L;
	
	/*--------------------------- Types and Enums ----------------------------*/
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
	
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	private NumberEditor integerEditor;
	private NumberEditor percentEditor;
	private JSlider slider;
	private JSpinner spinner;
	private BoundedNumberSpinnerModel model;
	
	/*--------------------------- Types and Enums ----------------------------*/
	/*----------------------------- Constructors -----------------------------*/
	public SliderSpinner(boolean decimalMode) {
		initialize(decimalMode);
		setEnabled(false);
	}
	
	public SliderSpinner(
		Number minimum,
		Number maximum,
		Number value,
		Number stepSize
	) {
		initialize((minimum instanceof Double));
		update(minimum, maximum, stepSize);
		setValue(value);
		setEnabled(false);
	}
	
	/*------------------------------ Interface -------------------------------*/
	@Override
	public boolean isEnabled() {
		return spinner.isEnabled();
	}
	
	public Number getLastValue() {
		return model.getLastValue();
	}
	
	public Number getMaximum() {
		return model.getMaximum();
	}
	
	public Number getMinimum() {
		return model.getMinimum();
	}
	
	public Number getStepSize() {
		return model.getStepSize();
	}
	
	public Number getValue() {
		return model.getValue();
	}
	
	public void addChangeListener(ChangeListener listener) {
		listenerList.add(ChangeListener.class, listener);
	}
	
	public void removeChangeListener(ChangeListener listener) {
		listenerList.remove(ChangeListener.class, listener);
	}
	
	@Override
	public void setEnabled(boolean b) {
		slider.setEnabled(b);
		spinner.setEnabled(b);
	}
	
	public void setValue(Number value) {
		Number oldValue = model.getValue();
		model.setValue(value);
		Number newValue = model.getValue();
		
		if (!oldValue.getClass().equals(newValue.getClass())) {
			if (newValue instanceof Double) {
				spinner.setEditor(percentEditor);
			} else {
				spinner.setEditor(integerEditor);
			}
		}
	}

	public void update(Number minimum, Number maximum, Number stepSize) {
		model.update(minimum, maximum, stepSize);
		
		Number number = model.getValue();
		double value;
		if (number == null) {
			value = (minimum.doubleValue() + maximum.doubleValue()) / 2;
		} else {
			value = number.doubleValue();
		}
		
		model.setValue(value);
	}
	
	/*------------------------------- Helpers --------------------------------*/
	private void buildPresentation() {
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 0.5;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		
		add(spinner, constraints);
		
		constraints.gridx = 1;
		add(slider, constraints);
	}
	
	private void fireStateChanged() {
		// This code was shamelessly stolen from the JSlider class, and then
		// even more shamelessly modified.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ChangeEvent changeEvent = new ChangeEvent(this);
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }
	
	private void initialize(boolean decimalMode) {
		initializeComponents(decimalMode);
		buildPresentation();
	}
	
	private void initializeComponents(boolean decimalMode) {
		final SliderSpinner reference = this;
		
		model = new BoundedNumberSpinnerModel(decimalMode);
		
		slider = new JSlider(model.getSliderModel());
		slider.addChangeListener(
			(e) -> {
				reference.fireStateChanged();
			}
		);
		
		spinner = new JSpinner(model.getSpinnerModel());
		spinner.setPreferredSize(
			new Dimension(67, spinner.getPreferredSize().height)
		);
		
		integerEditor = new NumberEditor(spinner);
		percentEditor = new NumberEditor(
			spinner,
			"##0.00%"
		);
		if (decimalMode) {
			spinner.setEditor(percentEditor);
		} else {
			spinner.setEditor(integerEditor);
		}
	}
}

package com.sadakatsu.mosaic.renderer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner.NumberEditor;

import com.sadakatsu.mosaic.MosaicData;
import com.sadakatsu.mosaic.MosaicData.SplitType;
import com.sadakatsu.mosaic.Region;

public class FilledWireframeRenderer extends AbstractMosaicRenderer {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	/*--------------------------- Types and Enums ----------------------------*/
	private static enum OPTIONS {
		SOLID ("Solid Color"),
		OPPOSITE ("Opposite Color"),
		DARKEN ("Darken"),
		LIGHTEN ("Lighten");
		
		private String label;
		
		private OPTIONS(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	private ControlPanel controls;
	
	/*--------------------------- Types and Enums ----------------------------*/
	private class ControlPanel extends RendererControlPanel {
		private Color wireframeColor = Color.BLACK;
		private double darkenScale = 0.5;
		private double lightenScale = 0.5;
		private JButton colorButton;
		private JRadioButton darken;
		private JRadioButton lighten;
		private JRadioButton opposite;
		private JRadioButton solid;
		private JSpinner darkenSpinner;
		private JSpinner lightenSpinner;
		
		public ControlPanel() {
			// Instantiate and prepare the radio buttons.
			solid = new JRadioButton(OPTIONS.SOLID.label);
			solid.addActionListener(this);
			
			opposite = new JRadioButton(OPTIONS.OPPOSITE.label);
			opposite.addActionListener(this);
			
			darken = new JRadioButton(OPTIONS.DARKEN.label, true);
			darken.addActionListener(this);
			
			lighten = new JRadioButton(OPTIONS.LIGHTEN.label);
			lighten.addActionListener(this);
			
			ButtonGroup group = new ButtonGroup();
			group.add(solid);
			group.add(opposite);
			group.add(darken);
			group.add(lighten);
			
			// Instantiate the solid color change control.
			int size = 12;
			BufferedImage display = new BufferedImage(
				size,
				size,
				BufferedImage.TYPE_3BYTE_BGR
			);
			Graphics graphics = display.getGraphics();
			
			graphics.setColor(Color.WHITE);
			graphics.drawRect(1, 1, size - 3, size - 3);
			
			ImageIcon icon = new ImageIcon(display);
			
			colorButton = new JButton("Change Color", icon);
			colorButton.addActionListener(
				(e) -> {
					Color selected = JColorChooser.showDialog(
						null,
						"Select the wireframe color",
						wireframeColor
					);
					
					if (selected != null) {
						wireframeColor = selected;
						graphics.setColor(selected);
						graphics.fillRect(2, 2, size - 4, size - 4);
						fireStateChanged();
					}
				}
			);
			colorButton.setEnabled(false);
			
			// Instantiate the darken spinner.
			darkenSpinner = new JSpinner(
				new SpinnerNumberModel(
					darkenScale,
					0.,
					1.,
					0.001
				)
			);
			darkenSpinner.setEditor(new NumberEditor(darkenSpinner, "##0.0%"));
			darkenSpinner.addChangeListener(
				(e) -> {
					darkenScale = (Double) darkenSpinner.getValue();
					fireStateChanged();
				}
			);
			
			// Instantiate the darken spinner.
			lightenSpinner = new JSpinner(
				new SpinnerNumberModel(
					lightenScale,
					0.,
					1.,
					0.001
				)
			);
			lightenSpinner.setEditor(
				new NumberEditor(lightenSpinner, "##0.0%")
			);
			lightenSpinner.addChangeListener(
				(e) -> {
					lightenScale = (Double) lightenSpinner.getValue();
					fireStateChanged();
				}
			);
			lightenSpinner.setEnabled(false);
			
			// Compose the panel.
			setLayout(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.anchor = GridBagConstraints.WEST;
			
			JLabel label = new JLabel("Choose Outline Method:");
			add(label, constraints);
			
			constraints.gridy = 1;
			add(solid, constraints);
			
			constraints.gridx = 1;
			add(colorButton, constraints);
			
			constraints.gridx = 0;
			constraints.gridy = 2;
			add(opposite, constraints);
			
			constraints.gridy = 3;
			add(darken, constraints);
			
			constraints.gridx = 1;
			add(darkenSpinner, constraints);
			
			constraints.gridx = 0;
			constraints.gridy = 4;
			add(lighten, constraints);
			
			constraints.gridx = 1;
			add(lightenSpinner, constraints);
		}
		
		public Color getWireframeColorFor(Color color) {
			Color toReturn;
			int r = color.getRed();
			int g = color.getGreen();
			int b = color.getBlue();
			
			if (solid.isSelected()) {
				toReturn = wireframeColor;
			} else if (opposite.isSelected()) {
				toReturn = new Color(255 - r, 255 - g, 255 - b);
			} else if (darken.isSelected()) {
				double c = 1 - darkenScale;
				toReturn = new Color(
					(int) Math.round(c * r),
					(int) Math.round(c * g),
					(int) Math.round(c * b)
				);
			} else {
				toReturn = new Color(
					(int) Math.round(r + (255 - r) * lightenScale),
					(int) Math.round(g + (255 - g) * lightenScale),
					(int) Math.round(b + (255 - b) * lightenScale)
				);
			}
			
			return toReturn;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			colorButton.setEnabled(solid.isSelected());
			darkenSpinner.setEnabled(darken.isSelected());
			lightenSpinner.setEnabled(lighten.isSelected());
			
			super.actionPerformed(e);
		}
	}
	
	/*----------------------------- Constructors -----------------------------*/
	public FilledWireframeRenderer(WhenDone hook) {
		super(hook);
	}
	
	/*------------------------------ Interface -------------------------------*/
	@Override
	public RendererControlPanel getControls() {
		if (controls == null) {
			controls = new ControlPanel();
		}
		return controls;
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
					
					Polygon polygon = new RegionPolygon(region);
					
					Color color = region.getColor();
					graphics.setColor(color);
					graphics.fillPolygon(polygon);
					
					graphics.setColor(controls.getWireframeColorFor(color));
					if (region.getArea() == 1) {
						image.setRGB(
							region.getX(),
							region.getY(),
							color.getRGB()
						);
					} else {
						graphics.drawPolygon(region.getPolygon());
					}
				}
			}
		};
	}
	
	@Override
	public String toString() {
		return "Filled Wireframe";
	}
	
	/*------------------------------- Helpers --------------------------------*/
}

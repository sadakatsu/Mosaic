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

import com.sadakatsu.mosaic.MosaicData;
import com.sadakatsu.mosaic.Region;
import com.sadakatsu.mosaic.MosaicData.SplitType;

public class ColoringBookRenderer extends AbstractMosaicRenderer {
	private static enum OPTIONS {
		BLACK ("Black"),
		POLYGON ("Polygon Color"),
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
	
	private class ControlPanel extends RendererControlPanel {
		private static final long serialVersionUID = 1838925416824784707L;
		
		private Color wireframeColor = Color.BLACK;
		private JButton colorButton;
		private JRadioButton black;
		private JRadioButton darken;
		private JRadioButton lighten;
		private JRadioButton opposite;
		private JRadioButton polygon;
		private JRadioButton solid;
		private PercentSpinner darkenSpinner;
		private PercentSpinner lightenSpinner;
		
		ControlPanel() {
			prepareRadioButtons();
			prepareColorSelect();
			prepareSpinners();
			composePanel();
			setInitiallyEnabledControls();
		}
		
		private void prepareRadioButtons() {
			instantiateRadioButtons();
			groupRadioButtons();
		}
		
		private void instantiateRadioButtons() {
			black = createRadioButtonForOption(OPTIONS.BLACK);
			polygon = createRadioButtonForOption(OPTIONS.POLYGON);
			solid = createRadioButtonForOption(OPTIONS.SOLID);
			opposite = createRadioButtonForOption(OPTIONS.OPPOSITE);
			darken = createRadioButtonForOption(OPTIONS.DARKEN);
			lighten = createRadioButtonForOption(OPTIONS.LIGHTEN);
		}
		
		private JRadioButton createRadioButtonForOption(OPTIONS option) {
			JRadioButton button = new JRadioButton(option.label);
			button.addActionListener(this);
			return button;
		}
		
		private void groupRadioButtons() {
			ButtonGroup group = new ButtonGroup();
			group.add(black);
			group.add(polygon);
			group.add(solid);
			group.add(opposite);
			group.add(darken);
			group.add(lighten);
		}
		
		private void prepareColorSelect() {
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
		}
		
		private void prepareSpinners() {
			darkenSpinner = new PercentSpinner(this, 0.5);
			lightenSpinner = new PercentSpinner(this, 0.5);
		}
		
		private void composePanel() {
			GridBagLayout layout = new GridBagLayout();
			setLayout(layout);
			
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.anchor = GridBagConstraints.WEST;
			
			JLabel label = new JLabel("Choose Outline Method:");
			add(label, constraints);
			
			constraints.gridy = 1;
			add(black, constraints);
			
			constraints.gridy = 2;
			add(polygon, constraints);
			
			constraints.gridy = 3;
			add(solid, constraints);
			
			constraints.gridx = 1;
			add(colorButton, constraints);
			
			constraints.gridx = 0;
			constraints.gridy = 4;
			add(opposite, constraints);
			
			constraints.gridy = 5;
			add(darken, constraints);
			
			constraints.gridx = 1;
			add(darkenSpinner, constraints);
			
			constraints.gridx = 0;
			constraints.gridy = 6;
			add(lighten, constraints);
			
			constraints.gridx = 1;
			add(lightenSpinner, constraints);
		}
		
		private void setInitiallyEnabledControls() {
			black.setSelected(true);
			colorButton.setEnabled(false);
			darkenSpinner.setEnabled(false);
			lightenSpinner.setEnabled(false);
			
		}
		
		Color getWireframeColorFor(Color color) {
			Color toReturn;
			
			int r = color.getRed();
			int g = color.getGreen();
			int b = color.getBlue();
			
			if (black.isSelected()) {
				toReturn = Color.BLACK;
			} else if (polygon.isSelected()) {
				toReturn = color;
			} else if (solid.isSelected()) {
				toReturn = wireframeColor;
			} else if (opposite.isSelected()) {
				toReturn = new Color(255 - r, 255 - g, 255 - b);
			} else if (darken.isSelected()) {
				double c = 1 - darkenSpinner.getPercentage();
				toReturn = new Color(
					(int) Math.round(c * r),
					(int) Math.round(c * g),
					(int) Math.round(c * b)
				);
			} else {
				double c = lightenSpinner.getPercentage();
				toReturn = new Color(
					(int) Math.round(r + (255 - r) * c),
					(int) Math.round(g + (255 - g) * c),
					(int) Math.round(b + (255 - b) * c)
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
	
	private class PercentSpinner extends JSpinner {
		private static final long serialVersionUID = 6076522296158221710L;
		
		private double percentage;
		private RendererControlPanel owner;

		PercentSpinner(RendererControlPanel owner, double initialValue) {
			super(
				new SpinnerNumberModel(
					initialValue,
					0.,
					1.,
					0.001
				)
			);
			
			this.owner = owner;
			percentage = initialValue;
			
			setEditor(new NumberEditor(this, "##0.0%"));
			addChangeListener(
				(e) -> {
					percentage = (Double) getValue();
					owner.fireStateChanged();
				}
			);
		}
		
		double getPercentage() {
			return percentage;
		}
	}
	
	
	private ControlPanel controls;
	

	public ColoringBookRenderer(WhenDone hook) {
		super(hook);
	}

	@Override
	public RendererControlPanel getControls() {
		if (controls == null) {
			controls = new ControlPanel();
		}
		return controls;
	}

	@Override
	public RenderWorker buildThread(MosaicData data, SplitType method, int count) {
		return new RenderWorker(data, method, count) {
			@Override
			protected void performRender() {
				Graphics graphics = image.getGraphics();
				
				for (Region region : data.getRegions(method, count)) {
					if (isStopped()) {
						break;
					}
					
					Polygon polygon = new RegionPolygon(region);
					
					graphics.setColor(Color.WHITE);
					graphics.fillPolygon(polygon);
					
					Color color = region.getColor();
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
		return "Coloring Book";
	}

}

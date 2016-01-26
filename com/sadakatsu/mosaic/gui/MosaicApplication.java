package com.sadakatsu.mosaic.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sadakatsu.mosaic.MosaicData;
import com.sadakatsu.mosaic.MosaicData.SplitType;
import com.sadakatsu.mosaic.renderer.AbstractMosaicRenderer;
import com.sadakatsu.mosaic.renderer.AbstractMosaicRenderer.RendererControlPanel;
import com.sadakatsu.mosaic.renderer.BasicRenderer;
import com.sadakatsu.mosaic.renderer.ColoringBookRenderer;
import com.sadakatsu.mosaic.renderer.FilledWireframeRenderer;
import com.sadakatsu.util.Pair;

public class MosaicApplication extends JFrame {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	private static final int LABEL_WIDTH = 73;
	private static final int PADDING = 5;
	private static final long serialVersionUID = -7822752461338174701L;
	
	/*--------------------------- Types and Enums ----------------------------*/
	private static class RenderPanel extends JPanel implements Scrollable {
		private static final long serialVersionUID = 4070954889909123479L;
		
		private BufferedImage image;
		private double scale = 1.;
		
		private double getScale() {
			return scale;
		}
		
		public void changeImage(BufferedImage newImage) {
			image = newImage;
			setScale(scale);
		}
		
		public void setScale(Dimension baseSize) {
			Dimension dimension = this.getParent().getSize();
			
			double newScale = Math.min(
				(double) dimension.width / baseSize.width,
				(double) dimension.height / baseSize.height
			);
			
			setScale(newScale);
		}
		
		public void setScale(double scale) {
			this.scale = scale;
			if (image != null) {
				setPreferredSize(
					new Dimension(
						(int) Math.round(image.getWidth() * scale),
						(int) Math.round(image.getHeight() * scale)
					)
				);
			}
		}
		
		public void setScaleToFit() {
			if (image != null) {
				Dimension dimension = this.getParent().getSize();
				
				double newScale = Math.min(
					(double) dimension.width / image.getWidth(),
					(double) dimension.height / image.getHeight()
				);
				
				setScale(newScale);
			}
		}
		
		public void setScaleToFitHeight() {
			// TODO: This method does not calculate the same size between two
			// consecutive calls (even if no resize events happened in the
			// parent).  Try to determine why this is and fix it.
			if (image != null) {
				Dimension dimension = this.getParent().getSize();
				double newScale = (double) dimension.height / image.getHeight();
				setScale(newScale);
			}
		}
		
		public void setScaleToFitWidth() {
			// TODO: This method does not calculate the same size between two
			// consecutive calls (even if no resize events happened in the
			// parent).  Try to determine why this is and fix it.
			if (image != null) {
				Dimension dimension = this.getParent().getSize();
				double newScale = (double) dimension.width / image.getWidth();
				setScale(newScale);
			}
		}
		
		public void update() {
			revalidate();
			repaint();
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Dimension dimension = getPreferredSize();
	        g.drawImage(image, 0, 0, dimension.width, dimension.height, this);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(
			Rectangle visibleRect,
			int orientation,
			int direction
		) {
			return (int) Math.round(1 / scale);
		}

		@Override
		public int getScrollableBlockIncrement(
			Rectangle visibleRect,
			int orientation,
			int direction
		) {
			int length = (
				orientation == SwingConstants.HORIZONTAL ?
					visibleRect.width :
					visibleRect.height
			);
			return (int) Math.round(length / scale);
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return false;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}
	
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	private AbstractMosaicRenderer renderer;
	private boolean fitRender;
	private ChangeListener rendererControlPanelListener;
	private File file;
	private JPanel wrapper;
	private MosaicData data;
	private RendererControlPanel rendererControls;
	private RenderPanel render;
	private SliderSpinner accuracy;
	private SliderSpinner polygons;
	private SliderSpinner zoom;
	private SplitType type = SplitType.STRAIGHT;
	private volatile SwingWorker<Void, Void> renderThread = null;
	
	/*--------------------------- Types and Enums ----------------------------*/
	/*----------------------------- Constructors -----------------------------*/
	public MosaicApplication() {
		prepareFrame();
		prepareControls();
		prepareRenderer();
		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}
	
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
	private JPanel createZoomSliderSpinner() {
		zoom = new SliderSpinner(0., 16., 1., 0.01);
		zoom.addChangeListener(
			(e) -> {
				if (zoom.isEnabled()) {
					fitRender = false;
					
					double scale = (Double) zoom.getValue();
					render.setScale(scale);
					render.update();
				}
			}
		);
		zoom.setEnabled(true);
		
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
		
		JLabel label = new JLabel("Zoom:");
		setLabelSize(label);
		panel.add(label, constraints);
		
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 1;
		panel.add(zoom, constraints);
		
		return panel;
	}

	private JPanel prepareMosaicSettings() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Mosaic Settings"));
		panel.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
		
		prepareFileLoadControls(panel, constraints);
		
		constraints.gridy = 1;
		prepareSplitTypeCombo(panel, constraints);
		
		constraints.gridy = 2;
		preparePolygonSlider(panel, constraints);
		
		constraints.gridy = 3;
		prepareAccuracySlider(panel, constraints);
		
		constraints.gridy = 4;
		prepareRendererCombo(panel, constraints);
		
		constraints.gridy = 5;
		prepareSaveButton(panel, constraints);
		
		return panel;
	}
	
	private JPanel prepareRendererControlPanel() {
		rendererControls = renderer.getControls();
		
		wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createTitledBorder("Renderer Controls"));
		
		if (rendererControls != null) {
			wrapper.add(rendererControls);
			rendererControls.addChangeListener(rendererControlPanelListener);
		}
		
		return wrapper;
	}
	
	private JPanel prepareViewControls() {
		JPanel panel = new JPanel();
		panel.setBorder(
			BorderFactory.createTitledBorder("View Controls")
		);
		panel.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
		
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.gridwidth = 4;
		panel.add(createZoomSliderSpinner(), constraints);
		
		JButton fit = new JButton("Best Fit");
		fit.addActionListener(
			(e) -> {
				render.setScaleToFit();
				fitRender = true;
				
				zoom.setEnabled(false);
				zoom.setValue(render.getScale());
				zoom.setEnabled(true);
				
				render.update();
			}
		);
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridwidth = 1;
		constraints.gridy = 1;
		panel.add(fit, constraints);
		
		JButton actual = new JButton("Actual Size");
		actual.addActionListener(
			(e) -> {
				render.setScale(1);
				fitRender = false;
				
				zoom.setEnabled(false);
				zoom.setValue(render.getScale());
				zoom.setEnabled(true);
				
				render.update();
			}
		);
		constraints.gridx = 1;
		panel.add(actual, constraints);
		
		JButton fitWidth = new JButton("Fit Width");
		fitWidth.addActionListener(
			(e) -> {
				render.setScaleToFitWidth();
				fitRender = false;
				
				zoom.setEnabled(false);
				zoom.setValue(render.getScale());
				zoom.setEnabled(true);
				
				render.update();
			}
		);
		constraints.gridx = 2;
		panel.add(fitWidth, constraints);
		
		JButton fitHeight = new JButton("Fit Height");
		fitHeight.addActionListener(
			(e) -> {
				render.setScaleToFitHeight();
				fitRender = false;
				
				zoom.setEnabled(false);
				zoom.setValue(render.getScale());
				zoom.setEnabled(true);
				
				render.update();
			}
		);
		constraints.gridx = 3;
		panel.add(fitHeight, constraints);
		
		return panel;
	}
	
	private void performRender(boolean isNewImage) {
		if (data != null) {
			if (!(renderThread == null || renderThread.isDone())) {
				renderThread.cancel(true);
			}
			
			if (isNewImage) {
				Dimension size = new Dimension(
					data.getWidth(),
					data.getHeight()
				);
				render.setScale(size);
				
				zoom.setEnabled(false);
				zoom.setValue(render.getScale());
				zoom.setEnabled(true);
			}
			
			fitRender = (fitRender || isNewImage);
			
			renderThread = renderer.buildThread(
				data,
				type,
				(Integer) polygons.getValue()
			);
			renderThread.execute();
		}
	}
	
	private void prepareAccuracySlider(
		JPanel panel,
		GridBagConstraints constraints
	) {
		accuracy = new SliderSpinner(0., 1., 0.75, 0.0005);
		accuracy.addChangeListener(
			e -> {
				if (!accuracy.isEnabled()) {
					return;
				}
				
				accuracy.setEnabled(false);
				polygons.setEnabled(false);
				
				double proximity = accuracy.getValue().doubleValue();
				int count = data.getCount(type, proximity);
				if (count != (Integer) polygons.getValue()) {
					polygons.setValue(count);
				}
				
				accuracy.setEnabled(true);
				polygons.setEnabled(true);
				
				performRender(false);
			}
		);
		
		JLabel label = new JLabel("Accuracy:");
		setLabelSize(label);
		
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridx = 0;
		panel.add(label, constraints);
		
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 1;
		panel.add(accuracy, constraints);
	}
	
	private void prepareFileLoadControls(
		JPanel panel,
		GridBagConstraints constraints
	) {
		JLabel label = new JLabel("File:");
		setLabelSize(label);
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridx = 0;
		panel.add(label, constraints);
		
		JPanel subpanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		JTextField field = new JTextField(10);
		field.setEditable(false);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(0, 0, 0, PADDING);
		c.weightx = 1.;
		subpanel.add(field, c);
		
		JButton button = new JButton("Load");
		button.addActionListener(
			(e) -> {
				Pair<File, MosaicData> pair =
					ImageAndSettingsDialog.showImageDialog();
				
				if (
					pair != null &&
					pair.getFirst() != null &&
					pair.getSecond() != null
				) {
					file = pair.getFirst();
					data = pair.getSecond();
					
					field.setText(file.getName());
					
					int max = data.getPolygonCount(type);
					
					boolean enabled = polygons.isEnabled();
					accuracy.setEnabled(false);
					polygons.setEnabled(false);
					
					polygons.update(1, max, 1);
					if (!enabled) {
						int count = data.getCount(
							type,
							(Double) accuracy.getValue()
						);
						polygons.setValue(count);
					}
					
					int trueCount = (Integer) polygons.getValue();
					accuracy.setValue(data.getProximity(type, trueCount));
					
					accuracy.setEnabled(true);
					polygons.setEnabled(true);
					
					performRender(true);
				}
			}
		);
		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.insets = new Insets(0, 0, 0, 0);
		c.weightx = 0.;
		subpanel.add(button, c);
		
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 1;
		panel.add(subpanel, constraints);
	}
	
	private void prepareControls() {
		JPanel controlPanel = new JPanel(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		
		controlPanel.add(prepareMosaicSettings(), constraints);
		
		constraints.gridy = 1;
		controlPanel.add(prepareViewControls(), constraints);
		
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridy = 2;
		constraints.weighty = 1;
		controlPanel.add(prepareRendererControlPanel(), constraints);
		
		add(controlPanel, BorderLayout.WEST);
	}
	
	private void prepareFrame() {
		setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
		setTitle("Mosaic");
		
		Class<? extends MosaicApplication> c = getClass();
		URL url = c.getResource("icon/mosaicIcon.png");
		ImageIcon icon = new ImageIcon(url);
		setIconImage(icon.getImage());
		
		setLayout(new BorderLayout());
		
		addComponentListener(
			new ComponentListener() {
				@Override
				public void componentShown(ComponentEvent e) {
					// work();
				}
				
				@Override
				public void componentResized(ComponentEvent e) {
					work();
				}
				
				@Override
				public void componentMoved(ComponentEvent e) {
					// work();
				}
				
				@Override
				public void componentHidden(ComponentEvent e) {
					// work();
				}
				
				private void work() {
					if (data != null && fitRender) {
						render.setScaleToFit();
						render.update();
					}
					
				}
			}
		);
		
		rendererControlPanelListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				performRender(false);
			}
		};
	}
	
	private void preparePolygonSlider(
		JPanel panel,
		GridBagConstraints constraints
	) {
		polygons = new SliderSpinner(false);
		polygons.addChangeListener(
			e -> {
				if (!polygons.isEnabled()) {
					return;
				}
				
				accuracy.setEnabled(false);
				polygons.setEnabled(false);
				
				int newCount = (Integer) polygons.getValue();
				double low = data.getProximity(type, newCount);
				double high = (
					newCount < (Integer) polygons.getMaximum() ?
						data.getProximity(type, newCount + 1) :
						1.
				);
				
				double current = (Double) accuracy.getValue();
				if (current < low || current >= high) {
					accuracy.setValue(low);
				}
				
				accuracy.setEnabled(true);
				polygons.setEnabled(true);
				
				performRender(false);
			}
		);
		
		JLabel label = new JLabel("Polygons:");
		setLabelSize(label);
		
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridx = 0;
		panel.add(label, constraints);
		
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 1;
		panel.add(polygons, constraints);
	}
	
	private void prepareRendererCombo(
		JPanel panel,
		GridBagConstraints constraints
	) {
		Vector<AbstractMosaicRenderer> options = new Vector<>();
		AbstractMosaicRenderer initial = new BasicRenderer(
			this::showRenderResults
		);
		options.add(initial);
		options.add(new FilledWireframeRenderer(this::showRenderResults));
		options.add(new ColoringBookRenderer(this::showRenderResults));
		
		
		// TODO: Add more built-in renderers as I write them.
		
		// TODO: Add the capability to dynamically load renderers from a nearby
		// folder.
		
		options.sort((a, b) -> a.toString().compareTo(b.toString()));
		
		JComboBox<AbstractMosaicRenderer> combo = new JComboBox<>(options);
		combo.addActionListener(
			(e) -> {
				renderer = (AbstractMosaicRenderer) combo.getSelectedItem();
				
				boolean changed = false;
				if (rendererControls != null) {
					rendererControls.removeChangeListener(
						rendererControlPanelListener
					);
					wrapper.remove(rendererControls);
					changed = true;
				}
				
				rendererControls = renderer.getControls();
				if (rendererControls != null) {
					rendererControls.addChangeListener(
						rendererControlPanelListener
					);
					wrapper.add(rendererControls);
					changed = true;
				}
				
				if (changed) {
					wrapper.revalidate();
					wrapper.repaint();
				}
				
				if (data != null) {
					performRender(false);
				}
			}
		);
		combo.setSelectedItem(initial);
		
		
		JLabel label = new JLabel("Renderer:");
		setLabelSize(label);
		constraints.gridx = 0;
		constraints.fill = GridBagConstraints.NONE;
		panel.add(label, constraints);
		
		constraints.gridx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add(combo, constraints);
	}
	
	private void prepareSaveButton(
		JPanel panel,
		GridBagConstraints constraints
	) {
		// TODO: This is a quick hack just to make it so I can play with the
		// program for now.  This should be reworked later to be better.
		JButton button = new JButton("Save Render");
		button.addActionListener(
			(e) -> {
				JFileChooser dialog = new JFileChooser(".");
				if (
					data != null &&
					dialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION
				) {
					file = dialog.getSelectedFile();
					try {
						ImageIO.write(render.image, "PNG", file);
					} catch (IOException f) {}
				}
			}
		);
		
		constraints.gridwidth = 2;
		constraints.gridx = 0;
		constraints.fill = GridBagConstraints.NONE;
		panel.add(button, constraints);
	}
	
	private void prepareSplitTypeCombo(
		JPanel panel,
		GridBagConstraints constraints
	) {
		JComboBox<SplitType> combo = new JComboBox<>(SplitType.values());
		combo.addActionListener(
			(e) -> {
				SplitType newMethod = (SplitType) combo.getSelectedItem();
				if (!type.equals(newMethod)) {
					type = newMethod;
					
					if (data != null) {
						boolean enabled = polygons.isEnabled();
						accuracy.setEnabled(false);
						polygons.setEnabled(false);
						
						int oldMax = (Integer) polygons.getMaximum();
						int newMax = (Integer) data.getPolygonCount(type);
						if (oldMax != newMax) {
							polygons.update(1, newMax, 1);
						}
						
						accuracy.setValue(
							data.getProximity(
								type,
								(Integer) polygons.getValue()
							)
						);
						
						accuracy.setEnabled(enabled);
						polygons.setEnabled(enabled);
						
						performRender(false);
					}
				}
			}
		);
		
		JLabel label = new JLabel("Split Method:");
		setLabelSize(label);
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridx = 0;
		panel.add(label, constraints);
		
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 1;
		panel.add(combo, constraints);
	}
	
	private void prepareRenderer() {
		render = new RenderPanel();
		
		JScrollPane pane = new JScrollPane(render);
		pane.setViewportBorder(BorderFactory.createTitledBorder("Render"));
		add(pane, BorderLayout.CENTER);
	}
	
	private void setLabelSize(JLabel label) {
		label.setPreferredSize(
			new Dimension(
				LABEL_WIDTH,
				label.getPreferredSize().height
			)
		);
	}
	
	private void showRenderResults(BufferedImage image) {
		render.changeImage(image);
		render.update();
	}
}

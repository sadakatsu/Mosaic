package com.sadakatsu.mosaic.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.sadakatsu.mosaic.MosaicData;
import com.sadakatsu.mosaic.MosaicData.LoadProgress;
import com.sadakatsu.mosaic.MosaicData.Loader;
import com.sadakatsu.mosaic.MosaicData.SplitType;

public class LoadProgressDialog extends JOptionPane {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	private static final long serialVersionUID = -1061138151833700849L;
	
	private static final String[] BUTTON_TEXT = { "Cancel" };
	
	/*--------------------------- Types and Enums ----------------------------*/
	private static class DialogRunnable implements Runnable {
		public MosaicData data;
		
		private final BufferedImage image;
		private final Double threshold;
		private final File filename;
		
		public DialogRunnable(
			File filename,
			BufferedImage image,
			Double threshold
		) {
			this.image = image;
			this.filename = filename;
			this.threshold = threshold;
		}
		
		@Override
		public void run() {
			data = LoadProgressDialog.showLoaderDialog(
				filename,
				image,
				threshold
			);
		}
	}
	
	/*------------------------------ Interface -------------------------------*/
	public static MosaicData showLoaderDialog(
		File filename,
		BufferedImage image
	) {
		return showLoaderDialog(filename, image, null);
	}
	
	public static MosaicData showLoaderDialog(
		File filename,
		BufferedImage image,
		Double threshold
	) {
		MosaicData data;
		if (SwingUtilities.isEventDispatchThread()) {
			LoadProgressDialog pane = new LoadProgressDialog(
				image,
				threshold
			);
			pane.run(filename, image.getWidth(), image.getHeight());
			data = (MosaicData) pane.getValue();
		} else {
			DialogRunnable task = new DialogRunnable(
				filename,
				image,
				threshold
			);
			
			try {
				SwingUtilities.invokeAndWait(task);
				data = task.data;
			} catch (InvocationTargetException | InterruptedException e) {
				data = null;
			}
		}
		
		return data;
	}
	
	/*------------------------------- Helpers --------------------------------*/
	
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	private JDialog dialog;
	
	private JProgressBar[][] bar;
	
	private Loader loader;
	
	/*--------------------------- Types and Enums ----------------------------*/
	/*----------------------------- Constructors -----------------------------*/
	private LoadProgressDialog(BufferedImage image) {
		this(image, null);
	}
	
	private LoadProgressDialog(
		BufferedImage image,
		Double threshold
	) {
		super(null, PLAIN_MESSAGE, DEFAULT_OPTION, null, BUTTON_TEXT);
		loader = MosaicData.getLoader(image, threshold, this::processProgress);
		buildPanel();
		
	}
	
	/*------------------------------ Interface -------------------------------*/
	
	
	/*------------------------------- Helpers --------------------------------*/
	@Override
	public Object getValue() {
		try {
			return (loader.isDone() ? loader.get() : null);
		} catch (
			CancellationException |
			ExecutionException |
			InterruptedException e
		) {
			return null;
		}
	}
	
	private void buildPanel() {
		// Prepare to add everything to a panel to make the pane's message.
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		
		// Add labels to the layout manager.
		constraints.gridx = 1;
		constraints.gridy = 0;
		panel.add(new JLabel("Polygons"), constraints);
		
		constraints.gridx = 2;
		constraints.gridy = 0;
		panel.add(new JLabel("Accuracy"), constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 1;
		panel.add(new JLabel("STRAIGHT:"), constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 2;
		panel.add(new JLabel("DIAGONAL:"), constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 3;
		panel.add(new JLabel("BOTH:"), constraints);
		
		// Initialize the progress bars and add them to the panel.
		bar = new JProgressBar[3][];
		for (int i = 0; i < bar.length; ++i) {
			bar[i] = new JProgressBar[2];
			constraints.gridy = i + 1;
			for (int j = 0; j < bar[i].length; ++j) {
				JProgressBar current = bar[i][j] = new JProgressBar();
				current.setStringPainted(true);
				current.setString("");
				
				constraints.gridx = j + 1;
				panel.add(current, constraints);
			}
		}
		
		// Add a panel with all the labels and progress bars to this pane.
		setMessage(panel);
	}

	private void processProgress(LoadProgress progress) {
		for (SplitType method : SplitType.values()) {
			JProgressBar[] row = bar[method.index];
			
			int max = progress.getLimit(method);
			int value = progress.getLoaded(method);
			
			row[0].setMaximum(max);
			row[0].setString(String.format("%d / %d", value, max));
			row[0].setValue(value);
			
			double proximity = progress.getProximity(method) * 100;
			row[1].setValue((int) Math.round(proximity));
			row[1].setString(String.format("%.2f%%", proximity));
		}
		
		if (loader.isDone()) {
			dialog.dispose();
		}
	}
	
	private void run(File filename, int width, int height) {
		dialog = super.createDialog(
			String.format(
				"%s: %d x %d",
					filename.getName(),
						width,
						height
			)
		);
		
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		loader.execute();
		dialog.setVisible(true);
		Object result = super.getValue(); 
		if (result == null || result != UNINITIALIZED_VALUE) {
			loader.cancel(true);
		}
		dialog.dispose();
	}
}

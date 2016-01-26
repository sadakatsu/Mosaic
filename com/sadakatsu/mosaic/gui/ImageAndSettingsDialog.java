package com.sadakatsu.mosaic.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import com.sadakatsu.mosaic.MosaicData;
import com.sadakatsu.mosaic.Region;
import com.sadakatsu.util.Pair;

public class ImageAndSettingsDialog extends JOptionPane {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	private static final long serialVersionUID = 7329539474683026413L;
	
	/*--------------------------- Types and Enums ----------------------------*/
	private static class DialogRunnable implements Runnable {
		public Pair<File, MosaicData> data;
		
		@Override
		public void run() {
			data = ImageAndSettingsDialog.showImageDialog();
		}
	}
	
	private static class ImageFileFilter extends FileFilter {
		private static final String[] EXTENSIONS =
			ImageIO.getReaderFileSuffixes();
		
		@Override
		public boolean accept(File f) {
			boolean acceptable = false;
			
			if (f.isDirectory()) {
				acceptable = true;
			} else {						
				String name = f.getName().toLowerCase();
				for (String extension : EXTENSIONS) {
					if (name.endsWith(extension)) {
						acceptable = true;
						break;
					}
				}
			}
			return acceptable;
		}
		
		@Override
		public String getDescription() {
			return "Images";
		}
	}
	
	/*------------------------------ Interface -------------------------------*/
	public static Pair<File, MosaicData> showImageDialog() {
		Pair<File, MosaicData> result = null;
		
		if (SwingUtilities.isEventDispatchThread()) {
			ImageAndSettingsDialog pane = new ImageAndSettingsDialog();
			pane.run();
			result = new Pair<>(pane.file, (MosaicData) pane.getValue());
		} else {
			DialogRunnable task = new DialogRunnable();
			
			try {
				SwingUtilities.invokeAndWait(task);
				result = task.data;
			} catch (InvocationTargetException | InterruptedException e) {
				result = null;
			}
		}
		
		return result;
	}
	
	/*------------------------------- Helpers --------------------------------*/
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	private final String lastPath;
	
	private BufferedImage image;
	
	private File file;
	
	private JButton ok;
	
	private JDialog dialog;
	
	private JLabel area;
	private JLabel height;
	private JLabel maxRegions;
	private JLabel variance;
	private JLabel width;
	
	private JSlider accuracySlider;
	
	private JSpinner accuracySpinner;
	
	private JTextField filename;
	
	/*--------------------------- Types and Enums ----------------------------*/
	private class LoadImage extends SwingWorker<Void, Void> {
		private final File source;
		private final ImageAndSettingsDialog reference;
		
		private BufferedImage result;
		private Region region;
		
		public LoadImage(File file, ImageAndSettingsDialog reference) {
			this.source = file;
			this.reference = reference;
			region = null;
			result = null;
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			try {
				result = ImageIO.read(source);
				region = new Region();
				region.loadFrom(result);
			} catch (IOException e) {
				result = null;
				region = null;
			}
			return null;
		}

		@Override
		protected void done() {
			if (result != null) {
				area.setText(Long.toString(region.getArea()));
				height.setText(Integer.toString(region.getHeight()));
				maxRegions.setText(
					Long.toString(Region.getRecommendedMax(region))
				);
				variance.setText(Double.toString(region.getVariance()));
				width.setText(Integer.toString(region.getWidth()));
				
				image = result;
				
				ok.setEnabled(true);
			} else {
				area.setText("");
				filename.setText("");
				height.setText("");
				maxRegions.setText("");
				variance.setText("");
				width.setText("");
				
				JOptionPane.showMessageDialog(
					null,
					"The selected file could not be opened.  It probably is " +
					"not an image file.",
					"Could not open the file",
					JOptionPane.ERROR_MESSAGE
				);
				
				file = null;
				image = null;
			}
			
			reference.setCursor(Cursor.getDefaultCursor());
		}
	}
	
	private class SelectAction implements ActionListener {
		private final ImageAndSettingsDialog reference;
		
		public SelectAction(ImageAndSettingsDialog reference) {
			this.reference = reference;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			JFileChooser chooser = new JFileChooser(
				(lastPath == null ? "." : lastPath)
			);
			chooser.setFileFilter(new ImageFileFilter());
			
			if (
				chooser.showOpenDialog(reference) == JFileChooser.APPROVE_OPTION
			) {
				file = chooser.getSelectedFile();
				filename.setText(file.getPath());
				reference.setCursor(
					Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
				);
				ok.setEnabled(false);
				new LoadImage(file, reference).execute();
			}
		}
	}
	
	/*----------------------------- Constructors -----------------------------*/
	private ImageAndSettingsDialog() {
		this(null);
	}
	
	private ImageAndSettingsDialog(String lastPath) {
		this.lastPath = lastPath;
		preparePanel();
		prepareButtons();
	}
	
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
	private void run() {
		dialog = super.createDialog("Select an image to Mosaic!");
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
		dialog.dispose();
	}
	
	private void prepareButtons() {
		Object[] buttons = new Object[2];
		ok = new JButton("OK");
		ok.setEnabled(false);
		ok.addActionListener(
			(e) -> {
				MosaicData data = LoadProgressDialog.showLoaderDialog(
					file,
					image,
					accuracySlider.getValue() / 2000.
				);
				if (data != null) {
					setValue(data);
					dialog.dispose();
				}
			}
		);
		buttons[0] = ok;
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(
				(e) -> {
					setValue(null);
					dialog.dispose();
				}
		);
		buttons[1] = cancel;
		
		this.setOptions(buttons);
	}
	
	private void preparePanel() {
		final int labelWidth = 80;
		final int gap = 5;
		final Dimension dimension = new Dimension(labelWidth, 16);
		
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(gap, gap, gap, gap);
		GridBagConstraints original = (GridBagConstraints) constraints.clone();
		
		JLabel filenameLabel = new JLabel("Filename:");
		filenameLabel.setPreferredSize(dimension);
		filenameLabel.setMaximumSize(dimension);
		panel.add(filenameLabel, constraints);
		
		filename = new JTextField();
		filename.setEditable(false);
		filename.setPreferredSize(
			new Dimension(
				labelWidth * 4 + gap * 3,
				filename.getPreferredSize().height
			)
		);
		constraints.gridwidth = 4;
		constraints.gridx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add(filename, constraints);
		
		JButton select = new JButton("Select");
		select.addActionListener(new SelectAction(this));
		constraints = (GridBagConstraints) original.clone();
		constraints.gridx = 5;
		panel.add(select, constraints);
		
		JLabel widthLabel = new JLabel("Width:", JLabel.RIGHT);
		widthLabel.setPreferredSize(dimension);
		widthLabel.setMaximumSize(dimension);
		constraints.gridx = 0;
		constraints.gridy = 1;
		panel.add(widthLabel, constraints);
		
		width = new JLabel("", JLabel.RIGHT);
		width.setPreferredSize(dimension);
		width.setMaximumSize(dimension);
		constraints.gridx = 1;
		panel.add(width, constraints);
		
		JLabel heightLabel = new JLabel("Height:", JLabel.RIGHT);
		heightLabel.setPreferredSize(dimension);
		heightLabel.setMaximumSize(dimension);
		constraints.gridx = 2;
		panel.add(heightLabel, constraints);
		
		height = new JLabel("", JLabel.RIGHT);
		height.setPreferredSize(dimension);
		height.setMaximumSize(dimension);
		constraints.gridx = 3;
		panel.add(height, constraints);
		
		JLabel areaLabel = new JLabel("Area:", JLabel.RIGHT);
		areaLabel.setPreferredSize(dimension);
		areaLabel.setMaximumSize(dimension);
		constraints.gridx = 4;
		panel.add(areaLabel, constraints);
		
		area = new JLabel("", JLabel.RIGHT);
		area.setPreferredSize(dimension);
		area.setMaximumSize(dimension);
		constraints.gridx = 5;
		panel.add(area, constraints);
		
		JLabel maxRegionsLabel = new JLabel("Max Regions:", JLabel.RIGHT);
		maxRegionsLabel.setPreferredSize(dimension);
		maxRegionsLabel.setMaximumSize(dimension);
		constraints.gridx = 0;
		constraints.gridy = 2;
		panel.add(maxRegionsLabel, constraints);
		
		maxRegions = new JLabel("", JLabel.RIGHT);
		maxRegions.setPreferredSize(dimension);
		maxRegions.setMaximumSize(dimension);
		constraints.gridx = 1;
		panel.add(maxRegions, constraints);
		
		JLabel varianceLabel = new JLabel("Variance:", JLabel.RIGHT);
		varianceLabel.setPreferredSize(dimension);
		varianceLabel.setMaximumSize(dimension);
		constraints.gridx = 2;
		panel.add(varianceLabel, constraints);
		
		variance = new JLabel("", JLabel.RIGHT);
		variance.setPreferredSize(dimension);
		variance.setMaximumSize(dimension);
		constraints.gridx = 3;
		panel.add(variance, constraints);
		
		JLabel accuracyLabel = new JLabel("Accuracy:", JLabel.RIGHT);
		constraints.anchor = GridBagConstraints.EAST;
		constraints.gridx = 1;
		constraints.gridy = 3;
		panel.add(accuracyLabel, constraints);
		
		accuracySpinner = new JSpinner(
			new SpinnerNumberModel(
				100.,
				0.,
				100.,
				0.05
			)
		);
		accuracySpinner.setPreferredSize(dimension);
		accuracySpinner.setMaximumSize(dimension);
		
		accuracySpinner.addChangeListener(
			new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					Double value = (Double) accuracySpinner.getValue();
					if (value < 0) {
						value = 0.;
					} else if (value > 100) {
						value = 100.;
					} else {
						value = ((int) Math.round(value * 20)) / 20.;
					}
					accuracySpinner.setValue(value);
					accuracySlider.setValue((int) Math.round(value * 20));
				}
			}
		);
		
		constraints.gridx = 2;
		panel.add(accuracySpinner, constraints);
		
		accuracySlider = new JSlider(0, 2000, 2000);
		accuracySlider.setPreferredSize(
			new Dimension(
				labelWidth * 2 + gap,
				accuracySlider.getPreferredSize().height * 2
			)
		);
		accuracySlider.setMaximumSize(accuracySlider.getPreferredSize());
		accuracySlider.setPaintTicks(true);
		accuracySlider.setMajorTickSpacing(200);
		accuracySlider.setMinorTickSpacing(40);
		
		accuracySlider.addChangeListener(
			new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					accuracySpinner.setValue(
						accuracySlider.getValue() / 20.
					);
				}
				
			}
		);
		
		constraints.gridx = 3;
		constraints.gridwidth = 2;
		panel.add(accuracySlider, constraints);
		
		setMessage(panel);
	}
}

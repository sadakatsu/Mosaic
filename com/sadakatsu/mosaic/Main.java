package com.sadakatsu.mosaic;

import javax.swing.SwingUtilities;

import com.sadakatsu.mosaic.gui.MosaicApplication;

public class Main {
	public static void main(String[] args) throws Exception {
		SwingUtilities.invokeLater(
			new Runnable() {
				@Override
				public void run() {
					new MosaicApplication();
				}
			}
		);
	}
}

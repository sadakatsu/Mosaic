package com.sadakatsu.mosaic.renderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import com.sadakatsu.mosaic.MosaicData;
import com.sadakatsu.mosaic.MosaicData.SplitType;

public abstract class AbstractMosaicRenderer {
	/*================================ CLASS =================================*/
	/*-------------------------------- Fields --------------------------------*/
	/*--------------------------- Types and Enums ----------------------------*/
	public static class RendererControlPanel
	extends JPanel
	implements ActionListener {
		private static final long serialVersionUID = -4762570424673413709L;
		
		private final EventListenerList listenerList = new EventListenerList();
		
		public void addChangeListener(ChangeListener x) {
			listenerList.add(ChangeListener.class, x);
		}
		
		public void removeChangeListener(ChangeListener x) {
			listenerList.remove(ChangeListener.class, x);
		}
		
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

		@Override
		public void actionPerformed(ActionEvent e) {
			fireStateChanged();
		}
	}
	
	@FunctionalInterface
	public static interface WhenDone {
		public void perform(BufferedImage result);
	}
	
	/*------------------------------ Interface -------------------------------*/
	/*------------------------------- Helpers --------------------------------*/
	
	/*=============================== INSTANCE ===============================*/
	/*-------------------------------- Fields --------------------------------*/
	private final WhenDone hook;
	
	/*--------------------------- Types and Enums ----------------------------*/
	protected abstract class RenderWorker extends SwingWorker<Void, Void> {
		protected BufferedImage image;
		protected final int count;
		protected final MosaicData data;
		protected final SplitType method;
		
		private boolean stopped;
		
		public RenderWorker(MosaicData data, SplitType method, int count) {
			this.count = count;
			this.data = data;
			this.method = method;
		}
		
		public boolean isStopped() {
			if (!stopped && (isCancelled() || Thread.interrupted())) {
				stopped = true;
			}
			
			return stopped;
		}
		
		@Override
		protected void done() {
			if (hook != null) {
				synchronized (hook) {
					if (!isStopped()) {
						hook.perform(image);
					}
				}
			}
		}
		
		protected abstract void performRender();
		
		@Override
		protected Void doInBackground() {
			image = new BufferedImage(
				data.getWidth(),
				data.getHeight(),
				BufferedImage.TYPE_3BYTE_BGR
			);
			performRender();
			return null;
		}
	}
	
	/*----------------------------- Constructors -----------------------------*/
	public AbstractMosaicRenderer(WhenDone hook) {
		this.hook = hook;
	}
	
	/*------------------------------ Interface -------------------------------*/
	public abstract RendererControlPanel getControls();
	
	public abstract RenderWorker buildThread(
		MosaicData data,
		SplitType method,
		int count
	);
	
	@Override
	public abstract String toString();
	
	/*------------------------------- Helpers --------------------------------*/
}

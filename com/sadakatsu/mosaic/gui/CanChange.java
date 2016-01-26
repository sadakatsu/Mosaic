package com.sadakatsu.mosaic.gui;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

public abstract class CanChange {
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
				((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
			}
		}
	}
}
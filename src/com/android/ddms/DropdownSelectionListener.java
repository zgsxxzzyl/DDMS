package com.android.ddms;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import com.android.ddmlib.Log;

public class DropdownSelectionListener extends SelectionAdapter {
	private Menu mMenu;
	private ToolItem mDropdown;

	public DropdownSelectionListener(ToolItem item) {
		this.mDropdown = item;
		this.mMenu = new Menu(item.getParent().getShell(), 8);
	}

	public void add(String label) {
		MenuItem item = new MenuItem(this.mMenu, 0);
		item.setText(label);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MenuItem sel = (MenuItem) e.widget;
				DropdownSelectionListener.this.mDropdown.setText(sel.getText());
			}
		});
	}

	public void widgetSelected(SelectionEvent e) {
		if (e.detail == 4) {
			ToolItem item = (ToolItem) e.widget;
			Rectangle rect = item.getBounds();
			Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
			this.mMenu.setLocation(pt.x, pt.y + rect.height);
			this.mMenu.setVisible(true);
		} else {
			Log.d("ddms", this.mDropdown.getText() + " Pressed");
		}
	}
}

/* Location:           D:\android-sdk-windows\tools\lib\ddms.jar

 * Qualified Name:     com.android.ddms.DropdownSelectionListener

 * JD-Core Version:    0.7.0.1

 */
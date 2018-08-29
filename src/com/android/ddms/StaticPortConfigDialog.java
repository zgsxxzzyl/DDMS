package com.android.ddms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.android.ddmuilib.TableHelper;

public class StaticPortConfigDialog extends Dialog {
	private static final String PREFS_DEVICE_COL = "spcd.deviceColumn";
	private static final String PREFS_APP_COL = "spcd.AppColumn";
	private static final String PREFS_PORT_COL = "spcd.PortColumn";
	private static final int COL_DEVICE = 0;
	private static final int COL_APPLICATION = 1;
	private static final int COL_PORT = 2;
	private static final int DLG_WIDTH = 500;
	private static final int DLG_HEIGHT = 300;
	private Shell mShell;
	private Shell mParent;
	private Table mPortTable;
	private ArrayList<Integer> mPorts = new ArrayList();

	public StaticPortConfigDialog(Shell parent) {
		super(parent, 67680);
	}

	public void open() {
		createUI();
		if ((this.mParent == null) || (this.mShell == null)) {
			return;
		}
		updateFromStore();

		this.mShell.setMinimumSize(500, 300);
		Rectangle r = this.mParent.getBounds();

		int cx = r.x + r.width / 2;
		int x = cx - 250;
		int cy = r.y + r.height / 2;
		int y = cy - 150;
		this.mShell.setBounds(x, y, 500, 300);

		this.mShell.pack();

		this.mShell.open();

		Display display = this.mParent.getDisplay();
		while (!this.mShell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private void createUI() {
		this.mParent = getParent();
		this.mShell = new Shell(this.mParent, getStyle());
		this.mShell.setText("Static Port Configuration");

		this.mShell.setLayout(new GridLayout(1, true));

		this.mShell.addListener(21, new Listener() {
			public void handleEvent(Event event) {
				event.doit = true;
			}
		});
		Composite main = new Composite(this.mShell, 0);
		main.setLayoutData(new GridData(1808));
		main.setLayout(new GridLayout(2, false));

		this.mPortTable = new Table(main, 65540);
		this.mPortTable.setLayoutData(new GridData(1808));
		this.mPortTable.setHeaderVisible(true);
		this.mPortTable.setLinesVisible(true);

		TableHelper.createTableColumn(this.mPortTable, "Device Serial Number", 16384, "emulator-5554",
				"spcd.deviceColumn", PrefsDialog.getStore());

		TableHelper.createTableColumn(this.mPortTable, "Application Package", 16384, "com.android.samples.phone",
				"spcd.AppColumn", PrefsDialog.getStore());

		TableHelper.createTableColumn(this.mPortTable, "Debug Port", 131072, "Debug Port", "spcd.PortColumn",
				PrefsDialog.getStore());

		Composite buttons = new Composite(main, 0);
		buttons.setLayoutData(new GridData(1040));
		buttons.setLayout(new GridLayout(1, true));

		Button newButton = new Button(buttons, 0);
		newButton.setText("New...");
		newButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StaticPortEditDialog dlg = new StaticPortEditDialog(StaticPortConfigDialog.this.mShell,
						StaticPortConfigDialog.this.mPorts);
				if (dlg.open()) {
					String device = dlg.getDeviceSN();
					String app = dlg.getAppName();
					int port = dlg.getPortNumber();

					StaticPortConfigDialog.this.addEntry(device, app, port);
				}
			}
		});
		final Button editButton = new Button(buttons, 0);
		editButton.setText("Edit...");
		editButton.setEnabled(false);
		editButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = StaticPortConfigDialog.this.mPortTable.getSelectionIndex();
				String oldDeviceName = StaticPortConfigDialog.this.getDeviceName(index);
				String oldAppName = StaticPortConfigDialog.this.getAppName(index);
				String oldPortNumber = StaticPortConfigDialog.this.getPortNumber(index);
				StaticPortEditDialog dlg = new StaticPortEditDialog(StaticPortConfigDialog.this.mShell,
						StaticPortConfigDialog.this.mPorts, oldDeviceName, oldAppName, oldPortNumber);
				if (dlg.open()) {
					String deviceName = dlg.getDeviceSN();
					String app = dlg.getAppName();
					int port = dlg.getPortNumber();

					StaticPortConfigDialog.this.replaceEntry(index, deviceName, app, port);
				}
			}
		});
		final Button deleteButton = new Button(buttons, 0);
		deleteButton.setText("Delete");
		deleteButton.setEnabled(false);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = StaticPortConfigDialog.this.mPortTable.getSelectionIndex();
				StaticPortConfigDialog.this.removeEntry(index);
			}
		});
		Composite bottomComp = new Composite(this.mShell, 0);
		bottomComp.setLayoutData(new GridData(64));

		bottomComp.setLayout(new GridLayout(2, true));

		Button okButton = new Button(bottomComp, 0);
		okButton.setText("OK");
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StaticPortConfigDialog.this.updateStore();
				StaticPortConfigDialog.this.mShell.close();
			}
		});
		Button cancelButton = new Button(bottomComp, 0);
		cancelButton.setText("Cancel");
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StaticPortConfigDialog.this.mShell.close();
			}
		});
		this.mPortTable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = StaticPortConfigDialog.this.mPortTable.getSelectionIndex();

				boolean enabled = index != -1;
				editButton.setEnabled(enabled);
				deleteButton.setEnabled(enabled);
			}
		});
		this.mShell.pack();
	}

	private void addEntry(String deviceName, String appName, int portNumber) {
		TableItem item = new TableItem(this.mPortTable, 0);

		item.setText(0, deviceName);
		item.setText(1, appName);
		item.setText(2, Integer.toString(portNumber));

		this.mPorts.add(Integer.valueOf(portNumber));
	}

	private void removeEntry(int index) {
		this.mPortTable.remove(index);

		this.mPorts.remove(index);
	}

	private void replaceEntry(int index, String deviceName, String appName, int portNumber) {
		TableItem item = this.mPortTable.getItem(index);

		item.setText(0, deviceName);
		item.setText(1, appName);
		item.setText(2, Integer.toString(portNumber));

		this.mPorts.set(index, Integer.valueOf(portNumber));
	}

	private String getDeviceName(int index) {
		TableItem item = this.mPortTable.getItem(index);
		return item.getText(0);
	}

	private String getAppName(int index) {
		TableItem item = this.mPortTable.getItem(index);
		return item.getText(1);
	}

	private String getPortNumber(int index) {
		TableItem item = this.mPortTable.getItem(index);
		return item.getText(2);
	}

	private void updateFromStore() {
		DebugPortProvider provider = DebugPortProvider.getInstance();
		Map<String, Map<String, Integer>> map = provider.getPortList();

		Set<String> deviceKeys = map.keySet();
		for (Iterator it = deviceKeys.iterator(); it.hasNext();) {
			String deviceKey = (String) it.next();
			Map deviceMap = (Map) map.get(deviceKey);
			if (deviceMap != null) {
				Set<String> appKeys = deviceMap.keySet();
				for (String appKey : appKeys) {
					Integer port = (Integer) deviceMap.get(appKey);
					if (port != null) {
						addEntry(deviceKey, appKey, port.intValue());
					}
				}
			}
		}
		String deviceKey;
		Map<String, Integer> deviceMap;
	}

	private void updateStore() {
		HashMap<String, Map<String, Integer>> map = new HashMap();

		int count = this.mPortTable.getItemCount();
		for (int i = 0; i < count; i++) {
			TableItem item = this.mPortTable.getItem(i);
			String deviceName = item.getText(0);

			Map<String, Integer> deviceMap = (Map) map.get(deviceName);
			if (deviceMap == null) {
				deviceMap = new HashMap();
				map.put(deviceName, deviceMap);
			}
			deviceMap.put(item.getText(1), Integer.valueOf(item.getText(2)));
		}
		DebugPortProvider provider = DebugPortProvider.getInstance();
		provider.setPortList(map);
	}
}

/* Location:           D:\android-sdk-windows\tools\lib\ddms.jar

 * Qualified Name:     com.android.ddms.StaticPortConfigDialog

 * JD-Core Version:    0.7.0.1

 */
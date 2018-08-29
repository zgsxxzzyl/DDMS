package com.android.ddms;

import java.util.ArrayList;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class StaticPortEditDialog extends Dialog {
	private static final int DLG_WIDTH = 400;
	private static final int DLG_HEIGHT = 200;
	private Shell mParent;
	private Shell mShell;
	private boolean mOk = false;
	private String mAppName;
	private String mPortNumber;
	private Button mOkButton;
	private Label mWarning;
	private ArrayList<Integer> mPorts;
	private int mEditPort = -1;
	private String mDeviceSn;

	public StaticPortEditDialog(Shell parent, ArrayList<Integer> ports) {
		super(parent, 67680);
		this.mPorts = ports;
		this.mDeviceSn = "emulator-5554";
	}

	public StaticPortEditDialog(Shell shell, ArrayList<Integer> ports, String oldDeviceSN, String oldAppName,
			String oldPortNumber) {
		this(shell, ports);

		this.mDeviceSn = oldDeviceSN;
		this.mAppName = oldAppName;
		this.mPortNumber = oldPortNumber;
		this.mEditPort = Integer.valueOf(this.mPortNumber).intValue();
	}

	public boolean open() {
		createUI();
		if ((this.mParent == null) || (this.mShell == null)) {
			return false;
		}
		this.mShell.setMinimumSize(400, 200);
		Rectangle r = this.mParent.getBounds();

		int cx = r.x + r.width / 2;
		int x = cx - 200;
		int cy = r.y + r.height / 2;
		int y = cy - 100;
		this.mShell.setBounds(x, y, 400, 200);

		this.mShell.open();

		Display display = this.mParent.getDisplay();
		while (!this.mShell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return this.mOk;
	}

	public String getDeviceSN() {
		return this.mDeviceSn;
	}

	public String getAppName() {
		return this.mAppName;
	}

	public int getPortNumber() {
		return Integer.valueOf(this.mPortNumber).intValue();
	}

	private void createUI() {
		this.mParent = getParent();
		this.mShell = new Shell(this.mParent, getStyle());
		this.mShell.setText("Static Port");

		this.mShell.setLayout(new GridLayout(1, false));

		this.mShell.addListener(21, new Listener() {
			public void handleEvent(Event event) {
			}
		});
		Composite main = new Composite(this.mShell, 0);
		main.setLayoutData(new GridData(1808));
		main.setLayout(new GridLayout(2, false));

		Label l0 = new Label(main, 0);
		l0.setText("Device Name:");

		final Text deviceSNText = new Text(main, 2052);
		deviceSNText.setLayoutData(new GridData(768));
		if (this.mDeviceSn != null) {
			deviceSNText.setText(this.mDeviceSn);
		}
		deviceSNText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				StaticPortEditDialog.this.mDeviceSn = deviceSNText.getText().trim();
				StaticPortEditDialog.this.validate();
			}
		});
		Label l = new Label(main, 0);
		l.setText("Application Name:");

		final Text appNameText = new Text(main, 2052);
		if (this.mAppName != null) {
			appNameText.setText(this.mAppName);
		}
		appNameText.setLayoutData(new GridData(768));
		appNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				StaticPortEditDialog.this.mAppName = appNameText.getText().trim();
				StaticPortEditDialog.this.validate();
			}
		});
		Label l2 = new Label(main, 0);
		l2.setText("Debug Port:");

		final Text debugPortText = new Text(main, 2052);
		if (this.mPortNumber != null) {
			debugPortText.setText(this.mPortNumber);
		}
		debugPortText.setLayoutData(new GridData(768));
		debugPortText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				StaticPortEditDialog.this.mPortNumber = debugPortText.getText().trim();
				StaticPortEditDialog.this.validate();
			}
		});
		Composite warningComp = new Composite(this.mShell, 0);
		warningComp.setLayoutData(new GridData(768));
		warningComp.setLayout(new GridLayout(1, true));

		this.mWarning = new Label(warningComp, 0);
		this.mWarning.setText("");
		this.mWarning.setLayoutData(new GridData(768));

		Composite bottomComp = new Composite(this.mShell, 0);
		bottomComp.setLayoutData(new GridData(64));

		bottomComp.setLayout(new GridLayout(2, true));

		this.mOkButton = new Button(bottomComp, 0);
		this.mOkButton.setText("OK");
		this.mOkButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StaticPortEditDialog.this.mOk = true;
				StaticPortEditDialog.this.mShell.close();
			}
		});
		this.mOkButton.setEnabled(false);
		this.mShell.setDefaultButton(this.mOkButton);

		Button cancelButton = new Button(bottomComp, 0);
		cancelButton.setText("Cancel");
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StaticPortEditDialog.this.mShell.close();
			}
		});
		validate();
	}

	private void validate() {
		this.mWarning.setText("");
		if ((this.mDeviceSn == null) || (this.mDeviceSn.length() == 0)) {
			this.mWarning.setText("Device name missing.");
			this.mOkButton.setEnabled(false);
			return;
		}
		if ((this.mAppName == null) || (this.mAppName.length() == 0)) {
			this.mWarning.setText("Application name missing.");
			this.mOkButton.setEnabled(false);
			return;
		}
		String packageError = "Application name must be a valid Java package name.";

		String[] packageSegments = this.mAppName.split("\\.");
		for (String p : packageSegments) {
			if (!p.matches("^[a-zA-Z][a-zA-Z0-9]*")) {
				this.mWarning.setText(packageError);
				this.mOkButton.setEnabled(false);
				return;
			}
			if (!p.matches("^[a-z][a-z0-9]*")) {
				this.mWarning.setText("Lower case is recommended for Java packages.");
			}
		}
		if (this.mAppName.charAt(this.mAppName.length() - 1) == '.') {
			this.mWarning.setText(packageError);
			this.mOkButton.setEnabled(false);
			return;
		}
		if ((this.mPortNumber == null) || (this.mPortNumber.length() == 0)) {
			this.mWarning.setText("Port Number missing.");
			this.mOkButton.setEnabled(false);
			return;
		}
		if (!this.mPortNumber.matches("[0-9]*")) {
			this.mWarning.setText("Port Number invalid.");
			this.mOkButton.setEnabled(false);
			return;
		}
		long port = Long.valueOf(this.mPortNumber).longValue();
		if (port >= 32767L) {
			this.mOkButton.setEnabled(false);
			return;
		}
		if (port != this.mEditPort) {
			for (Integer i : this.mPorts) {
				if (port == i.intValue()) {
					this.mWarning.setText("Port already in use.");
					this.mOkButton.setEnabled(false);
					return;
				}
			}
		}
		this.mOkButton.setEnabled(true);
	}
}

/* Location:           D:\android-sdk-windows\tools\lib\ddms.jar

 * Qualified Name:     com.android.ddms.StaticPortEditDialog

 * JD-Core Version:    0.7.0.1

 */
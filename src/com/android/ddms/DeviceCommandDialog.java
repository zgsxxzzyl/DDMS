package com.android.ddms;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.Log;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

public class DeviceCommandDialog extends Dialog {
	public static final int DEVICE_STATE = 0;
	public static final int APP_STATE = 1;
	public static final int RADIO_STATE = 2;
	public static final int LOGCAT = 3;
	private String mCommand;
	private String mFileName;
	private Label mStatusLabel;
	private Button mCancelDone;
	private Button mSave;
	private Text mText;
	private Font mFont = null;
	private boolean mCancel;
	private boolean mFinished;

	public DeviceCommandDialog(String command, String fileName, Shell parent) {
		this(command, fileName, parent, 67696);
	}

	public DeviceCommandDialog(String command, String fileName, Shell parent, int style) {
		super(parent, style);
		this.mCommand = command;
		this.mFileName = fileName;
	}

	public void open(IDevice currentDevice) {
		Shell parent = getParent();
		Shell shell = new Shell(parent, getStyle());
		shell.setText("Remote Command");

		this.mFinished = false;
		this.mFont = findFont(shell.getDisplay());
		createContents(shell);

		shell.setMinimumSize(500, 200);
		shell.setSize(800, 600);
		shell.open();

		executeCommand(shell, currentDevice);

		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if (this.mFont != null) {
			this.mFont.dispose();
		}
	}

	private void createContents(final Shell shell) {
		shell.setLayout(new GridLayout(2, true));

		shell.addListener(21, new Listener() {
			public void handleEvent(Event event) {
				if (!DeviceCommandDialog.this.mFinished) {
					Log.d("ddms", "NOT closing - cancelling command");
					event.doit = false;
					DeviceCommandDialog.this.mCancel = true;
				}
			}
		});
		this.mStatusLabel = new Label(shell, 0);
		this.mStatusLabel.setText("Executing '" + shortCommandString() + "'");
		GridData data = new GridData(32);
		data.horizontalSpan = 2;
		this.mStatusLabel.setLayoutData(data);

		this.mText = new Text(shell, 770);
		this.mText.setEditable(false);
		this.mText.setFont(this.mFont);
		data = new GridData(1808);
		data.horizontalSpan = 2;
		this.mText.setLayoutData(data);

		this.mSave = new Button(shell, 8);
		this.mSave.setText("Save");
		data = new GridData(64);
		data.widthHint = 80;
		this.mSave.setLayoutData(data);
		this.mSave.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DeviceCommandDialog.this.saveText(shell);
			}
		});
		this.mSave.setEnabled(false);

		this.mCancelDone = new Button(shell, 8);
		this.mCancelDone.setText("Cancel");
		data = new GridData(64);
		data.widthHint = 80;
		this.mCancelDone.setLayoutData(data);
		this.mCancelDone.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!DeviceCommandDialog.this.mFinished) {
					DeviceCommandDialog.this.mCancel = true;
				} else {
					shell.close();
				}
			}
		});
	}

	private Font findFont(Display display) {
		String fontStr = PrefsDialog.getStore().getString("textOutputFont");
		if (fontStr != null) {
			FontData fdat = new FontData(fontStr);
			if (fdat != null) {
				return new Font(display, fdat);
			}
		}
		return null;
	}

	class Gatherer extends Thread implements IShellOutputReceiver {
		public static final int RESULT_UNKNOWN = 0;
		public static final int RESULT_SUCCESS = 1;
		public static final int RESULT_FAILURE = 2;
		public static final int RESULT_CANCELLED = 3;
		private Shell mShell;
		private String mCommand;
		private Text mText;
		private int mResult;
		private IDevice mDevice;

		public Gatherer(Shell shell, IDevice device, String command, Text text) {
			this.mShell = shell;
			this.mDevice = device;
			this.mCommand = command;
			this.mText = text;
			this.mResult = 0;

			DeviceCommandDialog.this.mCancel = false;
		}

		public void run() {
			if (this.mDevice == null) {
				Log.w("ddms", "Cannot execute command: no device selected.");
				this.mResult = 2;
			} else {
				try {
					this.mDevice.executeShellCommand(this.mCommand, this);
					if (DeviceCommandDialog.this.mCancel) {
						this.mResult = 3;
					} else {
						this.mResult = 1;
					}
				} catch (IOException ioe) {
					Log.w("ddms", "Remote exec failed: " + ioe.getMessage());
					this.mResult = 2;
				} catch (TimeoutException e) {
					Log.w("ddms", "Remote exec failed: " + e.getMessage());
					this.mResult = 2;
				} catch (AdbCommandRejectedException e) {
					Log.w("ddms", "Remote exec failed: " + e.getMessage());
					this.mResult = 2;
				} catch (ShellCommandUnresponsiveException e) {
					Log.w("ddms", "Remote exec failed: " + e.getMessage());
					this.mResult = 2;
				}
			}
			this.mShell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					DeviceCommandDialog.this.updateForResult(DeviceCommandDialog.Gatherer.this.mResult);
				}
			});
		}

		public void addOutput(byte[] data, int offset, int length) {
			Log.v("ddms", "received " + length + " bytes");
			try {
				final String text = new String(data, offset, length, "ISO-8859-1");

				this.mText.getDisplay().asyncExec(new Runnable() {
					public void run() {
						DeviceCommandDialog.Gatherer.this.mText.append(text);
					}
				});
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			}
		}

		public void flush() {
		}

		public boolean isCancelled() {
			return DeviceCommandDialog.this.mCancel;
		}
	}

	private void executeCommand(Shell shell, IDevice device) {
		Gatherer gath = new Gatherer(shell, device, commandString(), this.mText);
		gath.start();
	}

	private void updateForResult(int result) {
		if (result == 1) {
			this.mStatusLabel.setText("Successfully executed '" + shortCommandString() + "'");

			this.mSave.setEnabled(true);
		} else if (result == 3) {
			this.mStatusLabel.setText("Execution cancelled; partial results below");
			this.mSave.setEnabled(true);
		} else if (result == 2) {
			this.mStatusLabel.setText("Failed");
		}
		this.mStatusLabel.pack();
		this.mCancelDone.setText("Done");
		this.mFinished = true;
	}

	private void saveText(Shell shell) {
		FileDialog dlg = new FileDialog(shell, 8192);

		dlg.setText("Save output...");
		dlg.setFileName(defaultFileName());
		dlg.setFilterPath(PrefsDialog.getStore().getString("lastTextSaveDir"));
		dlg.setFilterNames(new String[] { "Text Files (*.txt)" });

		dlg.setFilterExtensions(new String[] { "*.txt" });

		String fileName = dlg.open();
		if (fileName != null) {
			PrefsDialog.getStore().setValue("lastTextSaveDir", dlg.getFilterPath());

			Log.d("ddms", "Saving output to " + fileName);

			String text = this.mText.getText();
			byte[] ascii;
			try {
				ascii = text.getBytes("ISO-8859-1");
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
				ascii = new byte[0];
			}
			try {
				int length = ascii.length;

				FileOutputStream outFile = new FileOutputStream(fileName);
				BufferedOutputStream out = new BufferedOutputStream(outFile);
				for (int i = 0; i < length; i++) {
					if ((i >= length - 1) || (ascii[i] != 13) || (ascii[(i + 1)] != 10)) {
						out.write(ascii[i]);
					}
				}
				out.close();
			} catch (IOException ioe) {
				Log.w("ddms", "Unable to save " + fileName + ": " + ioe);
			}
		}
	}

	private String commandString() {
		return this.mCommand;
	}

	private String defaultFileName() {
		return this.mFileName;
	}

	private String shortCommandString() {
		String str = commandString();
		if (str.length() > 50) {
			return str.substring(0, 50) + "...";
		}
		return str;
	}
}

/* Location:           D:\android-sdk-windows\tools\lib\ddms.jar

 * Qualified Name:     com.android.ddms.DeviceCommandDialog

 * JD-Core Version:    0.7.0.1

 */
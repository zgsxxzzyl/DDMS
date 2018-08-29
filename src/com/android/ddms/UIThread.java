package com.android.ddms;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.android.SdkConstants;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmuilib.AllocationPanel;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.ddmuilib.DevicePanel;
import com.android.ddmuilib.EmulatorControlPanel;
import com.android.ddmuilib.HeapPanel;
import com.android.ddmuilib.ITableFocusListener;
import com.android.ddmuilib.ImageLoader;
import com.android.ddmuilib.InfoPanel;
import com.android.ddmuilib.ScreenShotDialog;
import com.android.ddmuilib.SysinfoPanel;
import com.android.ddmuilib.TablePanel;
import com.android.ddmuilib.ThreadPanel;
import com.android.ddmuilib.actions.ToolItemAction;
import com.android.ddmuilib.explorer.DeviceExplorer;
import com.android.ddmuilib.handler.BaseFileHandler;
import com.android.ddmuilib.handler.MethodProfilingHandler;
import com.android.ddmuilib.log.event.EventLogPanel;
import com.android.ddmuilib.logcat.LogCatPanel;
import com.android.ddmuilib.logcat.LogColors;
import com.android.ddmuilib.logcat.LogFilter;
import com.android.ddmuilib.logcat.LogPanel;
import com.android.ddmuilib.net.NetworkPanel;
import com.android.ddmuilib.screenrecord.ScreenRecorderAction;
import com.android.menubar.IMenuBarCallback;
import com.android.menubar.IMenuBarEnhancer;
import com.android.menubar.MenuBarEnhancer;

public class UIThread implements DevicePanel.IUiSelectionListener, AndroidDebugBridge.IClientChangeListener {
	public static final String APP_NAME = "DDMS";
	public static final int PANEL_CLIENT_LIST = -1;
	public static final int PANEL_INFO = 0;
	public static final int PANEL_THREAD = 1;
	public static final int PANEL_HEAP = 2;
	private static final int PANEL_NATIVE_HEAP = 3;
	private static final int PANEL_ALLOCATIONS = 4;
	private static final int PANEL_SYSINFO = 5;
	private static final int PANEL_NETWORK = 6;
	private static final int PANEL_COUNT = 7;
	private static TablePanel[] mPanels = new TablePanel[7];
	private static final String[] mPanelNames = { "Info", "Threads", "VM Heap", "Native Heap", "Allocation Tracker",
			"Sysinfo", "Network" };
	private static final String[] mPanelTips = { "Client information", "Thread status", "VM heap status",
			"Native heap status", "Allocation Tracker", "Sysinfo graphs", "Network usage" };
	private static final String PREFERENCE_LOGSASH = "logSashLocation";
	private static final String PREFERENCE_SASH = "sashLocation";
	private static final String PREFS_COL_TIME = "logcat.time";
	private static final String PREFS_COL_LEVEL = "logcat.level";
	private static final String PREFS_COL_PID = "logcat.pid";
	private static final String PREFS_COL_TAG = "logcat.tag";
	private static final String PREFS_COL_MESSAGE = "logcat.message";
	private static final String PREFS_FILTERS = "logcat.filter";
	private static UIThread mInstance = new UIThread();
	private Display mDisplay;
	private DevicePanel mDevicePanel;
	private IDevice mCurrentDevice = null;
	private Client mCurrentClient = null;
	private Label mStatusLine;
	private ToolItem mTBShowThreadUpdates;
	private ToolItem mTBShowHeapUpdates;
	private ToolItem mTBHalt;
	private ToolItem mTBCauseGc;
	private ToolItem mTBDumpHprof;
	private ToolItem mTBProfiling;

	private final class FilterStorage implements LogPanel.ILogFilterStorageManager {
		private FilterStorage() {
		}

		public LogFilter[] getFilterFromStore() {
			String filterPrefs = PrefsDialog.getStore().getString("logcat.filter");

			String[] filters = filterPrefs.split("\\|");

			ArrayList<LogFilter> list = new ArrayList(filters.length);
			for (String f : filters) {
				if (f.length() > 0) {
					LogFilter logFilter = new LogFilter();
					if (logFilter.loadFromString(f)) {
						list.add(logFilter);
					}
				}
			}
			return (LogFilter[]) list.toArray(new LogFilter[list.size()]);
		}

		public void saveFilters(LogFilter[] filters) {
			StringBuilder sb = new StringBuilder();
			for (LogFilter f : filters) {
				String filterString = f.toString();
				sb.append(filterString);
				sb.append('|');
			}
			PrefsDialog.getStore().setValue("logcat.filter", sb.toString());
		}

		public boolean requiresDefaultFilter() {
			return true;
		}
	}

	private static final String USE_OLD_LOGCAT_VIEW = System.getenv("ANDROID_USE_OLD_LOGCAT_VIEW");
	private LogPanel mLogPanel;
	private LogCatPanel mLogCatPanel;
	private ToolItemAction mCreateFilterAction;
	private ToolItemAction mDeleteFilterAction;
	private ToolItemAction mEditFilterAction;
	private ToolItemAction mExportAction;
	private ToolItemAction mClearAction;
	private ToolItemAction[] mLogLevelActions;

	public static boolean useOldLogCatView() {
		return USE_OLD_LOGCAT_VIEW != null;
	}

	private String[] mLogLevelIcons = { "v.png", "d.png", "i.png", "w.png", "e.png" };
	protected Clipboard mClipboard;
	private MenuItem mCopyMenuItem;
	private MenuItem mSelectAllMenuItem;
	private TableFocusListener mTableListener;
	private DeviceExplorer mExplorer = null;
	private Shell mExplorerShell = null;
	private EmulatorControlPanel mEmulatorPanel;
	private EventLogPanel mEventLogPanel;
	private Image mTracingStartImage;
	private Image mTracingStopImage;
	private ImageLoader mDdmUiLibLoader;

	private class TableFocusListener implements ITableFocusListener {
		private ITableFocusListener.IFocusedTableActivator mCurrentActivator;

		private TableFocusListener() {
		}

		public void focusGained(ITableFocusListener.IFocusedTableActivator activator) {
			this.mCurrentActivator = activator;
			if (!UIThread.this.mCopyMenuItem.isDisposed()) {
				UIThread.this.mCopyMenuItem.setEnabled(true);
				UIThread.this.mSelectAllMenuItem.setEnabled(true);
			}
		}

		public void focusLost(ITableFocusListener.IFocusedTableActivator activator) {
			if (activator == this.mCurrentActivator) {
				activator = null;
				if (!UIThread.this.mCopyMenuItem.isDisposed()) {
					UIThread.this.mCopyMenuItem.setEnabled(false);
					UIThread.this.mSelectAllMenuItem.setEnabled(false);
				}
			}
		}

		public void copy(Clipboard clipboard) {
			if (this.mCurrentActivator != null) {
				this.mCurrentActivator.copy(clipboard);
			}
		}

		public void selectAll() {
			if (this.mCurrentActivator != null) {
				this.mCurrentActivator.selectAll();
			}
		}
	}

	private class HProfHandler extends BaseFileHandler implements ClientData.IHprofDumpHandler {
		public HProfHandler(Shell parentShell) {
			super(parentShell);
		}

		public void onEndFailure(final Client client, final String message) {
			UIThread.this.mDisplay.asyncExec(new Runnable() {
				public void run() {
					try {
						UIThread.HProfHandler.this.displayErrorFromUiThread(
								"Unable to create HPROF file for application '%1$s'\n\n%2$sCheck logcat for more information.",
								new Object[] { client.getClientData().getClientDescription(),
										message != null ? message + "\n\n" : "" });
					} finally {
						UIThread.this.enableButtons();
					}
				}
			});
		}

		public void onSuccess(final String remoteFilePath, final Client client) {
			UIThread.this.mDisplay.asyncExec(new Runnable() {
				public void run() {
					IDevice device = client.getDevice();
					try {
						SyncService sync = client.getDevice().getSyncService();
						if (sync != null) {
							UIThread.HProfHandler.this.promptAndPull(sync,
									client.getClientData().getClientDescription() + ".hprof", remoteFilePath,
									"Save HPROF file");
						} else {
							UIThread.HProfHandler.this.displayErrorFromUiThread(
									"Unable to download HPROF file from device '%1$s'.",
									new Object[] { device.getSerialNumber() });
						}
					} catch (SyncException e) {
						if (!e.wasCanceled()) {
							UIThread.HProfHandler.this.displayErrorFromUiThread(
									"Unable to download HPROF file from device '%1$s'.\n\n%2$s",
									new Object[] { device.getSerialNumber(), e.getMessage() });
						}
					} catch (Exception e) {
						UIThread.HProfHandler.this.displayErrorFromUiThread(
								"Unable to download HPROF file from device '%1$s'.",
								new Object[] { device.getSerialNumber() });
					} finally {
						UIThread.this.enableButtons();
					}
				}
			});
		}

		public void onSuccess(final byte[] data, final Client client) {
			UIThread.this.mDisplay.asyncExec(new Runnable() {
				public void run() {
					UIThread.HProfHandler.this.promptAndSave(client.getClientData().getClientDescription() + ".hprof",
							data, "Save HPROF file");
				}
			});
		}

		protected String getDialogTitle() {
			return "HPROF Error";
		}
	}

	private UIThread() {
		mPanels[0] = new InfoPanel();
		mPanels[1] = new ThreadPanel();
		mPanels[2] = new HeapPanel();
		if (PrefsDialog.getStore().getBoolean("native")) {
			if (System.getenv("ANDROID_DDMS_OLD_HEAP_PANEL") != null) {
				mPanels[3] = new com.android.ddmuilib.NativeHeapPanel();
			} else {
				mPanels[3] = new com.android.ddmuilib.heap.NativeHeapPanel(getStore());
			}
		} else {
			mPanels[3] = null;
		}
		mPanels[4] = new AllocationPanel();
		mPanels[5] = new SysinfoPanel();
		mPanels[6] = new NetworkPanel();
	}

	public static UIThread getInstance() {
		return mInstance;
	}

	public Display getDisplay() {
		return this.mDisplay;
	}

	public void asyncExec(Runnable r) {
		if ((this.mDisplay != null) && (!this.mDisplay.isDisposed())) {
			this.mDisplay.asyncExec(r);
		}
	}

	public IPreferenceStore getStore() {
		return PrefsDialog.getStore();
	}

	public void runUI(String ddmsParentLocation) {
		Display.setAppName("DDMS");
		this.mDisplay = Display.getDefault();
		Shell shell = new Shell(this.mDisplay, 1264);

		this.mDdmUiLibLoader = ImageLoader.getDdmUiLibLoader();

		shell.setImage(ImageLoader.getLoader(getClass()).loadImage(this.mDisplay, "ddms-128.png", 100, 50, null));

		Log.setLogOutput(new Log.ILogOutput() {
			public void printAndPromptLog(final Log.LogLevel logLevel, final String tag, final String message) {
				Log.printLog(logLevel, tag, message);

				UIThread.this.mDisplay.asyncExec(new Runnable() {
					public void run() {
						Shell activeShell = UIThread.this.mDisplay.getActiveShell();
						if (logLevel == Log.LogLevel.ERROR) {
							MessageDialog.openError(activeShell, tag, message);
						} else {
							MessageDialog.openWarning(activeShell, tag, message);
						}
					}
				});
			}

			public void printLog(Log.LogLevel logLevel, String tag, String message) {
				Log.printLog(logLevel, tag, message);
			}
		});
		ClientData.setHprofDumpHandler(new HProfHandler(shell));
		ClientData.setMethodProfilingHandler(new MethodProfilingHandler(shell));
		String adbLocation;
		if ((ddmsParentLocation != null) && (ddmsParentLocation.length() != 0)) {
			File platformTools = new File(new File(ddmsParentLocation).getParent(), "platform-tools");
			if (platformTools.isDirectory()) {
				adbLocation = platformTools.getAbsolutePath() + File.separator + SdkConstants.FN_ADB;
			} else {
				String androidOut = System.getenv("ANDROID_HOST_OUT");
				if (androidOut != null) {
					adbLocation = androidOut + File.separator + "bin" + File.separator + SdkConstants.FN_ADB;
				} else {
					adbLocation = SdkConstants.FN_ADB;
				}
			}
		} else {
			adbLocation = SdkConstants.FN_ADB;
		}
		AndroidDebugBridge.init(true);
		AndroidDebugBridge.createBridge(adbLocation, true);

		AndroidDebugBridge.addClientChangeListener(this);

		shell.setText("Dalvik Debug Monitor");
		setConfirmClose(shell);
		createMenus(shell);
		createWidgets(shell);

		shell.pack();
		setSizeAndPosition(shell);
		shell.open();

		Log.d("ddms", "UI is up");
		while (!shell.isDisposed()) {
			if (!this.mDisplay.readAndDispatch()) {
				this.mDisplay.sleep();
			}
		}
		if (useOldLogCatView()) {
			this.mLogPanel.stopLogCat(true);
		}
		this.mDevicePanel.dispose();
		for (TablePanel panel : mPanels) {
			if (panel != null) {
				panel.dispose();
			}
		}
		ImageLoader.dispose();

		this.mDisplay.dispose();
		Log.d("ddms", "UI is down");
	}

	private void setSizeAndPosition(final Shell shell) {
		shell.setMinimumSize(400, 200);

		PreferenceStore prefs = PrefsDialog.getStore();
		int x = prefs.getInt("shellX");
		int y = prefs.getInt("shellY");
		int w = prefs.getInt("shellWidth");
		int h = prefs.getInt("shellHeight");

		Rectangle rect = this.mDisplay.getClientArea();
		if (w > rect.width) {
			w = rect.width;
			prefs.setValue("shellWidth", rect.width);
		}
		if (h > rect.height) {
			h = rect.height;
			prefs.setValue("shellHeight", rect.height);
		}
		if (x < rect.x) {
			x = rect.x;
			prefs.setValue("shellX", rect.x);
		} else if (x >= rect.x + rect.width) {
			x = rect.x + rect.width - w;
			prefs.setValue("shellX", rect.x);
		}
		if (y < rect.y) {
			y = rect.y;
			prefs.setValue("shellY", rect.y);
		} else if (y >= rect.y + rect.height) {
			y = rect.y + rect.height - h;
			prefs.setValue("shellY", rect.y);
		}
		shell.setBounds(x, y, w, h);

		shell.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
				Rectangle controlBounds = shell.getBounds();

				PreferenceStore currentPrefs = PrefsDialog.getStore();
				currentPrefs.setValue("shellX", controlBounds.x);
				currentPrefs.setValue("shellY", controlBounds.y);
			}

			public void controlResized(ControlEvent e) {
				Rectangle controlBounds = shell.getBounds();

				PreferenceStore currentPrefs = PrefsDialog.getStore();
				currentPrefs.setValue("shellWidth", controlBounds.width);
				currentPrefs.setValue("shellHeight", controlBounds.height);
			}
		});
	}

	private void setExplorerSizeAndPosition(final Shell shell) {
		shell.setMinimumSize(400, 200);

		PreferenceStore prefs = PrefsDialog.getStore();
		int x = prefs.getInt("explorerShellX");
		int y = prefs.getInt("explorerShellY");
		int w = prefs.getInt("explorerShellWidth");
		int h = prefs.getInt("explorerShellHeight");

		Rectangle rect = this.mDisplay.getClientArea();
		if (w > rect.width) {
			w = rect.width;
			prefs.setValue("explorerShellWidth", rect.width);
		}
		if (h > rect.height) {
			h = rect.height;
			prefs.setValue("explorerShellHeight", rect.height);
		}
		if (x < rect.x) {
			x = rect.x;
			prefs.setValue("explorerShellX", rect.x);
		} else if (x >= rect.x + rect.width) {
			x = rect.x + rect.width - w;
			prefs.setValue("explorerShellX", rect.x);
		}
		if (y < rect.y) {
			y = rect.y;
			prefs.setValue("explorerShellY", rect.y);
		} else if (y >= rect.y + rect.height) {
			y = rect.y + rect.height - h;
			prefs.setValue("explorerShellY", rect.y);
		}
		shell.setBounds(x, y, w, h);

		shell.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
				Rectangle controlBounds = shell.getBounds();

				PreferenceStore currentPrefs = PrefsDialog.getStore();
				currentPrefs.setValue("explorerShellX", controlBounds.x);
				currentPrefs.setValue("explorerShellY", controlBounds.y);
			}

			public void controlResized(ControlEvent e) {
				Rectangle controlBounds = shell.getBounds();

				PreferenceStore currentPrefs = PrefsDialog.getStore();
				currentPrefs.setValue("explorerShellWidth", controlBounds.width);
				currentPrefs.setValue("explorerShellHeight", controlBounds.height);
			}
		});
	}

	private void setConfirmClose(Shell shell) {
	}

	private void createMenus(final Shell shell) {
		Menu menuBar = new Menu(shell, 2);

		MenuItem fileItem = new MenuItem(menuBar, 64);
		fileItem.setText("&File");
		MenuItem editItem = new MenuItem(menuBar, 64);
		editItem.setText("&Edit");
		MenuItem actionItem = new MenuItem(menuBar, 64);
		actionItem.setText("&Actions");
		MenuItem deviceItem = new MenuItem(menuBar, 64);
		deviceItem.setText("&Device");

		Menu fileMenu = new Menu(menuBar);
		fileItem.setMenu(fileMenu);
		Menu editMenu = new Menu(menuBar);
		editItem.setMenu(editMenu);
		Menu actionMenu = new Menu(menuBar);
		actionItem.setMenu(actionMenu);
		Menu deviceMenu = new Menu(menuBar);
		deviceItem.setMenu(deviceMenu);

		MenuItem item = new MenuItem(fileMenu, 0);
		item.setText("&Static Port Configuration...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StaticPortConfigDialog dlg = new StaticPortConfigDialog(shell);
				dlg.open();
			}
		});
		IMenuBarEnhancer enhancer = MenuBarEnhancer.setupMenu("DDMS", fileMenu, new IMenuBarCallback() {
			public void printError(String format, Object... args) {
				Log.e("DDMS Menu Bar", String.format(format, args));
			}

			public void onPreferencesMenuSelected() {
				PrefsDialog.run(shell);
			}

			public void onAboutMenuSelected() {
				AboutDialog dlg = new AboutDialog(shell);
				dlg.open();
			}
		});
		if (enhancer.getMenuBarMode() == IMenuBarEnhancer.MenuBarMode.GENERIC) {
			new MenuItem(fileMenu, 2);

			item = new MenuItem(fileMenu, 0);
			item.setText("E&xit\tCtrl-Q");
			item.setAccelerator(0x51 | SWT.MOD1);
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					shell.close();
				}
			});
		}
		this.mCopyMenuItem = new MenuItem(editMenu, 0);
		this.mCopyMenuItem.setText("&Copy\tCtrl-C");
		this.mCopyMenuItem.setAccelerator(0x43 | SWT.MOD1);
		this.mCopyMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mTableListener.copy(UIThread.this.mClipboard);
			}
		});
		new MenuItem(editMenu, 2);

		this.mSelectAllMenuItem = new MenuItem(editMenu, 0);
		this.mSelectAllMenuItem.setText("Select &All\tCtrl-A");
		this.mSelectAllMenuItem.setAccelerator(0x41 | SWT.MOD1);
		this.mSelectAllMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mTableListener.selectAll();
			}
		});
		final MenuItem actionHaltItem = new MenuItem(actionMenu, 0);
		actionHaltItem.setText("&Halt VM");
		actionHaltItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mDevicePanel.killSelectedClient();
			}
		});
		final MenuItem actionCauseGcItem = new MenuItem(actionMenu, 0);
		actionCauseGcItem.setText("Cause &GC");
		actionCauseGcItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mDevicePanel.forceGcOnSelectedClient();
			}
		});
		final MenuItem actionResetAdb = new MenuItem(actionMenu, 0);
		actionResetAdb.setText("&Reset adb");
		actionResetAdb.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
				if (bridge != null) {
					bridge.restart();
				}
			}
		});
		actionMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				actionHaltItem
						.setEnabled((UIThread.this.mTBHalt.getEnabled()) && (UIThread.this.mCurrentClient != null));
				actionCauseGcItem
						.setEnabled((UIThread.this.mTBCauseGc.getEnabled()) && (UIThread.this.mCurrentClient != null));
				actionResetAdb.setEnabled(true);
			}
		});
		final MenuItem screenShotItem = new MenuItem(deviceMenu, 0);

		screenShotItem.setText("&Screen capture...\tCtrl-S");
		screenShotItem.setAccelerator(0x53 | SWT.MOD1);

		final MenuItem screenRecordItem = new MenuItem(deviceMenu, 0);
		screenRecordItem.setText("Screen Record");

		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (UIThread.this.mCurrentDevice == null) {
					return;
				}
				if (e.getSource() == screenShotItem) {
					ScreenShotDialog dlg = new ScreenShotDialog(shell);
					dlg.open(UIThread.this.mCurrentDevice);
				} else if (e.getSource() == screenRecordItem) {
					new ScreenRecorderAction(shell, UIThread.this.mCurrentDevice).performAction();
				}
			}
		};
		screenShotItem.addSelectionListener(selectionListener);
		screenRecordItem.addSelectionListener(selectionListener);

		new MenuItem(deviceMenu, 2);

		final MenuItem explorerItem = new MenuItem(deviceMenu, 0);
		explorerItem.setText("File Explorer...");
		explorerItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.createFileExplorer();
			}
		});
		new MenuItem(deviceMenu, 2);

		final MenuItem processItem = new MenuItem(deviceMenu, 0);
		processItem.setText("Show &process status...");
		processItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DeviceCommandDialog dlg = new DeviceCommandDialog("ps -x", "ps-x.txt", shell);
				dlg.open(UIThread.this.mCurrentDevice);
			}
		});
		final MenuItem deviceStateItem = new MenuItem(deviceMenu, 0);
		deviceStateItem.setText("Dump &device state...");
		deviceStateItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DeviceCommandDialog dlg = new DeviceCommandDialog("/system/bin/dumpstate /proc/self/fd/0",
						"device-state.txt", shell);

				dlg.open(UIThread.this.mCurrentDevice);
			}
		});
		final MenuItem appStateItem = new MenuItem(deviceMenu, 0);
		appStateItem.setText("Dump &app state...");
		appStateItem.setEnabled(false);
		appStateItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DeviceCommandDialog dlg = new DeviceCommandDialog("dumpsys", "app-state.txt", shell);
				dlg.open(UIThread.this.mCurrentDevice);
			}
		});
		final MenuItem radioStateItem = new MenuItem(deviceMenu, 0);
		radioStateItem.setText("Dump &radio state...");
		radioStateItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DeviceCommandDialog dlg = new DeviceCommandDialog(
						"cat /data/logs/radio.4 /data/logs/radio.3 /data/logs/radio.2 /data/logs/radio.1 /data/logs/radio",
						"radio-state.txt", shell);

				dlg.open(UIThread.this.mCurrentDevice);
			}
		});
		final MenuItem logCatItem = new MenuItem(deviceMenu, 0);
		logCatItem.setText("Run &logcat...");
		logCatItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DeviceCommandDialog dlg = new DeviceCommandDialog("logcat '*:d jdwp:w'", "log.txt", shell);

				dlg.open(UIThread.this.mCurrentDevice);
			}
		});
		deviceMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				boolean deviceEnabled = UIThread.this.mCurrentDevice != null;
				screenShotItem.setEnabled(deviceEnabled);
				explorerItem.setEnabled(deviceEnabled);
				processItem.setEnabled(deviceEnabled);
				deviceStateItem.setEnabled(deviceEnabled);
				appStateItem.setEnabled(deviceEnabled);
				radioStateItem.setEnabled(deviceEnabled);
				logCatItem.setEnabled(deviceEnabled);
				screenRecordItem.setEnabled((UIThread.this.mCurrentDevice != null)
						&& (UIThread.this.mCurrentDevice.supportsFeature(IDevice.Feature.SCREEN_RECORD)));
			}
		});
		shell.setMenuBar(menuBar);
	}

	private void createWidgets(Shell shell) {
		Color darkGray = shell.getDisplay().getSystemColor(16);

		shell.setLayout(new GridLayout(1, false));

		final Composite panelArea = new Composite(shell, 2048);

		panelArea.setLayoutData(new GridData(1808));

		this.mStatusLine = new Label(shell, 0);

		this.mStatusLine.setLayoutData(new GridData(768));

		this.mStatusLine.setText("Initializing...");

		final PreferenceStore prefs = PrefsDialog.getStore();

		Composite topPanel = new Composite(panelArea, 0);
		final Sash sash = new Sash(panelArea, 256);
		sash.setBackground(darkGray);
		Composite bottomPanel = new Composite(panelArea, 0);

		panelArea.setLayout(new FormLayout());

		createTopPanel(topPanel, darkGray);

		this.mClipboard = new Clipboard(panelArea.getDisplay());
		if (useOldLogCatView()) {
			createBottomPanel(bottomPanel);
		} else {
			createLogCatView(bottomPanel);
		}
		FormData data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.bottom = new FormAttachment(sash, 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		topPanel.setLayoutData(data);

		final FormData sashData = new FormData();
		if ((prefs != null) && (prefs.contains("logSashLocation"))) {
			sashData.top = new FormAttachment(0, prefs.getInt("logSashLocation"));
		} else {
			sashData.top = new FormAttachment(50, 0);
		}
		sashData.left = new FormAttachment(0, 0);
		sashData.right = new FormAttachment(100, 0);
		sash.setLayoutData(sashData);

		data = new FormData();
		data.top = new FormAttachment(sash, 0);
		data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		bottomPanel.setLayoutData(data);

		sash.addListener(13, new Listener() {
			public void handleEvent(Event e) {
				Rectangle sashRect = sash.getBounds();
				Rectangle panelRect = panelArea.getClientArea();
				int bottom = panelRect.height - sashRect.height - 100;
				e.y = Math.max(Math.min(e.y, bottom), 100);
				if (e.y != sashRect.y) {
					sashData.top = new FormAttachment(0, e.y);
					if (prefs != null) {
						prefs.setValue("logSashLocation", e.y);
					}
					panelArea.layout();
				}
			}
		});
		this.mTableListener = new TableFocusListener();
		if (useOldLogCatView()) {
			this.mLogPanel.setTableFocusListener(this.mTableListener);
		} else {
			this.mLogCatPanel.setTableFocusListener(this.mTableListener);
		}
		this.mEventLogPanel.setTableFocusListener(this.mTableListener);
		for (TablePanel p : mPanels) {
			if (p != null) {
				p.setTableFocusListener(this.mTableListener);
			}
		}
		this.mStatusLine.setText("");
	}

	private void createDevicePanelToolBar(ToolBar toolBar) {
		Display display = toolBar.getDisplay();

		this.mTBShowHeapUpdates = new ToolItem(toolBar, 32);
		this.mTBShowHeapUpdates.setImage(this.mDdmUiLibLoader.loadImage(display, "heap.png", 16, 16, null));

		this.mTBShowHeapUpdates.setToolTipText("Show heap updates");
		this.mTBShowHeapUpdates.setEnabled(false);
		this.mTBShowHeapUpdates.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (UIThread.this.mCurrentClient != null) {
					boolean enable = !UIThread.this.mCurrentClient.isHeapUpdateEnabled();
					UIThread.this.mCurrentClient.setHeapUpdateEnabled(enable);
				} else {
					e.doit = false;
				}
			}
		});
		this.mTBDumpHprof = new ToolItem(toolBar, 8);
		this.mTBDumpHprof.setToolTipText("Dump HPROF file");
		this.mTBDumpHprof.setEnabled(false);
		this.mTBDumpHprof.setImage(this.mDdmUiLibLoader.loadImage(display, "hprof.png", 16, 16, null));

		this.mTBDumpHprof.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mDevicePanel.dumpHprof();

				UIThread.this.enableButtons();
			}
		});
		this.mTBCauseGc = new ToolItem(toolBar, 8);
		this.mTBCauseGc.setToolTipText("Cause an immediate GC");
		this.mTBCauseGc.setEnabled(false);
		this.mTBCauseGc.setImage(this.mDdmUiLibLoader.loadImage(display, "gc.png", 16, 16, null));

		this.mTBCauseGc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mDevicePanel.forceGcOnSelectedClient();
			}
		});
		new ToolItem(toolBar, 2);

		this.mTBShowThreadUpdates = new ToolItem(toolBar, 32);
		this.mTBShowThreadUpdates.setImage(this.mDdmUiLibLoader.loadImage(display, "thread.png", 16, 16, null));

		this.mTBShowThreadUpdates.setToolTipText("Show thread updates");
		this.mTBShowThreadUpdates.setEnabled(false);
		this.mTBShowThreadUpdates.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (UIThread.this.mCurrentClient != null) {
					boolean enable = !UIThread.this.mCurrentClient.isThreadUpdateEnabled();

					UIThread.this.mCurrentClient.setThreadUpdateEnabled(enable);
				} else {
					e.doit = false;
				}
			}
		});
		this.mTracingStartImage = this.mDdmUiLibLoader.loadImage(display, "tracing_start.png", 16, 16, null);

		this.mTracingStopImage = this.mDdmUiLibLoader.loadImage(display, "tracing_stop.png", 16, 16, null);

		this.mTBProfiling = new ToolItem(toolBar, 8);
		this.mTBProfiling.setToolTipText("Start Method Profiling");
		this.mTBProfiling.setEnabled(false);
		this.mTBProfiling.setImage(this.mTracingStartImage);
		this.mTBProfiling.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mDevicePanel.toggleMethodProfiling();
			}
		});
		new ToolItem(toolBar, 2);

		this.mTBHalt = new ToolItem(toolBar, 8);
		this.mTBHalt.setToolTipText("Halt the target VM");
		this.mTBHalt.setEnabled(false);
		this.mTBHalt.setImage(this.mDdmUiLibLoader.loadImage(display, "halt.png", 16, 16, null));

		this.mTBHalt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mDevicePanel.killSelectedClient();
			}
		});
		toolBar.pack();
	}

	private void createTopPanel(final Composite comp, Color darkGray) {
		final PreferenceStore prefs = PrefsDialog.getStore();

		comp.setLayout(new FormLayout());

		Composite leftPanel = new Composite(comp, 0);
		final Sash sash = new Sash(comp, 512);
		sash.setBackground(darkGray);
		Composite rightPanel = new Composite(comp, 0);

		createLeftPanel(leftPanel);
		createRightPanel(rightPanel);

		FormData data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(sash, 0);
		leftPanel.setLayoutData(data);

		final FormData sashData = new FormData();
		sashData.top = new FormAttachment(0, 0);
		sashData.bottom = new FormAttachment(100, 0);
		if ((prefs != null) && (prefs.contains("sashLocation"))) {
			sashData.left = new FormAttachment(0, prefs.getInt("sashLocation"));
		} else {
			sashData.left = new FormAttachment(0, 380);
		}
		sash.setLayoutData(sashData);

		data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(sash, 0);
		data.right = new FormAttachment(100, 0);
		rightPanel.setLayoutData(data);

		int minPanelWidth = 60;

		sash.addListener(13, new Listener() {
			public void handleEvent(Event e) {
				Rectangle sashRect = sash.getBounds();
				Rectangle panelRect = comp.getClientArea();
				int right = panelRect.width - sashRect.width - 60;
				e.x = Math.max(Math.min(e.x, right), 60);
				if (e.x != sashRect.x) {
					sashData.left = new FormAttachment(0, e.x);
					if (prefs != null) {
						prefs.setValue("sashLocation", e.x);
					}
					comp.layout();
				}
			}
		});
	}

	private void createBottomPanel(Composite comp) {
		PreferenceStore prefs = PrefsDialog.getStore();

		Display display = comp.getDisplay();

		LogColors colors = new LogColors();

		colors.infoColor = new Color(display, 0, 127, 0);
		colors.debugColor = new Color(display, 0, 0, 127);
		colors.errorColor = new Color(display, 255, 0, 0);
		colors.warningColor = new Color(display, 255, 127, 0);
		colors.verboseColor = new Color(display, 0, 0, 0);

		LogPanel.PREFS_TIME = "logcat.time";
		LogPanel.PREFS_LEVEL = "logcat.level";
		LogPanel.PREFS_PID = "logcat.pid";
		LogPanel.PREFS_TAG = "logcat.tag";
		LogPanel.PREFS_MESSAGE = "logcat.message";

		comp.setLayout(new GridLayout(1, false));

		ToolBar toolBar = new ToolBar(comp, 256);

		this.mCreateFilterAction = new ToolItemAction(toolBar, 8);
		this.mCreateFilterAction.item.setToolTipText("Create Filter");
		this.mCreateFilterAction.item.setImage(this.mDdmUiLibLoader.loadImage(this.mDisplay, "add.png", 16, 16, null));

		this.mCreateFilterAction.item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mLogPanel.addFilter();
			}
		});
		this.mEditFilterAction = new ToolItemAction(toolBar, 8);
		this.mEditFilterAction.item.setToolTipText("Edit Filter");
		this.mEditFilterAction.item.setImage(this.mDdmUiLibLoader.loadImage(this.mDisplay, "edit.png", 16, 16, null));

		this.mEditFilterAction.item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mLogPanel.editFilter();
			}
		});
		this.mDeleteFilterAction = new ToolItemAction(toolBar, 8);
		this.mDeleteFilterAction.item.setToolTipText("Delete Filter");
		this.mDeleteFilterAction.item
				.setImage(this.mDdmUiLibLoader.loadImage(this.mDisplay, "delete.png", 16, 16, null));

		this.mDeleteFilterAction.item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mLogPanel.deleteFilter();
			}
		});
		new ToolItem(toolBar, 2);

		Log.LogLevel[] levels = Log.LogLevel.values();
		this.mLogLevelActions = new ToolItemAction[this.mLogLevelIcons.length];
		for (int i = 0; i < this.mLogLevelActions.length; i++) {
			String name = levels[i].getStringValue();
			final ToolItemAction newAction = new ToolItemAction(toolBar, 32);
			this.mLogLevelActions[i] = newAction;

			newAction.item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					for (int k = 0; k < UIThread.this.mLogLevelActions.length; k++) {
						ToolItemAction a = UIThread.this.mLogLevelActions[k];
						if (a == newAction) {
							a.setChecked(true);

							UIThread.this.mLogPanel.setCurrentFilterLogLevel(k + 2);
						} else {
							a.setChecked(false);
						}
					}
				}
			});
			newAction.item.setToolTipText(name);
			newAction.item
					.setImage(this.mDdmUiLibLoader.loadImage(this.mDisplay, this.mLogLevelIcons[i], 16, 16, null));
		}
		new ToolItem(toolBar, 2);

		this.mClearAction = new ToolItemAction(toolBar, 8);
		this.mClearAction.item.setToolTipText("Clear Log");

		this.mClearAction.item.setImage(this.mDdmUiLibLoader.loadImage(this.mDisplay, "clear.png", 16, 16, null));

		this.mClearAction.item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mLogPanel.clear();
			}
		});
		new ToolItem(toolBar, 2);

		this.mExportAction = new ToolItemAction(toolBar, 8);
		this.mExportAction.item.setToolTipText("Export Selection As Text...");
		this.mExportAction.item.setImage(this.mDdmUiLibLoader.loadImage(this.mDisplay, "save.png", 16, 16, null));

		this.mExportAction.item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				UIThread.this.mLogPanel.save();
			}
		});
		toolBar.pack();

		this.mLogPanel = new LogPanel(colors, new FilterStorage(), 1);

		this.mLogPanel.setActions(this.mDeleteFilterAction, this.mEditFilterAction, this.mLogLevelActions);

		String colMode = prefs.getString("ddmsLogColumnMode");
		if ("auto".equals(colMode)) {
			this.mLogPanel.setColumnMode(1);
		}
		String fontStr = PrefsDialog.getStore().getString("ddmsLogFont");
		if (fontStr != null) {
			try {
				FontData fdat = new FontData(fontStr);
				this.mLogPanel.setFont(new Font(display, fdat));
			} catch (IllegalArgumentException e) {
			} catch (SWTError e2) {
			}
		}
		this.mLogPanel.createPanel(comp);

		this.mLogPanel.startLogCat(this.mCurrentDevice);
	}

	private void createLogCatView(Composite parent) {
		IPreferenceStore prefStore = DdmUiPreferences.getStore();
		this.mLogCatPanel = new LogCatPanel(prefStore);
		this.mLogCatPanel.createPanel(parent);
		if (this.mCurrentDevice != null) {
			this.mLogCatPanel.deviceSelected(this.mCurrentDevice);
		}
	}

	private void createLeftPanel(Composite comp) {
		comp.setLayout(new GridLayout(1, false));
		ToolBar toolBar = new ToolBar(comp, 131392);
		toolBar.setLayoutData(new GridData(768));
		createDevicePanelToolBar(toolBar);

		Composite c = new Composite(comp, 0);
		c.setLayoutData(new GridData(1808));

		this.mDevicePanel = new DevicePanel(true);
		this.mDevicePanel.createPanel(c);

		this.mDevicePanel.addSelectionListener(this);
	}

	private void createRightPanel(Composite comp) {
		comp.setLayout(new FillLayout());

		TabFolder tabFolder = new TabFolder(comp, 0);
		for (int i = 0; i < mPanels.length; i++) {
			if (mPanels[i] != null) {
				TabItem item = new TabItem(tabFolder, 0);
				item.setText(mPanelNames[i]);
				item.setToolTipText(mPanelTips[i]);
				item.setControl(mPanels[i].createPanel(tabFolder));
			}
		}
		TabItem item = new TabItem(tabFolder, 0);
		item.setText("Emulator Control");
		item.setToolTipText("Emulator Control Panel");
		this.mEmulatorPanel = new EmulatorControlPanel();
		item.setControl(this.mEmulatorPanel.createPanel(tabFolder));

		item = new TabItem(tabFolder, 0);
		item.setText("Event Log");
		item.setToolTipText("Event Log");

		Composite eventLogTopComposite = new Composite(tabFolder, 0);
		item.setControl(eventLogTopComposite);
		eventLogTopComposite.setLayout(new GridLayout(1, false));

		ToolBar toolbar = new ToolBar(eventLogTopComposite, 256);
		toolbar.setLayoutData(new GridData(768));

		ToolItemAction optionsAction = new ToolItemAction(toolbar, 8);
		optionsAction.item.setToolTipText("Opens the options panel");
		optionsAction.item.setImage(this.mDdmUiLibLoader.loadImage(comp.getDisplay(), "edit.png", 16, 16, null));

		ToolItemAction clearAction = new ToolItemAction(toolbar, 8);
		clearAction.item.setToolTipText("Clears the event log");
		clearAction.item.setImage(this.mDdmUiLibLoader.loadImage(comp.getDisplay(), "clear.png", 16, 16, null));

		new ToolItem(toolbar, 2);

		ToolItemAction saveAction = new ToolItemAction(toolbar, 8);
		saveAction.item.setToolTipText("Saves the event log");
		saveAction.item.setImage(this.mDdmUiLibLoader.loadImage(comp.getDisplay(), "save.png", 16, 16, null));

		ToolItemAction loadAction = new ToolItemAction(toolbar, 8);
		loadAction.item.setToolTipText("Loads an event log");
		loadAction.item.setImage(this.mDdmUiLibLoader.loadImage(comp.getDisplay(), "load.png", 16, 16, null));

		ToolItemAction importBugAction = new ToolItemAction(toolbar, 8);
		importBugAction.item.setToolTipText("Imports a bug report");
		importBugAction.item.setImage(this.mDdmUiLibLoader.loadImage(comp.getDisplay(), "importBug.png", 16, 16, null));

		this.mEventLogPanel = new EventLogPanel();

		this.mEventLogPanel.setActions(optionsAction, clearAction, saveAction, loadAction, importBugAction);

		this.mEventLogPanel.createPanel(eventLogTopComposite);
	}

	private void createFileExplorer() {
		if (this.mExplorer == null) {
			this.mExplorerShell = new Shell(this.mDisplay);

			this.mExplorerShell.setLayout(new GridLayout(1, false));

			ToolBar toolBar = new ToolBar(this.mExplorerShell, 256);
			toolBar.setLayoutData(new GridData(768));

			ToolItemAction pullAction = new ToolItemAction(toolBar, 8);
			pullAction.item.setToolTipText("Pull File from Device");
			Image image = this.mDdmUiLibLoader.loadImage("pull.png", this.mDisplay);
			if (image != null) {
				pullAction.item.setImage(image);
			} else {
				pullAction.item.setText("Pull");
			}
			ToolItemAction pushAction = new ToolItemAction(toolBar, 8);
			pushAction.item.setToolTipText("Push file onto Device");
			image = this.mDdmUiLibLoader.loadImage("push.png", this.mDisplay);
			if (image != null) {
				pushAction.item.setImage(image);
			} else {
				pushAction.item.setText("Push");
			}
			ToolItemAction deleteAction = new ToolItemAction(toolBar, 8);
			deleteAction.item.setToolTipText("Delete");
			image = this.mDdmUiLibLoader.loadImage("delete.png", this.mDisplay);
			if (image != null) {
				deleteAction.item.setImage(image);
			} else {
				deleteAction.item.setText("Delete");
			}
			ToolItemAction createNewFolderAction = new ToolItemAction(toolBar, 8);
			createNewFolderAction.item.setToolTipText("New Folder");
			image = this.mDdmUiLibLoader.loadImage("add.png", this.mDisplay);
			if (image != null) {
				createNewFolderAction.item.setImage(image);
			} else {
				createNewFolderAction.item.setText("New Folder");
			}
			this.mExplorer = new DeviceExplorer();
			this.mExplorer.setActions(pushAction, pullAction, deleteAction, createNewFolderAction);

			pullAction.item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					UIThread.this.mExplorer.pullSelection();
				}
			});
			pullAction.setEnabled(false);

			pushAction.item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					UIThread.this.mExplorer.pushIntoSelection();
				}
			});
			pushAction.setEnabled(false);

			deleteAction.item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					UIThread.this.mExplorer.deleteSelection();
				}
			});
			deleteAction.setEnabled(false);

			createNewFolderAction.item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					UIThread.this.mExplorer.createNewFolderInSelection();
				}
			});
			createNewFolderAction.setEnabled(false);

			Composite parent = new Composite(this.mExplorerShell, 0);
			parent.setLayoutData(new GridData(1808));

			this.mExplorer.createPanel(parent);
			this.mExplorer.switchDevice(this.mCurrentDevice);

			this.mExplorerShell.addShellListener(new ShellListener() {
				public void shellActivated(ShellEvent e) {
				}

				public void shellClosed(ShellEvent e) {
					UIThread.this.mExplorer = null;
					UIThread.this.mExplorerShell = null;
				}

				public void shellDeactivated(ShellEvent e) {
				}

				public void shellDeiconified(ShellEvent e) {
				}

				public void shellIconified(ShellEvent e) {
				}
			});
			this.mExplorerShell.pack();
			setExplorerSizeAndPosition(this.mExplorerShell);
			this.mExplorerShell.open();
		} else if (this.mExplorerShell != null) {
			this.mExplorerShell.forceActive();
		}
	}

	public void setStatusLine(final String str) {
		try {
			this.mDisplay.asyncExec(new Runnable() {
				public void run() {
					UIThread.this.doSetStatusLine(str);
				}
			});
		} catch (SWTException swte) {
			if (!this.mDisplay.isDisposed()) {
				throw swte;
			}
		}
	}

	private void doSetStatusLine(String str) {
		if (this.mStatusLine.isDisposed()) {
			return;
		}
		if (!this.mStatusLine.getText().equals(str)) {
			this.mStatusLine.setText(str);
		}
	}

	public void displayError(final String msg) {
		try {
			this.mDisplay.syncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(UIThread.this.mDisplay.getActiveShell(), "Error", msg);
				}
			});
		} catch (SWTException swte) {
			if (!this.mDisplay.isDisposed()) {
				throw swte;
			}
		}
	}

	private void enableButtons() {
		if (this.mCurrentClient != null) {
			this.mTBShowThreadUpdates.setSelection(this.mCurrentClient.isThreadUpdateEnabled());
			this.mTBShowThreadUpdates.setEnabled(true);
			this.mTBShowHeapUpdates.setSelection(this.mCurrentClient.isHeapUpdateEnabled());
			this.mTBShowHeapUpdates.setEnabled(true);
			this.mTBHalt.setEnabled(true);
			this.mTBCauseGc.setEnabled(true);

			ClientData data = this.mCurrentClient.getClientData();
			if (data.hasFeature("hprof-heap-dump")) {
				this.mTBDumpHprof.setEnabled(!data.hasPendingHprofDump());
				this.mTBDumpHprof.setToolTipText("Dump HPROF file");
			} else {
				this.mTBDumpHprof.setEnabled(false);
				this.mTBDumpHprof.setToolTipText("Dump HPROF file (not supported by this VM)");
			}
			if (data.hasFeature("method-trace-profiling")) {
				this.mTBProfiling.setEnabled(true);
				if ((data.getMethodProfilingStatus() == ClientData.MethodProfilingStatus.TRACER_ON)
						|| (data.getMethodProfilingStatus() == ClientData.MethodProfilingStatus.SAMPLER_ON)) {
					this.mTBProfiling.setToolTipText("Stop Method Profiling");
					this.mTBProfiling.setImage(this.mTracingStopImage);
				} else {
					this.mTBProfiling.setToolTipText("Start Method Profiling");
					this.mTBProfiling.setImage(this.mTracingStartImage);
				}
			} else {
				this.mTBProfiling.setEnabled(false);
				this.mTBProfiling.setImage(this.mTracingStartImage);
				this.mTBProfiling.setToolTipText("Method Profiling (not supported by this VM)");
			}
		} else {
			this.mTBShowThreadUpdates.setSelection(false);
			this.mTBShowThreadUpdates.setEnabled(false);
			this.mTBShowHeapUpdates.setSelection(false);
			this.mTBShowHeapUpdates.setEnabled(false);
			this.mTBHalt.setEnabled(false);
			this.mTBCauseGc.setEnabled(false);

			this.mTBDumpHprof.setEnabled(false);
			this.mTBDumpHprof.setToolTipText("Dump HPROF file");

			this.mTBProfiling.setEnabled(false);
			this.mTBProfiling.setImage(this.mTracingStartImage);
			this.mTBProfiling.setToolTipText("Start Method Profiling");
		}
	}

	public void selectionChanged(IDevice selectedDevice, Client selectedClient) {
		if (this.mCurrentDevice != selectedDevice) {
			this.mCurrentDevice = selectedDevice;
			for (TablePanel panel : mPanels) {
				if (panel != null) {
					panel.deviceSelected(this.mCurrentDevice);
				}
			}
			this.mEmulatorPanel.deviceSelected(this.mCurrentDevice);
			if (useOldLogCatView()) {
				this.mLogPanel.deviceSelected(this.mCurrentDevice);
			} else {
				this.mLogCatPanel.deviceSelected(this.mCurrentDevice);
			}
			if (this.mEventLogPanel != null) {
				this.mEventLogPanel.deviceSelected(this.mCurrentDevice);
			}
			if (this.mExplorer != null) {
				this.mExplorer.switchDevice(this.mCurrentDevice);
			}
		}
		if (this.mCurrentClient != selectedClient) {
			AndroidDebugBridge.getBridge().setSelectedClient(selectedClient);
			this.mCurrentClient = selectedClient;
			for (TablePanel panel : mPanels) {
				if (panel != null) {
					panel.clientSelected(this.mCurrentClient);
				}
			}
			enableButtons();
		}
	}

	public void clientChanged(Client client, int changeMask) {
		if ((changeMask & 0x800) == 2048) {
			if (this.mCurrentClient == client) {
				this.mDisplay.asyncExec(new Runnable() {
					public void run() {
						UIThread.this.enableButtons();
					}
				});
			}
		}
	}
}

/* Location:           D:\android-sdk-windows\tools\lib\ddms.jar

 * Qualified Name:     com.android.ddms.UIThread

 * JD-Core Version:    0.7.0.1

 */
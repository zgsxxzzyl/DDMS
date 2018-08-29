package com.android.ddms;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import com.android.ddmlib.DdmConstants;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.Log;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.sdkstats.DdmsPreferenceStore;
import com.android.sdkstats.SdkStatsPermissionDialog;

public final class PrefsDialog {
	public static final String SHELL_X = "shellX";
	public static final String SHELL_Y = "shellY";
	public static final String SHELL_WIDTH = "shellWidth";
	public static final String SHELL_HEIGHT = "shellHeight";
	public static final String EXPLORER_SHELL_X = "explorerShellX";
	public static final String EXPLORER_SHELL_Y = "explorerShellY";
	public static final String EXPLORER_SHELL_WIDTH = "explorerShellWidth";
	public static final String EXPLORER_SHELL_HEIGHT = "explorerShellHeight";
	public static final String SHOW_NATIVE_HEAP = "native";
	public static final String LOGCAT_COLUMN_MODE = "ddmsLogColumnMode";
	public static final String LOGCAT_FONT = "ddmsLogFont";
	public static final String LOGCAT_COLUMN_MODE_AUTO = "auto";
	public static final String LOGCAT_COLUMN_MODE_MANUAL = "manual";
	private static final String PREFS_DEBUG_PORT_BASE = "adbDebugBasePort";
	private static final String PREFS_SELECTED_DEBUG_PORT = "debugSelectedPort";
	private static final String PREFS_DEFAULT_THREAD_UPDATE = "defaultThreadUpdateEnabled";
	private static final String PREFS_DEFAULT_HEAP_UPDATE = "defaultHeapUpdateEnabled";
	private static final String PREFS_THREAD_REFRESH_INTERVAL = "threadStatusInterval";
	private static final String PREFS_LOG_LEVEL = "ddmsLogLevel";
	private static final String PREFS_TIMEOUT = "timeOut";
	private static final String PREFS_PROFILER_BUFFER_SIZE_MB = "profilerBufferSizeMb";
	private static final String PREFS_USE_ADBHOST = "useAdbHost";
	private static final String PREFS_ADBHOST_VALUE = "adbHostValue";
	private static DdmsPreferenceStore mStore = new DdmsPreferenceStore();

	@Deprecated
	public static PreferenceStore getStore() {
		return mStore.getPreferenceStore();
	}

	@Deprecated
	public static void save() {
		try {
			mStore.getPreferenceStore().save();
		} catch (IOException ioe) {
			Log.w("ddms", "Failed saving prefs file: " + ioe.getMessage());
		}
	}

	public static void init() {
		PreferenceStore prefStore = mStore.getPreferenceStore();
		if (prefStore == null) {
			Log.e("ddms", "failed to access both the user HOME directory and the system wide temp folder. Quitting.");

			System.exit(1);
		}
		setDefaults(System.getProperty("user.home"));

		prefStore.addPropertyChangeListener(new ChangeListener());

		DdmPreferences.setDebugPortBase(prefStore.getInt("adbDebugBasePort"));
		DdmPreferences.setSelectedDebugPort(prefStore.getInt("debugSelectedPort"));
		DdmPreferences.setLogLevel(prefStore.getString("ddmsLogLevel"));
		DdmPreferences.setInitialThreadUpdate(prefStore.getBoolean("defaultThreadUpdateEnabled"));
		DdmPreferences.setInitialHeapUpdate(prefStore.getBoolean("defaultHeapUpdateEnabled"));
		DdmPreferences.setTimeOut(prefStore.getInt("timeOut"));
		DdmPreferences.setProfilerBufferSizeMb(prefStore.getInt("profilerBufferSizeMb"));
		DdmPreferences.setUseAdbHost(prefStore.getBoolean("useAdbHost"));
		DdmPreferences.setAdbHostValue(prefStore.getString("adbHostValue"));

		String out = System.getenv("ANDROID_PRODUCT_OUT");
		DdmUiPreferences.setSymbolsLocation(out + File.separator + "symbols");
		DdmUiPreferences.setAddr2LineLocation("arm-linux-androideabi-addr2line");
		DdmUiPreferences.setAddr2LineLocation64("aarch64-linux-android-addr2line");
		String traceview = System.getProperty("com.android.ddms.bindir");
		if ((traceview != null) && (traceview.length() != 0)) {
			traceview = traceview + File.separator + DdmConstants.FN_TRACEVIEW;
		} else {
			traceview = DdmConstants.FN_TRACEVIEW;
		}
		DdmUiPreferences.setTraceviewLocation(traceview);

		DdmUiPreferences.setStore(prefStore);
		DdmUiPreferences.setThreadRefreshInterval(prefStore.getInt("threadStatusInterval"));
	}

	private static void setDefaults(String homeDir) {
		PreferenceStore prefStore = mStore.getPreferenceStore();

		prefStore.setDefault("adbDebugBasePort", 8600);

		prefStore.setDefault("debugSelectedPort", 8700);

		prefStore.setDefault("useAdbHost", false);
		prefStore.setDefault("adbHostValue", "127.0.0.1");

		prefStore.setDefault("defaultThreadUpdateEnabled", true);
		prefStore.setDefault("defaultHeapUpdateEnabled", false);
		prefStore.setDefault("threadStatusInterval", 4);

		prefStore.setDefault("textSaveDir", homeDir);
		prefStore.setDefault("imageSaveDir", homeDir);

		prefStore.setDefault("ddmsLogLevel", "info");

		prefStore.setDefault("timeOut", 5000);
		prefStore.setDefault("profilerBufferSizeMb", 8);

		FontData fdat = new FontData("Courier", 10, 0);
		prefStore.setDefault("textOutputFont", fdat.toString());

		prefStore.setDefault("shellX", 100);
		prefStore.setDefault("shellY", 100);
		prefStore.setDefault("shellWidth", 800);
		prefStore.setDefault("shellHeight", 600);

		prefStore.setDefault("explorerShellX", 50);
		prefStore.setDefault("explorerShellY", 50);

		prefStore.setDefault("native", false);
	}

	private static class ChangeListener implements IPropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			String changed = event.getProperty();
			PreferenceStore prefStore = PrefsDialog.mStore.getPreferenceStore();
			if (changed.equals("adbDebugBasePort")) {
				DdmPreferences.setDebugPortBase(prefStore.getInt("adbDebugBasePort"));
			} else if (changed.equals("debugSelectedPort")) {
				DdmPreferences.setSelectedDebugPort(prefStore.getInt("debugSelectedPort"));
			} else if (changed.equals("ddmsLogLevel")) {
				DdmPreferences.setLogLevel((String) event.getNewValue());
			} else if (changed.equals("textSaveDir")) {
				prefStore.setValue("lastTextSaveDir", (String) event.getNewValue());
			} else if (changed.equals("imageSaveDir")) {
				prefStore.setValue("lastImageSaveDir", (String) event.getNewValue());
			} else if (changed.equals("timeOut")) {
				DdmPreferences.setTimeOut(prefStore.getInt("timeOut"));
			} else if (changed.equals("profilerBufferSizeMb")) {
				DdmPreferences.setProfilerBufferSizeMb(prefStore.getInt("profilerBufferSizeMb"));
			} else if (changed.equals("useAdbHost")) {
				DdmPreferences.setUseAdbHost(prefStore.getBoolean("useAdbHost"));
			} else if (changed.equals("adbHostValue")) {
				DdmPreferences.setAdbHostValue(prefStore.getString("adbHostValue"));
			} else {
				Log.v("ddms", "Preference change: " + event.getProperty() + ": '" + event.getOldValue() + "' --> '"
						+ event.getNewValue() + "'");
			}
		}
	}

	public static void run(Shell shell) {
		PreferenceStore prefStore = mStore.getPreferenceStore();
		assert (prefStore != null);

		PreferenceManager prefMgr = new PreferenceManager();

		PreferenceNode node = new PreferenceNode("debugger", new DebuggerPrefs());
		prefMgr.addToRoot(node);

		PreferenceNode subNode = new PreferenceNode("panel", new PanelPrefs());

		prefMgr.addToRoot(subNode);

		node = new PreferenceNode("LogCat", new LogCatPrefs());
		prefMgr.addToRoot(node);

		node = new PreferenceNode("misc", new MiscPrefs());
		prefMgr.addToRoot(node);

		node = new PreferenceNode("stats", new UsageStatsPrefs());
		prefMgr.addToRoot(node);

		PreferenceDialog dlg = new PreferenceDialog(shell, prefMgr);
		dlg.setPreferenceStore(prefStore);
		try {
			dlg.open();
		} catch (Throwable t) {
			Log.e("ddms", t);
		}
		try {
			prefStore.save();
		} catch (IOException ioe) {
		}
	}

	private static class DebuggerPrefs extends FieldEditorPreferencePage {
		private BooleanFieldEditor mUseAdbHost;
		private StringFieldEditor mAdbHostValue;

		public DebuggerPrefs() {
			super();
			setTitle("Debugger");
		}

		protected void createFieldEditors() {
			IntegerFieldEditor ife = new IntegerFieldEditor("adbDebugBasePort", "Starting value for local port:",
					getFieldEditorParent());

			ife.setValidRange(1024, 32767);
			addField(ife);

			ife = new IntegerFieldEditor("debugSelectedPort", "Port of Selected VM:", getFieldEditorParent());

			ife.setValidRange(1024, 32767);
			addField(ife);

			this.mUseAdbHost = new BooleanFieldEditor("useAdbHost", "Use ADBHOST", getFieldEditorParent());

			addField(this.mUseAdbHost);

			this.mAdbHostValue = new StringFieldEditor("adbHostValue", "ADBHOST value:", getFieldEditorParent());

			this.mAdbHostValue.setEnabled(getPreferenceStore().getBoolean("useAdbHost"), getFieldEditorParent());

			addField(this.mAdbHostValue);
		}

		public void propertyChange(PropertyChangeEvent event) {
			if (event.getSource().equals(this.mUseAdbHost)) {
				this.mAdbHostValue.setEnabled(this.mUseAdbHost.getBooleanValue(), getFieldEditorParent());
			}
			super.propertyChange(event);
		}
	}

	private static class PanelPrefs extends FieldEditorPreferencePage {
		public PanelPrefs() {
			super();
			setTitle("Info Panels");
		}

		protected void createFieldEditors() {
			BooleanFieldEditor bfe = new BooleanFieldEditor("defaultThreadUpdateEnabled",
					"Thread updates enabled by default", getFieldEditorParent());

			addField(bfe);

			bfe = new BooleanFieldEditor("defaultHeapUpdateEnabled", "Heap updates enabled by default",
					getFieldEditorParent());

			addField(bfe);

			IntegerFieldEditor ife = new IntegerFieldEditor("threadStatusInterval", "Thread status interval (seconds):",
					getFieldEditorParent());

			ife.setValidRange(1, 60);
			addField(ife);
		}
	}

	private static class LogCatPrefs extends FieldEditorPreferencePage {
		public LogCatPrefs() {
			super();
			setTitle("Logcat");
		}

		protected void createFieldEditors() {
			if (UIThread.useOldLogCatView()) {
				RadioGroupFieldEditor rgfe = new RadioGroupFieldEditor("ddmsLogColumnMode",
						"Message Column Resizing Mode", 1,
						new String[][] { { "Manual", "manual" }, { "Automatic", "auto" } }, getFieldEditorParent(),
						true);

				addField(rgfe);

				FontFieldEditor ffe = new FontFieldEditor("ddmsLogFont", "Text output font:", getFieldEditorParent());

				addField(ffe);
			} else {
				FontFieldEditor ffe = new FontFieldEditor("logcat.view.font", "Text output font:",
						getFieldEditorParent());

				addField(ffe);

				IntegerFieldEditor maxMessages = new IntegerFieldEditor("logcat.messagelist.max.size",
						"Maximum number of logcat messages to buffer", getFieldEditorParent());

				addField(maxMessages);

				BooleanFieldEditor autoScrollLock = new BooleanFieldEditor("logcat.view.auto-scroll-lock",
						"Automatically enable/disable scroll lock based on the scrollbar position",
						getFieldEditorParent());

				addField(autoScrollLock);
			}
		}
	}

	private static class MiscPrefs extends FieldEditorPreferencePage {
		public MiscPrefs() {
			super();
			setTitle("Misc");
		}

		protected void createFieldEditors() {
			IntegerFieldEditor ife = new IntegerFieldEditor("timeOut", "ADB connection time out (ms):",
					getFieldEditorParent());

			addField(ife);

			ife = new IntegerFieldEditor("profilerBufferSizeMb", "Profiler buffer size (MB):", getFieldEditorParent());

			addField(ife);

			DirectoryFieldEditor dfe = new DirectoryFieldEditor("textSaveDir", "Default text save dir:",
					getFieldEditorParent());

			addField(dfe);

			dfe = new DirectoryFieldEditor("imageSaveDir", "Default image save dir:", getFieldEditorParent());

			addField(dfe);

			FontFieldEditor ffe = new FontFieldEditor("textOutputFont", "Text output font:", getFieldEditorParent());

			addField(ffe);

			RadioGroupFieldEditor rgfe = new RadioGroupFieldEditor("ddmsLogLevel", "Logging Level", 1,
					new String[][] { { "Verbose", Log.LogLevel.VERBOSE.getStringValue() },
							{ "Debug", Log.LogLevel.DEBUG.getStringValue() },
							{ "Info", Log.LogLevel.INFO.getStringValue() },
							{ "Warning", Log.LogLevel.WARN.getStringValue() },
							{ "Error", Log.LogLevel.ERROR.getStringValue() },
							{ "Assert", Log.LogLevel.ASSERT.getStringValue() } },
					getFieldEditorParent(), true);

			addField(rgfe);
		}
	}

	private static class UsageStatsPrefs extends PreferencePage {
		private BooleanFieldEditor mOptInCheckbox;
		private Composite mTop;

		public UsageStatsPrefs() {
			setTitle("Usage Stats");
		}

		protected Control createContents(Composite parent) {
			this.mTop = new Composite(parent, 0);
			this.mTop.setLayout(new GridLayout(1, false));
			this.mTop.setLayoutData(new GridData(1808));

			Label text = new Label(this.mTop, 64);
			text.setLayoutData(new GridData(768));
			text.setText(
					"By choosing to send certain usage statistics to Google, you can help us improve the Android SDK. These usage statistics lets us measure things like active usage of the SDK, and let us know things like which versions of the SDK are in use and which tools are the most popular with developers. This limited data is not associated with personal information about you, and is examined on an aggregate basis, and is maintained in accordance with the Google Privacy Policy.");

			Link privacyPolicyLink = new Link(this.mTop, 64);
			privacyPolicyLink
					.setText("<a href=\"http://www.google.com/intl/en/privacy.html\">Google Privacy Policy</a>");
			privacyPolicyLink.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					SdkStatsPermissionDialog.openUrl(event.text);
				}
			});
			this.mOptInCheckbox = new BooleanFieldEditor("pingOptIn", "Send usage statistics to Google.", this.mTop);

			this.mOptInCheckbox.setPage(this);
			this.mOptInCheckbox.setPreferenceStore(getPreferenceStore());
			this.mOptInCheckbox.load();

			return null;
		}

		protected Point doComputeSize() {
			if (this.mTop != null) {
				return this.mTop.computeSize(450, -1, true);
			}
			return super.doComputeSize();
		}

		protected void performDefaults() {
			if (this.mOptInCheckbox != null) {
				this.mOptInCheckbox.loadDefault();
			}
			super.performDefaults();
		}

		public void performApply() {
			if (this.mOptInCheckbox != null) {
				this.mOptInCheckbox.store();
			}
			super.performApply();
		}

		public boolean performOk() {
			if (this.mOptInCheckbox != null) {
				this.mOptInCheckbox.store();
			}
			return super.performOk();
		}
	}
}

/* Location:           D:\android-sdk-windows\tools\lib\ddms.jar

 * Qualified Name:     com.android.ddms.PrefsDialog

 * JD-Core Version:    0.7.0.1

 */
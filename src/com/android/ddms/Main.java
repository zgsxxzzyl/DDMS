package com.android.ddms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Properties;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.DebugPortManager;
import com.android.ddmlib.Log;
import com.android.sdkstats.SdkStatsService;

public class Main {
	public static String sRevision;

	private static class UncaughtHandler implements Thread.UncaughtExceptionHandler {
		public void uncaughtException(Thread t, Throwable e) {
			Log.e("ddms", "shutting down due to uncaught exception");
			Log.e("ddms", e);
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		if (isMac()) {
			RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
			System.setProperty("JAVA_STARTED_ON_FIRST_THREAD_" + rt.getName().split("@")[0], "1");
		}
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtHandler());

		PrefsDialog.init();

		Log.d("ddms", "Initializing");

		Display.setAppName("DDMS");
		Shell shell = new Shell(Display.getDefault());

		SdkStatsService stats = new SdkStatsService();
		stats.checkUserPermissionForPing(shell);
		if ((args.length >= 3) && (args[0].equals("ping"))) {
			stats.ping(args);
			return;
		}
		if (args.length > 0) {
			Log.e("ddms", "Unknown argument: " + args[0]);
			System.exit(1);
		}
		String ddmsParentLocation = System.getProperty("com.android.ddms.bindir");
		if (ddmsParentLocation == null) {
			ddmsParentLocation = System.getenv("com.android.ddms.bindir");
		}
		ping(stats, ddmsParentLocation);
		stats = null;

		DebugPortManager.setProvider(DebugPortProvider.getInstance());

		UIThread ui = UIThread.getInstance();
		try {
			ui.runUI(ddmsParentLocation);
		} finally {
			PrefsDialog.save();

			AndroidDebugBridge.terminate();
		}
		Log.d("ddms", "Bye");

		System.exit(0);
	}

	static boolean isMac() {
		return System.getProperty("os.name").startsWith("Mac OS");
	}

	private static void ping(SdkStatsService stats, String ddmsParentLocation) {
		Properties p = new Properties();
		try {
			File sourceProp;
			if ((ddmsParentLocation != null) && (ddmsParentLocation.length() > 0)) {
				sourceProp = new File(ddmsParentLocation, "source.properties");
			} else {
				sourceProp = new File("source.properties");
			}
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(sourceProp);
				p.load(fis);
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException ignore) {
					}
				}
				sRevision = p.getProperty("Pkg.Revision");
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException ignore) {
					}
				}
			}
			if ((sRevision != null) && (sRevision.length() > 0)) {
				stats.ping("ddms", sRevision);
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
}
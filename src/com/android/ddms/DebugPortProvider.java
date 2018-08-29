package com.android.ddms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;

import com.android.ddmlib.DebugPortManager.IDebugPortProvider;
import com.android.ddmlib.IDevice;

public class DebugPortProvider implements IDebugPortProvider {
	private static DebugPortProvider sThis = new DebugPortProvider();
	public static final String PREFS_STATIC_PORT_LIST = "android.staticPortList";
	private Map<String, Map<String, Integer>> mMap;

	public static DebugPortProvider getInstance() {
		return sThis;
	}

	private DebugPortProvider() {
		computePortList();
	}

	public int getPort(IDevice device, String appName) {
		if (this.mMap != null) {
			Map<String, Integer> deviceMap = (Map) this.mMap.get(device.getSerialNumber());
			if (deviceMap != null) {
				Integer i = (Integer) deviceMap.get(appName);
				if (i != null) {
					return i.intValue();
				}
			}
		}
		return -1;
	}

	public Map<String, Map<String, Integer>> getPortList() {
		return this.mMap;
	}

	private void computePortList() {
		this.mMap = new HashMap();

		IPreferenceStore store = PrefsDialog.getStore();
		String value = store.getString("android.staticPortList");
		if ((value != null) && (value.length() > 0)) {
			String[] portSegments = value.split("\\|");
			for (String seg : portSegments) {
				String[] entry = seg.split(":");

				String deviceName = null;
				if (entry.length == 3) {
					deviceName = entry[2];
				} else {
					deviceName = "emulator-5554";
				}
				Map<String, Integer> deviceMap = (Map) this.mMap.get(deviceName);
				if (deviceMap == null) {
					deviceMap = new HashMap();
					this.mMap.put(deviceName, deviceMap);
				}
				deviceMap.put(entry[0], Integer.valueOf(entry[1]));
			}
		}
	}

	public void setPortList(Map<String, Map<String, Integer>> map) {
		this.mMap.clear();
		this.mMap.putAll(map);

		StringBuilder sb = new StringBuilder();

		Set<String> deviceKeys = map.keySet();
		for (Iterator it = deviceKeys.iterator(); it.hasNext();) {
			String deviceKey = (String) it.next();
			Map deviceMap = (Map) map.get(deviceKey);
			if (deviceMap != null) {
				Set<String> appKeys = deviceMap.keySet();
				for (String appKey : appKeys) {
					Integer port = (Integer) deviceMap.get(appKey);
					if (port != null) {
						sb.append(appKey).append(':').append(port.intValue()).append(':').append(deviceKey).append('|');
					}
				}
			}
		}
		String deviceKey;
		Map<String, Integer> deviceMap;
		String value = sb.toString();

		IPreferenceStore store = PrefsDialog.getStore();

		store.setValue("android.staticPortList", value);
	}
}
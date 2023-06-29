package com.pervacio.wds.app;

import android.util.Log;

import com.pervacio.wds.custom.utils.Constants;

import java.net.InetAddress;

public class EMDeviceInfo {
	public final static int EM_SUPPORTS_CONTACTS = 1;
	public final static int EM_SUPPORTS_CALENDAR = 2;
	public final static int EM_SUPPORTS_PHOTOS = 4;
	public final static int EM_SUPPORTS_NOTES = 8;
	public final static int EM_SUPPORTS_DOCUMENTS = 16;
	public final static int EM_SUPPORTS_MUSIC = 32;
	public final static int EM_SUPPORTS_ACCOUNTS = 64;
	public final static int EM_SUPPORTS_TASKS = 128;
	public final static int EM_SUPPORTS_VIDEOS = 256;
	public final static int EM_SUPPORTS_SMS_MESSAGES = 512;
	public final static int EM_SUPPORTS_APP = 1024;

	public final static int EM_SUPPORTS_ROLE_MIGRATION_SOURCE = 1;
	public final static int EM_SUPPORTS_ROLE_MIGRATION_TARGET = 2;

	public String mDeviceName;
	public String mDeviceUniqueId;
	//	public InetAddress mIpAddress;
	public InetAddress mIpV6Address;
	public InetAddress mIpV4Address;
	//	public String mHostName;
	public int mPort;
	public int mCapabilities;
	public int mRoles;
	public String mServiceName;
	public boolean mKeyboardShortcutImporterAvailable;
	public boolean mThisDeviceIsTargetAutoConnect;
	public String deniedPermissionsDataTypes;

	// START – Pervacio
	//Added for Dashboard Logging. db = "D"ash"b"oard
	public String dbDeviceMake = Constants.UNKNOWN;
	public String dbDeviceModel = Constants.UNKNOWN;
	public String dbDeviceOSVersion = Constants.UNKNOWN;
	public String dbDevicePlatform = Constants.UNKNOWN;
	public String dbOperationType = Constants.UNKNOWN;
	public String dbDeviceBuildNumber = Constants.UNKNOWN;
	public String dbDeviceFirmware = Constants.UNKNOWN;
	public String dbDeviceIMEI = Constants.UNKNOWN;
	public long dbDeviceFreeStorage = Constants.NO_STORAGE;
	public long dbDeviceTotalStorage = Constants.NO_STORAGE;
	public String appVersion = Constants.UNKNOWN;
	// END – Pervacio


	public EMDeviceInfo clone() {
		EMDeviceInfo deviceInfoCopy = new EMDeviceInfo();

		deviceInfoCopy.mDeviceName = new String(mDeviceName);
//		deviceInfoCopy.mIpAddress = new InetAddress(mIpAddress);
//		deviceInfoCopy.mIpAddress = mIpAddress; // TODO: not copying this - assume no problem as it's immutable?
		deviceInfoCopy.mIpV4Address = mIpV4Address; // TODO: not copying this - assume no problem as it's immutable?
		deviceInfoCopy.mIpV6Address = mIpV6Address; // TODO: not copying this - assume no problem as it's immutable?
//		deviceInfoCopy.mHostName = new String(mHostName);
		deviceInfoCopy.mPort = mPort;
		deviceInfoCopy.mCapabilities = mCapabilities;
		deviceInfoCopy.mRoles = mRoles;
		deviceInfoCopy.mServiceName = new String(mServiceName);
		deviceInfoCopy.mKeyboardShortcutImporterAvailable = mKeyboardShortcutImporterAvailable;
		deviceInfoCopy.mDeviceUniqueId = mDeviceUniqueId;
		deviceInfoCopy.mThisDeviceIsTargetAutoConnect = mThisDeviceIsTargetAutoConnect;
		deviceInfoCopy.deniedPermissionsDataTypes=deniedPermissionsDataTypes;

		// START – Pervacio
		deviceInfoCopy.dbDeviceMake = dbDeviceMake;
		deviceInfoCopy.dbDeviceModel = dbDeviceModel;
		deviceInfoCopy.dbDeviceOSVersion = dbDeviceOSVersion;
		deviceInfoCopy.dbDevicePlatform = dbDevicePlatform;
		deviceInfoCopy.dbOperationType = dbOperationType;
		deviceInfoCopy.dbDeviceBuildNumber = dbDeviceBuildNumber;
		deviceInfoCopy.dbDeviceFirmware = dbDeviceFirmware;
		deviceInfoCopy.dbDeviceIMEI = dbDeviceIMEI;
		deviceInfoCopy.dbDeviceFreeStorage = dbDeviceFreeStorage;
		deviceInfoCopy.dbDeviceTotalStorage = dbDeviceTotalStorage;
		deviceInfoCopy.appVersion = appVersion;
		// END – Pervacio

		return deviceInfoCopy;
	}

	public void log() {
		DLog.log("Device info:");
		DLog.log(String.format("    mDeviceName: %s", mDeviceName));
//		DLog.log(String.format("    mIpAddress: %s", mIpAddress));
		DLog.log(String.format("    mIpV4Address: %s", mIpV4Address));
		DLog.log(String.format("    mIpV6Address: %s", mIpV6Address));
		DLog.log(String.format("    mPort: %d", mPort));
		DLog.log(String.format("    mCapabilities: %d", mCapabilities));
		DLog.log(String.format("    mRoles: %d", mRoles));
		DLog.log(String.format("    mServiceName: %s", mServiceName));
		DLog.log(String.format("    mDeviceUniqueId: %s", mDeviceUniqueId));
		DLog.log(String.format("    mThisDeviceIsTargetAutoConnect: %s", mThisDeviceIsTargetAutoConnect));

		// START – Pervacio
		DLog.log("dbDeviceMake : " + dbDeviceMake);
		DLog.log("dbDeviceModel : " + dbDeviceModel);
		DLog.log("dbDeviceOSVersion : " + dbDeviceOSVersion);
		DLog.log("dbDevicePlatform : " + dbDevicePlatform);
		DLog.log("dbOperationType : " + dbOperationType);
		DLog.log("dbDeviceBuildNumber : " + dbDeviceBuildNumber);
		DLog.log("dbDeviceFirmware : " + dbDeviceFirmware);
		DLog.log("dbDeviceIMEI : " + dbDeviceIMEI);
		DLog.log("dbDeviceFreeStorage : " + dbDeviceFreeStorage);
		DLog.log("dbDeviceTotalStorage : " + dbDeviceTotalStorage);
		DLog.log("dbAppVersion : " + appVersion);
		// END – Pervacio
	}

	@Override
	public String toString() {
		return new String(mDeviceName);
	}
}

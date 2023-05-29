/*************************************************************************
 *
 * Media Mushroom Limited CONFIDENTIAL
 * __________________
 *
 *  Copyright 2017 Media Mushroom Limited
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Media Mushroom Limited.
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Media Mushroom Limited.
 */

package com.pervacio.wds.sdk.internal.google;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.drive.DriveId;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.sdk.CMDError;

// TODO: should these be merged into CMDGoogleDriveAccess? Most functions here take CMDGoogleDriveAccess as a parameter.
public class CMDGoogleUtility {
	
	public enum CMDGoogleDriveObjectType {
		CMD_GOOGLE_DRIVE_FOLDER,
		CMD_GOOGLE_DRIVE_FILE,
		CMD_GOOGLE_DRIVE_ANY
	}

	static boolean mBackupFolderCreated = false;

	static private String getLatestItemIdForName(List<CMDGoogleDriveAccess.CMDGoogleDriveItem> itemsToSearch,
											  	String itemName) {
		String matchingObjectId = null;

		CMDGoogleDriveAccess.CMDGoogleDriveItem item = getLatestItemForName(itemsToSearch, itemName);

		if (item != null)
			matchingObjectId = item.mGoogleDriveId;

		return matchingObjectId;
	}

	static private CMDGoogleDriveAccess.CMDGoogleDriveItem getLatestItemForName(List<CMDGoogleDriveAccess.CMDGoogleDriveItem> itemsToSearch,
												 String itemName) {
		CMDGoogleDriveAccess.CMDGoogleDriveItem matchingItem = null;

		java.util.Date mostRecentMatchingDate = null;
		for (CMDGoogleDriveAccess.CMDGoogleDriveItem childItem : itemsToSearch) {
			if (childItem.mIsTrashed) {
				continue;
			}

			String title = childItem.mName;

			String requestedObjectName = itemName;

			if (title.equals(requestedObjectName)) {
				java.util.Date modifiedDate = childItem.mModifiedDate;

				if (mostRecentMatchingDate == null)
				{
					matchingItem = childItem;
					mostRecentMatchingDate = modifiedDate;
				}
				else if (modifiedDate.after(mostRecentMatchingDate))
				{
					// If this matching entry is later than the previous latest then choose this one
					matchingItem = childItem;
					mostRecentMatchingDate = modifiedDate;
				}
			}
		}

		return matchingItem;
	}

	static public CMDGoogleDriveAccess.CMDGoogleDriveItem getDriveItemForPathBlocking(String path,
												   CMDGoogleDriveAccess googleDriveAccess, EMProgressHandler aProgressHandler) {
		CMDGoogleDriveAccess.CMDGoogleDriveItem driveItem = null;

		String[] pathComponents = path.split("/");

		if (pathComponents.length > 0) {
			CMDGoogleDriveAccess.CMDGoogleDriveItem rootFolder = googleDriveAccess.new CMDGoogleDriveItem();
			int result = googleDriveAccess.getRootFolder(rootFolder);

			String  currentFolderId = rootFolder.mGoogleDriveId;

			for (int pathComponentIndex = 0; pathComponentIndex < pathComponents.length; pathComponentIndex++) {

				CMDGoogleDriveAccess.CMDGoogleDriveItem matchingObject = null;
				if (pathComponents[pathComponentIndex].equals(""))
					continue; // Ignore any empty path components


				ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem> childItems = new ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem>();
				result = googleDriveAccess.listChildren(currentFolderId, childItems, aProgressHandler);
				if (result != CMDError.CMD_RESULT_OK)
					return null; // An error has occured listing the child items, so return null (signalling an error)

				// Enumerate child files and folders
				matchingObject = getLatestItemForName(childItems, pathComponents[pathComponentIndex]);

				if (matchingObject == null) {
					// We have not been able to find the object - so return null
					// TODO: should we provide more info of what caused the error?
					return null;
				}

				if (pathComponentIndex == pathComponents.length - 1) {
					// If this is the last element in the path then we have our ID
					driveItem = matchingObject;
				}
				else
				{
					// If this isn't the last element in the path then switch to this folder
					currentFolderId = matchingObject.mGoogleDriveId;
				}
			}
		}

		return driveItem;
	}

	static public String getDriveIdForPathBlocking(String path,
												CMDGoogleDriveAccess googleDriveAccess,
												   EMProgressHandler aProgressHandler) {
		String driveId = null;

		CMDGoogleDriveAccess.CMDGoogleDriveItem item = getDriveItemForPathBlocking(path, googleDriveAccess, aProgressHandler);

		if (item != null)
			driveId = item.mGoogleDriveId;

		return driveId;
	}

	// Make the path and return the ID
	// If the path already exists then the ID of the existing item is returned (unless aForceCreate)
	public static String makePath(String path,
								CMDGoogleDriveAccess googleDriveAccess,
								  EMProgressHandler aProgressHandler) {

		// Set the current parent ID to the root
		int result = CMDError.CMD_RESULT_OK;
		CMDGoogleDriveAccess.CMDGoogleDriveItem rootItem = googleDriveAccess.new CMDGoogleDriveItem();
		result = googleDriveAccess.getRootFolder(rootItem);
		if (result != CMDError.CMD_RESULT_OK)
			return null;
		String currentParentId = rootItem.mGoogleDriveId;

		// For each non-empty path component:
		String[] remotePathComponents = path.split("/");
		for(String pathComponent : remotePathComponents) {
			if (pathComponent.isEmpty())
				continue;

			// Does the folder exist under the current ID?
			ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem> childItems = new ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem>();
			result = googleDriveAccess.listChildren(currentParentId, childItems, aProgressHandler);
			if (result != CMDError.CMD_RESULT_OK)
				return null; // An error has occured listing the child items, so return null (signalling an error)
			String matchingObjectId = getLatestItemIdForName(childItems, pathComponent);
			if ((matchingObjectId == null)
				|| (!mBackupFolderCreated)) { // Always create the first folder, even if there's already a matching one (to ensure that we always get a new backup)
				// We have not been able to find the item - so create the folder
				mBackupFolderCreated = true;
				CMDGoogleDriveAccess.CMDGoogleDriveItem createdFolder = googleDriveAccess.createFolder(currentParentId, pathComponent, aProgressHandler);
				if (createdFolder == null)
					return null;
				else
					currentParentId = createdFolder.mGoogleDriveId;
			}
			else {
				// make the ID the current parent ID and continue
				currentParentId = matchingObjectId;
			}
		}

        return currentParentId;
	}

	private DriveId mBackupDriveId = null;
}

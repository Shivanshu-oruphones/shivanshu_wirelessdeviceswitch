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

import android.app.Activity;

import com.pervacio.wds.app.EMConfig;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSimpleAsyncTask;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.EMXmlGenerator;
import com.pervacio.wds.app.EMXmlPullParser;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDCopyFileProgressDelegate;

import java.util.ArrayList;

public class CMDGoogleAuthenticateAsyncTask extends EMSimpleAsyncTask implements EMProgressHandler {
    public CMDGoogleAuthenticateAsyncTask(String aAccountName,
                                          Activity aActivity,
                                          CMDGoogleDriveAccess aGoogleDriveAccess,
                                          boolean aDestinationDeviceMode) {
        mAccountName = aAccountName;
        mActivity = aActivity;
        mGoogleDriveAccess = aGoogleDriveAccess;
        mDestinationDeviceMode = aDestinationDeviceMode;
    }

    @Override
    public void runTask() {
        int result = mGoogleDriveAccess.authenticate(mAccountName, mActivity);

        if (result != CMDError.CMD_RESULT_OK) {
            setFailed(result);
            return;
        }
        else {
            if (mDestinationDeviceMode) {
                try {
                    EMXmlGenerator xmlGenerator = new EMXmlGenerator();
                    xmlGenerator.startDocument();
                    xmlGenerator.startElement(EMStringConsts.EM_XML_DESTINATION_INFO_SOFTWARE_VERSION_ID);
                    xmlGenerator.writeText(EMConfig.DESTINATION_DEVICE_SOFTWARE_ID);
                    xmlGenerator.endElement(EMStringConsts.EM_XML_DESTINATION_INFO_SOFTWARE_VERSION_ID);
                    String sourceDeviceInfoFilename = xmlGenerator.endDocument();

                    CMDCopyFileProgressDelegate dummyCopyFileProgress = new CMDCopyFileProgressDelegate() {
                        @Override
                        public void onCopyFileProgress(long aTotalFileDataCopied) {
                            // Ignore
                        }
                    };

                    int writeDeviceInfoResult = mGoogleDriveAccess.copyFileFromLocal("root", sourceDeviceInfoFilename, EMStringConsts.EM_DEVICE_INFO_FILENAME, this, dummyCopyFileProgress);
                    if (writeDeviceInfoResult != CMDError.CMD_RESULT_OK) {
                        setFailed(writeDeviceInfoResult);
                        return;
                    }
                }
                catch (Exception ex) {
                    setFailed(CMDError.CMD_ERROR_UNABLE_TO_CREATE_DEVICE_INFO_FILE);
                    return;
                }
            }
            else {
                ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem> childItems = new ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem>();
                EMProgressHandler dummyProgressHandler = new EMProgressHandler() {
                    @Override
                    public void taskComplete(boolean aSuccess) {
                        // Dummy
                    }

                    @Override
                    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
                        // Dummy
                    }

                    @Override
                    public void progressUpdate(EMProgressInfo aProgressInfo) {
                        // Dummy
                    }
                };

                if (EMConfig.REQUIRES_SPECIFIC_REMOTE_DEVICE_APP_VERSION) {
                    int getDeviceInfoResult = mGoogleDriveAccess.listChildren("root", childItems, dummyProgressHandler);
                    if (getDeviceInfoResult != CMDError.CMD_RESULT_OK) {
                        result = CMDError.CMD_GOOGLE_DRIVE_DESTINATION_DEVICE_REQUIRES_UPDATE;
                    } else {
                        boolean otherDeviceRequiresUpdate = true;

                        for (CMDGoogleDriveAccess.CMDGoogleDriveItem driveItem : childItems) {
                            if (driveItem.mName.equalsIgnoreCase(EMStringConsts.EM_DEVICE_INFO_FILENAME)) {
                                String temporaryFileName = EMUtility.temporaryFileName();

                                CMDCopyFileProgressDelegate dummyCopyFileProgress = new CMDCopyFileProgressDelegate() {
                                    @Override
                                    public void onCopyFileProgress(long aTotalFileDataCopied) {
                                        // Ignore
                                    }
                                };

                                result = mGoogleDriveAccess.copyFileToLocal(driveItem.mGoogleDriveId, temporaryFileName, dummyProgressHandler, dummyCopyFileProgress);
                                if (result != CMDError.CMD_RESULT_OK) {
                                    result = CMDError.CMD_GOOGLE_DRIVE_DESTINATION_DEVICE_REQUIRES_UPDATE;
                                } else {
                                    int destinationSoftwareVersion = -1;
                                    try {
                                        EMXmlPullParser deviceInfoParser = new EMXmlPullParser();
                                        deviceInfoParser.setFilePath(temporaryFileName);
                                        EMXmlPullParser.EMXmlNodeType nodeType = deviceInfoParser.readNode();
                                        String mostRecentValue = "";
                                        while (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT) {
                                            if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                                                mostRecentValue = deviceInfoParser.value();
                                            }

                                            if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT) {
                                                String elementName = deviceInfoParser.name();

                                                if (elementName.equalsIgnoreCase(EMStringConsts.EM_XML_DESTINATION_INFO_SOFTWARE_VERSION_ID)) {
                                                    int versionNumber;

                                                    try {
                                                        versionNumber = Integer.parseInt(mostRecentValue);
                                                    } catch (Exception ex) {
                                                        versionNumber = -1;
                                                    }

                                                    if (versionNumber >= EMConfig.MINIMUM_DESTINATION_APP_VERSION) {
                                                        otherDeviceRequiresUpdate = false;
                                                        break;
                                                    }
                                                }
                                            }

                                            nodeType = deviceInfoParser.readNode();
                                        }
                                    } catch (Exception ex) {
                                        // Ignore
                                    }
                                }

                                break;
                            }
                        }

                        if (otherDeviceRequiresUpdate) {
                            result = CMDError.CMD_GOOGLE_DRIVE_DESTINATION_DEVICE_REQUIRES_UPDATE;
                        }

                    }
                }
            }
        }

        if (result != CMDError.CMD_RESULT_OK) {
            setFailed(result);
            return;
        }
    }

    private String mAccountName;
    private Activity mActivity;
    private CMDGoogleDriveAccess mGoogleDriveAccess;
    private boolean mDestinationDeviceMode;

    @Override
    public void taskComplete(boolean aSuccess) {
        // Ignore
    }

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
        // Ignore
    }

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo) {
        // Ignore
    }
}

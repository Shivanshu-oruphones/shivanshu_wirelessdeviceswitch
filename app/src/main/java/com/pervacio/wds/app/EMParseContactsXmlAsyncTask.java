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

package com.pervacio.wds.app;

import android.os.Environment;

import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;

import java.io.ByteArrayInputStream;
import java.io.File;

public class EMParseContactsXmlAsyncTask extends EMParseDataInThread {

    private int parseXml(boolean aCountOnly) {
        DLog.log("parseXml aCountOnly:" + aCountOnly);

        int numberOfEntriesRet = 0;

        if (EMConfig.LOG_CONTACTS_XML) {
            File extStore = Environment.getExternalStorageDirectory();
            String contactDestinationPath = extStore.toString() + File.separator + "contacts.txt";
            EMUtility.copyFile(new File(mFilePath), new File(contactDestinationPath));
        }

        EMXmlPullParser.EMXmlNodeType nodeType;
        EMXmlPullParser pullParser = new EMXmlPullParser();
        int currentItem = 0;

        try {
            pullParser.setFilePath(mFilePath);
            nodeType = pullParser.readNode();
        } catch (Exception ex) {
            // TODO: log an error
            // Something has gone badly wrong and we can't read the file to bail out

            return 0;
        }

        int addressBookSaveCounter = 0;

        VCardAdder vcardAdder = new VCardAdder(mContext);
        long lastUpdateTime = 0;
        long uiUpdateIntervalInMs = 3*1000;

        while (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT) // TODO: need to check this with more corrupt XML data (e.g. without end elements)
        {
            /*if (isCancelled())
                return 0;*/

            try {
                if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT) {
                    String elementName = pullParser.name();

                    if ((elementName.equalsIgnoreCase(EMStringConsts.EM_XML_CONTACT_ENTRY))
                            || (elementName.equalsIgnoreCase(EMStringConsts.EM_XML_CONTACT_VCARD_ENTRY))) {
                        nodeType = pullParser.readNode();

                        if (elementName.equalsIgnoreCase(EMStringConsts.EM_XML_CONTACT_ENTRY))
                            numberOfEntriesRet++;

                        if ((nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) && (!aCountOnly)) {
                            if (elementName.equalsIgnoreCase(EMStringConsts.EM_XML_CONTACT_ENTRY)) {
                                ++currentItem;
                                if ((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs) {
                                    lastUpdateTime = System.currentTimeMillis();
                                    EMProgressInfo progress = new EMProgressInfo();
                                    progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
                                    progress.mDataType = EMDataType.EM_DATA_TYPE_CONTACTS;
                                    progress.mTotalItems = mNumberOfEntries;
                                    progress.mCurrentItemNumber = currentItem;
                                    updateProgressFromWorkerThread(progress);
                                    DLog.log("Processing contact >> " + currentItem);
                                }
                            }
                            String vcardString = pullParser.value();
                            String contactHash = EMUtility.md5(vcardString);
                            if (emPreviouslyTransferredContentRegistry.itemHasBeenPreviouslyTransferred(contactHash)) {
                                // TODO: log that we have skipped this entry?
                                DLog.log("Skipping previously transferred contact "+currentItem);
                                EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_CONTACTS);
                                continue;
                            }
                            emPreviouslyTransferredContentRegistry.addToPreviouslyTransferredItem(contactHash);
                            if (!vcardString.contains("\r\n")) {
                                vcardString = vcardString.replace("\n", "\r\n");
                            }

                            byte[] vcardBytes = vcardString.getBytes("UTF-8");
                            ByteArrayInputStream byteArrayInputString = new ByteArrayInputStream(vcardBytes);

                            try {
                                if (vcardString.length() > 20) { // If the vCard string is less that 20 then we've probably picked up some whitespace, so ignore it (observed when transferring from BBOS)
                                    vcardAdder.ProcessVCard(byteArrayInputString);
                                    EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_CONTACTS);
                                }
                            } catch (Exception ex) {
                                DLog.log("Unable to parse contact vcard", ex);
                                ex.printStackTrace();
                                EMMigrateStatus.addItemNotTransferred(EMDataType.EM_DATA_TYPE_CONTACTS);
                            }
                        }
                        // TODO: what should we do if it's not text? signal a bad-xml error probably?
                    }

                }

                nodeType = pullParser.readNode();
            } catch (Exception ex) {
                // TODO: log an error
                // Stop parsing
                nodeType = EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT;
                DLog.log("Exception:", ex);
                ex.printStackTrace();
            }
        }
        if (!aCountOnly) {
            EMProgressInfo progress = new EMProgressInfo();
            progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
            progress.mDataType = EMDataType.EM_DATA_TYPE_CONTACTS;
            progress.mTotalItems = mNumberOfEntries;
            progress.mCurrentItemNumber = mNumberOfEntries;
            updateProgressFromWorkerThread(progress);
            DLog.log("Processing Contacts Done >> ");
        }
        return numberOfEntriesRet;
    }

    @Override
    protected void runTask() {
        DLog.log("Contacts parseData Async task");
        emPreviouslyTransferredContentRegistry = EMPreviouslyTransferredContentRegistry.getInstanceOfEMPreviouslyTransferredContentRegistry();
        DashboardLog.getInstance().setContactStartTime(CommonUtil.getInstance().getBackupStartedTime());
        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CONTACT, -1, -1, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, true);
        mNumberOfEntries = parseXml(true); // Count the total number of entries (needed to get the correct progress information)
        parseXml(false); // Actually parse the entries
        DashboardLog.getInstance().setContactEndTime(System.currentTimeMillis());
        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CONTACT, mNumberOfEntries, -1, Constants.TRANSFER_STATUS.SUCCESS, Constants.TRANSFER_STATE.COMPLETED, true);
    }

    private int mNumberOfEntries;
    static final String TAG = "EMParseContactsXmlAsyncTask";
    EMPreviouslyTransferredContentRegistry emPreviouslyTransferredContentRegistry = null;
}

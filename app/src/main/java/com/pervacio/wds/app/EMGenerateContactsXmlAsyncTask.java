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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.pervacio.pim.vcard.VCardConfig;
import com.pervacio.vcard.VCardComposer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class EMGenerateContactsXmlAsyncTask extends EMGenerateDataTask {
	
	int mNumberOfEntries;

//	String mTempFilePath;

	private EMPreviouslyTransferredContentRegistry mPreviouslyTransferredItemsRegistry;

	void setPreviouslyTransferredItemsRegistry(EMPreviouslyTransferredContentRegistry aPreviouslyTransferredItemsRegistry) {
		mPreviouslyTransferredItemsRegistry = aPreviouslyTransferredItemsRegistry;
	}

    boolean vcardContainsMinimalInfo(String aVCardString) {
    	boolean containsMinimalInfo = false;
    	
    	String[] vCardLines = aVCardString.split("\r\n");
    	for (int lineIndex = 0; lineIndex < vCardLines.length; lineIndex++) {
    		String trimmedLine = vCardLines[lineIndex].trim();
    		if ((trimmedLine.startsWith("N:"))
    				|| (trimmedLine.startsWith("N;"))
    				|| (trimmedLine.startsWith("ORG:")
    				|| (trimmedLine.startsWith("ORG;")))) {
    			containsMinimalInfo = true;
    		}
    		
    		if (containsMinimalInfo)
    			break;
    	}
    	
    	return containsMinimalInfo;
    }

	HashMap<String,Integer> mNameCounterMap = new HashMap<String, Integer>();

	@Override
	public void runTask()
	{
		DLog.log(">> EMGenerateContactsXmlAsyncTask::runTask");

		EMXmlGenerator xmlGenerator = null;
		long lastUpdateTime = 0;
		long uiUpdateIntervalInMs = 3*1000;

		// Generate the handshake XML
		try {
			xmlGenerator = new EMXmlGenerator();
			xmlGenerator.startDocument();

			DLog.log("About to query contacts");

			Cursor contactCursor = EMUtility.Context().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

			if (contactCursor == null) {
				DLog.log("contactCursor is null");
			}
			else {
				DLog.log("contactCursor.getCount()" + contactCursor.getCount());
			}

			if (contactCursor.moveToFirst()) {
				int currentItemNumber = 1;

				do {
					if (isCancelled()) {
						contactCursor.close();
						// TODO: remove temporary file
						return;
					}

					// TODO: get vCard details for contact
					String lookupKey = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
					//DLog.log("lookupKey: " + lookupKey);
					try {
						String inVisibleGroup = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.IN_DEFAULT_DIRECTORY));

						if (!inVisibleGroup.equals("1")) {// Skip invisible contacts
							DLog.log("Skipping invisible contact");
							//continue;
						}
					} catch (Exception ex) {
						// Ignore any exceptions checking whether the contact is in the visible group - just continue
					}

					Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
					AssetFileDescriptor vCardFileDescriptor = null;
					FileInputStream vCardInputStream = null;
					String vCardString = null;
					byte[] vCardDataBuffer=new byte[1];
					try {
						//DLog.log("About to creat vCard using Android APIs");
						vCardFileDescriptor = EMUtility.Context().getContentResolver().openAssetFileDescriptor(uri, "r");

						/*FileDescriptor fdd = vCardFileDescriptor.getFileDescriptor();
						vCardInputStream = new FileInputStream(fdd);*/
						vCardInputStream = vCardFileDescriptor.createInputStream();
						if (vCardFileDescriptor.getDeclaredLength() > 0) {
							vCardDataBuffer = new byte[(int) vCardFileDescriptor.getDeclaredLength()];
							vCardInputStream.read(vCardDataBuffer);
							vCardString = new String(vCardDataBuffer);
						} else {
							if (vCardInputStream.available() > 0) {
								try {
									vCardDataBuffer = new byte[vCardInputStream.available()];
									vCardInputStream.read(vCardDataBuffer);
									vCardString = new String(vCardDataBuffer);
								} catch (Exception ex) {
									DLog.log(ex.getMessage());
								}
							} else {
								int contactIdColumn = contactCursor.getColumnIndex(ContactsContract.Contacts._ID);
								String contactId = contactCursor.getString(contactIdColumn);
								vCardDataBuffer = getVCardBytesFromAggregateContactId(contactId);
								vCardString = new String(vCardDataBuffer);
							}
						}
						//DLog.log("Got vCard from Android API");
					} catch (Exception ex) {
						try {
							// Some Android versions have a bug where the above vCard generation can fail, if this happens then we try to use our own internal generator
							//DLog.log(ex);
							int contactIdColumn = contactCursor.getColumnIndex(ContactsContract.Contacts._ID);
							String contactId = contactCursor.getString(contactIdColumn);
							byte[] vcardBytes = getVCardBytesFromAggregateContactId(contactId);
							vCardString = new String(vcardBytes);
							//	DLog.log("Got vcard from internal generator");
							// TODO: handle
						} catch (Exception ex2) {
							DLog.log(ex2);
							DLog.log("Failed to generate vCard using internal generator");
						}
					} finally {
						try {
							if (vCardInputStream != null)
								vCardInputStream.close();
						} catch (Exception ex) {
							DLog.log(ex);
							// Ignore. There's not much we can do about this
						}
					}
					try {
						//if (vcardContainsMinimalInfo(vCardString)) {
						if ((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs) {
							lastUpdateTime = System.currentTimeMillis();
							EMProgressInfo progress = new EMProgressInfo();
							progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA;
							progress.mDataType = EMDataType.EM_DATA_TYPE_CONTACTS;
							progress.mTotalItems = contactCursor.getCount();
							progress.mCurrentItemNumber = currentItemNumber;
							updateProgressFromWorkerThread(progress);
							DLog.log("Processing Contact >> " + currentItemNumber);
						}
						currentItemNumber++;
						mNumberOfEntries++;

							/*String contactHash = EMUtility.md5(vCardString);
							if (itemHasBeenPreviouslyTransferred(contactHash)) {
								// TODO: log that we have skipped this entry?
								DLog.log("Skipping previously trasferred contact");
							} else {*/
						// Write the vCard as an XML entry
						xmlGenerator.startElement(EMStringConsts.EM_XML_CONTACT_ENTRY);

						// TODO: check that this escapes the required XML strings (e.g. quotes)
						xmlGenerator.writeText(vCardString);
						xmlGenerator.endElement(EMStringConsts.EM_XML_CONTACT_ENTRY);
						EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_CONTACTS);
						//addToPreviouslyTransferredItems(contactHash);
						//}

						//}
						if (vCardInputStream != null)
							vCardInputStream.close();
					} catch (Exception aExceptation) {
						DLog.log(aExceptation);
						EMMigrateStatus.addItemNotTransferred(EMDataType.EM_DATA_TYPE_CONTACTS);
					} finally {
						// No clean up required
					}

					if (vCardFileDescriptor != null) {
						vCardFileDescriptor.close();
					}

				} while (contactCursor.moveToNext());
			}

		    EMProgressInfo progress=new EMProgressInfo();
			progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA;
			progress.mDataType = EMDataType.EM_DATA_TYPE_CONTACTS;
			progress.mTotalItems = contactCursor.getCount();
			progress.mCurrentItemNumber = contactCursor.getCount();
			updateProgressFromWorkerThread(progress);
			DLog.log("Processing Contacts Completed >> ");

			contactCursor.close();
		} catch (Exception ex)
		{
			ex.printStackTrace();
			DLog.log(ex);
			EMMigrateStatus.setTotalFailure(EMDataType.EM_DATA_TYPE_CONTACTS);
		}
		finally {
			if (xmlGenerator != null) {
				try {
					xmlGenerator.endElement(EMStringConsts.EM_XML_ROOT);
				} catch (IOException e) {
					// Nothing we can do here
				}

				try {
					setFilePath(xmlGenerator.endDocument());
				} catch (IOException e) {
					// Nothing we can do here
				}
			}
		}

		DLog.log("<< EMGenerateContactsXmlAsyncTask::runTask");
	}


	/**
	 *
	 * From Android AOSP
	 *
	 * Output matching the requested selection in the vCard
	 * format to the given {@link OutputStream}. This method returns silently if
	 * any errors encountered.
	 */
	private void outputAggregateContactsAsVCard(OutputStream stream, String selection,
												String[] selectionArgs) {
		final Context context = EMUtility.Context();
		final VCardComposer composer =
				new VCardComposer(context, VCardConfig.VCARD_TYPE_DEFAULT, false);
		composer.addHandler(composer.new HandlerForOutputStream(stream));

		// No extra checks since composer always uses restricted views
		if (!composer.init(selection, selectionArgs)) {
			DLog.log("Failed to init VCardComposer");
			return;
		}

		while (!composer.isAfterLast()) {
			if (!composer.createOneEntry()) {
				DLog.log("Failed to output a contact.");
			}
		}
		composer.terminate();
	}

	private byte[] getVCardBytesFromAggregateContactId(String aAggregateContactId) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		String[] idSelectionArgs = {aAggregateContactId};
		final String selection = ContactsContract.Contacts._ID + "=?";

		outputAggregateContactsAsVCard(byteArrayOutputStream, selection, idSelectionArgs);

		return byteArrayOutputStream.toByteArray();
	}

	/*
	public String getFilePath() {
		return mTempFilePath;
	}
	*/

	public int getNumberOfEntries() {
		return mNumberOfEntries;
	}
}

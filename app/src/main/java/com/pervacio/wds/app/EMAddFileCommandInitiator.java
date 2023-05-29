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

import android.util.Log;

import java.io.File;

public class EMAddFileCommandInitiator implements EMCommandHandler {

	private final static String TAG = "EMAddFileCommandInit";

	enum EMAddFileState
	{
	    EM_SENDING_ADD_FILE_COMMAND,
	    EM_WAITING_FOR_ADD_FILE_RESPONSE,
	    EM_SENDING_ADD_FILE_XML,
	    EM_WAITING_FOR_XML_OK,
	    EM_SENDING_RAW_FILE_DATA,
	    EM_WAITING_FOR_FINAL_OK,
		EM_CANCELLED
	};
	
    EMDataCommandDelegate mDataCommandDelegate;
    EMCommandDelegate mCommandDelegate;
    EMAddFileState mState;
    
    EMFileMetaData mMetaData;
	EMFileSendingProgressDelegate mFileSendingProgressDelegate;
	
    EMAddFileCommandInitiator(EMFileMetaData aMetaData, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
    	mMetaData = aMetaData;
		mFileSendingProgressDelegate = aFileSendingProgressDelegate;
    }
    
    private void generateMetaData() {
    	try {
			// Log.d(TAG, "=== generateMetaData" +
//					", Current File: " +mMetaData.mCurrentFileNumber+
//					", Num Files: "    +mMetaData.mTotalFiles+
//					", This Size: "    +mMetaData.mSize+
//					", This Type: "    +mMetaData.mDataType+
//					", Total Size: "   +mMetaData.mTotalMediaSize);

	    	EMXmlGenerator xmlGenerator = new EMXmlGenerator();
	    	xmlGenerator.startDocument();
	    	
	    	File file = new File(mMetaData.mSourceFilePath);
	    	// TODO: check file exists
	    	
	    	// TODO: later we need to say what type of file?
	    	xmlGenerator.startElement(EMStringConsts.EM_XML_FILE);
	    	
	    	{
		    	xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_SIZE);
		    	xmlGenerator.writeText(String.valueOf(file.length()));
		    	xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_SIZE);
	    	
		    	xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_NAME);
		    	xmlGenerator.writeText(mMetaData.mFileName);
		    	xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_NAME);

				xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_TOTAL_MEDIA_SIZE);
				xmlGenerator.writeText(String.valueOf(mMetaData.mTotalMediaSize));
				xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_TOTAL_MEDIA_SIZE);

		    	if (mMetaData.mRelativePath != null) {
		    		if (!mMetaData.mRelativePath.equalsIgnoreCase("")) {
				    	xmlGenerator.startElement(EMStringConsts.EM_XML_RELATIVE_PATH);
				    	xmlGenerator.writeText(mMetaData.mRelativePath);
				    	xmlGenerator.endElement(EMStringConsts.EM_XML_RELATIVE_PATH);
		    		}
		    	}
		    	
		    	xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_TYPE);
		    	switch (mMetaData.mDataType) {
		    		case EMDataType.EM_DATA_TYPE_PHOTOS:
		    			xmlGenerator.writeText(EMStringConsts.EM_XML_PHOTOS);
		    			break;
		    		case EMDataType.EM_DATA_TYPE_VIDEO:
		    			xmlGenerator.writeText(EMStringConsts.EM_XML_VIDEO);
		    			break;
					case EMDataType.EM_DATA_TYPE_MUSIC:
		    			xmlGenerator.writeText(EMStringConsts.EM_XML_MUSIC);
		    			break;
					case EMDataType.EM_DATA_TYPE_DOCUMENTS:
						xmlGenerator.writeText(EMStringConsts.EM_XML_DOCUMENTS);
						break;
					case EMDataType.EM_DATA_TYPE_APP:
						xmlGenerator.writeText(EMStringConsts.EM_XML_APP);
						break;
		    		default:
		    			// TODO: unknown file type
		    			break;
		    	}
		    	xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_TYPE);
	    	}
	    	
	    	xmlGenerator.endElement(EMStringConsts.EM_XML_FILE);
	    	
			mGeneratedMetadataXmlFilePath = xmlGenerator.endDocument();
    	} catch (Exception ex) {
    		// TODO: handle error
    	}
    }

	private String mGeneratedMetadataXmlFilePath;
    
	@Override
	public void start(EMCommandDelegate aDelegate) {
	    DLog.log(">> start");
	    mCommandDelegate = aDelegate;

	    mState = EMAddFileState.EM_SENDING_ADD_FILE_COMMAND;

		generateMetaData();

		File metadataFile = new File(mGeneratedMetadataXmlFilePath);

	    mCommandDelegate.sendText(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " " + Long.toString(metadataFile.length()));
	    DLog.log("<< start");
	}

	@Override
	public boolean handlesCommand(String aCommand) {
		return false;
	}

	@Override
	public boolean gotText(String aText) {
	    DLog.log(">> gotText");
	    
	    if (aText.equalsIgnoreCase(EMStringConsts.EM_TEXT_RESPONSE_ALREADY_EXISTS)) {
	    	DLog.log("Already exists");
	    	mCommandDelegate.commandComplete(true); // Ignore files that already exist
	    	return true;
	    }
	    
	    boolean ok = aText.equals(EMStringConsts.EM_TEXT_RESPONSE_OK);
	    if (!ok) {
	        mCommandDelegate.commandComplete(false);
	        return true;
	    }
	    
	    if (mState == EMAddFileState.EM_WAITING_FOR_ADD_FILE_RESPONSE)
	    {
	    	mState = EMAddFileState.EM_SENDING_ADD_FILE_XML;
			mCommandDelegate.sendFile(mGeneratedMetadataXmlFilePath, true, null);
	    }
	    else if (mState == EMAddFileState.EM_WAITING_FOR_XML_OK) {
	    	// TODO: we should sent the file size here so we write exactly the number of bytes we advertised in the metadata
	    	mState = EMAddFileState.EM_SENDING_RAW_FILE_DATA;
	    	mCommandDelegate.sendFile(mMetaData.mSourceFilePath, false, mFileSendingProgressDelegate);
	    }
	    else if (mState == EMAddFileState.EM_WAITING_FOR_FINAL_OK)
	    {
	        mCommandDelegate.commandComplete(true);
	    }

	    DLog.log("<< gotText");

	    return true;
	}

	@Override
	public boolean gotFile(String aDataPath) {
		// Ignore - we haven't asked for any files
		return true;
	}

	@Override
	public void sent() {
	    DLog.log(">> sent");

	    if (mState == EMAddFileState.EM_SENDING_ADD_FILE_COMMAND)
	    {
	        mState = EMAddFileState.EM_WAITING_FOR_ADD_FILE_RESPONSE;
	        mCommandDelegate.getText();
	    }
	    else if (mState == EMAddFileState.EM_SENDING_ADD_FILE_XML)
	    {
	    	mState = EMAddFileState.EM_WAITING_FOR_XML_OK;
	    	mCommandDelegate.getText();
	    }
	    else if (mState == EMAddFileState.EM_SENDING_RAW_FILE_DATA)
	    {
	        mState = EMAddFileState.EM_WAITING_FOR_FINAL_OK;
			EMMigrateStatus.addItemTransferred(mMetaData.mDataType);
	        mCommandDelegate.getText();
	    }
	    else
	    {
	    	// TODO: 
//	        EM_EXCEPTION_RAISE_BAD_STATE(mState);
	    }
	    
	    DLog.log("<< sent");
	}
	
	// Set the delegate to receive notifications about data sending progress
	public void setDataCommandDelegate(EMDataCommandDelegate aDataCommandDelegate)
	{
	    mDataCommandDelegate = aDataCommandDelegate;
	}

	@Override
	public void cancel() {
		mState = EMAddFileState.EM_CANCELLED;
	}
}

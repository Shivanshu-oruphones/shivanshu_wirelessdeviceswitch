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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

// A simple XML generator
// Writes the contents to a temporary file, then writes the contents to the output stream when done
// (which would usually be a socket). The temporary file is used to ensure that the the XML exactly meets the formatting
// requirements.
public class EMXmlGenerator {
	String mFilePath;
	XmlSerializer mXmlTextWriter;

	public void startDocument() throws IllegalArgumentException, IllegalStateException, IOException
	{
	    mFilePath = EMUtility.temporaryFileName();
	    OutputStream outputStream = new FileOutputStream(mFilePath);
	    
	    mXmlTextWriter = Xml.newSerializer();
	    mXmlTextWriter.setOutput(outputStream, "UTF-8");
	    mXmlTextWriter.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
	    mXmlTextWriter.startDocument("UTF-8", true);
	    mXmlTextWriter.startTag(null, EMStringConsts.EM_XML_ROOT);	    
	}

	public String endDocument() throws IllegalArgumentException, IllegalStateException, IOException // Write the last element (always </root> for us)
	{
	    mXmlTextWriter.endDocument();
	    mXmlTextWriter.flush();
	    
	    File file = new File(mFilePath);
	    long fileLength = file.length();
	    if (fileLength > 3) {
		    RandomAccessFile raf = new RandomAccessFile(file, "rw");
		    try {
		        raf.seek(fileLength - 3);
		        if ((raf.readByte() == ' ') && (raf.readByte() == '/') && (raf.readByte() == '>')){
		        	raf.seek(fileLength - 3);
		        	raf.writeByte('/');
		        	raf.writeByte('>');
				    raf.setLength(fileLength - 1);
		        }
		    } finally {
		        raf.close(); // Flush/save changes and close resource.
		    }
		    		    
		    raf.close();
	    }
	    
	    return mFilePath;
	}
	
	public void startElement(String aElementName) throws IllegalArgumentException, IllegalStateException, IOException
	{
	    mXmlTextWriter.startTag(null, aElementName);	    
	}

	public void endElement(String aElementName) throws IllegalArgumentException, IllegalStateException, IOException
	{
	    // Write the end element to the file
		mXmlTextWriter.endTag(null, aElementName);
	}

	public void writeText(String aText) throws IllegalArgumentException, IllegalStateException, IOException
	{
		if (aText == null) {
			aText = "";
		}
		mXmlTextWriter.text(aText);
	}

/*
	void writeData:(NSData*)data
	{
	    if (data != NULL)
	    {
	        @autoreleasepool { // autorelease pool for the utf8 strings
	            xmlTextWriterWriteBase64(mXmlTextWriterPtr, [data bytes], 0, [data length]);
	            // TODO: check err
	        }
	    }
	}
*/
}

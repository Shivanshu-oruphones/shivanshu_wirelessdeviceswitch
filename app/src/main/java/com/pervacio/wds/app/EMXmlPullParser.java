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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class EMXmlPullParser {
	
	public enum EMXmlNodeType
	{
	    EM_NODE_TYPE_START_DOCUMENT,
	    EM_NODE_TYPE_START_ELEMENT,
	    EM_NODE_TYPE_END_ELEMENT,
	    EM_NODE_TYPE_ATTRIBUTES,
	    EM_NODE_TYPE_TEXT,
	    EM_NODE_TYPE_NO_NODE,
	    EM_NODE_TYPE_END_ROOT_ELEMENT,
	    EM_NODE_TYPE_IGNORE
	};
	
    private XmlPullParser mXmlTextReader; // = Xml.newPullParser();
    private String mNameString;
    private String mValueString;

	public void setInputStream(InputStream aInputStream) throws XmlPullParserException
	{
		mXmlTextReader = Xml.newPullParser();
		mXmlTextReader.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		mXmlTextReader.setInput(new InputStreamReader(aInputStream));
	}

	public void setFilePath(String aFilePath) throws FileNotFoundException, XmlPullParserException
	{
		InputStream inputStream = new FileInputStream(aFilePath);
        mXmlTextReader = Xml.newPullParser();
        mXmlTextReader.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        mXmlTextReader.setInput(new InputStreamReader(inputStream));
	}

	private boolean isWhitespace(String aString)
	{
	    boolean whitespace = false;
	    
	    for (int stringPos = aString.length() - 1; stringPos > 0; stringPos--)
	    {
	        switch (aString.charAt(stringPos))
	        {
	            case ' ':
	            case '\n':
	            case '\r':
	            case 9: // tab
	                break;
	            default:
	                whitespace = false;
	                break;
	        }
	    
	        if (!whitespace)
	        {
	            break;
	        }
	    }
	    
	    return whitespace;
	}

	public EMXmlNodeType readNode() throws XmlPullParserException, IOException
	{
	    boolean ignoreNode = false;
	    EMXmlNodeType nodeType = EMXmlNodeType.EM_NODE_TYPE_NO_NODE;
	    do {
	    	int xmlNodeType = mXmlTextReader.next();
	    	nodeType = EMXmlNodeType.EM_NODE_TYPE_IGNORE;
	    	
	    	switch (xmlNodeType)
	    	{
	    		case XmlPullParser.START_DOCUMENT:
	    			nodeType = EMXmlNodeType.EM_NODE_TYPE_START_DOCUMENT;
	    			break;
	    		case XmlPullParser.START_TAG:
	    			nodeType = EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT;
	    			break;
	    		case XmlPullParser.END_DOCUMENT:
	    			nodeType = EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT;
	    			break;
	    		case XmlPullParser.END_TAG:
	    			nodeType = EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT;
	    			break;
	    		case XmlPullParser.TEXT:
	    			nodeType = EMXmlNodeType.EM_NODE_TYPE_TEXT;
	    			break;
	    		default:
	    			ignoreNode = true;
	    			break;
	    	}
	    	    
	        mValueString = mXmlTextReader.getText();
	        mNameString = mXmlTextReader.getName();
	    
                
            if ((nodeType == EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT)
                && (mNameString.equals("root")))
            {
                nodeType = EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT;
            }
	                
            if (nodeType == EMXmlNodeType.EM_NODE_TYPE_TEXT)
            {
                ignoreNode = isWhitespace(mValueString);
            }
	        
            if (!ignoreNode)
                return nodeType; // The cast is safe, we've used the same values as libxml
	        
	    } while (ignoreNode);
	    
	    return nodeType;
	}

	public String value()
	{
	    return mValueString;
	}

	public String name()
	{
	    return mNameString;
	}
}

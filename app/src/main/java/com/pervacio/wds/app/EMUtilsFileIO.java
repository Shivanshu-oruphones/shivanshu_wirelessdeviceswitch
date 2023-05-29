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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class EMUtilsFileIO
{

	
	static public byte[] getFileContents(String aPath)
    {
    	traceit(">> getFileContents, Path: " +aPath);
    	
        File file = new File(aPath);

        if (! file.exists())
        {
        	return null;
        }

        int size = (int) file.length();

        byte[] bytes = new byte[size];

        try
        {
            BufferedInputStream buffer = new BufferedInputStream(new FileInputStream(file));

            buffer.read(bytes, 0, bytes.length);

            buffer.close();
        }
        catch (Exception e)
        {
            warnit("getFileContents, Exception:" +e+ "( " +aPath+ ")");
            bytes = null;
        }

    	traceit("<< getFileContents");        

        return bytes;
    }    
    
	static public void setFileContents(String aPath, byte[] aBytes)
    {
    	traceit(">> setFileContents, Path: " +aPath);
    	
        BufferedOutputStream buffer = null;
        
        try
        {
            File file = new File(aPath);
            
        	buffer = new BufferedOutputStream(new FileOutputStream(file));

            buffer.write(aBytes, 0, aBytes.length);

            buffer.close();
        }
        catch (Exception e)
        {
        	warnit("setFileContents, Exception:" +e+ "( " +aPath+ ")");        	
        }
        
        try
        {
        	if (buffer != null)
    		{
        		buffer.close();     		
    		}
        }
        catch (Exception e)
        {        	
        }
        
    	traceit("<< setFileContents");        
    }        

    static public String[] readFileByLines(String aPath)
    {
    	traceit(">> readFileByLines, Path: " +aPath);        
    	
    	File file = new File(aPath);
    	
    	if (! file.exists())
    	{
        	traceit("<< readFileByLines, No File");            		
    		return null;
    	}
    	
        BufferedReader buffer = null;
        
        List<String> lines = new ArrayList<String>();

    	try
    	{
	        FileReader reader = new FileReader(aPath);
	        buffer = new BufferedReader(reader);
	    	        
	        String thisLine = null;
	        
	        while ((thisLine = buffer.readLine()) != null)
	        {
	            lines.add(thisLine);
	            traceit("readFileByLines, Line: +" +thisLine+ "+");
	        }
    	}
    	catch (Exception e)
    	{
    		errorit("readFileByLines" +e);
    		lines = null;
    	}
    	
    	try	{ buffer.close(); } catch (Exception e) {}
            	
    	String[] fileLines = null;
    	
    	if (lines != null)
    	{
    		fileLines = lines.toArray(new String[lines.size()]);
    	}
    	
    	traceit("<< readFileByLines");        

        return fileLines;
    }    
    
    static public void appendLineToFile(String aPath, String aLine)
    {
    	traceit(">> appendLineToFile, File: " +aPath+ ", Line: " +aLine);            	
    	
    	try
    	{
    		FileWriter     writer  = new FileWriter(aPath, true);    	
    		BufferedWriter buffer  = new BufferedWriter(writer);    		
    	    PrintWriter    printer = new PrintWriter(buffer);
    	    
    	    printer.println(aLine);
    	    printer.close();
    	}
    	catch (Exception e)
    	{
    		errorit("appendLineToFile" +e);    		
    	}  

    	traceit("<< appendLineToFile");            	
    }	
    
	
    //=========================================================================
	// Logging Methods
	//=========================================================================
    
    
	private final static String TAG = "EMUtilsFileIO";
	
    static private void traceit(String aText)
    {
        //Log.v(TAG, aText);
//        DLog.verbose(TAG, aText);
    }

    static private void logit(String aText)
    {
        //// Log.d(TAG, aText);
        //DLog.log(TAG, aText);
    }

    static private void warnit(String aText)
    {
        //Log.e(TAG, aText);
//        DLog.warn(TAG, aText);
    }

    static private void errorit(String aText)
    {
        //Log.e(TAG, aText);
//        DLog.error(TAG, aText);
    }	
}

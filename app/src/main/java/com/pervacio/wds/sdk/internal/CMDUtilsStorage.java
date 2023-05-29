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




package com.pervacio.wds.sdk.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;


import android.content.Context;
import android.os.Build;
import android.os.Environment;


public class CMDUtilsStorage
{
	static EMGlobals emGlobals = new EMGlobals();
    final static private String TAG = "CMDUtilsStorage";

	static private boolean isPrimaryStorageReadable()
	{
	    String state = Environment.getExternalStorageState();
	    	    
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
	    {
	        return true;
	    }
	    
	    return false;
	}
	
	static private String getCanonicalPath(String aPath)
	{
		String canonical = null;
		
		try
		{
			File file = new File(aPath);
			
			canonical = file.getCanonicalPath();
		}
		catch (Exception e)
		{
			canonical = null;
		}
				
		return canonical;		
	}
	
	static private String createFlagFile()
	{
		Random rand = new Random();
		
		String flagFileName = "CMD" + rand.nextInt(100000);

		logit("createFlagFile, Flag: " +flagFileName);

		File primaryStorage = Environment.getExternalStorageDirectory();		
		
		String primaryPath = primaryStorage.getAbsolutePath();			
		
		File flagFile = new File(primaryPath + File.separator + flagFileName);
		
		FileOutputStream stream = null;
		
		try
		{
			stream = new FileOutputStream(flagFile);
			
			stream.write(flagFile.getAbsolutePath().getBytes());			
		}
		catch (Exception e)
		{
			warnit("writeFlagFile, Exception: " +e);
			flagFileName = null;
		}
		
		try
		{
			if (stream != null)
			{
				stream.close();
			}
		}
		catch (Exception e)
		{				
		}
		
		return flagFileName;
	}

	static private void deleteFlagFile(String aFileName)
	{
		if (aFileName == null) return;
		
		try
		{
			File primaryStorage = Environment.getExternalStorageDirectory();		
			
			String primaryPath = primaryStorage.getAbsolutePath();			
			
			File flagFile = new File(primaryPath + File.separator + aFileName);

			flagFile.delete();
		}
		catch (Exception e)
		{
			warnit("deleteFlagFile, Exception: " +e);
		}
				
	}
	
	static private List<String> getSecondaryStorageMounts()
	{	
        logit("getSecondaryStorageMounts");
		
		BufferedReader mountsReader = null;

		String flagName = createFlagFile();		
		
		List<String> secondaryMountPoints = new ArrayList<String>();		

        try
        {
            File mountPointsFile = new File("/proc/mounts");
            
            if (mountPointsFile.exists() && mountPointsFile.canRead())
            {
            	FileInputStream  mountsStream  = new FileInputStream(mountPointsFile);
            	mountsReader = new BufferedReader( new InputStreamReader(mountsStream) );           	

            	String line;
            	
                while ((line = mountsReader.readLine()) != null)
                {                
                    //logit("getSecondaryStorageMount, Line: " + line);
                	
                	line = line.trim();
                	
                	if (line.length() == 0)   continue;
                	if (line.startsWith("#")) continue;

                    String[] words = line.split(" ");

                    if (words.length < 2) continue;
                    
                    if (line.startsWith("/dev/block/vold/") || line.startsWith("/dev/fuse"))
                    {
                    	String thisMountPoint = getCanonicalPath(words[1]);
                    
                    	if (thisMountPoint == null) continue; 

                    	File mountFile = new File(thisMountPoint);

                        if (! mountFile.canRead())
                        {
                            logit(" => getSecondaryStorageMount, Cannot Read Mount: " + thisMountPoint);
                            continue;
                        }
                                                    
                        if (flagName != null)
                        {
                            File flagFile = new File(thisMountPoint + File.separator + flagName);
                            
                            if (flagFile.exists())
                            {
                                logit(" => getSecondaryStorageMount, Partition is Same as Primary: " + thisMountPoint);
                                continue;
                            }
                        }
                        
                        logit(" => getSecondaryStorageMount, Found Secondary Mount: " + thisMountPoint); 
                        
                        secondaryMountPoints.add(thisMountPoint);                            	
                    }
                }
            }
        } catch (Exception e)
        {
            warnit("*** getSecondaryStorageMount, Exception: " +e);
        }		
		  
        deleteFlagFile(flagName);
        
        if (mountsReader != null)
        {
        	try
        	{
        		mountsReader.close();
        	}
        	catch (Exception e) 
        	{        		
        	}        	
        }
        
		return secondaryMountPoints;
	}
	

	// aPathsList is a list of potential secondary storage areas separated by a ":"
	// E.g. /storage/extSdCard:/storage/UsbDriveA:/storage/UsbDriveB:/storage/UsbDriveC
	//
	// This method looks at each path and checks to see if it exists and can be read
	// from. It returns an ArrayList containing the paths to these valid storage areas. 
	//
	static private List<String> getStorageAreasFromList(String aPathsList)
	{
		List<String> paths = new ArrayList<String>();

		if (aPathsList == null)
		{
			logit("getStorageAreasFromList, NULL path list string passed");
			return paths;
		}

		logit("getStorageAreasFromList, Path List: " +aPathsList);

        String[] candidatePaths = aPathsList.split(":");

        for (String thisPath : candidatePaths)
        {
          	File storageArea = new File(thisPath);

            if ((! storageArea.exists()) || (! storageArea.canRead()))
            {
                logit("getStorageAreasFromList, Cannot Read Storage Area: " + thisPath);
                continue;
            }
            
            logit("getStorageAreasFromList, Found Storage Area: " + thisPath);
            
            paths.add(thisPath);
        }
		
		return paths;
	}
	
	// ========================================================================
	// NOTE: Prior to Android 4.4.4 (API Level 19) there is no standard means
	// of finding any secondary storage areas.
	// ========================================================================
	// 
	
	static private List<String> findStoragePaths()
	{
		logit(">> findStoragePaths, Finding storage areas");
		
		List<String> paths = new ArrayList<String>();
				
		File primaryStorage = Environment.getExternalStorageDirectory();		
		
		String primaryPath = primaryStorage.getAbsolutePath();
		
		logit("findStoragePaths, Primary Path: " +primaryPath);

		// if primary storage is removable then assume there are no more external storage areas
				
		if (Environment.isExternalStorageRemovable())
		{
			logit("findStoragePaths, Primary storage is removable");
			// Check if the removable storage can be read
			
			if (isPrimaryStorageReadable())
			{
				logit("findStoragePaths, Primary storage is removable AND readable");
				paths.add(primaryPath);
			}
			
			return paths;
		}
		
		// Add the internal primary storage to paths and look for secondary (i.e. truly external) storage
		
		paths.add(primaryStorage.getAbsolutePath());
		
		String secondaryPaths = System.getenv("SECONDARY_STORAGE"); // Majority of devices
		
		if (secondaryPaths == null) secondaryPaths = System.getenv("EXTERNAL_ADD_STORAGE");  // LG anyway
			
		//secondaryPaths = "/storage/extSdCard:/storage/UsbDriveA:/storage/UsbDriveB:/storage/UsbDriveC:/storage/UsbDriveD:/storage/UsbDriveE:/storage/UsbDriveF";
		
		logit("findStoragePaths, Secondary Storage Environment: " +secondaryPaths);
		
		if (secondaryPaths != null)
		{
			List<String> secondaryAreas = getStorageAreasFromList(secondaryPaths);
			
			paths.addAll(secondaryAreas);
			return paths;
		}
		
		// No secondary storage environment variable set
		// look for any secondary mounts the hard way
		
		List<String> secondaryMounts = getSecondaryStorageMounts();
		
		if (secondaryMounts != null)
		{
			paths.addAll(secondaryMounts);
		}

		return paths;
	}
	
	// ========================================================================
	// NOTE: This function must only be called on devices running 4.4.4
	// (API Level 19) or later.
	//
	// Android 4.4.4 introduced Context.getExternalFilesDirs() as a standard
	// means of finding the application's storage directories across all
	// "external" storage areas. However, this returns the sub-folder where the  
	// application has write access. We need the root of each of these storage
	// areas. The sub-folder part of the path is removed by comparing each of the
	// paths from getExternalFilesDir() with that returned by
	// Environment.getExternalStorageDirectory(). The latter contains the
	// root of the primary storage area. We assume that application's sub-folder  
	// is common across all storage areas.
	//
	// Note: From 4.4.4 Applications can only write to their designated sub
	// folder in any external storage area but have read access to all of it. 
	// ========================================================================
		
		static private List<String> getExternalPaths(Context aContext)
		{
			logit(">> getExternalPaths, Finding storage areas");			
			
			File primaryStorage = Environment.getExternalStorageDirectory();		
			
			String primaryPath = primaryStorage.getAbsolutePath();
		
			logit("getExternalPaths, Primary Path: " +primaryPath);
			
			List<String> paths = new ArrayList<String>();
			
			Context appContext = emGlobals.getmContext();

			File[] files = new File[0];  //>>> //TODO
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
				files = appContext.getExternalFilesDirs(null);
			}

			// See if we can find the primary external storage dir in the
			// list of all external storage dirs.
			// This is used to work out the common application sub-folder
			// section - which is then removed from the external dirs to
			// get the "root" of all the storage areas.

			String appSubFolder = null;

			for (File thisFile : files) {
				if (thisFile == null) continue;

				String thisPath = thisFile.getAbsolutePath();

				logit("getExternalPaths, Dir: " + thisPath);

				if (thisPath.startsWith(primaryPath)) {
					int primaryLength = primaryPath.length();

					if (primaryPath.length() < thisPath.length()) {
						appSubFolder = thisPath.substring(primaryLength);
						break;
					}
				}
			}

			logit("getExternalPaths, App Sub Folder: " + appSubFolder);

			if (appSubFolder == null) {
				warnit("getExternalPaths, No common application subfolder found!!!");

				appSubFolder = "/Android/data/";
			}

			for (File thisFile : files) {
				if (thisFile == null) continue;

				String thisPath = thisFile.getAbsolutePath();

				int subFolderStart = thisPath.indexOf(appSubFolder);

				if (subFolderStart != -1) {
					String storageRoot = thisPath.substring(0, subFolderStart);

					logit("getExternalPaths, Dir: " + thisPath + ", Root: " + storageRoot);

					paths.add(storageRoot);
				}
			}
				
			return paths;
		}

    static public String[] getSDCardStoragePaths(Context aContext, boolean aGetWritableApplicationSubfolder)
    {
        List<String> sdCardPaths = new ArrayList<String>();

        String paths[] = getStoragePaths(aContext);

        File primaryStorage = Environment.getExternalStorageDirectory();
        String primaryExternalStoragePath = primaryStorage.getAbsolutePath();
        boolean primaryStorageIsRemovable = Environment.isExternalStorageRemovable();

        for (String path : paths) {
            // If primary is not removable, and this path starts with the primany path then ignore it
            if (path.equalsIgnoreCase(primaryExternalStoragePath)) {
                if (primaryStorageIsRemovable) {
                    sdCardPaths.add(path);
                }
            }
            else {
                sdCardPaths.add(path); // This isn't the main storage, so assume it's removable storage
            }
        }

		if (aGetWritableApplicationSubfolder) {
			ArrayList<String> applicationWritablePaths = new ArrayList<>();
			for(String path : sdCardPaths) {
				String fixedPath = path + "/Android/data/" + aContext.getPackageName() + "/files";
				applicationWritablePaths.add(fixedPath);
			}
			sdCardPaths = applicationWritablePaths;
		}

        return sdCardPaths.toArray(new String[sdCardPaths.size()]);
    }
	
	static public String[] getStoragePaths(Context aContext)
	{
		List<String> paths;
		
                int deviceApiLevel = Build.VERSION.SDK_INT;
		
		logit(">> getStoragePaths, Device API Level: " +deviceApiLevel);
		
        if (deviceApiLevel >= Build.VERSION_CODES.KITKAT)  //>>> //TODO
        {
    		paths = getExternalPaths(aContext);
        }
        else
        {
    		paths = findStoragePaths();
        }		
		
		String[] pathsArray = new String[paths.size()];		
		
		pathsArray = paths.toArray(pathsArray);
		
		return pathsArray;
	}
	
    static private void traceit(String aText)
    {
        //Log.v(TAG, aText);
        DLog.log(aText);
    }

    static private void logit(String aText)
    {
        //// Log.d(TAG, aText);
        DLog.log(aText);
    }

    static private void warnit(String aText)
    {
        //Log.e(TAG, aText);
        DLog.log(aText);
    }

    static private void errorit(String aText)
    {
        //Log.e(TAG, aText);
        DLog.log(aText);
    }
}

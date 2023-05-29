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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Telephony;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.google.gson.Gson;
import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.R;
import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.DataBaseWork;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.models.ContentDetails;
import com.pervacio.wds.custom.models.MigrationStats;
import com.pervacio.wds.custom.service.TransactionLogService;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.PreferenceHelper;
import com.pervacio.wds.sdk.internal.CMDCryptoSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class EMUtility {

	static private Context mContext;
	static EMGlobals emGlobals = new EMGlobals();

	private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
	private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
	private static final String NUMBER = "0123456789";
	private static final String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
	private static SecureRandom random = new SecureRandom();
	
	static public void setContext(Context aContext)
	{
		mContext = aContext;
	}
	
	public static Context Context()
	{
		if (mContext == null) {
			mContext = emGlobals.getmContext();
		}
		return mContext;
	}
	
	public static String temporaryFileName() {
		String filepath = null;
		try {
			File tempDir = EMUtility.Context().getFilesDir();
			File tempFile = File.createTempFile("emtemp", "tmp", tempDir);
			filepath = tempFile.getPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			DLog.log(e);
		}		
		return filepath;
	}

	public static String createTempFile(String fileName) {
		String filepath = null;
		try {
			File myDir = new File(emGlobals.getmContext().getCacheDir(), "cache");
			myDir.mkdir();
			File tempFile = new File(myDir.getAbsoluteFile(), fileName);
			if (!tempFile.exists()) {
				tempFile.createNewFile();
			}
			filepath = tempFile.getPath();
		} catch (IOException e) {
			DLog.log(e);
		}
		return filepath;
	}


	public static String createTempFile2(String dir) {
		String filepath = null;
		try {
			File myDir = new File(dir, "cache");
			myDir.mkdir();
			File tempFile = new File(myDir.getAbsoluteFile(), "tempfile.txt");
			if (!tempFile.exists()) {
				tempFile.createNewFile();
			}
			filepath = tempFile.getPath();
		} catch (IOException e) {
			DLog.log(e);
		}
		return filepath;
	}



	// Only call this from the main thread
	public static void displayAlert(String aTitle, String aMessage) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
 
		// set title
		alertDialogBuilder.setTitle(aTitle); // TODO: internationalize this

		// set dialog message
		alertDialogBuilder
			.setMessage(aMessage)
			.setCancelable(false)
			.setPositiveButton(mContext.getString(R.string.ept_ok),new DialogInterface.OnClickListener() { // TODO: localize the OK
				public void onClick(DialogInterface dialog,int id) {
					// Ignore
				}
			  });

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

			// show it
		alertDialog.show();
	}
	
	public static boolean copyFile(File aSource, File aDestination) {
		boolean result = true;
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new FileInputStream(aSource);
			output = new FileOutputStream(aDestination);
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buffer)) > 0) {
				output.write(buffer, 0, bytesRead);
			}
		}
		catch (Exception ex) {
			result = false;
		}
		finally {
			try {
				input.close();
				output.close();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		
		return result;
	}

	public static boolean copyFileWithDecrypt(File aSource, File aDestination) {
		boolean result = true;
		InputStream input = null;
		OutputStream output = null;

		try {
			input = new FileInputStream(aSource);
			output = new FileOutputStream(aDestination);

			try {
				output = CMDCryptoSettings.getCipherDecryptOutputStream(output); // Decrypt when writing to the output file (if crypto is enabled)
			} catch (Exception e) {
				return false;
			}

			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buffer)) > 0) {
				output.write(buffer, 0, bytesRead);
			}
		}
		catch (Exception ex) {
			result = false;
		}
		finally {
			try {
				input.close();
				output.close();
			}
			catch (Exception ex) {
				// Ignore
			}
		}

		return result;
	}

	public static byte[] readFileToByteArray(File file) throws IOException {
		RandomAccessFile f = new RandomAccessFile(file, "r");
		try {
			long longlength = f.length();
			int length = (int) longlength;
			if (length != longlength)
				throw new IOException("File too large to read into buffer");
			byte[] data = new byte[length];
			f.readFully(data);
			return data;
		} finally {
			f.close();
		}
	}

	public static void writeByteArrayToFile(byte[] aBytes, File aFile) throws IOException {
		FileOutputStream output = new FileOutputStream(aFile);
		output.write(aBytes);
	}

	/* Attempts to find a relative path for a given full file path.
		For example the full path: /mnt/sdcard/photos/vacation2015/myimage.jpg
		Would return a relative path of /vacation2015/myimage.jpg
		The logic looks for file paths that are located in standard Android folders and returns a relative path relative to that
		If the files are not in standard folders then no relative path is returned
	 */
	static public String relativePathForFilePath(String aFilePath) {
		StringBuilder relativePathStringBuilder = new StringBuilder();
		List<String> standardAndroidFolderNames = new ArrayList<String>();

		addStandardFolderName(Environment.DIRECTORY_DCIM, standardAndroidFolderNames);

		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentApiVersion >= 19) {
			addStandardFolderName(Environment.DIRECTORY_DOCUMENTS, standardAndroidFolderNames);
		}

		addStandardFolderName(Environment.DIRECTORY_PICTURES, standardAndroidFolderNames);
		addStandardFolderName(Environment.DIRECTORY_DOWNLOADS, standardAndroidFolderNames);
		addStandardFolderName(Environment.DIRECTORY_MOVIES, standardAndroidFolderNames);
		addStandardFolderName(Environment.DIRECTORY_MUSIC, standardAndroidFolderNames);
		addStandardFolderName(Environment.DIRECTORY_PODCASTS, standardAndroidFolderNames);

		String[] pathComponents = aFilePath.split("/");

		// Check for a standard folder name as any part of the path, then make the relative path the remainder of that
		boolean standardFolderFound = false;
		List<String> pathComponentsAfterStandardFolder = new ArrayList<String>();
		for (String pathComponent : pathComponents) {
			if (pathComponent.isEmpty())
				continue;
			else if (standardFolderFound)
				pathComponentsAfterStandardFolder.add(pathComponent);
			else {
				for (String standardFolderName : standardAndroidFolderNames) {
					if (pathComponent.equalsIgnoreCase(standardFolderName))
						standardFolderFound = true;
				}
			}
		}

		if (pathComponentsAfterStandardFolder.size() > 0)
			pathComponentsAfterStandardFolder.remove(pathComponentsAfterStandardFolder.size() - 1); // Remove the last item because this is the file name

		for (String relativePathComponent: pathComponentsAfterStandardFolder) {
			relativePathStringBuilder.append(relativePathComponent);
			relativePathStringBuilder.append("/");
		}

		return relativePathStringBuilder.toString();
	}

	static private void addStandardFolderName(String aTypeString, List<String> aFolderNameList) {
		try {
			File path = Environment.getExternalStoragePublicDirectory(aTypeString);
			String fileName = path.getName();
			if (fileName != null)
				aFolderNameList.add(fileName);
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	// Checks if any item in aStringList occurs as a substring in aString
	static boolean itemInListIsSubstringInString(String aString,
												 List<String> aStringList) {
		if (aString == null)
			return false;

		for(String stringInList : aStringList) {
			if (aString.contains(stringInList))
				return true;
		}

		return false;
	}

	public static String createReferenceFileWithSendingDeviceInfo() {
		String referenceDataFilePath = null;
		EMXmlGenerator referenceDataGenerator = new EMXmlGenerator();
		try {
			referenceDataGenerator.startDocument();
			referenceDataGenerator.startElement(EMStringConsts.EM_XML_DEVICE_TYPE);
			referenceDataGenerator.writeText(Constants.PLATFORM);
			referenceDataGenerator.endElement(EMStringConsts.EM_XML_DEVICE_TYPE);
			referenceDataFilePath = referenceDataGenerator.endDocument();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return referenceDataFilePath;
	}

	public static void parseSendingDeviceInfo(byte[] aDeviceInfoBytes) throws XmlPullParserException, IOException {
		InputStream inputStream = new ByteArrayInputStream(aDeviceInfoBytes);
		parseSendingDeviceInfo(inputStream);
	}

	public static void parseSendingDeviceInfo(String aDeviceInfoFile) throws IOException, XmlPullParserException {
		File file = new File(aDeviceInfoFile);
		InputStream inputStream = new FileInputStream(file);
		parseSendingDeviceInfo(inputStream);
	}

	private static void parseSendingDeviceInfo(InputStream aInputStream) throws XmlPullParserException, IOException {
		DLog.log(">> parseSendingDeviceInfo");
		XmlPullParser xmlTextReader = Xml.newPullParser();
		xmlTextReader.setInput(new InputStreamReader(aInputStream));
		xmlTextReader.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

		int xmlNodeType = -1;
		boolean inDeviceTypeElement = false;
		String deviceTypeString = EMStringConsts.EM_DEVICE_TYPE_UNKNOWN;
		while (xmlTextReader.next() != XmlPullParser.END_DOCUMENT) {
			int eventType = xmlTextReader.getEventType();
			switch (eventType) {
				case XmlPullParser.START_TAG:
					String tagname = xmlTextReader.getName();
					if (tagname.equalsIgnoreCase(EMStringConsts.EM_XML_DEVICE_TYPE)) {
						inDeviceTypeElement = true;
					} else {
						inDeviceTypeElement = false;
					}
					break;

				case XmlPullParser.TEXT:
					String text = xmlTextReader.getText();
					if (inDeviceTypeElement) {
						deviceTypeString = text;
					}
					break;

				case XmlPullParser.END_TAG:
					inDeviceTypeElement = false;
					break;
			}
		}

		EMMigrateStatus.setSourceDeviceType(deviceTypeString);
		DLog.log("Source device was: " + deviceTypeString);

		DLog.log("<< parseSendingDeviceInfo");
	}

	public static int checkSelfPermission(Context aContext, String aPermission) {
		return PackageManager.PERMISSION_GRANTED;
	}

	public static boolean makePathForLocalFile(String aLocalFilePath) {
		String pathToCreate = "";
		String[] remotePathComponents = aLocalFilePath.split("/");
		for(int index = 0; index < remotePathComponents.length - 1; index++) {
			String pathComponent = remotePathComponents[index];
			if (pathComponent.isEmpty())
				continue;

			pathToCreate += "/" + pathComponent;
		}

		File pathFile = new File(pathToCreate);
		boolean mkPathResult = pathFile.mkdirs();

		boolean success = (mkPathResult || pathFile.isDirectory());

		return success;
	}

	@SuppressLint("NewApi")
	public synchronized static void bindSocketToWiFiNetwork(Socket aSocket) {
		try {
			boolean socketBound = false;
			if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
				Context context = EMUtility.Context();
				ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				Network[] network = connectivityManager.getAllNetworks();
				if ((network != null) && (network.length > 0)) {
					for (int networkIndex = 0 ; networkIndex < network.length ; networkIndex++) {
						if (!socketBound) {
							NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network[networkIndex]);
							int networkType = networkInfo.getType();
							if (ConnectivityManager.TYPE_WIFI == networkType){
								try {
									network[networkIndex].bindSocket(aSocket);
									socketBound = true;
								} catch (IOException e) {
									// Nothing we can do here, just leave socketBound as false
									// Maybe the socket is already connected, and we're trying to bind it again
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			DLog.log("Exception in bindSocketToWiFiNetwork "+e.getMessage());
		}
	}

	// From Stack Overflow
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len/2];

		for(int i = 0; i < len; i+=2){
			data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
		}

		return data;
	}

	// From Stack Overflow
	final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	public static String byteArrayToHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length*2];
		int v;

		for(int j=0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j*2] = hexArray[v>>>4];
			hexChars[j*2 + 1] = hexArray[v & 0x0F];
		}

		return new String(hexChars);
	}

	// From Stack Overflow
	public static String md5(String s)
	{
		MessageDigest digest;
		try
		{
			digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes(Charset.forName("US-ASCII")),0,s.length());
			byte[] magnitude = digest.digest();
			BigInteger bi = new BigInteger(1, magnitude);
			String hash = String.format("%0" + (magnitude.length << 1) + "x", bi);
			return hash;
		}
		catch (Exception e)
		{
			DLog.log("Exception in generating md5");
		}
		return "";
	}

	public static boolean isNullOrEmpty(String str) {
		if (str == null || "".equalsIgnoreCase(str) || "null".equalsIgnoreCase(str)) {
			return true;
		} else {
			return false;
		}
	}

	public static void createWorkerForDBUpdate() {
		DLog.log("<--createWorkerForDBUpdate");
		Data data = TransactionLogService.getDataForServiceCall();

		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		final OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(DataBaseWork.class)
				.setInputData(data)
				.setConstraints(constraints)
				.addTag("simple_db_work")
				.build();
		WorkManager.getInstance().enqueue(simpleRequest);
		DLog.log("createWorkerForDBUpdate-->");
	}

	public static String readableFileSize(long size) {
		String readableSize="";
		if(size <= 0) return readableSize;
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
		DecimalFormat formatter = new DecimalFormat("#,##0.##");
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		formatter.setDecimalFormatSymbols(symbols);
		readableSize=formatter.format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
		return readableSize;
	}

	static Object lock = null;

	public static Object getLockObject() {
		if (lock == null) {
			lock = new Object();
		}
		return lock;
	}

    public static void makeActionOverflowMenuShown(Activity activity) {
        try {
            ViewConfiguration config = ViewConfiguration.get(activity);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            // Log.d("", e.getLocalizedMessage());
        }
          /*Method is used to enable the overflow icon color if it is below API level 19*/
        setOverflowButtonColor(activity);
    }

    public static void setOverflowButtonColor(final Activity activity) {
        final String overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<View>();
                decorView.findViewsWithText(outViews, overflowDescription,
                        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) {
                    return;
                }
                AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
//                if (BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT)) {
//                    overflow.setColorFilter(Color.BLACK);
//                } else {
				overflow.setColorFilter(Color.WHITE);
//                }
            }
        });
    }

	public static byte[] gzip(String s) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(bos);
			OutputStreamWriter osw = new OutputStreamWriter(gzip, Charset.forName("UTF-8"));
			osw.write(s);
			osw.close();
			byte[] bytes = bos.toByteArray();
			// String hexcodes=Hex.encodeHexString( bytes ) ;
			return bytes;
		} catch (Exception e) {
			DLog.log( "GZip Exception" + e.getMessage());
		}
		return null;
	}

	public static void copyTextToClipboard(String lable,String text){
		ClipboardManager clipboard = (ClipboardManager) mContext.getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText(lable, text);
		clipboard.setPrimaryClip(clip);
	}

	public static int getRandomSecurePIN(){
		SecureRandom secureRandom = new SecureRandom();
		float randomFloat = secureRandom.nextFloat();
		float pinFloat = randomFloat * 9999;
		return  (int) pinFloat;
	}

	public static SpannableStringBuilder getSpannableText(String text, int lowerLimit, int upperLimit) {
		final SpannableStringBuilder sb = new SpannableStringBuilder(text);

// Span to set text color to some RGB value
		final ForegroundColorSpan fcs = new ForegroundColorSpan(Color.rgb(0, 158, 158));

// Span to make text bold
		final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);

// Set the text color for first 4 characters
		sb.setSpan(fcs, lowerLimit, upperLimit, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

// make them also bold
		sb.setSpan(bss, 0, text.length() - 4, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		return sb;
	}


	public static String getReadableTime(Context mContext,long timeInMillis,boolean roundToMin){
		StringBuilder readableTime = new StringBuilder();
		try {
			if(roundToMin){
                timeInMillis = roundUp(timeInMillis,Constants.ONE_MINUTE); //Rounding to next Minute (60 sec)
            }
			int seconds = (int) (timeInMillis / 1000) % 60;
			int minutes = (int) ((timeInMillis / (1000 * 60)) % 60);
			int hours = (int) ((timeInMillis / (1000 * 60 * 60)) % 24);
			int days = (int) (timeInMillis / (1000 * 60 * 60)) / 24;
			hours = hours + days * 24; //Converting days to hrs(displays time only in hrs)
			if (hours != 0) {
                readableTime.append(hours + " "+ mContext.getString(R.string.hour)+" ");
            }
			if (minutes != 0) {
                readableTime.append(minutes +" "+mContext.getString(R.string.min)+" ");
            }
			if (seconds != 0) {
                readableTime.append(seconds + " "+ mContext.getString(R.string.sec));
            }
		} catch (Exception e) {
			DLog.log(e);
		}
		return readableTime.toString();
	}


	/** round n up to nearest multiple of m */
	public static long roundUp(long n, long m) {
		return n >= 0 ? ((n + m - 1) / m) * m : (n / m) * m;
	}

	public static boolean changeWifiState(boolean state) {
		DLog.log("Changing wifistate : " + state);
		boolean isStateChanged = false;
		try {
			WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
			if (wifiManager != null) {
				boolean currentState = wifiManager.isWifiEnabled();
				if (currentState != state) {
					isStateChanged = wifiManager.setWifiEnabled(state);
				}
			}
			if (state && isStateChanged) {
				wifiManager.startScan();
				wifiManager.reconnect();
			}
		} catch (Exception e) {
			DLog.log(e.getMessage());
		}
		return isStateChanged;
	}

	//compares two strings by eliminating special characters (for BB)

	public static boolean isStringMatches(String s1, String s2) {
		boolean isEquals = false;
        try {
            if (s1 != null && s2 != null) {
                if (s1.equals(s2)) {
                    isEquals = true;
                } else if (s1.length() == s2.length()) {
                    s1 = s1.replaceAll("[^a-zA-Z0-9]", "");
                    s2 = s2.replaceAll("[^a-zA-Z0-9]", "");
                    if (s1.equals(s2)) {
                        isEquals = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isEquals;
	}

    public static boolean isJSONValid(String test) {
        boolean isJSONValid = true;
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // if in case JSONArray is valid...
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                isJSONValid = false;
            }
        }
        return isJSONValid;
    }


	/**
	 * Method used to updating the port number
	 *
	 * @return Avaialble port number
	 */
	public static int getAvailblePort(int port, boolean dataTransferport) {
		DLog.log("checking port number : " + port);
		int numOfAttempts = 0;
		try {
			while (isLocalPortInUse(port)) {
				if (numOfAttempts == 10) {
					break;
				}
				if (numOfAttempts == 0)
					port = dataTransferport ? 51263 : 52764;      //Taking some random port number
				else
					port = port + (dataTransferport ? 146 : 135); //Incrementing with some random number
				++numOfAttempts;
				DLog.log("Changed port : " + port + ", no.of attempts : " + numOfAttempts);
			}
			if (isLocalPortInUse(port)) {
				port = getOpenport();
			}
		} catch (Exception e) {
			DLog.log("Exception in getAvailblePort : " + e.getMessage());
		}
		DLog.log("available port : " + port);
		return port;
	}

	/**
	 * @param port Given port number
	 * @return true, if Port is using, else false.
	 */
	public static boolean isLocalPortInUse(int port) {
		boolean portInUse = false;
		try {
			// ServerSocket try to open a LOCAL port
			new ServerSocket(port).close();
			// local port can be opened, it's available
		} catch (IOException e) {
			DLog.log("Exception while creating the server,Port is using : " + port);
			// local port cannot be opened, it's in use
			portInUse = true;
		} catch (Exception e) {
			DLog.log("Exception while creating the serer on port : " + port+ " "+ e.getMessage());
		}
		return portInUse;
	}

	public static int getOpenport() {
		int openPort = 0;
		try {
			ServerSocket serverSocket = new ServerSocket(0);
			openPort = serverSocket.getLocalPort();
			DLog.log("server established on  : " + openPort);
			serverSocket.close();
		} catch (IOException e) {
			DLog.log("Exception while creating the server with open port");
			// local port cannot be opened, it's in use
		} catch (Exception e) {
            DLog.log("Exception while creating the serer with open port ");
        }
		return openPort;
	}

	public static void checkforProblematicModel() {
		String problematicModel = null;
		if (!DeviceInfo.getInstance().isP2PSupported()) {
			problematicModel = Build.MODEL;
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !DeviceInfo.getInstance().isCarrierModel("sprint")) {
			if ((Build.MODEL.contains("SM-A500") || Build.MODEL.contains("SM-A510") || Build.MODEL.contains("SM-A520")) && (Build.MODEL.endsWith("F") || Build.MODEL.endsWith("FN"))) { // Adding samsung A5 2015, 2016 and 2017 all Europe specific models.
				DLog.log("Samsung A5 >>> Model added to problematic list : " + Build.MODEL);
				problematicModel = Build.MODEL;
			} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N && DeviceInfo.getInstance().get_make().equalsIgnoreCase("samsung") && (Build.MODEL.endsWith("F") || Build.MODEL.endsWith("FN"))) { //Europe models ends with F or FN.
				DLog.log("Samsung Nougat >>> Model added to problematic list : " + Build.MODEL);
				problematicModel = Build.MODEL;
			}
		}
		if (problematicModel != null) {
			if (!Constants.WIFI_PROBLAMATIC_MODELS.contains(problematicModel)) {
				Constants.WIFI_PROBLAMATIC_MODELS.add(problematicModel);
			}
			if (!Constants.WIFI_DIRECT_PROBLAMATIC_MODELS.contains(problematicModel)) {
				Constants.WIFI_DIRECT_PROBLAMATIC_MODELS.add(problematicModel);
			}
		}
		//Removing from list, as hotspot is not handled for Oreo yet.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < 30) {
			Constants.WIFI_PROBLAMATIC_MODELS.remove(Build.MODEL);
			Constants.WIFI_DIRECT_PROBLAMATIC_MODELS.remove(Build.MODEL);
		}
        if ("KYOCERA".equalsIgnoreCase(DeviceInfo.getInstance().get_make())) {
            Constants.SMS_BULK_INSERTION_PROBLAMATIC_MODELS.add(Build.MODEL);
        }
        if (Constants.P2P_MODELS.contains("all") || FeatureConfig.getInstance().getDeviceConfig().isEnableWifiP2P()) {
            Constants.P2P_MODELS.add(Build.MODEL);
        }
        if (FeatureConfig.getInstance().getDeviceConfig().isHasWifiDirectProblem() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Constants.WIFI_PROBLAMATIC_MODELS.add(Build.MODEL);
            Constants.WIFI_DIRECT_PROBLAMATIC_MODELS.add(Build.MODEL);
        }
        if (FeatureConfig.getInstance().getDeviceConfig().isHasBulkSMSInsertionProblem()) {
            Constants.SMS_BULK_INSERTION_PROBLAMATIC_MODELS.add(Build.MODEL);
        }
	}


	public static boolean checkVariation(long l1, long l2, int variationPercent) {
		boolean isVaried = false;
		try {
			long difference = Math.abs(l1 - l2);
			if (difference > (l1 * (variationPercent / 100)))
				isVaried = true;
		} catch (Exception e) {
			DLog.log(e.getMessage());
		}
		return isVaried;
	}

    public static long getMean(List<Long> list) {
        Long mean = 0L;
        try {
            if (!list.isEmpty()) {
                long sum = 0;
                for (Long mark : list) {
                    sum += mark;
                }
                return sum / list.size();
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
        return mean;
    }

	/**
	 * This method to get the difference between two different timezones.
	 * <p>
	 * timezone1 - timezone as string
	 * timezone2 - timezone as string
	 *
	 * @param timeZone1
	 * @param timeZone2
	 * @return - long - time in milliseconds
	 */

	public static long getTimeZoneDiff(String timeZone1, String timeZone2) {
		TimeZone tz1 = TimeZone.getTimeZone(timeZone1);
		TimeZone tz2 = TimeZone.getTimeZone(timeZone2);
		return tz1.getRawOffset() - tz2.getRawOffset() + tz1.getDSTSavings() - tz2.getDSTSavings();
	}

	public static String readFile(String filePath) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				sb.append(sCurrentLine);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static String readPropertyFile() {
		String totalString = readFile(Constants.PROPERTY_FILE_PATH);
		String language = "";
		try {
			language = totalString.split("=")[1];
		} catch (Exception e) {
			DLog.log("Exception in getting language");
		}
		return language.trim();
	}

	public static String getDateFromMillis(long milliSeconds) {
		String dateString = "";
		try {
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(milliSeconds);
			System.out.println(formatter.format(calendar.getTime()));
			dateString = formatter.format(calendar.getTime());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dateString;
	}

	public static long getMillisFromDate(String dateString) {
		Date date;
		long millis = 0;
		try {
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			date = formatter.parse(dateString);
			millis = date.getTime();
			System.out.println(millis);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return millis;
	}


	public static int getCompanyID() {
		int companyId = 0;
		if (Constants.COMPANY_NAME.equalsIgnoreCase(Constants.COMPANY_SPRINT)) { //Adding company id for sprint(will remove this once we implement feature config)
			companyId = BuildConfig.DEBUG ? 3 : 12;
		} else if (Constants.COMPANY_NAME.equalsIgnoreCase(Constants.COMPANY_MOBILECOPY)) { //Adding company id for Mobilecopy(will remove this once we implement feature config)
			companyId = 28;
		}
		return companyId;
	}

	public static String getRange(long timeinmillis,Context context){
		try {
			StringBuilder range = new StringBuilder();
			long doubleRange = 2 * roundUp(timeinmillis, 60 * 1000L);
			range.append(getReadableTime(context,timeinmillis, true));
			range.append("-");
			range.append(getReadableTime(context,doubleRange, true));
			return range.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public static boolean shouldBindSocketToWifi() {
		boolean shouldBind = true;
		if (CommonUtil.getInstance().isGroupOwner() || Constants.mTransferMode.equalsIgnoreCase(Constants.P2P_MODE)) {
			shouldBind = false;
		}
		return shouldBind;
	}
	public static String authToken = null;

	public static String getAuthToken() {
		DLog.log("getAuthToken... " );
		if(authToken == null || "".equalsIgnoreCase(authToken)) {
			DLog.log("getAuthToken is null or empty... " );
			byte[] data = new byte[0];
			try {
				data = (Constants.HTTP_AUTHENTICATION_USERNAME + ":" + Constants.HTTP_AUTHENTICATION_PASSWORD).getBytes("UTF-8");
				authToken = Base64.encodeToString(data, Base64.NO_WRAP);
				DLog.log("In getAuthToken Old Auth: " + authToken);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return "Basic " + authToken;
	}
	public static void setAuthToken(String token) {
		DLog.log("In setAuthToken New Auth: " + token);
		authToken = token;
	}
	public static void saveMigrationStats(LinkedHashMap<Integer, ContentDetails> contentDetails) {
		DLog.log("<---saveMigrationStats");
		if (contentDetails != null)
			MigrationStats.getInstance().setContentDetailsMap(contentDetails);
		MigrationStats.getInstance().setMigrateStatus(EMMigrateStatus.mInstance);
		MigrationStats.getInstance().seteDeviceSwitchSession(new Gson().toJson(DashboardLog.getInstance().geteDeviceSwitchSession()));
		PreferenceHelper.getInstance(mContext).putStringItem(Constants.MIGRATION_STATS, new Gson().toJson(MigrationStats.getInstance()));
		DLog.log("saveMigrationStats-->");
	}

	public static MigrationStats getLastTransactionDetails() {
		MigrationStats migrationStats = null;
		try {
			String migrationDetails = PreferenceHelper.getInstance(emGlobals.getmContext()).getStringItem(Constants.MIGRATION_STATS);
			migrationStats = new Gson().fromJson(migrationDetails, MigrationStats.class);
		} catch (Exception ex) {

		}
		return migrationStats;
	}

	public static void clearTransaction() {
		DLog.log("--clearTransaction--");
		MigrationStats.setMigrationStats(null);
		PreferenceHelper.getInstance(emGlobals.getmContext()).putStringItem(Constants.MIGRATION_STATS, null);
	}

	public static void updateSupportingDataTypes() {
		String supportedTypes = FeatureConfig.getInstance().getProductConfig().getSupportedDatatypes();
		if (!TextUtils.isEmpty(supportedTypes)) {
			List<String> dataTypes = Arrays.asList(supportedTypes.split(","));
			for (Integer dataType : Constants.SUPPORTED_DATATYPE_MAP.keySet()) {
				if (dataTypes.contains(Constants.DATATYPE_VALUES.get(dataType))) {
					Constants.SUPPORTED_DATATYPE_MAP.put(dataType, true);
				} else {
					Constants.SUPPORTED_DATATYPE_MAP.put(dataType, false);
				}
			}
		}
	}

	public static boolean isDefaultSMSApp() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(emGlobals.getmContext());
			String thisPackage = emGlobals.getmContext().getPackageName();
			return defaultSmsApp.equals(thisPackage);
		}
		return true;
	}

    public static String getDevicesCombinationDetails() {
        StringBuilder deviceDetails = new StringBuilder();
        try {
            if (DashboardLog.getInstance().sourceEMDeviceInfo != null) {
                deviceDetails.append(DashboardLog.getInstance().sourceEMDeviceInfo.dbDeviceMake + "_" + DashboardLog.getInstance().sourceEMDeviceInfo.dbDeviceModel + "_To_");
            } else if (!DashboardLog.getInstance().isThisDest()) {
                deviceDetails.append(Build.MANUFACTURER + "_" + Build.MODEL + " ");
            }
            if (DashboardLog.getInstance().destinationEMDeviceInfo != null) {
                deviceDetails.append(DashboardLog.getInstance().destinationEMDeviceInfo.dbDeviceMake + "_" + DashboardLog.getInstance().destinationEMDeviceInfo.dbDeviceModel);
            } else if (DashboardLog.getInstance().isThisDest()) {
                deviceDetails.append(Build.MANUFACTURER + "_" + Build.MODEL + " ");
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
            deviceDetails.append(Build.MANUFACTURER + "_" + Build.MODEL + " ");
        }
        return deviceDetails.toString();
    }


	public static String generateRandomString(int length) {
		if (length < 1) throw new IllegalArgumentException();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			// 0-62 (exclusive), random returns 0-61
			int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
			char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);
			// debug
			System.out.format("%d\t:\t%c%n", rndCharAt, rndChar);
			sb.append(rndChar);
		}
		return sb.toString();
	}

}

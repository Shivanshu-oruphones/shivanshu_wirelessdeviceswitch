package com.pervacio.wds.custom.asynctask;

import android.preference.PreferenceManager;

import com.pervacio.crashreportlib.LogReporting;
import com.pervacio.vcard.Base64;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.service.TransactionLogService;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility to upload file to server using multi-part form data.
 *
 * Server call will happen in Thread so it won't block any other operation.
 *
 * Usage: uploadLogToServer(File fileToBeUploaded, String transactionRefNumber)
 *
 * Rest of the method are documented inline, please refer to them.
 *
 * Created by Darpan Dodiya on 17-Aug-17.
 */


public class MultipartUtility {
    static EMGlobals emGlobals = new EMGlobals();
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private OutputStream outputStream;
    private PrintWriter writer;

    private String requestURL;
    private String basicAuth;
    private String sessionId;
    private String mRole;
    private String deviceDetails;

    private boolean serverResult = false;

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL URL Endpoint
     * @param basicAuth  Credentials in username:password format. Pass null to ignore.
     * @throws IOException
     */
    public MultipartUtility(String requestURL, String basicAuth, String[] details) {

        this.requestURL = requestURL;
        this.basicAuth = basicAuth;
        this.mRole = details[0];
        this.deviceDetails = details[1];
        this.sessionId = details[2];

        DLog.log("In MultipartUtility with session ID: " + sessionId);

        // Creates a unique boundary based on time stamp
        boundary = "---" + System.currentTimeMillis() + "---";
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                .append(LINE_FEED);
        writer.append("Content-Type: text/plain;").append(
                LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName  name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        FileInputStream inputStream = null;

        try {
            String fileName = uploadFile.getName();

            //Set boundary
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + fieldName
                            + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(fileName))
                    .append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            //Get local file's input stream and write to connection's output stream
            inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            writer.append(LINE_FEED);
            writer.flush();
        } catch (Exception ex) {
            DLog.log(ex);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
     * Adds a header field to the request.
     *
     * @param name  - name of the header field
     * @param value - value of the header field
     */
    public void addHeaderField(String name, String value) {
        writer.append(name + ": " + value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<String>();
        writer.append(LINE_FEED).flush();
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();

        // Checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            DLog.log("Server returned OK status: " + status);
            //Get local file's input stream
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            try {
                if (!response.isEmpty() && response.get(0) != null) {
                    JSONObject jsonObj = new JSONObject(response.get(0));
                    int code = jsonObj.getInt("code");
                    if (code == 0) {
                        serverResult = true;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            reader.close();
            httpConn.disconnect();

            //200 OK implies that the transaction was successful
        } else {
            DLog.log("Server returned non-OK status: " + status);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getErrorStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        }
        return response;
    }

    /*
     * Reference: https://stackoverflow.com/questions/6683600/zip-compress-a-folder-full-of-files-on-android
     *
     * Zips a file at a location and places the resulting zip file at the toLocation
     * Example: zipFileAtPath("downloads/myfolder", "downloads/myFolder.zip");
     */

    private static boolean zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;

        File sourceFile = new File(sourcePath);
        DLog.log(sourceFile + ", size : " + sourceFile.length());
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));

            byte data[] = new byte[BUFFER];
            FileInputStream fi = new FileInputStream(sourcePath);
            origin = new BufferedInputStream(fi, BUFFER);
            ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
     * gets the last path component
     *
     * Example: getLastPathComponent("downloads/example/fileToZip");
     * Result: "fileToZip"
     */
    private static String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        return lastPathComponent;
    }

    public void uploadLogToServer(final File file){
        new Thread(){
            @Override
            public void run() {
                super.run();
                DLog.log("Enter uploadLogToServer calling uploadLogFileToServer");
                uploadLogFileToServer(file);
            }
        }.start();
    }


    public boolean uploadLogFileToServer(final File fileToUpload) {
        DLog.log("Enter uploadLogFileToServer ");
        try {
            if (NetworkUtil.isInternetAvailable()) {
                DLog.log("In MultipartUtility with Server URL: " + requestURL);
                DLog.log("In MultipartUtility Old Auth: " + new String(new Base64().encode(basicAuth.getBytes())));
                URL url = new URL(requestURL);
                httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setUseCaches(false);
                httpConn.setDoOutput(true);    // Indicates POST method
                httpConn.setDoInput(true);
                httpConn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);

                //Set Basic authentication
/*                if (basicAuth != null) {
                    httpConn.setRequestProperty("Authorization", "Basic " + new String(new Base64().encode(basicAuth.getBytes())));
                }*/

                //Set Basic authentication
                if (EMUtility.getAuthToken() != null && !"".equalsIgnoreCase(EMUtility.getAuthToken())) {
                    //httpConn.setRequestProperty("Authorization", "Basic " + new String(new Base64().encode(basicAuth.getBytes())));
                    httpConn.setRequestProperty("Authorization", EMUtility.getAuthToken());
                } else {
                    httpConn.setRequestProperty("Authorization", "Basic " + new String(new Base64().encode(basicAuth.getBytes())));
                }
                //Get output stream
                outputStream = httpConn.getOutputStream();
                writer = new PrintWriter(new OutputStreamWriter(outputStream), true);

                //Add parameters value
                addFormField("fileType", mRole);

                //Set some transaction reference number if its null. It will be Device Identifier + Current Timestamp
//                String identifier = sessionId == null ? DeviceInfo.getInstance().getSingleIMEI() : sessionId;
                String identifier = sessionId == null ? DeviceInfo.getInstance().get_imei() : sessionId;
                String timeStamp = new SimpleDateFormat("hh:mm:ss").format(new Date());
                String fileName = identifier + "_" + deviceDetails + "_" + timeStamp;
                fileName = fileName.replaceAll("\\s", "");
                String company = PreferenceManager.getDefaultSharedPreferences(emGlobals.getmContext()).getString(LogReporting.COMPANY_NAME, Constants.COMPANY_NAME);

                addFormField("txRefNum", fileName);
                addFormField("company", company);
                addFormField("fileFormat", "zip");
                addFormField("product", "wds");

                String zipFilePath = fileToUpload.getParent() + "/log.zip";

                //Zip log file
                zipFileAtPath(fileToUpload.getAbsolutePath(), zipFilePath);

                //Add file(s) to upload
                addFilePart("file", new File(zipFilePath));

                //Collect response
                List<String> response = finish();
                StringBuilder entireResponse = new StringBuilder();

                for (String line : response) {
                    entireResponse.append(line);
                }
                DLog.log("uploading file : " + company + "/" + mRole + "_" + fileName);
                DLog.log("Upload Files Response:::" + entireResponse.toString());

                //Once you get the 200 OK response, delete both log.zip and log.txt
                if (serverResult) {
                    new File(zipFilePath).delete();
                    fileToUpload.delete();
                } else if (entireResponse.toString().contains("<body>404 - Not Found</body>")) {
                    //some times we are getting body not found exception,in this case no need to upload file again
                    serverResult = true;
                    DLog.log("body not found exception came,no need to upload file again");
                }
            } else {
                DLog.log("Network not available. Not uploading file.");
            }

        } catch (IOException ioe) {
            DLog.log(ioe);
        }

        //If it's called from service then send call back upon getting response
        if (isBackgroundCall && tlsCallBack != null) {
            tlsCallBack.sendCallBack(TransactionLogService.CALLBACK_TYPE.S3_LOG, serverResult);
        }
        return serverResult;
    }

    private boolean isBackgroundCall;

    public void setIsBackgroundCall(boolean isBackgroundCall) {
        this.isBackgroundCall = isBackgroundCall;
    }

    TransactionLogService tlsCallBack = null;

    public void setTlsCallBack(TransactionLogService tlsCallBack) {
        this.tlsCallBack = tlsCallBack;
    }
}

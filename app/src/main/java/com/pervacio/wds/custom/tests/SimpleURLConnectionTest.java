package com.pervacio.wds.custom.tests;

import com.pervacio.vcard.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Supporting class for LoggingAPITest.
 *
 * Created by: Darpan Dodiya on 5/24/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

public class SimpleURLConnectionTest {

    public static String doInBackground(String[] strings) {

        String server_response;
        URL url;
        HttpURLConnection urlConnection = null;
        String userCredentials = "appstoreuser:$ecr3T";
        String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));

        try {
            url = new URL(strings[0]);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setConnectTimeout(30000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty ("Authorization", basicAuth);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.write(strings[1].getBytes());

            int responseCode = urlConnection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                server_response = readStream(urlConnection.getInputStream());
                System.out.println("Response: " + server_response);
                LoggingAPITest.callA(server_response);
                //Log.v("CatalogClient", server_response);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Converting InputStream to String
    private static String readStream(InputStream in) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        try {
            while ((length = in.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
        } catch (IOException e) {
        }
        try {
            return baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return baos.toString();
        }
    }
}

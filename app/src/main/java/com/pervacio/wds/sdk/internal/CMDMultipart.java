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

import android.util.Log;

import com.pervacio.wds.app.EMConfig;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

public class CMDMultipart {

    private class CMDMultipartPart {
        String mContentType;
        byte[] mBytes; // Set if we're sending a byte array
        File mFile; // Set if we're sending a file
        boolean mEncrypt;
    }

    public String getBoundary() {
        return mBoundary;
    }
    private static final String TAG = "TAG";

    public void addBody(byte[] aBytes,
                    String aContentType,
                    boolean aEncrypt) {
        CMDMultipartPart part = new CMDMultipartPart();
        part.mBytes = aBytes;
        part.mEncrypt = aEncrypt;
        part.mContentType = aContentType;
        mPartList.add(part);
    }

    /*
    public void addBody(InputStream aStream,
                            String aContentType,
                            boolean aEncrypt) {
        CMDMultipartPart part = new CMDMultipartPart();
        part.mInputStream = aStream;
        part.mContentType = aContentType;
//        part.mAutoClose = aAutoClose;
        part.mEncrypt = aEncrypt;
        mPartList.add(part);
    }
    */

    public void addBody(File aFile,
                        String aContentType,
                        boolean aEncrypt) throws FileNotFoundException {
//        FileInputStream fileInputStream = new FileInputStream(aFile);
//        addBody(fileInputStream, aContentType, aEncrypt);
        CMDMultipartPart part = new CMDMultipartPart();
        part.mFile = aFile;
        part.mEncrypt = aEncrypt;
        part.mContentType = aContentType;
        mPartList.add(part);
    }

    private void writeHeaderToOutputStream(OutputStream aOutputStream,
                                            String aHeaderName,
                                            String aHeaderValue) throws IOException {
        String headerLine = aHeaderName + ": " + aHeaderValue;
        writeLineToOutputStream(aOutputStream, headerLine);
    }

    private void writeLineToOutputStream(OutputStream aOutputStream,
                                            String aLine) throws IOException {
        String linePlusCrLf = aLine + "\r\n";
        aOutputStream.write(linePlusCrLf.getBytes("UTF-8"));
    }

    private class NonCloseableOutputStream extends FilterOutputStream {

        public NonCloseableOutputStream(OutputStream aOutputStream) {
            super(aOutputStream);
        }

        @Override
        public void close() throws IOException {
            // We don't want to close the underlying stream
        }
    }

    public void writeToOutputStream(OutputStream aOutputStream,
                                    EMProgressHandler aProgressHandler,
                                    CMDCopyFileProgressDelegate aCopyFileProgressDelegate,
                                    boolean aPreviouslyPaused) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, InvalidAlgorithmParameterException { // aCleanup: Close all input streams and frees resources


        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(aOutputStream, EMConfig.EM_GOOGLE_DRIVE_WRITE_BUFFER_SIZE);

        boolean firstPart = true;
        for(CMDMultipartPart part : mPartList) {
            OutputStream outputStream = bufferedOutputStream;

            CipherOutputStream cipherOutputStream = null;
            if (part.mEncrypt) {
                NonCloseableOutputStream nonCloseableOutputStream = new NonCloseableOutputStream(bufferedOutputStream);
                cipherOutputStream = CMDCryptoSettings.getCipherEncryptOutputStream(nonCloseableOutputStream); // We use a non closable stream otherwise closing the cipher stream closes the underlying stream and we can't write any more data to it
            }

            InputStream inputStream = null;
            if (part.mFile != null) {
                inputStream = new FileInputStream(part.mFile);
            }
            else if (part.mBytes != null) {
                inputStream = new ByteArrayInputStream(part.mBytes);
            }

            // Write the part boundary and header
            String partBoundary = "";
            if (!firstPart) { // Don't add the CRLF before the first boundary
                partBoundary += "\r\n";
            }
            firstPart = false;
            partBoundary  += "--" + mBoundary; // Non-final boundary
            writeLineToOutputStream(outputStream, partBoundary);
            writeHeaderToOutputStream(outputStream, "Content-Type", part.mContentType);
            writeLineToOutputStream(outputStream, ""); //spacer line

            // Write the body data
            byte[] buffer = new byte[EMConfig.EM_GOOGLE_DRIVE_WRITE_BUFFER_SIZE];
            int len = inputStream.read(buffer);
            OutputStream payloadDataOutputStream = outputStream;

            if (cipherOutputStream != null)
                payloadDataOutputStream = cipherOutputStream;

            long totalDataTransferred = 0;

            // Log.d(TAG, "+++ writeToOutputStream, Sending data");

            while (len != -1) {
                payloadDataOutputStream.write(buffer, 0, len);

                if (aPreviouslyPaused) {
                    // Log.d(TAG, "unpausing ");
                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_RESUMED;
                    aProgressHandler.progressUpdate(progressInfo);
                    aPreviouslyPaused = false;
                }

                if (aCopyFileProgressDelegate != null)
                {
                    totalDataTransferred += len;
                    aCopyFileProgressDelegate.onCopyFileProgress(totalDataTransferred);
                }

                len = inputStream.read(buffer);
            }

            if (cipherOutputStream != null) {
                cipherOutputStream.flush();
                cipherOutputStream.close();
            }

            if (inputStream != null)
                inputStream.close(); // TODO: this could potentially be skipped in the event of any exceptions in the above code
        }

        String endPartBoundary = "\r\n" + "--" + mBoundary + "--";
        writeLineToOutputStream(bufferedOutputStream, endPartBoundary);

        bufferedOutputStream.flush();
        bufferedOutputStream.close();
    }

    private List<CMDMultipartPart> mPartList = new ArrayList<CMDMultipartPart>();
    private String mBoundary = new String("HOME_BOUNDARY_382746287");
}

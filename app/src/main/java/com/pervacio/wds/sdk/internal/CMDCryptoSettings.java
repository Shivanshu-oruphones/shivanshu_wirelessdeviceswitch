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

import android.os.Build;

import com.pervacio.wds.app.CPbkdf2;
import com.pervacio.wds.app.EMUtility;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.Normalizer;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

// A singleton for managing passwords, keys, generation of ciphers, etc.
// Call generateSalt or setSalt
// Call setPassword
// Then call getCipherDecryptOutputStream or getCipherEncryptOutputStream
public class CMDCryptoSettings {

    private static CMDCryptoSettings mInstance;
    private final static int PBKDF2_ITERATIONS = 1000;
    private final static int KEY_LENGTH = 256;

    public static void initialize() {
        mInstance = new CMDCryptoSettings();
    }

    static public byte[] generateSalt() {
        return mInstance.generateSaltInstance();
    }

    private byte[] generateSaltInstance() {
        SecureRandom sr = new SecureRandom();
        mSalt = new byte[16];
        sr.nextBytes(mSalt);
        return mSalt;
    }

    public static byte[] getSalt() {
        return mInstance.getSaltInstance();
    }

    private byte[] getSaltInstance() {
        return mSalt;
    }

    static public void setSalt(byte[] aSalt) {
        mInstance.setSaltInstance(aSalt);
    }

    public void setSaltInstance(byte[] aSalt) {
        mSalt = aSalt;
    }

    static public void setPassword(String aPassword) {
        mInstance.setPasswordInstance(aPassword);
    }

    public void setPasswordInstance(String aPassword) {
        mPassword = aPassword;
    }

    static public String getPassword() {
        return mInstance.getPasswordInstance();
    }

    public String getPasswordInstance() {
        return mPassword;
    }

    public static boolean passwordsAreEqual(String aPassword1,
                                                String aPassword2) {
        return mInstance.passwordsAreEqualInstance(aPassword1, aPassword2);
    }

    private boolean passwordsAreEqualInstance(String aPassword1,
                                                String aPassword2) {
        String normalizedPasswordString1 = Normalizer.normalize(aPassword1, Normalizer.Form.NFC);
        String normalizedPasswordString2 = Normalizer.normalize(aPassword2, Normalizer.Form.NFC);
        return (normalizedPasswordString2.equals(normalizedPasswordString1));
    }

    public static void generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        mInstance.generateKeyInstance();
    }

    public static void setKeyBytes(byte[] aKeyBytes) {
        mInstance.setKeyBytesInstance(aKeyBytes);
    }

    public void setKeyBytesInstance(byte[] aKeyBytes) {
        mSecretKey = new SecretKeySpec(aKeyBytes, 0, aKeyBytes.length, "AES");
        mIv = new IvParameterSpec(iv);
        setEnabled(true);
    }

    private void generateKeyInstance() throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        String normalizedPasswordString = Normalizer.normalize(mPassword, Normalizer.Form.NFC);
        byte[] passwordBytes = normalizedPasswordString.getBytes("UTF-8");
        String utf8String = new String();

        // Convert the byte array back to a string (with the lower 8 bytes of each char representing a byte of the utf-8 password array)
        // This is horrible but is needed because older versions of Android only use the lower 8 bits of each char when computing the key from the password
        for(byte passwordByte : passwordBytes) {
            char c = (char) (passwordByte & 0xFF);
            utf8String += c;
        }

        KeySpec keySpec;
        SecretKeyFactory secretKeyFactory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Use compatibility key factory -- only uses lower 8-bits of passphrase chars
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1And8bit");
            keySpec = new PBEKeySpec(utf8String.toCharArray(), mSalt, PBKDF2_ITERATIONS, KEY_LENGTH);
            mSecretKey = secretKeyFactory.generateSecret(keySpec);
        } else {
            /*
            // Traditional key factory. Will use lower 8-bits of passphrase chars on
            // older Android versions (API level 18 and lower)
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            */
            byte[] keyBytes = CPbkdf2.derive(passwordBytes, mSalt, PBKDF2_ITERATIONS, KEY_LENGTH / 8);
            mSecretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
        }

        mIv = new IvParameterSpec(iv);
    }

    public static CipherOutputStream getCipherDecryptOutputStream(OutputStream aOutputStream) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        return mInstance.getCipherDecryptOutputStreamInstance(aOutputStream);
    }

    private CipherOutputStream getCipherDecryptOutputStreamInstance(OutputStream aOutputStream) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        if (mSecretKey == null)
            generateKey();

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, mSecretKey,mIv);
        return new CipherOutputStream(aOutputStream, cipher);
    }

    public static CipherInputStream getCipherDecryptInputStream(InputStream aInputStream) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        return mInstance.getCipherDecryptInputStreamInstance(aInputStream);
    }

    private CipherInputStream getCipherDecryptInputStreamInstance(InputStream aInputStream) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        if (mSecretKey == null)
            generateKey();

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, mSecretKey,mIv);
        return new CipherInputStream(aInputStream, cipher);
    }

    public static CipherOutputStream getCipherEncryptOutputStream(OutputStream aOutputStream) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        return mInstance.getCipherEncryptOutputStreamInstance(aOutputStream);
    }

    private CipherOutputStream getCipherEncryptOutputStreamInstance(OutputStream aOutputStream) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        if (mSecretKey == null)
            generateKey();

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, mSecretKey,mIv);
        return new CipherOutputStream(aOutputStream, cipher);
    }

    public static CipherInputStream getCipherEncryptInputStream(InputStream aInputStream) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        return mInstance.getCipherEncryptInputStreamInstance(aInputStream);
    }

    private CipherInputStream getCipherEncryptInputStreamInstance(InputStream aInputStream) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        if (mSecretKey == null)
            generateKey();

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, mSecretKey,mIv);
        return new CipherInputStream(aInputStream, cipher);
    }

    public static boolean enabled() {
        return mInstance.enabledInstance();
    }

    private boolean enabledInstance() {
        return mEnabled;
    }

    public static void setEnabled(boolean aEnabled) {
        mInstance.setEnabledInstance(aEnabled);
    }

    private void setEnabledInstance(boolean aEnabled) {
        mEnabled = aEnabled;
    }

    public static boolean testDecryptionWithReferenceXML(byte[] aReferenceData, boolean aAlreadyDecrypted) {
        return mInstance.testDecryptionWithReferenceXMLInstance(aReferenceData, aAlreadyDecrypted);
    }

    private boolean testDecryptionWithReferenceXMLInstance(byte[] aReferenceData, boolean aAlreadyDecrypted) {
        boolean dataAsExpected = false;

        if (aReferenceData == null)
            dataAsExpected = false;
        else {
            try {
                byte[] decryptedReferenceData = aReferenceData;
                if (!aAlreadyDecrypted) {
                    // We need to decrypt the data first, before validating it
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, mSecretKey,mIv);
                    decryptedReferenceData = cipher.doFinal(aReferenceData);
                }

                EMUtility.parseSendingDeviceInfo(decryptedReferenceData);

                dataAsExpected = true;
            }
            catch (Exception ex) {
                // There's been an exception, so this probably isn't decrypted correctly
                dataAsExpected = false;
            }
        }

        mDecryptionVerified = true;

        return dataAsExpected;
    }

    static public boolean isConfigured() {
        return mInstance.isConfiguredInstance();
    }

    private boolean isConfiguredInstance() {
        boolean configured = true;

        if (mPassword == null)
            configured = false;
        else if (mPassword.equals(""))
            configured = false;

        return configured;
    }

    static public boolean encryptionVerified() {
        return mInstance.encryptionVerifiedInstance();
    }

    private boolean encryptionVerifiedInstance() {
        return mDecryptionVerified;
    }

    private boolean mDecryptionVerified = false;
    private boolean mEnabled = false;
    private SecretKey mSecretKey;
    private byte[] mSalt;
    private String mPassword;
    private IvParameterSpec mIv;
    private byte[] iv = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
}

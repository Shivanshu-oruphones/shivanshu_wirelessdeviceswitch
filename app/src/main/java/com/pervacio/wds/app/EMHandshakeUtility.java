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

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;

import java.net.InetAddress;

public class EMHandshakeUtility {

    static void sendHandshakeXml(EMDeviceInfo aDeviceInfo, EMCommandDelegate aDelegate) {
        DLog.log("In sendHandshakeXml");
        aDeviceInfo.log();

        try {
            // Generate the handshake XML
            EMXmlGenerator xmlGenerator = new EMXmlGenerator();
            xmlGenerator.startDocument();

            xmlGenerator.startElement(EMStringConsts.EM_XML_DEVICE_INFO);

            // Write the device name element
            xmlGenerator.startElement(EMStringConsts.EM_XML_DEVICE_NAME);
            xmlGenerator.writeText(aDeviceInfo.mDeviceName);
            xmlGenerator.endElement(EMStringConsts.EM_XML_DEVICE_NAME);

            // Write the port number for the service
            xmlGenerator.startElement(EMStringConsts.EM_XML_DEVICE_PORT);
            xmlGenerator.writeText(Integer.toString(aDeviceInfo.mPort));
            xmlGenerator.endElement(EMStringConsts.EM_XML_DEVICE_PORT);

            // Write the service name element
            xmlGenerator.startElement(EMStringConsts.EM_XML_SERVICE_NAME);
            xmlGenerator.writeText(aDeviceInfo.mServiceName);
            xmlGenerator.endElement(EMStringConsts.EM_XML_SERVICE_NAME);

            xmlGenerator.startElement(EMStringConsts.EM_XML_DEVICE_UNIQUE_ID);
            xmlGenerator.writeText(aDeviceInfo.mDeviceUniqueId);
            xmlGenerator.endElement(EMStringConsts.EM_XML_DEVICE_UNIQUE_ID);

            // Write the IPv4 address element if present
            if (aDeviceInfo.mIpV4Address != null) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_WIFI_SERVER_IP4_ADDRESS);
                xmlGenerator.writeText(aDeviceInfo.mIpV4Address.getHostAddress());
                xmlGenerator.endElement(EMStringConsts.EM_XML_WIFI_SERVER_IP4_ADDRESS);
            }

            // Write the IPv6 address element if present
            if (aDeviceInfo.mIpV6Address != null) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_WIFI_SERVER_IP6_ADDRESS);
                xmlGenerator.writeText(aDeviceInfo.mIpV4Address.toString());
                xmlGenerator.endElement(EMStringConsts.EM_XML_WIFI_SERVER_IP6_ADDRESS);
            }

            // START – Pervacio
            /*** Send data required for Dashboard Logging ***/

            //Write platform
            xmlGenerator.startElement(Constants.DB_XML.PLATFORM.value());
            if (aDeviceInfo.dbDevicePlatform != null) {
                xmlGenerator.writeText(aDeviceInfo.dbDevicePlatform);
            } else {
                xmlGenerator.writeText(Constants.UNKNOWN);
            }
            xmlGenerator.endElement(Constants.DB_XML.PLATFORM.value());


            //Write make
            xmlGenerator.startElement(Constants.DB_XML.MAKE.value());
            if (aDeviceInfo.dbDeviceMake != null) {
                xmlGenerator.writeText(aDeviceInfo.dbDeviceMake);
            } else {
                xmlGenerator.writeText(Constants.UNKNOWN);
            }
            xmlGenerator.endElement(Constants.DB_XML.MAKE.value());

            //Write model
            xmlGenerator.startElement(Constants.DB_XML.MODEL.value());
            if (aDeviceInfo.dbDeviceModel != null) {
                xmlGenerator.writeText(aDeviceInfo.dbDeviceModel);
            } else {
                xmlGenerator.writeText(Constants.UNKNOWN);
            }
            xmlGenerator.endElement(Constants.DB_XML.MODEL.value());

            //Write OS version
            xmlGenerator.startElement(Constants.DB_XML.OS_VERSION.value());
            if (aDeviceInfo.dbDeviceOSVersion != null) {
                xmlGenerator.writeText(aDeviceInfo.dbDeviceOSVersion);
            } else {
                xmlGenerator.writeText(Constants.UNKNOWN);
            }
            xmlGenerator.endElement(Constants.DB_XML.OS_VERSION.value());

            //Write build number
            xmlGenerator.startElement(Constants.DB_XML.BUILD_NO.value());
            if (aDeviceInfo.dbDeviceBuildNumber != null) {
                xmlGenerator.writeText(aDeviceInfo.dbDeviceBuildNumber);
            } else {
                xmlGenerator.writeText(Constants.UNKNOWN);
            }
            xmlGenerator.endElement(Constants.DB_XML.BUILD_NO.value());

            //Write firmware
            xmlGenerator.startElement(Constants.DB_XML.FIRMWARE.value());
            if (aDeviceInfo.dbDeviceFirmware != null) {
                xmlGenerator.writeText(aDeviceInfo.dbDeviceFirmware);
            } else {
                xmlGenerator.writeText(Constants.UNKNOWN);
            }
            xmlGenerator.endElement(Constants.DB_XML.FIRMWARE.value());

            //Write IMEI
            xmlGenerator.startElement(Constants.DB_XML.IMEI.value());
            if (aDeviceInfo.dbDeviceIMEI != null) {
                xmlGenerator.writeText(aDeviceInfo.dbDeviceIMEI);
            } else {
                xmlGenerator.writeText(Constants.UNKNOWN);
            }
            xmlGenerator.endElement(Constants.DB_XML.IMEI.value());

            //Write Operation type
            xmlGenerator.startElement(Constants.DB_XML.OPERATION_TYPE.value());
            if (aDeviceInfo.dbOperationType != null) {
                xmlGenerator.writeText(aDeviceInfo.dbOperationType);
            } else {
                xmlGenerator.writeText(Constants.UNKNOWN);
            }
            xmlGenerator.endElement(Constants.DB_XML.OPERATION_TYPE.value());

            //Total storage
            xmlGenerator.startElement(Constants.DB_XML.TOTAL_STORAGE.value());
            xmlGenerator.writeText(String.valueOf(aDeviceInfo.dbDeviceTotalStorage));
            xmlGenerator.endElement(Constants.DB_XML.TOTAL_STORAGE.value());

            //Free storage
            xmlGenerator.startElement(Constants.DB_XML.FREE_STORAGE.value());
            xmlGenerator.writeText(String.valueOf(aDeviceInfo.dbDeviceFreeStorage));
            xmlGenerator.endElement(Constants.DB_XML.FREE_STORAGE.value());

            //App version
            xmlGenerator.startElement(Constants.DB_XML.APP_VERSION.value());
            xmlGenerator.writeText(BuildConfig.VERSION_NAME);
            xmlGenerator.endElement(Constants.DB_XML.APP_VERSION.value());

            // End – Pervacio

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_CONTACTS) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_CONTACTS);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_CALENDAR) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_CALENDAR);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_PHOTOS) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_PHOTOS);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_VIDEOS) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_VIDEO);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_SMS_MESSAGES) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_SMS_MESSAGES);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_NOTES) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_NOTES);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_ACCOUNTS) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_ACCOUNTS);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_DOCUMENTS) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_DOCUMENTS);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_MUSIC) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_MUSIC);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mCapabilities & EMDeviceInfo.EM_SUPPORTS_APP) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_CAN_ADD);
                xmlGenerator.writeText(EMStringConsts.EM_XML_KEYBOARD_SHORTCUTS);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CAN_ADD);
            }

            if ((aDeviceInfo.mRoles & EMDeviceInfo.EM_SUPPORTS_ROLE_MIGRATION_SOURCE) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_SUPPORTS_ROLE);
                xmlGenerator.writeText(EMStringConsts.EM_XML_ROLE_MIGRATION_SOURCE);
                xmlGenerator.endElement(EMStringConsts.EM_XML_SUPPORTS_ROLE);
            }

            if ((aDeviceInfo.mRoles & EMDeviceInfo.EM_SUPPORTS_ROLE_MIGRATION_TARGET) != 0) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_SUPPORTS_ROLE);
                xmlGenerator.writeText(EMStringConsts.EM_XML_ROLE_MIGRATION_TARGET);
                xmlGenerator.endElement(EMStringConsts.EM_XML_SUPPORTS_ROLE);
            }

            if (CommonUtil.getInstance().getLinkSpeed() != 0 && aDeviceInfo.dbOperationType.equalsIgnoreCase(Constants.OPERATION_TYPE.RESTORE.value())) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_LINK_SPEED);
                xmlGenerator.writeText(String.valueOf(CommonUtil.getInstance().getLinkSpeed()));
                xmlGenerator.endElement(EMStringConsts.EM_XML_LINK_SPEED);
            }
            try {
                if (aDeviceInfo.dbOperationType.equalsIgnoreCase(Constants.OPERATION_TYPE.RESTORE.value()) && DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId() != null) {
                    xmlGenerator.startElement(EMStringConsts.EM_XML_SESSION_ID);
                    xmlGenerator.writeText(String.valueOf(DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId()));
                    xmlGenerator.endElement(EMStringConsts.EM_XML_SESSION_ID);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (aDeviceInfo.mThisDeviceIsTargetAutoConnect) {
                xmlGenerator.startElement(EMStringConsts.EM_XML_THIS_DEVICE_IS_TARGET_AUTO_CONNECT);
                xmlGenerator.writeText(EMStringConsts.EM_XML_XML_TRUE);
                xmlGenerator.endElement(EMStringConsts.EM_XML_THIS_DEVICE_IS_TARGET_AUTO_CONNECT);
            }

            xmlGenerator.startElement(EMStringConsts.EM_XML_KEYBOARD_SHORTCUT_IMPORTER_AVAILABLE);
            if (aDeviceInfo.mKeyboardShortcutImporterAvailable) {
                xmlGenerator.writeText(EMStringConsts.EM_XML_XML_TRUE);
            } else {
                xmlGenerator.writeText(EMStringConsts.EM_XML_XML_FALSE);
            }
            xmlGenerator.endElement(EMStringConsts.EM_XML_KEYBOARD_SHORTCUT_IMPORTER_AVAILABLE);

            int deniedDataType = 0;
            if (ContextCompat.checkSelfPermission(EMUtility.Context(), Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                deniedDataType |= EMDataType.EM_DATA_TYPE_CONTACTS;
            }
            if (ContextCompat.checkSelfPermission(EMUtility.Context(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                deniedDataType |= EMDataType.EM_DATA_TYPE_MUSIC;
                deniedDataType |= EMDataType.EM_DATA_TYPE_VIDEO;
                deniedDataType |= EMDataType.EM_DATA_TYPE_PHOTOS;
                deniedDataType |= EMDataType.EM_DATA_TYPE_APP;
            }
            if (ContextCompat.checkSelfPermission(EMUtility.Context(), Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                deniedDataType |= EMDataType.EM_DATA_TYPE_CALENDAR;
            }
            if (ContextCompat.checkSelfPermission(EMUtility.Context(), Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                deniedDataType |= EMDataType.EM_DATA_TYPE_CALL_LOGS;
            }
            if (ContextCompat.checkSelfPermission(EMUtility.Context(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                deniedDataType |= EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
            }
            xmlGenerator.startElement(EMStringConsts.DENIED_PERMISSIONS);
            xmlGenerator.writeText(String.valueOf(deniedDataType));
            xmlGenerator.endElement(EMStringConsts.DENIED_PERMISSIONS);

            xmlGenerator.endElement(EMStringConsts.EM_XML_DEVICE_INFO);

            String filePath = xmlGenerator.endDocument();

            // Send the XML file to the remote device, delete it when done
            aDelegate.sendFile(filePath, true, null);
        } catch (Exception ex) {
            ex.printStackTrace();
            DLog.log(ex);
            // TODO: handle exception
        }
    }

    static EMDeviceInfo processHandshakeXml(String aFilePath, boolean aDeleteAfterProcessing) {
        DLog.log("In processHandshakeXml");
        EMDeviceInfo deviceInfo = new EMDeviceInfo();

        try {

            EMXmlPullParser pullParser = new EMXmlPullParser();
            pullParser.setFilePath(aFilePath);

            EMXmlPullParser.EMXmlNodeType nodeType = pullParser.readNode();
            while ((nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT)
                    && (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_NO_NODE)) // While there is no error and we haven't reached the last node
            {
                if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT) {
                    String elementName = pullParser.name();

                    if (elementName.equals(EMStringConsts.EM_XML_DEVICE_NAME)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            deviceInfo.mDeviceName = pullParser.value();
                        }
                        // TODO: what should we do if it's not text? signal a bad-xml error probably - or just say "Unknown Device"?
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_SERVICE_NAME)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            deviceInfo.mServiceName = pullParser.value();
                        }
                        // TODO: what should we do if it's not text? signal a bad-xml error probably - or just say "Unknown Device"?
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_DEVICE_UNIQUE_ID)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            deviceInfo.mDeviceUniqueId = pullParser.value();
                        }
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_DEVICE_PORT)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.mPort = Integer.parseInt(pullParser.value());
                            }
                        }
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_WIFI_SERVER_IP4_ADDRESS)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.mIpV4Address = InetAddress.getByName(pullParser.value());
                            }
                        }
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_WIFI_SERVER_IP6_ADDRESS)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.mIpV6Address = InetAddress.getByName(pullParser.value());
                            }
                        }
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_THIS_DEVICE_IS_TARGET_AUTO_CONNECT)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            String autoConnectValue = pullParser.value();
                            if (autoConnectValue.equals(EMStringConsts.EM_XML_XML_TRUE)) {
                                deviceInfo.mThisDeviceIsTargetAutoConnect = true;
                            }
                        }
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_CAN_ADD)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            String dataTypeString = pullParser.value();
                            if (dataTypeString != null) {
                                if (dataTypeString.equals(EMStringConsts.EM_XML_CONTACTS))
                                    deviceInfo.mCapabilities |= EMDeviceInfo.EM_SUPPORTS_CONTACTS;
                                else if (dataTypeString.equals(EMStringConsts.EM_XML_CALENDAR))
                                    deviceInfo.mCapabilities |= EMDeviceInfo.EM_SUPPORTS_CALENDAR;
                                else if (dataTypeString.equals(EMStringConsts.EM_XML_PHOTOS))
                                    deviceInfo.mCapabilities |= EMDeviceInfo.EM_SUPPORTS_PHOTOS;
                            }
                        }
                    }

                    if (elementName.equals(EMStringConsts.DENIED_PERMISSIONS)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            String dataTypeString = pullParser.value();
                            if (dataTypeString != null) {
                               deviceInfo.deniedPermissionsDataTypes=dataTypeString;
                            }
                        }
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_SUPPORTS_ROLE)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            String dataTypeString = pullParser.value();
                            if (dataTypeString != null) {
                                if (dataTypeString.equals(EMStringConsts.EM_XML_ROLE_MIGRATION_SOURCE))
                                    deviceInfo.mRoles |= EMDeviceInfo.EM_SUPPORTS_ROLE_MIGRATION_SOURCE;
                                else if (dataTypeString.equals(EMStringConsts.EM_XML_ROLE_MIGRATION_TARGET))
                                    deviceInfo.mRoles |= EMDeviceInfo.EM_SUPPORTS_ROLE_MIGRATION_TARGET;
                            }
                        }
                    }

                    // START – Pervacio
                    /*** Parsing logic for Dashboard Logging ***/
                    //Get required device info


                    //Make
                    if (elementName.equals(Constants.DB_XML.MAKE.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbDeviceMake = pullParser.value();
                            } else {
                                deviceInfo.dbDeviceMake = Constants.UNKNOWN;
                            }
                        }
                    }

                    //Model
                    if (elementName.equals(Constants.DB_XML.MODEL.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbDeviceModel = pullParser.value();
                            } else {
                                deviceInfo.dbDeviceModel = Constants.UNKNOWN;
                            }
                        }
                    }

                    //OS Version
                    if (elementName.equals(Constants.DB_XML.OS_VERSION.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbDeviceOSVersion = pullParser.value();
                            } else {
                                deviceInfo.dbDeviceOSVersion = Constants.UNKNOWN;
                            }
                        }
                    }

                    //Platform
                    if (elementName.equals(Constants.DB_XML.PLATFORM.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbDevicePlatform = pullParser.value();
                            } else {
                                deviceInfo.dbDevicePlatform = Constants.UNKNOWN;
                            }
                        }
                    }

                    //Build no
                    if (elementName.equals(Constants.DB_XML.BUILD_NO.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbDeviceBuildNumber = pullParser.value();
                            } else {
                                deviceInfo.dbDeviceBuildNumber = Constants.UNKNOWN;
                            }
                        }
                    }

                    //Firmware
                    if (elementName.equals(Constants.DB_XML.FIRMWARE.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbDeviceFirmware = pullParser.value();
                            } else {
                                deviceInfo.dbDeviceFirmware = Constants.UNKNOWN;
                            }
                        }
                    }

                    //Operation type
                    if (elementName.equals(Constants.DB_XML.OPERATION_TYPE.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbOperationType = pullParser.value();
                            } else {
                                deviceInfo.dbOperationType = Constants.UNKNOWN;
                            }
                        }
                    }

                    //IMEI
                    if (elementName.equals(Constants.DB_XML.IMEI.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbDeviceIMEI = pullParser.value();
                            } else {
                                deviceInfo.dbDeviceIMEI = Constants.UNKNOWN;
                            }
                        }
                    }

                    //Total storage
                    if (elementName.equals(Constants.DB_XML.TOTAL_STORAGE.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbDeviceTotalStorage = Long.parseLong(pullParser.value());
                            } else {
                                deviceInfo.dbDeviceTotalStorage = Constants.NO_STORAGE;
                            }
                        }
                    }

                    //Free storage
                    if (elementName.equals(Constants.DB_XML.FREE_STORAGE.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.dbDeviceFreeStorage = Long.parseLong(pullParser.value());
                            } else {
                                deviceInfo.dbDeviceFreeStorage = Constants.NO_STORAGE;
                            }
                        }
                    }

                    if (elementName.equals(Constants.DB_XML.APP_VERSION.value())) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                deviceInfo.appVersion = pullParser.value();
                            } else {
                                deviceInfo.appVersion = BuildConfig.VERSION_NAME;
                            }
                        }
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_LINK_SPEED)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                CommonUtil.getInstance().setLinkSpeed(Integer.parseInt(pullParser.value()));
                            }
                        }
                    }

                    if (elementName.equals(EMStringConsts.EM_XML_SESSION_ID)) {
                        nodeType = pullParser.readNode();
                        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                            if (pullParser.value() != null) {
                                try {
                                    DashboardLog.getInstance().geteDeviceSwitchSession().setDeviceSwitchSessionId(pullParser.value());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                DLog.log("Session id "+pullParser.value());
                            }
                        }
                    }

                    // END – Pervacio

                }

                nodeType = pullParser.readNode();
            }

            if (aDeleteAfterProcessing) {
                // [[NSFileManager defaultManager] removeItemAtPath:filePath error:NULL];
                // TODO: delete the file
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            DLog.log(ex);
            // TODO: handle exception
        }

        if (deviceInfo != null) {
            deviceInfo.log();
        } else {
            DLog.log("Null device info");
        }

        // Return the device info from the parsed XML
        return deviceInfo;
    }
}

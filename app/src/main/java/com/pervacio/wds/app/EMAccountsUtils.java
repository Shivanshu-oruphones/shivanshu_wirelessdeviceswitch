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

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class EMAccountsUtils {
    static void parseAccounts(String aFilePath) throws IOException, XmlPullParserException {
        EMXmlPullParser pullParser = new EMXmlPullParser();
        pullParser.setFilePath(aFilePath);

        EMXmlPullParser.EMXmlNodeType nodeType = pullParser.readNode();
        while ((nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT) && (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_NO_NODE)) // While there is no error and we haven't reached the last node
        {
            if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT)
            {
                String elementName = pullParser.name();

                if (elementName.equals(EMStringConsts.EM_XML_EMAIL_ACCOUNT_ADDRESS))
                {
                    nodeType = pullParser.readNode();
                    if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT)
                    {
                        String emailAddress = pullParser.value();

                        if (!EMMigrateStatus.getEmailAccountAddresses().contains(emailAddress)) {
                            EMMigrateStatus.addEmailAccountAddress(emailAddress);
                            EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_ACCOUNTS);
                        }
                    }
                }
            }

            nodeType = pullParser.readNode();
        }
    }
}

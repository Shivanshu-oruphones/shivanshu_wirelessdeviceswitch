package com.pervacio.wds.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.models.MMS;
import com.pervacio.wds.custom.models.MMSPart;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

    /**
     * Created by Surya Polasanapalli
     **/

public class EMMMSUtils {

    /**
     * Get MMS attached data
     *
     * @param _id
     * @return
     */
    static EMGlobals emGlobals = new EMGlobals();
    private String getMMSPartData(String _id) {
        Uri partURI = Uri.parse("content://mms/part/" + _id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = emGlobals.getmContext().getContentResolver().openInputStream(partURI);
            if (is != null) {
                byte[] imageBytes = new byte[is.available()];
                is.read(imageBytes, 0, imageBytes.length);
                sb.append(Base64.encodeToString(imageBytes, Base64.DEFAULT));
            }
        } catch (OutOfMemoryError e) {
            DLog.log(e.getMessage());
        } catch (Exception e) {
            DLog.log(e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    DLog.log(e.getMessage());
                }
            }
        }
        return sb.toString();
    }


    /**
     * Get MMS text content
     *
     * @param id
     * @return
     */
    private String getMmsText(String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = emGlobals.getmContext().getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    DLog.log(e.getMessage());
                }
            }
        }
        return sb.toString();
    }


    private String getMMSAddress(String id) {
        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        String[] columns = {"address"};
        Cursor cursor = emGlobals.getmContext().getContentResolver().query(uriAddress, columns,
                null, null, null);
        StringBuilder address = new StringBuilder();
        String val;
        if (cursor.moveToFirst()) {
            do {
                val = cursor.getString(cursor.getColumnIndex("address"));
                if (val != null) {
                    address.append(val);
                    address.append(",");
                    // Use the first one found if more than one
                    break;
                }
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        // return address.replaceAll("[^0-9]", "");
        return address.toString();
    }


    public List<MMS> getMMSDetails(String whereQuery) {
        List<MMS> mmsList = new ArrayList<>();
        ContentResolver contentResolver = emGlobals.getmContext().getContentResolver();
        Uri uri = Uri.parse("content://mms");
        Cursor cursor = contentResolver.query(uri, null, whereQuery, null, "date" + " DESC");
        if (cursor != null) {
            StringBuilder smsBuilder = new StringBuilder();
            while (cursor.moveToNext()) {
                MMS mms = new MMS();
                List<MMSPart> parts = new ArrayList<>();
                String address = getMMSAddress(String.valueOf(cursor.getInt(cursor.getColumnIndex("_id"))));//getANumber(cursor.getInt(cursor.getColumnIndex("_id")));
                long date = cursor.getLong(cursor.getColumnIndex("date"));
                int read = cursor.getInt(cursor.getColumnIndex("read"));
                int msg_box = cursor.getInt(cursor.getColumnIndex("msg_box"));
                //int int_Type = cursor.getInt(cursor.getColumnIndexOrThrow("m_type"));
                String ct_t = cursor.getString(cursor.getColumnIndex("ct_t"));
                String subject = cursor.getString(cursor.getColumnIndex("sub"));
                String selectionPart = "mid = '" + cursor.getString(0) + "'";
                Cursor curPart = emGlobals.getmContext().getContentResolver().query(Uri.parse("content://mms/part"), null, selectionPart, null, null);
                while (curPart != null && curPart.moveToNext()) {
                    MMSPart mmsPart = new MMSPart();
                    String mmsdata = null;
                    String text = null;
                    String ct = curPart.getString(curPart.getColumnIndex("ct"));
                    String cid = curPart.getString(curPart.getColumnIndex("cid"));
                    String cl = curPart.getString(curPart.getColumnIndex("cl"));
                    mmsPart.setName(curPart.getString(curPart.getColumnIndex("name")));
                    if (ct != null && (ct.startsWith("video") || ct.startsWith("image") || ct.startsWith("audio") || ct.startsWith("application"))) {
                        mmsdata = getMMSPartData(curPart.getString(0));
                    } else if ("text/plain".equalsIgnoreCase(ct)) {
                        String _data = curPart.getString(curPart.getColumnIndex("_data"));
                        if (_data != null) {
                            text = getMmsText(curPart.getString(0));
                        } else {
                            text = curPart.getString(curPart.getColumnIndex("text"));
                        }
                    } else if ("text/x-vCard".equalsIgnoreCase(ct)) {
                        mmsdata = getMMSPartData(curPart.getString(0));
                    }
                    mmsPart.setCt(ct);
                    mmsPart.setCid(cid);
                    mmsPart.setCl(cl);
                    mmsPart.setData(mmsdata);
                    mmsPart.setText(text);
                    parts.add(mmsPart);
                }
                if (curPart != null) {
                    curPart.close();
                }
                mms.setSubject(subject);
                mms.setAddress(address);
                mms.setContentType(ct_t);
                mms.setDate(date);
                mms.setMsgBox(msg_box);
                mms.setRead(read);
                mms.setParts(parts);
                mmsList.add(mms);
            }
            cursor.close();
        }
        return mmsList;
    }


    public void insertMMS(MMS mms) {
        ContentResolver contentResolver = emGlobals.getmContext().getContentResolver();
        Uri uri = Uri.parse("content://mms");

        ContentValues values = new ContentValues();

        values.put("msg_box", mms.getMsgBox());
        values.put("read", mms.getRead());
        values.put("date", mms.getDate());
        values.put("seen", Integer.toString(1));
        values.put("ct_t", mms.getContentType());
        values.put("sub", mms.getSubject());
        values.put("sub_cs", 106);
        //values.put("exp", Base64.decode(mms.getParts().get(0).getData(), Base64.DEFAULT).length);
        values.put("m_cls", "personal");
        values.put("m_type", 128); // 132 (RETRIEVE CONF) 130 (NOTIF IND) 128 (SEND REQ)
        values.put("v", 19);
        values.put("pri", 129);
        values.put("tr_id", "T"+ Long.toHexString(System.currentTimeMillis()));
        values.put("resp_st", 128);


        Uri insertUri = contentResolver.insert(uri, values);
        String mmsId = insertUri.getLastPathSegment().trim();


        for (MMSPart mmsPart : mms.getParts()) {

            ContentValues mmsPartValue = new ContentValues();
            mmsPartValue.put("cid", mmsPart.getCid());
            mmsPartValue.put("ct", mmsPart.getCt());
            mmsPartValue.put("cl", mmsPart.getCl());
            mmsPartValue.put("text", mmsPart.getText());
            Uri partUri = Uri.parse("content://mms/" + mmsId+"/part");//Uri.parse("content://mmsmessageId" + "/part");
            Uri mmsPartUri = emGlobals.getmContext().getContentResolver().insert(partUri, mmsPartValue);

            try {
                OutputStream os = emGlobals.getmContext().getContentResolver().openOutputStream(mmsPartUri);
                //InputStream is = emGlobals.getmContext().getContentResolver().openInputStream(Base64.decode(mmsPart.getData(),Base64.DEFAULT));
                byte[] is = Base64.decode(mmsPart.getData(), Base64.DEFAULT);
                /*byte[] buffer = new byte[256];
                for (int len = 0; (len = is.read(buffer)) != -1; ) {
                    os.write(buffer, 0, len);
                }*/
                os.write(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        createAddr(mmsId, mms.getAddress());
    }

    private Uri createAddr(String id, String addr) {
        for (String add : addr.split(",")) {
            if (!TextUtils.isEmpty(add)) {
                try {
                    ContentValues addrValues = new ContentValues();
                    addrValues.put("address", add);
                    addrValues.put("charset", "106");
                    addrValues.put("type", 151); // TO
                    String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
                    Uri uriAddress = Uri.parse(uriStr);
                    Uri res = emGlobals.getmContext().getContentResolver().insert(uriAddress, addrValues);
                    return res;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


}

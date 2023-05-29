package com.pervacio.wds.custom.imeireader;

import java.util.List;

public interface IMEIReaderListener {

    enum ImeiStatus {
         FOUND ,NOT_FOUND, TIME_OUT, ERROR;
    }
    void onIMEI(ImeiStatus status , List<String> list);
    void onError(String error);
}

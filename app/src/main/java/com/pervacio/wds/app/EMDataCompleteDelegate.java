package com.pervacio.wds.app;

public interface EMDataCompleteDelegate {
	void commandComplete(String dataType,boolean status);
	void restoreCompleted(String dataType,boolean status);
}

package com.androidron.keyring;

import android.content.Intent;

public class DialogStore {
	
	Intent dialogIntent;
	String dialogName;
	
	public DialogStore(Intent dialogIntent, String dialogName) {
		super();
		this.dialogIntent = dialogIntent;
		this.dialogName = dialogName;
	}
	
	public Intent getDialogIntent() {
		return dialogIntent;
	}
	public String getDialogName() {
		return dialogName;
	}

}

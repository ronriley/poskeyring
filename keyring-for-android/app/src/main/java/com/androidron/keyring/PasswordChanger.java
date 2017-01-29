package com.androidron.keyring;

/*
Keyring for Android
Copyright (C) 2011 Ron Riley
(android.keyring@gmail.com)

Keyring for Android is based on: 

KeyringEditor
Copyright 2004 Markus Griessnig
Vienna University of Technology
Institute of Computer Technology

KeyringEditor is based on:
Java Keyring v0.6
Copyright 2004 Frank Taylor <keyring@lieder.me.uk>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
import java.io.FileNotFoundException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class PasswordChanger extends RootActivity {
	
	public static final int MAX_PASSWORD_LENGTH = 40; //TODO not here..
	
	static private ProgressDialog progressDialog;
	

	
	
	 final Handler mHandler = new Handler();

	    final Runnable mUpdateResults = new Runnable() {
	        public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						PasswordChanger.this);
				builder = buildConfirm(builder, R.string.message_password_changed);
				
				otherDialog = builder.show();
				progressDialog.cancel();
	        }
	    };

	@Override
	protected void onServiceReady() {

		super.onServiceReady();


		final EditText password1 = (EditText) findViewById(R.id.password1);
		final EditText password2 = (EditText) findViewById(R.id.password2);
		final Button saveButton = (Button) findViewById(R.id.button_save);
		Button buttonCancel = (Button) findViewById(R.id.button_cancel);


		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (password1.getText().toString().trim().length() == 0){
					AlertDialog.Builder builder = new AlertDialog.Builder(
							PasswordChanger.this);
					builder = buildCancel(builder, R.string.message_password_cannot_be_null);
					otherDialog = builder.show();
				}
				else if (password1.getText().toString().trim().length() > MAX_PASSWORD_LENGTH){
					AlertDialog.Builder builder = new AlertDialog.Builder(
							PasswordChanger.this);
					builder = buildCancel(builder, R.string.message_password_too_long);
					otherDialog = builder.show();
				}
				else if (!password1.getText().toString().equals(password2.getText().toString())){
					AlertDialog.Builder builder = new AlertDialog.Builder(
							PasswordChanger.this);
					builder = buildCancel(builder, R.string.message_passwords_do_not_match);
					otherDialog = builder.show();
				}
				else{
						progressDialog = ProgressDialog.show(PasswordChanger.this, "", 
								getString(R.string.updating_password_store), true);
						 // Fire off a thread to do some work that we shouldn't do directly in the UI thread
				        Thread t = new Thread() {
				            public void run() {
								try {
									fileService.changePassword(password1.getText().toString().toCharArray());
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								} catch (Exception e) {
									e.printStackTrace();
								}
				                mHandler.post(mUpdateResults);
				            }
				        };
				        t.start();
				}
			}
		});


		buttonCancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.password_changer);
		super.onCreate(savedInstanceState);
}
	
	

}

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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NewPrivateKeystore extends RootActivity {
	
	public static final int MAX_PASSWORD_LENGTH = 40; //TODO not here..
	
	static private ProgressDialog progressDialog;
	

	
	
	 final Handler mHandler = new Handler();

	    // Create runnable for posting
	    final Runnable mUpdateResults = new Runnable() {
	        public void run() {
//				AlertDialog.Builder builder = new AlertDialog.Builder(
//						NewPrivateKeystore.this);
//				builder = buildConfirm(builder, R.string.message_private_keyring_created);
//				
//				builder.create();
//				builder.show();
				progressDialog.cancel();
	        	Toast toast = Toast.makeText(getApplicationContext(), R.string.message_private_keyring_created, Toast.LENGTH_LONG);
	        	toast.show();
	        }
	    };

	@Override
	protected void onServiceReady() {

		super.onServiceReady();
		//if (!fileService.passwordSet())
		//	return;
		
		if (fileService.hasPrivateKeystore()) {
			
			final Intent mainPage = new Intent();
			mainPage.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");
			
			// warn the user that they might delete all their data..
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					NewPrivateKeystore.this);
			builder.setMessage(R.string.warn_keyfile_exists)
					.setCancelable(true)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									fileService.clearPassword();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
									finish();
									startActivity(mainPage);
								}
							});
			otherDialog = builder.show();
			//builder.show();
		}

		final EditText password1 = (EditText) findViewById(R.id.password1);
		final EditText password2 = (EditText) findViewById(R.id.password2);
		final Button saveButton = (Button) findViewById(R.id.button_save);
		Button buttonCancel = (Button) findViewById(R.id.button_cancel);
		
		final Intent mainPage = new Intent();
		mainPage.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");
		

		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (password1.getText().toString().trim().length() == 0){
					AlertDialog.Builder builder = new AlertDialog.Builder(
							NewPrivateKeystore.this);
					builder = buildCancel(builder, R.string.message_password_cannot_be_null);
					otherDialog = builder.show();
					//builder.show();
				}
				else if (password1.getText().toString().trim().length() > MAX_PASSWORD_LENGTH){
					AlertDialog.Builder builder = new AlertDialog.Builder(
							NewPrivateKeystore.this);
					builder = buildCancel(builder, R.string.message_password_too_long);
					otherDialog = builder.show();
					//builder.show();
				}
				else if (!password1.getText().toString().equals(password2.getText().toString())){
					AlertDialog.Builder builder = new AlertDialog.Builder(
							NewPrivateKeystore.this);
					builder = buildCancel(builder, R.string.message_passwords_do_not_match);
					otherDialog = builder.show();
					//builder.show();
				}
				else{
					//try {
						progressDialog = ProgressDialog.show(NewPrivateKeystore.this, "", 
		                        getString(R.string.creating_keyring), true);//TODO use R.string.value
						 // Fire off a thread to do some work that we shouldn't do directly in the UI thread
				        Thread t = new Thread() {
				            public void run() {
								try {
									fileService.createNewModel(password1.getText().toString().toCharArray());
									startActivity(mainPage);
									finish();
								} catch (Exception e) {
									// TODO Auto-generated catch block
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
				startActivity(mainPage);
				finish();
			}
		});

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.new_private_keystore);
		super.onCreate(savedInstanceState);
}
	
}

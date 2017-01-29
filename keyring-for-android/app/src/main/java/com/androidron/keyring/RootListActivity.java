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
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public abstract class RootListActivity extends ListActivity {

	public static final String KEYSTORE_FILENAME = "keyStore.pdb";
	private static final String TAG = "RootListActivity";
	protected PdbFileHandlerService fileService;
	protected boolean serviceIsBound = false;
	protected boolean serviceIsReady = false;
	// these to recover after orientation change pita
	protected Dialog passwordDialog;
	protected Dialog otherDialog;
//	protected WeakReference weakDialog;
	protected Intent passwordDialogIntent;
	//protected Intent otherDialogIntent;
	protected DialogStore dialogStore;
	//private ScreenOrientationHandler screenOrientationHandler;
	//private boolean paused = false;

	

	private void challengeForPassword(final Intent intent) {

		if (fileService.passwordSet()) {
			finish();
			startActivity(intent);
		} else if (fileService.hasPrivateKeystore()) {
			final Dialog dialog = new PasswordDialog(RootListActivity.this);
			passwordDialog = dialog;
			passwordDialogIntent = intent;
			dialog.setContentView(R.layout.password_dialog);
			dialog.setTitle(R.string.password);
			final EditText password = (EditText) dialog
					.findViewById(R.id.password);
			final TextView message = (TextView) dialog
					.findViewById(R.id.message);
			Button buttonPassword = (Button) dialog.findViewById(R.id.button);
			Button buttonCancel = (Button) dialog
					.findViewById(R.id.button_cancel);

			buttonCancel.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialogStore = null;
					dialog.dismiss();
				}
			});

			buttonPassword.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					try {
						fileService.setPassword(password.getText().toString()
								.toCharArray());

						dialogStore = null;
						dialog.dismiss();
						//finish();
						startActivity(intent);
					} catch (BadPasswordException pwd) {
						password.setText("");
						message.setText(R.string.label_failed_password);
					}
				}
			});
			onCreateDialog(R.layout.password_dialog);
			dialog.show();
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			Log.d(TAG,"onServiceConnnected.., serviceIsReady is: " + serviceIsReady);
			fileService = ((PdbFileHandlerService.LocalBinder) service)
					.getService();
			serviceIsReady = true;
			onServiceReady();

		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			fileService = null;
			serviceIsReady = false;
		}
	};

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	private boolean allMenusActive(){
		return serviceIsReady && fileService.hasPrivateKeystore();
	}
	
	private void doToastNoKeystore(){
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
	
		Toast toast = Toast.makeText(context, R.string.message_please_import_or_create_a_keyring, duration);
		toast.show();
	}
	

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Intent intent = new Intent();
		switch (item.getItemId()) {
		case R.id.import_pdb:
			intent.setClassName("com.androidron.keyring", "com.androidron.keyring.ImportPdb");
			if (fileService.hasPrivateKeystore()) {
				challengeForPassword(intent);
			} else if (!fileService.hasPrivateKeystore()) {
				finish();
				startActivity(intent);
			}
			return true;
		case R.id.export_pdb:
			if(allMenusActive()){
				intent.setClassName("com.androidron.keyring", "com.androidron.keyring.ExportPdb");
				challengeForPassword(intent);
			}
			else{
				 doToastNoKeystore();
			}
			return true;
		case R.id.category_editor:

			if (allMenusActive()) {
				intent.setClassName("com.androidron.keyring",
						"com.androidron.keyring.CategoryEditor");
				finish();
				startActivity(intent);
			} else {
				doToastNoKeystore();
			}
			return true;
		case R.id.password_timeout:
			if (allMenusActive()) {
			
			intent.setClassName("com.androidron.keyring",
					"com.androidron.keyring.PasswordTimeout");
			challengeForPassword(intent);
			} else {
				doToastNoKeystore();
			}
			return true;
		case R.id.password:
			if (allMenusActive()) {		
			intent.setClassName("com.androidron.keyring",
					"com.androidron.keyring.PasswordChanger");
			challengeForPassword(intent);
			} else {
				doToastNoKeystore();
			}
			return true;
		case R.id.help:
			intent.setClassName("com.androidron.keyring",
					"com.androidron.keyring.Help");
			startActivity(intent);
			return true;
		case R.id.new_pdb:
			intent.setClassName("com.androidron.keyring", "com.androidron.keyring.NewPrivateKeystore");
			if (fileService.hasPrivateKeystore()) {
				challengeForPassword(intent);
			} else if (!fileService.hasPrivateKeystore()) {
				startActivity(intent);
			}
			return true;
		case R.id.toggle_orientation:
			intent.setClassName("com.androidron.keyring", "com.androidron.keyring.ScreenOrientationHandlerActivity");
			startActivity(intent);
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	void doStartService() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		Intent service = new Intent();
		service.setClassName("com.androidron.keyring",
				"com.androidron.keyring.PdbFileHandlerService");
		startService(service); // kick off a persistent service if not already running
	}
	
	void doBindService() {
		Intent service = new Intent();
	service.setClassName("com.androidron.keyring",
	"com.androidron.keyring.PdbFileHandlerService");
		bindService(service, mConnection, Context.BIND_AUTO_CREATE); // bind to it 
		serviceIsBound = true;
	}

	void doUnbindService() {
		unbindService(mConnection);
			serviceIsBound = false;
	
	}

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		//doUnbindService();
	}

	protected  void onServiceReady(){
		if (dialogStore != null){
			challengeForPassword(dialogStore.getDialogIntent());
		}
	}

	protected void setUpActionBar() {

		final Intent intent = new Intent();

		ImageButton homeButton = (ImageButton) findViewById(R.id.go_home);

		homeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				intent.setClassName("com.androidron.keyring",
						"com.androidron.keyring.IndexList");
				finish();
				startActivity(intent);
			}
		});

		ImageButton addButton = (ImageButton) findViewById(R.id.go_add);

		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (allMenusActive()) {
					intent.setClassName("com.androidron.keyring",
							"com.androidron.keyring.Item");
					intent.putExtra("entry.id", -1);
					challengeForPassword(intent);
				} else {
					doToastNoKeystore();
				}
			}
		});
//	protected void setUpActionBar() {
//
//		final Intent intent = new Intent();
//
//		ImageButton homeButton = (ImageButton) findViewById(R.id.go_home);
//
//		homeButton.setOnClickListener(new OnClickListener() {
//			public void onClick(View v) {
//				intent.setClassName("com.androidron.keyring",
//						"com.androidron.keyring.IndexList");
//				//finish();
//				startActivity(intent);
//			}
//		});
//
//		ImageButton addButton = (ImageButton) findViewById(R.id.go_add);
//
//		addButton.setOnClickListener(new OnClickListener() {
//			public void onClick(View v) {
//				intent.setClassName("com.androidron.keyring", "com.androidron.keyring.Item");
//				intent.putExtra("entry.id", -1);
//				challengeForPassword(intent);
//			}
//		});
		ImageButton searchButton = (ImageButton) findViewById(R.id.go_search);

		searchButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (allMenusActive()) {
					if (serviceIsReady && fileService.hasPrivateKeystore()) {
						onSearchRequested();
					}
				} else {
					doToastNoKeystore();
				}
			}
		});
		

	}
	// TODO move this and RootActivity builders to utility class
	protected AlertDialog.Builder buildCancel(AlertDialog.Builder builder,
			int message) {
		builder.setMessage(message)
				// TODO lookup
				.setCancelable(true)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		return builder;
	}
	// TODO refactor out and have a static to keep a note of
	// whether they have been warned
	private void checkCharSet(){
		// palm os uses Windows-1252 charset
		// check charsets
		try{	
			Charset.forName(PdbFileHandlerService.getCharset(this));
		}
		catch (UnsupportedCharsetException noPalmCs){
			AlertDialog.Builder builder = new AlertDialog.Builder(
					RootListActivity.this);
			builder = buildCancel(builder,
					R.string.warn_unsupported_charset);
			//builder.create();
			otherDialog = builder.show();
			return;
		}
		
	}
	
	
//	public Object onRetainNonConfigurationInstance() {
//        return fileService;
//}
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  if (passwordDialog != null){
		  savedInstanceState.putBoolean("showPasswordDialog", true);
		  savedInstanceState.putParcelable("passwordDialogIntent", passwordDialogIntent);
	  }
//	  if (otherDialog != null){
//		  savedInstanceState.putBoolean("showOtherDialog", true);
//		  savedInstanceState.putParcelable("otherDialog", otherDialog.onSaveInstanceState());
//	  }
	  //fileService = null;
	  super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  // Restore UI state from the savedInstanceState.
	  // This bundle has also been passed to onCreate.
	  if (savedInstanceState.getBoolean("showPasswordDialog")){
		  Log.d(TAG,"invoking challenge for password");
		  Log.d(TAG,"intent is: " + (Intent)savedInstanceState.getParcelable("passwordDialogIntent"));
		  dialogStore = new DialogStore((Intent)savedInstanceState.getParcelable("passwordDialogIntent"),"PasswordDialog");
	  }
//	  if (savedInstanceState.getBoolean("showOtherDialog")){
//		  otherDialog.onRestoreInstanceState(savedInstanceState.getParcelable("otherDialog"));
//		 // otherDialog.show();
//	  }
	}
	
	
	
	@Override 
	public void onPause(){
		doUnbindService();
		if (passwordDialog != null){
			passwordDialog.dismiss(); // keep track of dialogs and dismiss them, imho Android should do this 
			passwordDialog = null;
		}
		if (otherDialog != null){
			otherDialog.dismiss(); // keep track of dialogs and dismiss them, imho Android should do this
			otherDialog = null;
		}
		super.onPause();

	}
	
	@Override 
	public void onResume(){
		doBindService();
		super.onResume();
	}


	public void onStart(){
		Log.d(TAG,"started");
		super.onStart();
	}
	

	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG,"created");
		if (ScreenOrientationHandler.getScreenOrientation(getApplicationContext()) != getResources().getConfiguration().orientation){
			//Log.d(TAG,"setting orientation to " + ScreenOrientationHandler.getScreenOrientation(getApplicationContext()));
			setRequestedOrientation(ScreenOrientationHandler.getScreenOrientation(getApplicationContext()));
		}

		this.setUpActionBar();
		checkCharSet();
		doStartService();
		super.onCreate(savedInstanceState);

	}
	
	
}
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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class PasswordTimeout extends RootActivity {
	
	private PasswordTimeoutVal passwordTimeoutVal;

	public class MyOnItemSelectedListener implements OnItemSelectedListener {
		

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	      // Do nothing.
	    }
	}

	public static final String TAG = "PasswordTimeout";
	
	protected void onServiceReady() {

		super.onServiceReady();
		initialise();
	}

	

	protected void initialise() {
		
		
		Log.d(TAG,"initialising..");
		final Intent intent = new Intent();
		intent.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");
		
		final Button saveButton = (Button) findViewById(R.id.button_save);
		Log.d(TAG,"saveBUTTON IS.." + saveButton);

		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Spinner spinner = (Spinner) findViewById(R.id.spinner);
				if (PasswordTimeoutVal.getTimeoutPosition() != spinner.getSelectedItemPosition()){
					passwordTimeoutVal.setTimeout(spinner.getSelectedItemPosition());
					fileService.clearPassword();
				}
				finish();
				startActivity(intent);
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.password_timeout);

		super.setUpActionBar();
		this.passwordTimeoutVal = new PasswordTimeoutVal(PasswordTimeout.this);
		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		   
		   ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
		            this, R.array.password_timeouts, android.R.layout.simple_spinner_item);
		    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		    spinner.setAdapter(adapter);

		    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
		    	       
	       spinner.setSelection(PasswordTimeoutVal.getTimeoutPosition());

			super.onCreate(savedInstanceState);

		
	}

		

}
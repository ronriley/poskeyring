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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import keyring.Entry;
import keyring.Model;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class IndexList extends RootListActivity {

	// TODO -- MAIN TODO LIST--
	// TODO require imported file password to import file, not current file password
	// TODO use icons in menu
	// TODO warn then delete keystore on final entry
	// TODO importdb - keystore warning displayed twice when locale is set (config change)
	// TODO ENHANCEMENT - do suggestions for search
	// TODO ENHANCEMENT show date on file import/export file list
	// TODO ENHANCEMENT allow file delete of .pdb files
	// TODO ENHANCEMENT password strength indicator
	// TODO ENHANCEMENET support file backup to cloud
	// TODO move to last edited item, highlight it - PUT IN SAME HEIGHT IN LIST
	// TODO try different file formats
	// TODO tidy code
	// TODO -- END MAIN TODO LIST --

	private int ALL_CATEGORY_POSITION = 0;
	

	public static final String TAG = "IndexList";
	


	private int popView(Model model, int category) {
		 String searchString = null;

		if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
	    	Log.d(TAG,"got action search, query is: "+ getIntent().getStringExtra(SearchManager.QUERY));
	      searchString = getIntent().getStringExtra(SearchManager.QUERY);
	    }
	    else{
	    	Log.d(TAG,"caller intent is " + getIntent());
	    	searchString = null;
	    }

		int uid = getIntent().getIntExtra("entry.id", -1);

		Log.d(TAG, "asked to fetch category: " + category);

		ArrayList<Entry> entries = new ArrayList<Entry>();
		

		Log.d(TAG, "model is: " + model);

		Iterator<?> i = extracted(model);

		Entry entry = null;
		Entry soughtEntry = null;
		while (i.hasNext()) {
			entry = (Entry) i.next();

			if (entry != null
					&& (entry.getCategory() == category || category == ALL_CATEGORY_POSITION)) {
				if (searchString == null || entry.getTitle().toLowerCase().contains(searchString.toLowerCase())){
					Log.d(TAG,"matching: " + entry.getTitle() + " to " + searchString);
					entries.add(entry);
				}
				if (entry.getUniqueId() == uid) {
					soughtEntry = entry;
				}
			}
		}
		if (entries.size() == 0){
			Toast.makeText(IndexList.this, R.string.message_no_data_found, Toast.LENGTH_SHORT).show();
		}
		extracted(entries);
		setListAdapter(new ArrayAdapter<Entry>(this, R.layout.list_item,
				entries));
		return entries.indexOf(soughtEntry);
	}


	private Iterator<?> extracted(Model model) {
		return model.getElements();
	}


	private void extracted(ArrayList<Entry> entries) {
		Collections.sort(entries);
	}

		
		private AlertDialog.Builder buildNoKeystoreDialog(
				AlertDialog.Builder builder) {
			builder.setMessage(R.string.message_no_private_keyring)
					.setCancelable(true)
					.setNegativeButton(R.string.create_a_new_keyring, new DialogInterface.OnClickListener() { 
								public void onClick(DialogInterface dialog, int id) {
									Intent intent = new Intent().setClassName("com.androidron.keyring", "com.androidron.keyring.NewPrivateKeystore");

									dialog.cancel();
						        	startActivity(intent);
								}
							})
							.setPositiveButton(R.string.import_an_existing_keyring, new DialogInterface.OnClickListener() { 
								public void onClick(DialogInterface dialog, int id) {
									Intent intent = new Intent().setClassName("com.androidron.keyring", "com.androidron.keyring.ImportPdb");

									dialog.cancel();
						        	startActivity(intent);
								}
							});
			return builder;
		}

    private void askWhatToDoNoKeystore(){
    	AlertDialog.Builder builder = new AlertDialog.Builder(
				IndexList.this);
		builder = buildNoKeystoreDialog(builder);
		otherDialog = builder.show();
	}
    

	
    @Override
    public void onResume(){
    	if (serviceIsReady && !fileService.hasPrivateKeystore()){
    		askWhatToDoNoKeystore();
    	}
    	super.onResume();
    }
    
    @Override
	protected void onServiceReady() {
		super.onServiceReady();
		
    	fileService.registerIndexList(this);
    	
		if (!fileService.hasPrivateKeystore()){
			Log.d(TAG,"No keystore..");
			askWhatToDoNoKeystore();
		}

		final Model model = fileService.getModel();

		final int nPos[] = { -1 };

		if (model == null)
			return;

		Collection<String> categories = (Collection<String>) model.getDisplayCategories();

		ALL_CATEGORY_POSITION = categories.size() - 1;

		Intent callerIntent = getIntent();
		
		int nCat = callerIntent.getIntExtra("category.position",
				ALL_CATEGORY_POSITION);

		 final Spinner spinner = (Spinner) findViewById(R.id.spinner);

		ArrayAdapter<Object> spinnerAdapter  = new ArrayAdapter<Object>(this,
				android.R.layout.simple_spinner_item, categories.toArray());

		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(spinnerAdapter);
		spinner.setSelection(nCat);// results in onItemSelected being called so use this to popView
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				Log.d(TAG,"spinner item selected, pop view...");
				nPos[0] = popView(model, pos); //

				ListView lv = getListView();
				lv.setTextFilterEnabled(true);

				Log.d(TAG, "scrolling to: " + nPos[0]);
				if (nPos[0] != -1){
					lv.requestFocusFromTouch();
					lv.setSelection(nPos[0]);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}

			
		}

		);

		Log.d(TAG,"service ready pop view...");

		ListView lv = getListView();

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, final View view,
					final int position, long id) {
				final Intent myIntent = new Intent();
				myIntent.putExtra("entry.id", ((Entry) getListAdapter()
						.getItem(position)).getUniqueId());// model.getEntries().elementAt(position).getEntryId()
				myIntent.putExtra("category.position",
						spinner.getSelectedItemPosition());
				if (fileService.passwordSet()) {
					myIntent.setClassName("com.androidron.keyring",
							"com.androidron.keyring.Item");
					Log.d(TAG,
							"position: "
									+ position
									+ " entry id: "
									+ ((Entry) getListAdapter().getItem(
											position)).getUniqueId());
					
					startActivity(myIntent);
				} else {
					final Dialog dialog = new PasswordDialog(IndexList.this);
					passwordDialog = dialog;
					passwordDialogIntent = myIntent;
					dialog.setContentView(R.layout.password_dialog);
					dialog.setTitle(R.string.locked);
					final EditText password = (EditText) dialog
							.findViewById(R.id.password);
					final TextView message = (TextView) dialog
							.findViewById(R.id.message);
					Button buttonPassword = (Button) dialog
							.findViewById(R.id.button);
					Button buttonCancel = (Button) dialog
							.findViewById(R.id.button_cancel);

					buttonCancel.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							dialog.dismiss();
						}
					});

					buttonPassword.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							try {
								fileService.setPassword(password.getText()
										.toString().toCharArray());
								dialog.dismiss();
								myIntent.setClassName("com.androidron.keyring",
										"com.androidron.keyring.Item");
								Log.d(TAG,
										"position: "
												+ position
												+ " entry id: "
												+ ((Entry) getListAdapter()
														.getItem(position))
														.getUniqueId());
								dialog.dismiss();
								startActivity(myIntent);
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
		});
	}
    


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.index);
		super.onCreate(savedInstanceState);

	}
	
	@Override 
	public void onDestroy(){
		if (serviceIsReady){
			fileService.unRegisterIndexList(this);
		}
		super.onDestroy();
	}
	
	

}

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
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/***
 * importing notes about charsets:  most western Palm OS used windows-1252.
 * BUT if an emulator has been used then we don't know
 * if KeyRing.jar has been used, it uses the charset of the host machine
 * e.g. if I run KeyRing.jar on my Mac the file is stored in mac format (when  гиг (y-Umlaut)
 * renders as stored: that shows as a Џ in windows-1252 
 @author ronriley
 */

public class ImportPdb extends RootListActivity {

	private static final String TAG = "ImportPdb";

	private TextView message;
	private Spinner spinner;
	
	private ArrayList<String> charsets;

	private void importFile(String filename, final Intent indexPage) {
		message.setText(R.string.message_importing);

		try {
			String outcome = "";
			if (fileService.importFile(filename, (String)spinner.getSelectedItem())){
				outcome = getString(R.string.message_import_success);
			}else{
				outcome = getString(R.string.message_import_failure) + " "
				+ fileService.getImportErrors();
			}
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					ImportPdb.this);
			builder.setMessage(outcome)
					.setCancelable(false)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									finish();
									startActivity(indexPage);
								}
							});
			
			otherDialog = builder.show();
		} catch (Exception e) {
			message.setText(R.string.message_import_failure);
			Toast.makeText(this,getString(R.string.message_import_failure) + ": " + e.getMessage(),Toast.LENGTH_LONG).show();
			Log.w(TAG,"error import failure", e);
		}
	}

	protected void onServiceReady() {

		super.onServiceReady();
		final Intent indexPage = new Intent();
		indexPage.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");
		final Intent mainPage = new Intent();
		mainPage.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");
		message = (TextView) findViewById(R.id.message);
		message.setText(fileService.checkSDAvailable() ? R.string.sd_ok
				: R.string.pdps_error_cannot_read_SD);
		if (fileService.checkSDAvailable()) {
			
			File rootDir = Environment.getExternalStorageDirectory();
			final File downloadsDir = new File(rootDir.toString() + "/downloads");
			if (!downloadsDir.exists() || !downloadsDir.isDirectory()) {

				final AlertDialog.Builder builder = new AlertDialog.Builder(
						ImportPdb.this);
				builder.setMessage(
						R.string.downloads_directory_not_found_please_put_file_to_import_into_the_downloads_directory_on_the_sd_card_you_may_need_to_create_this_directory_first_)
						.setCancelable(true)
						.setNegativeButton(R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.dismiss();
									}
								});
				otherDialog = builder.show();

			} else {
				if (fileService.hasPrivateKeystore()) {

					// warn the user that they might delete all their data..
					final AlertDialog.Builder builder = new AlertDialog.Builder(
							ImportPdb.this);
					builder.setMessage(R.string.warn_keyfile_exists)
							.setCancelable(true)
							.setPositiveButton(R.string.yes,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int id) {
											fileService.clearPassword();
										}
									})
							.setNegativeButton(R.string.no,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int id) {
											dialog.dismiss();
											finish();
											startActivity(mainPage);
										}
									});
					otherDialog = builder.show();
				}
				// allow user to pick a character encoding
				 spinner = (Spinner) findViewById(R.id.spinner);

					ArrayAdapter<Object> spinnerAdapter  = new ArrayAdapter<Object>(this,
							android.R.layout.simple_spinner_item, charsets.toArray());

					spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					spinner.setAdapter(spinnerAdapter);
					spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

						public void onItemSelected(AdapterView<?> parent, View view,
								int pos, long id) {
							Log.d(TAG,"spinner item selected, pop view...");

							ListView lv = getListView();
							lv.setTextFilterEnabled(true);
						}

						@Override
						public void onNothingSelected(AdapterView<?> arg0) {
							
						}

						
					}

					);


				final File[] file = downloadsDir.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return (name.endsWith(".pdb")||name.endsWith(".PDB"));
					}
				});
				String[] files = new String[file.length];
				for (int i = 0; i < file.length; i++){
					files[i] = file[i].getName();
				}


				setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item,
						files));

				ListView lv = getListView();
				lv.setTextFilterEnabled(true);

				lv.setOnItemClickListener(new OnItemClickListener() {

					public void onItemClick(AdapterView<?> parent,
							final View view, final int position, long id) {

						final AlertDialog.Builder builder = new AlertDialog.Builder(
								ImportPdb.this);
						builder.setMessage(
								getString(R.string.import_) + " " + ((TextView) view).getText() + "?")
								.setCancelable(true)
								.setPositiveButton(R.string.yes,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int id) {

												ImportPdb.this.importFile(
														((TextView) view)
																.getText()
																.toString(),
														indexPage);
											}
										})
								.setNegativeButton(R.string.no,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int id) {
												dialog.cancel();
											}
										});
						otherDialog = builder.show();
					}
				});
			}
		}
	}
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.importpdb);
		super.onCreate(savedInstanceState);
		Set <String>cs = Charset.availableCharsets().keySet();
		charsets = new ArrayList<String>(cs.size());
		charsets.add(PdbFileHandlerService.DEFAULT_CHARSET);
		charsets.addAll(cs);
	}

}
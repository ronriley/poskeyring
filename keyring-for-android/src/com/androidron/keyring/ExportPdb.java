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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ExportPdb extends RootListActivity {

	private static final String TAG = "ExportPdb";

	private TextView message;
	private EditText fileName;
	private Button buttonSave;
	private Button buttonCancel;

	private void exportToFile(String filename, final Intent indexPage) {
		message.setText(R.string.message_exporting);

		try {
			fileService.exportToFile(filename);
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					ExportPdb.this);
			builder.setMessage(R.string.message_export_success)
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
			message.setText(R.string.message_export_failure);
			e.printStackTrace();
		}
	}

	protected void onServiceReady() {

		super.onServiceReady();
		final Intent indexPage = new Intent();
		indexPage.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");
		final Intent mainPage = new Intent();
		mainPage.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList"); 

		message = (TextView) findViewById(R.id.message);
		fileName = (EditText) findViewById(R.id.fileName);
		fileName.setSelection(0); 
		buttonSave = (Button) findViewById(R.id.button_save);
		buttonCancel = (Button) findViewById(R.id.button_cancel);
		
		message.setText(fileService.checkSDAvailable() ? R.string.sd_ok
				: R.string.pdps_error_cannot_read_SD);
		if (fileService.checkSDAvailable()) {
			if (!fileService.hasPrivateKeystore()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(
						ExportPdb.this);
				builder.setMessage(R.string.warn_no_keyfile_exists)
						.setCancelable(true)
						.setNegativeButton(R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
										finish();
										startActivity(mainPage);
									}
								});
				otherDialog = builder.show();
				return;
			}
			String downloadDir = Environment.getExternalStorageDirectory()
					.toString() + "/downloads/"; 
			File downLoads = new File(downloadDir);
			if (!downLoads.exists()){
				downLoads.mkdir();
				Log.d(TAG,"created " + downLoads.toString());
			}
			File file[] = downLoads.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (name.endsWith(".pdb")  || name.endsWith(".PDB"));
				}
			});
			if (file == null) file = new File[0];
			String[] files = new String[file.length];
			for (int i = 0; i < file.length; i++){
				files[i] = file[i].getName();
			}
			
			message.setText(file.length > 0 ? R.string.message_pick_export
					: R.string.message_create_export);
			
			setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item,
					files));

			ListView lv = getListView();
			lv.setTextFilterEnabled(true);

			lv.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, final View view,
						int position, long id) {
					fileName.setText(((TextView) view).getText());
				}
			});
		}
		buttonSave.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				  
				final AlertDialog.Builder builder = new AlertDialog.Builder
				  (ExportPdb.this);
				  builder.setMessage(getString(R.string.save_to_) + " "+ ((TextView)fileName).getText() +"?") 
				  .setCancelable(true)
				  .setPositiveButton(R.string.yes, new DialogInterface .OnClickListener() {
					  public void onClick(DialogInterface dialog, int id) {
						  try {
							exportToFile(((EditText) fileName).getText().toString(),indexPage);
						} catch (Exception e) {
							e.printStackTrace();
						}
					  }
				   })
				  .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					  public void onClick(DialogInterface dialog, int id) {
						  	dialog.cancel();
					  }
				  });
				  otherDialog = builder.show();
			}
		});
		buttonCancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
				startActivity(indexPage);
			}
		});
		
	}

	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.exportpdb);
		super.onCreate(savedInstanceState);
}
	

}
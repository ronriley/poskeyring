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
import java.util.Collection;

import keyring.Model;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class CategoryEditor extends RootListActivity {

	public static final String TAG = "CategoryEditor";
	

	protected void onServiceReady() {
		super.onServiceReady();
		final Model model = fileService.getModel();

	//	final int nPos[] = { -1 };

		if (model == null)
			return;

		final Intent intent = new Intent();
		intent.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");

		Collection<String> categories = (Collection<String>) model.getCategories();// .clone();

		final ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		setListAdapter(new ArrayAdapter<Object>(this, R.layout.list_item,
				categories.toArray()));
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, final View view,
					final int position, long id) {

				Intent intent = new Intent();
				intent.setClassName("com.androidron.keyring",
						"com.androidron.keyring.CategoryItem");
				if (fileService.passwordSet()) {
					intent.putExtra(getString(R.string.category), position);
					finish();
					startActivity(intent);
				} else { 
					final Dialog dialog = new PasswordDialog(
							CategoryEditor.this);
					passwordDialog = dialog;
					passwordDialogIntent = intent;
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
								Intent intent = new Intent();
								intent.setClassName("com.androidron.keyring",
										"com.androidron.keyring.CategoryItem");
								intent.putExtra(getString(R.string.category), position);
								finish();
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
		});

		final Button cancelButton = (Button) findViewById(R.id.button_cancel);

		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
				startActivity(intent);
			}
		});
		final Button newButton = (Button) findViewById(R.id.button_new);

		newButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//TODO be dry, this is copied from above this code is in the superclass...
				
				if (fileService.passwordSet()) {
					Intent intent = new Intent();
					intent.setClassName("com.androidron.keyring",
							"com.androidron.keyring.CategoryItem");
					intent.putExtra(getString(R.string.category), -1);
					finish();
					startActivity(intent);
				} else { // TODO be dry, this code common with IndexList
					final Dialog dialog = new PasswordDialog(
							CategoryEditor.this);

					passwordDialog = dialog;
					passwordDialogIntent = intent;
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
							//finish();
							dialog.dismiss();
						}
					});

					buttonPassword.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							try {
								fileService.setPassword(password.getText()
										.toString().toCharArray());
								dialog.dismiss();
								Intent intent = new Intent();
								intent.setClassName("com.androidron.keyring",
										"com.androidron.keyring.CategoryItem");
								intent.putExtra(getString(R.string.category), -1);
								finish();

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
				// position.setText(position);
			}

		});
	}

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.category);
		ListView lv = getListView();
		View footer = getLayoutInflater().inflate(R.layout.category_footer, null);
		lv.addFooterView(footer);
		super.onCreate(savedInstanceState);
}
}

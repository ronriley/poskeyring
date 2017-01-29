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
import java.io.UnsupportedEncodingException;

import keyring.Model;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class CategoryItem extends RootActivity {

	// private boolean dirty;
	
	static ProgressDialog progressDialog;
	
	final Handler mHandler = new Handler();

    // Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {

    		final Intent categoryEditor = new Intent();
    		categoryEditor.setClassName("com.androidron.keyring",
    				"com.androidron.keyring.CategoryEditor");//TODO not dry
    		progressDialog.cancel();
			finish();
			startActivity(categoryEditor);
        }
    };

	public static final String TAG = "CategoryItem";

	private State state;

	protected void onServiceReady() {

		super.onServiceReady();
		initialise();
	}

	private AlertDialog.Builder buildDelete(AlertDialog.Builder builder,
			final String category, final Intent categoryEditor) {
		builder.setMessage(R.string.warn_delete_category)
				.setCancelable(true)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								//try {
									progressDialog = ProgressDialog.show(CategoryItem.this, "", 
					                        getString(R.string.updating_password_store_please_wait_), true);
									 // Fire off a thread to do some work that we shouldn't do directly in the UI thread
							        Thread t = new Thread() {
							            public void run() {
											try {
												fileService.deleteCategory(category);
					
											} catch (Exception e) {
												e.printStackTrace();
											}
							                mHandler.post(mUpdateResults);
							            }
							        };
							        t.start();
								
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		return builder;
	}



	private AlertDialog.Builder buildCategoryNameTooLong(
			AlertDialog.Builder builder) {
		builder.setMessage(R.string.warn_category_name_too_long)
				.setCancelable(true)
				.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() { 
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		return builder;
	}

	private AlertDialog.Builder buildReAssign(AlertDialog.Builder builder,
			final String fromCategory, final String toCategory,
			final Intent categoryEditor) {
		builder.setMessage(
				String.format(getString(R.string.re_assign_entries_with_category_) , fromCategory,toCategory))
				.setCancelable(true)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								try {
									try {
										fileService.reAssignCategory(fromCategory,
												toCategory);
									} catch (UnsupportedEncodingException e) {
										e.printStackTrace();
									}
								} catch (CategoryException e) {
									e.printStackTrace();
								}
								finish();
								startActivity(categoryEditor);
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		return builder;
	}

	private AlertDialog.Builder buildCategoryRename(
			AlertDialog.Builder builder, final String fromCategory,
			final String toCategory, final int index,
			final Intent categoryEditor) {
		builder.setMessage(
				String.format(getString(R.string.re_name_category_),fromCategory ,toCategory ))
				.setCancelable(true)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								try {
									fileService.updateCategory(index,
											toCategory);
								} catch (CategoryException e) {
									e.printStackTrace();
								}
								finish();
								startActivity(categoryEditor);
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		return builder;
	}

	protected void initialise() {


		final Model model = fileService.getModel();

		final EditText nameText = (EditText) findViewById(R.id.name);

		final Intent categoryEditor = new Intent();
		categoryEditor.setClassName("com.androidron.keyring",
				"com.androidron.keyring.CategoryEditor");
		Intent callerIntent = getIntent();

		final int categoryPosition = callerIntent.getIntExtra("category", -1);
		state = categoryPosition == -1 ? State.CREATING : State.EDITING;
		final String category = state == State.CREATING ? "" : model.getCategoryName(categoryPosition);
		nameText.setText(category);
		Log.d(TAG, "category is: " + category);
		Log.d(TAG, "categoryPosition is: " + categoryPosition);
		Log.d(TAG, "state is: " + state);

		final Button deleteButton = (Button) findViewById(R.id.button_delete);
		if (categoryPosition != Model.UNFILED_CATEGORY) {

			deleteButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							CategoryItem.this);
					switch (state) {
					case EDITING:
						builder = buildDelete(builder, category, categoryEditor);
						break;
					case CREATING:
						finish();
						break;
					}

					otherDialog = builder.show();
				}
			});
		} else if (categoryPosition == Model.UNFILED_CATEGORY) {
			deleteButton.setVisibility(Button.INVISIBLE);
//			deleteButton.setOnClickListener(new OnClickListener() {
//				public void onClick(View v) {
//					finish();
//				}
//			});
		}



		final Button cancelButton = (Button) findViewById(R.id.button_cancel);

		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				finish();
				startActivity(categoryEditor);
			}
		});
		
		final Button saveButton = (Button) findViewById(R.id.button_save);
		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG,"saving, state is: " + state);
				switch(state){
				case CREATING:{
					 
							try {
								fileService.addCategory(nameText.getText().toString());
								Log.d(TAG,"saved, state is: " + state);

								finish();
								startActivity(categoryEditor);
							} catch (TooManyCategoriesException e) {
								AlertDialog.Builder builder = new AlertDialog.Builder(
										CategoryItem.this);
								builder = buildCategoryNameTooLong(builder);
								otherDialog = builder.show();
							} catch (CategoryNameTooLongException e) {
								AlertDialog.Builder builder = new AlertDialog.Builder(
										CategoryItem.this);
								builder = buildCategoryNameTooLong(builder);
								otherDialog = builder.show();
							}
							break;
				 }
				case EDITING:{
					if (nameText.getText().toString().equals(category)){
						finish();
						startActivity(categoryEditor);
						return;
					}
					if (nameText.getText().toString().trim().equals("")
							&& categoryPosition == Model.UNFILED_CATEGORY) {
						Log.d(TAG, "setting rename button to cancel");
						finish();
						startActivity(categoryEditor);


					} else if (nameText.getText().toString().trim().equals("")
							&& categoryPosition != Model.UNFILED_CATEGORY) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								CategoryItem.this);
		
							builder = buildDelete(builder, category, categoryEditor);
			
						otherDialog = builder.show();
					} else if (model.getCategories().contains(
							nameText.getText().toString())) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								CategoryItem.this);
				
							builder = buildReAssign(builder, category, nameText
									.getText().toString(), categoryEditor);
					

						otherDialog = builder.show();
					} else if (nameText.getText().length() > Model.CATEGORY_MAXLENGTH) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								CategoryItem.this);
						builder = buildCategoryNameTooLong(builder);
						otherDialog = builder.show();
					} else if (nameText.getText().toString() != category) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								CategoryItem.this);
				
							builder = buildCategoryRename(builder, category,
									nameText.getText().toString(),
									categoryPosition, categoryEditor);
						

						otherDialog = builder.show();
					}
					
				}
			}
		}
		});
	}

	/** Called when the activity is first created. */
	@Override
		public void onCreate(Bundle savedInstanceState) {
			setContentView(R.layout.category_item);
			super.onCreate(savedInstanceState);
	}


}
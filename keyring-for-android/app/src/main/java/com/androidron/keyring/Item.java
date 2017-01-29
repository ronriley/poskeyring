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
import java.util.Random;

import keyring.Entry;
import keyring.Model;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.Ostermiller.util.RandPass;

public class Item extends RootActivity implements PasswordExpiryListener {
	
	//TODO localise date display
	
	private Entry entry;
	
	private State state;
	
	private static int instanceCount;
	private int myInstNum;
		
	private static final boolean TOAST_DEBUGS = false;


	public class MyOnItemSelectedListener implements OnItemSelectedListener {
		

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {
	    	Item.this.setDirty(true); //TODO notify button, date changed
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	      // Do nothing.
	    }
	}
	
	private boolean closing;

	public static final String TAG = "Item";
	
	private void close(){
		closing = true;
		fileService.removePasswordExpiryListener(this);
		fileService.clientFinished();
		finish();
	}
	
	
	protected void onServiceReady() {

		super.onServiceReady();
		initialise();
		fileService.addPasswordExpiryListener(this);
		if (TOAST_DEBUGS){
			Toast.makeText(this, "initialised item " + myInstNum, Toast.LENGTH_SHORT).show();
		}

	}
	
	// Implementing Fisher�Yates shuffle
	// thanks to http://stackoverflow.com/questions/1519736/random-shuffling-of-an-array-in-android
	  static String shuffle(String s)
	  {
		char[] ar = s.toCharArray();
	    Random rnd = new Random();
	    for (int i = ar.length - 1; i >= 0; i--)
	    {
	      int index = rnd.nextInt(i + 1);
	      // Simple swap
	      char a = ar[index];
	      ar[index] = ar[i];
	      ar[i] = a;
	    }
	    return new String(ar);
	  }
	
	public void onPasswordExpired(){
		if (closing){
			return;
		}
		
		Log.d(TAG,"timeout *start****");
		
		this.runOnUiThread(new Runnable(){
			
		public void run(){
			final Intent intent = new Intent();
			intent.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");
	
		    final Dialog dialog = new PasswordDialog(Item.this);
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
					
	        		startActivity(intent);
					dialog.dismiss();
	
					close();
	
				}
			});
			buttonPassword.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					try {
						fileService.setPassword(password.getText().toString()
								.toCharArray());
						dialog.dismiss();
					} catch (BadPasswordException pwd) {
						password.setText("");
						message.setText(R.string.label_failed_password);
					}
				}
			});
			try{
				if (!isFinishing()){
					onCreateDialog(R.layout.password_dialog);
					dialog.setCancelable(false);
					dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
					dialog.show();
				}
			}catch(BadTokenException parentWindowGone){
				Log.d (TAG,"caught parentWindowGone BadTokenException");
				return;
			}
			Log.d(TAG,"timeout **end***");
				}});
			

	}

	private boolean isDirty() throws UnsupportedEncodingException {
		boolean bRet = false;
		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		final EditText nameText = (EditText) findViewById(R.id.name);
		final EditText accountText = (EditText) findViewById(R.id.account);
		final EditText passwordText = (EditText) findViewById(R.id.password);
		final EditText notesText = (EditText) findViewById(R.id.notes);		
		Log.d(TAG,"spinner.getSelectedItemPosition() is: " + spinner.getSelectedItemPosition());

		try{
			bRet = (entry != null && 
				!(spinner.getSelectedItemPosition() == entry.getCategory()
				&& nameText.getText().toString().equals(entry.getTitle(false))
				&& accountText.getText().toString().equals(entry.getAccount())
				&& passwordText.getText().toString().equals(entry.getPassword())
				&& notesText.getText().toString().equals(entry.getNotes()))
				|| 
				(entry == null && (nameText.getText().length() > 0
				|| accountText.getText().length() > 0
				|| passwordText.getText().length() > 0
				|| notesText.getText().length() > 0
				))
				);
		}catch(Exception unexpected){
			Toast.makeText(getApplicationContext(), "Unable to handle entry: " + unexpected.getClass().getName(), Toast.LENGTH_SHORT).show();
		}
		return bRet;

	}

   
	@SuppressWarnings("rawtypes")
	protected void initialise() {


		closing = false;
		
		final Model model = fileService.getModel();
		
	    Spinner spinner = (Spinner) findViewById(R.id.spinner);
		@SuppressWarnings("unchecked")
		ArrayAdapter adapter = new ArrayAdapter(
	            this,android.R.layout.simple_spinner_item, model.getCategories().toArray() );
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());


		final EditText nameText = (EditText) findViewById(R.id.name);
		final EditText accountText = (EditText) findViewById(R.id.account);
		final EditText passwordText = (EditText) findViewById(R.id.password);
		final TextView dateChangedText = (TextView) findViewById(R.id.date_changed);

		final EditText notesText = (EditText) findViewById(R.id.notes);
		
	

		Intent callerIntent = getIntent();
		int entryUniqueId = callerIntent.getIntExtra("entry.id", -1);
		
		state = entryUniqueId == -1 ? State.CREATING : State.EDITING;
		
		final int categoryPosition = callerIntent.getIntExtra("category.position", -1);//TODO - use string resource?
		Log.d(TAG, "passed in category position " + categoryPosition);
		entry = null;

		if (entryUniqueId > -1 && model != null) {
			Log.d(TAG, "FETCHING id " + entryUniqueId);
			entry = (Entry)model.getEntries().get(entryUniqueId);
		}

		if (entry != null) {
	    	spinner.setSelection(entry.getCategory());
			nameText.setText(entry.getTitle(false));
			try {
				accountText.setText(entry.getAccount());
			}catch (Exception decryptFailed) {
				decryptFailed.printStackTrace();
				accountText.setText(R.string.decrypt_failed);
			}
			try {
				passwordText.setText(entry.getPassword());
			}catch (Exception decryptFailed) {
				decryptFailed.printStackTrace();
				passwordText.setText(R.string.decrypt_failed);
			}
			try {
				dateChangedText.setText(entry.getDate());
			}catch (Exception decryptFailed) {
				decryptFailed.printStackTrace();
				dateChangedText.setText(R.string.decrypt_failed);
			}
			try {
				notesText.setText(entry.getNotes());
			} catch (Exception decryptFailed) {
				decryptFailed.printStackTrace();
				notesText.setText(R.string.decrypt_failed);
			}
		}
	
		
		final Intent intent = new Intent();
		intent.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");


		intent.putExtra("entry.id", entryUniqueId);
		intent.putExtra("category.position", categoryPosition);

		
		final Button saveButton = (Button) findViewById(R.id.button_save);
		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				Spinner category = (Spinner) findViewById(R.id.spinner);
				try {
					Log.d(TAG, "form is dirty is: " + isDirty());
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}

				try {
					if (isDirty()) {
						if (Entry.matchesPlaceholder(nameText.getText().toString())) {
							AlertDialog.Builder builder = new AlertDialog.Builder(
									Item.this);
							builder = buildCancel(builder,
									R.string.message_please_provide_item_title);
							otherDialog = builder.show();
							return;
						} 
						else {
							StringBuffer sb = new StringBuffer();
							sb.append(fileService.unsupportedCharacters(nameText.getText().toString()));
							sb.append(fileService.unsupportedCharacters(accountText.getText().toString()));
							sb.append(fileService.unsupportedCharacters(passwordText.getText().toString()));
							sb.append(fileService.unsupportedCharacters(notesText.getText().toString()));
							if (sb.length() > 0){
								AlertDialog.Builder builder = new AlertDialog.Builder(
										Item.this);
								builder = buildCancel(builder,
										sb.length() > 1 ? 
												String.format(getString(R.string.sorry_more_than_one_unsupported_character),sb.toString())
												:String.format(getString(R.string.sorry_one_unsupported_character),sb.toString()));
								otherDialog = builder.show();
								return;
							} 
							
							if (state == State.CREATING) {
								

								entry = fileService.createEntry(
										Entry.matchesPlaceholder(nameText.getText()
												.toString()) ? "".toCharArray()
												: nameText.getText().toString()
														.toCharArray() 
										// // DO NOT STORE #0 type placeholders for
										// blanks
										, category.getSelectedItemPosition(),
										accountText.getText().toString()
												.toCharArray()
										,passwordText.getText().toString()
												.toCharArray()
										,notesText.getText()
												.toString().toCharArray());
							}
							fileService.updateAndSaveEntry(
									entry.getUniqueId(),
									Entry.matchesPlaceholder(nameText.getText()
											.toString()) ? "".toCharArray()
											: nameText.getText().toString()
													.toCharArray()
									, accountText.getText().toString()
											.toCharArray()
									, passwordText.getText()
											.toString().toCharArray()
									, notesText
											.getText().toString().toCharArray(),
									category.getSelectedItemPosition());
						}
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				intent.putExtra("category.position", category.getSelectedItemPosition()); 
				startActivity(intent);
				close();

			}
		});
		
		final Button deleteButton = (Button) findViewById(R.id.button_delete);
		deleteButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (state == State.EDITING){
				final AlertDialog.Builder builder = new AlertDialog.Builder(Item.this);
    	    	builder.setMessage(R.string.warn_delete_entry)
    	        .setCancelable(true)
    	        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int id) {
    	            		fileService.deleteEntry(entry);
    	   				 intent.putExtra("category.position", categoryPosition ); // pass back context

							startActivity(intent);
							
    	            		//exit();
							dialog.dismiss();
							
							close();

    	            }
    	        })
    	        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int id) {
    	                 dialog.cancel();
    	            }
    	        });
    	    	otherDialog = builder.show();
			}
			 else if (state == State.CREATING) {
					finish();
			}
		}});
		
		
		final Button generateButton = (Button) findViewById(R.id.button_generate);
		
		generateButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				final int [] passwordLength = {10};
				final char[] ALL_DIGITS = "0123456789".toCharArray();
				
				final int LOWERCASE_LETTERS = 0;
				final int UPPERCASE_LETTERS = 1;
				final int DIGITS = 2;
				final int SYMBOLS = 3;
				
				//final boolean charSets[] = {true,true,true,false,false};
				final int charSets[] = {1,1,1,0,0};

				
				final Dialog dialog = new Dialog(Item.this);

				dialog.setContentView(R.layout.password_generator_dialog);//TODO need this and later layout call?
				dialog.setTitle(R.string.password);
				
				RadioButton buttonLength4 = (RadioButton) dialog
						.findViewById(R.id.radio_4);
				buttonLength4.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						passwordLength[0] = 4;
					}
				});
				
				RadioButton buttonLength8 = (RadioButton) dialog
				.findViewById(R.id.radio_8);
				buttonLength8.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						passwordLength[0] = 8;
					}
				});
				
				RadioButton buttonLength10 = (RadioButton) dialog
				.findViewById(R.id.radio_10);
				buttonLength10.setChecked(true);
				buttonLength10.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						passwordLength[0] = 10;
					}
				});
				
				RadioButton buttonLength16 = (RadioButton) dialog
				.findViewById(R.id.radio_16);
				buttonLength16.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						passwordLength[0] = 16;
					}
				});
				
				RadioButton buttonLength20 = (RadioButton) dialog
				.findViewById(R.id.radio_20);
				buttonLength20.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						passwordLength[0] = 20;
					}
				});
				
				CheckBox buttonLowercaseLetters = (CheckBox) dialog
				.findViewById(R.id.checkbox_lowercase_letters);
				buttonLowercaseLetters.setChecked(true);
				buttonLowercaseLetters.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if (((CheckBox) v).isChecked()) {
							charSets[LOWERCASE_LETTERS] = 1;

				        } else {				        
							charSets[LOWERCASE_LETTERS] = 0;
				        }
					}
				});
				
				CheckBox buttonUppercaseLetters = (CheckBox) dialog
				.findViewById(R.id.checkbox_uppercase_letters);
				buttonUppercaseLetters.setChecked(true);	
				buttonUppercaseLetters.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if (((CheckBox) v).isChecked()) {
							charSets[UPPERCASE_LETTERS] = 1;

				        } else {		
							charSets[UPPERCASE_LETTERS] = 0;

				        }
					}
				});	
				
				CheckBox buttonDigits = (CheckBox) dialog
				.findViewById(R.id.checkbox_digits);	
				buttonDigits.setChecked(true);
				buttonDigits.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if (((CheckBox) v).isChecked()) {
							charSets[DIGITS] = 1;

				        } else {	
							charSets[DIGITS] = 0;

				        }
					}
				});
				

				
				CheckBox buttonSymbols = (CheckBox) dialog
				.findViewById(R.id.checkbox_symbols);	
				buttonSymbols.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if (((CheckBox) v).isChecked()) {
							charSets[SYMBOLS] = 1;

				        } else {	
							charSets[SYMBOLS] = 0;

				        }
					}
				});
				
				Button buttonGenerate = (Button) dialog
						.findViewById(R.id.button_generate);
				
				Button buttonCancel = (Button) dialog
						.findViewById(R.id.button_cancel);

				buttonCancel.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						dialog.dismiss();
					}
				});

				buttonGenerate.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						
						int nSets = 0;
						
						RandPass randPass = null;
						
						// see how many charsets are being used
						for (int n = 0; n < charSets.length;  n++){
								nSets+= charSets[n];
						}
						// make a note of which charsets are being used
						int[] usedSets = new int[nSets];
						int i = 0;
						for (int n = 0; n < charSets.length;  n++){
							if (charSets[n] > 0){
								usedSets[i] = n;
								i++;
							}
						}
						
						// allocate a random additional number of characters
						// randomly to each used character set up to a limit of password
						// length
						if (nSets > 0){
							Random r = new Random();
							int nRemaining = (passwordLength[0] - nSets);
							while (nRemaining > 0){
								int add = r.nextInt(nRemaining +1);
								nRemaining -= add;
								charSets[usedSets[r.nextInt(nSets)]] += add;
							}
						}
						
						
						
						if (charSets[LOWERCASE_LETTERS] > 0){
							randPass = new RandPass(RandPass.LOWERCASE_LETTERS_ALPHABET);
							randPass.addRequirement(RandPass.LOWERCASE_LETTERS_ALPHABET, charSets[LOWERCASE_LETTERS]);
						}
						if (charSets[UPPERCASE_LETTERS] > 0){
							if (randPass == null){
								randPass = new RandPass(RandPass.UPPERCASE_LETTERS_ALPHABET);
							}
							else {
								randPass.addRequirement(RandPass.UPPERCASE_LETTERS_ALPHABET, charSets[UPPERCASE_LETTERS]);
							}
						}
						if (charSets[DIGITS] > 0){
							if (randPass == null){
								randPass = new RandPass(ALL_DIGITS);
							}
							else {
								randPass.addRequirement(ALL_DIGITS, charSets[DIGITS]);
							}
						}
						if (charSets[SYMBOLS] > 0){
							if (randPass == null){
							randPass = new RandPass(RandPass.SYMBOLS_ALPHABET);
							}
							else {
								randPass.addRequirement(RandPass.SYMBOLS_ALPHABET, charSets[SYMBOLS]);
							}
						}
						if (nSets == 0){
							randPass = new RandPass();
						}
						

						String passwd = randPass.getPass(nSets > passwordLength[0]? nSets : passwordLength[0]);
						passwordText.setText(shuffle(passwd));
						dialog.dismiss();
					}
				});
				
				dialog.show();
			}
		});
		
	final Button cancelButton = (Button) findViewById(R.id.button_cancel);
		
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				close();
        		startActivity(intent);
			}
		});
	
	}
	
	public void onPause(){
		closing = true;
		if (TOAST_DEBUGS){
			Toast.makeText(this, "pausing item " + myInstNum, Toast.LENGTH_SHORT).show();
		}

		if (fileService !=null){
			if (TOAST_DEBUGS){
				Toast.makeText(this, "removing item " + myInstNum, Toast.LENGTH_SHORT).show();
			}


		}
		else{
			if (TOAST_DEBUGS){
				Toast.makeText(this, "file service null for item " + myInstNum, Toast.LENGTH_SHORT).show();
			}
			
		}
		super.onPause();

	}
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.item);
		myInstNum = instanceCount++;
		if (TOAST_DEBUGS){
			Toast.makeText(this, "Creating item " + myInstNum, Toast.LENGTH_SHORT).show();
		}
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onStart(){	
		
		if (TOAST_DEBUGS){
			Toast.makeText(this, "Starting item " + myInstNum, Toast.LENGTH_SHORT).show();
		}
        super.onStart();
	}
	
	@Override
	public void onResume(){

		super.onResume();
		
		closing = false;
		if (fileService != null){
			if (!fileService.passwordSet()) onPasswordExpired(); 
			fileService.addPasswordExpiryListener(this);
			if (TOAST_DEBUGS){
				Toast.makeText(this, "added self to pwd expiry listn ", Toast.LENGTH_SHORT).show();
			}
		}
	}


	public void setDirty(boolean dirty) {
	}
	
	

}
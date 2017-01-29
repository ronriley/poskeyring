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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import keyring.Entry;
import keyring.Model;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class PdbFileHandlerService extends Service {
	
	private static final int NOTIFICATION_ID = 1;
	
	// Object for intrinsic lock
	public static final Object[] sDataLock = new Object[0];

	public static final String TAG = "PdbFileHandlerService";
	
	static final String SD_DOWNLOADS_DIR = "/downloads/";
	
	static final String LOGOUT = "logout";
	
	static ArrayList<IndexList> indexLists = new ArrayList<IndexList>();
	
	static final String SYSTEM_ENTRY="test";
	
	static final String CHARSET = "charset";
	
	public static String DEFAULT_CHARSET="windows-1252";
	
	private String importErrors;
	
	//private BackupManager backupManager;

	public String getImportErrors() {
		return importErrors;
	}

	public static  String getCharset(Context ac){
		
		SharedPreferences preferences = ac.getSharedPreferences(CHARSET,Activity.MODE_PRIVATE);
	    return preferences.getString(CHARSET, DEFAULT_CHARSET);
	}
	
	public static void storeCharset(Context ac, String cs){
		SharedPreferences preferences = ac.getSharedPreferences(CHARSET,Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
	    editor.putString(CHARSET, cs);
	    editor.commit();
	}

	//private int passwordTimeout;// = PasswordTimeout.DEFAULT_TIMEOUT_MS; 

	private boolean hasPassword = false;

	protected char SEPARATOR = '/'; // entry title separator

	// This is the object that receives interactions from clients.
	// we only serve our own app
	private  final IBinder mBinder = new LocalBinder();
	private static Model model;
	
	private static Timer passwordExpirer = new Timer("passwordExpirer", true);
	

	private ArrayList <PasswordExpiryListener> passwordExpiryListeners =  new ArrayList<PasswordExpiryListener>();

	protected void changePassword(char[] newpwd) throws FileNotFoundException, Exception {
		switch (model.getPdbVersion()) {
		case 4:
			model.convertDatabase(4, 4, getFileOutputStream(), newpwd, 0, 0);
			onCreate();
			break;
		case 5:
			Log.d(TAG,"model is format 5");
			model.convertDatabase(5, 5, getFileOutputStream(), newpwd, model.crypto.getType(), model.crypto.getIterations());
			onCreate();
			break;
		default:
			System.out.println("model is unrecognised format "
					+ model.getPdbVersion());

		}
		// Crypto newCrypt = new Crypto();
	}
	
	public String unsupportedCharacters(String s){
		Charset palmCharset = Charset.forName(Model.PALM_CHARSET);
		ByteBuffer bb = palmCharset.encode(s);
		CharBuffer cb = palmCharset.decode(bb);
		String palmString = cb.toString();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++){
			if (s.charAt(i) != palmString.charAt(i)){
				sb.append(s.charAt(i));
			}
		}
		return sb.toString();
	}
	
	public void addPasswordExpiryListener(PasswordExpiryListener i){
		if (!passwordExpiryListeners.contains(i)){
			passwordExpiryListeners.add(i);
		}
	}
	
	
	public void removePasswordExpiryListener(PasswordExpiryListener i){
		passwordExpiryListeners.remove(i);
	}

	protected void clearPassword() {
		if (!hasPrivateKeystore()) return;
		if (model.crypto == null) return;
		
		hasPassword = false;
		Log.d(TAG,"clearing pwd, model is: " + model + " model crypto is: " + model.crypto);
		try {
			model.crypto.setPassword("0000000000000000".toCharArray());
		} catch (Exception e) {
			Log.d(TAG,"" + e.getMessage().equalsIgnoreCase("Password incorrect."));
			if (e.getMessage().equalsIgnoreCase("Password incorrect.")){
				Log.d(TAG,"cleared pwd");
				clearNotification();// don't leave the password icon hanging around
			}else{
				Log.e(TAG,e.getMessage() + ":could not clear password",e);
			}
		} 
		//if (passwordExpiryListener != null){
			Iterator <PasswordExpiryListener> pwdels = passwordExpiryListeners.iterator();
			while (pwdels.hasNext()){
				try{
					((PasswordExpiryListener)pwdels.next()).onPasswordExpired();
				}catch (Exception ignored){
					ignored.printStackTrace();
				}
			}
		//}
	}
	
	private void clearNotification(){
		Log.d(TAG,"clearNotifications");

		String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		Log.d(TAG,"clearNotifications2");

		mNotificationManager.cancelAll();
		Log.d(TAG,"cancelled notifications");
	}
	
	public void clientFinished(){
		if (PasswordTimeoutVal.isOneTimePassword()){

			Log.d(TAG,"client finished clearing password");
			clearPassword();
		}
	}
	
	private void notify(int message){
		Log.d(TAG,"notifying..");
		String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		int icon =  R.drawable.unlocked;
		CharSequence tickerText = "Keyring Unlocked";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = "Keyring";
		CharSequence contentText = "Lock Keyring";

		Intent notificationIntent = new Intent();
		notificationIntent.setClassName("com.androidron.keyring", "com.androidron.keyring.PdbFileHandlerServiceSupportActivity");
		notificationIntent.putExtra(LOGOUT, true);
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
		resetPasswordTimeout(true);
		 
	}

	protected void setPassword(char[] cs) throws BadPasswordException {
		Log.d(TAG,"hello model.crypto is: " + model.crypto);
		//for load testing
//		cs = new char[1];
//		cs[0] = 'a';
		try {
			model.crypto.setPassword(cs);
			if (!PasswordTimeoutVal.isOneTimePassword()) {
				hasPassword = true;
			} else if (PasswordTimeoutVal.isOneTimePassword()){
				Log.d(TAG, "got 1 time password");
				hasPassword = false;
			}
			notify(R.string.unlocked);
			resetPasswordTimeout(true);

		} catch (Exception e) {
			hasPassword = false;
			if (e.getMessage().startsWith("Password")) {
				throw new BadPasswordException();
			} else {
				Log.d(TAG,"",e);
			}
		}

	}
	
	

	public boolean passwordSet() {
	    return hasPassword;
	}

	//TODO - is this necessary?
	public Model getModel() {
		return model;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		PdbFileHandlerService getService() {
			return PdbFileHandlerService.this;
		}
	}

	@Override
	public void onCreate() {
		if (Log.isLoggable(TAG, Log.DEBUG)){
			Toast.makeText(this, "SERVICE CREATED!!", Toast.LENGTH_SHORT).show();
		}
		Log.d(TAG,"using charset " + getCharset(this));
		model = new Model(getCharset(this));
		loadPrivateKeystore();

		Log.d(TAG,"&&&&&&&&  on create clearing password");
		clearPassword();
		new PasswordTimeoutVal(this);
		
		//backupManager = new BackupManager(this.getApplicationContext());

	}
	
	private void loadPrivateKeystore(){
		FileInputStream fis = null;
		if (hasPrivateKeystore()) {
			try {
				fis = openFileInput(RootActivity.KEYSTORE_FILENAME);
				model.loadData(fis);

			} catch (Exception e) {
				Log.d(TAG,"",e);
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
				} catch (Exception ignored) {
				}
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		clearPassword();
        // Tell the user we stopped.
        Toast.makeText(this, "SERVICE STOPPED!!", Toast.LENGTH_SHORT).show();
		Log.d(TAG,"SERVICE ON DESTROY CALLED!!!");
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	protected boolean checkSDAvailable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return true;
		}
		return false;
	}

	protected boolean checkSDWriteable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state))
			return true;
		return false;
	}
	
	private FileOutputStream getFileOutputStream() throws FileNotFoundException{
		return openFileOutput(RootActivity.KEYSTORE_FILENAME,
				Context.MODE_PRIVATE);
	}

	protected void saveFileToPrivateStore(Model model) throws Exception {
		if (model.getEntriesSize() == 0){
			Log.d(TAG,"not saving model with no entries");
			return;
		}
		synchronized(sDataLock){
			if (this.hasPrivateKeystore()){
				deleteFile(RootActivity.KEYSTORE_FILENAME);
			}
			FileOutputStream fos = getFileOutputStream();
	
			model.saveData(fos);
		}
		//backupManager.dataChanged();
	}
	
//	private void doBackup() throws Exception{
//		exportToFile("Keys-Gtkr-bk.pdb");
//	}
	
	private void exportFile(String filename) throws Exception {
		File directory = new File(Environment.getExternalStorageDirectory().toString() + SD_DOWNLOADS_DIR);
		boolean hasDirectory = directory.exists();
		if (!hasDirectory) hasDirectory = directory.createNewFile();
		if (!hasDirectory){
			throw new Exception ("Unable to create " + directory.getAbsolutePath());
		}
		filename = (directory.getAbsolutePath() + "/" + filename);
		if (!filename.endsWith(".pdb")) filename += ".pdb";
		Log.d(TAG,"saving: " + filename);
		File f = new File(filename);
	    if (!f.exists())f.createNewFile();
	    if (!f.isDirectory())
	    	model.saveData(filename);
	}
	
	protected boolean exportToFile(String filename) throws Exception {
		if (!this.hasPrivateKeystore()){
			return false;
		}
		if (!checkSDAvailable()){
			return false;
		}
		if (!checkSDWriteable()){
			return false;
		}
		exportFile(filename);
		return true;
	}

	protected void saveFileToPrivateStore() throws Exception {
		saveFileToPrivateStore(model);
	}

	protected boolean importFile(String filename, String charset) throws Exception {
		//doBackup(); // backup current keystore in case import fails
		boolean bRet = false;
		int nEntries = 0;
		Model modelNew = new Model(charset);
		
		try {
			String file = Environment.getExternalStorageDirectory().toString() + SD_DOWNLOADS_DIR + filename;
			modelNew.loadData(file);
			nEntries = modelNew.getEntriesSize();
			Log.d(TAG, "loaded " + nEntries + " entries");
			if (modelNew.isWithErrors()){
				importErrors = modelNew.showErrorEntries();
			}
			else{
				importErrors = null;
				bRet = true;
			}
			if (nEntries > 0){
				storeCharset(this,charset);
				saveFileToPrivateStore(modelNew);
				model = modelNew;
			}
		} catch (Exception e) {
			Log.d(TAG,"",e);
			throw e;
		}
		return bRet;
	}
	
//	




	protected boolean hasPrivateKeystore() {
		try {
			FileInputStream fis = openFileInput(RootActivity.KEYSTORE_FILENAME);
			try {
				fis.close();
			} catch (IOException e) {
				Log.d(TAG,"hasPrivateKeystore",e);
			}
			return true;
		} catch (FileNotFoundException nof) {
			return false;
		}
	}

	public Entry createEntry(char[] title, int category, char[] account,
			char[] password, char[] notes) {
		return model.createEntry(title, category, account,password,notes);
	}

	public boolean deleteEntry(Entry entry) {
		if (model.getEntries().entrySet().size() <= 1){
			return false;  // do not delete last entry as it holds crypto key
		}
		model.removeEntry(entry);
		try {
			saveFileToPrivateStore(model);
			fixBlankNames(model);
			return true;
		} catch (Exception e) {
			Log.d(TAG,"",e);
			return false;
		}
	}

	public void updateCategory(int index, String newName)
			throws CategoryException {

		try {
			model.updateCategory(newName, index);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		try {
			saveFileToPrivateStore(model);
		} catch (Exception e) {
			Log.d(TAG,"",e);
		}

	}

	public void addCategory(String newName)
			throws CategoryNameTooLongException, TooManyCategoriesException {

		try {
			model.addCategory(newName);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			Log.d(TAG,"",e1);

		}
		try {
			saveFileToPrivateStore(model);
		} catch (Exception e) {
			Log.d(TAG,"",e);
		}

	}

	public void deleteCategory(String category) throws CategoryException, UnsupportedEncodingException {
		try {
			model.removeCategory(category);
		} catch (UnsupportedEncodingException e) {
			Log.d(TAG,"",e);
		}
		if (model.getEntries().size() < 1) return;
		Entry entry = null;
		Iterator<?> i = model.getEntries().values().iterator();// TODO find a
															// quicker way
		while (i.hasNext()) {
			entry = (Entry) i.next();
			updateEntry(entry.getUniqueId(), entry.getTitle().toCharArray(),
					entry.getAccount().toCharArray(), entry.getPassword()
							.toCharArray(), entry.getNotes().toCharArray(),
					entry.getCategory());
		}
		try {
			saveFileToPrivateStore(model);
		} catch (Exception e) {
			Log.d(TAG,"deleteCategory",e);
		}
	}

	public void reAssignCategory(String fromCategory, String toCategory)
			throws CategoryException, UnsupportedEncodingException {
		model.reAssignCategory(fromCategory, toCategory);
		if (model.getEntries().size() < 1) return;

		Entry entry = null;
		Iterator<?> i = model.getEntries().values().iterator();// TODO find a
															// quicker way
		while (i.hasNext()) {
			entry = (Entry) i.next();
			updateEntry(entry.getUniqueId(), entry.getTitle().toCharArray(),
					entry.getAccount().toCharArray(), entry.getPassword()
							.toCharArray(), entry.getNotes().toCharArray(),
					entry.getCategory());
		}
		try {
			saveFileToPrivateStore(model);
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.d(TAG,"", ex);
		}
	}

	public void updateAndSaveEntry(int id, char[] title, char[] account,
			char[] password, char[] notes, int categoryIndex) {
		try {
			updateEntry(id, title, account, password, notes, categoryIndex);
			saveFileToPrivateStore(model);
			fixBlankNames(model);
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.d(TAG, "",ex);
		}
	}
	
	private void fixBlankNames(Model model){
		Iterator<?> i = model.getElements();
		Entry entry = null;
		int emptyTitle = 0;
		while (i.hasNext()){
			entry = (Entry) i.next(); 
			if (Entry.matchesPlaceholder(entry.getTitle()) ||  entry.getTitle().equals("") ){
				entry.setTitle("#" + emptyTitle++);
			}
		}
	}

	public void updateEntry(int id, char[] title, char[] account,
			char[] password, char[] notes, int categoryIndex) {
		model.updateEntry(id,title, account,password, notes, categoryIndex);
	}
	
	private void resetPasswordTimeout(boolean newPwd){
//		if (newPwd){
//			passwordExpirer.cancel();
//			passwordExpirer.purge();
//		}
		long now = new Date().getTime();
		passwordExpirer.cancel();
		passwordExpirer.purge();
		passwordExpirer = new Timer();
		passwordExpirer.schedule(new TimerTask() {
			public void run() {
				//if (hasPassword){
				Log.d(TAG,"reset password timeout clearing password");
					PdbFileHandlerService.this.clearPassword();
				//}
			}
		}, new Date(now + PasswordTimeoutVal.getTimeout()));
	}

	public void onUserInteraction() {
		if (hasPassword){
			resetPasswordTimeout(false);
		}
		
	}
	


	public void createNewModel(char[] password) {
		try{
			model = new Model(getCharset(this));
			Model.writeNewDatabase(getFileOutputStream());
			loadPrivateKeystore();
			setPassword("test".toCharArray());
			changePassword(password);
			Iterator<?> i = model.getElements();
			((Entry)i.next()).setTitle(SYSTEM_ENTRY);
//			i = al.iterator();
//			while (i.hasNext()){
//				model.removeEntry((Entry)i.next());
//			}
			saveFileToPrivateStore();
			closeRegisteredIndexLists();
		}catch (Exception e){
			Log.d(TAG,"failed to create new model",e);
		}
			
	}
	
	public void registerIndexList(IndexList il){
		indexLists.add(il);
	}
	
	public void unRegisterIndexList(IndexList il){
		indexLists.remove(il);
	}
	
	/**can accumulate a few instances of IndexList, sometimes need
	 * to clear them all out, e.g. when importing a new keyring
	 */
	public void closeRegisteredIndexLists(){
		Iterator<IndexList> i = indexLists.iterator();
		IndexList il = null;
		while (i.hasNext()){
			il = i.next();
			il.finish();
			indexLists.remove(il);
		}
	}
	
}

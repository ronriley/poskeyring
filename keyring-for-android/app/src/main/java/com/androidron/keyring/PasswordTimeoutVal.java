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
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public  class PasswordTimeoutVal {
	

	private static final String TIMEOUT = "PasswordTimeoutVal";
	/**position of value in TIMEOUTS, i.e. 1 minute*/
	private static final int DEFAULT_TIMEOUT = 3;//TODO - check consistent with palm


	private static final int SECOND = 1000;
	private static final int MINUTE = 60 * SECOND;
	private static final int[] TIMEOUTS = {0,15 * SECOND, 30 * SECOND,1 * MINUTE,5 * MINUTE};//msecs
	//private static final int DEFAULT_TIMEOUT_MS = TIMEOUTS[DEFAULT_TIMEOUT];
	
	private static int timeout;
	private static int timeoutPosition;
	
	public static int getTimeoutPosition() {
		return timeoutPosition;
	}

	private Context context;
	
	public PasswordTimeoutVal(Context a){
		context = a;
		setTimeout(getPersistedTimeout());
	}
	
	private  int getPersistedTimeout(){
		SharedPreferences preferences = context.getSharedPreferences(TIMEOUT,Activity.MODE_PRIVATE);
	    timeoutPosition =  preferences.getInt(TIMEOUT, DEFAULT_TIMEOUT);
	    return timeoutPosition;
	}
	
	
	
	private  void storePersistedTimeout(int n){
		if (n < 0 || n >= TIMEOUTS.length){
			//Log.e(TAG,"index out of range: " + n + " should be between 0 and " + (TIMEOUTS.length -1));
			return;
		}
		SharedPreferences preferences = context.getSharedPreferences(TIMEOUT,Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
	    editor.putInt(TIMEOUT, n);
	    editor.commit();
	}

	public  void setTimeout(int timeoutIndex) {
		storePersistedTimeout(timeoutIndex);
		timeout = TIMEOUTS[timeoutIndex];
	}

	/* if user has set a 1 time only password expire it 
	 * after a minute
	 */
	public static int getTimeout() {
		return timeout == 0 ? MINUTE : timeout;
	}
	
	public static boolean isOneTimePassword(){
		return timeout == 0;
	}

		

}
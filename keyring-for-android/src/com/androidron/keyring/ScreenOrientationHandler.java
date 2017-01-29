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
import android.content.pm.ActivityInfo;

/**No GUI, just to help pass notification to PdbFileHandlerService
 * from notifications, probably a better way to do this...
 * @author ronriley
 *
 */
public  class ScreenOrientationHandler{
	
	static final String TAG="ScreenOrientationHandler";
	

	private static int screenOrientation = Integer.MAX_VALUE;
	static final String ORIENTATION="orientation";
	static private boolean justToggled = false;
	

	
	private static  int getPersistedOrientation(Context ac){
		
		SharedPreferences preferences = ac.getSharedPreferences(ORIENTATION,Activity.MODE_PRIVATE);
	    return preferences.getInt(ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	private static void storePersistedOrientation(Context ac, int n){
		SharedPreferences preferences = ac.getSharedPreferences(ORIENTATION,Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
	    editor.putInt(ORIENTATION, n);
	    editor.commit();
	}
	
	public static int getScreenOrientation(Context ac) {
		if (screenOrientation == Integer.MAX_VALUE){
			screenOrientation = getPersistedOrientation(ac);
		}
		justToggled = false;
		return screenOrientation;
	}
	
	public static int toggleScreenOrientation(Context ac){
		screenOrientation = getScreenOrientation(ac) == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? 
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE 
				:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		storePersistedOrientation(ac,screenOrientation);
		justToggled = true;
		return screenOrientation;
	}

	public static boolean newOrientation() {
		return justToggled;
	}
	

}
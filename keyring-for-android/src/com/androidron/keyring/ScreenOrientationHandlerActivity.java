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
import android.content.Intent;

/**No GUI, just to help pass notification to PdbFileHandlerService
 * from notifications, probably a better way to do this...
 * @author ronriley
 *
 */
public class ScreenOrientationHandlerActivity  extends Activity/*implements PasswordExpiryListener*/ {
	
	static final String TAG="ScreenOrientationHandlerActivity";
	
	//private ScreenOrientationHandler screenOrientationHandler;
	
//	private void setOrientation(Configuration c, int n){
//		Configuration config = new Configuration(c);
//		config.orientation = n;
//		DisplayMetrics metrics = new DisplayMetrics();
//		getWindowManager().getDefaultDisplay().getMetrics(metrics);
//		this.getResources().updateConfiguration(config, metrics);
////		config.setTo(config);
//	}

	/**@Override*/
	public void onStart(){
		super.onStart();
		//screenOrientationHandler = new ScreenOrientationHandler();
		ScreenOrientationHandler.toggleScreenOrientation(getApplicationContext());
//		Configuration currentConfig = this.getResources().getConfiguration();
//		switch (currentConfig.orientation)
//		{
//		case Configuration.ORIENTATION_PORTRAIT:
//		  setOrientation(currentConfig, Configuration.ORIENTATION_LANDSCAPE);
//		  break;
//		default:
//		  setOrientation(currentConfig, Configuration.ORIENTATION_PORTRAIT);
//		  break;
//		}
		Intent intent = new Intent();
		intent.setClassName("com.androidron.keyring", "com.androidron.keyring.IndexList");
		startActivity(intent);
		finish();
	}


	

}
package se.miun.mediasense.call;

import java.lang.reflect.Method;

import com.android.internal.telephony.ITelephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallHandler extends BroadcastReceiver {

	private ITelephony iTelephony;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		TelephonyManager telMan = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
	    try{
	    	Class<?> c = Class.forName(telMan.getClass().getName());
	    	Method m = c.getDeclaredMethod("getITelephony");
	    	m.setAccessible(true);
	    	iTelephony = (ITelephony) m.invoke(telMan);
	    } catch (Exception e) {
			// TODO: handle exception
		}
		
		if(prefs.getBoolean("mute_setting", false)){
			try {
				iTelephony.silenceRinger();
				Log.i("RECEIVER", "MUTE CALL");
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
			
		if(prefs.getBoolean("reject_setting", false)){
			try {
				iTelephony.endCall();
				Log.i("RECEIVER", "REJECT CALL");
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

}

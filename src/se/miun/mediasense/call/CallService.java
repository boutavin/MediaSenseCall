package se.miun.mediasense.call;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class CallService extends Service {

	private boolean mute;
	private boolean reject;
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i("SERVICE-onBIND", "mute: "+mute+"; reject: "+reject);  
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		boolean[] extras = intent.getBooleanArrayExtra("call_settings");
		mute = extras[0];
		reject = extras[1];
		Toast.makeText(this, "mute: "+mute+"; reject: "+reject, Toast.LENGTH_LONG).show();
//		Log.i("SERVICE-ONSTART", "mute: "+mute+"; reject: "+reject);
		return 1;
	}	
	

}

package se.miun.mediasense.call;

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/*
 * This class extends PhoneStateListener and behave to a call state change.
 * After that a call has ended, the source activity is restarted with the ListView at its previous position
 * 
 */
public class CallListener extends PhoneStateListener {

	private Context context;
	private int position;
	private boolean hasCalled = false;
	
	public CallListener(Context _context, int _position){
		context = _context;
		position = _position;
	}
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		if(state == TelephonyManager.CALL_STATE_OFFHOOK)
			hasCalled = true;
		// when call state is idle, the position of ListView is put as an extra and the activity is restarted 
		if(state == TelephonyManager.CALL_STATE_IDLE)
			if(hasCalled){
				Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
				intent.setAction("CALL_STATE_IDLE");
				intent.putExtra("position", position);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				context.startActivity(intent);
				
				hasCalled = false;
			}
	}
	
}

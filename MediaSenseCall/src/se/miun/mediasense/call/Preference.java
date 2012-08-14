package se.miun.mediasense.call;

import se.miun.mediasense.call.R;
import android.os.Bundle;

public class Preference extends android.preference.PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}

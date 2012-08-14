package se.miun.mediasense.call;

import java.util.Date;
import java.util.HashMap;

import se.miun.mediasense.addinlayer.extensions.publishsubscribe.PublishSubscribeExtension;
import se.miun.mediasense.addinlayer.extensions.publishsubscribe.SubscriptionEventListener;
import se.miun.mediasense.disseminationlayer.disseminationcore.DisseminationCore;
import se.miun.mediasense.disseminationlayer.disseminationcore.GetEventListener;
import se.miun.mediasense.disseminationlayer.disseminationcore.GetResponseListener;
import se.miun.mediasense.disseminationlayer.disseminationcore.ResolveResponseListener;
import se.miun.mediasense.interfacelayer.MediaSensePlatform;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MediaSenseCallActivity extends ListActivity implements OnClickListener, GetEventListener, ResolveResponseListener, GetResponseListener, OnSharedPreferenceChangeListener, SubscriptionEventListener {
    
	private LinearLayout header;
	private TextView txtName, txtNumber, txtStatus; // Name of the user, Phone number of the user, Status of the user
	private String idCal = "1", status; // Default calendar ID, User occupation status (BUSY or FREE)
	private ContentResolverQueryHandler query; // Handler for Contacts and Calendars queries
	private SimpleCursorAdapter adapter; // SimpleCursorAdapter for ListView
	private HashMap<String, String> statusMap; // HashMap - key: uci, value:status
	private SharedPreferences prefs;
	// MediaSense Platform Application Interfaces
	private MediaSensePlatform platform;
	private DisseminationCore core;
	private PublishSubscribeExtension pse;
	private String UCI;
    //////////////////////////////////////////
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        statusMap = new HashMap<String, String>();
        
        // Create components
        header = (LinearLayout) findViewById(R.id.header);
        header.setOnClickListener(this); // Set the whole header to be clickable
        txtName = (TextView) findViewById(R.id.headerName); 
        txtNumber = (TextView) findViewById(R.id.headerNumber);
        txtStatus = (TextView) findViewById(R.id.headerStatus);
        ///////////////////////////////////////
        
        // Header initialization
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        txtName.setText(prefs.getString("user_name", "Alexandre"));
        txtNumber.setText(prefs.getString("user_number", "0701111111"));
        ///////////////////////////////////////
        
        // MediaSense Platform initialization
        MediaSenseManager msManager = new MediaSenseManager();
        platform = msManager.getPlatform();
        core = msManager.getCore();
        UCI = FormatHandler.formatPhoneNumber(txtNumber.getText().toString())+"@mscall.se/status";
        msManager.registerUCI(UCI);
        core.setGetEventListener(this); // Set the event listeners
        core.setResolveResponseListener(this); // Set the response listeners
        core.setGetResponseListener(this);
        msManager.loadPubSubExtension();
        pse = msManager.getPubSubExt();
        pse.setSubscriptionEventListener(this);
        //////////////////////////////////////
        
        if(!isOnline()){
        	AlertDialog.Builder dialog = new AlertDialog.Builder(this);
	        dialog.
	        setMessage("No Internet Connection!").
			setNeutralButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).show();
        }
        
        query = new ContentResolverQueryHandler(this.getContentResolver());
        setListAdapter(getContactCursorAdapter()); // Populate the ListView thanks to the SimpleCursorAdapter for contacts
        
        // PhoneStateListener to monitor the state of the phone call, and restart this activity when call ends
//		CallListener callListener = new CallListener(MediaSenseCallActivity.this, getListView().getFirstVisiblePosition());
//		TelephonyManager telephonyManager = (TelephonyManager) MediaSenseCallActivity.this.getSystemService(Context.TELEPHONY_SERVICE);
//		telephonyManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);
        
        // If the activity is started after that a call has been ended, the position of the ListView will be set back as it was
        if(getIntent().getAction().equalsIgnoreCase("CALL_STATE_IDLE"))
        	getListView().setSelection(getIntent().getIntExtra("position", 0)); // Get position from Intent's extra
		 
        checkForAvailability(idCal); // Check for the availability of the user and set is status accordingly (BUSY or FREE)

        displayNotification();
    }
    
    // When the SharedPreference has changed, we update the TextViews with the new name and number 
    @Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
    	if(key.equalsIgnoreCase("user_name"))
    		txtName.setText(sharedPreferences.getString("user_name", "Alex"));
    	if(key.equalsIgnoreCase("user_number"))
    		txtNumber.setText(FormatHandler.formatPhoneNumber(sharedPreferences.getString("user_number", "070XXXXXXX")));
    	displayNotification();
	}
    
    /*
     * Get SimpleCursorAdapter with all the user's Contacts name and number
     * 
     * @return	SimpleCursorAdapter holding the names and the numbers of the user's Contacts
     */
    private SimpleCursorAdapter getContactCursorAdapter(){
    	// Cursor holding all the contacts' ID, names and phone numbers w/ no duplicates and ordered alphabetically
    	Cursor contacts;
    	if(isOldVersion())
    		contacts = query.contactQuery(); // For versions above 2.2
    	else
    		contacts = query.contactQueryNew(); // For versions above 4.0
	    startManagingCursor(contacts); // Let the system manage the cursor to close it at the appropriate time
	    
	    String[] from = { 	ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, 
							ContactsContract.CommonDataKinds.Phone.NUMBER,
							ContactsContract.CommonDataKinds.Phone._ID};
	    int[] to = {R.id.contactName, R.id.contactNumber, R.id.contactStatus};
	    
	    // Create SimpleCursorAdapter to hold the names and the phone numbers contained in the Contacts cursor
	    adapter = new SimpleCursorAdapter(this, R.layout.item_contact, contacts, from, to);

	    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() { // Get and assign the contact status for every view displayed in the ListView / Format phone number
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				String formattedNumber = FormatHandler.formatPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)));
				// Format the number
				if(columnIndex == cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)){
					((TextView) view.findViewById(R.id.contactNumber)).setText(formattedNumber);
					return true;
				}
				// Execute an asynchronous task to fetch the status
				if(columnIndex == cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)){
					TextView contactStatusTV = (TextView)view.findViewById(R.id.contactStatus);
					if(isOnline()){
						contactStatusTV.setText("loading...");
						contactStatusTV.setTextColor(Color.GRAY);
						new ContactStatusAsyncTask(view, formattedNumber, false, getApplicationContext()).execute();
					}
					return true;
				}
				return false;
			}
		});
	    ////////////////////////////////////////
	    
	    return adapter;
    }
    
    /* Perform Asynchronous task to resolve and get the status of a specific contact view
     * Show AlertDialog for confirm a call if ListItemClicked
     * 
     * Param: current View
     * Result: current View
     * 
     * @param view			View to perform the change of status
     * @param contactNumber	Number to resolve
     * @param showDialog	Show dialog after completion (true) or not (false)
     * @param context		Current Context
     * 
     * @see					Dialog if requested
     * 
     */
    private class ContactStatusAsyncTask extends AsyncTask<Void, Void, View>{
		View view;
		String contactName, contactNumber, contactStatus, uci, message, receivedStatus = "UNKNOWN";
		Context context;
		boolean showDialog;
		
		public ContactStatusAsyncTask(View view, String contactNumber, boolean showDialog, Context context) {
			this.view = view;
			this.contactNumber = contactNumber;
			this.showDialog = showDialog;
			this.context = context;
		}
		
    	@Override
		protected View doInBackground(Void... params) {
			uci = contactNumber+"@mscall.se/status";
			core.resolve(uci); // Resolve/get call to the correct UCI
			// When the GET response is received, the AsyncTask is completed
			while(!statusMap.containsKey(uci)){	
				// while the uci is not contained in the status HashMap, the AsyncTask will wait
			}
			receivedStatus = statusMap.get(uci);
			statusMap.remove(uci);
			return view;
		}
		protected void onPostExecute(View view) {
			FormatHandler.assignRightStatusAndColor(view, true, receivedStatus);
			
			if(showDialog){
				contactName = ((TextView) view.findViewById(R.id.contactName)).getText().toString(); 		// Get name
		    	contactStatus = ((TextView) view.findViewById(R.id.contactStatus)).getText().toString(); 	// Get status
		    	
		    	// Set message for AlertDialog according to the status
		    	if(contactStatus.equalsIgnoreCase("FREE"))
		    		message = contactName+ " appears to be available right now.\n\nDo you want to call him/her?";
		    	else if(contactStatus.equalsIgnoreCase("BUSY"))
		    		message = contactName+ " appears to be busy right now.\nDo you really want to call him/her?\n\nNote: Your call might be rejected!";
		    	else if(contactStatus.equalsIgnoreCase("NO MS"))
		    		message = contactName+ " appears to not have MediaSenseCall!\nInvite him/her to join the community!\n\nDo you want to call him/her?";
		    	else
		    		message = "There is currently no information regarding "+contactName+"'s status.\n\nDo you still want to call him/her?";
		    	
				// AlertDialog to confirm call
		    	AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		        dialog.setTitle("Call " +contactName+ " (" + contactNumber + " )").
		        setMessage(message).
				setPositiveButton("Yes, call!", new DialogInterface.OnClickListener() { // Give call
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+contactNumber));
						startActivity(callIntent);
						dialog.dismiss();
					}
				}).
				setNegativeButton("No, I'll try later", new DialogInterface.OnClickListener() { // Dismiss AlertDialog
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
			}
		}
    }
    
    // Get status of the selected contact and display Dialog to confirm a call
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	new ContactStatusAsyncTask(v, ((TextView)v.findViewById(R.id.contactNumber)).getText().toString(), true, this).execute();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	statusMap.clear();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	checkForAvailability(idCal);
    }

    // Triggered when the header is clicked on
	@Override
	public void onClick(View v) {
		// Create a AlertDialog w/ "Call control", "Change calendar" and "Change status" as choices
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Options")
        .setItems(new String[] {"Change calendar", "Change status", "Call settings"}, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0){ // When "Change calendar" is selected
					changeCalendar();
				}
				else if(which == 1){ // When "Change status" is selected
					changeStatus();
				} 
				else if(which == 2){ // When "Call settings" is selected
					startActivity(new Intent(getApplicationContext(), Preference.class)); // start Preference Activity
				}
			}
		}).show();
	}
	
	/*
	 * Check the availability of the user given a specific calendar ID
	 * Check if the status has changed 
	 * 
	 * @param id	The ID of the calendar to check
	 * @result		True or false, if the status has changed or not
	 * @see			Status change
	 */
	private boolean checkForAvailability(String id){
		String _status = "FREE";
		Cursor events;
		if(isOldVersion()) // For versions above 2.2
	        events = query.calendarEventsQuery(id);
		else // For versions above 4.0
			events = query.calendarEventsQueryNew(id);
			
		startManagingCursor(events); // Let the system manage the cursor for us (no need to close it ourselves)
		Date currentTime = new Date(), begin, end; // Date of the current time, Date for the beginning and the end of a calendar event
		// As soon as the currentTime is within the interval [begin, end], the user status is changed to BUSY
		while (events.moveToNext() && _status.equalsIgnoreCase("FREE")) {
			begin = new Date(events.getLong(0));
        	end = new Date(events.getLong(1));
        	if(begin.before(currentTime) && end.after(currentTime))
        		_status = "BUSY";
		}
        status = _status;
        pse.notifySubscribers(UCI, status);
        return FormatHandler.assignRightStatusAndColor(txtStatus, false, status);
	}
	
	private void changeCalendar(){
		Cursor cal;	// Cursor holding all the calendars 
		if(isOldVersion())
			cal = query.calendarsQuery();
		else
			cal = query.calendarsQueryNew();
		final Cursor calendars = cal;
		AlertDialog.Builder showCalendars = new AlertDialog.Builder(MediaSenseCallActivity.this);
		// Populate the AletDialog with the calendars cursor and then show them so the user can select one
		showCalendars.setSingleChoiceItems(calendars, -1, calendars.getColumnName(1), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				calendars.moveToPosition(which);
				idCal = calendars.getString(0); // Select calendar
				// Check for availability, change status if needed and if status changed --> notify MediaSense
				checkForAvailability(idCal);
				dialog.dismiss();
			}
		}).show();
	}
	
	private void changeStatus(){
		AlertDialog.Builder showChoiceForStatus = new AlertDialog.Builder(MediaSenseCallActivity.this);
		showChoiceForStatus.setTitle("Change status").setMessage("What do you want to change your status to?").
		setPositiveButton("BUSY", new DialogInterface.OnClickListener() { // Change status to BUSY
			@Override
			public void onClick(DialogInterface dialog, int which) {
				status = "BUSY";
				pse.notifySubscribers(UCI, status);
				FormatHandler.assignRightStatusAndColor(txtStatus, false, status);
				displayNotification();
				dialog.dismiss();
			}
		}).
		setNegativeButton("FREE", new DialogInterface.OnClickListener() { // Change status to FREE
			@Override
			public void onClick(DialogInterface dialog, int which) {
				status = "FREE";
				pse.notifySubscribers(UCI, status);
				FormatHandler.assignRightStatusAndColor(txtStatus, false, status);
				displayNotification();
				dialog.dismiss();
			}
		}).show();
	}

	// When a response from the GET call is received, the received value is assigned to 'receivedStatus' and 'received' is set to true
	// 	--> the AsyncTask, that triggered the GET call, is stopped and the right status is updated if needed
	@Override
	public void getResponse(String uci, String value) {
		statusMap.put(uci, value);
	}

	// Perform GET call
	@Override
	public void resolveResponse(String uci, String ip) {
		if(!ip.equalsIgnoreCase("null")){
			core.get(uci, ip);
			if(!statusMap.containsKey(uci))
				pse.startSubscription(uci, ip);
		} else
			statusMap.put(uci, "NO MS");
	}
	
	// When a GET request is received --> send back the status value to the source of the call
	@Override
	public void getEvent(String source, String uci) {
		platform.getDisseminationCore().notify(source, uci, status);
	}
	
	@Override
	public void subscriptionEvent(String uci, String value) {
		String[] contactNumber = uci.split("@");
		performToast("Contact with number "+contactNumber[0]+" is now "+value+"!");
	}
	
	/* 
	 * Perform Toast on UI thread
	 *
	 * @param _text		String to be display in a Toast
	 * @see				Toast with text 			
	 */
	private void performToast(String _text){
		final String text = _text;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MediaSenseCallActivity.this, text, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	// OptionsMenu when the MENU button is pressed
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 1, 1, "Calendars").setIcon(android.R.drawable.ic_menu_my_calendar); // Select calendars
		menu.add(0, 2, 2, "Status").setIcon(R.drawable.ic_menu_cc); // Change status
		menu.add(0, 3, 3, "Settings").setIcon(android.R.drawable.ic_menu_preferences); // Settings
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case 1: // Calendars selected
				changeCalendar();
				break;
			case 2: // Status selected
				changeStatus();
				break;
			case 3: // Settings selected
				startActivity(new Intent(getApplicationContext(), Preference.class));
				break;
		}
		return true;
	}
	
	private void displayNotification(){
		NotificationManager notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_launcher, 
        		"Your status: "+status+"\nSettings: Mute ("+prefs.getBoolean("mute_setting", false)+") - Reject ("+prefs.getBoolean("reject_setting", false)+")",
        		System.currentTimeMillis());
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, new Intent(this, MediaSenseCallActivity.class), 0);
        notification.setLatestEventInfo(this, "MediaSense Call", 
        		status+": Mute ("+prefs.getBoolean("mute_setting", false)+") - Reject ("+prefs.getBoolean("reject_setting", false)+")"
        		, pendInt);
        notMan.notify(101, notification);
	}
	
	public boolean isOldVersion(){
		return Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO && 
				Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}
	
	public boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

	    return cm.getActiveNetworkInfo() != null && 
	       cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}
	
}
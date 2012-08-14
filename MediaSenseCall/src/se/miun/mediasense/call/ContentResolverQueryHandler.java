package se.miun.mediasense.call;

import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.util.Log;

public class ContentResolverQueryHandler {

	private ContentResolver contentResolver;
	
	public ContentResolverQueryHandler(ContentResolver contentResolver){
		this.contentResolver = contentResolver;
	}
	
	/* For versions above 2.2
	 * 
	 * Cursor holding all the contacts' ID, names and phone numbers w/ no duplicates and ordered alphabetically
	 *
	 * @return	Cursor
	 */
	public Cursor contactQuery(){
		return contentResolver.query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
				new String[] { ContactsContract.CommonDataKinds.Phone.CONTACT_ID, 
								ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, 
								ContactsContract.CommonDataKinds.Phone.NUMBER,
								ContactsContract.CommonDataKinds.Phone._ID}, 
				ContactsContract.CommonDataKinds.Phone.TYPE+ "="+ ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + ") GROUP BY ("+ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
				null, 
				ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+ " ASC");
	}
	
	/* For versions above 4.0
	 * 
	 * Cursor holding all the contacts' ID, names and phone numbers w/ no duplicates and ordered alphabetically
	 *
	 * @return	Cursor
	 */
	public Cursor contactQueryNew(){
		return contentResolver.query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
				new String[] { ContactsContract.CommonDataKinds.Phone.CONTACT_ID, 
								ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, 
								ContactsContract.CommonDataKinds.Phone.NUMBER,
								ContactsContract.CommonDataKinds.Phone._ID}, 
				null, 
				null, 
				ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+ " ASC");
	}
	
	/* For versions above 2.2
	 * 
	 * Cursor holding all the calendars
	 * 
	 *  @return Cursor
	 */
	public Cursor calendarsQuery(){
		return contentResolver.query(Uri.parse("content://com.android.calendar/calendars"), 
        		new String[] { "_id", "displayName", "selected" }, null, null, null);
	}
	
	/* For versions above 4.0
	 * 
	 * Cursor holding all the calendars
	 * 
	 *  @return Cursor
	 */
	public Cursor calendarsQueryNew(){
		return contentResolver.query(CalendarContract.Calendars.CONTENT_URI, 
				new String[] {CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.VISIBLE}, null, null, null);
	}
	
	/* For versions above 2.2
	 * 
	 * Create a Cursor holding the calendar events including in the interval [-2days, 1day]
	 * 
	 * @return Cursor
	 */
	public Cursor calendarEventsQuery(String id){
		Uri.Builder uriBuilder = Uri.parse("content://com.android.calendar/instances/when").buildUpon(); // Build Uri for calendar instances
		Date currentTime = new Date(); // Date of the current time
        ContentUris.appendId(uriBuilder, currentTime.getTime() - 2*DateUtils.DAY_IN_MILLIS); // Include events from 2 days ago ... 
        ContentUris.appendId(uriBuilder, currentTime.getTime() + 2*DateUtils.DAY_IN_MILLIS); // ... to the couple upcoming days
        String[] eventProjection = new String[] {
        		"begin",
        		"end" };
        String selection = "Calendars._id="+id;
        String sortOrder = "startDay ASC, startMinute ASC";
        
        return contentResolver.query(uriBuilder.build(), eventProjection, selection, null, sortOrder);
	}
	
	/* For versions above 4.0
	 * 
	 * Create a Cursor holding the calendar events including in the interval [now, so on[
	 * 
	 * @return Cursor
	 */
	public Cursor calendarEventsQueryNew(String id){
		Uri uri = CalendarContract.Events.CONTENT_URI;	 
		String[] eventProjection = new String[] {
				CalendarContract.Events.DTSTART,
				CalendarContract.Events.DTEND };
		String selection = "calendar_id="+id;
		
		return contentResolver.query(uri,eventProjection,selection, null, null);
	}
	
}

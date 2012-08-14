package se.miun.mediasense.call;

import se.miun.mediasense.call.R;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

public class FormatHandler {

	/*
     *  Format phone number by removing dashes and pluses
     *  
     *  @ param number	String number to format
     *  @ return		String formatted number 
     */
    public static String formatPhoneNumber(String number){
    	return number.replace("-", "").replace("+", "00");
    }
	
	/*
	 * Assign the right status and the right color for a specific view (header or contact)
	 * 
	 * @param view			View from which to perform the status/color assignment
	 * @param contact		Boolean, true if the view is a 'contact' view or false if the view is the 'header'
	 * @param currentStatus	String representing the current status assigned to the view
	 * @param newStatus		String representing the new status to assign to the view
	 * 
	 * @return 				Boolean, true if the assignment must be performed or false if no change must be done
	 * 
	 * @see					Right status and color assigned to the specific view		
	 */
	public static boolean assignRightStatusAndColor(View view, boolean contact, String newStatus){
		TextView currentTextView ;
		if(contact)
			currentTextView = (TextView) view.findViewById(R.id.contactStatus);
		else
			currentTextView = (TextView) view.findViewById(R.id.headerStatus);
		if(!newStatus.equalsIgnoreCase(currentTextView.getText().toString())){
			currentTextView.setText(newStatus);
			if(newStatus.equalsIgnoreCase("BUSY"))
				currentTextView.setTextColor(Color.parseColor("#a22020")); // If BUSY change color to RED
			else if(newStatus.equalsIgnoreCase("FREE"))
				currentTextView.setTextColor(Color.parseColor("#1b881b")); // If FREE change color to GREEN
			else
				currentTextView.setTextColor(Color.parseColor("#cd8b29")); // If UNKNOWN change color to ORANGE
			return true;
		}
		return false;
	}
	
}

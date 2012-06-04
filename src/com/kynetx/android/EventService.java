package com.kynetx.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.kynetx.android.R;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.CallLog;
import android.util.Log;
import android.widget.Toast;


public class EventService extends Service {
	private static final String tag = "kynetx";
    private LocationManager lm;
    private LocationListener locationListener;
    private BroadcastReceiver batteryListener;
    
    private static long minTimeMillis = 5 * 60 * 1000;
    private static long minDistanceMeters = 75;

	
    /** Called when the activity is first created. */
    private void startLoggerService() {
    	//location monitoring
    	lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	locationListener = new MyLocationListener();
    	lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
    			minTimeMillis, 
    			minDistanceMeters,
    			locationListener);
    	//power monitoring
    	batteryListener = new BroadcastReceiver(){
    		@Override
    		public void onReceive(Context arg0, Intent intent) {
    			processBatteryData(intent);
    		}
    	};
    	this.registerReceiver(batteryListener, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    	//call log monitoring
    	getContentResolver()
        .registerContentObserver(
                android.provider.CallLog.Calls.CONTENT_URI, true,
                new CallsContentObserver(null));
    	//sms log monitoring
    	getContentResolver().registerContentObserver(
    			Uri.parse("content://sms/"), 
    			true, 
    			new SMSContentObserver(null));

    }
    private void shutdownLoggerService() {
    	lm.removeUpdates(locationListener);
    	this.unregisterReceiver(batteryListener);
    }
    
    private void processBatteryData(Intent intent) {
    	// Process battery data
    	int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
    	int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
    	int percent = (int) ((float) level / scale * 100);
    	String plugged = "unplugged";
    	switch (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
    	case BatteryManager.BATTERY_PLUGGED_AC: plugged = "AC"; break;
    	case BatteryManager.BATTERY_PLUGGED_USB: plugged = "USB"; break;
    	}

    	//TODO: Filter to 5% changes only

    	//create event
    	Event batteryEvent = new Event("smartphone", "battery");
    	batteryEvent.addAttribute("level", level);
    	batteryEvent.addAttribute("scale", scale);
    	batteryEvent.addAttribute("percent", percent);
    	batteryEvent.addAttribute("plugged", plugged);

    	//send event
    	sendEvent(batteryEvent);
    }
    
    private void sendEvent(Event event){
    	// Execute HTTP Post Request
    	try {
    		final SharedPreferences settings = getSharedPreferences(KynetxForAndroidActivity.PREFS_NAME, 0);
    		String signalurl = settings.getString("signalurl", "notpresent");
    		
    		// Create a new HttpClient and Post Header
    		HttpClient httpclient = new DefaultHttpClient();
    		HttpPost httppost = new HttpPost(signalurl);
    		
    		httppost.setEntity(event.asEntity());
    		HttpResponse response = httpclient.execute(httppost);
    		Log.i(tag, "Raised Event: "+event.typeName());
    	} catch (ClientProtocolException e) {
    		Log.e(tag, "protocol error!");
    	} catch (IOException e) {
    		Log.e(tag, "io error!");
    	}
    }

    
    
    

    
   class SMSContentObserver extends ContentObserver {
       public SMSContentObserver(Handler h) {
           super(h);
       }

       @Override
       public boolean deliverSelfNotifications() {
           return true;
       }

       @Override
       public void onChange(boolean selfChange) {
           //Log.i(tag, "sms log change");
           //do stuff 
    	   Uri allMessages = Uri.parse("content://sms/");
    	    //Cursor cursor = managedQuery(allMessages, null, null, null, null); Both are same
    	   Cursor c = getContentResolver().query(allMessages, null,
    	           null, null, null);
           //Log.i(tag, Integer.toString(c.getCount()) + " SMSs found");
           if(c.moveToFirst()){

        	   int type = Integer.parseInt(c.getString(c.getColumnIndex("type")));// for call type, Incoming or out going
	           if( type == 1 || type == 2){ 
	        	   //1 = received, 2 = sent, others mean other things
	        	   String typestring = "unknown";
	        	   if(type == 1){
	        		   typestring = "received";
	        	   } else if(type == 2){
	        		   typestring = "sent";
	        	   }
	        	   String num= c.getString(c.getColumnIndex("address"));// for  number
		            String body = c.getString(c.getColumnIndex("body"));// for duration
		          //create event
		        	Event callEvent = new Event("smartphone", "sms");
		        	callEvent.addAttribute("number", num);
		        	callEvent.addAttribute("body", body);
		        	callEvent.addAttribute("type", typestring);
		
		        	//send event
		        	sendEvent(callEvent);
	           }
           }
           
       	super.onChange(selfChange);
       }
   }
   
    class CallsContentObserver extends ContentObserver {
        public CallsContentObserver(Handler h) {
            super(h);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            //Log.i(tag, "call log change");
            //do stuff 
            Cursor c = getContentResolver().query(
                    android.provider.CallLog.Calls.CONTENT_URI, 
                    null, 
                    null, 
                    null, 
                    android.provider.CallLog.Calls.DATE + " DESC");
            //Log.i(tag, Integer.toString(c.getCount()) + " calls found");
            if(c.moveToFirst()){
            
	            String num= c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));// for  number
	            String name= c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));// for name
	            String duration = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));// for duration
	            int type = Integer.parseInt(c.getString(c.getColumnIndex(CallLog.Calls.TYPE)));// for call type, Incoming or out going
	            
	          //create event
	        	Event callEvent = new Event("smartphone", "call");
	        	callEvent.addAttribute("number", num);
	        	callEvent.addAttribute("name", name);
	        	callEvent.addAttribute("duration", duration);
	        	callEvent.addAttribute("type", CallTypeString(type));
	
	        	//send event
	        	sendEvent(callEvent);
            }
            
        	super.onChange(selfChange);
        }
    }
    
    private String CallTypeString(int calltype){
    	switch(calltype){
    	case CallLog.Calls.INCOMING_TYPE: return "Incoming";
    	case CallLog.Calls.MISSED_TYPE: return "Missed";
    	case CallLog.Calls.OUTGOING_TYPE: return "Outgoing";
    	}
    	return "Unknown"; //edge case where calltype is neither of the three
    }
    
    public class MyLocationListener implements LocationListener {

    	public void onLocationChanged(Location loc) {
    		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    		PowerManager.WakeLock wl = pm.newWakeLock(
    				PowerManager.PARTIAL_WAKE_LOCK, tag);
    		wl.acquire(); // keep processor on till reporting is complete

    	    	Event locationEvent = new Event("smartphone", "location");
    	    	locationEvent.addAttribute("latitude", loc.getLatitude());
    	    	locationEvent.addAttribute("longitude", loc.getLongitude());
    	    	locationEvent.addAttribute("accuracy", loc.getAccuracy());
    			
    	    	sendEvent(locationEvent);
    	    	
    		wl.release();
    	}

    	public void onProviderDisabled(String provider) {

    			Toast.makeText(getBaseContext(),
    					"onProviderDisabled: " + provider, Toast.LENGTH_SHORT)
    					.show();

    	}

    	public void onProviderEnabled(String provider) {

    			Toast.makeText(getBaseContext(),
    					"onProviderEnabled: " + provider, Toast.LENGTH_SHORT)
    					.show();

    	}

    	public void onStatusChanged(String provider, int status, Bundle extras) {
    		/*String showStatus = null;
    		if (status == LocationProvider.AVAILABLE)
    			showStatus = "Available";
    		if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
    			showStatus = "Temporarily Unavailable";
    		if (status == LocationProvider.OUT_OF_SERVICE)
    			showStatus = "Out of Service";
    		if (status != lastStatus && showingDebugToast) {
    			Toast.makeText(getBaseContext(), "new status: " + showStatus,
    					Toast.LENGTH_SHORT).show();
    		}
    		lastStatus = status;*/
    	}

    }
    
	
    @Override
    public void onCreate() {
            super.onCreate();
            //mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            startLoggerService();

            // Display a notification about us starting. We put an icon in the
            // status bar.
            ///showNotification();
            Log.i("kynetx", "eventservice started");
            Toast.makeText(this, R.string.active, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
            super.onDestroy();
            shutdownLoggerService();
            
            // Cancel the persistent notification.
            //mNM.cancel(R.string.local_service_started);

            // Tell the user we stopped.
            Log.i("kynetx", "eventservice ended");
            Toast.makeText(this, R.string.inactive, Toast.LENGTH_SHORT).show();
    }
	
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}

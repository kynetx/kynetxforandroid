package com.kynetx.android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

public class KynetxForAndroidActivity extends Activity {
	public static final String PREFS_NAME = "KynetxPrefs";
	static final String tag = "kynetx";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String signalurl = settings.getString("signalurl", "notpresent");
        boolean active = settings.getBoolean("active", false);

        if(signalurl == "notpresent"){
        	//need to launch pair activity
        	Intent goToNextActivity = new Intent(getApplicationContext(), ConnectActivity.class);
        	startActivity(goToNextActivity);
        	finish();
        }
        
        setContentView(R.layout.main);
        
        final ToggleButton button = (ToggleButton) findViewById(R.id.active_button);
        //set initial value
        button.setChecked(active);
        //handle change
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
    			SharedPreferences.Editor editor = settings.edit();
    			editor.putBoolean("active", button.isChecked());
    			editor.commit();
    			//update service state
    			if (button.isChecked()){
	    			startService(new Intent(KynetxForAndroidActivity.this, EventService.class));
	    		} else {
	    			stopService(new Intent(KynetxForAndroidActivity.this, EventService.class));
	            }
            }
        });
        
        //make sure service is started if at this point.
        if(active){
        	startService(new Intent(KynetxForAndroidActivity.this, EventService.class));
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.mainmenu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.reset:
            //reset config
        	resetConfig();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void resetConfig(){
    	//clear setting
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove("signalurl");
		editor.commit();
		//toast reset
		Toast.makeText(getApplicationContext(), "Connection URL Reset", Toast.LENGTH_SHORT).show();
		//forward to connect activity
		Intent goToNextActivity = new Intent(getApplicationContext(), ConnectActivity.class);
    	startActivity(goToNextActivity);
    	finish();
    }
    	  
}
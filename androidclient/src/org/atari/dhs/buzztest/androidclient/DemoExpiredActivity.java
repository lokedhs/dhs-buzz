package org.atari.dhs.buzztest.androidclient;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;

public class DemoExpiredActivity extends Activity
{
    public static final String DEMO_EXPIRY_DATE = null;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.demo_expired );
    }

    public static boolean demoExpired() {
        //noinspection ConstantConditions
        if( DEMO_EXPIRY_DATE == null ) {
            return false;
        }

        try {
            DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
            Date expirationDate = dateFormat.parse( DEMO_EXPIRY_DATE );
            return System.currentTimeMillis() > expirationDate.getTime();
        }
        catch( ParseException e ) {
            Log.e( "can't parse expiry date: " + DEMO_EXPIRY_DATE, e );
            return true;
        }
    }
}

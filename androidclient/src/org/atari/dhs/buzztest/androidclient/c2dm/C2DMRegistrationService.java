package org.atari.dhs.buzztest.androidclient.c2dm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.atari.dhs.buzztest.androidclient.DhsBuzzApp;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.common.C2DMSettings;

public class C2DMRegistrationService extends Service
{
    private static final String EXTRA_REGISTRATION_NAME = "registration";

    @Override
    public IBinder onBind( Intent intent ) {
        return null;
    }

    static void scheduleFutureRegistrationRequest( Context context ) {
        DhsBuzzApp app = (DhsBuzzApp)context.getApplicationContext();
        long nextRetry = app.getAndIncrementCurrentC2DMBackoffTime();
        Log.d( "scheduling C2DM registration in " + nextRetry + " ms" );

        AlarmManager alarmManager = (AlarmManager)context.getSystemService( ALARM_SERVICE );

        Intent intent = new Intent( context, C2DMRegistrationService.class );
        intent.putExtra( EXTRA_REGISTRATION_NAME, 1 );
        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );

        PendingIntent pendingIntent = PendingIntent.getService( context, 0, intent, 0 );

        alarmManager.set( AlarmManager.RTC_WAKEUP,
                          System.currentTimeMillis() + nextRetry,
                          pendingIntent );
    }

    @Override
    public void onStart( Intent intent, int startId ) {
        super.onStart( intent, startId );

        Log.d( "got StartRegistration intent. startId=" + startId );

        if( intent.getIntExtra( EXTRA_REGISTRATION_NAME, 0 ) != 1 ) {
            throw new IllegalStateException( "wrong extra data in C2DM registration intent" );
        }

        registerForC2DM( this );
    }

    public static void registerForC2DM( Context context ) {
        PreferencesManager prefs = new PreferencesManager( context );
        if( prefs.getC2DMKey() != null || prefs.isC2DMDisabled() ) {
            return;
        }

        Log.d( "registering C2DM" );
        Intent registrationIntent = new Intent( "com.google.android.c2dm.intent.REGISTER" );
        registrationIntent.putExtra( "app", PendingIntent.getBroadcast( context, 0, new Intent(), 0 ) ); // boilerplate
        registrationIntent.putExtra( "sender", C2DMSettings.C2DM_USER );
        ComponentName component = context.startService( registrationIntent );
        if( component == null ) {
            Log.i( "C2DM not supported on this device, disabling for this session" );
            DhsBuzzApp application = (DhsBuzzApp)context.getApplicationContext();
            application.setC2DMDisabled( true );
        }
    }
}

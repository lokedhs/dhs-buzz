package org.atari.dhs.buzztest.androidclient.c2dm;

import java.util.HashMap;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.common.C2DMSettings;

public class C2DMReceiver extends BroadcastReceiver
{
    private static final Map<String, C2DMHandler> HANDLERS;

    static {
        Map<String, C2DMHandler> h = new HashMap<String, C2DMHandler>();
        h.put( C2DMSettings.C2DM_TYPE_ACTIVITY, new ActivityC2DMHandler() );
        h.put( C2DMSettings.C2DM_TYPE_SEARCH, new SearchC2DMHandler() );
        HANDLERS = h;
    }

    @Override
    public void onReceive( Context context, Intent intent ) {
        Log.i( "got C2DM intent: " + intent.getAction() );

        StringBuilder buf = new StringBuilder();
        for( String s : intent.getExtras().keySet() ) {
            buf.append( "key=" );
            buf.append( s );
            buf.append( ", value=" );
            buf.append( intent.getExtras().getString( s ) );
            buf.append( "\n" );
        }
        Log.i( buf.toString() );

        String registrationId = intent.getStringExtra( "registration_id" );
        String error = intent.getStringExtra( "error" );
        String unregistered = intent.getStringExtra( "unregistered" );
        Log.i( "unreg=" + unregistered + ", regId=" + registrationId + ", error=" + error );

        PreferencesManager mgr = new PreferencesManager( context );
        if( registrationId != null ) {
            mgr.setC2DMKey( registrationId );
            Log.i( "registered for C2DM" );
        }
        else if( error == null ) {
            String type = intent.getStringExtra( C2DMSettings.C2DM_FIELD_TYPE );
            if( type == null ) {
                Log.w( "missing type in c2dm update" );
            }
            else {
                C2DMHandler handler = HANDLERS.get( type );
                if( handler == null ) {
                    Log.w( "no handler for type: " + type );
                }
                else {
                    handler.processIncomingUpdate( context, intent );
                }
            }
        }
        else if( error.equals( "PHONE_REGISTRATION_ERROR" ) ) {
            Log.w( "C2DM not supported on this phone" );
            mgr.setC2DMDisabled( true );
        }
        else if( error.equals( "INVALID_SENDER" ) ) {
            Log.w( "invalid C2DM sender email" );
            mgr.setC2DMDisabled( true );
        }
        else if( error.equals( "SERVICE_NOT_AVAILABLE" ) ) {
            C2DMRegistrationService.scheduleFutureRegistrationRequest( context );
        }
        else {
            Log.e( "unknown C2DM response error: " + error );
        }
    }
}

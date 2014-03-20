package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.http.AndroidHttpClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.service.*;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.common.C2DMSettings;

public class RegisterRealtimeTask implements ServiceTask
{
    public static final String COMMAND_NAME = "registerRealtime";
    public static final String ACTIVITY_ID_KEY = "activity";
    public static final String START_KEY = "start";

    /**
     * Key that indicates the sender name. Only used in the ticker text. If not
     * specified, will be replaced by "unknown".
     */
    public static final String SENDER_NAME_KEY = "senderName";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityId = args.getString( ACTIVITY_ID_KEY );
        if( activityId == null ) {
            throw new IllegalStateException( "missing activity id" );
        }

        boolean start = args.getBooleanWithCheck( START_KEY );

        try {
            performRealtimeRegistration( taskContext, activityId, start );
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new RestartableServiceTaskFailedException( msg, e );
        }
    }

    static void performRealtimeRegistration( TaskContext taskContext, String activityId, boolean start ) throws IOException, ServiceTaskFailedException {
        PreferencesManager prefs = new PreferencesManager( taskContext.getService() );
        String c2dmKey = prefs.getC2DMKey();
        if( c2dmKey == null ) {
            throw new IllegalStateException( "no c2dm key, should request one, but for now we'll just exit" );
        }

        AccountManager accountManager = AccountManager.get( taskContext.getService() );
        Account[] accounts = accountManager.getAccountsByType( "com.google" );
        String accountName = "empty";
        if( accounts.length > 0 ) {
            accountName = accounts[0].name;
        }

        String userId = prefs.getUserId();

        AndroidHttpClient http = AndroidHttpClient.newInstance( "dhsBuzz", taskContext.getService() );
        // We need to set a 30 second timeout here, since processing on the server side can take some time
        // when appengine starts up an instance
        int timeout = (int)(30 * DateHelper.SECOND_MILLIS);
        HttpParams httpConnectionParams = http.getParams();
        HttpConnectionParams.setConnectionTimeout( httpConnectionParams, timeout );
        HttpConnectionParams.setSoTimeout( httpConnectionParams, timeout );
        try {
            HttpPost request = new HttpPost( "https://lokemaptest.appspot.com/C2DMPush" );

            // We need to wait here as well, for the same reason as above
            HttpConnectionParams.setSoTimeout( request.getParams(), timeout );

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_COMMAND, start ? C2DMSettings.C2DM_COMMAND_VALUE_SUBSCRIBE : C2DMSettings.C2DM_COMMAND_VALUE_UNSUBSCRIBE ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_KEY, c2dmKey ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_ACTIVITY, activityId ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_USER_NAME, accountName ) );
            if( userId != null ) {
                params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_USER_ID, userId ) );
            }
            Log.d( "requesting c2dm updates for url=" + activityId );

            HttpEntity entity = new UrlEncodedFormEntity( params );
            request.setEntity( entity );

            HttpResponse result = http.execute( request );
            if( result.getStatusLine().getStatusCode() != 200 ) {
                Log.e( "error from subscription server: " + result.getStatusLine() );
                Resources resources = taskContext.getService().getResources();
                throw new ServiceTaskFailedException( resources.getString( R.string.server_error ) );
            }
        }
        finally {
            http.close();
        }

        updateMarkerTable( taskContext.getDatabase(), activityId, start );

        Intent intent = new Intent( IntentHelper.ACTION_BUZZ_ACTIVITY_REALTIME_CHANGED );
        intent.putExtra( IntentHelper.EXTRA_ACTIVITY_ID, activityId );
        intent.putExtra( IntentHelper.EXTRA_REALTIME_MODE, start );
        taskContext.getService().sendBroadcast( intent );
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        String name = args.getString( SENDER_NAME_KEY );
        if( name == null ) {
            name = "unknown";
        }
        Resources resources = context.getResources();
        String fmt = resources.getString( R.string.task_register_realtime_ticker );
        return MessageFormat.format( fmt, name );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }

    private static void updateMarkerTable( SQLiteDatabase db, String activityUrl, boolean enable ) {
        if( enable ) {
            db.beginTransaction();
            try {
                Cursor result = db.query( StorageHelper.REALTIME_USER_FEEDS_TABLE,
                                          new String[] { StorageHelper.REALTIME_USER_FEEDS_ID },
                                          StorageHelper.REALTIME_USER_FEEDS_ID + " = ?", new String[] { activityUrl },
                                          null, null,
                                          null );
                boolean hasRow = false;
                if( result.moveToNext() ) {
                    hasRow = true;
                }
                result.close();

                if( !hasRow ) {
                    ContentValues values = new ContentValues();
                    values.put( StorageHelper.REALTIME_USER_FEEDS_ID, activityUrl );
                    db.insert( StorageHelper.REALTIME_USER_FEEDS_TABLE,
                               StorageHelper.REALTIME_USER_FEEDS_ID,
                               values );
                }

                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
        else {
            db.delete( StorageHelper.REALTIME_USER_FEEDS_TABLE,
                       StorageHelper.REALTIME_USER_FEEDS_ID + " = ?", new String[] { activityUrl } );
        }
    }
}

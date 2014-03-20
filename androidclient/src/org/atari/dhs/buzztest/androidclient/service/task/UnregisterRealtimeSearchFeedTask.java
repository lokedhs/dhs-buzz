package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.search.realtime.RealtimePeriodicUpdateManager;
import org.atari.dhs.buzztest.androidclient.service.*;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.common.C2DMSettings;

public class UnregisterRealtimeSearchFeedTask implements ServiceTask
{
    public static final String COMMAND_NAME = "unregisterRealtimeSearchFeed";
    public static final String REALTIME_FEED_ID_KEY = "realtimeFeedId";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        try {
            long realtimeId = args.getLongWithCheck( REALTIME_FEED_ID_KEY );
            unregisterFromSubscriptionServer( taskContext, realtimeId );
        }
        catch( IOException e ) {
            BuzzManagerService.throwIOExceptionError( taskContext, e );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        Resources resources = context.getResources();
        return resources.getString( R.string.task_register_realtime_feed_task_ticker_remove );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }

    private void unregisterFromSubscriptionServer( TaskContext taskContext, long realtimeId ) throws ServiceTaskFailedException, IOException {
        SQLiteDatabase db = taskContext.getDatabase();

        Cursor queryResult = db.query( StorageHelper.REALTIME_SEARCH_TABLE,
                                       new String[] { StorageHelper.REALTIME_SEARCH_WORDS },
                                       StorageHelper.REALTIME_SEARCH_ID + " = ?", new String[] { String.valueOf( realtimeId ) },
                                       null, null,
                                       null );

        String searchWords;
        try {
            if( !queryResult.moveToNext() ) {
                throw new ServiceTaskFailedException( "trying to unregister from nonexistent realtime feed: " + realtimeId );
            }

            searchWords = queryResult.getString( 0 );
        }
        finally {
            queryResult.close();
        }

        PreferencesManager prefs = new PreferencesManager( taskContext.getService() );
        String c2dmKey = prefs.getC2DMKey();
        if( c2dmKey == null ) {
            throw new ServiceTaskFailedException( "C2DM not available" );
        }

        AndroidHttpClient http = AndroidHttpClient.newInstance( "dhsBuzz", taskContext.getService() );
        try {
            int timeout = (int)(30 * DateHelper.SECOND_MILLIS);
            HttpParams httpConnectionParams = http.getParams();
            HttpConnectionParams.setConnectionTimeout( httpConnectionParams, timeout );
            HttpConnectionParams.setSoTimeout( httpConnectionParams, timeout );
            HttpPost request = new HttpPost( "https://lokemaptest.appspot.com/SubscribeFeed" );

            HttpConnectionParams.setSoTimeout( request.getParams(), timeout );

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_COMMAND, C2DMSettings.C2DM_COMMAND_VALUE_UNSUBSCRIBE ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_DEVICE_SUBSCRIPTION_ID, String.valueOf( realtimeId ) ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_SEARCH_TARGET, searchWords ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_KEY, c2dmKey ) );
//            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_USER_NAME, accountName ) );
//            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_USER_ID, userId ) );
//            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_DEVICE_SUBSCRIPTION_ID, String.valueOf( subscriptionId ) ) );

            HttpEntity entity = new UrlEncodedFormEntity( params );
            request.setEntity( entity );

            HttpResponse httpResult = http.execute( request );
            if( httpResult.getStatusLine().getStatusCode() != 200 ) {
                Log.e( "error from subscription server: " + httpResult.getStatusLine() );
                Resources resources = taskContext.getService().getResources();
                throw new ReportableServiceTaskFailedException( resources.getString( R.string.server_error ) );
            }
        }
        finally {
            http.close();
        }

        db.delete( StorageHelper.REALTIME_SEARCH_TABLE,
                   StorageHelper.REALTIME_SEARCH_ID + " = ?", new String[] { String.valueOf( realtimeId ) } );

        Intent intent = new Intent( taskContext.getService(), RealtimePeriodicUpdateManager.class );
        intent.putExtra( RealtimePeriodicUpdateManager.EXTRA_TYPE, RealtimePeriodicUpdateManager.TYPE_CHECK_AND_START );
        taskContext.getService().startService( intent );
    }
}

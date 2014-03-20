package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import org.atari.dhs.buzztest.androidclient.buzz.BuzzUrl;
import org.atari.dhs.buzztest.androidclient.search.realtime.RealtimePeriodicUpdateManager;
import org.atari.dhs.buzztest.androidclient.service.*;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.common.C2DMSettings;

public class RegisterRealtimeSearchFeedTask implements ServiceTask
{
    public static final String COMMAND_NAME = "registerRealtimeFeed";
    public static final String SEARCH_WORDS_KEY = "searchWords";
    public static final String REALTIME_ID_KEY = "realtimeId";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        try {
            String searchWords = args.getStringWithCheck( SEARCH_WORDS_KEY );
            registerWithSubscriptionServer( taskContext, searchWords );
        }
        catch( IOException e ) {
            BuzzManagerService.throwIOExceptionError( taskContext, e );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        Resources resources = context.getResources();
        return resources.getString( R.string.task_register_realtime_feed_task_ticker_add );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }

    private void registerWithSubscriptionServer( TaskContext taskContext, String searchWords ) throws ServiceTaskFailedException, IOException {
        Log.i( "registering with server: " + searchWords );

        PreferencesManager prefs = new PreferencesManager( taskContext.getService() );
        String c2dmKey = prefs.getC2DMKey();
        if( c2dmKey == null ) {
            throw new ServiceTaskFailedException( "C2DM not available" );
        }

        AccountManager accountManager = AccountManager.get( taskContext.getService() );
        Account[] accounts = accountManager.getAccountsByType( "com.google" );
        String accountName = "empty";
        if( accounts.length > 0 ) {
            accountName = accounts[0].name;
        }

        String userId = prefs.getUserId();
        if( userId == null ) {
            throw new ServiceTaskFailedException( "userId not available" );
        }

        AndroidHttpClient http = AndroidHttpClient.newInstance( "dhsBuzz", taskContext.getService() );
        try {
            long subscriptionId = saveSubscriptionToDatabase( taskContext, searchWords );

            int timeout = (int)(30 * DateHelper.SECOND_MILLIS);
            HttpParams httpConnectionParams = http.getParams();
            HttpConnectionParams.setConnectionTimeout( httpConnectionParams, timeout );
            HttpConnectionParams.setSoTimeout( httpConnectionParams, timeout );
            HttpPost request = new HttpPost( "https://lokemaptest.appspot.com/SubscribeFeed" );

            HttpConnectionParams.setSoTimeout( request.getParams(), timeout );

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_COMMAND, C2DMSettings.C2DM_COMMAND_VALUE_SUBSCRIBE ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_SEARCH_TARGET, searchWords ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_KEY, c2dmKey ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_USER_NAME, accountName ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_USER_ID, userId ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_DEVICE_SUBSCRIPTION_ID, String.valueOf( subscriptionId ) ) );

            HttpEntity entity = new UrlEncodedFormEntity( params );
            request.setEntity( entity );

            HttpResponse result = http.execute( request );
            if( result.getStatusLine().getStatusCode() != 200 ) {
                Log.e( "error from subscription server: " + result.getStatusLine() );
                deleteSubscriptionFromDatabase( taskContext, subscriptionId );

                Resources resources = taskContext.getService().getResources();
                throw new ReportableServiceTaskFailedException( resources.getString( R.string.server_error ) );
            }
        }
        finally {
            http.close();
        }

        Intent intent = new Intent( taskContext.getService(), RealtimePeriodicUpdateManager.class );
        intent.putExtra( RealtimePeriodicUpdateManager.EXTRA_TYPE, RealtimePeriodicUpdateManager.TYPE_CHECK_AND_START );
        taskContext.getService().startService( intent );
    }

    private long saveSubscriptionToDatabase( TaskContext taskContext, String searchWords ) throws ServiceTaskFailedException {
        SQLiteDatabase db = taskContext.getDatabase();

        // TODO: just for testing. Should actually unsubscribe here.
        db.delete( StorageHelper.REALTIME_SEARCH_TABLE,
                   null, null );

        ContentValues values = new ContentValues();
        values.put( StorageHelper.REALTIME_SEARCH_FEED_KEY, "search" );
        values.put( StorageHelper.REALTIME_SEARCH_WORDS, searchWords );
        values.put( StorageHelper.REALTIME_SEARCH_URL, BuzzUrl.buildSearchUrl( searchWords ) );
        values.put( StorageHelper.REALTIME_SEARCH_NUM_UNREAD, 0 );
        long id = db.insert( StorageHelper.REALTIME_SEARCH_TABLE, StorageHelper.REALTIME_SEARCH_FEED_KEY, values );
        if( id == -1 ) {
            throw new ServiceTaskFailedException( "failed to insert row" );
        }

        return id;
    }

    private void deleteSubscriptionFromDatabase( TaskContext taskContext, long subscriptionId ) {
        SQLiteDatabase db = taskContext.getDatabase();
        db.delete( StorageHelper.REALTIME_SEARCH_TABLE,
                   StorageHelper.REALTIME_SEARCH_ID + " = ?", new String[] { String.valueOf( subscriptionId ) } );
    }
}

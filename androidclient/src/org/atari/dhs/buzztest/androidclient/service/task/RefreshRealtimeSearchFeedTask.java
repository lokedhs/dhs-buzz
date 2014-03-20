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

public class RefreshRealtimeSearchFeedTask implements ServiceTask
{
    public static final String COMMAND_NAME = "refreshRealtimeFeed";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        SQLiteDatabase db = taskContext.getDatabase();
        List<SearchInfo> activeFeeds = new ArrayList<SearchInfo>();
        Cursor result = db.query( StorageHelper.REALTIME_SEARCH_TABLE,
                                  new String[] { StorageHelper.REALTIME_SEARCH_ID,
                                                 StorageHelper.REALTIME_SEARCH_WORDS },
                                  null, null,
                                  null, null,
                                  null );
        try {
            while( result.moveToNext() ) {
                long id = result.getLong( 0 );
                String expression = result.getString( 1 );
                activeFeeds.add( new SearchInfo( id, expression ) );
            }
        }
        finally {
            result.close();
        }

        if( activeFeeds.isEmpty() ) {
            // This could only happen if the alarm is still active for some reason.
            // In this case, we'll send the CHECK_AND_START event which will cause the alarm to be disabled.

            Log.e( "trying to refresh with no active feeds" );

            Intent intent = new Intent( taskContext.getService(), RealtimePeriodicUpdateManager.class );
            intent.putExtra( RealtimePeriodicUpdateManager.EXTRA_TYPE, RealtimePeriodicUpdateManager.TYPE_CHECK_AND_START );
            taskContext.getService().startService( intent );
        }
        else {
            Log.d( "refreshing feeds: " + activeFeeds );
            try {
                sendRefreshRequest( activeFeeds, taskContext );
            }
            catch( IOException e ) {
                // A communication error here will be ignored, as a retry will happen
                // once the alarm is triggered the next time.
                // We might have to do a retry slightly earlier, but for now this will have to do.
                Log.w( "IOException when performing refresh", e );
            }
        }
    }

    private void sendRefreshRequest( List<SearchInfo> activeFeeds, TaskContext taskContext ) throws ServiceTaskFailedException, IOException {
        if( activeFeeds.size() > 1 ) {
            throw new ServiceTaskFailedException( "no support for refreshing multiple feeds yet" );
        }

        SearchInfo searchInfo = activeFeeds.get( 0 );

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
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_COMMAND, C2DMSettings.C2DM_COMMAND_VALUE_REFRESH ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_SEARCH_TARGET, searchInfo.expression ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_KEY, c2dmKey ) );
            params.add( new BasicNameValuePair( C2DMSettings.C2DM_PARAM_REQUEST_DEVICE_SUBSCRIPTION_ID, String.valueOf( searchInfo.id ) ) );

            HttpEntity entity = new UrlEncodedFormEntity( params );
            request.setEntity( entity );

            HttpResponse result = http.execute( request );
            if( result.getStatusLine().getStatusCode() != 200 ) {
                Log.e( "error from subscription server: " + result.getStatusLine() );
                Resources resources = taskContext.getService().getResources();
                // It might not be an overly great idea to report this, since the user has very little
                // control over what should be done in this case. Just ignoring the error is probably better.
                throw new ReportableServiceTaskFailedException( resources.getString( R.string.server_error ) );
            }

        }
        finally {
            http.close();
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return "Refreshing realtime tasks";
    }

    @Override
    public boolean displayNotification() {
        return false;
    }

    private static class SearchInfo
    {
        long id;
        String expression;

        private SearchInfo( long id, String expression ) {
            this.id = id;
            this.expression = expression;
        }

        @Override
        public String toString() {
            return "SearchInfo[" +
                   "id=" + id +
                   ", expression='" + expression + '\'' +
                   ']';
        }
    }
}

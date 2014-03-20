package org.atari.dhs.buzztest.androidclient;

import java.util.HashSet;
import java.util.Set;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.RemoteException;

import org.atari.dhs.buzztest.androidclient.c2dm.C2DMRegistrationService;
import org.atari.dhs.buzztest.androidclient.search.realtime.RealtimePeriodicUpdateManager;
import org.atari.dhs.buzztest.androidclient.service.AbstractServiceCallback;
import org.atari.dhs.buzztest.androidclient.service.IPostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceHelper;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

public class DhsBuzzApp extends Application//CrashReportingApplication
{
    public static final int IN_PROGRESS_NOTIFICATION_ID = 0;
    public static final int TASK_RESTARTABLE_ERROR_NOTIFICATION_ID = 1;
    public static final int INCOMING_REPLIES_NOTIFICATION_ID = 2;
    public static final int TASK_FAIL_NOTIFICATION_ID = 3;
    public static final int REALTIME_SEARCH_UPDATED_NOTIFICATION_ID = 4;

    public static final boolean BLOCK_DESTRUCTIVE_ACTIONS = false;
    public static final String API_KEY = "AIzaSyCGpjk2Pebmh3J4FHB1z1ahzAzyGV8JDk8";

    private SQLiteDatabase database;
    private Set<SQLiteDatabaseWrapper> databaseWrappers;

    private long currentC2DMBackoffTime = 1000;
    private boolean c2dmDisabled;

    private boolean realtimeActivityVisible = false;

    @Override
    public void onCreate() {
        super.onCreate();

        PreferencesManager prefs = new PreferencesManager( this );
        prefs.updateErrorReporterUsername();
        prefs.updateErrorReporterAccountName();

        NotificationManager notificationManager = (NotificationManager)getSystemService( NOTIFICATION_SERVICE );
        // In case the application was forcibly killed while the progress notification was being shown
        notificationManager.cancel( IN_PROGRESS_NOTIFICATION_ID );

        C2DMRegistrationService.registerForC2DM( this );

        startPeriodicUpdates( 2 * DateHelper.MINUTE_MILLIS );

        Intent updateIntent = new Intent( this, RealtimePeriodicUpdateManager.class );
        updateIntent.putExtra( RealtimePeriodicUpdateManager.EXTRA_TYPE, RealtimePeriodicUpdateManager.TYPE_CHECK_AND_START );
        startService( updateIntent );
    }

    public long getAndIncrementCurrentC2DMBackoffTime() {
        long time = currentC2DMBackoffTime;
        currentC2DMBackoffTime = Math.min( currentC2DMBackoffTime * 2, 48L * 60L * 60L * 1000L ); // Never backoff to more than 48 hours
        return time;
    }

//    @Override
//    public String getFormId() {
//        return "dEJsWHJVeGxBb1Q0ZDlubWs4QnlPYmc6MQ";
//    }

    public synchronized SQLiteDatabaseWrapper getDatabase() {
        if( database == null ) {
            StorageHelper storageHelper = new StorageHelper( this );
            database = storageHelper.getWritableDatabase();
        }

        if( databaseWrappers == null ) {
            databaseWrappers = new HashSet<SQLiteDatabaseWrapper>();
        }

        SQLiteDatabaseWrapper wrapper = new SQLiteDatabaseWrapper( this, database );
        databaseWrappers.add( wrapper );

        return wrapper;
    }

    public synchronized void releaseDatabaseWrapper( SQLiteDatabaseWrapper wrapper ) {
        if( !databaseWrappers.remove( wrapper ) ) {
            throw new IllegalStateException( "trying to remove a database wrapper that was never added" );
        }

        if( databaseWrappers.isEmpty() ) {
            database.close();
            database = null;
        }
    }

    private void startPeriodicUpdates( long delay ) {
        // Set up handler for the purge task
        Handler handler = new Handler();
        handler.postDelayed( new PurgeTask(), delay );
    }

    public void setC2DMDisabled( boolean c2dmDisabled ) {
        this.c2dmDisabled = c2dmDisabled;
    }

    public boolean isC2dmDisabled() {
        return c2dmDisabled;
    }

    private class PurgeTask implements Runnable
    {
        @Override
        public void run() {
            Log.i( "will purge now" );
            PostMessageServiceHelper.startServiceAndRunMethod( DhsBuzzApp.this, new AbstractServiceCallback()
            {
                @Override
                public void runWithService( IPostMessageService service ) throws RemoteException {
                    service.purge();
                }
            } );
            startPeriodicUpdates( 10 * DateHelper.HOUR_MILLIS );
        }
    }

    public static int getVersionCode( Context context ) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo( context.getPackageName(), PackageManager.GET_META_DATA );
            return pInfo.versionCode;
        }
        catch( PackageManager.NameNotFoundException e ) {
            Log.e( "Failed to get application version", e );
            return 0;
        }
    }

    public boolean isRealtimeActivityVisible() {
        return realtimeActivityVisible;
    }

    public void setRealtimeActivityVisible( boolean realtimeActivityVisible ) {
        this.realtimeActivityVisible = realtimeActivityVisible;
    }
}

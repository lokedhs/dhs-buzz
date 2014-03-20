package org.atari.dhs.buzztest.androidclient.search.realtime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.os.RemoteException;

import org.atari.dhs.buzztest.androidclient.SQLiteDatabaseWrapper;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.service.AbstractServiceCallback;
import org.atari.dhs.buzztest.androidclient.service.IPostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceHelper;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

public class RealtimePeriodicUpdateManager extends Service
{
    public static final String EXTRA_TYPE = "type";
    public static final String TYPE_CHECK_AND_START = "check";
    public static final String TYPE_SEND_REFRESH = "refresh";

    private AlarmManager alarmManager;
    private SQLiteDatabaseWrapper dbWrapper;

    @Override
    public void onCreate() {
        super.onCreate();

        alarmManager = (AlarmManager)getSystemService( Context.ALARM_SERVICE );
    }

    @Override
    public IBinder onBind( Intent intent ) {
        return null;
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
        String type = intent.getStringExtra( EXTRA_TYPE );
        if( TYPE_CHECK_AND_START.equals( type ) ) {
            checkAndStart();
        }
        else if( TYPE_SEND_REFRESH.equals( type ) ) {
            refreshSubscriptions();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if( dbWrapper != null ) {
            dbWrapper.close();
        }

        super.onDestroy();
    }

    public void checkAndStart() {
        SQLiteDatabase db = getDatabase();
        Cursor result = db.query( StorageHelper.REALTIME_SEARCH_TABLE,
                                  new String[] { StorageHelper.REALTIME_SEARCH_ID },
                                  null, null,
                                  null, null,
                                  null );
        boolean hasRealtime = result.moveToNext();
        result.close();

        PendingIntent intent = makePendingIntent();
        if( hasRealtime ) {
            alarmManager.setInexactRepeating( AlarmManager.RTC_WAKEUP, 2 * DateHelper.MINUTE_MILLIS, AlarmManager.INTERVAL_FIFTEEN_MINUTES, intent );
        }
        else {
            alarmManager.cancel( intent );
        }
    }

    private void refreshSubscriptions() {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback() {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.refreshRealtimeFeedSubscriptions();
            }
        });
    }

    private SQLiteDatabase getDatabase() {
        if( dbWrapper == null ) {
            dbWrapper = StorageHelper.getDatabase( this );
        }

        return dbWrapper.getDatabase();
    }

    private PendingIntent makePendingIntent() {
        Intent intent = new Intent( this, RealtimePeriodicUpdateManager.class );
        intent.putExtra( EXTRA_TYPE, TYPE_SEND_REFRESH );
        return PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

}

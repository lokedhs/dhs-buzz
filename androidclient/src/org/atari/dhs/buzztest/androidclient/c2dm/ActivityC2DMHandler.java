package org.atari.dhs.buzztest.androidclient.c2dm;

import java.text.MessageFormat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.atari.dhs.buzztest.androidclient.*;
import org.atari.dhs.buzztest.androidclient.displaynew.DisplayNew;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.common.C2DMSettings;

public class ActivityC2DMHandler implements C2DMHandler
{
    @Override
    @SuppressWarnings( { "ConstantConditions" })
    public void processIncomingUpdate( Context context, Intent intent ) {
        String senderName = intent.getStringExtra( C2DMSettings.C2DM_FIELD_ACTIVITY_SUB_MESSAGE_SENDER );
        String activityId = intent.getStringExtra( C2DMSettings.C2DM_FIELD_ACTIVITY_SUB_MESSAGE_ACTIVITY_ID );

        if( senderName == null || activityId == null ) {
            Log.w( "got C2DM message without sender, activity or error" );
            return;
        }

        SQLiteDatabaseWrapper dbWrapper = StorageHelper.getDatabase( context );
        try {
            SQLiteDatabase db = dbWrapper.getDatabase();

            try {
                db.beginTransaction();

                updateNewCount( activityId, db );
                updateRealtime( activityId, db );

                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }

            PreferencesManager prefs = new PreferencesManager( context );

            NotificationManager notificationManager = (NotificationManager)context.getSystemService( Context.NOTIFICATION_SERVICE );

            Notification notification = new Notification( R.drawable.ic_stat_notify_new_message, "New replies", System.currentTimeMillis() );

            notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
            notification.flags |= Notification.FLAG_AUTO_CANCEL;

            if( prefs.getPlaySound() ) {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
            if( prefs.getVibrate() ) {
                notification.defaults |= Notification.DEFAULT_VIBRATE;
            }
            int ballColour = prefs.getBallColour();
            if( ballColour != 0 ) {
                notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                notification.ledARGB = ballColour;
                notification.ledOnMS = 1000;
                notification.ledOffMS = 4000;
            }

            Intent notificationBarIntent = new Intent( context, DisplayNew.class );
            notificationBarIntent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
            PendingIntent contentIntent = PendingIntent.getActivity( context, 0, notificationBarIntent, 0 );

            Resources resources = context.getResources();
            String titleFmt = resources.getString( R.string.notification_new_replies_from_subscribed_title );
            String contentFmt = resources.getString( R.string.notification_new_replies_from_subscribed_content );
            notification.setLatestEventInfo( context,
                                             MessageFormat.format( titleFmt, senderName ),
                                             MessageFormat.format( contentFmt, senderName ),
                                             contentIntent );
            notificationManager.notify( DhsBuzzApp.INCOMING_REPLIES_NOTIFICATION_ID, notification );
        }
        finally {
            dbWrapper.close();
        }
    }

    private void updateNewCount( String activityId, SQLiteDatabase db ) {
        long now = System.currentTimeMillis();

        Cursor result = db.query( StorageHelper.UPDATE_COUNT_TABLE,
                                  new String[] { StorageHelper.UPDATE_COUNT_NUMBER },
                                  StorageHelper.UPDATE_COUNT_ACTIVITY_NAME + " = ?", new String[] { activityId },
                                  null, null,
                                  null );
        int updateCount = 0;
        boolean willUpdate = false;
        if( result.moveToNext() ) {
            updateCount = result.getInt( 0 );
            willUpdate = true;
        }
        result.close();

        if( willUpdate ) {
            ContentValues values = new ContentValues();
            values.put( StorageHelper.UPDATE_COUNT_NUMBER, updateCount + 1 );
            values.put( StorageHelper.UPDATE_COUNT_DATE, now );
            db.update( StorageHelper.UPDATE_COUNT_TABLE,
                       values,
                       StorageHelper.UPDATE_COUNT_ACTIVITY_NAME + " = ?", new String[] { activityId } );
        }
        else {
            ContentValues values = new ContentValues();
            values.put( StorageHelper.UPDATE_COUNT_ACTIVITY_NAME, activityId );
            values.put( StorageHelper.UPDATE_COUNT_NUMBER, 1 );
            values.put( StorageHelper.UPDATE_COUNT_DATE, now );
            db.insert( StorageHelper.UPDATE_COUNT_TABLE,
                       StorageHelper.UPDATE_COUNT_NUMBER,
                       values );
        }
    }

    private void updateRealtime( String activityId, SQLiteDatabase db ) {
        Cursor result = db.query( StorageHelper.REALTIME_USER_FEEDS_TABLE,
                                  new String[] { StorageHelper.REALTIME_USER_FEEDS_ID },
                                  StorageHelper.REALTIME_USER_FEEDS_ID + " = ?", new String[] { activityId },
                                  null, null,
                                  null );
        boolean found = result.moveToNext();
        result.close();

        if( !found ) {
            ContentValues values = new ContentValues();
            values.put( StorageHelper.REALTIME_USER_FEEDS_ID, activityId );
            db.insert( StorageHelper.REALTIME_USER_FEEDS_TABLE,
                       StorageHelper.REALTIME_USER_FEEDS_ID,
                       values );
        }
    }
}

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
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.CachedFeed;
import org.atari.dhs.buzztest.androidclient.buzz.CachedFeedActivity;
import org.atari.dhs.buzztest.androidclient.buzz.PersistedFeedWrapper;
import org.atari.dhs.buzztest.androidclient.displayfeed.DisplayFeed;
import org.atari.dhs.buzztest.androidclient.tools.SerialisationHelper;
import org.atari.dhs.buzztest.common.C2DMSettings;

public class SearchC2DMHandler implements C2DMHandler
{
    @Override
    public void processIncomingUpdate( Context context, Intent intent ) {
        Log.i( "got search update" );

        String subscriptionId = intent.getStringExtra( C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_DEVICE_SUBSCRIPTION_ID );
        String id = intent.getStringExtra( "collapse_key" );
        String actor = intent.getStringExtra( C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_ACTOR );
        String actorId = intent.getStringExtra( C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_ACTOR_ID );
        String numRepliesString = intent.getStringExtra( C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_NUM_REPLIES );
        String updatedString = intent.getStringExtra( C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_UPDATED );
        String title = intent.getStringExtra( C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_TITLE );
        String photoUrl = intent.getStringExtra( C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_PHOTO_URL );

        int numReplies = Integer.parseInt( numRepliesString );
        long updated = Long.parseLong( updatedString );

        Log.i( "id=" + id );
        Log.i( "subscriptionId=" + subscriptionId );
        Log.i( "actor=" + actor );
        Log.i( "replies=" + numReplies );
        Log.i( "updated=" + updatedString );
        Log.i( "title=" + title );
        Log.i( "photoUrl=" + photoUrl );

        SQLiteDatabaseWrapper dbWrapper = StorageHelper.getDatabase( context );
        try {
            SQLiteDatabase db = dbWrapper.getDatabase();
            String feedKey = null;
            String searchUrl = null;
            int numUnread = 0;

            db.beginTransaction();
            try {
                Cursor result = db.query( StorageHelper.REALTIME_SEARCH_TABLE,
                                          new String[] { StorageHelper.REALTIME_SEARCH_FEED_KEY,
                                                         StorageHelper.REALTIME_SEARCH_URL,
                                                         StorageHelper.REALTIME_SEARCH_NUM_UNREAD },
                                          StorageHelper.REALTIME_SEARCH_ID + " = ?", new String[] { subscriptionId },
                                          null, null,
                                          null );
                if( result.moveToNext() ) {
                    feedKey = result.getString( 0 );
                    searchUrl = result.getString( 1 );
                    numUnread = result.getInt( 2 );
                }

                result.close();

                if( feedKey != null ) {
                    ContentValues values = new ContentValues();
                    values.put( StorageHelper.REALTIME_SEARCH_NUM_UNREAD, numUnread + 1 );
                    db.update( StorageHelper.REALTIME_SEARCH_TABLE,
                               values,
                               StorageHelper.REALTIME_SEARCH_ID + " = ?", new String[] { subscriptionId } );
                }
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }

            if( feedKey == null ) {
                Log.w( "realtime search update for nonexistent search. id=" + id );
                return;
            }

            CachedFeedActivity activity = new CachedFeedActivity( id, actor, actorId, numReplies, updated, title, photoUrl.equals( "" ) ? null : photoUrl );
            updateCachedActivity( context, db, feedKey, activity );

//        Intent intent = new Intent( ACTION_SEARCH_FEED_UPDATED );
//        intent.putExtra( EXTRA_SEARCH_FEED_NAME, feedName );
//        return intent;
            Intent broadcast = new Intent( IntentHelper.ACTION_BUZZ_FEED_UPDATED );
            broadcast.putExtra( IntentHelper.EXTRA_FEED_ID, searchUrl );
            broadcast.putExtra( IntentHelper.EXTRA_STORED_FEED_KEY, feedKey );
            broadcast.putExtra( IntentHelper.EXTRA_NUM_UPDATED, numUnread );
            context.sendBroadcast( broadcast );

            displayNotification( context, numUnread );
        }
        finally {
            dbWrapper.close();
        }
    }

    private void displayNotification( Context context, int numUnread ) {
        DhsBuzzApp app = (DhsBuzzApp)context.getApplicationContext();
        if( !app.isRealtimeActivityVisible() ) {
            NotificationManager notificationManager = (NotificationManager)context.getSystemService( Context.NOTIFICATION_SERVICE );

            Notification notification = new Notification( R.drawable.ic_stat_notify_new_message, "Realtime updated", System.currentTimeMillis() );

            Intent notificationBarIntent = new Intent( context, DisplayFeed.class );
            notificationBarIntent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_REALTIME_SEARCH );
            notificationBarIntent.putExtra( DisplayFeed.EXTRA_REALTIME_SEARCH_NAME, "search" );
            notificationBarIntent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
            PendingIntent contentIntent = PendingIntent.getActivity( context, 0, notificationBarIntent, 0 );

            Resources resources = context.getResources();
            String titleFmt = resources.getString( R.string.notification_realtime_search_feed_updates_title );
            String contentFmt = resources.getString( R.string.notification_realtime_search_feed_updates_content );
            notification.setLatestEventInfo( context,
                                             MessageFormat.format( titleFmt, numUnread ),
                                             MessageFormat.format( contentFmt, numUnread ),
                                             contentIntent );
            notification.number = numUnread;
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notificationManager.notify( DhsBuzzApp.REALTIME_SEARCH_UPDATED_NOTIFICATION_ID, notification );
        }
    }

    private void updateCachedActivity( Context context, SQLiteDatabase db, String name, CachedFeedActivity activity ) {

        BuzzManager buzzManager = BuzzManager.createFromContext( context );

        PersistedFeedWrapper feed = buzzManager.loadFeedFromDatabase( name );
        CachedFeed cachedFeed;
        if( feed == null ) {
            cachedFeed = new CachedFeed();
        }
        else {
            cachedFeed = feed.getFeed();
        }

        cachedFeed.addActivityAndShiftDown( activity );

        ContentValues values = new ContentValues();
        values.put( StorageHelper.FEED_CACHE_CONTENT, SerialisationHelper.serialiseObject( cachedFeed ) );
        values.put( StorageHelper.FEED_CACHE_DOWNLOAD_DATE, System.currentTimeMillis() );
        values.put( StorageHelper.FEED_CACHE_CREATED_DATE, activity.getUpdated() );
        if( feed == null ) {
            values.put( StorageHelper.FEED_CACHE_LAST_READ_DATE, 0 );
            db.insert( StorageHelper.FEED_CACHE_TABLE, StorageHelper.FEED_CACHE_CONTENT, values );
        }
        else {
            db.update( StorageHelper.FEED_CACHE_TABLE,
                       values,
                       StorageHelper.FEED_CACHE_ID + " = ?", new String[] { name } );
        }
    }
}

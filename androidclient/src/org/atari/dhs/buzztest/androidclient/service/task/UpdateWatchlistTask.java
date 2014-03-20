package org.atari.dhs.buzztest.androidclient.service.task;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.service.BundleX;
import org.atari.dhs.buzztest.androidclient.service.ServiceTask;
import org.atari.dhs.buzztest.androidclient.service.ServiceTaskFailedException;
import org.atari.dhs.buzztest.androidclient.service.TaskContext;

public class UpdateWatchlistTask implements ServiceTask
{
    public static final String COMMAND_NAME = "setWatchlistStatus";
    public static final String ACTIVITY_ID_KEY = "activityId";
    public static final String ENABLE_KEY = "add";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityId = args.getStringWithCheck( ACTIVITY_ID_KEY );
        boolean add = args.getBooleanWithCheck( ENABLE_KEY );

        SQLiteDatabase db = taskContext.getDatabase();
        if( add ) {
            db.beginTransaction();
            try {
                Cursor result = db.query( StorageHelper.WATCHLIST_FEEDS_TABLE,
                                          new String[] { StorageHelper.WATCHLIST_FEEDS_ID },
                                          StorageHelper.WATCHLIST_FEEDS_ID + " = ?", new String[] { activityId },
                                          null, null,
                                          null );
                boolean hasRow = result.moveToNext();
                result.close();

                if( !hasRow ) {
                    ContentValues values = new ContentValues();
                    values.put( StorageHelper.WATCHLIST_FEEDS_ID, activityId );
                    values.put( StorageHelper.WATCHLIST_FEEDS_CREATED_DATE, System.currentTimeMillis() );
                    db.insert( StorageHelper.WATCHLIST_FEEDS_TABLE,
                               StorageHelper.WATCHLIST_FEEDS_ID,
                               values );
                }
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }

            sendUpdatedBroadcast( taskContext, true );
        }
        else {
            db.delete( StorageHelper.WATCHLIST_FEEDS_TABLE,
                       StorageHelper.WATCHLIST_FEEDS_ID + " = ?", new String[] { activityId } );
            sendUpdatedBroadcast( taskContext, false );
        }
    }

    private void sendUpdatedBroadcast( TaskContext taskContext, boolean add ) {
        Intent intent = new Intent( IntentHelper.ACTION_WATCHLIST_UPDATED );
        intent.putExtra( IntentHelper.EXTRA_WATCHLIST_UPDATE_TYPE_ADD, add );
        taskContext.getService().sendBroadcast( intent );
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        Resources resources = context.getResources();
        boolean add = args.getBooleanWithCheck( ENABLE_KEY );
        if( add ) {
            return resources.getString( R.string.task_update_watchlist_ticker_add );
        }
        else {
            return resources.getString( R.string.task_update_watchlist_ticker_remove );
        }
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

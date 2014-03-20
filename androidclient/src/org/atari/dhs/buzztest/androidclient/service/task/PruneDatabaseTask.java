package org.atari.dhs.buzztest.androidclient.service.task;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.service.BundleX;
import org.atari.dhs.buzztest.androidclient.service.ServiceTask;
import org.atari.dhs.buzztest.androidclient.service.ServiceTaskFailedException;
import org.atari.dhs.buzztest.androidclient.service.TaskContext;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

public class PruneDatabaseTask implements ServiceTask
{
    public static final String COMMAND_NAME = "cleanupDb";

    private static final long MINIMUM_PURGE_INTERVAL = 12 * DateHelper.HOUR_MILLIS;
    private static final long REPLIES_CACHE_CUTOFF_TIME = 1 * DateHelper.DAY_MILLIS;
    private static final long MESSAGE_CACHE_CUTOFF_TIME = 4 * DateHelper.DAY_MILLIS;
    private static final long IMAGE_CACHE_PURGE_CUTOFF_LONG = 1 * DateHelper.DAY_MILLIS;
    private static final long IMAGE_CACHE_PURGE_CUTOFF_SHORT = 1 * DateHelper.HOUR_MILLIS;

    public static long loadLastPurgeDate( SQLiteDatabase db ) {
        Cursor result = db.query( StorageHelper.PURGE_STATISTICS_TABLE,
                                  new String[] { StorageHelper.PURGE_STATISTICS_LAST_PURGE_DATE },
                                  null, null,
                                  null, null,
                                  null );
        long lastUpdate;
        if( result.moveToNext() ) {
            lastUpdate = result.getLong( 0 );
        }
        else {
            lastUpdate = -1;
        }
        result.close();
        return lastUpdate;
    }

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        SQLiteDatabase db = taskContext.getDatabase();
        long lastPurgeDate = loadLastPurgeDate( db );
        if( lastPurgeDate != -1 && lastPurgeDate > System.currentTimeMillis() - MINIMUM_PURGE_INTERVAL ) {
            return;
        }

        purgeRepliesCache( db );
        purgeMessageCache( db );
        purgeImageCache( taskContext, db );
        vacuumDb( db );

        updateLastPurgeDate( db );
    }

    private void updateLastPurgeDate( SQLiteDatabase db ) {
        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put( StorageHelper.PURGE_STATISTICS_LAST_PURGE_DATE, now );
            if( loadLastPurgeDate( db ) == -1 ) {
                db.insert( StorageHelper.PURGE_STATISTICS_TABLE,
                           StorageHelper.PURGE_STATISTICS_LAST_PURGE_DATE,
                           values );
            }
            else {
                db.update( StorageHelper.PURGE_STATISTICS_TABLE,
                           values,
                           null, null );
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    private void purgeRepliesCache( SQLiteDatabase db ) {
        // Simply delete all cached replies that hasn't been updated in a certain amount of time
        long cutoff = System.currentTimeMillis() - REPLIES_CACHE_CUTOFF_TIME;
        int rowCount = db.delete( StorageHelper.REPLIES_CACHE_TABLE,
                                  StorageHelper.REPLIES_CACHE_DOWNLOAD_DATE + " < ?", new String[] { String.valueOf( cutoff ) } );
        Log.i( "purged " + rowCount + " from " + StorageHelper.REPLIES_CACHE_TABLE );
    }

    private void purgeMessageCache( SQLiteDatabase db ) {
        long cutoff = System.currentTimeMillis() - MESSAGE_CACHE_CUTOFF_TIME;
        String cutoffAsString = String.valueOf( cutoff );
        int rowCount = db.delete( StorageHelper.MESSAGE_CACHE_TABLE,
                                  StorageHelper.MESSAGE_CACHE_LAST_READ_DATE + " < ? "
                                  + "and " + StorageHelper.MESSAGE_CACHE_LAST_READ_DATE + " < ?", new String[] { cutoffAsString, cutoffAsString } );
        Log.i( "purged " + rowCount + " from " + StorageHelper.MESSAGE_CACHE_TABLE );
    }

    private void purgeImageCache( TaskContext taskContext, SQLiteDatabase db ) {
        ImageCache imageCache = new ImageCache( taskContext.getService() );
        try {
            imageCache.purge( IMAGE_CACHE_PURGE_CUTOFF_LONG, IMAGE_CACHE_PURGE_CUTOFF_SHORT );
        }
        finally {
            imageCache.close();
        }
    }

    private void vacuumDb( SQLiteDatabase db ) {
        try {
            db.execSQL( "vacuum" );
        }
        catch( SQLiteException e ) {
            // the vacuum operation can fail if there is an ongoing transaction
            // if this happens, we can just ignore it
            Log.e( "exception when cleaning db", e );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return context.getResources().getString( R.string.task_prune_database_ticker );
    }

    @Override
    public boolean displayNotification() {
        return false;
    }
}

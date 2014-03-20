package org.atari.dhs.buzztest.androidclient.followers;

import java.io.IOException;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.atari.dhs.buzztest.androidclient.SQLiteDatabaseWrapper;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

public class FollowersManager
{
    private static final long FOLLOWERS_REFRESH_INTERVAL = 1 * DateHelper.DAY_MILLIS;

    private SQLiteDatabaseWrapper dbWrapper;
    private BuzzManager buzzManager;

    public FollowersManager( Context context ) {
        dbWrapper = StorageHelper.getDatabase( context );
        buzzManager = BuzzManager.createFromContext( context );
    }

    public void close() {
        dbWrapper.close();
        buzzManager.close();
    }

    public boolean isFollowingUser( String userId ) throws IOException {
        SQLiteDatabase db = dbWrapper.getDatabase();
        checkAndRefresh( db );

        Cursor result = db.query( StorageHelper.FOLLOWING_TABLE,
                                  new String[] { StorageHelper.FOLLOWING_USER_ID },
                                  StorageHelper.FOLLOWING_USER_ID + " = ?", new String[] { userId },
                                  null, null,
                                  null );
        boolean followingResult = result.moveToNext();
        result.close();
        return followingResult;
    }

    public boolean isFollowedByUser( String userId ) throws IOException {
        SQLiteDatabase db = dbWrapper.getDatabase();
        checkAndRefresh( db );

        Cursor result = db.query( StorageHelper.FOLLOWERS_TABLE,
                                  new String[] { StorageHelper.FOLLOWERS_USER_ID },
                                  StorageHelper.FOLLOWERS_USER_ID + " = ?", new String[] { userId },
                                  null, null,
                                  null );
        boolean followingResult = result.moveToNext();
        result.close();
        return followingResult;
    }

    private void checkAndRefresh( SQLiteDatabase db ) throws IOException {
        Cursor result = db.query( StorageHelper.FOLLOWERS_FOLLOWING_STATISTICS_TABLE,
                                  new String[] { StorageHelper.FOLLOWERS_FOLLOWING_STATISTICS_LAST_UPDATE },
                                  null, null,
                                  null, null,
                                  null );
        boolean willReload;
        if( !result.moveToNext() ) {
            willReload = true;
        }
        else {
            long lastUpdateDate = result.getLong( 0 );
            willReload = lastUpdateDate < System.currentTimeMillis() - FOLLOWERS_REFRESH_INTERVAL;
        }
        result.close();

        if( willReload ) {
            reloadCache( db );
        }
    }

    private void reloadCache( SQLiteDatabase db ) throws IOException {
        List<String> followerIds = buzzManager.loadFollowerIds();
        List<String> followingIds = buzzManager.loadFollowingIds();

        db.beginTransaction();
        try {
            db.delete( StorageHelper.FOLLOWERS_TABLE,
                       null, null );
            ContentValues values = new ContentValues();
            for( String id : followerIds ) {
                values.put( StorageHelper.FOLLOWERS_USER_ID, id );
                db.insert( StorageHelper.FOLLOWERS_TABLE,
                           StorageHelper.FOLLOWERS_USER_ID,
                           values );
            }

            db.delete( StorageHelper.FOLLOWING_TABLE,
                       null, null );
            values = new ContentValues();
            for( String id : followingIds ) {
                values.put( StorageHelper.FOLLOWING_USER_ID, id );
                db.insert( StorageHelper.FOLLOWING_TABLE,
                           StorageHelper.FOLLOWING_USER_ID,
                           values );
            }

            db.delete( StorageHelper.FOLLOWERS_FOLLOWING_STATISTICS_TABLE,
                       null, null );

            values = new ContentValues();
            values.put( StorageHelper.FOLLOWERS_FOLLOWING_STATISTICS_LAST_UPDATE, System.currentTimeMillis() );
            db.insert( StorageHelper.FOLLOWERS_FOLLOWING_STATISTICS_TABLE,
                       StorageHelper.FOLLOWERS_FOLLOWING_STATISTICS_LAST_UPDATE,
                       values );

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
}

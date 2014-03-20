package org.atari.dhs.buzztest.androidclient.displaywatchlist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.SQLiteDatabaseWrapper;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.CachedFeedActivity;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class LoadWatchlistTask extends AsyncTaskWrapper<LoadWatchlistTask.Args, Void, LoadWatchlistTask.Result>
{
    private Args args;

    @Override
    protected Result doInBackground( Args... argsList ) {
        args = argsList[0];

        SQLiteDatabase db = args.dbWrapper.getDatabase();
        Cursor result = db.query( StorageHelper.WATCHLIST_FEEDS_TABLE,
                                  new String[] { StorageHelper.WATCHLIST_FEEDS_ID, StorageHelper.WATCHLIST_FEEDS_CREATED_DATE },
                                  null, null,
                                  null, null,
                                  StorageHelper.WATCHLIST_FEEDS_CREATED_DATE + " desc" );
        List<String> activityIds = new ArrayList<String>();
        while( result.moveToNext() ) {
            activityIds.add( result.getString( 0 ) );
        }
        result.close();

        try {
            List<BuzzActivity> activities = new ArrayList<BuzzActivity>( activityIds.size() );
            for( String activityId : activityIds ) {
                BuzzActivity buzz = args.buzzManager.loadActivity( activityId, false );
                activities.add( buzz );
            }
            return new Result( makeCachedFeed( activities ) );
        }
        catch( IOException e ) {
            Log.w( "exception when loading buzz", e );
            return new Result( e.getLocalizedMessage() );
        }
    }

    private CachedFeedActivity[] makeCachedFeed( List<BuzzActivity> activities ) {
        DateHelper dateHelper = new DateHelper();

        CachedFeedActivity[] ret = new CachedFeedActivity[activities.size()];

        int i = 0;
        for( BuzzActivity act : activities ) {
            ret[i++] = new CachedFeedActivity( act, dateHelper );
        }

        return ret;
    }

    @Override
    protected void onPostExecute( Result result ) {
        super.onPostExecute( result );
        ((DisplayWatchlist)getUnderlyingActivity()).processActivitiesLoaded( result );
    }

    @Override
    protected void onClose() {
        args.dbWrapper.close();
        args.buzzManager.close();
        super.onClose();
    }

    static class Args
    {
        SQLiteDatabaseWrapper dbWrapper;
        BuzzManager buzzManager;

        Args( SQLiteDatabaseWrapper dbWrapper, BuzzManager buzzManager ) {
            this.dbWrapper = dbWrapper;
            this.buzzManager = buzzManager;
        }
    }

    static class Result
    {
        CachedFeedActivity[] activities;
        String errorMessage;

        Result( CachedFeedActivity[] activities ) {
            this.activities = activities;
        }

        Result( String errorMessage ) {
            this.errorMessage = errorMessage;
        }
    }
}

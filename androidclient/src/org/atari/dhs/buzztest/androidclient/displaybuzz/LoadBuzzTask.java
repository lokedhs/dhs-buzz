package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.io.IOException;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class LoadBuzzTask extends AsyncTaskWrapper<LoadBuzzTask.Args,Void, LoadBuzzTask.LoadResult>
{
    @Override
    protected LoadResult doInBackground( Args... argsList ) {
        Args args = argsList[0];

        try {
            return new LoadResult( args.buzzManager.loadActivity( args.activityId, false ), null, args.repliesForceReload );
        }
        catch( IOException e ) {
            Log.e( "error while loading buzz", e );
            return new LoadResult( null, e.getLocalizedMessage(), false );
        }
    }

    @Override
    protected void onPostExecute( LoadResult result ) {
        super.onPostExecute( result );

        DisplayBuzz underlyingActivity = (DisplayBuzz)getUnderlyingActivity();
        underlyingActivity.handleBackgroundLoadResult( result );
    }

    static class Args
    {
        String activityId;
        BuzzManager buzzManager;
        boolean repliesForceReload;

        Args( String activityId, BuzzManager buzzManager, boolean repliesForceReload ) {
            this.activityId = activityId;
            this.buzzManager = buzzManager;
            this.repliesForceReload = repliesForceReload;
        }
    }

    static class LoadResult
    {
        BuzzActivity buzz;
        String errorMessage;
        boolean repliesForceReload;

        LoadResult( BuzzActivity buzz, String errorMessage, boolean repliesForceReload ) {
            this.buzz = buzz;
            this.errorMessage = errorMessage;
            this.repliesForceReload = repliesForceReload;
        }
    }
}

package org.atari.dhs.buzztest.androidclient.displayfeed;

import java.io.IOException;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.CachedFeed;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivityFeed;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class LoadNextUnCachedTask extends AsyncTaskWrapper<LoadNextUnCachedTask.Args,Void,LoadNextUnCachedTask.Result>
{
    private Args args;

    @Override
    protected Result doInBackground( Args... argsList ) {
        args = argsList[0];

        try {
            BuzzActivityFeed feed = args.buzzManager.loadAndParse( args.nextLink, BuzzActivityFeed.class );
            return new Result( new CachedFeed( feed ) );
        }
        catch( IOException e ) {
            Log.w( "error loading next link", e );
            return new Result( e.getLocalizedMessage() );
        }
    }

    @Override
    protected void onPostExecute( Result result ) {
        super.onPostExecute( result );
        ((DisplayFeed)getUnderlyingActivity()).processLoadNextLinkResult( result );
    }

    @Override
    protected void onClose() {
        super.onClose();
        args.buzzManager.close();
    }

    static class Args
    {
        private BuzzManager buzzManager;
        private String nextLink;

        public Args( BuzzManager buzzManager, String nextLink ) {
            this.buzzManager = buzzManager;
            this.nextLink = nextLink;
        }
    }

    static class Result
    {
        CachedFeed feed;
        String errorMessage;

        Result( CachedFeed feed ) {
            this.feed = feed;
        }

        Result( String errorMessage ) {
            this.errorMessage = errorMessage;
        }
    }
}

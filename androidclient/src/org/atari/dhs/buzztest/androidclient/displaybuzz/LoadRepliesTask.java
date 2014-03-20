package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.io.IOException;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Replies;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply.BuzzReplyFeed;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class LoadRepliesTask extends AsyncTaskWrapper<LoadRepliesTask.Args, Void, LoadRepliesTask.LoadRepliesResult>
{
    private Args args;

    @Override
    protected LoadRepliesResult doInBackground( Args... args ) {
        try {
            this.args = args[0];
            BuzzReplyFeed replies;
            switch( this.args.repliesLoadCacheStrategy ) {
                case ONLY_IF_CACHED:
                    replies = this.args.buzzManager.loadCachedRepliesIfAvailable( this.args.activityId,
                                                                                  getUpdatedDateFromReplies( this.args.replies ),
                                                                                  true );
                    break;
                case PREFER_CACHE:
                    replies = this.args.buzzManager.loadCachedRepliesIfAvailable( this.args.activityId,
                                                                                  getUpdatedDateFromReplies( this.args.replies ),
                                                                                  false );
                    break;
                case FORCE_NETWORK:
                    replies = this.args.buzzManager.loadReplies( this.args.activityId );
                    break;
                default:
                    throw new IllegalStateException( "illegal cache strategy: " + this.args.repliesLoadCacheStrategy );
            }

            if( replies != null ) {
                updateLastReadDate();
            }

            return new LoadRepliesResult( replies, null );
        }
        catch( IOException e ) {
            Log.w( "IOException when downloading replies feed", e );
            return new LoadRepliesResult( null, e.getLocalizedMessage() );
        }
    }

    private long getUpdatedDateFromReplies( Replies replies ) {
        DateHelper dateHelper = new DateHelper();
        long date = dateHelper.parseDate( this.args.replies.updated ).getTime();
        return date;
    }

    private void updateLastReadDate() {
        args.buzzManager.updateLastReadDate( args.activityId, args.updatedLastReadTime );
    }

    @Override
    protected void onPostExecute( LoadRepliesResult result ) {
        super.onPostExecute( result );
        DisplayBuzz displayBuzz = (DisplayBuzz)getUnderlyingActivity();
        if( displayBuzz != null ) {
            displayBuzz.handleReplyFeedResult( result.feed, args.repliesLoadCacheStrategy != RepliesLoadCacheStrategy.ONLY_IF_CACHED, result.error );
        }
        args.buzzManager.close();
    }

    static class Args
    {
        private String activityId;
        private Replies replies;
        private BuzzManager buzzManager;
        private RepliesLoadCacheStrategy repliesLoadCacheStrategy;
        private long updatedLastReadTime;

        Args( String activityId, Replies replies, BuzzManager buzzManager, RepliesLoadCacheStrategy repliesLoadCacheStrategy, long updatedLastReadTime ) {
            this.activityId = activityId;
            this.replies = replies;
            this.buzzManager = buzzManager;
            this.repliesLoadCacheStrategy = repliesLoadCacheStrategy;
            this.updatedLastReadTime = updatedLastReadTime;
        }
    }

    static class LoadRepliesResult
    {
        BuzzReplyFeed feed;
        String error;

        LoadRepliesResult( BuzzReplyFeed feed, String error ) {
            this.feed = feed;
            this.error = error;
        }
    }
}

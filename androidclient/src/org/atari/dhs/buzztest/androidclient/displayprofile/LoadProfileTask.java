package org.atari.dhs.buzztest.androidclient.displayprofile;

import java.io.IOException;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.User;
import org.atari.dhs.buzztest.androidclient.followers.FollowersManager;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class LoadProfileTask extends AsyncTaskWrapper<LoadProfileTask.Args, Void, LoadProfileTask.Result>
{
    private Args args;

    @Override
    protected Result doInBackground( Args... argsList ) {
        args = argsList[0];

        User user = null;
        try {
            if( !args.inhibitProfileLoad ) {
                user = args.buzzManager.loadProfile( args.userId );
            }

            boolean isFollowing = false;
            boolean isFollowedBy = false;
            if( !args.inhibitFollowingLoad ) {
                isFollowing = args.followersManager.isFollowingUser( args.userId );
                isFollowedBy = args.followersManager.isFollowedByUser( args.userId );
            }

            return new Result( user, isFollowing, isFollowedBy );
        }
        catch( IOException e ) {
            Log.e( "error loading profile", e );
            return new Result( e.getLocalizedMessage() );
        }
    }

    @Override
    protected void onPostExecute( Result result ) {
        super.onPostExecute( result );

        ((ProfileLoadable)getUnderlyingActivity()).handleLoadResult( result );

        args.buzzManager.close();
        args.followersManager.close();
    }

    static class Args
    {
        String userId;
        boolean inhibitProfileLoad;
        private boolean inhibitFollowingLoad;
        BuzzManager buzzManager;
        FollowersManager followersManager;

        Args( String userId, boolean inhibitProfileLoad, boolean inhibitFollowingLoad, BuzzManager buzzManager, FollowersManager followersManager ) {
            this.userId = userId;
            this.inhibitProfileLoad = inhibitProfileLoad;
            this.inhibitFollowingLoad = inhibitFollowingLoad;
            this.buzzManager = buzzManager;
            this.followersManager = followersManager;
        }
    }

    static class Result
    {
        User user;
        boolean following;
        boolean follower;
        String errorMessage;

        Result( User user, boolean isFollowing, boolean isFollower ) {
            this.user = user;
            following = isFollowing;
            follower = isFollower;
        }

        public Result( String errorMessage ) {
            this.errorMessage = errorMessage;
        }
    }

    interface ProfileLoadable
    {
        void handleLoadResult( Result result );
    }
}

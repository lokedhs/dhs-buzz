package org.atari.dhs.buzztest.androidclient.userlist;

import java.io.IOException;

import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.UsersList;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class LoadUserListTask extends AsyncTaskWrapper<LoadUserListTask.Args,Void,LoadUserListTask.Result>
{
    private Args args;

    @Override
    protected Result doInBackground( Args... argsList ) {
        args = argsList[0];

        try {
            UsersList users = args.buzzManager.loadAndParse( args.url, UsersList.class );
            return new Result( users );
        }
        catch( IOException e ) {
            return new Result( e.getLocalizedMessage() );
        }
    }

    @Override
    protected void onPostExecute( Result result ) {
        super.onPostExecute( result );

        ((UserList)getUnderlyingActivity()).processLoadResult( result );

        args.buzzManager.close();
    }

    static class Args
    {
        String url;
        BuzzManager buzzManager;

        Args( String url, BuzzManager buzzManager ) {
            this.url = url;
            this.buzzManager = buzzManager;
        }
    }

    static class Result
    {
        UsersList users;
        String errorMessage;

        Result( UsersList users ) {
            this.users = users;
        }

        Result( String errorMessage ) {
            this.errorMessage = errorMessage;
        }
    }
}

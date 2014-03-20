package org.atari.dhs.buzztest.androidclient.groups;

import java.io.IOException;

import android.content.Context;

import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.groups.GroupsResult;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class LoadGroupsTask extends AsyncTaskWrapper<Void, Void, LoadGroupsTask.Result>
{
    private BuzzManager buzzManager;

    LoadGroupsTask( Context context ) {
        buzzManager = BuzzManager.createFromContext( context );
    }

    @Override
    protected Result doInBackground( Void... voids ) {
        try {
            GroupsResult groups = buzzManager.loadGroups();
            return new Result( groups );
        }
        catch( IOException e ) {
            return new Result( e.getLocalizedMessage() );
        }
    }

    @Override
    protected void onPostExecute( Result result ) {
        ((GroupSelector)getUnderlyingActivity()).handleLoadResult( result );
    }

    @Override
    protected void onClose() {
        buzzManager.close();
    }

    static class Result
    {
        GroupsResult groups;
        String errorMessage;

        Result( GroupsResult groups ) {
            this.groups = groups;
        }

        Result( String errorMessage ) {
            this.errorMessage = errorMessage;
        }
    }
}

package org.atari.dhs.buzztest.androidclient.userlist;

import java.io.IOException;

import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.UsersList;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class SearchUserTask extends AsyncTaskWrapper<SearchUserTask.Args,Void,SearchUserTask.Result>
{
    public static final int ITEMS_PER_PAGE = 20;

    private Args args;

    @Override
    protected Result doInBackground( Args... argsList ) {
        args = argsList[0];
        UsersList usersList;
        try {
            switch( args.searchType ) {
                case NAME:
                    usersList = args.buzzManager.searchUsers( args.text, ITEMS_PER_PAGE, args.startIndex );
                    break;

                case TOPIC:
                    usersList = args.buzzManager.searchUsersByTopic( args.text, ITEMS_PER_PAGE, args.startIndex );
                    break;

                default:
                    throw new IllegalStateException( "illegal search type: " + args.searchType );
            }
            return new Result( usersList, args.startIndex );
        }
        catch( IOException e ) {
            return new Result( e.getLocalizedMessage() );
        }
    }

    @Override
    protected void onPostExecute( Result result ) {
        super.onPostExecute( result );

        ((SearchUserActivity)getUnderlyingActivity()).processIncomingSearchResult( result );
    }

    @Override
    protected void onClose() {
        super.onClose();
        args.buzzManager.close();
    }

    static class Args
    {
        String text;
        int startIndex;
        UserSearchType searchType;
        BuzzManager buzzManager;

        Args( String text, int startIndex, UserSearchType searchType, BuzzManager buzzManager ) {
            this.text = text;
            this.startIndex = startIndex;
            this.searchType = searchType;
            this.buzzManager = buzzManager;
        }
    }

    static class Result
    {
        UsersList userList;
        String errorMessage;
        int startIndex;

        Result( UsersList userList, int startIndex ) {
            this.userList = userList;
            this.startIndex = startIndex;
        }

        Result( String errorMessage ) {
            this.errorMessage = errorMessage;
        }
    }
}

package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;
import java.text.MessageFormat;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.service.*;

public class UpdateFollowingTask implements ServiceTask
{
    public static final String COMMAND_NAME = "updateFollowing";
    public static final String USER_ID_KEY = "userId";
    public static final String USER_NAME_KEY = "userName";
    public static final String START_KEY = "start";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String userId = args.getStringWithCheck( USER_ID_KEY );
        boolean start = args.getBooleanWithCheck( START_KEY );

        try {
            BuzzManager buzzManager = taskContext.getBuzzManager();
            if( start ) {
                buzzManager.startFollow( userId );
            }
            else {
                buzzManager.stopFollow( userId );
            }
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new RestartableServiceTaskFailedException( msg, e );
        }

        Intent intent = new Intent( IntentHelper.ACTION_FOLLOWING_UPDATED );
        intent.putExtra( IntentHelper.EXTRA_USER_ID, userId );
        intent.putExtra( IntentHelper.EXTRA_FOLLOWING_TYPE_START, start );
        taskContext.getService().sendBroadcast( intent );
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        String userName = args.getString( USER_NAME_KEY );
        if( userName == null ) {
            userName = "unknown";
        }

        Resources resources = context.getResources();
        if( args.getBooleanWithCheck( START_KEY ) ) {
            String fmt = resources.getString( R.string.task_update_watchlist_ticker_add );
            return MessageFormat.format( fmt, userName );
        }
        else {
            String fmt = resources.getString( R.string.task_update_watchlist_ticker_remove );
            return MessageFormat.format( fmt, userName );
        }
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.service.*;

public class LoadMoreTask implements ServiceTask
{
    public static final String COMMAND_NAME = "loadMore";
    public static final String STORED_FEED_VALUE_KEY = "storedFeedKey";
    public static final String ACTIVITY_ID_KEY = "activityId";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityId = args.getString( ACTIVITY_ID_KEY );
        String storedFeedKey = args.getString( STORED_FEED_VALUE_KEY );
        if( activityId == null ) {
            throw new IllegalStateException( "missing activity id" );
        }

        BuzzManager buzzManager = taskContext.getBuzzManager();

        boolean wasUpdated;
        try {
            wasUpdated = buzzManager.loadMore( storedFeedKey, activityId );
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new ReportableServiceTaskFailedException( msg, e );
        }
        catch( ClassNotFoundException e ) {
            throw new ServiceTaskFailedException( "exception when loading more link", e );
        }

        if( wasUpdated ) {
            Intent intent = new Intent( IntentHelper.ACTION_BUZZ_FEED_UPDATED );
            intent.putExtra( IntentHelper.EXTRA_FEED_ID, activityId );
            Log.i( "sending extra for activity ID: " + activityId );
            taskContext.getService().sendBroadcast( intent );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return context.getResources().getString( R.string.task_load_more_ticker );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

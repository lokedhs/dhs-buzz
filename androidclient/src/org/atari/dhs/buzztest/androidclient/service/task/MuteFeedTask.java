package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzGlobal;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.service.*;

public class MuteFeedTask implements ServiceTask
{
    public static final String COMMAND_NAME = "muteFeed";
    public static final String ACTIVITY_ID_KEY = "activityId";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityId = args.getString( ACTIVITY_ID_KEY );
        if( activityId == null ) {
            throw new IllegalStateException( "missing activity id" );
        }

        BuzzManager buzzManager = taskContext.getBuzzManager();
        String url = BuzzGlobal.ROOT_URL + "activities/@me/@muted/" + activityId;
        try {
            buzzManager.putEmptyBuzzActivity( url );
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new RestartableServiceTaskFailedException( msg, e );
        }

        Intent intent = new Intent( PostMessageService.ACTION_HIDE_BUZZ_FROM_FEED );
        intent.putExtra( PostMessageService.EXTRA_BUZZ_ID, activityId );
        taskContext.getService().sendBroadcast( intent );
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return context.getResources().getString( R.string.task_mute_feed_ticker );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

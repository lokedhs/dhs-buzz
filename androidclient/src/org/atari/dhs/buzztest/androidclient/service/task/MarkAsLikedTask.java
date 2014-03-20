package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;

import android.content.Context;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzGlobal;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.service.*;

public class MarkAsLikedTask implements ServiceTask
{
    public static final String COMMAND_NAME = "markAsLiked";
    public static final String ACTIVITY_ID_KEY = "activityId";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityId = args.getString( ACTIVITY_ID_KEY );
        if( activityId == null ) {
            throw new IllegalStateException( "missing activity id" );
        }

        BuzzManager buzzManager = taskContext.getBuzzManager();
        try {
            buzzManager.putEmptyBuzzActivity( BuzzGlobal.ROOT_URL + "activities/@me/@liked/" + activityId );
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new RestartableServiceTaskFailedException( msg, e );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return context.getResources().getString( R.string.task_mark_as_liked_ticker );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

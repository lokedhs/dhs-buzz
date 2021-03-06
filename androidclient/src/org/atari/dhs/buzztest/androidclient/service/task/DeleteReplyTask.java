package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;

import android.content.Context;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.service.*;

public class DeleteReplyTask implements ServiceTask
{
    public static final String COMMAND_NAME = "deleteReply";

    public static final String ACTIVITY_ID_KEY = "activityId";
    public static final String REPLY_ID_KEY = "replyId";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityId = args.getStringWithCheck( ACTIVITY_ID_KEY );
        String replyId = args.getStringWithCheck( REPLY_ID_KEY );

        BuzzManager buzzManager = taskContext.getBuzzManager();
        try {
            buzzManager.deleteReply( activityId, replyId );
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new RestartableServiceTaskFailedException( msg, e );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return context.getResources().getString( R.string.task_delete_reply_ticker );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

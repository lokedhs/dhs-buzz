package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;

import android.content.Context;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.service.*;

public class EditPostTask implements ServiceTask
{
    public static final String COMMAND_NAME = "editPost";

    public static final String ACTIVITY_ID_KEY = "activityId";
    public static final String TEXT_KEY = "content";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityId = args.getStringWithCheck( ACTIVITY_ID_KEY );
        String content = args.getStringWithCheck( TEXT_KEY );

        BuzzManager buzzManager = taskContext.getBuzzManager();
        try {
            buzzManager.editPost( activityId, content );
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new RestartableServiceTaskFailedException( msg, e );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return context.getResources().getString( R.string.task_edit_post_ticker );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

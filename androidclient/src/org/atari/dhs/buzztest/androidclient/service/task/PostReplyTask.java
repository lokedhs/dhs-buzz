package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;

import android.content.Context;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.service.*;

public class PostReplyTask implements ServiceTask
{
    public static final String COMMAND_NAME = "postReply";

    public static final String ACTIVITY_ID_KEY = "activityId";
    public static final String TEXT_KEY = "content";
    public static final String SUBSCRIBE_REALTIME_KEY = "subscribe";

    private static final String ONLY_SUBSCRIBE_REALTIME_KEY = "postCompleted";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityId = args.getStringWithCheck( ACTIVITY_ID_KEY );
        String content = args.getStringWithCheck( TEXT_KEY );
        boolean subscribeRealtime = args.getBooleanWithCheck( SUBSCRIBE_REALTIME_KEY );

        boolean onlySubscribeRealtime = !args.getBoolean( ONLY_SUBSCRIBE_REALTIME_KEY, false );
        if( onlySubscribeRealtime ) {
            BuzzManager buzzManager = taskContext.getBuzzManager();
            try {
                buzzManager.postBuzzReply( activityId, content );
            }
            catch( IOException e ) {
                String msg = taskContext.getService().getResources().getString( R.string.error_communication );
                throw new RestartableServiceTaskFailedException( msg, e );
            }
        }

        if( subscribeRealtime ) {
            try {
                RegisterRealtimeTask.performRealtimeRegistration( taskContext, activityId, true );
            }
            catch( IOException e ) {
                String msg = taskContext.getService().getResources().getString( R.string.error_communication );
                BundleX newArgs = new BundleX( args );
                newArgs.putBoolean( ONLY_SUBSCRIBE_REALTIME_KEY, true );
                throw new RestartableServiceTaskFailedException( msg, e, newArgs );
            }
        }
        else if( onlySubscribeRealtime ) {
            Log.e( "ONLY_SUBSCRIBE_REALTIME set but SUBSCRIBE_REALTIME unset" );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return context.getResources().getString( R.string.task_post_reply_ticker );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzObject;
import org.atari.dhs.buzztest.androidclient.service.*;

public class ShareMessageTask implements ServiceTask
{
    public static final String COMMAND_NAME = "shareMessage";
    public static final String SHARED_ACTIVITY_ID_KEY = "activityId";
    public static final String ANNOTATION_KEY = "comment";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityId = args.getStringWithCheck( SHARED_ACTIVITY_ID_KEY );
        String annotation = args.getString( ANNOTATION_KEY );

        BuzzManager buzzManager = taskContext.getBuzzManager();
        BuzzActivity activity = new BuzzActivity();

        activity.object = new BuzzObject();
        activity.object.id = activityId;

        activity.verbs = new ArrayList<String>();
        activity.verbs.add( "share" );

        activity.annotation = annotation;

//        try {
//            StringWriter out = new StringWriter();
//            JsonGenerator generator = Json.JSON_FACTORY.createJsonGenerator( out );
//            Json.serialize( generator, activity );
//            generator.flush();
//            Log.i( "message:" + out.toString() );
//        }
//        catch( IOException e ) {
//            Log.e( "exception when generating JSON data", e );
//        }

        try {
            buzzManager.postActivity( activity );
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new RestartableServiceTaskFailedException( msg, e );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return "Posting reshare";
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

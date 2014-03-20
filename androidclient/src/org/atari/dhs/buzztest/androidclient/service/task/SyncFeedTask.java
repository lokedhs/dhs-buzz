package org.atari.dhs.buzztest.androidclient.service.task;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Replies;
import org.atari.dhs.buzztest.androidclient.service.*;
import org.atari.dhs.buzztest.androidclient.settings.AutoDownloadRepliesOption;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

public class SyncFeedTask implements ServiceTask
{
    public static final String COMMAND_NAME = "syncFeed";
    public static final String STORED_FEED_VALUE_KEY = "storedFeedKey";
    public static final String ACTIVITY_URL_KEY = "activityUrl";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String activityUrl = args.getString( ACTIVITY_URL_KEY );
        String storedFeedKey = args.getString( STORED_FEED_VALUE_KEY );
        BuzzManager buzzManager = taskContext.getBuzzManager();
        BuzzManager.LoadActivitiesResult activities;
        try {
            activities = buzzManager.loadActivities( storedFeedKey, activityUrl );
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new ReportableServiceTaskFailedException( msg, e );
        }

        int numUpdated = 0;
        if( activities.lastRead != 0 && activities.feed.activities != null ) {
            DateHelper dateHelper = new DateHelper();
            for( BuzzActivity activity : activities.feed.activities ) {
                long activityUpdateDate = dateHelper.parseDate( activity.updated ).getTime();
                if( activityUpdateDate > activities.lastRead ) {
                    numUpdated++;
                }
            }
        }

        Intent intent = new Intent( IntentHelper.ACTION_BUZZ_FEED_UPDATED );
        intent.putExtra( IntentHelper.EXTRA_FEED_ID, activityUrl );
        intent.putExtra( IntentHelper.EXTRA_STORED_FEED_KEY, storedFeedKey );
        intent.putExtra( IntentHelper.EXTRA_NUM_UPDATED, numUpdated );
        taskContext.getService().sendBroadcast( intent );

        PreferencesManager prefs = new PreferencesManager( taskContext.getService() );
        AutoDownloadRepliesOption autoDownloadReplies = prefs.getAutoDownloadReplies();
        if( autoDownloadReplies == AutoDownloadRepliesOption.ALWAYS ) {
            if( activities.feed.activities != null ) {
                try {
                    DateHelper dateHelper = new DateHelper();
                    for( BuzzActivity activity : activities.feed.activities ) {
                        Replies replies = activity.findReplies();
                        if( replies != null && replies.count > 0 ) {
                            long updateDate = dateHelper.parseDate( replies.updated ).getTime();
                            buzzManager.loadCachedRepliesIfAvailable( activity.id, updateDate, false );
                        }
                    }
                }
                catch( IOException e ) {
                    Log.e( "error downloading replies", e );
                }
            }
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        return context.getResources().getString( R.string.task_sync_feed_ticker );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }
}

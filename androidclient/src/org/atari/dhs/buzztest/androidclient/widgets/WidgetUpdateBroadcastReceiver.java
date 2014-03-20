package org.atari.dhs.buzztest.androidclient.widgets;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.displayfeed.DisplayFeed;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceImpl;

public class WidgetUpdateBroadcastReceiver extends BroadcastReceiver
{
    public void onReceive( Context context, Intent intent ) {
        String action = intent.getAction();
        Log.i( "got widget update event: " + action + ", date=" + new Date() );
        if( action != null && action.equals( IntentHelper.ACTION_UPDATE_BUZZ_WIDGET ) ) {
            try {
                PostMessageServiceImpl.startSyncWithContext( context,
                                                             DisplayFeed.STORED_FEED_KEY_PERSONAL_FEED,
                                                             BuzzManager.getConsumptionFeedUrl(),
                                                             true );
            }
            catch( RemoteException e ) {
                // Not much we can do if this happens.
                Log.e( "error when requesting sync", e );
            }
        }
    }
}

package org.atari.dhs.buzztest.androidclient.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.displayfeed.DisplayFeed;
import org.atari.dhs.buzztest.androidclient.service.PostMessageService;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

public class DhsBuzzWidget extends AppWidgetProvider
{
    private int numUpdated;

    @Override
    public void onEnabled( Context context ) {
        super.onEnabled( context );

        Log.i( "enabling widget updates" );

        AlarmManager alarmManager = (AlarmManager)context.getSystemService( Context.ALARM_SERVICE );
        PendingIntent pendingIntent = makeUpdateBuzzWidgetIntent( context );
        alarmManager.setInexactRepeating( AlarmManager.RTC_WAKEUP,
                                          System.currentTimeMillis() + DateHelper.MINUTE_MILLIS,
                                          AlarmManager.INTERVAL_HOUR,
                                          pendingIntent );
//        alarmManager.setRepeating( AlarmManager.RTC_WAKEUP,
//                                          System.currentTimeMillis() + DateHelper.MINUTE_MILLIS,
//                                          2 * DateHelper.MINUTE_MILLIS,
//                                          pendingIntent );
    }

    @Override
    public void onDisabled( Context context ) {
        Log.i( "disabling widget updates" );

        AlarmManager alarmManager = (AlarmManager)context.getSystemService( Context.ALARM_SERVICE );
        PendingIntent pendingIntent = makeUpdateBuzzWidgetIntent( context );
        alarmManager.cancel( pendingIntent );

        super.onDisabled( context );
    }

    private PendingIntent makeUpdateBuzzWidgetIntent( Context context ) {
        Intent intent = new Intent( IntentHelper.ACTION_UPDATE_BUZZ_WIDGET );
        intent.putExtra( PostMessageService.EXTRA_SERVICE_CALL, PostMessageService.SERVICE_CALL_SYNC_PERSONAL_FEED );
        return PendingIntent.getBroadcast( context, 0, intent, 0 );
    }

    @Override
    public void onUpdate( Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds ) {
        super.onUpdate( context, appWidgetManager, appWidgetIds );

        Log.i( "widget updated" );
        updateWidgets( context, appWidgetManager, appWidgetIds );
    }

    @Override
    public void onReceive( Context context, Intent intent ) {
        super.onReceive( context, intent );

        Log.i( "got broadcast: " + intent.getAction() );
        if( intent.getAction().equals( IntentHelper.ACTION_BUZZ_FEED_UPDATED ) ) {
            String feedKey = intent.getStringExtra( IntentHelper.EXTRA_STORED_FEED_KEY );
            if( DisplayFeed.STORED_FEED_KEY_PERSONAL_FEED.equals( feedKey ) ) {
                numUpdated = intent.getIntExtra( IntentHelper.EXTRA_NUM_UPDATED, 0 );

                AppWidgetManager mgr = AppWidgetManager.getInstance( context );
                ComponentName componentName = new ComponentName( context.getPackageName(), DhsBuzzWidget.class.getName() );
                int[] widgetIds = mgr.getAppWidgetIds( componentName );
                updateWidgets( context, mgr, widgetIds );
            }
        }
    }

    private void updateWidgets( Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds ) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for( int appWidgetId : appWidgetIds ) {
            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent( context, DisplayFeed.class );
            intent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_CONSUMPTION );
            PendingIntent pendingIntent = PendingIntent.getActivity( context, 0, intent, 0 );

            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews( context.getPackageName(), R.layout.widget );
//            views.setOnClickPendingIntent( R.id.view_feed_button, pendingIntent );
            views.setTextViewText( R.id.personal_feed_number_of_new, String.valueOf( numUpdated ) );

            // Tell the AppWidgetManager to perform an update on the current App Widget
            appWidgetManager.updateAppWidget( appWidgetId, views );
        }
    }
}

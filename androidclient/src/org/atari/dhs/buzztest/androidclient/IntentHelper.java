package org.atari.dhs.buzztest.androidclient;

import android.content.Context;
import android.content.Intent;

import com.google.common.base.Preconditions;

import org.atari.dhs.buzztest.androidclient.displaybuzz.DisplayBuzz;

public class IntentHelper
{
    public static final String ACTION_REPLY_FEED_UPDATED = "org.atari.dhs.buzz.REPLY_FEED_UPDATED";
    public static final String ACTION_BUZZ_FEED_UPDATED = "org.atari.dhs.buzz.NEW_FEED";
    public static final String ACTION_BUZZ_ACTIVITY_REALTIME_CHANGED = "org.atari.dhs.buzz.REALTIME_CHANGED";
    public static final String ACTION_BUZZ_ACTIVITY_UPDATED = "org.atari.dhs.buzz.BUZZ_ACTIVITY_UPDATED";
    public static final String ACTION_WATCHLIST_UPDATED = "org.atari.dhs.buzz.WATCHLIST_UPDATED";
    public static final String ACTION_FOLLOWING_UPDATED = "org.atari.dhs.buzz.FOLLOWING_UPDATED";
    public static final String ACTION_UPDATE_BUZZ_WIDGET = "org.atari.dhs.buzz.UPDATE_WIDGET";
    public static final String ACTION_SEARCH_FEED_UPDATED = "org.atari.dhs.buzz.SEARCH_FEED_UPDATED";

    public static final String EXTRA_FEED_ID = "feedName";
    public static final String EXTRA_FEED_TITLE = "feedTitle";
    public static final String EXTRA_REALTIME_MODE = "mode";
    public static final String EXTRA_SEARCH_FEED_NAME = "name";

    public static final String EXTRA_REPLIES_FORCE_RELOAD = "forceReload";

    public static final String EXTRA_ACTIVITY_ID = "activityId";
    public static final String EXTRA_STORED_FEED_KEY = "storedFeedKey";
    public static final String EXTRA_NUM_UPDATED = "numUpdated";
    public static final String EXTRA_WATCHLIST_UPDATE_TYPE_ADD = "add";
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_FOLLOWING_TYPE_START = "start";

    private IntentHelper() {
        // prevent instantiation
    }

    public static Intent makeReplyFeedUpdatedIntent( String activityId ) {
        Preconditions.checkNotNull( activityId );

        Intent intent = new Intent( ACTION_REPLY_FEED_UPDATED );
        intent.putExtra( EXTRA_ACTIVITY_ID, activityId );
        return intent;
    }

    public static Intent makeDisplayBuzzIntent( Context context, String activityId, boolean forceReload ) {
        Preconditions.checkNotNull( activityId );

        Intent intent = new Intent( context, DisplayBuzz.class );
        intent.putExtra( EXTRA_ACTIVITY_ID, activityId );
        intent.putExtra( EXTRA_REPLIES_FORCE_RELOAD, forceReload );
        return intent;
    }

    public static Intent makeActivityUpdatedIntent( String activityId ) {
        Preconditions.checkNotNull( activityId );

        Intent intent = new Intent( ACTION_BUZZ_ACTIVITY_UPDATED );
        intent.putExtra( EXTRA_ACTIVITY_ID, activityId );
        return intent;
    }
}

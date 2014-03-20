package org.atari.dhs.buzztest.androidclient.buzz;

import java.io.Serializable;
import java.util.List;

import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivityFeed;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

public class CachedFeed implements Serializable
{
    private static final int MAX_FEED_LENGTH = 40;

    private CachedFeedActivity[] messages;
    private String loadNextLink;

    public CachedFeed() {
        messages = new CachedFeedActivity[0];
        loadNextLink = null;
    }

    public CachedFeed( BuzzActivityFeed feed ) {
        loadNextLink = feed.findNextLink();

        DateHelper dateHelper = new DateHelper();

        CachedFeedActivity[] ids;
        List<BuzzActivity> actList = feed.activities;
        if( actList == null ) {
            ids = new CachedFeedActivity[0];
        }
        else {
            ids = new CachedFeedActivity[actList.size()];
            int i = 0;
            for( BuzzActivity act : actList ) {
                ids[i++] = new CachedFeedActivity( act, dateHelper );
            }
        }
        messages = ids;
    }

    public CachedFeedActivity[] getMessages() {
        return messages;
    }

    public String getLoadNextLink() {
        return loadNextLink;
    }

    public void addActivityAndShiftDown( CachedFeedActivity activity ) {
        int foundIndex = -1;
        for( int i = 0 ; i < messages.length ; i++ ) {
            if( messages[i].getId().equals( activity.getId() ) ) {
                foundIndex = i;
                break;
            }
        }

        CachedFeedActivity[] newFeed;
        if( foundIndex >= 0 ) {
            if( foundIndex > 0 ) {
                System.arraycopy( messages, 0, messages, 1, foundIndex );
            }
            messages[0] = activity;
        }
        else {
            if( messages.length < MAX_FEED_LENGTH ) {
                newFeed = new CachedFeedActivity[messages.length + 1];
                System.arraycopy( messages, 0, newFeed, 1, messages.length );
            }
            else {
                newFeed = new CachedFeedActivity[MAX_FEED_LENGTH];
                System.arraycopy( messages, 0, newFeed, 1, messages.length - 1 );
            }

            newFeed[0] = activity;
            messages = newFeed;
        }
    }

    public void addActivities( List<BuzzActivity> activities, String nextLink ) {
        if( activities != null ) {
            DateHelper dateHelper = new DateHelper();

            CachedFeedActivity[] newMessages = new CachedFeedActivity[messages.length + activities.size()];
            System.arraycopy( messages, 0, newMessages, 0, messages.length );

            int i = messages.length;
            for( BuzzActivity act : activities ) {
                newMessages[i++] = new CachedFeedActivity( act, dateHelper );
            }

            messages = newMessages;
            loadNextLink = nextLink;
        }
        else {
            loadNextLink = null;
        }
    }
}

package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;
import org.atari.dhs.buzztest.androidclient.Log;

public final class BuzzActivityFeed implements Serializable
{
    // Test URL: https://www.googleapis.com/buzz/v1/activities/lokedhs/@public?alt=json&prettyprint=true

    @Key("id")
    public String id;

    @Key("links")
    public BuzzActivityFeedLinks links;

    @Key("updated")
    public String updated;

    @Key("items")
    public List<BuzzActivity> activities;

    @Override
    public String toString() {
        return "BuzzActivityFeed[" +
               "id='" + id + '\'' +
               ", links=" + links +
               ", updated='" + updated + '\'' +
               ", activities=" + activities +
               ']';
    }

    public String findNextLink() {
        if( links == null || links.next == null || links.next.isEmpty() ) {
            return null;
        }

        if( links.next.size() > 1 ) {
            Log.w( "next link contains more than one entry" );
        }

        return links.next.get( 0 ).href;
    }
}

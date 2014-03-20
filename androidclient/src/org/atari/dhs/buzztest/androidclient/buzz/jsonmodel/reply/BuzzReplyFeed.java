package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class BuzzReplyFeed implements Serializable
{
    @Key("links")
    public ReplyLinks links;

    @Key("title")
    public String title;

    @Key("updated")
    public String updated;

    @Key("id")
    public String id;

    @Key("items")
    public List<BuzzReply> items;

    public String findNextLink() {
        if( links == null || links.next == null || links.next.isEmpty() ) {
            return null;
        }
        else {
            return links.next.get( 0 ).href;
        }
    }
}

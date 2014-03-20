package org.atari.dhs.buzztest.androidclient.buzz;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply.BuzzReplyFeed;

public class BuzzCache
{
    private static BuzzCache instance;

    private Map<String, FeedHolder<BuzzReplyFeed>> replyCache = new HashMap<String, FeedHolder<BuzzReplyFeed>>();

    private BuzzCache() {
        // prevent instantiation
    }

    public static synchronized BuzzCache getInstance() {
        if( instance == null ) {
            instance = new BuzzCache();
        }
        
        return instance;
    }

    public BuzzReplyFeed getReplyFeed( String id, Date updated ) {
        synchronized( replyCache ) {
            FeedHolder<BuzzReplyFeed> element = replyCache.get( id );
            if( element == null ) {
                return null;
            }
            else if( element.getUpdated().getTime() < updated.getTime() ) {
                return null;
            }
            else {
                return element.getFeed();
            }
        }
    }

    public void putReplyFeed( String id, Date updated, BuzzReplyFeed feed ) {
        synchronized( replyCache ) {
            replyCache.put( id, new FeedHolder<BuzzReplyFeed>( updated, feed ) );
        }
    }

    private static class FeedHolder<T>
    {
        private Date updated;
        private T feed;

        private FeedHolder( Date updated, T feed ) {
            this.updated = updated;
            this.feed = feed;
        }

        public Date getUpdated() {
            return updated;
        }

        public T getFeed() {
            return feed;
        }
    }
}

package org.atari.dhs.buzztest.androidclient.buzz;

public class PersistedFeedWrapper
{
    private CachedFeed feed;
    private long downloadDate;

    public PersistedFeedWrapper( CachedFeed feed, long downloadDate ) {
        this.feed = feed;
        this.downloadDate = downloadDate;
    }

    public CachedFeed getFeed() {
        return feed;
    }

    public long getDownloadDate() {
        return downloadDate;
    }
}

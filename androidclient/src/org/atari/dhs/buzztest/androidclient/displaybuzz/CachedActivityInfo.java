package org.atari.dhs.buzztest.androidclient.displaybuzz;

import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;

class CachedActivityInfo
{
    private BuzzActivity activity;
    private long lastReadTime;

    public CachedActivityInfo( BuzzActivity activity, long lastReadTime ) {
        this.activity = activity;
        this.lastReadTime = lastReadTime;
    }

    public BuzzActivity getActivity() {
        return activity;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }
}

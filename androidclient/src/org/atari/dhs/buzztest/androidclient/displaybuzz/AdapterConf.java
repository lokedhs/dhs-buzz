package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.util.List;

class AdapterConf
{
    public List<ReplyEntry> replies;
    public int lastUnreadPos;
    public boolean readRepliesCollapsed;

    public AdapterConf( List<ReplyEntry> replies, int lastUnreadPos, boolean readRepliesCollapsed ) {
        this.replies = replies;
        this.lastUnreadPos = lastUnreadPos;
        this.readRepliesCollapsed = readRepliesCollapsed;
    }
}

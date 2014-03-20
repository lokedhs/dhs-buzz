package org.atari.dhs.buzztest.server.c2dm.feed;

import java.io.Serializable;
import java.util.List;

public class FeedUpdateInfo implements Serializable
{
    private List<FeedUpdateInfoEntry> infoEntryList;

    public FeedUpdateInfo( List<FeedUpdateInfoEntry> infoEntryList ) {
        this.infoEntryList = infoEntryList;
    }

    public List<FeedUpdateInfoEntry> getInfoEntryList() {
        return infoEntryList;
    }

    @Override
    public String toString() {
        return "FeedUpdateInfo[" +
               "infoEntryList=" + infoEntryList +
               ']';
    }
}

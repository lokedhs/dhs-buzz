package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.groups;

import com.google.api.client.util.Key;

public class GroupItem
{
    @Key("id")
    public String id;

    @Key("title")
    public String title;

    @Key("links")
    public GroupItemLinks links;
}

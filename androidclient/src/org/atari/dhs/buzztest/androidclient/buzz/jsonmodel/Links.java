package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class Links implements Serializable
{
    @Key("liked")
    public List<Liked> liked;

    @Key("alternate")
    public List<Alternate> alternate;

    @Key("self")
    public List<Self> self;

    @Key("replies")
    public List<Replies> replies;
}

package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class Replies implements Serializable
{
    @Key("href")
    public String href;

    @Key("type")
    public String type;

    @Key("count")
    public int count;

    @Key("updated")
    public String updated;
}

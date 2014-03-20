package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class PhotoLink implements Serializable
{
    @Key("value")
    public String url;

    @Key("type")
    public String type;

    @Key("width")
    public int width;

    @Key("height")
    public int height;
}

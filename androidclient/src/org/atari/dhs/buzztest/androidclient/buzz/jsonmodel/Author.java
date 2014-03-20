package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class Author implements Serializable
{
    @Key("id")
    public String id;

    @Key("photoUrl")
    public String photoUrl;

    @Key("name")
    public String name;

    @Key("uri")
    public String uri;
}

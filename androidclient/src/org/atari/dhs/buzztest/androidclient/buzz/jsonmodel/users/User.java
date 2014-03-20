package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.users;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class User implements Serializable
{
    @Key("id")
    public String id;

    @Key("displayName")
    public String displayName;

    @Key("profileUrl")
    public String profileUrl;

    @Key("thumbnailUrl")
    public String thumbnailUrl;
}

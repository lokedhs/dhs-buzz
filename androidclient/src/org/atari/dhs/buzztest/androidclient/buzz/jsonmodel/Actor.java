package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class Actor implements Serializable
{
    @Key("id")
    public String id;

    @Key("name")
    public String name;

    @Key("profileUrl")
    public String profileUrl;

    @Key("thumbnailUrl")
    public String thumbnailUrl;

    @Override
    public String toString() {
        return "Actor[" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", profileUrl='" + profileUrl + '\'' +
               ", thumbnailUrl='" + thumbnailUrl + '\'' +
               ']';
    }
}

package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class Reshare implements Serializable
{
    @Key("original")
    public BuzzReshareRef original;

    @Key("sharedBy")
    public BuzzReshareRef sharedBy;

    public Author findOriginalPoster() {
        return original == null ? null : original.author;
    }
}

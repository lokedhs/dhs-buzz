package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class BuzzReshareRef implements Serializable
{
    @Key("author")
    public Author author;

    @Key("links")
    public ReshareLinksList links;
}

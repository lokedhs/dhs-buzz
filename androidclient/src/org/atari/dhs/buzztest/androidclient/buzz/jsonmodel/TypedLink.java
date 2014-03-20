package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class TypedLink implements Serializable
{
    @Key("href")
    public String href;

    @Key("type")
    public String type;
}

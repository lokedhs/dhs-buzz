package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class Liked implements Serializable
{
    @Key("count")
    public int count;
}

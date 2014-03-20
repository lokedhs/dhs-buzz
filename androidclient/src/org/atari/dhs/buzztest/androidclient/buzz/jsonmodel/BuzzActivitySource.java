package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class BuzzActivitySource implements Serializable
{
    @Key("title")
    public String title;
}

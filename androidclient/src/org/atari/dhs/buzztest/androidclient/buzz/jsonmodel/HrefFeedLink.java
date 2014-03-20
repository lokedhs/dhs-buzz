package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class HrefFeedLink implements Serializable
{
    @Key("href")
    public String href;
}

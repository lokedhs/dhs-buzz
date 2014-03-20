package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class BuzzActivityFeedLinks implements Serializable
{
    @Key("next")
    public List<HrefFeedLink> next;
}

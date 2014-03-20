package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.groups;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class GroupItemLink implements Serializable
{
    @Key("href")
    public String href;

    @Key("type")
    public String type;
}

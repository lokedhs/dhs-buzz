package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.groups;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class GroupItemLinks implements Serializable
{
    @Key("self")
    public List<GroupItemLink> self;
}

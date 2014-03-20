package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class OrganisationLink implements Serializable
{
    @Key("name")
    public String name;

    @Key("title")
    public String title;

    @Key("type")
    public String type;

    @Key("primary")
    public boolean primary;
}

package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class UsersList implements Serializable
{
    @Key("kind")
    public String kind;

    @Key("startIndex")
    public int startIndex;

    @Key("itemsPerPage")
    public int itemsPerPage;

    @Key("totalResults")
    public int totalResults;

    @Key("entry")
    public List<User> entry;
}

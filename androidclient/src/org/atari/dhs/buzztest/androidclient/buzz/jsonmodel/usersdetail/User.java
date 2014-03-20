package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class User implements Serializable
{
    @Key("kind")
    public String kind;

    @Key("id")
    public String id;

    @Key("displayName")
    public String displayName;

    @Key("profileUrl")
    public String profileUrl;

    @Key("thumbnailUrl")
    public String thumbnailUrl;

    @Key("aboutMe")
    public String aboutMe;

    @Key("urls")
    public List<UrlLink> urls;

    @Key("photos")
    public List<PhotoLink> photos;

    @Key("organizations")
    public List<OrganisationLink> organisations;

    @Key("interests")
    public List<String> interests;
}

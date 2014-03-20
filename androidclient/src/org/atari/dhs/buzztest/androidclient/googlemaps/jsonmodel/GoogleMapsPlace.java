package org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class GoogleMapsPlace implements Serializable
{
    @Key("name")
    public String name;

    @Key("vicinity")
    public String vicinity;

    @Key("types")
    public List<String> types;

    @Key("icon")
    public String icon;

    @Key("reference")
    public String reference;

    @Key("place_messages")
    public List<String> placeMessages;

    @Override
    public String toString() {
        return "GoogleMapsPlace[" +
               "name='" + name + '\'' +
               ", vicinity='" + vicinity + '\'' +
               ", types=" + types +
               ", icon='" + icon + '\'' +
               ", reference='" + reference + '\'' +
               ", placeMessages=" + placeMessages +
               ']';
    }
}

package org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class MapsLocation implements Serializable
{
    @Key("lat")
    public float lat;

    @Key("lng")
    public float lng;

    @Override
    public String toString() {
        return "MapsLocation[" +
               "lat=" + lat +
               ", lng=" + lng +
               ']';
    }
}

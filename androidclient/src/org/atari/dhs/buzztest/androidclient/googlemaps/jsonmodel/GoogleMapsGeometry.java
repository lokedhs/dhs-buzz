package org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class GoogleMapsGeometry implements Serializable
{
    @Key("location")
    public MapsLocation location;

    @Override
    public String toString() {
        return "GoogleMapsGeometry[" +
               "location=" + location +
               ']';
    }
}

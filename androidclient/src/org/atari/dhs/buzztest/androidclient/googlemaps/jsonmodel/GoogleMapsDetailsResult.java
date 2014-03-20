package org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class GoogleMapsDetailsResult implements Serializable
{
    @Key("status")
    public String status;

    @Key("result")
    public GoogleMapsDetailsContent result;
}

package org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class GoogleMapsPlaceResult implements Serializable
{
    @Key("status")
    public String status;

    @Key("results")
    public List<GoogleMapsPlace> results;

    @Key("html_attributions")
    public List<String> htmlAttributions;

    @Override
    public String toString() {
        return "GoogleMapsPlaceResult[" +
               "status='" + status + '\'' +
               ", results=" + results +
               ", htmlAttributions=" + htmlAttributions +
               ']';
    }
}

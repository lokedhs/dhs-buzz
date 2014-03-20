package org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class GoogleMapsDetailsContent implements Serializable
{
    @Key("name")
    public String name;

    @Key("vicinity")
    public String vicinity;

    @Key("formatted_phone_number")
    public String formattedPhoneNumber;

    @Key("formatted_address")
    public String formattedAddress;

    @Key("geometry")
    public GoogleMapsGeometry geometry;

    @Key("id")
    public String id;

    @Key("reference")
    public String reference;

    @Key("types")
    public List<String> types;

    @Key("html_attributions")
    public List<String> htmlAttributions;

    public boolean isOfType( String type ) {
        return types.contains( type );
    }

    @Override
    public String toString() {
        return "GoogleMapsDetailsContent[" +
               "formattedAddress='" + formattedAddress + '\'' +
               ", name='" + name + '\'' +
               ", vicinity='" + vicinity + '\'' +
               ", formattedPhoneNumber='" + formattedPhoneNumber + '\'' +
               ", geometry=" + geometry +
               ", id='" + id + '\'' +
               ", reference='" + reference + '\'' +
               ", types=" + types +
               ", htmlAttributions=" + htmlAttributions +
               ']';
    }
}

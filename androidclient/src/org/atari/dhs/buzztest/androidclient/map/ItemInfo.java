package org.atari.dhs.buzztest.androidclient.map;

import com.google.android.maps.GeoPoint;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;

class ItemInfo
{
    private GeoPoint geoPoint;
    private BuzzActivity buzzActivity;

    ItemInfo( GeoPoint geoPoint, BuzzActivity buzzActivity ) {
        this.geoPoint = geoPoint;
        this.buzzActivity = buzzActivity;
    }

    static ItemInfo parseActivity( BuzzActivity buzz ) {
        String geocode = buzz.geocode;
        if( geocode == null ) {
            return null;
        }

        int i = geocode.indexOf( " " );
        if( i == -1 ) {
            return null;
        }

        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble( geocode.substring( 0, i ) );
            longitude = Double.parseDouble( geocode.substring( i + 1 ) );
        }
        catch( NumberFormatException e ) {
            Log.e( "can't parse geocode: '" + geocode + "'", e );
            return null;
        }

        return new ItemInfo( new GeoPoint( (int)(latitude * 1000000), (int)(longitude * 1000000) ), buzz );
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public BuzzActivity getBuzzActivity() {
        return buzzActivity;
    }
}

package org.atari.dhs.buzztest.androidclient.post.selectlocation;

import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.GoogleMapsDetailsContent;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.MapsLocation;

public class MapLocationOverlay extends ItemizedOverlay
{
    private GoogleMapsDetailsContent location;
    private SelectLocationActivity selectLocationActivity;

    public MapLocationOverlay( Drawable drawable, SelectLocationActivity selectLocationActivity ) {
        super( boundCenterBottom( drawable ) );
        this.selectLocationActivity = selectLocationActivity;
    }

    @Override
    protected OverlayItem createItem( int i ) {
        if( i != 0 ) {
            throw new IllegalArgumentException( "argument must be 0" );
        }

        return new LocationOverlayItem( createGeoPoint(), location.name );
    }

    private GeoPoint createGeoPoint() {
        MapsLocation pos = location.geometry.location;
        return new GeoPoint( (int)(pos.lat * 1000000),
                             (int)(pos.lng * 1000000) );
    }

    @Override
    public int size() {
        int result = location == null ? 0 : 1;
        return result;
    }

    public void setResult( GoogleMapsDetailsContent location ) {
        this.location = location;
        populate();
    }

    @Override
    protected boolean onTap( int i ) {
        if( i == 0 ) {
            selectLocationActivity.selectLocationClicked();
            return true;
        }
        else {
            return false;
        }
    }
}

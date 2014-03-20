package org.atari.dhs.buzztest.androidclient.post.selectlocation;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class LocationOverlayItem extends OverlayItem
{
    public LocationOverlayItem( GeoPoint geoPoint, String title ) {
        super( geoPoint, title, "" );
    }
}

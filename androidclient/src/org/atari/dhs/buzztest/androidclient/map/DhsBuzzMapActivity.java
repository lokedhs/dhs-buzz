package org.atari.dhs.buzztest.androidclient.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivityFeed;

public class DhsBuzzMapActivity extends MapActivity
{
    private TestOverlay overlay;

    private Object lock = new Object();

    private GeoPoint location;
    private int zoomLevel;

    private GeoPoint locationToCheck;
    private int zoomLevelToCheck;

    private Timer timer;
    private Thread loadBuzzThread;
    private MapView mapView;

    private int mapViewWidth;
    private int mapViewHeight;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.buzz_map );

        mapView = (MapView)findViewById( R.id.buzz_map_view );
        mapView.setBuiltInZoomControls( true );

        Drawable icon = getResources().getDrawable( R.drawable.ic_stat_notify_loading );
        overlay = new TestOverlay( this );
        List<Overlay> overlays = mapView.getOverlays();

        overlays.add( overlay );
    }

    @Override
    protected void onDestroy() {
        overlay.close();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPeriodicCheck();
    }

    @Override
    protected void onPause() {
        stopPeriodicCheck();
        super.onPause();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    void startPeriodicCheck() {
        timer = new Timer( "LocationCheckTimer", false );
        TimerTask task = new TimerTask()
        {
            @Override
            public void run() {
                checkForUpdatedLocationInMainThread();
            }
        };
        timer.schedule( task, 1000, 1000 );
    }

    void stopPeriodicCheck() {
        timer.cancel();
        synchronized( lock ) {
            if( loadBuzzThread != null ) {
                locationToCheck = null;
                loadBuzzThread.interrupt();
                loadBuzzThread = null;
            }
        }
    }

    private void checkForUpdatedLocationInMainThread() {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run() {
                checkForUpdatedLocation();
            }
        } );
    }

    private void checkForUpdatedLocation() {
        synchronized( lock ) {
            GeoPoint newCentre = mapView.getMapCenter();
            int newZoomLevel = mapView.getZoomLevel();
            mapViewWidth = mapView.getWidth();
            mapViewHeight = mapView.getHeight();
            if( newCentre != null ) {
                if( location == null || (location.getLatitudeE6() != newCentre.getLatitudeE6()
                                         || location.getLongitudeE6() != newCentre.getLongitudeE6()
                                         || zoomLevel != newZoomLevel) ) {
                    location = newCentre;
                    zoomLevel = newZoomLevel;
                    requestNewMessages( location, zoomLevel );
                }
            }
        }
    }

    private void requestNewMessages( GeoPoint location, int zoomLevel ) {
        synchronized( lock ) {
            locationToCheck = location;
            zoomLevelToCheck = zoomLevel;
            lock.notify();
            if( loadBuzzThread == null ) {
                loadBuzzThread = new Thread( new LoadMessagesTask(), "LoadNearbyBuzzThread" );
                loadBuzzThread.start();
            }
        }
    }

    private class LoadMessagesTask implements Runnable
    {
        @Override
        public void run() {
            BuzzManager buzzManager = BuzzManager.createFromContext( DhsBuzzMapActivity.this );

            //noinspection UnusedCatchParameter
            try {
                while( !Thread.interrupted() ) {
                    GeoPoint locationCopy;
                    int zoomLevelCopy;
                    int mapViewWidthCopy;
                    int mapViewHeightCopy;
                    synchronized( lock ) {
                        while( locationToCheck == null ) {
                            lock.wait();
                        }

                        locationCopy = locationToCheck;
                        zoomLevelCopy = zoomLevelToCheck;
                        mapViewWidthCopy = mapViewWidth;
                        mapViewHeightCopy = mapViewHeight;
                        locationToCheck = null;
                    }

                    double latitude = (double)locationCopy.getLatitudeE6() / 1000000;
                    double longitude = (double)locationCopy.getLongitudeE6() / 1000000;

                    long lengthOfEquator = 40008 * 1000;
                    // zoomLevelCopy should be subtracted by 2 below in order to get the correct result
                    // the fact that this is not done, halves the radius of the search which actually
                    // results in better results
                    double metresPerPixel = lengthOfEquator / (256 * Math.pow( 2, zoomLevelCopy ));
                    double radius = metresPerPixel * Math.max( mapViewWidthCopy, mapViewHeightCopy ) / 2;

                    try {
                        Log.i( "checking activities at: lat=" + latitude + ", lon=" + longitude + ", radius=" + radius );
                        BuzzActivityFeed activities = buzzManager.loadActivities( null, BuzzManager.makeLocationSearchUrl( latitude, longitude, (int)radius, 0 ) ).feed;
                        Log.i( "got " + (activities.activities == null ? "null" : String.valueOf( activities.activities.size() )) + " activities" );

                        final List<ItemInfo> items = new ArrayList<ItemInfo>();
                        if( activities.activities != null ) {
                            for( BuzzActivity buzzActivity : activities.activities ) {
                                ItemInfo item = ItemInfo.parseActivity( buzzActivity );
                                if( item != null ) {
                                    items.add( item );
                                }
                            }
                        }

                        DhsBuzzMapActivity.this.runOnUiThread( new Runnable()
                        {
                            @Override
                            public void run() {
                                overlay.updateItems( items );
                                mapView.invalidate();
                            }
                        } );
                    }
                    catch( IOException e ) {
                        Log.e( "exception when loading", e );
                    }
                }
            }
            catch( InterruptedException e ) {
                Log.i( "location load thread was interrupted" );
            }
            Log.i( "shutting down location load thread" );
        }
    }
}

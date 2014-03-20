package org.atari.dhs.buzztest.androidclient.post.selectlocation;

import java.text.MessageFormat;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.*;
import android.widget.*;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.*;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.MapsAsyncSupportActivity;

public class SelectLocationActivity extends MapsAsyncSupportActivity
{
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_SELECTED_PLACE_ID = "placeId";
    public static final String EXTRA_SELECTED_PLACE_NAME = "name";
    public static final String EXTRA_SELECTED_LOCATION_GEOCODE = "position";

    private SelectAreaListAdapter selectAreaListAdapter;
    private boolean isInDetailView = false;

    private View selectAreaPanel;
    private View selectLocationPanel;
    private ViewGroup animationPanel;
    private ViewGroup areaNameHolder;
    private TextView attributionsTextView;
    private TextView mapsDetailAtributionsTextView;

    //    private TextView vicinityTextView;
    private TextView addressTextView;
    private MapView mapView;
    private MapLocationOverlay mapLocationOverlay;
    private View selectedItemPanel;
    private GoogleMapsDetailsContent locationsResult;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        setContentView( R.layout.location_select );

        selectAreaPanel = findViewById( R.id.select_area_panel );
        selectLocationPanel = findViewById( R.id.select_location_panel );
        animationPanel = (ViewGroup)findViewById( R.id.animation_panel );
        areaNameHolder = (ViewGroup)findViewById( R.id.area_name_holder );
//        vicinityTextView = (TextView)findViewById( R.id.vicinity_text );
        addressTextView = (TextView)findViewById( R.id.address_text );

        attributionsTextView = (TextView)findViewById( R.id.attributions_text_view );
        attributionsTextView.setMovementMethod( LinkMovementMethod.getInstance() );

        mapsDetailAtributionsTextView = (TextView)findViewById( R.id.map_detail_attributions_text_view );
        mapsDetailAtributionsTextView.setMovementMethod( LinkMovementMethod.getInstance() );

        ImageCache imageCache = new ImageCache( this );

        initMapView();

        Intent intent = getIntent();
        Location location = intent.getParcelableExtra( EXTRA_LOCATION );

        ListView selectAreaListView = (ListView)findViewById( R.id.area_select_list_view );

        selectAreaListAdapter = new SelectAreaListAdapter( this, imageCache );
        selectAreaListView.setAdapter( selectAreaListAdapter );

        selectAreaListView.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> adapterView, View view, int position, long id ) {
                handleAreaItemClick( view, position );
            }
        } );

        areaNameHolder.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view ) {
                selectLocationClicked();
            }
        } );

        loadAreas( location );
    }

    private void initMapView() {
        mapView = (MapView)findViewById( R.id.position_map_view );
        mapView.setBuiltInZoomControls( true );
        List<Overlay> overlays = mapView.getOverlays();

        Drawable marker = getResources().getDrawable( R.drawable.ic_stat_notify_new_message );
        mapLocationOverlay = new MapLocationOverlay( marker, this );
        overlays.add( mapLocationOverlay );
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private void handleAreaItemClick( View view, int position ) {
        moveToDetailSelector( view, position );
    }

    @Override
    public void onBackPressed() {
        if( isInDetailView ) {
            moveToAreaSelectView();
        }
        else {
            super.onBackPressed();
        }
    }

    private void moveToAreaSelectView() {
        if( !isInDetailView ) {
            throw new IllegalStateException( "must be in detail view in order to be able to go to area select mode" );
        }
        isInDetailView = false;

        locationsResult = null;

        selectLocationPanel.setVisibility( View.INVISIBLE );
        selectAreaPanel.setVisibility( View.VISIBLE );
        areaNameHolder.removeAllViews();
        selectedItemPanel = null;

//        vicinityTextView.setVisibility( View.GONE );
        addressTextView.setVisibility( View.GONE );

        mapView.setVisibility( View.INVISIBLE );
    }

    private void moveToDetailSelector( View clickedView, int position ) {
        if( isInDetailView ) {
            throw new IllegalStateException( "attempt to go to detail from the wrong mode" );
        }
        isInDetailView = true;

        int[] positionArray = new int[2];
        clickedView.getLocationInWindow( positionArray );

        selectAreaPanel.setVisibility( View.INVISIBLE );
        selectLocationPanel.setVisibility( View.VISIBLE );
        animationPanel.setVisibility( View.VISIBLE );
        areaNameHolder.removeAllViews();

        final GoogleMapsPlace item = selectAreaListAdapter.getAddressItemByPosition( position );
        selectedItemPanel = selectAreaListAdapter.getView( position, null, animationPanel );
        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams( FrameLayout.LayoutParams.WRAP_CONTENT,
                                                                              FrameLayout.LayoutParams.WRAP_CONTENT );

        int[] animationPanelPosition = new int[2];
        animationPanel.getLocationInWindow( animationPanelPosition );

        layout.leftMargin = 0;
        layout.topMargin = 0;
        animationPanel.addView( selectedItemPanel, layout );

        Animation anim = new TranslateAnimation( positionArray[0] - animationPanelPosition[0],
                                                 0,
                                                 positionArray[1] - animationPanelPosition[1],
                                                 0 );

        Interpolator interpolator = new AccelerateDecelerateInterpolator();
        anim.setInterpolator( interpolator );
        anim.setDuration( DateHelper.SECOND_MILLIS / 2 );
        anim.setAnimationListener( new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart( Animation animation ) {
            }

            @Override
            public void onAnimationEnd( Animation animation ) {
                animationPanel.removeView( selectedItemPanel );
                areaNameHolder.addView( selectedItemPanel, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT,
                                                                                          LinearLayout.LayoutParams.WRAP_CONTENT ) );
                animationPanel.setVisibility( View.INVISIBLE );
                loadLocations( item );
            }

            @Override
            public void onAnimationRepeat( Animation animation ) {
            }
        } );
        selectedItemPanel.startAnimation( anim );
    }

    private void returnSelectedLocation( GoogleMapsDetailsContent mapsLocation ) {
        Intent intent = new Intent();
        if( locationsResult.isOfType( "establishment" ) ) {
            intent.putExtra( EXTRA_SELECTED_PLACE_ID, mapsLocation.reference );
        }
        intent.putExtra( EXTRA_SELECTED_PLACE_NAME, mapsLocation.name );

        MapsLocation location = mapsLocation.geometry.location;
        String geocode = location.lat + " " + location.lng;
        intent.putExtra( EXTRA_SELECTED_LOCATION_GEOCODE, geocode );

        setResult( Activity.RESULT_OK, intent );
        finish();
    }

    private void loadAreas( Location location ) {
        setProgressBarIndeterminateVisibility( true );
        startAsyncTask( new LoadAreasTask(), new LoadAreasTask.Args( this, location ) );
    }

    void processLoadAreasResult( LoadAreasTask.Result result ) {
        setProgressBarIndeterminateVisibility( false );
        GoogleMapsPlaceResult locations;
        if( result.errorMessage != null ) {
            String fmt = getResources().getString( R.string.select_location_loading_failed );
            Toast.makeText( this, MessageFormat.format( fmt, result.errorMessage ), Toast.LENGTH_LONG ).show();
            locations = null;
            attributionsTextView.setText( "" );
            attributionsTextView.setVisibility( View.GONE );
        }
        else {
            locations = result.areasResult;
            formatAttributions( result.areasResult.htmlAttributions, attributionsTextView );
        }
        selectAreaListAdapter.setLocations( locations );
    }

    private void loadLocations( GoogleMapsPlace location ) {
        setProgressBarIndeterminateVisibility( true );
        startAsyncTask( new LoadLocationsTask(), new LoadLocationsTask.Args( this, location ) );
    }

    public void processLoadLocationsResult( LoadLocationsTask.Result result ) {
        setProgressBarIndeterminateVisibility( false );
        if( result.errorMessage != null ) {
            Toast.makeText( this, "Failed to load locations: " + result.errorMessage, Toast.LENGTH_LONG ).show();
        }
        else {
            GoogleMapsDetailsResult locations = result.locations;
            locationsResult = locations.result;

            if( locationsResult != null ) {
//                updateOrHideTextView( vicinityTextView, locationsResult.vicinity );
                updateOrHideTextView( addressTextView, locationsResult.formattedAddress );
                formatAttributions( locationsResult.htmlAttributions, mapsDetailAtributionsTextView );

                if( locationsResult.geometry != null && locationsResult.geometry.location != null ) {
                    MapsLocation pos = locationsResult.geometry.location;
                    MapController controller = mapView.getController();
                    controller.setCenter( new GeoPoint( (int)(pos.lat * 1000000),
                                                        (int)(pos.lng * 1000000) ) );
                    controller.setZoom( 17 );

                    Animation anim = new AlphaAnimation( 0, 1 );
                    Interpolator interpolator = new LinearInterpolator();
                    anim.setInterpolator( interpolator );
                    anim.setDuration( DateHelper.SECOND_MILLIS / 2 );
                    mapView.setVisibility( View.VISIBLE );
                    mapLocationOverlay.setResult( locationsResult );
                    mapView.startAnimation( anim );

//                    selectedItemPanel.setFocusable( true );
//                    selectedItemPanel.setOnClickListener( new View.OnClickListener()
//                    {
//                        @Override
//                        public void onClick( View view ) {
//                            returnSelectedLocation( locationsResult );
//                        }
//                    } );
                }
            }
        }
    }

    private void updateOrHideTextView( TextView textView, String text ) {
        if( text != null && !text.equals( "" ) ) {
            textView.setText( text );
            textView.setVisibility( View.VISIBLE );
        }
        else {
            textView.setVisibility( View.GONE );
        }
    }

    void selectLocationClicked() {
        if( locationsResult != null ) {
            returnSelectedLocation( locationsResult );
        }
    }

    private void formatAttributions( List<String> htmlAttributions, TextView textView ) {
        if( htmlAttributions == null || htmlAttributions.isEmpty() ) {
            textView.setText( "" );
            textView.setVisibility( View.GONE );
        }
        else {
            StringBuilder buf = new StringBuilder();
            boolean first = true;
            for( String s : htmlAttributions ) {
                if( first ) {
                    first = false;
                }
                else {
                    buf.append( ", " );
                }

                buf.append( s );
            }

            textView.setText( Html.fromHtml( buf.toString() ) );
            textView.setVisibility( View.VISIBLE );
        }
    }
}

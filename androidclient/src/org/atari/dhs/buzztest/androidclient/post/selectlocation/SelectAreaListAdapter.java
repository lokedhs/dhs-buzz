package org.atari.dhs.buzztest.androidclient.post.selectlocation;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.GoogleMapsPlace;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.GoogleMapsPlaceResult;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.imagecache.LoadImageCallback;
import org.atari.dhs.buzztest.androidclient.imagecache.StorageType;
import org.atari.dhs.buzztest.androidclient.tools.ReassignableImageView;

class SelectAreaListAdapter extends BaseAdapter
{
    private ArrayList<GoogleMapsPlace> locationList;
    private SelectLocationActivity parentActivity;
    private ImageCache imageCache;
    private LayoutInflater inflater;
    private float density;

    public SelectAreaListAdapter( SelectLocationActivity parentActivity, ImageCache imageCache ) {
        this.parentActivity = parentActivity;
        this.imageCache = imageCache;
        this.inflater = (LayoutInflater)parentActivity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        DisplayMetrics metrics = new DisplayMetrics();
        parentActivity.getWindowManager().getDefaultDisplay().getMetrics( metrics );
        density = metrics.density;
    }

    @Override
    public int getCount() {
        return locationList == null ? 0 : locationList.size();
    }

    @Override
    public Object getItem( int position ) {
        return locationList.get( position );
    }

    @Override
    public long getItemId( int position ) {
        return position;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        return getContentView( position, convertView, parent );
    }

    private View getContentView( int position, View convertView, ViewGroup parent ) {
        View v;
        if( convertView != null && convertView.getId() == R.id.location_select_row_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.location_select_row, parent, false );
        }

        GoogleMapsPlace address = locationList.get( position );

        TextView textView = (TextView)v.findViewById( R.id.content );
        textView.setText( address.name );

        TextView detailTextView = (TextView)v.findViewById( R.id.detail );
        if( address.vicinity == null || address.vicinity.equals( "" ) ) {
//            detailTextView.setVisibility( View.GONE );
            detailTextView.setText( "" );
            detailTextView.setVisibility( View.VISIBLE );
        }
        else {
            detailTextView.setText( address.vicinity );
            detailTextView.setVisibility( View.VISIBLE );
        }

        final ReassignableImageView imageView = (ReassignableImageView)v.findViewById( R.id.image );
        if( address.icon == null ) {
            imageView.setVisibility( View.GONE );
        }
        else {
            int imageSize = (int)(24 * density);
            boolean isDelayed = imageCache.loadImage( address.icon, imageSize, imageSize, StorageType.LONG, new MyLoadImageCallback( imageView ) );
            if( isDelayed ) {
                imageView.setImageResource( R.drawable.empty_image_24 );
            }
        }

        return v;
    }

    private String makeAddressString( Address address ) {
        StringBuilder buf = new StringBuilder();
        buf.append( address.getAddressLine( 0 ) );
        appendIfNotNull( buf, address.getAddressLine( 1 ) );
        appendIfNotNull( buf, address.getAddressLine( 2 ) );
        appendIfNotNull( buf, address.getAdminArea() );
        appendIfNotNull( buf, address.getCountryName() );
        return buf.toString();
    }

    private void appendIfNotNull( StringBuilder buf, String s ) {
        if( s != null ) {
            buf.append( ", " );
            buf.append( s );
        }
    }

    public void setLocations( GoogleMapsPlaceResult locations ) {
        locationList = new ArrayList<GoogleMapsPlace>();
        if( locations != null && locations.results != null ) {
            for( GoogleMapsPlace a : locations.results ) {
                locationList.add( a );
            }
        }
        notifyDataSetChanged();
    }

    public GoogleMapsPlace getAddressItemByPosition( int position ) {
        if( locationList == null ) {
            return null;
        }
        else {
            return locationList.get( position );
        }
    }

    private static class MyLoadImageCallback implements LoadImageCallback
    {
        private ReassignableImageView image;
        private boolean wasDetached;

        public MyLoadImageCallback( ReassignableImageView image ) {
            this.image = image;

            image.setDetachListener( new ReassignableImageView.OnDetachListener()
            {
                @Override
                public void detached() {
                    wasDetached = true;
                }
            } );
        }

        @Override
        public void bitmapLoaded( Bitmap bitmap ) {
            if( !wasDetached ) {
                image.setImageBitmap( bitmap );
            }
        }
    }
}

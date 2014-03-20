package org.atari.dhs.buzztest.androidclient.userlist;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.User;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.imagecache.LoadImageCallback;
import org.atari.dhs.buzztest.androidclient.imagecache.StorageType;
import org.atari.dhs.buzztest.androidclient.tools.ReassignableImageView;

public class SearchResultListAdapter extends BaseAdapter
{
    private List<User> users;
    private boolean lastPage;

    private LayoutInflater inflater;
    private ImageCache imageCache;
    private int contactPhotoWidth;
    private int contactPhotoHeight;

    public SearchResultListAdapter( LayoutInflater inflater, ImageCache imageCache, int contactPhotoWidth, int contactPhotoHeight ) {
        this.inflater = inflater;
        this.imageCache = imageCache;
        this.contactPhotoWidth = contactPhotoWidth;
        this.contactPhotoHeight = contactPhotoHeight;
    }

    public void setUsers( List<User> users, boolean lastPage ) {
        this.users = new ArrayList<User>( users );
        this.lastPage = lastPage;
        notifyDataSetChanged();
    }

    public void addUsers( List<User> users, boolean lastPage ) {
        this.users.addAll( users );
        this.lastPage = lastPage;
        notifyDataSetChanged();
    }

    public void markLastPage() {
        lastPage = true;
    }

    @Override
    public int getCount() {
        if( users == null ) {
            return 0;
        }
        else {
            return lastPage ? users.size() : users.size() + 1;
        }
    }

    @Override
    public Object getItem( int i ) {
        return users.get( i );
    }

    @Override
    public long getItemId( int i ) {
        return i;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        if( !lastPage && position == users.size() ) {
            return loadMoreView( convertView, parent );
        }
        else {
            return getUserRowView( position, convertView, parent );
        }
    }

    private View getUserRowView( int position, View convertView, ViewGroup parent ) {
        View v;
        if( convertView != null && convertView.getId() == R.id.search_user_row_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.search_user_row, parent, false );
        }

        TextView nameTextView = (TextView)v.findViewById( R.id.user_name_text );

        User user = users.get( position );
        nameTextView.setText( user.displayName );

        ReassignableImageView imageView = (ReassignableImageView)v.findViewById( R.id.image );
        updateImage( imageView, user );

        return v;
    }

    private View loadMoreView( View convertView, ViewGroup parent ) {
        View v;
        if( convertView != null && convertView.getId() == R.id.search_user_load_more_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.search_user_load_more_row, parent, false );
        }
        return v;
    }

    private void updateImage( final ReassignableImageView image, User user ) {
        String thumbnailUrl = user.thumbnailUrl;
        if( thumbnailUrl == null || thumbnailUrl.equals( "" ) ) {
            image.setImageResource( R.drawable.photo_default );
        }
        else {
            boolean isDelayed = imageCache.loadImage( user.thumbnailUrl, contactPhotoWidth, contactPhotoHeight, StorageType.DONT_STORE, new MyLoadImageCallback( image ) );
            if( isDelayed ) {
                image.setImageResource( R.drawable.image_loading );
            }
        }
    }

    public User getRowForPosition( int position ) {
        return users.get( position );
    }

    public int getUserCount() {
        if( users == null ) {
            throw new IllegalStateException( "trying to get user count before the users have been loaded" );
        }
        return users.size();
    }

    public StoredConfig getConfig() {
        imageCache.stopAll();
        return new StoredConfig( users, lastPage );
    }

    public void setConfig( StoredConfig config ) {
        users = config.users;
        lastPage = config.lastPage;
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

    static class StoredConfig
    {
        private StoredConfig( List<User> users, boolean lastPage ) {
            this.users = users;
            this.lastPage = lastPage;
        }

        List<User> users;
        boolean lastPage;
    }
}

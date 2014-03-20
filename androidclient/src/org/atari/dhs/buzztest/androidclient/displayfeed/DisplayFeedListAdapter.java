package org.atari.dhs.buzztest.androidclient.displayfeed;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.CachedFeed;
import org.atari.dhs.buzztest.androidclient.buzz.CachedFeedActivity;
import org.atari.dhs.buzztest.androidclient.buzz.TitleParser;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivityFeed;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.imagecache.LoadImageCallback;
import org.atari.dhs.buzztest.androidclient.imagecache.StorageType;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.androidclient.tools.ReassignableImageView;

public class DisplayFeedListAdapter extends BaseAdapter
{
    private Context context;
    private LayoutInflater inflater;
    private ImageCache imageCache;
    private DateHelper dateHelper = new DateHelper();

    private List<FeedEntry> entries;
    private String nextLink;

    private int contactPhotoWidth;
    private int contactPhotoHeight;

    public DisplayFeedListAdapter( Context context, LayoutInflater inflater, ImageCache imageCache, int contactPhotoWidth, int contactPhotoHeight ) {
        this.context = context;
        this.inflater = inflater;
        this.imageCache = imageCache;
        this.contactPhotoWidth = contactPhotoWidth;
        this.contactPhotoHeight = contactPhotoHeight;
    }

    @Override
    public int getCount() {
        if( entries == null ) {
            return 0;
        }
        else {
            return entries.size() + (nextLink == null ? 0 : 1);
        }
    }

    @Override
    public Object getItem( int position ) {
        return hasNextLinkAndPositionIsNext( position ) ? null : entries.get( position );
    }

    @Override
    public long getItemId( int position ) {
        return position;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        if( hasNextLinkAndPositionIsNext( position ) ) {
            return getMoreView( convertView, parent );
        }
        else {
            return getRowView( position, convertView, parent );
        }
    }

    private View getMoreView( View convertView, ViewGroup parent ) {
        View view;
        if( convertView != null && convertView.getId() == R.id.message_list_more_row_view ) {
            view = convertView;
        }
        else {
            view = inflater.inflate( R.layout.message_list_more_row, parent, false );
        }
        return view;
    }

    private View getRowView( int position, View convertView, ViewGroup parent ) {
        FeedEntry feedEntry = entries.get( position );
        CachedFeedActivity activity = feedEntry.getActivity();
        View v;
        if( convertView != null && convertView.getId() == R.id.message_list_row_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.message_list_row, parent, false );
        }

        TextView senderTextView = (TextView)v.findViewById( R.id.sender );
        senderTextView.setText( activity.getActor() );

        TitleParser titleParser = new TitleParser();
        TextView titleTextView = (TextView)v.findViewById( R.id.message_text );
        titleTextView.setText( titleParser.parseTitle( activity.getTitle() ) );

        TextView sentDateView = (TextView)v.findViewById( R.id.date );
        sentDateView.setText( feedEntry.getFormattedDate() );

        TextView numberOfRepliesView = (TextView)v.findViewById( R.id.number_of_replies );
        int replies = activity.getNumReplies();
        Resources resources = context.getResources();
        String fmt = resources.getString( R.string.display_feed_heading_number_of_replies );
        numberOfRepliesView.setText( MessageFormat.format( fmt, replies ) );

        ReassignableImageView image = (ReassignableImageView)v.findViewById( R.id.image );
        updateImage( image, activity );

        return v;
    }

    private void updateImage( final ReassignableImageView image, CachedFeedActivity activity ) {
        String thumbnailUrl = activity.getThumbnailUrl();
        if( thumbnailUrl == null || thumbnailUrl.equals( "" ) ) {
            image.setImageResource( R.drawable.photo_default );
        }
        else {
            boolean isDelayed = imageCache.loadImage( thumbnailUrl, contactPhotoWidth, contactPhotoHeight, StorageType.LONG, new MyLoadImageCallback( image ) );
            if( isDelayed ) {
                image.setImageResource( R.drawable.image_loading );
            }
        }
    }

    public void setActivityFeed( CachedFeed feed ) {
        if( entries == null ) {
            entries = new ArrayList<FeedEntry>();
        }
        else {
            entries.clear();
        }

        addToEntries( feed.getMessages(), feed.getLoadNextLink() );
    }

    public void setStaticActivityList( CachedFeedActivity[] activities ) {
        if( entries == null ) {
            entries = new ArrayList<FeedEntry>();
        }
        else {
            entries.clear();
        }
        addToEntries( activities, null );
    }

    public void addToEntries( CachedFeedActivity[] activities, String nextLink ) {
        if( activities != null ) {
            for( CachedFeedActivity activity : activities ) {
                entries.add( new FeedEntry( activity, dateHelper ) );
            }
        }

        this.nextLink = nextLink;

        notifyDataSetChanged();
    }

    public boolean hasNextLinkAndPositionIsNext( int position ) {
        return nextLink != null && position == entries.size();
    }

    private String findNextLink( BuzzActivityFeed feed ) {
        String result;
        if( feed.links == null || feed.links.next == null || feed.links.next.isEmpty() ) {
            result = null;
        }
        else {
            if( feed.links.next.size() > 1 ) {
                Log.w( "more than one next link, returning the first one" );
            }
            result = feed.links.next.get( 0 ).href;
        }
        return result;
    }

    public FeedEntry getActivityByPosition( int position ) {
        if( nextLink != null && position == entries.size() ) {
            return null;
        }
        else if( position >= 0 && position < entries.size() ) {
            return entries.get( position );
        }
        else {
            throw new IllegalStateException( "invalid activity position: " + position + " (total activities=" + entries.size() + ")" );
        }
    }

    public String getNextLink() {
        return nextLink;
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

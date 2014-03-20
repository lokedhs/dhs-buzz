package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Attachment;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply.BuzzReply;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply.BuzzReplyFeed;
import org.atari.dhs.buzztest.androidclient.displayfeed.DisplayFeed;
import org.atari.dhs.buzztest.androidclient.displayprofile.DisplayProfileTabActivity;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.imagecache.LoadImageCallback;
import org.atari.dhs.buzztest.androidclient.imagecache.StorageType;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.androidclient.tools.HashTagSpan;
import org.atari.dhs.buzztest.androidclient.tools.HashtagParser;
import org.atari.dhs.buzztest.androidclient.translation.TranslateActivity;

/**
 * List adapter that implements the data provider for the list
 * in the "display buzz" activity.
 * <p/>
 * The list can be in one of two states:
 * <ul>
 * <li>Before replies has been loaded. In this state, the list contains two elements:
 * The first element being the buzz itself, and the second one containing a button
 * that specifies the number of replies to this buzz.
 * <li>Once the button has been pressed and the replies has been loaded, the list will
 * now contain the buzz itself in cell 0 followed by all the replies.
 * </ul>
 * <p/>
 * If the member {@link #replies} is null, the list is in the first state,
 * otherwise it is in the second state.
 */
class DisplayBuzzListAdapter extends BaseAdapter
{
    /**
     * The number of read messages that are shown when the reply list is collapsed.
     */
    private static final int COLLAPSED_READ_REPLY_COUNT = 0;

    private DisplayBuzz displayBuzzActivity;
    private BuzzActivity buzz;
    private List<ReplyEntry> replies;

    private LayoutInflater inflater;
    private ImageCache imageCache;

    private int contactPhotoWidth;
    private int contactPhotoHeight;
    private int firstUnreadPos;
    private long lastReadTime;

    private static Map<String, LinkViewFactory> LINK_VIEW_FACTORIES;
    private float linkViewPadding;

    private View contentView;
    private boolean readRepliesCollapsed = true;
    private ImageView realtimeIcon;
    private String collapsedText;
    private List<LinkDescriptor> contentViewUrls;

    private String latString;
    private String lonString;

    static {
        Map<String, LinkViewFactory> v = new HashMap<String, LinkViewFactory>();
        v.put( "article", new ArticleLinkViewFactory() );
        v.put( "note", new NoteLinkViewFactory() );
        v.put( "video", new VideoLinkViewFactory() );
        v.put( "audio", new AudioLinkViewFactory() );
        LINK_VIEW_FACTORIES = v;
    }

    public DisplayBuzzListAdapter( DisplayBuzz displayBuzzActivity, ImageCache imageCache, int contactPhotoWidth, int contactPhotoHeight ) {
        this.displayBuzzActivity = displayBuzzActivity;
        this.imageCache = imageCache;
        this.contactPhotoWidth = contactPhotoWidth;
        this.contactPhotoHeight = contactPhotoHeight;

        Resources resources = displayBuzzActivity.getResources();
        linkViewPadding = resources.getDimension( R.dimen.display_buzz_link_padding );

        this.inflater = (LayoutInflater)displayBuzzActivity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
    }

    public void setBuzz( BuzzActivity buzz, long lastReadTime ) {
        this.buzz = buzz;
        this.lastReadTime = lastReadTime;

        boolean geoCodeWasSet = false;
        if( buzz.geocode != null ) {
            String[] p = buzz.geocode.split( " " );
            if( p.length == 2 ) {
                latString = p[0];
                lonString = p[1];
                geoCodeWasSet = true;
            }
        }
        if( !geoCodeWasSet ) {
            latString = null;
            lonString = null;
        }

        contentView = null;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if( buzz == null ) {
            return 0;
        }
        else if( replies == null ) {
            return 2;
        }
        else if( readRepliesCollapsed ) {
            //noinspection PointlessArithmeticExpression
            return replies.size() - firstUnreadPos + COLLAPSED_READ_REPLY_COUNT + 2;
        }
        else {
            return replies.size() + 1;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
//        return false;
        return true;
    }
//
//    @Override
//    public boolean isEnabled( int position ) {
//        return position == 1 && (readRepliesCollapsed || replies == null);
//    }

    @Override
    public Object getItem( int row ) {
        return null;
    }

    @Override
    public long getItemId( int row ) {
        return row;
    }

    @Override
    public View getView( int row, View convertView, ViewGroup parent ) {
        if( row == 0 ) {
            return getContentView( convertView, parent );
        }
        else if( replies == null ) {
            if( row != 1 ) {
                throw new IllegalStateException( "attempt to retrieve reply view before replies has been loaded" );
            }
            return getLoadOrLoadingRepliesView( convertView, parent );
        }
        else {
            return getReplyOrCollapsedView( row, convertView, parent );
        }
    }

    private View getContentView( View convertView, ViewGroup parent ) {
        if( contentView == null ) {
            View v = inflater.inflate( R.layout.display_buzz_content, parent, false );

            TextView nameTextView = (TextView)v.findViewById( R.id.name );
            nameTextView.setMovementMethod( LinkMovementMethod.getInstance() );

            TextView textContent = (TextView)v.findViewById( R.id.content );
            textContent.setMovementMethod( LinkMovementMethod.getInstance() );

            nameTextView.setText( Html.fromHtml( makeLink( buzz.actor.profileUrl, buzz.actor.name ) ) );

            DateHelper dateHelper = new DateHelper();
            String formattedUpdatedDate = dateHelper.formatDateTimeOutputFormat( dateHelper.parseDate( buzz.updated ) );
            setTextContentById( v, R.id.date, formattedUpdatedDate );

            if( buzz.isReshare() ) {
                updateResharePanel( v );
            }

            CharSequence fromContent;
            Resources resources = displayBuzzActivity.getResources();
            String fmt = resources.getString( R.string.display_buzz_posted_from );
            fromContent = MessageFormat.format( fmt, buzz.findFrom() );
            setTextContentById( v, R.id.from, fromContent );

            ImageView imageView = (ImageView)v.findViewById( R.id.image );
            updateImage( imageView );

            String contentString = buzz.object.content;
            Spanned contentSpanned = HashtagParser.parseHashtaggedHtml( contentString );
            addUrlsToContentViewUrlList( extractLinksFromSpan( contentSpanned ) );
            textContent.setText( contentSpanned );

            updateLinksView( (LinearLayout)v.findViewById( R.id.links ) );
            updateImagesView( (LinearLayout)v.findViewById( R.id.images_view ) );

            realtimeIcon = (ImageView)v.findViewById( R.id.realtime_icon_image_view );
            updateRealtimeIcon( displayBuzzActivity.checkIfRealtime() );

            View locationView = v.findViewById( R.id.location_view );
            TextView locationText = (TextView)v.findViewById( R.id.location_text );

            if( latString != null && lonString != null ) {
                String text = buzz.placeName;
                if( text == null ) {
                    text = buzz.address;
                }
                if( text == null ) {
                    text = resources.getString( R.string.display_buzz_missing_location );
                }
                locationText.setText( Html.fromHtml( text ) );
                locationView.setVisibility( View.VISIBLE );
            }
            else {
                locationText.setText( "" );
                locationView.setVisibility( View.GONE );
            }

            contentView = v;
        }

        return contentView;
    }

    private void updateResharePanel( View v ) {
        View reshareInfoPanel = v.findViewById( R.id.reshare_info );
        TextView reshareText = (TextView)v.findViewById( R.id.shared_by_text );
        TextView reshareFrom = (TextView)v.findViewById( R.id.shared_by_user );

        reshareInfoPanel.setVisibility( View.VISIBLE );

        String comment = buzz.annotation;
        if( comment != null ) {
            Spanned formattedText = Html.fromHtml( comment );
            reshareText.setText( formattedText );
        }

        Resources resources = displayBuzzActivity.getResources();
        String fmt = resources.getString( R.string.display_buzz_reshared_post_by );
        reshareFrom.setText( MessageFormat.format( fmt, buzz.object.findActorName() ) );
    }

    private List<LinkDescriptor> extractLinksFromSpan( Spanned contentSpanned ) {
        List<LinkDescriptor> links = null;
        URLSpan[] spans = contentSpanned.getSpans( 0, contentSpanned.length(), URLSpan.class );
        for( URLSpan urlSpan : spans ) {
            int start = contentSpanned.getSpanStart( urlSpan );
            int end = contentSpanned.getSpanEnd( urlSpan );
            if( start == -1 || end == -1 ) {
                throw new IllegalStateException( "urlSpan not found in string, weird" );
            }
            String description = contentSpanned.subSequence( start, end ).toString();
            if( links == null ) {
                links = new ArrayList<LinkDescriptor>();
            }
            links.add( new LinkDescriptor( urlSpan.getURL(), description, LinkDescriptor.LinkType.URL ) );
        }

        HashTagSpan[] hashtagSpans = contentSpanned.getSpans( 0, contentSpanned.length(), HashTagSpan.class );
        for( HashTagSpan hashtagSpan : hashtagSpans ) {
            String description = hashtagSpan.getHashtag();
            if( links == null ) {
                links = new ArrayList<LinkDescriptor>();
            }
            links.add( new LinkDescriptor( description, description, LinkDescriptor.LinkType.HASHTAG ) );
        }

        return links;
    }

    private void addUrlToContentViewUrlList( String url, String title ) {
        if( contentViewUrls == null ) {
            contentViewUrls = new ArrayList<LinkDescriptor>();
        }
        contentViewUrls.add( new LinkDescriptor( url, title, LinkDescriptor.LinkType.URL ) );
    }

    private void addUrlsToContentViewUrlList( List<LinkDescriptor> links ) {
        if( links != null ) {
            if( contentViewUrls == null ) {
                contentViewUrls = new ArrayList<LinkDescriptor>();
            }
            contentViewUrls.addAll( links );
        }
    }

    void updateRealtimeIcon( boolean realtime ) {
        if( realtimeIcon != null ) {
            realtimeIcon.setVisibility( realtime ? View.VISIBLE : View.GONE );
        }
    }

    private void updateLinksView( LinearLayout linksView ) {
        linksView.removeAllViews();

        List<Attachment> attachments = buzz.object.attachments;

        boolean wasSet = false;

        if( attachments != null ) {
            LinkViewFactoryContext linkViewFactoryContext = new LinkViewFactoryContext()
            {
                @Override
                public void processSpannableForLinks( Spanned spanned ) {
                    List<LinkDescriptor> links = extractLinksFromSpan( spanned );
                    addUrlsToContentViewUrlList( links );
                }

                @Override
                public void addLink( String url, String title ) {
                    addUrlToContentViewUrlList( url, title );
                }
            };

            for( Attachment a : attachments ) {
                LinkViewFactory factory = LINK_VIEW_FACTORIES.get( a.type );
                if( factory != null ) {
                    View v = factory.makeView( displayBuzzActivity, inflater, linksView, a, linkViewFactoryContext );
                    if( v != null ) {
                        addViewToLinksView( linksView, v );
                        wasSet = true;
                    }
                }
            }
        }

        linksView.setVisibility( wasSet ? View.VISIBLE : View.GONE );
    }

    private void updateImagesView( LinearLayout imagesView ) {
        List<Attachment> attachments = buzz.object.attachments;

        if( attachments != null ) {
            View imagePanel = getImageAttachmentPanel( imagesView, attachments );
            if( imagePanel != null ) {
                imagesView.addView( imagePanel );
                imagesView.setVisibility( View.VISIBLE );
            }
        }
    }

    private View getImageAttachmentPanel( LinearLayout linksView, List<Attachment> attachments ) {
        View imageListView = null;
        LinearLayout images = null;

        for( Attachment a : attachments ) {
            ImageAttachmentInfo info = ImageAttachmentInfo.parse( a );
            if( info != null ) {
                if( imageListView == null ) {
                    imageListView = inflater.inflate( R.layout.image_list_view, linksView, false );
                    images = (LinearLayout)imageListView.findViewById( R.id.images );
                }
                info.attachTo( displayBuzzActivity, imageCache, images );
            }
        }

        return imageListView;
    }

    private void addViewToLinksView( LinearLayout linksView, View v ) {
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT,
                                                                       LinearLayout.LayoutParams.WRAP_CONTENT );
        v.setLayoutParams( params );
        v.setPadding( (int)linkViewPadding,
                      (int)linkViewPadding,
                      (int)linkViewPadding,
                      (int)linkViewPadding );

        linksView.addView( v );
    }

    private void updateImage( final ImageView image ) {
        String thumbnailUrl = buzz.actor.thumbnailUrl;
        if( buzz.actor.thumbnailUrl == null || thumbnailUrl.equals( "" ) ) {
            image.setImageResource( R.drawable.photo_default );
        }
        else {
            boolean isDelayed = imageCache.loadImage( buzz.actor.thumbnailUrl, contactPhotoWidth, contactPhotoHeight, StorageType.LONG, new LoadImageCallback()
            {
                @Override
                public void bitmapLoaded( Bitmap bitmap ) {
                    image.setImageBitmap( bitmap );
                }
            } );
            if( isDelayed ) {
                image.setImageResource( R.drawable.image_loading );
            }
        }
    }

    private void setTextContentById( View view, int id, CharSequence text ) {
        TextView textView = (TextView)view.findViewById( id );
        textView.setText( text );
    }

    private View getLoadOrLoadingRepliesView( View convertView, ViewGroup parent ) {
        if( displayBuzzActivity.isLoadingInProgress() ) {
            return getLoadingRepliesView( convertView, parent );
        }
        else {
            return getLoadRepliesView( convertView, parent );
        }
    }

    private View getLoadRepliesView( View convertView, ViewGroup parent ) {
        View v;
        if( convertView != null && convertView.getId() == R.id.display_buzz_load_replies_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.display_buzz_load_replies, parent, false );
        }

        TextView textView = (TextView)v.findViewById( R.id.load_content );

        String s = displayBuzzActivity.getResources().getString( R.string.display_buzz_load_replies );
        textView.setText( MessageFormat.format( s, buzz.findReplyCount() ) );

        return v;
    }

    private View getLoadingRepliesView( View convertView, ViewGroup parent ) {
        View v;
        if( convertView != null && convertView.getId() == R.id.display_buzz_loading_replies_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.display_buzz_loading_replies, parent, false );
        }
        return v;
    }

    private View getReplyOrCollapsedView( int row, View convertView, ViewGroup parent ) {
        if( readRepliesCollapsed && row == 1 ) {
            return getCollapsedView( convertView, parent );
        }
        else {
            return getReplyView( replies.get( adjustPositionForCollapsed( row ) ), convertView, parent );
        }
    }

    private View getCollapsedView( View convertView, ViewGroup parent ) {
        View v;
        if( convertView != null && convertView.getId() == R.id.display_buzz_collapsed_message_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.display_buzz_collapsed_message, parent, false );
        }

        TextView textView = (TextView)v.findViewById( R.id.load_content );
        @SuppressWarnings( { "PointlessArithmeticExpression" })
        int numCollapsed = firstUnreadPos - COLLAPSED_READ_REPLY_COUNT;
        if( collapsedText == null ) {
            Resources resources = displayBuzzActivity.getResources();
            String s = resources.getString( R.string.display_buzz_show_hidden );
            collapsedText = MessageFormat.format( s, numCollapsed );
        }
        textView.setText( collapsedText );

        return v;
    }

    private View getReplyView( ReplyEntry reply, View convertView, ViewGroup parent ) {
        View v;
        TextView from;
        TextView content;
        if( convertView != null && convertView.getId() == R.id.display_buzz_reply_view ) {
            v = convertView;
            content = (TextView)v.findViewById( R.id.content );
            from = (TextView)v.findViewById( R.id.from );
        }
        else {
            v = inflater.inflate( R.layout.display_buzz_reply, parent, false );
            content = (TextView)v.findViewById( R.id.content );
            content.setMovementMethod( LinkMovementMethod.getInstance() );
            from = (TextView)v.findViewById( R.id.from );
            from.setMovementMethod( LinkMovementMethod.getInstance() );
        }

        TextView dateTextView = (TextView)v.findViewById( R.id.date );

        from.setText( reply.getFormattedFrom() );
        content.setText( reply.getFormattedText() );
        dateTextView.setText( reply.getFormattedDate() );

        View unreadMarker = v.findViewById( R.id.unread_marker );
        if( lastReadTime < reply.getDate().getTime() ) {
            unreadMarker.setBackgroundResource( R.color.background_unread );
        }
        else {
            unreadMarker.setBackgroundResource( R.color.background_read );
        }

        return v;
    }

    private int findPositionOfLastUnread() {
        if( replies == null ) {
            return -1;
        }

        int position = -1;
        int n = replies.size();
        for( int i = 0 ; i < n ; i++ ) {
            ReplyEntry reply = replies.get( i );
            if( lastReadTime < reply.getDate().getTime() ) {
                position = i;
                break;
            }
        }

        // If no unread messages were found, return the index of the last element
        if( position == -1 ) {
            return n - 1;
        }
        else {
            return position;
        }
    }

    void handleItemClick( int position ) {
        if( replies == null && position == 1 ) {
            displayBuzzActivity.loadRepliesUsingCacheStrategy( RepliesLoadCacheStrategy.FORCE_NETWORK );
        }
        else if( replies != null && readRepliesCollapsed && position == 1 ) {
            readRepliesCollapsed = false;
            notifyDataSetChanged();
        }
    }

    void setReplyFeed( BuzzReplyFeed feed ) {
        replies = new ArrayList<ReplyEntry>();
        List<BuzzReply> items = feed.items;
        if( items != null ) {
            for( BuzzReply reply : feed.items ) {
                replies.add( new ReplyEntry( reply ) );
            }
        }
        firstUnreadPos = findPositionOfLastUnread();
        readRepliesCollapsed = (firstUnreadPos > COLLAPSED_READ_REPLY_COUNT) && readRepliesCollapsed;
        collapsedText = null;
        notifyDataSetChanged();
    }

    void resetLoadingMessage() {
        // The loading message is actually controlled by the loadingInProgress property of
        // the DisplayBuzz class. At this point, this property has already been changed
        // so simply triggering the ListView to update its model is enough to get the
        // message to be restored.
        notifyDataSetChanged();
    }

    AdapterConf getAdapterConf() {
        return new AdapterConf( replies, firstUnreadPos, readRepliesCollapsed );
    }

    void setAdapterConf( AdapterConf config ) {
        this.replies = config.replies;
        this.firstUnreadPos = config.lastUnreadPos;
        this.readRepliesCollapsed = config.readRepliesCollapsed;
    }

    void notifyReplyLoadStart() {
        notifyDataSetChanged();
    }

    void handleContextMenu( ContextMenu menu, int position ) {
        if( position == 0 ) {
            updateContextMenuForMainContent( menu );
        }
        else {
            ReplyEntry reply = findReplyForPosition( position );
            if( reply != null ) {
                updateContextMenuForReply( menu, reply );
            }
        }
    }

    private ReplyEntry findReplyForPosition( int position ) {
        if( position == 0 ) {
            return null;
        }

        if( readRepliesCollapsed && position == 1 ) {
            return null;
        }

        return replies.get( adjustPositionForCollapsed( position ) );
    }

    private int adjustPositionForCollapsed( int position ) {
        if( readRepliesCollapsed ) {
            //noinspection PointlessArithmeticExpression
            return (position - 2) + (firstUnreadPos - COLLAPSED_READ_REPLY_COUNT);
        }
        else {
            return position - 1;
        }
    }

    private void updateContextMenuForMainContent( ContextMenu menu ) {
        String actorName = buzz.actor.name;
        menu.setHeaderTitle( formatMessage( actorName, R.string.display_buzz_menu_title_post ) );

        addDisplayProfileEntryToMenu( menu, buzz.actor.id );

        final String alternateLink = buzz.findAlternateLinkOfType( "text/html" );
        if( alternateLink != null ) {
            menu.add( R.string.display_buzz_view_on_web ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick( MenuItem menuItem ) {
                    Intent intent = new Intent( Intent.ACTION_VIEW );
                    intent.setData( Uri.parse( alternateLink ) );
                    displayBuzzActivity.startActivity( intent );
                    return true;
                }
            } );
        }

        menu.add( formatMessage( actorName, R.string.display_buzz_menu_reply_to ) ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem menuItem ) {
                displayBuzzActivity.addAtReply( buzz.actor );
                return true;
            }
        } );

        if( latString != null && lonString != null ) {
            menu.add( R.string.display_buzz_view_location_in_map ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick( MenuItem menuItem ) {
                    Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( "geo:" + latString + "," + lonString + "?z=16" ) );
                    displayBuzzActivity.startActivity( intent );
                    return true;
                }
            } );
        }

//        if( buzz.actor.id.equals( displayBuzzActivity.getUserId() ) ) {
//            menu.add( R.string.display_buzz_menu_edit_post ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
//            {
//                @Override
//                public boolean onMenuItemClick( MenuItem menuItem ) {
//                    displayBuzzActivity.editPostSelected();
//                    return true;
//                }
//            } );
//        }

        addTranslateToContextMenu( menu, buzz.object.content );

        addLinksSubMenus( menu, contentViewUrls );
    }

    private void addTranslateToContextMenu( ContextMenu menu, final String text ) {
        menu.add( R.string.menu_text_translate ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem menuItem ) {
                Intent intent = new Intent( displayBuzzActivity, TranslateActivity.class );
                intent.putExtra( TranslateActivity.EXTRA_TEXT, Html.fromHtml( text ).toString() );
                displayBuzzActivity.startActivity( intent );
                return true;
            }
        } );
    }

    private void updateContextMenuForReply( ContextMenu menu, final ReplyEntry replyEntry ) {
        String actorName = replyEntry.getReply().actor.name;

        menu.setHeaderTitle( formatMessage( actorName, R.string.display_buzz_menu_title_reply ) );

        final BuzzReply reply = replyEntry.getReply();
        addDisplayProfileEntryToMenu( menu, reply.actor.id );

        menu.add( formatMessage( actorName, R.string.display_buzz_menu_reply_to ) ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem menuItem ) {
                displayBuzzActivity.addAtReply( reply.actor );
                return true;
            }
        } );

        addTranslateToContextMenu( menu, reply.content );

        if( replyEntry.getReply().actor.id.equals( displayBuzzActivity.getUserId() ) ) {
            menu.add( R.string.display_buzz_menu_edit_reply ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick( MenuItem menuItem ) {
                    displayBuzzActivity.editReplySelected( replyEntry );
                    return true;
                }
            } );

            menu.add( R.string.display_buzz_menu_delete_reply ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick( MenuItem menuItem ) {
                    displayBuzzActivity.deleteReplySelected( replyEntry );
                    return true;
                }
            } );
        }

        List<LinkDescriptor> links;
        if( replyEntry.linksParsed() ) {
            links = replyEntry.getLinks();
        }
        else {
            links = extractLinksFromSpan( replyEntry.getFormattedText() );
            replyEntry.setLinks( links );
        }

        addLinksSubMenus( menu, links );
    }

    private void addLinksSubMenus( ContextMenu menu, List<LinkDescriptor> links ) {
        List<LinkDescriptor> urls = new ArrayList<LinkDescriptor>();
        List<LinkDescriptor> hashtags = new ArrayList<LinkDescriptor>();
        if( links != null ) {
            for( LinkDescriptor desc : links ) {
                switch( desc.getType() ) {
                    case URL:
                        urls.add( desc );
                        break;

                    case HASHTAG:
                        hashtags.add( desc );
                        break;
                }
            }
        }

        if( !urls.isEmpty() ) {
            SubMenu subMenu = menu.addSubMenu( R.string.display_buzz_menu_links_header );
            for( LinkDescriptor desc : urls ) {
                final LinkDescriptor desc2 = desc;
                subMenu.add( desc.getDescription() ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick( MenuItem menuItem ) {
                        Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( desc2.getUrl() ) );
                        displayBuzzActivity.startActivity( intent );
                        return true;
                    }
                } );
            }
        }

        if( !hashtags.isEmpty() ) {
            SubMenu subMenu = menu.addSubMenu( R.string.display_buzz_menu_hashtags_header );
            for( LinkDescriptor desc : hashtags ) {
                final LinkDescriptor desc2 = desc;
                subMenu.add( desc.getDescription() ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick( MenuItem menuItem ) {
                        Intent intent = new Intent( displayBuzzActivity, DisplayFeed.class );
                        intent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_SEARCH );
                        intent.putExtra( DisplayFeed.EXTRA_SEARCH_WORDS, new String[] { desc2.getUrl() } );
                        displayBuzzActivity.startActivity( intent );
                        return true;
                    }
                } );
            }
        }
    }

    private void addDisplayProfileEntryToMenu( ContextMenu menu, final String userId ) {
        menu.add( R.string.display_buzz_menu_start_profile ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem menuItem ) {
                Intent intent = new Intent( displayBuzzActivity, DisplayProfileTabActivity.class );
                intent.putExtra( DisplayProfileTabActivity.EXTRA_USER_ID, userId );
                displayBuzzActivity.startActivity( intent );
                return true;
            }
        } );
    }

    private String formatMessage( String name, int stringId ) {
        Resources resources = displayBuzzActivity.getResources();
        String format = resources.getString( stringId );
        return MessageFormat.format( format, name );
    }

    static String makeLink( String url, String content ) {
        StringBuilder buf = new StringBuilder();
        buf.append( "<a href='" );
        formatHtml( buf, url );
        buf.append( "'>" );
        formatHtml( buf, content );
        buf.append( "</a>" );
        return buf.toString();
    }

    @SuppressWarnings( { "ConstantConditions" })
    private static void formatHtml( StringBuilder buf, String text ) {
        int n = text.length();
        for( int i = 0 ; i < n ; i++ ) {
            char ch = text.charAt( i );
            switch( ch ) {
                case '\'':
                    buf.append( "&#39;" );
                    break;
                case '\"':
                    buf.append( "&quot;" );
                    break;
                case '&':
                    buf.append( "&amp;" );
                    break;
                case '<':
                    buf.append( "&lt;" );
                    break;
                case '>':
                    buf.append( "&gt;" );
                    break;
                default:
                    buf.append( ch );
            }
        }
    }
}

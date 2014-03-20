package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Attachment;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.AttachmentLinkImageElement;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.imagecache.LoadImageCallback;
import org.atari.dhs.buzztest.androidclient.imagecache.StorageType;

public class ImageAttachmentInfo
{
    private String previewHref;
    private String contentHref;

    public ImageAttachmentInfo( String previewHref, String contentHref ) {
        this.previewHref = previewHref;
        this.contentHref = contentHref;
    }

    public static ImageAttachmentInfo parse( Attachment a ) {
        if( a == null || a.type == null || !a.type.equals( Attachment.TYPE_PHOTO ) ) {
            return null;
        }

        if( a.links == null ) {
            return null;
        }

        String previewHref = findLinkElement( a.links.preview );
        if( previewHref == null ) {
            return null;
        }

        String contentHref = findLinkElement( a.links.enclosure );
        if( contentHref == null ) {
            return null;
        }

        return new ImageAttachmentInfo( previewHref, contentHref );
    }

    private static String findLinkElement( List<AttachmentLinkImageElement> element ) {
        if( element == null || element.isEmpty() ) {
            return null;
        }

        if( element.size() > 1 ) {
            Log.w( "more than one element in attachment link list, returning the first instance" );
        }

        return element.get( 0 ).href;
    }

    public void attachTo( final Context context, final ImageCache imageCache, final LinearLayout images ) {
        final ImageView imageView = new ImageView( context );

//        imageView.setClickable( true );
//        imageView.setOnClickListener( new View.OnClickListener()
//        {
//            @Override
//            public void onClick( View view ) {
//                Intent intent = new Intent( Intent.ACTION_VIEW );
//                intent.setData( Uri.parse( contentHref ) );
//                context.startActivity( intent );
//            }
//        } );

        imageView.setImageResource( R.drawable.attached_image_loading );

        Resources resources = context.getResources();
        int width = resources.getDimensionPixelSize( R.dimen.image_preview_width );
        int height = resources.getDimensionPixelSize( R.dimen.image_preview_height );


        boolean isDelayed = imageCache.loadImage( previewHref, width, height, StorageType.SHORT, new LoadImageCallback()
        {
            @Override
            public void bitmapLoaded( Bitmap bitmap ) {
                imageView.setImageBitmap( bitmap );
            }
        } );
        if( isDelayed ) {
            imageView.setImageResource( R.drawable.attached_image_loading );
        }

        float linkViewPadding = context.getResources().getDimension( R.dimen.image_preview_padding );

        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                       LinearLayout.LayoutParams.WRAP_CONTENT );
        imageView.setLayoutParams( params );
        imageView.setPadding( (int)linkViewPadding,
                              (int)linkViewPadding,
                              (int)linkViewPadding,
                              (int)linkViewPadding );


        images.addView( imageView );
    }
}

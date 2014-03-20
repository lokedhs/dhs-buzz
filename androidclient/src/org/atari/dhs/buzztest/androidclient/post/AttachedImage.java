package org.atari.dhs.buzztest.androidclient.post;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.tools.ImageHelpers;

public class AttachedImage
{
    private String file;
    private String remoteUrl;
    private ImageView imageView;

    public AttachedImage( Context context, String file, String remoteUrl ) {
        this.file = file;
        this.remoteUrl = remoteUrl;

        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT,
                                                                       LinearLayout.LayoutParams.WRAP_CONTENT );
        imageView = new ImageView( context );
        imageView.setLayoutParams( params );

        Resources resources = context.getResources();
        float padding = resources.getDimension( R.dimen.image_preview_padding );
        int width = resources.getDimensionPixelSize( R.dimen.image_preview_width );
        int height = resources.getDimensionPixelSize( R.dimen.image_preview_height );

        int p = (int)padding;
        imageView.setPadding( p, p, p, p );

//        Bitmap tmpBitmap = BitmapFactory.decodeFile( file );
//        int w = tmpBitmap.getWidth();
//        int h = tmpBitmap.getHeight();
//
//        Matrix matrix = new Matrix();
//        RectF srcRect = new RectF( 0, 0, w, h );
//        RectF destRect = new RectF( 0, 0, width, height );
//        matrix.setRectToRect( srcRect, destRect, Matrix.ScaleToFit.CENTER );
//        Bitmap bitmap = Bitmap.createBitmap( tmpBitmap, 0, 0, w, h, matrix, true );
//        tmpBitmap.recycle();

        Bitmap bitmap = ImageHelpers.loadAndScaleBitmap( file, width, height );

        imageView.setImageBitmap( bitmap );
    }

    public ImageView getView() {
        return imageView;
    }

    public String getFile() {
        return file;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }
}

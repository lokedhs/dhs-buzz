package org.atari.dhs.buzztest.androidclient.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;

import org.atari.dhs.buzztest.androidclient.Log;

public class ImageHelpers
{
    private static final double LOG_2 = Math.log( 2 );

    private ImageHelpers() {
        // prevent instantiation
    }

    public static Bitmap loadAndScaleBitmap( String file, int width, int height ) {
        return loadAndScaleBitmapWithMinimumSize( file, width, height, -1, -1 );
    }

    public static Bitmap loadAndScaleBitmapWithMinimumSize( String file, int width, int height, int minimumWidth, int minimumHeight ) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile( file, options );

        if( minimumWidth != -1 && minimumHeight != -1 && (options.outWidth < minimumWidth || options.outHeight < minimumHeight) ) {
            Log.d( "skipping: w=" + options.outWidth + ", h=" + options.outHeight );
            return null;
        }

        double ratio = Math.max( options.outWidth / (double)width, options.outHeight / (double)height );
        int exp = Math.max( (int)Math.floor( Math.log( ratio ) / LOG_2 ), 0 );
        int scale = 1 << exp;
        Log.d( "orig:(" + options.outWidth + "," + options.outHeight + "), req:(" + width + "," + height + "), scale=" + scale );

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap tmpBitmap = BitmapFactory.decodeFile( file, o2 );
        if( tmpBitmap == null ) {
            return null;
        }

        int w = tmpBitmap.getWidth();
        int h = tmpBitmap.getHeight();

        Matrix matrix = new Matrix();
        RectF srcRect = new RectF( 0, 0, w, h );
        RectF destRect = new RectF( 0, 0, width, height );
        matrix.setRectToRect( srcRect, destRect, Matrix.ScaleToFit.CENTER );
        Bitmap bitmap = Bitmap.createBitmap( tmpBitmap, 0, 0, w, h, matrix, true );
        tmpBitmap.recycle();

        return bitmap;
    }
}

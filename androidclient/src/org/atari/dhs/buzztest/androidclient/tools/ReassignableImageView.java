package org.atari.dhs.buzztest.androidclient.tools;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ReassignableImageView extends ImageView
{
    private OnDetachListener detachListener;

    public ReassignableImageView( Context context ) {
        super( context );
    }

    public ReassignableImageView( Context context, AttributeSet attrs ) {
        super( context, attrs );
    }

    public ReassignableImageView( Context context, AttributeSet attrs, int defStyle ) {
        super( context, attrs, defStyle );
    }

    @Override
    public void onStartTemporaryDetach() {
        super.onStartTemporaryDetach();

        if( detachListener != null ) {
            detachListener.detached();
        }
    }

    public void setDetachListener( OnDetachListener detachListener ) {
        this.detachListener = detachListener;
    }

    public interface OnDetachListener
    {
        void detached();
    }
}

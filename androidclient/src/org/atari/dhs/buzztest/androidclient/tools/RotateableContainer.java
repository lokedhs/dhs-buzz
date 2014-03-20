package org.atari.dhs.buzztest.androidclient.tools;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;

import org.atari.dhs.buzztest.androidclient.Log;

public class RotateableContainer extends LinearLayout
{
    private boolean rotated;
    private boolean animationInProgress = false;
    private RotationCompleteListener listener;

    public RotateableContainer( Context context ) {
        super( context );
        setWillNotDraw( false );
        setStaticTransformationsEnabled( true );
    }

    public RotateableContainer( Context context, AttributeSet attrs ) {
        super( context, attrs );
        setWillNotDraw( false );
        setStaticTransformationsEnabled( true );
    }

    public void setOnRotationCompleteListener( RotationCompleteListener listener ) {
        this.listener = listener;
    }

    public void setRotation( boolean rotated ) {
        this.rotated = rotated;

        verifyChildState();
        View child = getChildAt( 0 );
        float pivot = child.getHeight() / 2f;
        animationInProgress = true;

        float start;
        float stop;
        if( rotated ) {
            start = 0;
            stop = 90;
        }
        else {
            start = 90;
            stop = 0;
        }

        Animation animation = new RotateAnimation( start, stop, pivot, pivot );
        animation.setInterpolator( new AccelerateDecelerateInterpolator() );
        animation.setDuration( DateHelper.SECOND_MILLIS / 2 );
        animation.setAnimationListener( new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart( Animation animation ) {
            }

            @Override
            public void onAnimationEnd( Animation animation ) {
                animationInProgress = false;
                requestLayout();
                if( listener != null ) {
                    listener.onRotationComplete( RotateableContainer.this.rotated );
                }
            }

            @Override
            public void onAnimationRepeat( Animation animation ) {
            }
        } );
        child.setAnimation( animation );

        invalidate();
        requestLayout();
    }

    @Override
    protected boolean getChildStaticTransformation( View child, Transformation t ) {
        verifyChildState();
        float pivot = child.getHeight() / 2f;

        t.setTransformationType( Transformation.TYPE_MATRIX );
        Matrix matrix = t.getMatrix();
        matrix.setRotate( rotated ? 90 : 0, pivot, pivot );
        return true;
    }

    @Override
    protected void onLayout( boolean changed, int left, int right, int top, int bottom ) {
        super.onLayout( changed, left, right, top, bottom );
    }

    @Override
    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
        super.onMeasure( widthMeasureSpec, heightMeasureSpec );

        verifyChildState();

        View child = getChildAt( 0 );
        Log.i( "measuring:" + getMeasuredWidth() + "," + getMeasuredHeight() );
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        if( animationInProgress ) {
            setMeasuredDimension( measuredWidth, measuredWidth );
        }
        else if( rotated ) {
            setMeasuredDimension( measuredHeight, measuredWidth );
        }
    }

//    @Override
//    protected void onDraw( Canvas canvas ) {
//        Log.i( "onDraw called" );
//
//        Matrix matrix = canvas.getMatrix();
////        matrix.postRotate( degrees, pivotX, pivotY );
//        canvas.rotate( degrees, pivotX, pivotY );
//        super.onDraw( canvas );
////        canvas.setMatrix( matrix );
//
////        Paint paint = new Paint();
////        paint.setARGB( 255, 255, 0, 0 );
////        canvas.drawLine( 0, 0, 10, 10, paint );
//    }

    private void verifyChildState() {
        if( getChildCount() != 1 ) {
            throw new IllegalStateException( "only one child is allowed" );
        }
    }

    public interface RotationCompleteListener
    {
        void onRotationComplete( boolean rotated );
    }
}

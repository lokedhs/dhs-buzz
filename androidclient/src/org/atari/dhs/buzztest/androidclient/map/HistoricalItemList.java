package org.atari.dhs.buzztest.androidclient.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

import com.google.android.maps.Projection;

class HistoricalItemList
{
    private static final long FADEOUT_TIME = 1000;

    private List<ItemInfo> items;
    private float density;

    private boolean toRemove = false;
    private long startRemoveTimestamp = -1;

    HistoricalItemList( List<ItemInfo> items, float density ) {
        this.items = new ArrayList<ItemInfo>( items );
        this.density = density;
    }

    public void cleanEntries( HistoricalItemList l ) {
        for( Iterator<ItemInfo> i = items.iterator() ; i.hasNext() ; ) {
            ItemInfo item = i.next();
            String id = item.getBuzzActivity().id;
            for( ItemInfo n : l.items ) {
                if( id.equals( n.getBuzzActivity().id ) ) {
                    i.remove();
                }
            }
        }
    }

    public void markForRemove() {
        toRemove = true;
    }

    public HistoricalItemDrawStatus drawItems( Canvas canvas, Projection projection, Point resultPoint, long when ) {
        Paint paint = new Paint();
        paint.setARGB( 255, 0, 255, 0 );

        boolean needRedraw = false;
        if( toRemove ) {
            if( startRemoveTimestamp == -1 ) {
                startRemoveTimestamp = when;
            }
            long timeDelta = when - startRemoveTimestamp;
            if( timeDelta > FADEOUT_TIME ) {
                return HistoricalItemDrawStatus.EXPIRED;
            }
            else {
                needRedraw = true;
                int alpha = (int)(255 - ((255 * timeDelta) / FADEOUT_TIME));
                paint.setAlpha( alpha );
            }
        }

        Paint textPant = new Paint( paint );
        int alpha = textPant.getAlpha();

        float textHeight = 10 * density;
        textPant.setARGB( alpha, 0, 0, 0 );
        textPant.setTextAlign( Paint.Align.LEFT );
        textPant.setTextSize( textHeight );

        Paint backgroundPaint = new Paint( textPant );
        backgroundPaint.setARGB( alpha, 255, 255, 255 );
        backgroundPaint.setStyle( Paint.Style.STROKE );
        backgroundPaint.setStrokeWidth( 2 );

        RectF backgroundRect = new RectF();

        for( ItemInfo info : items ) {
            projection.toPixels( info.getGeoPoint(), resultPoint );

            String name = info.getBuzzActivity().actor.name;
            float width = textPant.measureText( name );

            float radius = 5 * density;
            backgroundRect.set( resultPoint.x, resultPoint.y - textHeight, resultPoint.x + width, resultPoint.y );
//            canvas.drawRect( backgroundRect, backgroundPaint );
            canvas.drawText( name, resultPoint.x, resultPoint.y - radius, backgroundPaint );
            canvas.drawText( name, resultPoint.x, resultPoint.y - radius, textPant );
            canvas.drawCircle( resultPoint.x, resultPoint.y, radius, paint );
        }

        if( needRedraw ) {
            return HistoricalItemDrawStatus.NEEDS_REDRAW;
        }
        else {
            return HistoricalItemDrawStatus.ONCE;
        }
    }

    public ItemDistanceInfo getClosestItem( Point tapLocation, Point itemLocation, Projection projection ) {
        ItemInfo closestItem = null;
        double closestDistance = Float.MAX_VALUE;
        for( ItemInfo item : items ) {
            projection.toPixels( item.getGeoPoint(), itemLocation );
            double distance = Math.hypot( tapLocation.x - itemLocation.x, tapLocation.y - itemLocation.y );
            if( distance < closestDistance ) {
                closestItem = item;
                closestDistance = distance;
            }
        }
        return new ItemDistanceInfo( closestItem, closestDistance );
    }

    static class ItemDistanceInfo
    {
        ItemInfo item;
        double distance;

        ItemDistanceInfo( ItemInfo item, double distance ) {
            this.item = item;
            this.distance = distance;
        }
    }
}

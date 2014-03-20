package org.atari.dhs.buzztest.androidclient.map;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.DisplayMetrics;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.SQLiteDatabaseWrapper;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.displaybuzz.DisplayBuzz;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

class TestOverlay extends Overlay
{
    private List<HistoricalItemList> itemLists = new LinkedList<HistoricalItemList>();

    private Context context;

    private DateHelper dateHelper;
    private BuzzManager buzzManager;
    private SQLiteDatabaseWrapper dbWrapper;
    private float density;

    TestOverlay( DhsBuzzMapActivity dhsBuzzMapActivity ) {
        this.context = dhsBuzzMapActivity;

        dateHelper = new DateHelper();
        buzzManager = BuzzManager.createFromContext( dhsBuzzMapActivity );
        dbWrapper = StorageHelper.getDatabase( dhsBuzzMapActivity );

        DisplayMetrics metrics = new DisplayMetrics();
        dhsBuzzMapActivity.getWindowManager().getDefaultDisplay().getMetrics( metrics );
        density = metrics.density;
    }

    @Override
    public void draw( Canvas canvas, MapView mapView, boolean shadow ) {
    }

    @Override
    public boolean draw( Canvas canvas, MapView mapView, boolean shadow, long when ) {
        if( !shadow ) {
            Projection projection = mapView.getProjection();
            Point resultPoint = new Point();

            boolean needRedraw = false;
            for( Iterator<HistoricalItemList> i = itemLists.iterator() ; i.hasNext() ; ) {
                HistoricalItemList l = i.next();
                HistoricalItemDrawStatus status = l.drawItems( canvas, projection, resultPoint, when );
                if( status == HistoricalItemDrawStatus.EXPIRED ) {
                    i.remove();
                }
                else if( status == HistoricalItemDrawStatus.NEEDS_REDRAW ) {
                    needRedraw = true;
                }
            }

            return needRedraw;
        }
        else {
            return false;
        }
    }

    public void updateItems( List<ItemInfo> itemInfoList ) {
        HistoricalItemList l = new HistoricalItemList( itemInfoList, density );
        if( !itemLists.isEmpty() ) {
            for( HistoricalItemList toClear : itemLists ) {
                toClear.cleanEntries( l );
            }
            itemLists.get( 0 ).markForRemove();
        }
        itemLists.add( 0, l );
    }

    public void close() {
        buzzManager.close();
        dbWrapper.close();
    }

    @Override
    public boolean onTap( GeoPoint geoPoint, MapView mapView ) {
        Projection projection = mapView.getProjection();
        Point tapPoint = new Point();
        projection.toPixels( geoPoint, tapPoint );

        HistoricalItemList.ItemDistanceInfo closestItem = null;
        Point itemLocation = new Point();
        for( HistoricalItemList l : itemLists ) {
            HistoricalItemList.ItemDistanceInfo info = l.getClosestItem( tapPoint, itemLocation, projection );
            if( closestItem == null || info.distance < closestItem.distance ) {
                closestItem = info;
            }
        }

        if( closestItem != null ) {
            if( closestItem.distance < 20 * density ) {
                saveAndOpenBuzzActivity( closestItem.item );
                return true;
            }
        }

        return false;
    }

    private void saveAndOpenBuzzActivity( ItemInfo item ) {
        BuzzActivity buzzActivity = item.getBuzzActivity();
        buzzManager.saveActivity( dbWrapper.getDatabase(), false, System.currentTimeMillis(), dateHelper, buzzActivity );

        Intent intent = new Intent( context, DisplayBuzz.class );
        intent.putExtra( IntentHelper.EXTRA_ACTIVITY_ID, buzzActivity.id );
        context.startActivity( intent );
    }
}

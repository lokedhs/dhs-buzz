package org.atari.dhs.buzztest.androidclient.post.selectimages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import org.atari.dhs.buzztest.androidclient.R;

public class SelectImagesListAdapter extends BaseAdapter
{
    private LayoutInflater inflater;
    private List<RowWrapper> rows = new ArrayList<RowWrapper>();
    private CompoundButton.OnCheckedChangeListener onChangeListener;

    public SelectImagesListAdapter( LayoutInflater inflater, Config oldConfig ) {
        this.inflater = inflater;

        onChangeListener = new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged( CompoundButton compoundButton, boolean b ) {
                RowWrapper wrapper = (RowWrapper)compoundButton.getTag();
                wrapper.selected = b;
            }
        };

        if( oldConfig != null ) {
            rows = oldConfig.rows;
            notifyDataSetChanged();
        }
    }

    public Config getConfig() {
        return new Config( rows );
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem( int i ) {
        return rows.get( i );
    }

    @Override
    public long getItemId( int i ) {
        return i;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        View v;
        CheckBox imageSelected;
        if( convertView != null && convertView.getId() == R.id.select_image_row_view ) {
            v = convertView;
            imageSelected = (CheckBox)v.findViewById( R.id.image_selected );
        }
        else {
            v = inflater.inflate( R.layout.post_select_image_row, parent, false );
            imageSelected = (CheckBox)v.findViewById( R.id.image_selected );
            imageSelected.setOnCheckedChangeListener( onChangeListener );
        }

        RowWrapper row = rows.get( position );

        ImageView imageView = (ImageView)v.findViewById( R.id.image );
        imageView.setImageBitmap( row.bitmap );

        imageSelected.setTag( row );
        imageSelected.setChecked( row.selected );

        return v;
    }

    public void addImageInfo( String url, File file, Bitmap bitmap ) {
        rows.add( new RowWrapper( url, file, bitmap ) );
        notifyDataSetChanged();
    }

    public SelectedImageResult[] getResultList() {
        List<SelectedImageResult> resultList = new ArrayList<SelectedImageResult>();
        for( RowWrapper wrapper : rows ) {
            if( wrapper.selected ) {
                resultList.add( new SelectedImageResult( wrapper.url, wrapper.file.getPath() ) );
            }
        }
        return resultList.toArray( new SelectedImageResult[resultList.size()] );
    }

    private static class RowWrapper
    {
        private String url;
        public File file;
        public Bitmap bitmap;
        boolean selected;

        private RowWrapper( String url, File file, Bitmap bitmap ) {
            this.url = url;
            this.file = file;
            this.bitmap = bitmap;
        }
    }

    static class Config
    {
        public List<RowWrapper> rows;

        Config( List<RowWrapper> rows ) {
            this.rows = rows;
        }
    }
}

package org.atari.dhs.buzztest.androidclient.post.selectimages;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncSupportActivity;

public class PostSelectImagesActivity extends AsyncSupportActivity
{
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_RESULT_LIST = "files";

    private SelectImagesListAdapter adapter;
    private ProgressBar progressBar;
    private ProgressBar spinningProgress;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.post_select_image );

        Intent intent = getIntent();
        String url = intent.getStringExtra( EXTRA_URL );
        if( url == null ) {
            throw new IllegalStateException( "missing url in intent extra" );
        }

        LayoutInflater inflater = (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        progressBar = (ProgressBar)findViewById( R.id.load_images_progress );
        spinningProgress = (ProgressBar)findViewById( R.id.spinning_progress );

        SavedConfig conf = (SavedConfig)getLastNonConfigurationInstance2();

        ListView listView = (ListView)findViewById( R.id.image_list_view );
        adapter = new SelectImagesListAdapter( inflater, conf == null ? null : conf.adapterConfig );
        listView.setAdapter( adapter );

        if( conf == null ) {
            File extDir = Environment.getExternalStorageDirectory();

            File dhsBuzzDir = new File( extDir, "DHS_Buzz" );
            createDirIfNeeded( dhsBuzzDir );

            File tmpDir = new File( dhsBuzzDir, "tmp" );
            createDirIfNeeded( tmpDir );

            Resources resources = getResources();
            int imageWidth = resources.getDimensionPixelSize( R.dimen.select_images_pic_width );
            int imageHeight = resources.getDimensionPixelSize( R.dimen.select_images_pic_height );

            ParseHtmlTask task = new ParseHtmlTask();
            startAsyncTask( task, new ParseHtmlTask.Args( url, tmpDir, imageWidth, imageHeight ) );
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance2() {
        return new SavedConfig( adapter.getConfig() );
    }

    private void createDirIfNeeded( File dir ) {
        if( !dir.exists() ) {
            if( !dir.mkdir() ) {
                throw new IllegalStateException( "can't create directory: " + dir );
            }
        }
    }

    public void imageDownloaded( ParseHtmlTask.DownloadedImageInfo imageInfo ) {
        if( imageInfo.totalFiles >= 0 ) {
            progressBar.setMax( imageInfo.totalFiles + 1 );
            progressBar.setProgress( 0 );
        }
        if( imageInfo.fileIndex >= 0 ) {
            progressBar.setProgress( imageInfo.fileIndex );
        }
        if( imageInfo.file != null ) {
            adapter.addImageInfo( imageInfo.url, imageInfo.file, imageInfo.bitmap );
        }
    }

    public void downloadFinished() {
        progressBar.setProgress( progressBar.getMax() );
        progressBar.setVisibility( View.INVISIBLE );
        spinningProgress.setVisibility( View.INVISIBLE );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void attachButtonClicked( View view ) {
        Intent data = new Intent();
        data.putExtra( EXTRA_RESULT_LIST, adapter.getResultList() );
        setResult( RESULT_OK, data );
        finish();
    }

    private static class SavedConfig
    {
        public SelectImagesListAdapter.Config adapterConfig;

        private SavedConfig( SelectImagesListAdapter.Config adapterConfig ) {
            this.adapterConfig = adapterConfig;
        }
    }
}

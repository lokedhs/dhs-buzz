package org.atari.dhs.buzztest.androidclient.post.selectimages;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import android.graphics.Bitmap;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.tools.ImageHelpers;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

class ParseHtmlTask extends AsyncTaskWrapper<ParseHtmlTask.Args, ParseHtmlTask.DownloadedImageInfo, Void>
{
    private static final String TMP_FILE_PREFIX = "dhsBuzzTmpImage_";

    @Override
    protected Void doInBackground( Args... argsList ) {
        Args args = argsList[0];

        purgePreviousFiles( args.storageDirectory );

        NodeList result;
        try {
            Log.i( "parsing url: " + args.url );
            URL url = new URL( args.url );
            URLConnection conn = url.openConnection();
            Parser parser = new Parser( conn );

            result = parser.parse( createImgFilter() );
        }
        catch( MalformedURLException e ) {
            Log.w( "exception when reading images", e );
            return null;
        }
        catch( IOException e ) {
            Log.w( "exception when reading images", e );
            return null;
        }
        catch( ParserException e ) {
            Log.w( "exception when reading images", e );
            return null;
        }
        Log.i( "parsing succeeded" );

        int maxNumImages = 10;
//        int totalImages = Math.min( result.size(), maxNumImages );
        int totalImages = result.size();

        publishProgress( new DownloadedImageInfo( null, null, null, totalImages, -1 ) );

        Set<String> handledFiles = new HashSet<String>();
        SimpleNodeIterator i = result.elements();
        int displayedImageIndex = 0;
        int fileIndex = 0;
        while( displayedImageIndex < maxNumImages && i.hasMoreNodes() ) {
            Node node = i.nextNode();
            if( node instanceof ImageTag ) {
                ImageTag imageTag = (ImageTag)node;
                String imageUrl = imageTag.getImageURL();
                if( !handledFiles.contains( imageUrl ) ) {
                    Log.i( "image:" + imageUrl );
                    DownloadedImageInfo info = processImageUrl( imageUrl, args.storageDirectory, fileIndex, args.imageWidth, args.imageHeight );
                    if( info != null ) {
                        publishProgress( info );
                        displayedImageIndex++;
                    }
                    else {
                        publishProgress( new DownloadedImageInfo( null, null, null, -1, fileIndex ) );
                    }
                    handledFiles.add( imageUrl );
                }
            }
            fileIndex++;
        }

        return null;
    }

    private void purgePreviousFiles( File storageDirectory ) {
        FilenameFilter filter = new FilenameFilter()
        {
            @Override
            public boolean accept( File file, String name ) {
                return name.startsWith( TMP_FILE_PREFIX );
            }
        };
        for( File f : storageDirectory.listFiles( filter ) ) {
            Log.i( "deleting old file: " + f );
            if( !f.delete() ) {
                Log.w( "unable to delete file when purging tmp dir: " + f );
            }
        }
    }

    @Override
    protected void onProgressUpdate( DownloadedImageInfo... values ) {
        super.onProgressUpdate( values );
        ((PostSelectImagesActivity)getUnderlyingActivity()).imageDownloaded( values[0] );
    }

    @Override
    protected void onPostExecute( Void aVoid ) {
        super.onPostExecute( aVoid );
        ((PostSelectImagesActivity)getUnderlyingActivity()).downloadFinished();
    }

    private DownloadedImageInfo processImageUrl( String imageUrlString, File storageDirectory, int fileIndex, int imageWidth, int imageHeight ) {
        File file;
        try {
            file = ImageCache.copyUrlToFile( storageDirectory, imageUrlString, TMP_FILE_PREFIX );
        }
        catch( IOException e ) {
            Log.w( "unable to download file: " + imageUrlString, e );
            return null;
        }

        DownloadedImageInfo info = null;
        try {
            Bitmap bitmap = ImageHelpers.loadAndScaleBitmapWithMinimumSize( file.getPath(), imageWidth, imageHeight, 128, 128 );
            if( bitmap != null ) {
                info = new DownloadedImageInfo( imageUrlString, file, bitmap, -1, fileIndex );
            }
        }
        finally {
            if( info == null ) {
                if( !file.delete() ) {
                    Log.w( "error deleting file: " + file );
                }
            }
        }

        return info;
    }

    private NodeFilter createImgFilter() {
        return new TagNameFilter( "img" );
    }

    static class Args
    {
        public String url;
        public File storageDirectory;
        public int imageWidth;
        public int imageHeight;

        public Args( String url, File storageDirectory, int imageWidth, int imageHeight ) {
            this.url = url;
            this.storageDirectory = storageDirectory;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }
    }

    static class DownloadedImageInfo
    {
        public String url;
        public File file;
        public Bitmap bitmap;
        public int totalFiles;
        public int fileIndex;

        public DownloadedImageInfo( String url, File file, Bitmap bitmap, int totalFiles, int fileIndex ) {
            this.url = url;
            this.file = file;
            this.bitmap = bitmap;
            this.totalFiles = totalFiles;
            this.fileIndex = fileIndex;
        }
    }
}

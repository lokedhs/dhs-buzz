package org.atari.dhs.buzztest.androidclient.imagecache;

import java.io.*;
import java.net.URL;
import java.util.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.SQLiteDatabaseWrapper;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.tools.ImageHelpers;

public class ImageCache
{
    private static final String IMAGE_CACHE_DIR_NAME = "images";

    private Context context;

    private Map<String, BitmapCacheEntry> bitmapCache = new HashMap<String, BitmapCacheEntry>();

    private boolean initialised = false;
    private boolean shuttingDown = false;
    private boolean shutDown = false;

    private File cacheDir;
    private SQLiteDatabaseWrapper db;

    private boolean loadTaskIsActive = false;
    private List<LoadQueueEntry> loadQueue = new LinkedList<LoadQueueEntry>();

    public ImageCache( Context context ) {
        this.context = context;
    }

    public synchronized void close() {
        synchronized( bitmapCache ) {
            if( loadTaskIsActive ) {
                shuttingDown = true;
            }
            else {
                shutdownReal();
            }
        }
    }

    /**
     * This method is always to be called with the <code>bitmapCache</code> lock held.
     */
    private void shutdownReal() {
        if( !shutDown ) {
            if( db != null ) {
                SQLiteDatabaseWrapper dbCopy = db;
                db = null;
                dbCopy.close();
            }

            shutDown = true;
            shuttingDown = false;
        }
    }

    public boolean loadImage( final String url, final int imageWidth, final int imageHeight, final StorageType storageType, final LoadImageCallback callback ) {
        initialiseIfNeeded();

        boolean shouldStartTask = false;
        BitmapCacheEntry cacheEntry;
        synchronized( bitmapCache ) {
            if( shuttingDown ) {
                return true;
            }

            cacheEntry = bitmapCache.get( url );
            if( cacheEntry == null ) {
                cacheEntry = new BitmapCacheEntry( true );
                bitmapCache.put( url, cacheEntry );
                cacheEntry.addCallback( callback );
                loadQueue.add( new LoadQueueEntry( url, imageWidth, imageHeight, storageType, cacheEntry ) );
                if( !loadTaskIsActive ) {
                    loadTaskIsActive = true;
                    shouldStartTask = true;
                }
                Log.d( "created new cache entry. current load queue size=" + loadQueue.size() + ", willStartNewTask=" + shouldStartTask );
            }
            else {
                if( cacheEntry.loading ) {
                    cacheEntry.addCallback( callback );
                }
                else {
                    Bitmap bitmap = cacheEntry.bitmap;
                    if( bitmap != null ) {
                        callback.bitmapLoaded( cacheEntry.bitmap );
                        return false;
                    }
                    else {
                        Log.w( "null bitmap with loading = false" );
                        return true;
                    }
                }
            }
        }

        if( shouldStartTask ) {
            AsyncTask<Void, BackgroundLoadResult, Void> task = new LoadImagesTask();
            task.execute();
        }

        return true;
    }

    public static File copyUrlToFile( File cacheDirCopy, String urlString, String tmpFilePrefix ) throws IOException {
        StringBuilder buf = new StringBuilder();
        if( tmpFilePrefix != null ) {
            buf.append( tmpFilePrefix );
        }
        for( int i = 0 ; i < 20 ; i++ ) {
            buf.append( (char)('a' + ((int)(Math.random() * ('z' - 'a' + 1)))) );
        }
        buf.append( '_' );
        String s = buf.toString();
        File found = null;
        for( int i = 0 ; i < 30 ; i++ ) {
            String name = s + i;
            File f = new File( cacheDirCopy, name );
            if( f.createNewFile() ) {
                found = f;
                break;
            }
        }

        if( found == null ) {
            Log.w( "failed to create file name" );
            return null;
        }

        URL url = new URL( urlString );
        InputStream in = null;
        OutputStream out = null;
        try {
            try {
                in = url.openStream();
                out = new FileOutputStream( found );
                byte[] fileBuf = new byte[1024 * 16];
                int n;
                while( (n = in.read( fileBuf )) != -1 ) {
                    out.write( fileBuf, 0, n );
                }
            }
            finally {
                if( in != null ) {
                    in.close();
                }
                if( out != null ) {
                    out.close();
                }
            }
        }
        catch( IOException e ) {
            if( !found.delete() ) {
                Log.w( "exception when trying to delete broken download file: " + found );
            }
            throw e;
        }

        return found;
    }

    private void addCacheEntryToDatabase( String url, File newFile, StorageType storageType, boolean markFileAsAvailable ) {
        initialiseIfNeeded();

        ContentValues content = new ContentValues();
        content.put( StorageHelper.IMAGE_CACHE_NAME, url );
        content.put( StorageHelper.IMAGE_CACHE_FILENAME, newFile == null ? null : newFile.getName() );
        content.put( StorageHelper.IMAGE_CACHE_CREATED_DATE, System.currentTimeMillis() );
        content.put( StorageHelper.IMAGE_CACHE_CAN_DELETE, storageType != StorageType.LONG );
        content.put( StorageHelper.IMAGE_CACHE_IMAGE_AVAILABLE, markFileAsAvailable );
        db.getDatabase().insert( StorageHelper.IMAGE_CACHE_TABLE, "filename", content );
    }

    private void deleteCacheEntryFromDatabase( String url ) {
        initialiseIfNeeded();

        db.getDatabase().delete( StorageHelper.IMAGE_CACHE_TABLE, StorageHelper.IMAGE_CACHE_NAME + " = ?", new String[] { url } );
    }

    private File findCachedFileInDatabase( SQLiteDatabase dbCopy, File cacheDirCopy, String url ) {
        dbCopy.beginTransaction();
        Cursor result = null;
        try {
            result = dbCopy.query( StorageHelper.IMAGE_CACHE_TABLE,
                                   new String[] { "filename" },
                                   StorageHelper.IMAGE_CACHE_NAME + " = ?", new String[] { url },
                                   null,
                                   null,
                                   null );
            if( !result.moveToNext() ) {
                return null;
            }

            String filename = result.getString( 0 );

            result.close();
            result = null;

            File file = new File( cacheDirCopy, filename );
            if( !file.exists() ) {
                deleteCacheEntryFromDatabase( url );
                file = null;
            }

            dbCopy.setTransactionSuccessful();

            return file;
        }
        finally {
            if( result != null ) {
                result.close();
            }
            dbCopy.endTransaction();
        }
    }

    private void initialiseIfNeeded() {
        synchronized( bitmapCache ) {
            if( initialised ) {
                return;
            }
            if( shutDown ) {
                throw new IllegalStateException( "trying to initialise when already shut down" );
            }

            File root = context.getCacheDir();
            if( root != null ) {
                File dir = new File( root, IMAGE_CACHE_DIR_NAME );
                if( !dir.exists() ) {
                    if( dir.mkdir() ) {
                        cacheDir = dir;
                    }
                }
                else {
                    cacheDir = dir;
                }
            }

            if( cacheDir != null ) {
                db = StorageHelper.getDatabase( context );
            }

            initialised = true;
        }
    }

    public void purge( long cutoffOffsetLong, long cutoffOffsetShort ) {
        initialiseIfNeeded();

        long now = System.currentTimeMillis();
        long cutoffLong = now - cutoffOffsetLong;
        long cutoffShort = now - cutoffOffsetShort;

        List<String> toDelete = new ArrayList<String>();

        SQLiteDatabase database = db.getDatabase();
        Cursor result = database.query( StorageHelper.IMAGE_CACHE_TABLE,
                                        new String[] { StorageHelper.IMAGE_CACHE_NAME, StorageHelper.IMAGE_CACHE_FILENAME, StorageHelper.IMAGE_CACHE_CREATED_DATE, StorageHelper.IMAGE_CACHE_CAN_DELETE },
                                        null, null,
                                        null, null,
                                        null );
        try {
            while( result.moveToNext() ) {
                String name = result.getString( 0 );
                String fileName = result.getString( 1 );
                long createdDate = result.getLong( 2 );
                boolean canDelete = result.getInt( 3 ) != 0;

                File file = new File( cacheDir, fileName );
                if( !file.exists() ) {
                    toDelete.add( name );
                }
                else if( (canDelete && createdDate < cutoffShort) || (!canDelete && createdDate < cutoffLong) ) {
                    toDelete.add( name );
                    if( !file.delete() ) {
                        Log.w( "could not delete file: " + file );
                    }
                }
            }
        }
        finally {
            result.close();
        }

        for( String s : toDelete ) {
            database.delete( StorageHelper.IMAGE_CACHE_TABLE,
                             StorageHelper.IMAGE_CACHE_NAME + " = ?", new String[] { s } );
        }
    }

    public void stopAll() {
        synchronized( bitmapCache ) {
            loadQueue.clear();
        }
    }

    private static class BackgroundLoadResult
    {
        private String url;
        private BitmapCacheEntry bitmapCacheEntry;
        private Bitmap bitmap;

        public BackgroundLoadResult( String url, BitmapCacheEntry bitmapCacheEntry, Bitmap bitmap ) {
            this.url = url;
            this.bitmapCacheEntry = bitmapCacheEntry;
            this.bitmap = bitmap;
        }

        @Override
        public String toString() {
            return "BackgroundLoadResult[" +
                   "url='" + url + '\'' +
                   ", bitmapCacheEntry=" + bitmapCacheEntry +
                   ", bitmap=" + bitmap +
                   ']';
        }
    }

    private class LoadImagesTask extends AsyncTask<Void, BackgroundLoadResult, Void>
    {
        @Override
        protected Void doInBackground( Void... voids ) {
            LoadQueueEntry queueEntry;
            while( (queueEntry = getNextEntryAndMaybeUpdateStatus()) != null ) {
                SQLiteDatabaseWrapper dbCopy;
                File cacheDirCopy;
                synchronized( this ) {
                    dbCopy = db;
                    cacheDirCopy = cacheDir;
                }

                if( dbCopy == null || cacheDirCopy == null ) {
                    // Session has been closed, cancel right away
                    return null;
                }

                File cachedFile = findCachedFileInDatabase( dbCopy.getDatabase(), cacheDirCopy, queueEntry.url );
                Log.d( "cached image file=" + cachedFile + ", for url=" + queueEntry.url );
                boolean wasCached = cachedFile != null;
                if( cachedFile == null ) {
                    try {
                        cachedFile = copyUrlToFile( cacheDirCopy, queueEntry.url, null );
                        addCacheEntryToDatabase( queueEntry.url, cachedFile, queueEntry.storageType, false );
                    }
                    catch( IOException e ) {
                        Log.w( "failed to load image: '" + queueEntry.url + "'", e );
                        cachedFile = null;
                    }
                }

                Bitmap bitmap = null;
                if( cachedFile != null ) {
                    bitmap = ImageHelpers.loadAndScaleBitmap( cachedFile.getPath(), queueEntry.imageWidth, queueEntry.imageHeight );
                    // If the file should not be stored in the cache, and it was loaded (i.e. it wasn't already
                    // stored in the cache) it should be deleted at this point.
                    if( bitmap == null || (queueEntry.storageType == StorageType.DONT_STORE && !wasCached) ) {
                        removeOldFile( queueEntry.url, cachedFile );
                    }
                }
                if( bitmap == null ) {
                    // TODO: should delete the broken file at this point?
                    //markFileAsNotAvailable = true;
                }
                else {
                    publishProgress( new BackgroundLoadResult( queueEntry.url,
                                                               queueEntry.bitmapCacheEntry,
                                                               bitmap ) );
                }
            }

            synchronized( bitmapCache ) {
                if( shuttingDown ) {
                    shutdownReal();
                }
            }

            return null;
        }

        private LoadQueueEntry getNextEntryAndMaybeUpdateStatus() {
            synchronized( bitmapCache ) {
                if( loadQueue.isEmpty() ) {
                    loadTaskIsActive = false;
                    return null;
                }
                else {
                    return loadQueue.remove( 0 );
                }
            }
        }

        private void removeOldFile( String url, File file ) {
            if( !file.delete() ) {
                Log.w( "failed to delete file: " + file );
            }
            deleteCacheEntryFromDatabase( url );
        }

        @Override
        protected void onProgressUpdate( BackgroundLoadResult... values ) {
            BackgroundLoadResult result = values[0];
            Log.d( "onProgressUpdate. result=" + result );
            List<LoadImageCallback> callbacksCopy;
            Bitmap bitmap;
            synchronized( bitmapCache ) {
                BitmapCacheEntry entry = result.bitmapCacheEntry;
                bitmap = result.bitmap;
                entry.bitmap = bitmap;
                callbacksCopy = new ArrayList<LoadImageCallback>( entry.callbacks );
                entry.loading = false;
                entry.callbacks = null;
            }

            Log.d( "before calling callbacks. n=" + callbacksCopy.size() + ", bm=" + bitmap );
            if( bitmap != null ) {
                for( LoadImageCallback callback : callbacksCopy ) {
                    callback.bitmapLoaded( bitmap );
                }
            }
        }
    }
}

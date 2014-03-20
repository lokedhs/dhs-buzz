package org.atari.dhs.buzztest.androidclient.imagecache;

class LoadQueueEntry
{
    String url;
    int imageWidth;
    int imageHeight;
    StorageType storageType;
    BitmapCacheEntry bitmapCacheEntry;

    LoadQueueEntry( String url, int imageWidth, int imageHeight, StorageType storageType, BitmapCacheEntry bitmapCacheEntry ) {
        this.url = url;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.storageType = storageType;
        this.bitmapCacheEntry = bitmapCacheEntry;
    }
}

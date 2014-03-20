package org.atari.dhs.buzztest.androidclient.tools;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;

import org.atari.dhs.buzztest.androidclient.displayfeed.DisplayFeed;

public class HashTagSpan extends ClickableSpan implements ParcelableSpan
{
    private String hashtag;
    private URLSpan delegate;

    public HashTagSpan( String hashtag ) {
        this.hashtag = hashtag;
        this.delegate = new URLSpan( "http://link" );
    }

    public HashTagSpan( Parcel src ) {
        this.hashtag = src.readString();
        this.delegate = src.readParcelable( null );
    }

    @Override
    public void onClick( View view ) {
        Context context = view.getContext();
        Intent intent = new Intent( context, DisplayFeed.class );
        intent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_SEARCH );
        intent.putExtra( DisplayFeed.EXTRA_SEARCH_WORDS, new String[] { hashtag } );
    }

    @Override
    public int getSpanTypeId() {
        return delegate.getSpanTypeId();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel( Parcel parcel, int flags ) {
        parcel.writeString( hashtag );
        parcel.writeParcelable( delegate, flags );
    }

    public String getHashtag() {
        return hashtag;
    }
}

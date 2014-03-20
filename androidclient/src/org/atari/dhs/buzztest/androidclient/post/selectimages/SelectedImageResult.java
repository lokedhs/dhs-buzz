package org.atari.dhs.buzztest.androidclient.post.selectimages;

import android.os.Parcel;
import android.os.Parcelable;

public class SelectedImageResult implements Parcelable
{
    public String url;
    public String localFile;

    @SuppressWarnings({ "UnusedDeclaration" })
    public static final Parcelable.Creator<SelectedImageResult> CREATOR = new Parcelable.Creator<SelectedImageResult>()
    {
        public SelectedImageResult createFromParcel( Parcel in ) {
            return new SelectedImageResult( in );
        }

        public SelectedImageResult[] newArray( int size ) {
            return new SelectedImageResult[size];
        }
    };

    private SelectedImageResult( Parcel in ) {
        url = in.readString();
        localFile = in.readString();
    }

    public SelectedImageResult( String url, String localFile ) {
        this.url = url;
        this.localFile = localFile;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel( Parcel parcel, int flags ) {
        parcel.writeString( url );
        parcel.writeString( localFile );
    }
}

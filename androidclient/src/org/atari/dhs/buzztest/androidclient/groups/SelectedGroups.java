package org.atari.dhs.buzztest.androidclient.groups;

import android.os.Parcel;
import android.os.Parcelable;

public class SelectedGroups implements Parcelable
{
    private GroupItemInfo[] items;

    @SuppressWarnings( { "UnusedDeclaration" })
    public static final Parcelable.Creator<SelectedGroups> CREATOR = new Parcelable.Creator<SelectedGroups>()
    {
        public SelectedGroups createFromParcel( Parcel in ) {
            return new SelectedGroups( in );
        }

        public SelectedGroups[] newArray( int size ) {
            return new SelectedGroups[size];
        }
    };

    public SelectedGroups( Parcel in ) {
        items = in.createTypedArray( GroupItemInfo.CREATOR );
    }

    public SelectedGroups( GroupItemInfo[] items ) {
        this.items = items;
    }

    public GroupItemInfo[] getItems() {
        return items;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel( Parcel parcel, int flags ) {
        parcel.writeTypedArray( items, flags );
    }
}

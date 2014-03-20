package org.atari.dhs.buzztest.androidclient.groups;

import android.os.Parcel;
import android.os.Parcelable;

public class GroupItemInfo implements Parcelable
{
    private String groupName;
    private String id;

    @SuppressWarnings( { "UnusedDeclaration" })
    public static final Parcelable.Creator<GroupItemInfo> CREATOR = new Parcelable.Creator<GroupItemInfo>()
    {
        public GroupItemInfo createFromParcel( Parcel in ) {
            return new GroupItemInfo( in );
        }

        public GroupItemInfo[] newArray( int size ) {
            return new GroupItemInfo[size];
        }
    };

    public GroupItemInfo( Parcel in ) {
        groupName = in.readString();
        id = in.readString();
    }

    public GroupItemInfo( String groupName, String id ) {
        this.groupName = groupName;
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return groupName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel( Parcel parcel, int flags ) {
        parcel.writeString( groupName );
        parcel.writeString( id );
    }
}

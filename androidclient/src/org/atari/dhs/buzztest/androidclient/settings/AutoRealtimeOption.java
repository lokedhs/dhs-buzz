package org.atari.dhs.buzztest.androidclient.settings;

public enum AutoRealtimeOption
{
    ENABLE( 0 ),
    DISABLE( 1 ),
    ASK( 2 );

    private int index;

    AutoRealtimeOption( int index ) {
        this.index = index;
    }

    public static AutoRealtimeOption findByIndex( int index ) {
        for( AutoRealtimeOption option : values() ) {
            if( option.index == index ) {
                return option;
            }
        }

        throw new IllegalArgumentException( "illegal index: " + index );
    }

    public int getIndex() {
        return index;
    }
}

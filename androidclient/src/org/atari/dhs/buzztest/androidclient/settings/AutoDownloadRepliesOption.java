package org.atari.dhs.buzztest.androidclient.settings;

public enum AutoDownloadRepliesOption
{
    NEVER( 0 ),
    ALWAYS( 1 ),
    ONLY_WHEN_ON_WLAN( 2 );

    private int index;

    private AutoDownloadRepliesOption( int index ) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static AutoDownloadRepliesOption fromIndex( int index ) {
        for( AutoDownloadRepliesOption o : values() ) {
            if( o.index == index ) {
                return o;
            }
        }

        throw new IllegalArgumentException( "unknown index: " + index );
    }
}

package org.atari.dhs.buzztest.androidclient.settings;

public enum StartActivityType
{
    WELCOME_SCREEN( 0 ),
    PERSONAL_FEED( 1 ),
    NEARBY( 2 );

    private int index;

    StartActivityType( int index ) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static StartActivityType fromIndex( int index ) {
        for( StartActivityType type : values() ) {
            if( type.getIndex() == index ) {
                return type;
            }
        }
        throw new IllegalArgumentException( "illegal index: " + index );
    }
}

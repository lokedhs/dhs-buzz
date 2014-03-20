package org.atari.dhs.buzztest.androidclient.tools.posteditor;

public class SearchText implements CharSequence
{
    private String text;

    public SearchText( String text ) {
        this.text = text;
    }

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public char charAt( int index ) {
        return text.charAt( index );
    }

    @Override
    public CharSequence subSequence( int start, int end ) {
        return text.subSequence( start, end );
    }

    @Override
    public String toString() {
        return text;
    }
}

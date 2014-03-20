package org.atari.dhs.buzztest.androidclient.displaybuzz;

import android.text.Spanned;

public interface LinkViewFactoryContext
{
    void processSpannableForLinks( Spanned spanned );

    void addLink( String url, String title );
}

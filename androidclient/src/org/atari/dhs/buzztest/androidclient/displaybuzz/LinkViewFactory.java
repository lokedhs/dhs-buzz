package org.atari.dhs.buzztest.androidclient.displaybuzz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Attachment;

public interface LinkViewFactory
{
    View makeView( Context context, LayoutInflater layoutInflater, ViewGroup root, Attachment attachment, LinkViewFactoryContext linkViewFactoryContext );
}

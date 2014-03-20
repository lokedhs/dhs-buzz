package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.util.List;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Attachment;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.AttachmentLinkElement;

class NoteLinkViewFactory implements LinkViewFactory
{
    @Override
    public View makeView( Context context,
                          LayoutInflater layoutInflater,
                          ViewGroup root,
                          Attachment attachment,
                          LinkViewFactoryContext linkViewFactoryContext ) {
        String title = attachment.title;
        if( attachment.links == null ) {
            return null;
        }

        List<AttachmentLinkElement> l = attachment.links.alternate;
        if( l == null || l.isEmpty() ) {
            return null;
        }

        if( l.size() > 1 ) {
            Log.w( "more than one link in attachment link list" );
        }

        String url = l.get( 0 ).href;

        TextView view = new TextView( context );

//        view.setLinksClickable( true );
        view.setMovementMethod( LinkMovementMethod.getInstance() );
        view.setText( Html.fromHtml( DisplayBuzzListAdapter.makeLink( url, title ) ) );
        linkViewFactoryContext.addLink( url, title );

        return view;
    }
}

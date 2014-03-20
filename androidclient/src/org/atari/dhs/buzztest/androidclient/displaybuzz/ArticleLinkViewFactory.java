package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.util.List;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Attachment;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.AttachmentLinkElement;

class ArticleLinkViewFactory implements LinkViewFactory
{
    @Override
    public View makeView( Context context, LayoutInflater layoutInflater, ViewGroup root, Attachment attachment, LinkViewFactoryContext linkViewFactoryContext ) {
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

        if( attachment.title == null ) {
            // TODO: simply skip simple image links as they don't have a title. Probably should download and display the image instead.
            // It can be seen in this buzz:
            // https://www.googleapis.com/buzz/v1/activities/101261243957067319422/@self/B:z121exrqln2yzl1on04cetx4wky4ypowmt00k?alt=json&prettyprint=true
            return null;
        }

        String url = l.get( 0 ).href;

        View view = layoutInflater.inflate( R.layout.article_link_content, root, false );

        TextView titleTextView = (TextView)view.findViewById( R.id.title );
        titleTextView.setMovementMethod( LinkMovementMethod.getInstance() );

        TextView contentTextView = (TextView)view.findViewById( R.id.content );
        contentTextView.setMovementMethod( LinkMovementMethod.getInstance() );

//        titleTextView.setLinksClickable( true );
//        titleTextView.setMovementMethod( LinkMovementMethod.getInstance() );
        Spanned spannedContent = Html.fromHtml( DisplayBuzzListAdapter.makeLink( url, attachment.title ) );
        titleTextView.setText( spannedContent );
        linkViewFactoryContext.processSpannableForLinks( spannedContent );

        if( attachment.content == null || attachment.content.trim().equals( "" ) ) {
            contentTextView.setVisibility( View.GONE );
        }
        else {
            contentTextView.setText( Html.fromHtml( attachment.content ) );
        }

        return view;
    }
}

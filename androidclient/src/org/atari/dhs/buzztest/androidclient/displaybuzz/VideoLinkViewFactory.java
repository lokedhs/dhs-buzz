package org.atari.dhs.buzztest.androidclient.displaybuzz;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Attachment;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.AttachmentLinkElement;

class VideoLinkViewFactory implements LinkViewFactory
{
    @Override
    public View makeView( Context context,
                          LayoutInflater layoutInflater,
                          ViewGroup root,
                          Attachment attachment,
                          LinkViewFactoryContext linkViewFactoryContext ) {
        // Example video link: https://www.googleapis.com/buzz/v1/activities/118153417237614725639/@self/B:z13zejvgplrfdb1yv04chh0rbsezhfuyc2w?alt=json

        View v = layoutInflater.inflate( R.layout.display_buzz_video, root, false );

        TextView titleTextView = (TextView)v.findViewById( R.id.video_link_title );
        CharSequence title = attachment.title == null ? "" : Html.fromHtml( attachment.title );
        titleTextView.setText( title );

        TextView contentTextView = (TextView)v.findViewById( R.id.video_link_content );
        CharSequence content = attachment.content == null ? "" : Html.fromHtml( attachment.content );
        contentTextView.setText( content );

        for( AttachmentLinkElement element : attachment.links.alternate ) {
            if( element.type.equals( "text/html" ) ) {
                linkViewFactoryContext.addLink( element.href, attachment.title == null ? element.href : attachment.title );
                break;
            }
        }

        return v;
    }
}

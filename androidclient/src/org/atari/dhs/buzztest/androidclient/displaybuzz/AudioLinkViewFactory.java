package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Attachment;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.AttachmentLinkImageElement;

public class AudioLinkViewFactory implements LinkViewFactory
{
    @Override
    public View makeView( final Context context,
                          final LayoutInflater layoutInflater,
                          final ViewGroup root,
                          final Attachment attachment,
                          final LinkViewFactoryContext linkViewFactoryContext ) {
        if( attachment.links == null || attachment.links.enclosure == null || attachment.links.enclosure.isEmpty() ) {
            return null;
        }

        List<AttachmentLinkImageElement> enclosure = attachment.links.enclosure;
        String foundUrl = null;
        for( AttachmentLinkImageElement element : enclosure ) {
            if( element.type.equals( "audio/mpeg" ) ) {
                foundUrl = element.href;
                break;
            }
        }

        if( foundUrl == null ) {
            Log.w( "only non-mpeg links found in enclosure" );
            return null;
        }

        View v = layoutInflater.inflate( R.layout.display_buzz_audio, root, false );

//        final String foundUrl0 = foundUrl;
//
//        Button playButton = (Button)v.findViewById( R.id.play_audio_button );
//        playButton.setOnClickListener( new View.OnClickListener()
//        {
//            @Override
//            public void onClick( View view ) {
//                Log.d( "starting play activity" );
//                startPlayActivity( context, foundUrl0 );
//            }
//        } );

        TextView linkTextView = (TextView)v.findViewById( R.id.play_audio_text_view );
        String text = "<a href=\"" + foundUrl + "\">To play the attached audio, select Play from the Links menu entry</a>";
        linkTextView.setText( Html.fromHtml( text ) );

        linkViewFactoryContext.addLink( foundUrl, "Play" );

        return v;
    }

    private void startPlayActivity( Context context, String foundUrl ) {
        Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( foundUrl ) );
        context.startActivity( intent );
    }
}

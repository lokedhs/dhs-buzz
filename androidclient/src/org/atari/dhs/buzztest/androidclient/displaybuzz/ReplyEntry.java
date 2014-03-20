package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.util.Date;
import java.util.List;

import android.text.Html;
import android.text.Spanned;

import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply.BuzzReply;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.androidclient.tools.HashtagParser;

public class ReplyEntry
{
    private BuzzReply reply;
    private String formattedDate;
    private Spanned formattedFrom;
    private Spanned formattedText;
    private Date date;

    private List<LinkDescriptor> links;
    private boolean linksSet = false;

    public ReplyEntry( BuzzReply reply ) {
        this.reply = reply;
    }

    public BuzzReply getReply() {
        return reply;
    }
    public Date getDate() {
        getFormattedDate();
        return date;
    }

    public String getFormattedDate() {
        if( formattedDate == null ) {
            DateHelper dateHelper = new DateHelper();
            date = dateHelper.parseDate( reply.published );
            formattedDate = dateHelper.formatDateTimeOutputFormat( date );
        }
        return formattedDate;
    }

    public Spanned getFormattedFrom() {
        if( formattedFrom == null ) {
            formattedFrom = Html.fromHtml( DisplayBuzzListAdapter.makeLink( reply.actor.profileUrl, reply.actor.name ) );
        }
        return formattedFrom;
    }

    public Spanned getFormattedText() {
        if( formattedText == null ) {
            formattedText = HashtagParser.parseHashtaggedHtml( reply.content );
        }
        return formattedText;
    }

    public boolean linksParsed() {
        return linksSet;
    }

    public List<LinkDescriptor> getLinks() {
        return links;
    }

    public void setLinks( List<LinkDescriptor> links ) {
        this.links = links;
        linksSet = true;
    }
}

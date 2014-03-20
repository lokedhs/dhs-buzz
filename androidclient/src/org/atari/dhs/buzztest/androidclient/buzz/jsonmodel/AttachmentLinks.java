package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class AttachmentLinks implements Serializable
{
    @Key("preview")
    public List<AttachmentLinkImageElement> preview;

    @Key("enclosure")
    public List<AttachmentLinkImageElement> enclosure;

    @Key("alternate")
    public List<AttachmentLinkElement> alternate;

    @Override
    public String toString() {
        return "AttachmentLinks[" +
               "preview=" + preview +
               ", enclosure=" + enclosure +
               ", alternate=" + alternate +
               ']';
    }
}

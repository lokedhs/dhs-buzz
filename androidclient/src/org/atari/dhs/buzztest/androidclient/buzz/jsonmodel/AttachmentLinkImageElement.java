package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class AttachmentLinkImageElement implements Serializable
{
    @Key("href")
    public String href;

    @Key("type")
    public String type;

    @Key("height")
    public int height;

    @Key("width")
    public int width;

    @Override
    public String toString() {
        return "AttachmentLinkImageElement[" +
               "href='" + href + '\'' +
               ", type='" + type + '\'' +
               ", height=" + height +
               ", width=" + width +
               ']';
    }
}

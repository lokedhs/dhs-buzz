package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class AttachmentLinkElement implements Serializable
{
    @Key("href")
    public String href;

    @Key("type")
    public String type;

    @Override
    public String toString() {
        return "AttachmentLinkElement[" +
               "href='" + href + '\'' +
               ", type='" + type + '\'' +
               ']';
    }
}

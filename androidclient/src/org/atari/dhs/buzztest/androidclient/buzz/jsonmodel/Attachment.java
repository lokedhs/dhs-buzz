package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class Attachment implements Serializable
{
    public static final String TYPE_PHOTO = "photo";
    public static final String TYPE_ARTICLE = "article";

    @Key("type")
    public String type;

    @Key("title")
    public String title;

    @Key("links")
    public AttachmentLinks links;

    @Key("content")
    public String content;

    @Override
    public String toString() {
        return "Attachment[" +
               "type='" + type + '\'' +
               ", title='" + title + '\'' +
               ", links=" + links +
               ", content='" + content + '\'' +
               ']';
    }
}

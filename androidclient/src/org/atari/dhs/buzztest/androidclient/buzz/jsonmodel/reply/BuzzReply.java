package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply;

import java.io.Serializable;

import com.google.api.client.util.Key;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Actor;

public class BuzzReply implements Serializable
{
    @Key("kind")
    public String kind;

    @Key("id")
    public String id;

    @Key("published")
    public String published;

    @Key("actor")
    public Actor actor;

    @Key("content")
    public String content;

    @Key("originalContent")
    public String originalContent;
}

package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class BuzzObject implements Serializable
{
    @Key("id")
    public String id;

    @Key("content")
    public String content;

    @Key("attachments")
    public List<Attachment> attachments;

    @Key("actor")
    public Actor actor;

    @Override
    public String toString() {
        return "BuzzObject[" +
               "content='" + content + '\'' +
               ", attachments=" + attachments +
               ", actor=" + actor +
               ']';
    }

    public String findActorName() {
        if( actor != null && actor.name != null ) {
            return actor.name;
        }
        else {
            return "unknown";
        }
    }
}

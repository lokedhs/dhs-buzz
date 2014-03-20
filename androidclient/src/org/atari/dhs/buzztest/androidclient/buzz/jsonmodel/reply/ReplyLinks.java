package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;

public class ReplyLinks implements Serializable
{
    @Key("next")
    public List<ReplyNextLink> next;
}

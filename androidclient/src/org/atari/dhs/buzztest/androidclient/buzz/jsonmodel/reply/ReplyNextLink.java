package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class ReplyNextLink implements Serializable
{
    @Key("href")
    public String href;
}

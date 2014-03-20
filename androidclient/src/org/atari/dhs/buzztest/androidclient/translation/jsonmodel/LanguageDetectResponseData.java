package org.atari.dhs.buzztest.androidclient.translation.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class LanguageDetectResponseData implements Serializable
{
    @Key("language")
    public String language;

    @Key("isReliable")
    public boolean isReliable;

    @Key("confidence")
    public float confidence;
}

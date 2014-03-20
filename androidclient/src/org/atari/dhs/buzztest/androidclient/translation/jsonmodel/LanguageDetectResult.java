package org.atari.dhs.buzztest.androidclient.translation.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class LanguageDetectResult implements Serializable
{
    @Key("responseData")
    public LanguageDetectResponseData responseData;

    @Key("responseDetails")
    public String responseDetails;

    @Key("responseStatus")
    public int responseStatus;
}

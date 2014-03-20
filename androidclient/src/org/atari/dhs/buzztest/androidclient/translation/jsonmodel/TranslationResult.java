package org.atari.dhs.buzztest.androidclient.translation.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class TranslationResult implements Serializable
{
    @Key("responseData")
    public TranslationResponseData responseData;

    @Key("responseDetails")
    public String responseDetails;

    @Key("responseStatus")
    public int responseStatus;

    @Override
    public String toString() {
        return "TranslationResult[" +
               "responseData=" + responseData +
               ", responseDetails='" + responseDetails + '\'' +
               ", responseStatus=" + responseStatus +
               ']';
    }
}

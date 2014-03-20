package org.atari.dhs.buzztest.androidclient.translation.jsonmodel;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class TranslationResponseData implements Serializable
{
    @Key("translatedText")
    public String translatedText;

    @Override
    public String toString() {
        return "TranslationResponseData[" +
               "translatedText='" + translatedText + '\'' +
               ']';
    }
}

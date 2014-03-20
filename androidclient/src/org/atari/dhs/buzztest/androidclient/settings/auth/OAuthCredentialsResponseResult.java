package org.atari.dhs.buzztest.androidclient.settings.auth;

import com.google.api.client.auth.oauth.OAuthCredentialsResponse;

class OAuthCredentialsResponseResult
{
    OAuthCredentialsResponse response;
    String errorMessage;

    OAuthCredentialsResponseResult( OAuthCredentialsResponse response ) {
        this.response = response;
    }

    OAuthCredentialsResponseResult( String errorMessage ) {
        this.errorMessage = errorMessage;
    }
}

package org.atari.dhs.buzztest.androidclient.buzz;

public final class BuzzGlobal
{

    /**
     * Version name.
     */
    public static final String VERSION = "1";

    /**
     * Root URL.
     */
    public static final String ROOT_URL = "https://www.googleapis.com/buzz/v1/";

    /**
     * The authentication token type used for Client Login.
     */
    public static final String AUTH_TOKEN_TYPE = "buzz";

    /**
     * OAuth authorization service endpoint.
     *
     * @since 2.3
     */
    public static final String OAUTH_AUTHORIZATION_URL =
            "https://www.google.com/buzz/api/auth/OAuthAuthorizeToken";

    /**
     * @since 2.3
     */
    public static final String OAUTH_SCOPE =
            "https://www.googleapis.com/auth/buzz";

    /**
     * @since 2.3
     */
    public static final String OAUTH_SCOPE_READ_ONLY =
            "https://www.googleapis.com/auth/buzz.readonly";

    private BuzzGlobal()
    {
    }
}

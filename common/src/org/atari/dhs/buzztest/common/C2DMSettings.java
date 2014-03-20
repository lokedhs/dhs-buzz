package org.atari.dhs.buzztest.common;

public class C2DMSettings
{
    public static final String C2DM_USER = "loke.develtest@gmail.com";

    public static final String C2DM_FIELD_TYPE = "t";

    public static final String C2DM_FIELD_ACTIVITY_SUB_MESSAGE_SENDER = "buzz_sender";
    public static final String C2DM_FIELD_ACTIVITY_SUB_MESSAGE_PUBLISHED = "buzz_published";
    public static final String C2DM_FIELD_ACTIVITY_SUB_MESSAGE_ACTIVITY_ID = "buzz_activityurl";

    public static final String C2DM_FIELD_SEARCH_SUB_MESSAGE_ID = "id";
    public static final String C2DM_FIELD_SEARCH_SUB_MESSAGE_DEVICE_SUBSCRIPTION_ID = "si";
    public static final String C2DM_FIELD_SEARCH_SUB_MESSAGE_ACTOR = "act";
    public static final String C2DM_FIELD_SEARCH_SUB_MESSAGE_ACTOR_ID = "aId";
    public static final String C2DM_FIELD_SEARCH_SUB_MESSAGE_NUM_REPLIES = "rep";
    public static final String C2DM_FIELD_SEARCH_SUB_MESSAGE_UPDATED = "upd";
    public static final String C2DM_FIELD_SEARCH_SUB_MESSAGE_TITLE = "content";
    public static final String C2DM_FIELD_SEARCH_SUB_MESSAGE_PHOTO_URL = "photo";

    public static final String C2DM_TYPE_ACTIVITY = "a";
    public static final String C2DM_TYPE_SEARCH = "s";
    public static final String C2DM_TYPE_UNSUBSCRIPTION_NOTIFICATION = "u";

    public static final String C2DM_PARAM_REQUEST_COMMAND = "cmd";
    public static final String C2DM_PARAM_REQUEST_KEY = "c2dmKey";
    public static final String C2DM_PARAM_REQUEST_ACTIVITY = "activity";
    public static final String C2DM_PARAM_REQUEST_USER_NAME = "user";
    public static final String C2DM_PARAM_REQUEST_USER_ID = "userId";
    public static final String C2DM_PARAM_REQUEST_SEARCH_TARGET = "search";
    public static final String C2DM_PARAM_REQUEST_DEVICE_SUBSCRIPTION_ID = "subscriptionId";

    public static final String C2DM_COMMAND_VALUE_SUBSCRIBE = "subscribe";
    public static final String C2DM_COMMAND_VALUE_UNSUBSCRIBE = "unsubscribe";
    public static final String C2DM_COMMAND_VALUE_REFRESH = "refresh";

    private C2DMSettings() {
        // prevent instantiation
    }
}

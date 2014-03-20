package org.atari.dhs.buzztest.androidclient.service;

interface IPostMessageService {
    void postMessage( in String text,
                        in String attachedUrlTitle, in String attachedUrl,
                        in String[] attachedImages, in String[] attachImageUrls,
                        in String geocode, in String placeId, in String[] groups );

    void postReshare( in String activityId, in String annotation );

    void editMessage( in String activityId, in String text );

    void postReply( in String activityId, in String text, in boolean subscribe );

    void editReply( in String activityId, in String replyId, in String content );

    void deleteReply( in String activityId, in String replyId );

    void startSync( in String storedFeedKey, in String activityId );

    void loadMore( in String storedFeedKey, in String activityId );

    void muteFeed( in String activityId );

    void markAsSpam( in String activityId );

    void markAsLiked( in String activityId );

    void updateWatchlistStatus( in String activityId, in boolean add );

    void startRealtimeSubscription( in String activityId, in String senderName, in boolean start );

    void startRealtimeFeedSubscription( in String searchWords );

    void stopRealtimeFeedSubscription( in long realtimeFeedId );

    void refreshRealtimeFeedSubscriptions();

    void updateFollow( in String userId, in String userName, in boolean start );

    void purge();
}

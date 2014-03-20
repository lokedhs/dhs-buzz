package org.atari.dhs.buzztest.androidclient.service;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import org.atari.dhs.buzztest.androidclient.service.task.*;

public class PostMessageServiceImpl extends IPostMessageService.Stub
{
    private PostMessageService postMessageService;

    public PostMessageServiceImpl( PostMessageService postMessageService ) {
        this.postMessageService = postMessageService;
    }

    @Override
    public void postMessage( String text,
                             String attachedUrlTitle, String attachedUrl,
                             String[] attachedImages, String[] attachedImageUrls,
                             String geocode, String placeId, String[] groups ) throws RemoteException {
        BundleX b = new BundleX();
        b.putString( PostMessageTask.TEXT_KEY, text );
        if( attachedUrl != null && attachedUrlTitle != null ) {
            b.putString( PostMessageTask.ATTACHED_URL_TITLE_KEY, attachedUrlTitle );
            b.putString( PostMessageTask.ATTACHED_URL_KEY, attachedUrl );
        }
        if( attachedImageUrls != null ) {
            b.putStringArray( PostMessageTask.ATTACHED_IMAGE_URLS_KEY, attachedImageUrls );
        }
        if( geocode != null ) {
            b.putString( PostMessageTask.GEOCODE_KEY, geocode );
            b.putString( PostMessageTask.PLACE_ID_KEY, placeId );
        }
        if( groups != null ) {
            b.putStringArray( PostMessageTask.POST_PRIVATE_GROUPS_KEY, groups );
        }
        b.putStringArray( PostMessageTask.ATTACHED_IMAGES_KEY, attachedImages );
        createIntentAndStartWithLocalContext( PostMessageTask.COMMAND_NAME, b );
    }

    @Override
    public void postReshare( String activityId, String annotation ) throws RemoteException {
        BundleX b = new BundleX();
        b.putString( ShareMessageTask.SHARED_ACTIVITY_ID_KEY, activityId );
        b.putString( ShareMessageTask.ANNOTATION_KEY, annotation );
        createIntentAndStartWithLocalContext( ShareMessageTask.COMMAND_NAME, b );
    }

    @Override
    public void editMessage( String activityId, String text ) throws RemoteException {
        BundleX b = new BundleX();
        b.putString( EditPostTask.ACTIVITY_ID_KEY, activityId );
        b.putString( EditPostTask.TEXT_KEY, text );
        createIntentAndStartWithLocalContext( EditPostTask.COMMAND_NAME, b );
    }

    @Override
    public void postReply( String activityId, String text, boolean subscribe ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( PostReplyTask.ACTIVITY_ID_KEY, activityId );
        args.putString( PostReplyTask.TEXT_KEY, text );
        args.putBoolean( PostReplyTask.SUBSCRIBE_REALTIME_KEY, subscribe );
        createIntentAndStartWithLocalContext( PostReplyTask.COMMAND_NAME, args );
    }

    @Override
    public void editReply( String activityId, String replyId, String content ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( EditReplyTask.ACTIVITY_ID_KEY, activityId );
        args.putString( EditReplyTask.REPLY_ID_KEY, replyId );
        args.putString( EditReplyTask.TEXT_KEY, content );
        createIntentAndStartWithLocalContext( EditReplyTask.COMMAND_NAME, args );
    }

    @Override
    public void deleteReply( String activityId, String replyId ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( DeleteReplyTask.ACTIVITY_ID_KEY, activityId );
        args.putString( DeleteReplyTask.REPLY_ID_KEY, replyId );
        createIntentAndStartWithLocalContext( DeleteReplyTask.COMMAND_NAME, args );
    }

    @Override
    public void startSync( String storedFeedKey, String activityId ) throws RemoteException {
//        BundleX args = new BundleX();
//        args.putString( SyncFeedTask.ACTIVITY_URL_KEY, activityId );
//        args.putString( SyncFeedTask.STORED_FEED_VALUE_KEY, storedFeedKey );
//        createIntentAndStartWithLocalContext( SyncFeedTask.COMMAND_NAME, args );
        startSyncWithContext( postMessageService, storedFeedKey, activityId, false );
    }

    public static void startSyncWithContext( Context context, String storedFeedKey, String activityId, boolean awake ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( SyncFeedTask.ACTIVITY_URL_KEY, activityId );
        args.putString( SyncFeedTask.STORED_FEED_VALUE_KEY, storedFeedKey );
        createIntentAndStart( context, SyncFeedTask.COMMAND_NAME, args, awake );
    }

    @Override
    public void loadMore( String storedFeedId, String activityId ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( LoadMoreTask.ACTIVITY_ID_KEY, activityId );
        args.putString( LoadMoreTask.STORED_FEED_VALUE_KEY, storedFeedId );
        createIntentAndStartWithLocalContext( LoadMoreTask.COMMAND_NAME, args );
    }

    @Override
    public void muteFeed( String activityId ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( MuteFeedTask.ACTIVITY_ID_KEY, activityId );
        createIntentAndStartWithLocalContext( MuteFeedTask.COMMAND_NAME, args );
    }

    @Override
    public void markAsSpam( String activityId ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( MarkAsSpamTask.ACTIVITY_ID_KEY, activityId );
        createIntentAndStartWithLocalContext( MarkAsSpamTask.COMMAND_NAME, args );
    }

    @Override
    public void markAsLiked( String activityId ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( MarkAsLikedTask.ACTIVITY_ID_KEY, activityId );
        createIntentAndStartWithLocalContext( MarkAsLikedTask.COMMAND_NAME, args );
    }

    @Override
    public void updateWatchlistStatus( String activityId, boolean add ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( UpdateWatchlistTask.ACTIVITY_ID_KEY, activityId );
        args.putBoolean( UpdateWatchlistTask.ENABLE_KEY, add );
        createIntentAndStartWithLocalContext( UpdateWatchlistTask.COMMAND_NAME, args );
    }

    @Override
    public void startRealtimeSubscription( String activityId, String senderName, boolean start ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( RegisterRealtimeTask.ACTIVITY_ID_KEY, activityId );
        args.putBoolean( RegisterRealtimeTask.START_KEY, start );
        if( senderName != null ) {
            args.putString( RegisterRealtimeTask.SENDER_NAME_KEY, senderName );
        }
        createIntentAndStartWithLocalContext( RegisterRealtimeTask.COMMAND_NAME, args );
    }

    @Override
    public void startRealtimeFeedSubscription( String searchWords ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( RegisterRealtimeSearchFeedTask.SEARCH_WORDS_KEY, searchWords );
        createIntentAndStartWithLocalContext( RegisterRealtimeSearchFeedTask.COMMAND_NAME, args );
    }

    @Override
    public void stopRealtimeFeedSubscription( long realtimeFeedId ) throws RemoteException {
        BundleX args = new BundleX();
        args.putLong( UnregisterRealtimeSearchFeedTask.REALTIME_FEED_ID_KEY, realtimeFeedId );
        createIntentAndStartWithLocalContext( UnregisterRealtimeSearchFeedTask.COMMAND_NAME, args );
    }

    @Override
    public void refreshRealtimeFeedSubscriptions() throws RemoteException {
        BundleX args = new BundleX();
        createIntentAndStartWithLocalContext( RefreshRealtimeSearchFeedTask.COMMAND_NAME, args );
    }

    @Override
    public void updateFollow( String userId, String userName, boolean start ) throws RemoteException {
        BundleX args = new BundleX();
        args.putString( UpdateFollowingTask.USER_ID_KEY, userId );
        if( userName != null ) {
            args.putString( UpdateFollowingTask.USER_NAME_KEY, userName );
        }
        args.putBoolean( UpdateFollowingTask.START_KEY, start );
        createIntentAndStartWithLocalContext( UpdateFollowingTask.COMMAND_NAME, args );
    }

    @Override
    public void purge() throws RemoteException {
        BundleX args = new BundleX();
        createIntentAndStartWithLocalContext( PruneDatabaseTask.COMMAND_NAME, args );
    }

    private void createIntentAndStartWithLocalContext( String commandName, BundleX args ) {
        createIntentAndStart( postMessageService, commandName, args, false );
    }

    public static void createIntentAndStart( Context context, String commandName, BundleX args, boolean keepWakelock ) {
        Intent intent = createIntent( context, commandName, args, keepWakelock );
        context.startService( intent );
    }

    public static Intent createIntent( Context context, String commandName, BundleX args, boolean keepWakelock ) {
        Intent intent = new Intent( context, BuzzManagerService.class );
        intent.putExtra( BuzzManagerService.COMMAND_NAME_KEY, commandName );
        if( args != null ) {
            intent.putExtra( BuzzManagerService.ARGUMENTS_KEY, args );
        }
        intent.putExtra( BuzzManagerService.KEEP_WAKELOCK_KEY, keepWakelock );
        return intent;
    }
}

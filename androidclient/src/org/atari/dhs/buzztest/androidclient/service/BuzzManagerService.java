package org.atari.dhs.buzztest.androidclient.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.atari.dhs.buzztest.androidclient.*;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.failedtasks.FailedTasks;
import org.atari.dhs.buzztest.androidclient.service.task.*;
import org.atari.dhs.buzztest.androidclient.tools.PendingIntentFactory;
import org.atari.dhs.buzztest.androidclient.tools.SerialisationHelper;

public class BuzzManagerService extends Service
{
    public static final String COMMAND_NAME_KEY = "command";
    public static final String ARGUMENTS_KEY = "args";
    public static final String KEEP_WAKELOCK_KEY = "keepWakelock";

    private static final Map<String, ServiceTask> TASK_REGISTRY;

    private NotificationManager notificationManager;

    private TaskContext taskContext;

    private BuzzManager buzzManager;
    private SQLiteDatabaseWrapper dbInternal;

    private List<QueueEntry> queue = new LinkedList<QueueEntry>();
    private Thread queueHandlerThread;
    private PowerManager powerManager;

    static {
        Map<String, ServiceTask> t = new HashMap<String, ServiceTask>();
        t.put( SyncFeedTask.COMMAND_NAME, new SyncFeedTask() );
        t.put( PostReplyTask.COMMAND_NAME, new PostReplyTask() );
        t.put( EditReplyTask.COMMAND_NAME, new EditReplyTask() );
        t.put( DeleteReplyTask.COMMAND_NAME, new DeleteReplyTask() );
        t.put( PostMessageTask.COMMAND_NAME, new PostMessageTask() );
        t.put( ShareMessageTask.COMMAND_NAME, new ShareMessageTask() );
        t.put( MuteFeedTask.COMMAND_NAME, new MuteFeedTask() );
        t.put( MarkAsSpamTask.COMMAND_NAME, new MarkAsSpamTask() );
        t.put( MarkAsLikedTask.COMMAND_NAME, new MarkAsLikedTask() );
        t.put( RegisterRealtimeTask.COMMAND_NAME, new RegisterRealtimeTask() );
        t.put( RegisterRealtimeSearchFeedTask.COMMAND_NAME, new RegisterRealtimeSearchFeedTask() );
        t.put( UnregisterRealtimeSearchFeedTask.COMMAND_NAME, new UnregisterRealtimeSearchFeedTask() );
        t.put( RefreshRealtimeSearchFeedTask.COMMAND_NAME, new RefreshRealtimeSearchFeedTask() );
        t.put( UpdateWatchlistTask.COMMAND_NAME, new UpdateWatchlistTask() );
        t.put( LoadMoreTask.COMMAND_NAME, new LoadMoreTask() );
        t.put( PruneDatabaseTask.COMMAND_NAME, new PruneDatabaseTask() );
        t.put( EditPostTask.COMMAND_NAME, new EditPostTask() );
        t.put( UpdateFollowingTask.COMMAND_NAME, new UpdateFollowingTask() );
        TASK_REGISTRY = t;
    }

    public BuzzManagerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        powerManager = (PowerManager)getSystemService( Context.POWER_SERVICE );

        taskContext = new TaskContextImpl();
        notificationManager = (NotificationManager)getSystemService( NOTIFICATION_SERVICE );

        queueHandlerThread = new Thread( new QueueHandler() );
        queueHandlerThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        queueHandlerThread.interrupt();

        if( dbInternal != null ) {
            dbInternal.close();
            dbInternal = null;
        }
        if( buzzManager != null ) {
            buzzManager.close();
            buzzManager = null;
        }
    }

    @Override
    public IBinder onBind( Intent intent ) {
        return null;
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
        Log.i( "service onStartCommand" );

        if( intent != null ) {
            Bundle extras = intent.getExtras();
            String command = extras.getString( COMMAND_NAME_KEY );
            boolean keepWakelock = extras.getBoolean( KEEP_WAKELOCK_KEY, false );

            BundleX args = (BundleX)extras.getSerializable( ARGUMENTS_KEY );
            synchronized( queue ) {
                queue.add( new QueueEntry( command, keepWakelock, args ) );
                queue.notify();
            }
        }

        return START_STICKY;
    }

    @SuppressWarnings( { "unchecked" })
    private void handleIntentCommand( String command, boolean keepWakelock, BundleX args ) {
        ServiceTask task = TASK_REGISTRY.get( command );
        if( task == null ) {
            Log.e( "nonexistent command: " + command );
        }
        else {
            try {
                long now = System.currentTimeMillis();

                Notification notification = null;
                if( task.displayNotification() ) {
                    String msg = getResources().getString( R.string.buzz_manager_communicating_with_server );
                    notification = new Notification( R.drawable.ic_stat_notify_loading, msg, now );
                    //                notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
                    notification.flags |= Notification.FLAG_ONGOING_EVENT;
                    notification.flags |= Notification.FLAG_NO_CLEAR;
                    Intent notificationBarIntent = new Intent( this, WelcomeScreen.class );
                    PendingIntent contentIntent = PendingIntent.getActivity( this, 0, notificationBarIntent, 0 );
                    notification.setLatestEventInfo( this, msg, task.getNotificationTickerText( this, args ), contentIntent );
                    notificationManager.notify( DhsBuzzApp.IN_PROGRESS_NOTIFICATION_ID, notification );
                }
                try {
                    runTaskWithWakelock( args, keepWakelock, task );
                }
                finally {
                    if( notification != null ) {
                        notificationManager.cancel( DhsBuzzApp.IN_PROGRESS_NOTIFICATION_ID );
                    }
                }
            }
            catch( RestartableServiceTaskFailedException e ) {
                reportError( "task failed, restartable", e );
                BundleX newArgs = args;
                BundleX restartArgs = e.getRestartArgs();
                if( restartArgs != null ) {
                    newArgs = restartArgs;
                }
                saveTaskForRestart( task.getNotificationTickerText( this, newArgs ).toString(), e.getLocalizedMessage(), command, newArgs );
            }
            catch( ReportableServiceTaskFailedException e ) {
                reportError( "task failed, will report in notification", e );
                reportErrorInNotification( task.getNotificationTickerText( this, args ).toString(), e.getLocalizedMessage(), e.getIntent() );
            }
            catch( ServiceTaskFailedException e ) {
                throw new RuntimeException( "task failed: " + command, e );
            }
        }
    }

    private void runTaskWithWakelock( BundleX args, boolean keepWakelock, ServiceTask task ) throws ServiceTaskFailedException {
        PowerManager.WakeLock wakelock = null;
        if( keepWakelock ) {
            wakelock = powerManager.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, "Buzz service" );
            wakelock.acquire();
        }
        try {
            task.processTask( taskContext, args );
        }
        finally {
            if( keepWakelock ) {
                wakelock.release();
            }
        }
    }

    private void reportError( String message, ServiceTaskFailedException e ) {
        Log.w( message, e );

        Throwable cause = e.getCause();
        if( cause != null && cause instanceof HttpResponseException ) {
            Log.w( "underlying error was http exception, detailed result" );

            HttpResponseException responseException = (HttpResponseException)cause;
            HttpResponse response = responseException.response;
            if( response != null ) {
                try {
                    InputStream content = response.getContent();
                    if( content != null ) {
                        BufferedReader in = new BufferedReader( new InputStreamReader( content, "UTF-8" ) );
                        String s;
                        while( (s = in.readLine()) != null ) {
                            Log.w( "line: " + s );
                        }
                        in.close();
                    }
                }
                catch( IOException e2 ) {
                    Log.e( "exception when processing detailed error", e2 );
                }
            }
        }
    }

    private void saveTaskForRestart( String title, String message, String command, BundleX args ) {
        SQLiteDatabase db = getDatabase();

        ContentValues values = new ContentValues();
        values.put( StorageHelper.PENDING_TASK_CREATED_DATE, System.currentTimeMillis() );
        values.put( StorageHelper.PENDING_TASK_TITLE, title );
        values.put( StorageHelper.PENDING_TASK_MESSAGE, message );
        values.put( StorageHelper.PENDING_TASK_OBJECT, SerialisationHelper.serialiseObject( new PersistedCommand( command, args ) ) );
        db.insert( StorageHelper.PENDING_TASK_TABLE, StorageHelper.PENDING_TASK_OBJECT, values );

        Resources resources = taskContext.getService().getResources();
        Notification notification = new Notification( R.drawable.ic_stat_notify_error, resources.getString( R.string.task_restartable_failed_notification ), System.currentTimeMillis() );
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.ledARGB = 0xFFFFFFFF;
        notification.ledOnMS = 1000;
        notification.ledOffMS = 1000;
        Intent notificationBarIntent = new Intent( this, FailedTasks.class );
        PendingIntent contentIntent = PendingIntent.getActivity( this, 0, notificationBarIntent, 0 );
        notification.setLatestEventInfo( this,
                                         resources.getString( R.string.task_restartable_failed_title ),
                                         resources.getString( R.string.task_restartable_failed_content ),
                                         contentIntent );
        notificationManager.notify( DhsBuzzApp.TASK_RESTARTABLE_ERROR_NOTIFICATION_ID, notification );
    }

    private void reportErrorInNotification( String title, String message, PendingIntentFactory intent ) {
        Resources resources = taskContext.getService().getResources();
        Notification notification = new Notification( R.drawable.ic_stat_notify_error, resources.getString( R.string.task_non_restart_failed_notification ), System.currentTimeMillis() );
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        PendingIntent contentIntent;
        if( intent == null ) {
            Intent notificationBarIntent = new Intent( this, WelcomeScreen.class );
            contentIntent = PendingIntent.getActivity( this, 0, notificationBarIntent, 0 );
        }
        else {
            contentIntent = intent.makePendingIntent( this, 0 );
        }
        notification.setLatestEventInfo( this, title, message, contentIntent );
        notificationManager.notify( DhsBuzzApp.TASK_FAIL_NOTIFICATION_ID, notification );
    }

    private BuzzManager getBuzzManager() {
        synchronized( this ) {
            if( buzzManager == null ) {
                buzzManager = BuzzManager.createFromContext( this );
            }
            return buzzManager;
        }
    }

    private SQLiteDatabase getDatabase() {
        synchronized( this ) {
            if( dbInternal == null ) {
                dbInternal = StorageHelper.getDatabase( this );
            }
            return dbInternal.getDatabase();
        }
    }

    public static String throwIOExceptionError( TaskContext context, IOException exception ) throws RestartableServiceTaskFailedException {
        Resources resources = context.getService().getResources();
        String fmt = resources.getString( R.string.error_communication_with_description );
        throw new RestartableServiceTaskFailedException( MessageFormat.format( fmt, exception.getLocalizedMessage() ), exception );
    }

    private class TaskContextImpl implements TaskContext
    {
        @Override
        public BuzzManager getBuzzManager() {
            return BuzzManagerService.this.getBuzzManager();
        }

        @Override
        public SQLiteDatabase getDatabase() {
            return BuzzManagerService.this.getDatabase();
        }

        @Override
        public Service getService() {
            return BuzzManagerService.this;
        }
    }

    private static class QueueEntry
    {
        String command;
        boolean keepWakelock;
        BundleX args;

        private QueueEntry( String command, boolean keepWakelock, BundleX args ) {
            this.command = command;
            this.keepWakelock = keepWakelock;
            this.args = args;
        }
    }

    private class QueueHandler implements Runnable
    {
        @SuppressWarnings( { "UnusedCatchParameter" })
        public void run() {
            try {
                while( !Thread.interrupted() ) {
                    QueueEntry entry;
                    synchronized( queue ) {
                        while( queue.isEmpty() ) {
                            queue.wait();
                        }
                        entry = queue.remove( 0 );
                    }

                    handleIntentCommand( entry.command, entry.keepWakelock, entry.args );
                }
            }
            catch( InterruptedException e ) {
                Log.i( "stopping service thread" );
                synchronized( queue ) {
                    for( QueueEntry entry : queue ) {
                        ServiceTask task = TASK_REGISTRY.get( entry.command );
                        if( task == null ) {
                            Log.e( "missing task for command when saving: " + entry.command );
                        }
                        else {
                            saveTaskForRestart( task.getNotificationTickerText( BuzzManagerService.this, entry.args ).toString(), "Service stopped", entry.command, entry.args );
                        }
                    }
                    queue.clear();
                }
            }
        }
    }
}

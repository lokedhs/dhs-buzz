package org.atari.dhs.buzztest.androidclient.tools;

public interface ProgressDialogExecutorTask<T>
{
    T run() throws BackgroundTaskException;
}

package org.atari.dhs.buzztest.androidclient.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;

import org.atari.dhs.buzztest.androidclient.Log;

public class QueryDialogBox extends AlertDialog
{
    private QueryDialogBox( Context context, OnCancelListener cancelListener ) {
        super( context, true, cancelListener );

        getWindow().setFlags( WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                              WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM );
    }

    public static QueryDialogBox create( Context context,
                                         String title, String text,
                                         String button1Text, final DialogClickListener button1Listener,
                                         String button2Text, final DialogClickListener button2Listener ) {
        final QueryDialogBox dialog = new QueryDialogBox( context, new DialogCancelListener() );
        if( title != null ) {
            dialog.setTitle( title );
        }
        dialog.setMessage( text );
        dialog.setButton( AlertDialog.BUTTON_POSITIVE, button1Text, new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialogInterface, int i ) {
                if( button1Listener != null ) {
                    button1Listener.buttonPressed();
                }
                dialog.dismiss();
            }
        } );
        dialog.setButton( AlertDialog.BUTTON_NEGATIVE, button2Text, new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialogInterface, int i ) {
                if( button2Listener != null ) {
                    button2Listener.buttonPressed();
                }
                dialog.dismiss();
            }
        } );
        return dialog;
    }

    private static class DialogCancelListener implements OnCancelListener
    {
        @Override
        public void onCancel( DialogInterface dialogInterface ) {
            Log.i( "dialog was cancelled" );
        }
    }

    public interface DialogClickListener
    {
        void buttonPressed();
    }
}

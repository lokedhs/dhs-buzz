package org.atari.dhs.buzztest.androidclient.tools;

import java.io.*;

import org.atari.dhs.buzztest.androidclient.Log;

public class SerialisationHelper
{
    private SerialisationHelper() {
        // prevent instantiation
    }

    public static byte[] serialiseObject( Object feed ) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream( out );
            oos.writeObject( feed );
            return out.toByteArray();
        }
        catch( IOException e ) {
            Log.e( "IOException when writing to byte array", e );
            throw new IllegalStateException( e );
        }
    }

    public static Object deserialiseObject( byte[] buf ) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream( buf );
        ObjectInputStream ois = new ObjectInputStream( in );
        return ois.readObject();
    }
}

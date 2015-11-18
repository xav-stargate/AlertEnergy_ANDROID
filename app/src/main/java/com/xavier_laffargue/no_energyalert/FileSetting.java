package com.xavier_laffargue.no_energyalert;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Xavier on 18/11/2015.
 */
public class FileSetting {
    public static String FILENAME = "settingPhoneNumber";


    public String readNumberPhone(Context context)
    {
        byte[] bs = new byte[12];

        String numeroTemp = "";
        try {
            FileInputStream fis = context.openFileInput(FILENAME);
            int i = fis.read(bs);

            for(byte b:bs)
            {
                numeroTemp +=(char)b;
            }

            fis.close();
        } catch(IOException ex) { numeroTemp = "00000000"; }

        return numeroTemp;
    }

    public boolean writeNumberPhone(Context context, String numberPhone)
    {
        try {
            FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(numberPhone.getBytes());
            fos.close();

            return true;
        }
        catch(IOException ex) { return false; }
    }
}

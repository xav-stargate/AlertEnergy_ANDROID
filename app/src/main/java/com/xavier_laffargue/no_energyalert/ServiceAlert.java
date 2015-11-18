package com.xavier_laffargue.no_energyalert;

/**
 * Created by Xavier on 05/10/15.
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ServiceAlert extends Service {

    private int historyPlugged = 0;
    private FileSetting fileSetting;
    public static final String BROADCAST_ACTION = "CHANGE_VALUE_UI";
    public static final String TAG_LOG = "ALERT_SERVICE";

    /**
     * Lancement du service
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG_LOG, "onStartCommand");

        fileSetting = new FileSetting();

        final IntentFilter battChangeFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        this.registerReceiver(this.batteryChangeReceiver, battChangeFilter);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Vérification en continu du branchement ou non du device
     * Si ce n'est pas le cas : Affichage d'un message + sendSms
     */
    private final BroadcastReceiver batteryChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            int plugged;

            //Vérification que le device est branché (USB ou AC peut importe)
            if(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == BatteryManager.BATTERY_PLUGGED_AC ||
                    BatteryManager.BATTERY_PLUGGED_USB == intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                plugged = 1;
            } else {
                plugged = -1;
            }

            SimpleDateFormat dateFormatDDMMYYYY = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
            Intent intentUI = new Intent(BROADCAST_ACTION);

            //Si l'état du branchement a changé
            if(historyPlugged != plugged) {
                if (plugged == 1) {
                    Log.d(TAG_LOG, "Connected");
                    intentUI.putExtra("TEXT", "Branché " + dateFormatDDMMYYYY.format(new Date()));
                } else {
                    Log.d(TAG_LOG, "Alerte");
                    intentUI.putExtra("TEXT", "Panne " + dateFormatDDMMYYYY.format(new Date()));
                    sendSms("Panne électrique depuis " + dateFormatDDMMYYYY.format(new Date()));
                }
                sendBroadcast(intentUI);
                historyPlugged = plugged;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(this.batteryChangeReceiver);
        super.onDestroy();
    }

    /**
     * Envoit un sms au numero inscrit dans le fichier constante FILENAME de SettingActivity
     * @param message Message à envoyer
     */
    private void sendSms(String message) {
        Log.d(TAG_LOG, "Number : " + fileSetting.readNumberPhone(getApplicationContext()));
        SmsManager manager = SmsManager.getDefault();
        manager.sendTextMessage(fileSetting.readNumberPhone(getApplicationContext()), null, message, null, null);
    }

}
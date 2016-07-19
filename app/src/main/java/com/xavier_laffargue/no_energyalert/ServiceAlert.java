package com.xavier_laffargue.no_energyalert;

/**
 * Created by Xavier on 05/10/15.
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.text.format.Time;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ServiceAlert extends Service {

    public JSONParser jsonParser = new JSONParser();
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
                    new CallAPI().execute("work");

                } else {
                    Log.d(TAG_LOG, "Alerte");
                    intentUI.putExtra("TEXT", "Panne " + dateFormatDDMMYYYY.format(new Date()));
                    sendSms("Panne électrique depuis " + dateFormatDDMMYYYY.format(new Date()));
                    new CallAPI().execute("breakdown");


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

    class CallAPI extends AsyncTask<String, String, String> {

        public CallAPI() {
            //set context variables if required
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected String doInBackground(String... paramsF) {
            // Building Parameters   '2016-07-21 10:37:46',
            SimpleDateFormat dateFormatDDMMYYYY = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE);


            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("token_api", fileSetting.readTokenAPI(getApplicationContext())));
            params.add(new BasicNameValuePair("action", paramsF[0]));
            params.add(new BasicNameValuePair("date_event", dateFormatDDMMYYYY.format(new Date())));

            // getting JSON Object
            // Note that create product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest("http://37.187.47.89/noenergy/index.php", "POST", params);

            // check log cat fro response
            Log.d("Create Response", json.toString());

            // check for success tag
            try {
                int success = json.getInt("success");

                if (success == 1) {
                    Log.d(TAG_LOG, "Update statut on webservice success");
                } else {
                    Log.d(TAG_LOG, "Error update statut");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onPostExecute(String result) {
            //Update the UI
        }
    }
}
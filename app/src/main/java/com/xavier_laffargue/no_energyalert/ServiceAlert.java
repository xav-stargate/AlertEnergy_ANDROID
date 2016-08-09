package com.xavier_laffargue.no_energyalert;

/**
 * Created by Xavier on 05/10/15.
 */

import android.app.Activity;
import android.app.PendingIntent;
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
import android.widget.Toast;

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


    private int historyPlugged = 0;
    private FileSetting fileSetting;
    public JSONParser jsonParser = new JSONParser();
    private String urlAPI = "http://37.187.47.89/noenergy/index.php";
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
                    sendSMS(fileSetting.readNumberPhone(getApplicationContext()), "Reprise");
                    intentUI.putExtra("TEXT", "Branché " + dateFormatDDMMYYYY.format(new Date()));
                    new CallAPI().execute("work");

                } else {
                    Log.d(TAG_LOG, "Alerte");
                    sendSMS(fileSetting.readNumberPhone(getApplicationContext()), "Panne");
                    intentUI.putExtra("TEXT", "Panne " + dateFormatDDMMYYYY.format(new Date()));
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
     *
     * @param phoneNumber
     * @param message
     */
    private void sendSMS(String  phoneNumber, String  message)
    {
        String  SENT = "SMS_SENT";
        String  DELIVERED = "SMS_DELIVERED";

        final Intent intentUI = new Intent(BROADCAST_ACTION);

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context  arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        intentUI.putExtra("TEXT", "SMS sent");
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        intentUI.putExtra("TEXT", "Generic failure");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        intentUI.putExtra("TEXT", "No service");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        intentUI.putExtra("TEXT", "Null PDU");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        intentUI.putExtra("TEXT", "Radio off");
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context  arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        intentUI.putExtra("TEXT", "SMS delivered");
                        break;
                    case Activity.RESULT_CANCELED:
                        intentUI.putExtra("TEXT", "SMS not delivered");
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
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

            // check log cat for response
            Log.d("Response", json.toString());

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
package com.xavier_laffargue.no_energyalert;


import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.Date;

public class MainActivity extends ActionBarActivity {

    public static final String TAG_LOG = "ALERT_SERVICE";
    private EditText logEvent;
    private ImageView startImg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        startImg = (ImageView)findViewById(R.id.start);
        logEvent = (EditText)findViewById(R.id.editText);

        event();

        if(!isMyServiceRunning(ServiceAlert.class))
        {
            startImg.setImageResource(R.mipmap.ic_play_arrow_black_48dp);
        }
        else {
            startImg.setImageResource(R.drawable.ic_pause_black_48dp);
        }

        //Pour la modification de l'interface utilisateur par le service (autre Thread)
        registerReceiver(broadcastReceiver, new IntentFilter(ServiceAlert.BROADCAST_ACTION));
    }

    /**
     * Evenement que peut avoir l'appplication
     */
    public void event()
    {
        // Start checking
        startImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date actualDate = new Date();
                Log.d(TAG_LOG, actualDate.toString());

                if(isMyServiceRunning(ServiceAlert.class))
                {
                    stopService(new Intent(MainActivity.this, ServiceAlert.class));
                    startImg.setImageResource(R.mipmap.ic_play_arrow_black_48dp);
                    logEvent.setText(logEvent.getText() + "\nFin : " + actualDate.toString());
                }
                else {

                    logEvent.setText(logEvent.getText() + "\nDebut : " + actualDate.toString());
                    startImg.setImageResource(R.drawable.ic_pause_black_48dp);
                    startService(new Intent(MainActivity.this, ServiceAlert.class));
                }
            }
        });
    }


    /**
     * Vérification si le service est démarré ou non
     * @param serviceClass
     * @return
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mise à jours du blog de log
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            logEvent.setText(logEvent.getText() + "\n" + intent.getStringExtra("TEXT"));
        }
    };

    /**
     * Menu
     * @param menu
     * @return
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_app, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.parametre:
                Intent setting = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(setting);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

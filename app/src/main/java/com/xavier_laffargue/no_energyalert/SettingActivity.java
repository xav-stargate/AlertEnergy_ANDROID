package com.xavier_laffargue.no_energyalert;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class SettingActivity extends ActionBarActivity {

    private EditText numeroTelephone;
    private Button sauvegarder;
    private FileSetting fileSetting;
    public static String FILENAME = "settingPhoneNumber";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        numeroTelephone = (EditText)findViewById(R.id.idTelephone);
        sauvegarder = (Button)findViewById(R.id.idSauvegarder);

        fileSetting = new FileSetting();

        numeroTelephone.setText(fileSetting.readNumberPhone(getApplicationContext()));

        event();
    }


    public void event()
    {
        sauvegarder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fileSetting.writeNumberPhone(getApplicationContext(), numeroTelephone.getText().toString())) {
                    Toast.makeText(getApplicationContext(), "Données sauvegardées", Toast.LENGTH_LONG).show();
                    Intent setting = new Intent(SettingActivity.this, MainActivity.class);
                    startActivity(setting);
                }
                else {
                    Toast.makeText(getApplicationContext(), "Erreur lors de l'enregistrement", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

}

package testing.gps_service;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Button btn_start, btn_stop;
    private TextView textView;
    private BroadcastReceiver broadcastReceiver;


    //aqui preguntamos si existe el BroadcastReceiver , si no existe lo creamos,
    //pasandole el filtro "location_update".
    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    textView.append("\n" +intent.getExtras().get("coordinates"));

                }
            };
        }
        //registramos el receptor creado, y le pasamos el filtro que hace que este
        //solo atento a los intents que le lleguen  con el string "location_update"
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));
    }


    //desregistramos el receptor cuando ya no se necesita.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_start = (Button) findViewById(R.id.button);
        btn_stop = (Button) findViewById(R.id.button2);
        textView = (TextView) findViewById(R.id.textView);


        //si no necesitamos checkear permisos, llamamos al método para habilitar los botones.
        //si es que se necesita pedir permisos al usuario, entonces
        if(!runtime_permissions())
            enable_buttons();

    }


    private void enable_buttons() {
        //arranca el servicio
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i =new Intent(getApplicationContext(),GPS_Service.class);
                startService(i);
            }
        });
        //para el servicio.
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(getApplicationContext(),GPS_Service.class);
                stopService(i);

            }
        });

    }
    //si pedimos permisos, devuelve true, sino false
    private boolean runtime_permissions() {
        //si la version del SDK es mayor de 23 o igual, y no tenemos los permisos necesarios, los pedimos.
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
        //El ultimo valor (100), es un codigo unico para comprobación de permisos.
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return true;
        }
        return false;
    }

 //chequeamos que el requestCode, sea 100, si es así , habilitamos los botones, sino pedimos permiso otra vez.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                enable_buttons();
            }else {
                runtime_permissions();
            }
        }
    }
}

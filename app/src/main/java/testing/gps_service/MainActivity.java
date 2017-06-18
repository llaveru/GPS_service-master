package testing.gps_service;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button btn_start, btn_stop;
    private TextView textView;
    private BroadcastReceiver broadcastReceiver;
    TelephonyManager tMgr;
    String idTelefono;


    //método para obtener la id del teléfono:(emai) hay que añadir el permiso en el manifest.

    private String obtenerIdTelefono() {
        tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        idTelefono = tMgr.getDeviceId();
        Log.d("PRUEBAS", idTelefono);
        return idTelefono;
    }



    //aqui preguntamos si existe el BroadcastReceiver , si no existe lo creamos,
    //y le pasamos el filtro "location_update".para que el receptor solo este "atento", o gestione
    //solamente las retransmisiones que llaven asociado el String "location_update";

    @Override
    protected void onResume() {
        super.onResume();
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {


                    String cadenaRecibida = (String) intent.getExtras().get("coordinates");
                    Log.d("PRUEBA", cadenaRecibida);
                    String[] partes = cadenaRecibida.split(" ");


                    //cuando reciba el cambio de posicion, guardo las coordenadas y la id del telefono en la base de datos.
                    TareaInsertar tarea = new TareaInsertar(MainActivity.this);
                    tarea.execute(partes[0], partes[1], idTelefono);

                }
            };
        }
        //registramos el receptor creado, y le pasamos el filtro que hace que este
        //solo atento a los intents que le lleguen  con el string "location_update"
        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));
    }


    //desregistramos el receptor cuando ya no se necesita.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        idTelefono = obtenerIdTelefono();
        btn_start = (Button) findViewById(R.id.button);
        btn_stop = (Button) findViewById(R.id.button2);
        textView = (TextView) findViewById(R.id.textView);


        //si no necesitamos checkear permisos, llamamos al método para habilitar los botones.
        //si es que se necesita pedir permisos al usuario, entonces
        if (!runtime_permissions())
            enable_buttons();

    }


    private void enable_buttons() {
        //arranca el servicio
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), GPS_Service.class);
                startService(i);

            }
        });
        //para el servicio.
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(getApplicationContext(), GPS_Service.class);
                stopService(i);

                finish();

            }
        });

    }

    //si pedimos permisos, devuelve true, sino false
    private boolean runtime_permissions() {
        //si la version del SDK es mayor de 23 o igual, y no tenemos los permisos necesarios, los pedimos.
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //El ultimo valor (100), es un codigo unico para comprobar que el resultadodeelapeticion es el nuestro
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);

            return true;
        }
        return false;
    }

    //chequeamos que el requestCode, sea 100, si es así, y la respuesta a la solicitud de permisos
    // es buena, (GRANTED), habilitamos los botones, sino pedimos permiso otra vez.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enable_buttons();
            } else {
                runtime_permissions();
            }
        }
    }
}


///CLASE ASYNCRONA PARA INSERTAR LOS DATOS EN LA BASE DE DATOS:
class TareaInsertar extends AsyncTask<String, Void, String> {
    TextView textoUI;
    Context contexto;
    Activity activity;
    TextView tvlat = null;
    TextView tvlon = null;
    String lon = null;
    String lat = null;
    String idTlf = null;

    //necesario crear este constructor, para al instanciar la clase, obtener el contexto de los edittext, que
    //estan en la actividad de la UI, por eso se pasa como parametro la activity

    public TareaInsertar(Activity actividadUI) {
        this.contexto = actividadUI.getApplicationContext();
        this.activity = actividadUI;
        tvlat = (TextView) this.activity.findViewById(R.id.etlat);
        tvlon = (TextView) this.activity.findViewById(R.id.etlon);
    }

    //en preexecute obtenemos los datos de los editText, por eso necesitamos el contexto
    @Override
    protected void onPreExecute() {

        TextView etlat = (TextView) this.activity.findViewById(R.id.etlat);
        TextView etlon = (TextView) this.activity.findViewById(R.id.etlon);


        super.onPreExecute();

    }


    //coloca en el TextView "registro correcto", que es lo que recibe este
    //metodo como parámetro.
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        textoUI = (TextView) this.activity.findViewById(R.id.texto);
        textoUI.setText(s);
        tvlat.setText(lat);
        tvlon.setText(lon);
    }

    @Override
    protected String doInBackground(String... params) {
        //aqui es donde se conecta
        String response = "";
        lat = params[0];
        lon = params[1];
        idTlf = params[2];


        String data = "";
        URL url = null;
        try {
            //la url a la que voy a hacer el POST .
            url = new URL("http://www.motosmieres.com/pruebacongb.php");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }


        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        try {
            //en el php del servidor, debe estar tambien en POST
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        conn.setDoOutput(true);
        OutputStream os = null;
        try {
            os = conn.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = null;

            /*
            EXPLICACION DE URL.Encoder por si interesa

            Utility class for HTML form encoding.
            This class contains static methods for converting a String to the application/x-www-form-urlencoded MIME format.

When encoding a String, the following rules apply:

    The alphanumeric characters "a" through "z", "A" through "Z" and "0" through "9" remain the same.
    The special characters ".", "-", "*", and "_" remain the same.
    The space character "   " is converted into a plus sign "+".
    All other characters are unsafe and are first converted into one or more bytes using some encoding scheme.
    Then each byte is represented by the 3-character string "%xy", where xy is the two-digit hexadecimal representation of the byte.
     The recommended encoding scheme to use is UTF-8.
     However, for compatibility reasons, if an encoding is not specified, then the default encoding of the platform is used.

             */
        try {
            bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

            data = URLEncoder.encode("a", "UTF-8") + "=" + URLEncoder.encode(lon, "UTF-8") + "&" + URLEncoder.encode("b", "UTF-8") + "=" + URLEncoder.encode(lat, "UTF-8")
                    + "&" + URLEncoder.encode("c", "UTF-8") + "=" + (URLEncoder.encode(idTlf, "UTF-8"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        try {

            bw.write(data);
            bw.flush();

            bw.close();

            os.close();
            InputStream is = conn.getInputStream();

            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//esta cadena la recogera el onPostExecute, para utilizarla en la UI.
        return "registro correcto";


    }


}


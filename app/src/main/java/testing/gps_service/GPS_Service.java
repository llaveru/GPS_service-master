package testing.gps_service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * Created by H.Pasarin 6/6/2017.
 */
public class GPS_Service extends Service {
    //declaramos los objetos que necesitamos.
    private LocationListener listener;
    private LocationManager locationManager;


    //debemos sobrescribir el metodo onBind
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //cuando empieza el servicio:
    @Override
    public void onCreate() {

        Toast.makeText(getApplicationContext(),"Comienza geolocalización.",Toast.LENGTH_LONG).show();
            //inicializamos el LocationListener
            listener = new LocationListener() {

                //cuando cambia la posicion, se ejecuta onLocationChanged
                //que nos facilita un objeto Location.
                //los datos del Location los pasamos a la mainActivity con un intent
                //a traves de sus Extras., hay varias formas de hacerlo, esta vez con la
                //clase BroadcastReceiver.

                //este instant se retransmite,. asi que necesitamos crear un intentFilter
                //en la actividad principal, para que esté atenta a esta "retransmision"

                @Override
                public void onLocationChanged(Location location) {
                    //el String "location_update" es el filtro, en la actividad principal debe userse este mismo filtro.
                    //cuando la posicion cambia la retransmitimos por medio de un intent.

                    Intent i = new Intent("location_update");
                    i.putExtra("coordinates",location.getLongitude()+" "+location.getLatitude());
                    sendBroadcast(i);
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                //si los servicios de localizacion, estan deshabilitados en el teléfono, pedimos habilitarlos
                //en este metodo. que lo que hace es mostrar al usuario la pantalla de configuración donde se
                //pueden habilitar.
                @Override
                public void onProviderDisabled(String s) {
                        Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            };

            //inicializamos el locationManager con el metodo getSystemService.
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        //para empezar el rpoceso, llamamos al manager con el metodo requestLocationUpdates, configurandolo
        //para que actualice cada 20 segundos,.
        //noinspection MissingPermission
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,20000,0,listener);

    }
    //cuando se destruye el servicio.
    //para optimizar la memoria, decimos al locationManager, que ya no queremos actualizaciones
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationManager != null){
            Toast.makeText(getApplicationContext(),"Se detiene la geolocalizacion.",Toast.LENGTH_LONG).show();

            locationManager.removeUpdates(listener);
        }
    }
}

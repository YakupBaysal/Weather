package com.example.weather;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    TextView temperature,city,latitude,longitude;
    ImageView weatherImage;

    enum weatherConditions{
        Clouds,
        Mist,
        Rain,
        Clear
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        temperature = (TextView)findViewById(R.id.temperature);
        city = (TextView)findViewById(R.id.city);
        latitude = (TextView)findViewById(R.id.latitude);
        longitude = (TextView)findViewById(R.id.longitude);
        weatherImage = (ImageView) findViewById(R.id.weatherImage);


        mapFragment.getMapAsync(this);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

       locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
       locationListener = new LocationListener() {

           @Override
           public void onLocationChanged(Location location) {

           }

           @Override
           public void onStatusChanged(String s, int i, Bundle bundle) {

           }

           @Override
           public void onProviderEnabled(String s) {

           }

           @Override
           public void onProviderDisabled(String s) {

           }
       };

            LatLng userLastLocation = null;
            if (Build.VERSION.SDK_INT >= 23){
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
                }
                else{
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,500,10,locationListener);

                    Location lastLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
                    System.out.println("lastLocation: " +lastLocation);
                    userLastLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().title("Your Location: " +userLastLocation).position(userLastLocation));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation,10));

                }
            }
            else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,500,10,locationListener);
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                System.out.println("lastLocation: " + lastLocation);
                userLastLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().title("Your Location: " +userLastLocation).position(userLastLocation));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation,10));
            }
            GetWeatherData getWeatherData = new GetWeatherData();
            try{
                String url = "";
                getWeatherData.execute(userLastLocation);

            }catch (Exception e) {

            }

            mMap.setOnMapLongClickListener(this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (grantResults.length > 0){
            if (requestCode == 1){
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,500,10,locationListener);
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.clear();

        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        String address ="";
        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude,1);
            if (addressList != null && addressList.size() > 0) {
                if (addressList.get(0).getThoroughfare() != null){
                    address += addressList.get(0).getThoroughfare();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (address.matches("")) {
            address = "No Address";
        }

        GetWeatherData getWeatherData = new GetWeatherData();
        try{
            String url = "";
            getWeatherData.execute(latLng);
        }catch (Exception e)
        {

        }

        mMap.addMarker(new MarkerOptions().position(latLng).title(address+" Your Location: " +latLng));

    }

    public class GetWeatherData extends AsyncTask<LatLng,Void,String> {

        @Override
        protected String doInBackground(LatLng... latLngs) {
            String result = "";
            URL url;
            HttpURLConnection httpURLConnection;

            try{
                url = new URL("http://api.openweathermap.org/data/2.5/weather?lat="+latLngs[0].latitude+"&lon="+latLngs[0].longitude+"&APPID=f0dbaf60b464c65e210c0b6f609fa79f&units=metric");
                httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStream.read();

                while (data > 0)
                {
                    char character = (char) data;
                    result += character;
                    data = inputStreamReader.read();
                }
                return result;

            }catch (Exception e){
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try{
                WeatherApiResponse weatherApiResponse = new WeatherApiResponse();
                JSONObject jsonObject = new JSONObject(s);
                JSONObject coord = jsonObject.getJSONObject("coord");
                JSONObject mainObject = jsonObject.getJSONObject("main");
                String cityName = jsonObject.getString("name");
                JSONArray weatherArray = jsonObject.getJSONArray("weather");
                JSONObject weatherJSONObject = weatherArray.getJSONObject(0);
                String weather = weatherJSONObject.getString("main");
                String name = "";

                city.setText("Konum : " + cityName);
                temperature.setText("Sıcaklık : " + mainObject.getString("temp"));
                latitude.setText("Enlem : " + coord.getString("lat"));
                longitude.setText("Boylam : " + coord.getString("lon"));


                if (weather.equalsIgnoreCase(weatherConditions.Clouds.toString()))
                    weatherImage.setImageResource(R.drawable.clouds);
                else if (weather.equalsIgnoreCase(weatherConditions.Rain.toString()))
                    weatherImage.setImageResource(R.drawable.rain);
                else if (weather.equalsIgnoreCase(weatherConditions.Mist.toString()))
                    weatherImage.setImageResource(R.drawable.mist);
                else if (weather.equalsIgnoreCase(weatherConditions.Clear.toString()))
                    weatherImage.setImageResource(R.drawable.clear);


            }catch (Exception e){

            }
        }
    }

}

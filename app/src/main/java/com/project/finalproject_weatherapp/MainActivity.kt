package com.project.finalproject_weatherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.location.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.floor

class MainActivity : AppCompatActivity() {
    lateinit var url: String
    var cityName = "Tampere"
    var previousCityName = "Tampere" // backup to go back to if fetch fails
    lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen();
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getWeatherData()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    fun setSearchBar(view:View){
        previousCityName = cityName
        cityName = findViewById<EditText>(R.id.searchEditText).text.toString()
        getWeatherData()
    }

    private fun disableButtons() {
        findViewById<Button>(R.id.updateButton).isEnabled = false
        findViewById<Button>(R.id.updateLocationButton).isEnabled = false
        findViewById<Button>(R.id.forecastButton).isEnabled = false
    }

    private fun enableButtons() {
        findViewById<Button>(R.id.updateButton).isEnabled = true
        findViewById<Button>(R.id.updateLocationButton).isEnabled = true
        findViewById<Button>(R.id.forecastButton).isEnabled = true
    }

    private fun getWeatherData(){
        // Insert own API key here
        val apiKey = ""
        url = "https://api.openweathermap.org/data/2.5/weather?q=${cityName}&units=metric&appid=${apiKey}"
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response -> handleResponse(response)},
            { handleVolleyError()})

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
        disableButtons()
    }

    private fun handleResponse(response: String){
        val weatherObject = JSONObject(response)
        val weatherType = weatherObject.getJSONArray("weather").getJSONObject(0).getString("main")
        val temperature = weatherObject.getJSONObject("main").getDouble("temp")
        val windChill = weatherObject.getJSONObject("main").getDouble("feels_like")
        val windSpeed = weatherObject.getJSONObject("wind").getDouble("speed")
        val windDirection = weatherObject.getJSONObject("wind").getInt("deg")
        val maxTemperature = weatherObject.getJSONObject("main").getDouble("temp_max")
        val minTemperature = weatherObject.getJSONObject("main").getDouble("temp_min")
        val iconName = weatherObject.getJSONArray("weather").getJSONObject(0).getString("icon")

        val direction = degreesToCompass(windDirection)

        findViewById<TextView>(R.id.locationTextView).text = cityName
        findViewById<TextView>(R.id.weatherTextView).text = weatherType
        findViewById<TextView>(R.id.temperatureTextView).text = "${temperature}°C (Feels like ${windChill}°C)"
        findViewById<TextView>(R.id.windspeedTextView).text = "$windSpeed m/s (${direction})"
        findViewById<TextView>(R.id.tempspanTextView).text = "${maxTemperature}°C to ${minTemperature}°C"

        // Icon
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        val weatherIconView = findViewById<ImageView>(R.id.weatherIcon)
        weatherIconView.setImageDrawable(null) // clear previous icon
        executor.execute {

            val imageURL = "https://openweathermap.org/img/wn/$iconName.png"
            try {
                val `in` = java.net.URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                // Only for making changes in UI
                handler.post {
                    weatherIconView.setImageBitmap(image)
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Add sneaky delay to feedback because the fetch is too fast to demonstrate the state change
        Handler(Looper.getMainLooper()).postDelayed(
            {
                // Re-enable buttons
                enableButtons()
            },
            300
        )
    }

    private fun degreesToCompass(windDirection: Int): String {
        val value = floor((windDirection/45)+0.5)
        val directions = arrayOf("↑ N", "↗ NE", "→ E", "↘ SE", "↓ S", "↙ SW", "← W", "↖ NW")
        return directions[(value % 8).toInt()]
    }

    private fun handleVolleyError(){
        Toast.makeText(this, "City not found.\nPlease ensure you typed it correctly.", Toast.LENGTH_LONG).show()
        cityName = previousCityName // roll back to previous city
        Handler(Looper.getMainLooper()).postDelayed(
            { enableButtons() },
            200
        )
    }

    fun showForecast(view: View) {
        val intent = Intent(this, ForecastActivity::class.java)
        intent.putExtra("cityName",cityName)
        startActivity(intent)
    }

    // TODO: call again after permission is granted so user doesn't have to press button twice
    fun getLocationWeatherData(view: View) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0)
            return
        }

        val mLocationRequest: LocationRequest = LocationRequest.create()
        mLocationRequest.interval = 60000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val mLocationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    return
                }
            }
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude =  location.latitude
                    val longitude = location.longitude
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val city = addresses[0].locality
                    cityName = city
                    getWeatherData()
                } else {
                    Toast.makeText(this, "Location not available.\nPlease turn on your Location Information.", Toast.LENGTH_LONG).show()
                }
            }
    }
}
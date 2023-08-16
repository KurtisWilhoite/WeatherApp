package com.project.finalproject_weatherapp
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject


class ForecastActivity : AppCompatActivity() {
    // Insert own API key here
    val apiKey = ""
    var url ="https://api.openweathermap.org/data/2.5/forecast?q=Tampere&units=metric&appid=${apiKey}"
    lateinit var cityName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        cityName = intent.getStringExtra("cityName").toString()
        url = "https://api.openweathermap.org/data/2.5/forecast?q=${cityName}&units=metric&appid=${apiKey}"
        getWeatherForecast()
    }

    fun getWeatherForecast(){
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response -> handleResponse(response)},
            { handleVolleyError()})

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
    }

    private fun handleResponse(response: String){
        val rootJsonObject = JSONObject(response)
        val forecastList = rootJsonObject.getJSONArray("list")
        for(i in 0 until forecastList.length()){
            val forecastListItem: JSONObject = forecastList.getJSONObject(i)
            val temperature: Int = forecastListItem.getJSONObject("main").getDouble("temp").toInt()
            val description: String = forecastListItem.getJSONArray("weather").getJSONObject(0).getString("main").toString()
            var time: String = forecastListItem.getString("dt_txt")
            time = time.drop(5)
            time = time.dropLast(3)

            findViewById<TextView>(R.id.titleRow).text = "Date & Time   Weather"
            findViewById<TextView>(R.id.locationTextView).text = "$cityName Forecast"
            findViewById<TextView>(R.id.forecastListTextView).append("$time    $description $temperature°C\n")
        }
    }

    private fun handleVolleyError(){
        Toast.makeText(this, "City not found.\nPlease ensure you typed it correctly.", Toast.LENGTH_LONG).show()
    }

    fun endActivity(view: View) {
        finish()
    }
}
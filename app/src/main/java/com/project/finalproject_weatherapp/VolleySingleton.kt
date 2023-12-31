package com.project.finalproject_weatherapp
import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class VolleySingleton constructor(context: Context){
    companion object{
        @Volatile
        private var INSTANCE: VolleySingleton? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this){
                INSTANCE ?: VolleySingleton(context).also{
                    INSTANCE = it
                }
            }
    }

    val requestQueue: RequestQueue by lazy{
        Volley.newRequestQueue(context.applicationContext)
    }

    fun addToRequestQueue(req: Request<String>){
        requestQueue.add(req)
    }
}
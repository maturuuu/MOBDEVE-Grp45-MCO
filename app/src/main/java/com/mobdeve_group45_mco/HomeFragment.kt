package com.mobdeve_group45_mco

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve_group45_mco.api.ApiCall
import com.mobdeve_group45_mco.dailyWeather.DailyAdapter
import com.mobdeve_group45_mco.databinding.FragmentHomeBinding
import com.mobdeve_group45_mco.forecast.Forecast
import com.mobdeve_group45_mco.hourlyWeather.HourlyAdapter
import com.mobdeve_group45_mco.post.PostAdapter
import com.mobdeve_group45_mco.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {

    private lateinit var viewBinding: FragmentHomeBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val postList = ArrayList<Post>()

    // Default coordinates for Manila, Philippines
    private val defaultLatitude = 14.599512
    private val defaultLongitude = 120.984222

    private var currentForecast: Forecast? = null

    //initialize the new PostManager
    private var postManager = PostManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    private fun handleForecastData(forecast: Forecast) {
        currentForecast = forecast
        Log.i("Hello!", forecast.current.isDay.toString())
        if (forecast.current.isDay == 0) {
            viewBinding.fragmentHomeFrameLayout.setBackgroundResource(R.drawable.anime_style_clouds_night)
        }

        // Bind the forecast data to the views
        viewBinding.fragmentHomeFrameLayout
        viewBinding.fragmentHomeTvConditions.text = Utils.getWeatherDescription(forecast.current.weatherCode)
        viewBinding.fragmentHomeRvHours.adapter = HourlyAdapter(forecast.hourly)
        viewBinding.fragmentHomeRvHours.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        viewBinding.fragmentHomeRvDays.adapter = DailyAdapter(forecast.daily)
        viewBinding.fragmentHomeRvDays.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.fragmentHomeRvPosts.adapter = PostAdapter(postList)
        viewBinding.fragmentHomeRvPosts.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.fragmentHomeTvTemperature.text = "${forecast.current.temperature}°"
        viewBinding.fragmentHomeTvCity.text = forecast.location.name
        val dividerItemDecoration = DividerItemDecoration(
            viewBinding.fragmentHomeRvPosts.context,
            LinearLayoutManager.VERTICAL
        )
        viewBinding.fragmentHomeRvPosts.addItemDecoration(dividerItemDecoration)

        // Fetch posts related to the city
        fetchPosts(forecast)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchPosts(forecast: Forecast) {
        //old post fetching logic
        // db.collection("posts")
        //     .get()
        //     .addOnSuccessListener { documents ->
        //         postList.clear()
        //         for (document in documents) {
        //             if (document.getString("city") == forecast.location.name &&
        //                 document.getString("countryCode") == forecast.location.country_code
        //             ) {
        //                 val post = document.toObject(Post::class.java)
        //                 postList.add(post)
        //             }
        //         }
        //         viewBinding.fragmentHomeRvPosts.adapter?.notifyDataSetChanged()
        //     }
        //     .addOnFailureListener { exception ->
        //         Log.e("FirestoreError", "Error fetching posts: ${exception.message}")
        //     }

        //new post fetch using PostManager
        postManager.getPosts(forecast.location.name, forecast.location.country_code,
            onSuccess = { posts ->
                postList.clear()
                postList.addAll(posts)
                viewBinding.fragmentHomeRvPosts.adapter?.notifyDataSetChanged()
            },
            onFailure = { errorMessage ->
                Log.e("FirestoreError", errorMessage)
            }
        )
    }


    private fun getCoordinatesFromSharedPreferences(): Pair<Double, Double> {
        // Retrieve the saved JSON string from SharedPreferences
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("ForecastPrefs", Context.MODE_PRIVATE)
        val forecastJson = sharedPreferences.getString("saved_forecast", null)
        Log.i("Hello", "Hello")
        if (forecastJson == null) {
            // If the JSON is null (i.e., no saved forecast), fetch the current location
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

            fetchCurrentLocation { latitude, longitude ->
                // Use the fetched coordinates to make an API call
                ApiCall().getForecast(requireContext(), { forecast ->
                    handleForecastData(forecast)
                }, latitude, longitude)
            }

            // Return default coordinates while the location fetch is asynchronous
            return Pair(defaultLatitude, defaultLongitude)
        } else {
            // Deserialize the JSON string into a Forecast object
            val forecast = Utils.deserializeForecast(forecastJson)

            // Extract latitude and longitude from the Forecast object
            val latitude = forecast.location.latitude ?: defaultLatitude
            val longitude = forecast.location.longitude ?: defaultLongitude

            return Pair(latitude, longitude)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun callback(forecast: Forecast) {
        currentForecast = forecast
        Log.i("Hello!", forecast.current.isDay.toString())
        if (forecast.current.isDay == 0) {
            viewBinding.fragmentHomeFrameLayout.setBackgroundResource(R.drawable.anime_style_clouds_night)

            // Set text color to white for the TextViews
            viewBinding.fragmentHomeTvForecast.setTextColor(Color.WHITE)
            viewBinding.fragmentHomeTvCity.setTextColor(Color.WHITE)
            viewBinding.fragmentHomeTvTemperature.setTextColor(Color.WHITE)
            viewBinding.fragmentHomeTvConditions.setTextColor(Color.WHITE)
        }

        // Bind the forecast data to the views
        viewBinding.fragmentHomeTvConditions.text = Utils.getWeatherDescription(forecast.current.weatherCode)
        viewBinding.fragmentHomeRvHours.adapter = HourlyAdapter(forecast.hourly)
        viewBinding.fragmentHomeRvHours.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        viewBinding.fragmentHomeRvDays.adapter = DailyAdapter(forecast.daily)
        viewBinding.fragmentHomeRvDays.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.fragmentHomeRvPosts.adapter = PostAdapter(postList)
        viewBinding.fragmentHomeRvPosts.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.fragmentHomeTvTemperature.text = "${forecast.current.temperature}°"
        viewBinding.fragmentHomeTvCity.text = forecast.location.name
        val dividerItemDecoration = DividerItemDecoration(viewBinding.fragmentHomeRvPosts.context, LinearLayoutManager.VERTICAL)
        viewBinding.fragmentHomeRvPosts.addItemDecoration(dividerItemDecoration)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve coordinates from SharedPreferences or use default coordinates
        val (latitude, longitude) = getCoordinatesFromSharedPreferences()

        // Make the API call using the retrieved latitude and longitude
        ApiCall().getForecast(requireContext(), { forecast ->
            try {
                db.collection("posts")
                    .get()
                    .addOnSuccessListener { documents ->
                        postList.clear() // Clear existing data before adding new ones
                        for (document in documents) {
                            if (document.getString("city") == forecast.location.name &&
                                document.getString("countryCode") == forecast.location.country_code) {
                                val post = document.toObject(Post::class.java)
                                postList.add(post)
                            }
                        }
                        viewBinding.fragmentHomeRvPosts.adapter?.notifyDataSetChanged()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirestoreError", "Error fetching posts: ${exception.message}")
                    }

                callback(forecast)

                // Add listener for Create Post button
                viewBinding.btnCreatePost.setOnClickListener {
                    val intent = Intent(requireContext(), CreatePost::class.java)

                    intent.putExtra("CITY", forecast.location.name)
                    intent.putExtra("COUNTRY_CODE", forecast.location.country_code)

                    startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("CallbackError", "An error occurred in the callback: ${e.message}")
            }
        }, latitude, longitude)  // Pass the coordinates to the API call
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation(onLocationFetched: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions if not granted
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1001
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                onLocationFetched(location.latitude, location.longitude)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Unable to fetch current location. Using default coordinates.",
                    Toast.LENGTH_SHORT
                ).show()
                onLocationFetched(defaultLatitude, defaultLongitude)
            }
        }.addOnFailureListener {
            Log.e("LocationError", "Failed to fetch location: ${it.message}")
            onLocationFetched(defaultLatitude, defaultLongitude)
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        // Retrieve coordinates from SharedPreferences or use default coordinates
        val (latitude, longitude) = getCoordinatesFromSharedPreferences()

        // Make the API call using the retrieved latitude and longitude
        ApiCall().getForecast(requireContext(), { forecast ->
            try {
                db.collection("posts")
                    .get()
                    .addOnSuccessListener { documents ->
                        postList.clear() // Clear existing data before adding new ones
                        for (document in documents) {
                            if (document.getString("city") == forecast.location.name &&
                                document.getString("countryCode") == forecast.location.country_code) {
                                val post = document.toObject(Post::class.java)
                                postList.add(post)
                            }
                        }
                        viewBinding.fragmentHomeRvPosts.adapter?.notifyDataSetChanged()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirestoreError", "Error fetching posts: ${exception.message}")
                    }

                callback(forecast)

                // Add listener for Create Post button
                viewBinding.btnCreatePost.setOnClickListener {
                    val intent = Intent(requireContext(), CreatePost::class.java)

                    intent.putExtra("CITY", forecast.location.name)
                    intent.putExtra("COUNTRY_CODE", forecast.location.country_code)

                    startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("CallbackError", "An error occurred in the callback: ${e.message}")
            }
        }, latitude, longitude)  // Pass the coordinates to the API call
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
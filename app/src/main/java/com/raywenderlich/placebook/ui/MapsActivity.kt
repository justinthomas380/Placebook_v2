package com.raywenderlich.placebook.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.adapter.BookmarkInfoWindowAdapter
import com.raywenderlich.placebook.viewmodel.MapsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    //This is a lazy delegate and creates the viewModel only once after the Activity is created.
    //If a configuration change occurs this will return the previously created MapsViewModel.
    private val mapsViewModel by viewModels<MapsViewModel>()

    // When calling the getMapAsync, SupportMapFragment object handles the setup the map and creating
    // the GoogleMap object. This will be used to control and query the map.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupLocationClient()
        setupPlacesClient()
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupMapListeners()
        getCurrentLocation()
        createBookmarkMarkerObserver()
    }

    //
    private fun setupMapListeners(){
        map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        // This lambda is called when the user clicks a point of interest.
        map.setOnPoiClickListener {
            displayPoi(it)
        }
        map.setOnInfoWindowClickListener {
            handleInfoWindowClick(it)
        }
    }

    //Creates the places client. The API key and the object context must be sent with each API call.
    //The applicationContext is used when passing a context to objects that have a long lifecycle.
    //This has the benefit of not holding a reference thus preventing memory leaks
    private fun setupPlacesClient() {
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }else{
                Log.e(TAG, "Location permission denied")
            }
        }
    }

    private fun setupLocationClient(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    // Passes the current activity as the context and an array of requested permissions,
    // and a requestCode to identify the request.
    private fun requestLocationPermissions(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION
        )
    }

    companion object{
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }


    // Checks user permissions then gets the users current location and center the map at
    // that location
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
        } else{
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnCompleteListener{
                val location =it.result
                if(location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    map.moveCamera(update)
                }else{
                 Log.e(TAG, "No location found")
                }
            }
        }
    }


    private fun displayPoi(pointOfInterest: PointOfInterest) {
        displayPoiGetPlaceStep(pointOfInterest)
    }
    // First the placeId is retrieved. A field mask is created that contains place the attributes.
    // The two objects are used to to create a fetch request using the builder.
    // The details are fetched using placesClient.
    // If the response is valid the onSuccessListener
    // shows the message containing the object details.
    // If not the exception callback shows ta messages containing the error code
    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        val placeId = pointOfInterest.placeId
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            displayPoiGetPhotoStep(place)
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                val statusCode = exception.statusCode
                Log.e(
                    TAG, "Place not found: " + exception.message + ", " +
                            "statusCode: " + statusCode
                )
            }
        }
    }

    //Gets the photo limiting the width to 480px and height to 270px. Then handles exceptions.
    private fun displayPoiGetPhotoStep(place: Place) {
        val photoMetadata = place.photoMetadatas?.get(0)
        if(photoMetadata == null) {displayPoiDisplayStep(place, null)}
        val photoRequest = FetchPhotoRequest.builder(photoMetadata).setMaxWidth(
                resources.getDimensionPixelSize(R.dimen.default_image_width))
                .setMaxHeight(resources.getDimensionPixelSize(R.dimen.default_image_height)).
                build()
        placesClient.fetchPhoto(photoRequest).addOnSuccessListener {fetchPhotoResponse ->
            val bitmap = fetchPhotoResponse.bitmap
            displayPoiDisplayStep(place, bitmap)
        }.addOnFailureListener { exception ->
            if(exception is ApiException) {
                val statusCode = exception.statusCode
                Log.e(TAG, "Place not found " + exception.message + ", " + statusCode)
            }
        }
    }

    //Places marker with place object attributes
    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
        val marker = map.addMarker(MarkerOptions()
            .position(place.latLng as LatLng)
            .title(place.name)
            .snippet(place.phoneNumber)
        )
        marker?.tag = PlaceInfo(place, photo)
    }

    //Defines PlaceInfo to hold a Place and Bitmap. pg 346 for more info
    class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)

    //Handles taps on the place info window.
    // If the marker.tag is not null the place is added to the repo.
    // Then the marker is removed.
    private fun handleInfoWindowClick(marker: Marker) {
        val placeInfo = (marker.tag as PlaceInfo)
        if(placeInfo.place != null) {
            GlobalScope.launch{
                mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
            }
        }
        marker.remove()
    }

    //Adds a blue marker
    private fun addPlaceMarker(bookmark: MapsViewModel.BookmarkMarkerView): Marker? {
        val marker = map.addMarker(MarkerOptions().position(bookmark.location)
            .icon(BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_AZURE)).alpha(0.8f))
        marker?.tag = bookmark
        return marker
    }

    //Displays all bookmark markers
    private fun displayAllBookmarks(bookmarks: List<MapsViewModel.BookmarkMarkerView>) {
        bookmarks.forEach {addPlaceMarker(it)}
    }

    //Updates the view with the changes to BookmarkMarkerView
    private fun createBookmarkMarkerObserver() {
        mapsViewModel.getBookmarkMarkerViews()?.observe(this,{
            map.clear()
            it?.let{
                displayAllBookmarks(it)
            }
        })
    }
}
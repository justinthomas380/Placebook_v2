package com.raywenderlich.placebook.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.view.animation.Transformation
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.Transformations.map
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.repository.BookmarkRepo

//Creates the ViewModel and inherits from AndroidViewModel
class MapsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MapsViewModel"
    private var bookmarks: LiveData<List<BookmarkMarkerView>>? = null
    //Creates a BookmarkRepo object
    private val bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())

    //Creates a bookmark from a place identified by the user
    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id ?: ""
        bookmark.name = place.name?.toString() ?: ""
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber?.toString() ?: ""
        bookmark.address = place.address?.toString() ?: ""
        //image.also { bookmark.image = it }

        //Saves the Bookmark and display message.
        val newId = bookmarkRepo.addBookmark(bookmark)
        Log.i(TAG, "New bookmark $newId added to the database")
    }


    data class BookmarkMarkerView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0)
    )

    //Converts a Bookmark object from the repo into a BookmarkMarkerView object.
    private fun bookmarkToMarkerView(bookmark: Bookmark) =
        BookmarkMarkerView(bookmark.id, LatLng(bookmark.latitude, bookmark.longitude))

    //Maps the LiveData list objects
    private fun mapBookmarksToMarkerView() {
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks) {
            repoBookmarks ->
                repoBookmarks.map {bookmark -> bookmarkToMarkerView(bookmark)}
        }
    }

    //Returns the LiveData object that will be observed by MapsActivity.
    fun getBookmarkMarkerViews() : LiveData<List<BookmarkMarkerView>>? {
        if(bookmarks == null) {
            mapBookmarksToMarkerView()
        }
        return bookmarks
    }
}
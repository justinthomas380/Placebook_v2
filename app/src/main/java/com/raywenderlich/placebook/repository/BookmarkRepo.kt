package com.raywenderlich.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.raywenderlich.placebook.db.BookmarkDao
import com.raywenderlich.placebook.db.PlaceBookDatabase
import com.raywenderlich.placebook.model.Bookmark

//Defines the BookmarkRepo class.
class BookmarkRepo(context: Context) {
    private val db = PlaceBookDatabase.getInstance(context)
    private val bookmarkDao: BookmarkDao = db.bookmarkDao()

    //defines the two data sorces that BookmarkRepo will use.
    fun addBookmark(bookmark: Bookmark): Long? {
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId
        return newId
    }

    //Allows a single bookmark to be added to the repo.
    fun createBookmark(): Bookmark {
        return Bookmark()
    }

    //Creates property that returns LiveData list of the bookmarks in the repo.
    val allBookmarks: LiveData<List<Bookmark>> get(){
            return bookmarkDao.loadAll()
        }
}
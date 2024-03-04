package com.raywenderlich.placebook.model

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

//The @Entity annotation tells room that this is a database entity class.
//When a table name is not used the default is the class name.
@Entity
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    var placeId: String = "",
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var phone: String = ""//,
    //var image: Bitmap? = null
)

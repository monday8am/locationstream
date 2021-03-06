package com.monday8am.locationstream.redux

import android.util.Log
import com.monday8am.locationstream.LocationApp
import com.monday8am.locationstream.data.Photo
import com.monday8am.locationstream.data.UserLocation
import io.reactivex.disposables.Disposable
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware


private var savePhotoDisposable: Disposable? = null
private var getImageDisposable: Disposable? = null

internal val loggingMiddleware: Middleware<AppState> = { _, _ ->
    { next ->
        { action ->
            Log.d("New Action dispatched:", Thread.currentThread().id.toString() + "-" + action.toString())
            next(action)
        }
    }
}

internal val networkMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is NewLocationDetected -> {
                    val lastLocation = state()?.lastLocationSaved
                    if (action.location.isUseful(lastLocation = lastLocation)) {
                        savePhotoForLocation(action.location, dispatch)
                    }
                }
                is AddNewPhoto -> {
                    getImageForLocation(action.photo, dispatch)
                }
                is StartStopUpdating -> LocationApp.repository?.setRequestingLocation(action.isUpdating)
            }
            next(action)
        }
    }
}

fun savePhotoForLocation(location: UserLocation, dispatch: DispatchFunction) {
    savePhotoDisposable = LocationApp.repository?.addPhotoFromLocation(location = location)
                                                ?.subscribe { photo ->
                                                    if (photo != null) {
                                                        dispatch(AddNewPhoto(photo, location))
                                                    }
                                                }
}

fun getImageForLocation(photo: Photo, dispatch: DispatchFunction) {
    val repo = LocationApp.repository
    if (repo != null) {
        getImageDisposable = repo
            .getRemoteImageFor(longitude = photo.longitude, latitude = photo.latitude)
            .subscribe { imageUrl ->
                repo.updatePhotoWithImage(photo, imageUrl)
                    .subscribe {
                        val updated = photo.copy(imageUrl = imageUrl)
                        dispatch(UpdatePhotoWithImage(updated))
                    }
            }
    }
}

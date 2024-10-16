package com.gs.wialonlocal.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import cocoapods.GoogleMaps.GMSAdvancedMarker.Companion.markerImageWithColor
import cocoapods.GoogleMaps.GMSCameraPosition
import cocoapods.GoogleMaps.GMSCameraUpdate
import cocoapods.GoogleMaps.GMSCircle
import cocoapods.GoogleMaps.GMSCoordinateBounds
import cocoapods.GoogleMaps.GMSFeatureLayer
import cocoapods.GoogleMaps.GMSMapStyle
import cocoapods.GoogleMaps.GMSMapView
import cocoapods.GoogleMaps.GMSMapViewDelegateProtocol
import cocoapods.GoogleMaps.GMSMapViewOptions
import cocoapods.GoogleMaps.GMSMapViewType
import cocoapods.GoogleMaps.GMSMarker
import cocoapods.GoogleMaps.GMSMutablePath
import cocoapods.GoogleMaps.GMSPolyline
import cocoapods.GoogleMaps.animateWithCameraUpdate
import cocoapods.GoogleMaps.kGMSTypeHybrid
import cocoapods.GoogleMaps.kGMSTypeNormal
import cocoapods.GoogleMaps.kGMSTypeSatellite
import com.gs.wialonlocal.core.network.AppHttpClient
import com.gs.wialonlocal.features.geofence.data.entity.geofence.P
import com.gs.wialonlocal.features.monitoring.data.entity.history.Trip
import com.gs.wialonlocal.features.monitoring.domain.model.UnitModel
import com.gs.wialonlocal.features.settings.data.settings.MapType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationDegrees
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.MapKit.MKMapView
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.darwin.NSObject
import kotlin.coroutines.CoroutineContext
import platform.CoreGraphics.*
import platform.UIKit.UIColor.Companion.blueColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext

// Coroutine scope for main thread operations
val uiScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

val client = AppHttpClient

// A list to keep track of markers
@OptIn(ExperimentalForeignApi::class)
val markers = mutableListOf<GMSMarker>()

@OptIn(ExperimentalForeignApi::class)
fun clearMarkers() {
    // Remove all existing markers from the map
    markers.forEach { marker ->
        marker.map = null // This will remove the marker from the map
    }
    markers.clear() // Clear the list after removing the markers
}

@OptIn(ExperimentalForeignApi::class)
fun resizeImage(image: UIImage, targetWidth: Double, targetHeight: Double): UIImage? {
    val newSize = CGSizeMake(targetWidth, targetHeight)
    UIGraphicsBeginImageContextWithOptions(newSize, false, 0.0)

    image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return resizedImage
}

@OptIn(ExperimentalForeignApi::class)
suspend fun downloadImage(url: String, targetWidth: Double, targetHeight: Double): UIImage? {
    return try {
        val response = client.get(url) // Download the image data
        val byteArray = response.body<ByteArray>()
        val nsData = byteArray.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), byteArray.size.toULong())
        }
        val image = UIImage.imageWithData(nsData)  // Convert NSData to UIImage
        image?.let { resizeImage(it, targetWidth, targetHeight) }  // Resize the image
    } catch (e: Exception) {
        println("Failed to download image: ${e.message}")
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
fun setMarkerIconFromUrlKtor(
    urlString: String,
    latitude: Double,
    longitude: Double,
    mapView: GMSMapView,
    title: String
) {
    uiScope.launch {
        val image = downloadImage(urlString, 30.0,30.0)  // Download image using Ktor
        if (image != null) {
            // Set up the marker with the downloaded image
            val position = CLLocationCoordinate2DMake(latitude = latitude, longitude = longitude)
            val marker = GMSMarker()

            marker.position = position
            marker.title = title
            marker.icon = image
            marker.map = mapView

            // Add the new marker to the list
            markers.add(marker)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GoogleMaps(
    modifier: Modifier,
    cameraPosition: CameraPosition,
    units: List<UnitModel>,
    geofences: Map<String, List<P>>,
    onUnitClick: (UnitModel) -> Unit,
    mapType: MapType,
    polyline: List<String?>?,
    singleMarker: LatLong?
) {
    val googleMapView = rememberSaveable {
        GMSMapView().apply {
            setMyLocationEnabled(true)
            settings.myLocationButton = true
            settings.setMyLocationButton(true)
            settings.setScrollGestures(true)
            settings.setZoomGestures(true)
            settings.setCompassButton(true)
            this.setMapStyle(
                GMSMapStyle.styleWithJSONString(
                    mapStyle1(),
                    error = null
                )
            )
        }
    }

    LaunchedEffect(true) {
        googleMapView.camera = GMSCameraPosition.cameraWithTarget(
            CLLocationCoordinate2DMake(latitude = cameraPosition.target.latitude, longitude = cameraPosition.target.longitude),
            cameraPosition.zoom
        )
    }

    LaunchedEffect(mapType) {
        when(mapType) {
            MapType.SATELLITE -> {
                googleMapView.mapType = kGMSTypeSatellite
            }
            MapType.HYBRID -> {
                googleMapView.mapType = kGMSTypeHybrid
            }
            else -> {
                googleMapView.mapType = kGMSTypeNormal
            }
        }
    }

    LaunchedEffect(units) {
        clearMarkers() // Remove old markers
        units.forEachIndexed { index, unitModel ->
            setMarkerIconFromUrlKtor(
                urlString = unitModel.image,
                latitude = unitModel.latitude,
                longitude = unitModel.longitude,
                mapView = googleMapView, // Your GMSMapView instance
                title = unitModel.carNumber
            )

            val path = GMSMutablePath()
            // Gradient colors
            val startColor = Color.Gray
            val endColor = Color.Green

            unitModel.trips.forEach {
                path.addLatitude(it.latitude, it.longitude)
            }

            val poly = GMSPolyline.polylineWithPath(path)
            poly.strokeWidth = 10.0
            poly.geodesic = true
            poly.strokeColor = blueColor
            poly.map = googleMapView


        }
    }

    // Only used to track selected marker
//    val mapMarkers = remember(units) { mutableStateMapOf<String,GMSMarker>() }
//    var selectedMarker by remember(googleMapView.selectedMarker) { mutableStateOf(googleMapView.selectedMarker) }
//    val delegate = remember { object : NSObject(), GMSMapViewDelegateProtocol {
//
//        // Note: this shows an error, but it compiles and runs fine(!)
//        override fun mapView(
//            mapView: GMSMapView,
//            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")  // found this hacky fix was found on jetbrains site
//            didTapInfoWindowOfMarker: GMSMarker
//        ) {
//            val userData = didTapInfoWindowOfMarker.userData()
//        }
//
//    }}

    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            googleMapView.apply {
//                setDelegate(delegate)
//                this.selectedMarker = selectedMarker
                this.setMyLocationEnabled(true)
            }

            googleMapView
        },
        update = { view->

        },
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true
        )
    )
}
// https://mapstyle.withgoogle.com/
fun mapStyle1(): String {
    return """
    [
  {
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#242f3e"
      }
    ]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#746855"
      }
    ]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [
      {
        "color": "#242f3e"
      }
    ]
  },
  {
    "featureType": "administrative.land_parcel",
    "elementType": "labels",
    "stylers": [
      {
        "visibility": "off"
      }
    ]
  },
  {
    "featureType": "administrative.locality",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#d59563"
      }
    ]
  },
  {
    "featureType": "poi",
    "elementType": "labels.text",
    "stylers": [
      {
        "visibility": "off"
      }
    ]
  },
  {
    "featureType": "poi",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#d59563"
      }
    ]
  },
  {
    "featureType": "poi.business",
    "stylers": [
      {
        "visibility": "off"
      }
    ]
  },
  {
    "featureType": "poi.park",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#263c3f"
      }
    ]
  },
  {
    "featureType": "poi.park",
    "elementType": "labels.text",
    "stylers": [
      {
        "visibility": "off"
      }
    ]
  },
  {
    "featureType": "poi.park",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#6b9a76"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#38414e"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry.stroke",
    "stylers": [
      {
        "color": "#212a37"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#9ca5b3"
      }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#746855"
      }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry.stroke",
    "stylers": [
      {
        "color": "#1f2835"
      }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#f3d19c"
      }
    ]
  },
  {
    "featureType": "road.highway.controlled_access",
    "stylers": [
      {
        "visibility": "simplified"
      }
    ]
  },
  {
    "featureType": "road.highway.controlled_access",
    "elementType": "geometry",
    "stylers": [
      {
        "visibility": "simplified"
      }
    ]
  },
  {
    "featureType": "road.highway.controlled_access",
    "elementType": "labels",
    "stylers": [
      {
        "visibility": "on"
      }
    ]
  },
  {
    "featureType": "road.local",
    "elementType": "labels",
    "stylers": [
      {
        "visibility": "off"
      }
    ]
  },
  {
    "featureType": "transit",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#2f3948"
      }
    ]
  },
  {
    "featureType": "transit.station",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#d59563"
      }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#17263c"
      }
    ]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#515c6d"
      }
    ]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.stroke",
    "stylers": [
      {
        "color": "#17263c"
      }
    ]
  }
]
    """.trimIndent()

}
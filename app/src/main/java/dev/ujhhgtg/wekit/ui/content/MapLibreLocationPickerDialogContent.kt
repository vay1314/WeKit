package dev.ujhhgtg.wekit.ui.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Check
import com.composables.icons.materialsymbols.outlined.Close
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import kotlin.math.atan2
import kotlin.math.sqrt

// Default OpenStreetMap Raster Style JSON to emulate Osmdroid's MAPNIK
const val DEFAULT_OSM_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256
    }
  },
  "layers": [{
    "id": "osm",
    "type": "raster",
    "source": "osm"
  }]
}
"""

/**
 * A dialog wrapping a MapLibre [MapView].
 * The user taps anywhere on the map to drop a pin; tapping Confirm fires
 * [onLocationSelected] with the chosen [LatLng].
 *
 * @param initialLocation    Camera target on open (defaults to Shanghai).
 * @param initialZoom        Initial zoom level (defaults to 5.0).
 * @param styleJson          Map style JSON (defaults to OSM raster tiles).
 * @param onLocationSelected Called with the confirmed [LatLng]; dismiss here.
 * @param onDismiss          Called when the user cancels.
 */
@Composable
fun MapLibreLocationPickerDialogContent(
    initialLocation: LatLng = LatLng(31.224361, 121.469170), // Shanghai
    initialZoom: Double = 5.0,
    styleJson: String = DEFAULT_OSM_STYLE_JSON,
    onLocationSelected: (LatLng) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // MapLibre requires initialization before creating the MapView
    SideEffect {
        MapLibre.getInstance(context)
    }

    var pickedPoint by remember { mutableStateOf<LatLng?>(null) }

    // Create and bind MapView to Compose Lifecycle safely
    val mapView = rememberMapViewWithLifecycle()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction = 0.9f),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────
            MapPickerHeader(
                pickedPoint = pickedPoint,
                onDismiss = onDismiss,
                onConfirm = { pickedPoint?.let(onLocationSelected) },
            )

            HorizontalDivider()

            // ── Map ───────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f).clipToBounds()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        mapView.apply {
                            getMapAsync { map ->
                                map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                                    // 1. Add a generated pin image to the style
                                    style.addImage("marker-icon", createDefaultMarkerBitmap(context))

                                    // 2. Add an empty GeoJsonSource for our dropped pin
                                    style.addSource(GeoJsonSource("marker-source"))

                                    // 3. Render the marker at the source coordinate
                                    style.addLayer(
                                        SymbolLayer("marker-layer", "marker-source").withProperties(
                                            PropertyFactory.iconImage("marker-icon"),
                                            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                                            PropertyFactory.iconAllowOverlap(true)
                                        )
                                    )
                                }

                                map.cameraPosition = CameraPosition.Builder()
                                    .target(initialLocation)
                                    .zoom(initialZoom)
                                    .build()

                                // Tap-to-pin listener
                                map.addOnMapClickListener { latLng ->
                                    pickedPoint = latLng

                                    // Move marker by updating the GeoJSON source
                                    map.style?.getSourceAs<GeoJsonSource>("marker-source")?.setGeoJson(
                                        Feature.fromGeometry(Point.fromLngLat(latLng.longitude, latLng.latitude))
                                    )
                                    true
                                }
                            }
                        }
                    },
                    update = { view ->
                        // Dynamically update style if it changes on recomposition
                        view.getMapAsync { map ->
                            map.style?.let { _ ->
                                // Note: Compare stripped JSONs or simply ignore if you don't swap dynamically
                            }
                        }
                    }
                )

                // Hint overlay – hidden once a point is chosen
                if (pickedPoint == null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    ) {
                        Text(
                            text = "点击地图以选择虚拟位置",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            // ── Coordinate readout ────────────────────────────────────
            pickedPoint?.let { pt ->
                HorizontalDivider()
                MapCoordinateReadout(point = pt)
            }
        }
    }
}

// ── Internal composables ──────────────────────────────────────────────────────

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = View.generateViewId()
        }
    }

    val lifecycleObserver = remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, lifecycleObserver) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

@Composable
private fun MapPickerHeader(
    pickedPoint: LatLng?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(MaterialSymbols.Outlined.Close, contentDescription = "Cancel")
        }
        Text(
            text = "地图选点",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onConfirm,
            enabled = pickedPoint != null,
        ) {
            Icon(
                imageVector = MaterialSymbols.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("确定")
        }
    }
}

@Composable
private fun MapCoordinateReadout(point: LatLng) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MapCoordinateChip(label = "纬度", value = "%.6f".format(point.latitude))
        MapCoordinateChip(label = "经度", value = "%.6f".format(point.longitude))
    }
}

@Composable
private fun MapCoordinateChip(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

private fun createDefaultMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt()
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E53935".toColorInt()
        style = Paint.Style.FILL
    }

    // ── Geometry ──────────────────────────────────────────────────────
    val cx   = size / 2f
    val r    = size * 0.38f
    val cy   = r + size * 0.04f          // circle center, slightly below top edge
    val tipY = size * 0.96f              // pin tip near the bottom

    // Where the two straight sides are tangent to the circle:
    //   d     = distance from tip to circle center
    //   sinA  = r/d  →  the half-angle between the axis and each tangent line
    val d    = tipY - cy
    val sinA = r / d
    val cosA = sqrt(1.0 - sinA * sinA).toFloat()

    val tx = r * cosA                    // horizontal offset of each tangent point from cx
    val ty = cy + r * sinA               // y-coordinate of both tangent points

    // Angles of the two tangent points measured from the circle center
    // (Android canvas: 0° = right, 90° = down, 270° = top)
    val leftAngle  = Math.toDegrees(atan2((ty - cy).toDouble(), (-tx).toDouble())).toFloat()
    val rightAngle = Math.toDegrees(atan2((ty - cy).toDouble(),   tx .toDouble())).toFloat()

    // Clockwise sweep from leftAngle → through the top (270°) → rightAngle
    // leftAngle ≈ 135°, rightAngle ≈ 45°  →  sweep ≈ 270°
    val sweepAngle = rightAngle - leftAngle + 360f

    // ── Pin path ──────────────────────────────────────────────────────
    val path = Path().apply {
        moveTo(cx, tipY)                          // tip
        lineTo(cx - tx, ty)                       // up-left to left tangent point
        arcTo(                                    // arc over the top of the circle
            RectF(cx - r, cy - r, cx + r, cy + r),
            leftAngle,
            sweepAngle,
        )
        // arcTo leaves the pen at the right tangent point; close() draws back to tip
        close()
    }
    canvas.drawPath(path, paint)

    // ── Inner white circle ────────────────────────────────────────────
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cy, r * 0.42f, paint)

    return bitmap
}

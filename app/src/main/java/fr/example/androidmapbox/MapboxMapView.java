package fr.example.androidmapbox;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapboxMapView implements OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapClickListener, Serializable {

    public final static int CAMERA_ANIMATION_DURATION = 1000;

    protected Context context;

    protected MapView mapView;
    protected MapboxMap mapboxMap;

    protected PermissionsManager permissionsManager;

    protected List<OnRouteRequestListener> routeRequestListeners = new ArrayList<>();

    protected List<Layer> layers = new ArrayList<>();
    protected List<Source> sources = new ArrayList<>();

    protected List<Hotel> data;

    int primaryColor;
    int secondaryColor;

    public MapboxMapView(Context context, Bundle savedInstanceState, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.mapView.onCreate(savedInstanceState);
        this.mapView.getMapAsync(this);

        primaryColor = ThemeUtils.getThemeColor(context, R.attr.colorPrimary);
        secondaryColor = ThemeUtils.getThemeColor(context, R.attr.colorSecondary);
    }

    public interface OnMapReadyListener {
        void onMapReady(MapboxMap mapboxMap, Style style);
    }

    public interface OnRouteRequestListener {
        void onRouteRequest();
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        mapboxMap.getUiSettings().setCompassEnabled(false);
        MapboxMapView.this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(getMapStyle(), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);

                mapboxMap.addOnMapClickListener(MapboxMapView.this);

                for (Layer layer: style.getLayers()) {
                    String label = "label";
                    if (layer.getId().contains(label)) {
                        layer.setProperties(PropertyFactory.visibility(Property.NONE));
                    }
                }
            }
        });
    }

    public void addOnRouteRequestListener(OnRouteRequestListener listener) {
        routeRequestListeners.add(listener);
    }

    public void addOnMapReadyListener(OnMapReadyListener listener) {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                mapboxMap.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        listener.onMapReady(mapboxMap, style);
                    }
                });
            }
        });
    }

    public void clearLayersAndSources() {
        mapboxMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                for (Layer layer: layers) {
                    style.removeLayer(layer);
                }
                layers.clear();

                for (Source source: sources) {
                    style.removeSource(source);
                }
                sources.clear();
            }
        });
    }

    public String getMapStyle() {
        if (ThemeUtils.isDarkThemeEnabled(context.getResources())) {
            return Style.DARK;
        }
        return Style.LIGHT;
    }

    public void setData(List<Hotel> data) {
        this.data = data;
    }

    @SuppressLint("MissingPermission")
    public void enableLocationComponent(@NonNull Style style) {
        if (PermissionsManager.areLocationPermissionsGranted(context)) {
            Resources resources = context.getResources();
            LocationComponentOptions locationComponentOptions = LocationComponentOptions.builder(context)
                    .accuracyColor(ResourcesCompat.getColor(resources, R.color.purple_300, null))
                    .foregroundStaleTintColor(primaryColor)
                    .foregroundTintColor(primaryColor)
                    .pulseEnabled(false)
                    .build();
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style)
                            .locationComponentOptions(locationComponentOptions)
                            .build()
            );
            locationComponent.setLocationComponentEnabled(true);
            zoomToLocation(locationComponent.getLastKnownLocation());
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions((Activity) context);
        }
    }

    public LatLng getCurrentLocation() {
        Location currentLocation = mapboxMap.getLocationComponent().getLastKnownLocation();
        LatLng coords = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        return coords;
    }

    public void zoomToLocation(Location location) {

        if (location == null)
            return;

        LatLng coords = new LatLng(location.getLatitude(), location.getLongitude());
        CameraPosition position = new CameraPosition.Builder()
                .target(coords)
                .zoom(16)
                .build();

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        LatLng currentLocation = getCurrentLocation();

        Point startingPoint = Point.fromLngLat(currentLocation.getLongitude(), currentLocation.getLatitude());
        Point endingPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());

        MapboxDirections client = MapboxDirections.builder()
                .accessToken(getMapboxAccessToken(context))
                .origin(startingPoint)
                .destination(endingPoint)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .build();

        LoadingDialog.showLoader((Activity) context);

        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.body() == null) {
                    Toast.makeText(context, "Cannot find route", Toast.LENGTH_LONG).show();
                    return;
                } else if (response.body().routes().size() < 1) {
                    Toast.makeText(context, "Cannot find route", Toast.LENGTH_LONG).show();
                    return;
                }

                DirectionsRoute route = response.body().routes().get(0);

                mapboxMap.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                        GeoJsonSource routeLineSource = style.getSourceAs("route-line-source");
                        GeoJsonSource routeMarkerSource = style.getSourceAs("route-marker-source");
                        GeoJsonSource routeHotelMarkerSource = style.getSourceAs("route-hotel-marker-source");

                        Layer routeLineLayer = style.getLayer("route-line-layer");
                        Layer routeMarkerLayer = style.getLayer("route-marker-layer");
                        Layer routeHotelMarkerLayer = style.getLayer("route-hotel-marker-layer");

                        LineString lineString = LineString.fromPolyline(route.geometry(), 6);

                        List<Feature> hotelFeatures = new ArrayList<>();
                        Random random = new Random();

                        for (Hotel hotel: data) {
                            int randomIndex = random.nextInt(lineString.coordinates().size());
                            Point p = lineString.coordinates().get(randomIndex);
                            Feature feature = Feature.fromGeometry(p);
                            feature.addNumberProperty("HOTEL_ID", hotel.id);
                            hotelFeatures.add(feature);
                        }
                        FeatureCollection hotelFeatureCollection = FeatureCollection.fromFeatures(hotelFeatures);

                        Feature markerFeature = Feature.fromGeometry(Point.fromLngLat(point.getLongitude(), point.getLatitude()));

                        if (routeLineSource == null) {
                            routeLineSource = new GeoJsonSource("route-line-source", lineString);
                            style.addSource(routeLineSource);
                            sources.add(routeLineSource);
                        } else {
                            routeLineSource.setGeoJson(lineString);
                        }

                        if (routeMarkerSource == null) {
                            routeMarkerSource = new GeoJsonSource("route-marker-source", markerFeature);
                            style.addSource(routeMarkerSource);
                            sources.add(routeMarkerSource);
                        } else {
                            routeMarkerSource.setGeoJson(markerFeature);
                        }

                        if (routeHotelMarkerSource == null) {
                            routeHotelMarkerSource = new GeoJsonSource("route-hotel-marker-source", hotelFeatureCollection);
                            style.addSource(routeHotelMarkerSource);
                            sources.add(routeHotelMarkerSource);
                        } else {
                            routeHotelMarkerSource.setGeoJson(hotelFeatureCollection);
                        }

                        if (routeLineLayer == null) {
                            routeLineLayer = new LineLayer("route-line-layer", "route-line-source")
                                    .withProperties(
                                            PropertyFactory.lineColor(primaryColor),
                                            PropertyFactory.lineWidth(5f),
                                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND)
                                    );

                            style.addLayerBelow(routeLineLayer, "mapbox-location-shadow-layer"); //road-label-minor
                            layers.add(routeLineLayer);
                        }

                        if (routeMarkerLayer == null) {
                            Drawable markerDrawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.map_route_marker, null);
                            style.addImage("route-marker", markerDrawable);

                            routeMarkerLayer = new SymbolLayer("route-marker-layer", "route-marker-source")
                                    .withProperties(
                                            PropertyFactory.iconImage("route-marker"),
                                            PropertyFactory.iconAllowOverlap(true)
                                    );
                            style.addLayerAbove(routeMarkerLayer, routeLineLayer.getId());
                            layers.add(routeMarkerLayer);
                        }

                        if (routeHotelMarkerLayer == null) {
                            Drawable hotelMarkerDrawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.map_route_marker, null);
                            hotelMarkerDrawable.setTint(secondaryColor);
                            style.addImage("route-hotel-marker", hotelMarkerDrawable);

                            routeHotelMarkerLayer = new SymbolLayer("route-hotel-marker-layer", "route-hotel-marker-source")
                                    .withProperties(
                                            PropertyFactory.iconImage("route-hotel-marker"),
                                            PropertyFactory.iconAllowOverlap(true)
                                    );
                            style.addLayerAbove(routeHotelMarkerLayer, routeLineLayer.getId());
                            layers.add(routeHotelMarkerLayer);
                        }

                        for (OnRouteRequestListener routeRequestListener: routeRequestListeners) {
                            routeRequestListener.onRouteRequest();
                        }

                        LatLngBounds bounds = new LatLngBounds.Builder()
                                .include(currentLocation)
                                .include(point)
                                .build();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 350), CAMERA_ANIMATION_DURATION);
                            }
                        }, 300);
                    }
                });

                LoadingDialog.dismissLoader();

            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                LoadingDialog.dismissLoader();
                Toast.makeText(context, "Cannot find route", Toast.LENGTH_LONG).show();
            }
        });

        return true;
    }

    public void zoomToLatLng(LatLng coords) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 15), CAMERA_ANIMATION_DURATION);
            }
        }, 300);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(context, "Need location permissions", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(context, "Need location permissions", Toast.LENGTH_LONG).show();
        }
    }

    public static String getMapboxAccessToken(@NonNull Context context) {
        try {
            // Read out AndroidManifest
            String token = Mapbox.getAccessToken();
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException();
            }
            return token;
        } catch (Exception exception) {
            // Use fallback on string resource, used for development
            int tokenResId = context.getResources()
                    .getIdentifier("mapbox_access_token", "string", context.getPackageName());
            return tokenResId != 0 ? context.getString(tokenResId) : null;
        }
    }
}

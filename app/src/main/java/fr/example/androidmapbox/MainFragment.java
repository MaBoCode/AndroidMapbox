package fr.example.androidmapbox;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import fr.example.androidmapbox.databinding.FrgMainBinding;

import static fr.example.androidmapbox.MapboxMapView.CAMERA_ANIMATION_DURATION;

public class MainFragment extends Fragment implements HotelAdapter.HotelItemClickListener {

    protected FrgMainBinding binding;

    protected MapboxMapView mapboxMapView;

    protected List<Hotel> data;

    protected boolean hotelListItemClicked = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FrgMainBinding.inflate(inflater, container, false);

        binding.btnSwitchTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThemeUtils.switchTheme(getResources());
            }
        });

        mapboxMapView = new MapboxMapView(getContext(), savedInstanceState, binding.mapView);
        mapboxMapView.addOnMapReadyListener(new MapboxMapView.OnMapReadyListener() {
            @Override
            public void onMapReady(MapboxMap mapboxMap, Style style) {
                showMapView();
            }
        });

        init();

        return binding.getRoot();
    }

    public void showMapView() {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(binding.mapView, "alpha", 0f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimator);
        animatorSet.setDuration(300L);
        animatorSet.setStartDelay(100L);

        animatorSet.start();
    }

    public void init() {
        initData();
        mapboxMapView.setData(data);

        RecyclerView hotelRecyclerView = binding.hotelRecyclerView;
        HotelAdapter hotelAdapter = new HotelAdapter(new Hotel.HotelDiff(), this);
        hotelAdapter.submitList(data);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        layoutManager.setSmoothScrollbarEnabled(true);

        hotelRecyclerView.setHasFixedSize(true);
        hotelRecyclerView.setLayoutManager(layoutManager);
        hotelRecyclerView.setAdapter(hotelAdapter);

        hotelRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                if (!hotelListItemClicked && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Log.d("LOG", "[DEBUG] HERE: " + layoutManager.findFirstVisibleItemPosition());
                    Log.d("LOG", "[DEBUG] THERE: " + layoutManager.findFirstCompletelyVisibleItemPosition());
                    mapboxMapView.addOnMapReadyListener(new MapboxMapView.OnMapReadyListener() {
                        @Override
                        public void onMapReady(MapboxMap mapboxMap, Style style) {
                            int position = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();

                            if (position < 0)
                                return;

                            Hotel hotel = data.get(position);
                            LatLng coords = getHotelCoords(style, hotel);
                            mapboxMapView.zoomToLatLng(coords);
                        }
                    });
                } else {
                    hotelListItemClicked = false;
                }
            }
        });

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(hotelRecyclerView);

        binding.tlbMain.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideRecyclerView();
                hideToolbar();
                mapboxMapView.clearLayersAndSources();
            }
        });

        mapboxMapView.addOnRouteRequestListener(new MapboxMapView.OnRouteRequestListener() {
            @Override
            public void onRouteRequest() {
                showToolbar();
                showRecyclerView();
            }
        });
    }

    public LatLng getHotelCoords(Style style, Hotel hotel) {
        GeoJsonSource hotelMarkerSource = style.getSourceAs("route-hotel-marker-source");
        List<Feature> features = hotelMarkerSource.querySourceFeatures(null);

        LatLng coords = null;
        for (Feature feature: features) {
            if (feature.getNumberProperty("HOTEL_ID").intValue() == hotel.id) {
                if (feature.geometry() instanceof Point) {
                    Point point = (Point) feature.geometry();
                    coords = new LatLng(point.latitude(), point.longitude());
                    break;
                }
            }
        }
        return coords;
    }

    @Override
    public void onItemClick(Hotel hotel) {
        hotelListItemClicked = true;
        LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(binding.hotelRecyclerView.getContext()) {
            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 0.4f;
            }
        };
        linearSmoothScroller.setTargetPosition(hotel.id);
        binding.hotelRecyclerView.getLayoutManager().startSmoothScroll(linearSmoothScroller);

        mapboxMapView.addOnMapReadyListener(new MapboxMapView.OnMapReadyListener() {
            @Override
            public void onMapReady(MapboxMap mapboxMap, Style style) {
                LatLng coords = getHotelCoords(style, hotel);
                mapboxMapView.zoomToLatLng(coords);
            }
        });
    }

    public void showToolbar() {
        ObjectAnimator translationAnimator = ObjectAnimator.ofFloat(binding.tlbMain, "translationY", -56f , 0f);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(binding.tlbMain, "alpha", 0f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translationAnimator, alphaAnimator);
        animatorSet.setDuration(500L);

        animatorSet.start();
    }

    public void hideToolbar() {
        ObjectAnimator translationAnimator = ObjectAnimator.ofFloat(binding.tlbMain, "translationY", 0f, -56f);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(binding.tlbMain, "alpha", 1f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translationAnimator, alphaAnimator);
        animatorSet.setDuration(500L);

        animatorSet.start();
    }

    public void showRecyclerView() {
        ObjectAnimator translationAnimator = ObjectAnimator.ofFloat(binding.hotelRecyclerView, "translationY", 180f, 0f);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(binding.hotelRecyclerView, "alpha", 0f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translationAnimator, alphaAnimator);
        animatorSet.setInterpolator(new BounceInterpolator());
        animatorSet.setDuration(500L);

        animatorSet.start();
    }

    public void hideRecyclerView() {
        ObjectAnimator translationAnimator = ObjectAnimator.ofFloat(binding.hotelRecyclerView, "translationY", 0f, 180f);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(binding.hotelRecyclerView, "alpha", 1f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translationAnimator, alphaAnimator);
        animatorSet.setDuration(500L);

        animatorSet.start();
    }

    private Drawable resizeResource(int id) {
        Resources resources = getResources();

        Bitmap bitmap = BitmapFactory.decodeResource(resources, id);
        bitmap = Bitmap.createScaledBitmap(bitmap, 400, 600, false);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, bitmap);
        return bitmapDrawable;
    }

    public void initData() {

        Drawable hotel1 = resizeResource(R.drawable.hotel_1);
        Drawable hotel2 = resizeResource(R.drawable.hotel_2);
        Drawable hotel3 = resizeResource(R.drawable.hotel_3);
        Drawable hotel4 = resizeResource(R.drawable.hotel_4);
        Drawable hotel5 = resizeResource(R.drawable.hotel_5);

        data = new ArrayList<>();
        data.add(new Hotel(0, "Zino Hotel", "One of the best hotels in Norway, ready for all road trippers ! Great reviews !", hotel1));
        data.add(new Hotel(1, "Kaplo Hotel", "One of the best hotels in Norway, ready for all road trippers ! Great reviews !", hotel2));
        data.add(new Hotel(2, "Bijo Hotel", "One of the best hotels in Norway, ready for all road trippers ! Great reviews !", hotel3));
        data.add(new Hotel(3, "Plaza Hotel", "One of the best hotels in Norway, ready for all road trippers ! Great reviews !", hotel4));
        data.add(new Hotel(4, "Opka Hotel", "One of the best hotels in Norway, ready for all road trippers ! Great reviews !", hotel5));

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

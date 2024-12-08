package com.example.campustouring;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Earth;
import com.google.ar.core.Frame;
import com.google.ar.core.GeospatialPose;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.VpsAvailability;
import com.google.ar.core.VpsAvailabilityFuture;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.FineLocationPermissionNotGrantedException;
import com.google.ar.core.exceptions.GooglePlayServicesLocationLibraryNotLinkedException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.core.exceptions.UnsupportedConfigurationException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import androidx.navigation.fragment.NavHostFragment;

import common.helpers.CameraPermissionHelper;
import common.helpers.DisplayRotationHelper;
import common.helpers.FullScreenHelper;
import common.helpers.LocationPermissionHelper;
import common.helpers.TrackingStateHelper;
import common.samplerender.Framebuffer;
import common.samplerender.Mesh;
import common.samplerender.SampleRender;
import common.helpers.SnackbarHelper;
import common.samplerender.Shader;
import common.samplerender.Texture;
import common.samplerender.VertexBuffer;
import common.samplerender.arcore.BackgroundRenderer;
import common.samplerender.arcore.PlaneRenderer;
import de.javagl.obj.Obj;

import com.opencsv.CSVReader;

public class ARFragment extends Fragment implements SampleRender.Renderer {

    private static final String TAG = ARFragment.class.getSimpleName();
    private static final String ALLOW_GEOSPATIAL_ACCESS_KEY = "ALLOW_GEOSPATIAL_ACCESS";
    private static final String SHARED_PREFERENCES_SAVED_ANCHORS = "SHARED_PREFERENCES_SAVED_ANCHORS";
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000f;

    private float minAnchorRange = 50.0f;
    private boolean createdPos = false;

    private boolean defaultCreated = false;

    private List<String> defaultPoints;

    private HashMap<Integer, Pose> loadedAnchors = new HashMap<>();

    // The thresholds that are required for horizontal and orientation accuracies before entering into
    // the LOCALIZED state. Once the accuracies are equal or less than these values, the app will
    // allow the user to place anchors.
    private static final double LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS = 25;
    private static final double LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES = 25;

    // Once in the LOCALIZED state, if either accuracies degrade beyond these amounts, the app will
    // revert back to the LOCALIZING state.
    private static final double LOCALIZED_HORIZONTAL_ACCURACY_HYSTERESIS_METERS = 25;
    private static final double LOCALIZED_ORIENTATION_YAW_ACCURACY_HYSTERESIS_DEGREES = 25;

    private static final int LOCALIZING_TIMEOUT_SECONDS = 180;
    private static final int MAXIMUM_ANCHORS = 20;
    private static final long DURATION_FOR_NO_TERRAIN_ANCHOR_RESULT_MS = 10000;
    private Session session;
    private SharedPreferences sharedPreferences;
    private SampleRender render;
    private DisplayRotationHelper displayRotationHelper;
    private GLSurfaceView surfaceView;
    private boolean installRequested;
    private final HashMap<Integer, Anchor> anchors = new HashMap<>();
    private long localizingStartTimestamp;

    private PlaneRenderer planeRenderer;
    private BackgroundRenderer backgroundRenderer;
    private Framebuffer virtualSceneFramebuffer;

    private Mesh virtualObjectMesh;
    private Shader geospatialAnchorVirtualObjectShader;
    // Virtual object (ARCore geospatial terrain)
    private Shader terrainAnchorVirtualObjectShader;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(getActivity());

    private final Object singleTapLock = new Object();

    @GuardedBy("singleTapLock")
    private MotionEvent queuedSingleTap;

    private GestureDetector gestureDetector;

    private VertexBuffer pointCloudVertexBuffer;
    private long lastPointCloudTimestamp = 0;
    private Mesh pointCloudMesh;
    private Shader pointCloudShader;
    private boolean hasSetTextureNames = false;

    private FusedLocationProviderClient fusedLocationClient;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    enum State {
        UNINITIALIZED,
        UNSUPPORTED,
        EARTH_STATE_ERROR,
        PRETRACKING,
        LOCALIZING,
        LOCALIZING_FAILED,
        LOCALIZED
    }

    private State state = State.UNINITIALIZED;

    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16]; // view x model
    private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model

    private final float[] identityQuaternion = {0, 0, 0, 1};

    private final Object anchorsLock = new Object();

    private AssetManager assetManager;
    private final MainActivity mainActivity = (MainActivity) getActivity();
    private CustomMarkerContract contract;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_a_r, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        surfaceView = view.findViewById(R.id.surfaceview);

        render = new SampleRender(surfaceView, this, assetManager);

        gestureDetector =
                new GestureDetector(
                        getActivity(),
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                synchronized (singleTapLock) {
                                    queuedSingleTap = e;
                                }
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });
        surfaceView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        installRequested = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        assetManager = getActivity().getAssets();


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(/* context= */ getActivity());

        if(session == null){
            if (!sharedPreferences.edit().putBoolean(ALLOW_GEOSPATIAL_ACCESS_KEY, true).commit()) {
                throw new AssertionError("Could not save the user preference to SharedPreferences!");
            }

            createSession();
        }
        displayRotationHelper = new DisplayRotationHelper(/* activity= */ getActivity());
    }


    @Override
    public void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        super.onDestroy();
    }

    @Override
    public void onResume() {

        if (sharedPreferences.getBoolean(ALLOW_GEOSPATIAL_ACCESS_KEY, /* defValue= */ false)) {
            createSession();
        }
        super.onResume();
        surfaceView.onResume();
    }

    private void createSession() {
        Exception exception = null;
        String message = null;
        if (session == null) {

            try {
                switch (ArCoreApk.getInstance().requestInstall(getActivity(), !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.

                if (!CameraPermissionHelper.hasCameraPermission(getActivity())) {
                    CameraPermissionHelper.requestCameraPermission(getActivity());
                    return;
                }
                if (!LocationPermissionHelper.hasFineLocationPermission(getActivity())) {
                    LocationPermissionHelper.requestFineLocationPermission(getActivity());
                    return;
                }

                // Create the session.
                // Plane finding mode is default on, which will help the dynamic alignment of terrain
                // anchors on ground.
                session = new Session(/* context= */ getActivity());
            } catch (UnavailableArcoreNotInstalledException
                     | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(getActivity(), message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }
        // Check VPS availability before configure and resume session.
        if (session != null) {
            getLastLocation();
        }
        contract = new CustomMarkerContract(this.getContext());
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            configureSession();
            session.resume();
        } catch (CameraNotAvailableException e) {
            message = "Camera not available. Try restarting the app.";
            exception = e;
        } catch (GooglePlayServicesLocationLibraryNotLinkedException e) {
            message = "Google Play Services location library not linked or obfuscated with Proguard.";
            exception = e;
        } catch (FineLocationPermissionNotGrantedException e) {
            message = "The Android permission ACCESS_FINE_LOCATION was not granted.";
            exception = e;
        } catch (UnsupportedConfigurationException e) {
            message = "This device does not support GeospatialMode.ENABLED.";
            exception = e;
        } catch (SecurityException e) {
            message = "Camera failure or the internet permission has not been granted.";
            exception = e;
        }

        if (message != null) {
            session = null;
            messageSnackbarHelper.showError(getActivity(), message);
            Log.e(TAG, "Exception configuring and resuming the session", exception);
            return;
        }
    }

    @Override
    public void onSurfaceCreated(SampleRender render) {
        // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
        // an IOException.

        try {
            planeRenderer = new PlaneRenderer(render);
            backgroundRenderer = new BackgroundRenderer(render);
            virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

            // Virtual object to render (ARCore geospatial)
            Texture virtualObjectTexture =
                    Texture.createFromAsset(
                            render,
                            "models/AprilFools_basecolor.png",
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            Texture.ColorFormat.SRGB);

            virtualObjectMesh = Mesh.createFromAsset(render, "models/AprilFoolsPin.obj");
            geospatialAnchorVirtualObjectShader =
                    Shader.createFromAssets(
                                    render,
                                    "shaders/ar_unlit_object.vert",
                                    "shaders/ar_unlit_object.frag",
                                    /* defines= */ null)
                            .setTexture("u_Texture", virtualObjectTexture);

            backgroundRenderer.setUseDepthVisualization(render, false);
            backgroundRenderer.setUseOcclusion(render, false);

            // Point cloud
            pointCloudShader =
                    Shader.createFromAssets(
                                    render,
                                    "shaders/point_cloud.vert",
                                    "shaders/point_cloud.frag",
                                    /* defines= */ null)
                            .setVec4(
                                    "u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
                            .setFloat("u_PointSize", 5.0f);
            // four entries per vertex: X, Y, Z, confidence
            pointCloudVertexBuffer =
                    new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
            final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
            pointCloudMesh =
                    new Mesh(
                            render, Mesh.PrimitiveMode.POINTS, /* indexBuffer= */ null, pointCloudVertexBuffers);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read a required asset file", e);
            messageSnackbarHelper.showError(getActivity(), "Failed to read a required asset file: " + e);
        }
    }

    @Override
    public void onSurfaceChanged(SampleRender render, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        virtualSceneFramebuffer.resize(width, height);
    }

    @Override
    public void onDrawFrame(SampleRender render) {

        if (session == null) {
            return;
        }

        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(
                    new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
            hasSetTextureNames = true;
        }

        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        Frame frame;
        try {
            frame = session.update();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onDrawFrame", e);
            messageSnackbarHelper.showError(getActivity(), "Camera not available. Try restarting the app.");
            return;
        }
        Camera camera = frame.getCamera();

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame);

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        //trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        Earth earth = session.getEarth();
        if (earth != null) {
            updateGeospatialState(earth);
        }

        // -- Draw background

        if (frame.getTimestamp() != 0) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            backgroundRenderer.drawBackground(render);
        }

        // If not tracking, don't draw 3D objects.
        if (camera.getTrackingState() != TrackingState.TRACKING || state != State.LOCALIZED) {
            return;
        }
//        loadDefaultPoints();

        if(!createdPos){
            Pose newPose = session.getEarth().getPose(session.getEarth().getCameraGeospatialPose().getLatitude(),
            session.getEarth().getCameraGeospatialPose().getLongitude(),
                    session.getEarth().getCameraGeospatialPose().getAltitude() + 6f,
                    0,0,0,0);
            GeospatialPose pose = session.getEarth().getGeospatialPose(newPose);
            createAnchorWithGeospatialPose(20, session.getEarth(), pose);
            createdPos = true;
        }

        for (HashMap.Entry<Integer, Pose> entry : loadedAnchors.entrySet()) {
            if(calculateDistance(camera.getPose().getTranslation(), entry.getValue().getTranslation()) < minAnchorRange) {
                GeospatialPose pose = session.getEarth().getGeospatialPose(entry.getValue());
                createAnchorWithGeospatialPose(entry.getKey(), session.getEarth(), pose);
            }
            else {
                anchors.remove(entry.getKey());
            }
        }

        for(HashMap.Entry<Integer, Anchor> entry: anchors.entrySet()){
            if(isFacingAnchor(camera.getPose(), entry.getValue())){
                Log.i(TAG, "Facing " + entry.getKey() + " : " + entry.getValue().toString());
                // if tapped send int associated with anchor to info frame
                handleTap(frame, camera.getTrackingState(), entry.getKey());

            }
        }
        // -- Draw virtual objects

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0);

        // Visualize tracked points.
        // Use try-with-resources to automatically release the point cloud.
        try (PointCloud pointCloud = frame.acquirePointCloud()) {
            if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
                pointCloudVertexBuffer.set(pointCloud.getPoints());
                lastPointCloudTimestamp = pointCloud.getTimestamp();
            }
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            render.draw(pointCloudMesh, pointCloudShader);
        }catch (Exception e){
            Log.e(TAG, "onDrawFrame: ", e);
        }

        // Visualize planes.
        planeRenderer.drawPlanes(
                render,
                session.getAllTrackables(Plane.class),
                camera.getDisplayOrientedPose(),
                projectionMatrix);

        // Visualize anchors created by touch.
        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);

        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);
        synchronized (anchorsLock) {

            for(HashMap.Entry<Integer, Anchor> entry : anchors.entrySet()) {
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                if (entry.getValue().getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }

                // Convert the anchor's pose to a matrix
                entry.getValue().getPose().toMatrix(modelMatrix, 0);

                entry.getValue().getPose().toMatrix(modelMatrix, 0);
                float[] scaleMatrix = new float[16];
                Matrix.setIdentityM(scaleMatrix, 0);
                float scale = getScale(entry.getValue().getPose(), camera.getDisplayOrientedPose()) * 20;
                scaleMatrix[0] = scale;
                scaleMatrix[5] = scale;
                scaleMatrix[10] = scale;
                Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
                // Rotate the virtual object 180 degrees around the Y axis to make the object face the GL
                // camera -Z axis, since camera Z axis faces toward users.
                float[] rotationMatrix = new float[16];
                Matrix.setRotateM(rotationMatrix, 0, 90, 1.0f, 0.0f, 0.0f);
                float[] rotationModelMatrix = new float[16];
                Matrix.multiplyMM(rotationModelMatrix, 0, modelMatrix, 0, rotationMatrix, 0);
                // Calculate model/view/projection matrices
                Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, rotationModelMatrix, 0);
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

                geospatialAnchorVirtualObjectShader.setMat4(
                        "u_ModelViewProjection", modelViewProjectionMatrix);
                render.draw(
                        virtualObjectMesh, geospatialAnchorVirtualObjectShader, virtualSceneFramebuffer);
            }
        }

        // Compose the virtual scene with the background.
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
    }
    private void getLastLocation() {
        try {
            fusedLocationClient
                    .getLastLocation()
                    .addOnSuccessListener(
                            new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    double latitude = 0;
                                    double longitude = 0;
                                    if (location != null) {
                                        latitude = location.getLatitude();
                                        longitude = location.getLongitude();
                                    } else {
                                        Log.e(TAG, "Error location is null");
                                    }
                                    checkVpsAvailability(latitude, longitude);
                                }
                            });
        } catch (SecurityException e) {
            Log.e(TAG, "No location permissions granted by User!");
        }
    }

    private void updateGeospatialState(Earth earth) {
        if (earth.getEarthState() != Earth.EarthState.ENABLED) {
            state = State.EARTH_STATE_ERROR;
            return;
        }
        if (earth.getTrackingState() != TrackingState.TRACKING) {
            state = State.PRETRACKING;
            return;
        }
        if (state == State.PRETRACKING) {
            updatePretrackingState(earth);
        } else if (state == State.LOCALIZING) {
            updateLocalizingState(earth);
        } else if (state == State.LOCALIZED) {
            updateLocalizedState(earth);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            surfaceView.onPause();
            session.pause();
        }
    }


    // Return the scale in range [1, 2] after mapping a distance between camera and anchor to [2, 20].
    private float getScale(Pose anchorPose, Pose cameraPose) {
        double distance =
                Math.sqrt(
                        Math.pow(anchorPose.tx() - cameraPose.tx(), 2.0)
                                + Math.pow(anchorPose.ty() - cameraPose.ty(), 2.0)
                                + Math.pow(anchorPose.tz() - cameraPose.tz(), 2.0));
        double mapDistance = Math.min(Math.max(2, distance), 20);
        return (float) (mapDistance - 2) / (20 - 2) + 1;
    }

    private void updatePretrackingState(Earth earth) {
        if (earth.getTrackingState() == TrackingState.TRACKING) {
            state = State.LOCALIZING;
            return;
        }
    }

    private void checkVpsAvailability(double latitude, double longitude) {
        final VpsAvailabilityFuture future =
                session.checkVpsAvailabilityAsync(
                        latitude,
                        longitude,
                        availability -> {
                            if (availability != VpsAvailability.AVAILABLE) {

                            }
                        });
    }

    private void updateLocalizingState(Earth earth) {
        GeospatialPose geospatialPose = earth.getCameraGeospatialPose();
        if (geospatialPose.getHorizontalAccuracy() <= LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS
                && geospatialPose.getOrientationYawAccuracy()
                <= LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES) {
            state = State.LOCALIZED;
            synchronized (anchorsLock) {
                final int anchorNum = anchors.size();
                if (anchorNum == 0) {
                    //createAnchorFromSharedPreferences(earth);
                    Log.i(TAG, "updateLocalizingState: 0 anchors");
                }
            }
            return;
        }

        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - localizingStartTimestamp)
                > LOCALIZING_TIMEOUT_SECONDS) {
            state = State.LOCALIZING_FAILED;
            return;
        }
    }

    private void updateLocalizedState(Earth earth) {
        GeospatialPose geospatialPose = earth.getCameraGeospatialPose();
        // Check if either accuracy has degraded to the point we should enter back into the LOCALIZING
        // state.
        if (geospatialPose.getHorizontalAccuracy()
                > LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS
                + LOCALIZED_HORIZONTAL_ACCURACY_HYSTERESIS_METERS
                || geospatialPose.getOrientationYawAccuracy()
                > LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES
                + LOCALIZED_ORIENTATION_YAW_ACCURACY_HYSTERESIS_DEGREES) {
            // Accuracies have degenerated, return to the localizing state.
            state = State.LOCALIZING;
            localizingStartTimestamp = System.currentTimeMillis();
        }
    }

    private void createAnchor(
            int index, Earth earth, double latitude, double longitude, double altitude, float[] quaternion) {
        Anchor anchor =
                earth.createAnchor(
                        latitude,
                        longitude,
                        altitude,
                        quaternion[0],
                        quaternion[1],
                        quaternion[2],
                        quaternion[3]);
        synchronized (anchorsLock) {
            anchors.put(index, anchor);
        }
    }

    private void configureSession() {
        // Earth mode may not be supported on this device due to insufficient sensor quality.
        if (!session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
            state = State.UNSUPPORTED;
            return;
        }

        Config config = session.getConfig();
        config = config.setGeospatialMode(Config.GeospatialMode.ENABLED);
        session.configure(config);
        state = State.PRETRACKING;
        localizingStartTimestamp = System.currentTimeMillis();
    }


    private void createAnchorWithGeospatialPose(int index, Earth earth, GeospatialPose geospatialPose) {
        double latitude = geospatialPose.getLatitude();
        double longitude = geospatialPose.getLongitude();
        double altitude = geospatialPose.getAltitude();

        createAnchor(index, earth, latitude, longitude, altitude, identityQuaternion);
        storeAnchorParameters(latitude, longitude, altitude, identityQuaternion);
    }

    private void storeAnchorParameters(
            double latitude, double longitude, double altitude, float[] quaternion) {
        Set<String> anchorParameterSet =
                sharedPreferences.getStringSet(SHARED_PREFERENCES_SAVED_ANCHORS, new HashSet<>());
        HashSet<String> newAnchorParameterSet = new HashSet<>(anchorParameterSet);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        newAnchorParameterSet.add(
                String.format(
                        "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f",
                        latitude,
                        longitude,
                        altitude,
                        quaternion[0],
                        quaternion[1],
                        quaternion[2],
                        quaternion[3]));
        editor.putStringSet(SHARED_PREFERENCES_SAVED_ANCHORS, newAnchorParameterSet);
        editor.commit();
    }

    private void handleTap(Frame frame, TrackingState cameraTrackingState, int entry) {
        // Handle taps. Handling only one tap per frame, as taps are usually low frequency
        // compared to frame rate.
        synchronized (singleTapLock) {
            synchronized (anchorsLock) {
                if (queuedSingleTap == null
                        || anchors.size() >= MAXIMUM_ANCHORS
                        || cameraTrackingState != TrackingState.TRACKING) {
                    queuedSingleTap = null;
                    return;
                }
            }
            Earth earth = session.getEarth();
            if (earth == null || earth.getTrackingState() != TrackingState.TRACKING) {
                queuedSingleTap = null;
                return;
            }


            transitionToDetails(requireActivity(), entry);
            queuedSingleTap = null;

        }
    }

    public void loadDefaultPoints() {
        if (!defaultCreated) {
            try (CSVReader reader = new CSVReader (new InputStreamReader(getResources().openRawResource(R.raw.master)), '~')){
                List<String[]> rows = reader.readAll();
                String[] headers = rows.remove(0); // Remove and store the header row
                ArrayList<HashMap<String, String>> data = new ArrayList<>();

                for (String[] row : rows) {
                    HashMap<String, String> rowData = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        rowData.put(headers[i], row[i]);
                    }
                    data.add(rowData);
                }

                for (HashMap<String, String> row : data) {
                    double rowLat = Double.parseDouble(row.get("lat"));
                    double rowLong = Double.parseDouble(row.get("long"));
                    Pose newPose = session.getEarth().getPose(rowLat, rowLong, session.getEarth().getCameraGeospatialPose().getAltitude(), 0,0,0,0);
                    loadedAnchors.put(Integer.parseInt(row.get("index")), newPose);
                }
            } catch (Exception ex) {
                Log.e("TAG", "loadDefaultPoints FAILED");
                Log.e("TAG", ex.toString());
            }
            defaultCreated = true;
        }
    }

    public void loadCustomPoints() {
        List<CustomMarkerContract.MarkerEntryObj> customPointList = contract.readAllFromDb();

        for (CustomMarkerContract.MarkerEntryObj row : customPointList) {
            Pose newPose = session.getEarth().getPose(row.latitude, row.longitude,
                    session.getEarth().getCameraGeospatialPose().getAltitude(), 0,0,0,0);
            loadedAnchors.put((int)(row._id), newPose);
        }
    }
    public void loadPoints() {
        loadDefaultPoints();
        loadCustomPoints();
    }

//    private ArrayList<Anchor> getAnchorsInRange(Pose cameraPose) {
//        ArrayList<Anchor> nearbyAnchors = new ArrayList<>();
//        float[] cameraPosition = cameraPose.getTranslation();
//        for (Anchor anchor : anchors) {
//            Pose anchorPose = anchor.getPose();
//            float[] anchorPosition = anchorPose.getTranslation();
//
//            if(calculateDistance(cameraPosition, anchorPosition) < minAnchorRange){
//                nearbyAnchors.add(anchor);
//            }
//            else {
//                nearbyAnchors.remove(anchor);
//            }
//        }
//        return nearbyAnchors;
//    }

    private boolean isFacingAnchor(Pose cameraPose, Anchor anchor){
        float[] cameraForward = cameraPose.getZAxis();
        float[] cameraPosition = cameraPose.getTranslation();

        Pose anchorPose = anchor.getPose();
        float[] anchorPosition = anchorPose.getTranslation();

        float[] directionToAnchor = {
                anchorPosition[0] - cameraPosition[0],
                anchorPosition[1] - cameraPosition[1],
                anchorPosition[2] - cameraPosition[2]
        };

        float norm = (float) Math.sqrt(directionToAnchor[0] * directionToAnchor[0] + directionToAnchor[1] * directionToAnchor[1] + directionToAnchor[2] * directionToAnchor[2]);
        directionToAnchor[0] /= norm;
        directionToAnchor[1] /= norm;
        directionToAnchor[2] /= norm;

        float dotProduct = cameraForward[0] * directionToAnchor[0] + cameraForward[1] * directionToAnchor[1] + cameraForward[2] * directionToAnchor[2];
        float threshold = -0.8f;

        if (dotProduct < threshold) {
            return true;
        }

        return false;
    }

    public float calculateDistance(float[] pos1, float[] pos2) {
        return (float) Math.sqrt(
                Math.pow(pos1[0] - pos2[0], 2) +
                        Math.pow(pos1[1] - pos2[1], 2) +
                        Math.pow(pos1[2] - pos2[2], 2)
        );
    }
    private void transitionToDetails(Activity activity, int entry) {
        session.pause();
        for (Anchor anchor : session.getAllAnchors()) {
            anchor.detach();
        }
        for (Trackable trackable : session.getAllTrackables(null)) {
            trackable = null;
        }
        surfaceView.setRenderMode(0);
        session.close();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_content_main);
                Bundle args = new Bundle();
                args.putString("snippet", Integer.toString(entry));
                navController.navigate(
                        R.id.action_ARFragment_to_MarkerInfoFragment,
                        args
                );
            }
        });

    }
}
package com.yalantis.ucrop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yalantis.ucrop.callback.BitmapCropCallback;
import com.yalantis.ucrop.model.AspectRatio;
import com.yalantis.ucrop.util.SelectedStateListDrawable;
import com.yalantis.ucrop.view.CropImageView;
import com.yalantis.ucrop.view.GestureCropImageView;
import com.yalantis.ucrop.view.OverlayView;
import com.yalantis.ucrop.view.TransformImageView;
import com.yalantis.ucrop.view.UCropView;
import com.yalantis.ucrop.view.widget.AspectRatioTextView;
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


public class UCropFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public static final int DEFAULT_COMPRESS_QUALITY = 90;
    public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;

    public static final int NONE = 0;
    public static final int SCALE = 1;
    public static final int ROTATE = 2;
    public static final int ALL = 3;

    @IntDef({NONE, SCALE, ROTATE, ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GestureTypes {

    }

    private static final String TAG = "UCropActivity";
    private static final String EXTRA_PREFIX = BuildConfig.APPLICATION_ID;
    public static final String EXTRA_INPUT_URI = EXTRA_PREFIX + ".InputUri";
    public static final String EXTRA_OUTPUT_URI = EXTRA_PREFIX + ".OutputUri";

    private static final int TABS_COUNT = 3;
    private static final int SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000;
    private static final int ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42;

    private String mToolbarTitle;

    // Enables dynamic coloring
    private int mToolbarColor;
    private int mStatusBarColor;
    private int mActiveWidgetColor;
    private int mToolbarWidgetColor;
    @ColorInt
    private int mRootViewBackgroundColor;
    @DrawableRes
    private int mToolbarCancelDrawable;
    @DrawableRes private int mToolbarCropDrawable;
    private int mLogoColor;

    private boolean mShowBottomControls;
    private boolean mShowLoader = true;

    private UCropView mUCropView;
    private GestureCropImageView mGestureCropImageView;
    private OverlayView mOverlayView;
    private FrameLayout background;
    private ImageView imageLogo;
    private ViewGroup mWrapperStateAspectRatio, mWrapperStateRotate, mWrapperStateScale;
    private ViewGroup mLayoutAspectRatio, mLayoutRotate, mLayoutScale;
    private List<ViewGroup> mCropAspectRatioViews = new ArrayList<>();
    private TextView mTextViewRotateAngle, mTextViewScalePercent;
    private View mBlockingView;

    private Bitmap.CompressFormat mCompressFormat = DEFAULT_COMPRESS_FORMAT;
    private int mCompressQuality = DEFAULT_COMPRESS_QUALITY;
    private int[] mAllowedGestures = new int[]{SCALE, ROTATE, ALL};
    private Uri source;
    private Uri destination;
    private OnFragmentResultUriListener onFragmentResultUriListener;

    private Intent mCropIntent;
    private FrameLayout mWrapperControls;
    private LinearLayout mWrapperStates;
    private static int ANIM_DURATION = 400;
    private ImageView uCropShadow;
    private Toolbar toolbar;
    private WeakReference<FrameLayout> weakRefContainer;
    private boolean startShowing;
    private boolean imageInit;
    private Rect fromRect;
    private int imageSize;
    private int width;
    private int height;
    private View view;

    //private OnFragmentInteractionListener mListener;

    public UCropFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static UCropFragment newInstance(FrameLayout fragmentContent) {
        UCropFragment fragment = new UCropFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.weakRefContainer = new WeakReference<>(fragmentContent);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("onCreate","onCreate");
        setHasOptionsMenu(true);
        if (getArguments() != null) {
//            source = Uri.parse(getArguments().getString(ARG_PARAM1));
//            destination = Uri.parse(getArguments().getString(ARG_PARAM2));
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }
    }

    public void setItem(Rect fromRect, String param1, String param2) {
        imageInit = true;
        this.fromRect = fromRect;
        source = Uri.parse(param1);
        destination = Uri.parse(param2);
    }

    public void show() {
        Log.e("qwe","show");
        FrameLayout layout = weakRefContainer.get();
        startShowing = true;
        if (layout != null && imageInit) {
            setupAll(view);
            startShow();
        }
    }

    private void startShow(){
        Log.e("qwe","startShow");
        FrameLayout layout = weakRefContainer.get();
        if (layout != null) {
//            setupImagePositionOnShow();
//            mGestureCropImageView.animate().start();
//            startShowing = false;
            setupStateBeforeAnimation();
            layout.setVisibility(View.VISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mGestureCropImageView.clearAnimation();
                    mGestureCropImageView.setTranslationX(fromRect.left);
                    mGestureCropImageView.setTranslationY(fromRect.top);
                    mGestureCropImageView.setPivotX(0);
                    mGestureCropImageView.setPivotY(0);
                    mGestureCropImageView.setScaleX((float) fromRect.width() / imageSize);
                    mGestureCropImageView.setScaleY((float) fromRect.height() / imageSize);
                    mGestureCropImageView.animate()
                            .setDuration(1000)
                            .translationX(0)
                            .translationY(0)
                            .scaleX(1)
                            .scaleY(1)
                            .withStartAction(new Runnable() {
                                @Override
                                public void run() {
                                    background.setVisibility(View.VISIBLE);
                                }
                            })
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e("animation", "setupImagePositionOnShow end");
                                    setupStateAfterAnimation();
                                }
                            });
                }
            }, 100);
        }
    }

    private void setupImagePositionOnHide() {
        final FrameLayout layout = weakRefContainer.get();
        if(layout!=null){
            mGestureCropImageView.clearAnimation();
            mGestureCropImageView.setTranslationX(0);
            mGestureCropImageView.setTranslationY(0);
            mGestureCropImageView.setPivotX(0);
            mGestureCropImageView.setPivotY(0);
            mGestureCropImageView.setScaleX(1);
            mGestureCropImageView.setScaleY(1);

            mGestureCropImageView.animate()
                    .setDuration(1000)
                    .translationX(fromRect.left)
                    .translationY(fromRect.top)
                    .scaleX((float) fromRect.width() / imageSize)
                    .scaleY((float) fromRect.height() / imageSize)
                    .setStartDelay(100)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("animation", "setupImagePositionOnHide");
                            layout.setVisibility(View.GONE);
                            onFragmentResultUriListener.setResultUri(RESULT_CANCELED ,new Intent());
                        }
                    });
            toolbar.setAlpha(1);
            mWrapperStates.setAlpha(1);
            uCropShadow.setVisibility(View.GONE);
            imageLogo.setAlpha(1);
            mOverlayView.setAlpha(1);
            toolbar.animate()
                    .setDuration(ANIM_DURATION)
                    .alpha(0);
            mWrapperStates.animate()
                    .setDuration(ANIM_DURATION)
                    .alpha(0);
            imageLogo.animate()
                    .setDuration(ANIM_DURATION)
                    .alpha(0);
            mOverlayView.animate()
                    .setDuration(ANIM_DURATION)
                    .alpha(0);
            toolbar.animate().start();
            mWrapperStates.animate().start();
            background.setBackgroundColor(Color.TRANSPARENT);
            imageLogo.animate().start();
            mOverlayView.animate().start();
            mGestureCropImageView.animate().start();
        }
    }

    private void setupImagePositionOnHidePositive() {
        final FrameLayout layout = weakRefContainer.get();
        if (layout!=null){
            mGestureCropImageView.clearAnimation();
            mGestureCropImageView.setTranslationX(0);
            mGestureCropImageView.setTranslationY(0);
            mGestureCropImageView.setPivotX(0);
            mGestureCropImageView.setPivotY(0);
            mGestureCropImageView.setScaleX(1);
            mGestureCropImageView.setScaleY(1);

            mGestureCropImageView.animate()
                    .setDuration(ANIM_DURATION)
                    .translationX(getWidth())
                    .translationY(getHeight())
                    .scaleX((float) 0.001)
                    .scaleY((float) 0.001)
                    .setStartDelay(100)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("animation", "setupImagePositionOnHide");
                            //((RelativeLayout) view.findViewById(R.id.ucrop_photobox)).removeView(mBlockingView);
                            layout.setVisibility(View.GONE);
                        }
                    });
            toolbar.setAlpha(1);
            mWrapperStates.setAlpha(1);
            uCropShadow.setVisibility(View.GONE);
            imageLogo.setAlpha(1);
            mOverlayView.setAlpha(1);
            toolbar.animate()
                    .setDuration(ANIM_DURATION)
                    .alpha(0);
            mWrapperStates.animate()
                    .setDuration(ANIM_DURATION)
                    .alpha(0);
            imageLogo.animate()
                    .setDuration(ANIM_DURATION)
                    .alpha(0);
            mOverlayView.animate()
                    .setDuration(ANIM_DURATION)
                    .alpha(0);
            toolbar.animate().start();
            mWrapperStates.animate().start();
            background.setBackgroundColor(Color.TRANSPARENT);
            imageLogo.animate().start();
            mOverlayView.animate().start();
            mGestureCropImageView.animate().start();
        }
    }

    public void setupStateBeforeAnimation(){
        Log.e("qwe","setupStateBeforeAnimation");
        background.setBackgroundColor(Color.TRANSPARENT);
        uCropShadow.setVisibility(View.GONE);
        toolbar.setAlpha(0);
        mWrapperStates.setAlpha(0);
        imageLogo.setAlpha(0);
        mOverlayView.setAlpha(0);
    }

    public void setupStateAfterAnimation(){
        Log.e("qwe","setupStateAfterAnimation");
        toolbar.animate()
                .setDuration(ANIM_DURATION)
                .alpha(1);
        mWrapperStates.animate()
                .setDuration(ANIM_DURATION)
                .alpha(1);
        imageLogo.animate()
                .setDuration(ANIM_DURATION)
                .alpha(1);
        mOverlayView.animate()
                .setDuration(ANIM_DURATION)
                .alpha(1);
        toolbar.animate().start();
        mWrapperStates.animate().start();
        background.setBackgroundColor(mRootViewBackgroundColor);
        imageLogo.animate().start();
        uCropShadow.setVisibility(View.VISIBLE);
        mOverlayView.animate().start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e("onCreateView","onCreateView");
        startShowing = false;
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.ucrop_fragment, container, false);
        some();

        //mStateClickListener.onClick(mWrapperStateRotate);
//        mWrapperStateAspectRatio.setOnClickListener(null);
//        mWrapperStateRotate.setOnClickListener(null);
//        mWrapperStateScale.setOnClickListener(null);
//        mWrapperStateAspectRatio.setClickable(false);
//        mWrapperStateRotate.setClickable(false);
//        mWrapperStateScale.setClickable(false);
        //setupStateBeforeAnimation();
        return view;
    }

    private void setupAll(View view){
        mCropIntent = new Intent();
        Bundle mCropOptionsBundle = new Bundle();
        mCropOptionsBundle.putParcelable(EXTRA_INPUT_URI, source);
        mCropOptionsBundle.putParcelable(EXTRA_OUTPUT_URI, destination);
        mCropOptionsBundle.putFloat(UCrop.EXTRA_ASPECT_RATIO_X, 1);
        mCropOptionsBundle.putFloat(UCrop.EXTRA_ASPECT_RATIO_Y, 1);
        mCropIntent.putExtras(mCropOptionsBundle);
        imageSize = getResources().getDisplayMetrics().widthPixels;
        setupViews(mCropIntent, view);
        setImageData(mCropIntent);
        setInitialState();
        addBlockingView(view);
        mWrapperStateScale.setSelected(false);
        mWrapperStateScale.setBackgroundColor(getResources().getColor(R.color.ucrop_color_widget_background));
        mOverlayView.setCropGridColor(Color.TRANSPARENT);

        setRotateSetting();
    }

    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }


    private void setupViews(@NonNull Intent intent, View view) {
        mStatusBarColor = intent.getIntExtra(UCrop.Options.EXTRA_STATUS_BAR_COLOR, ContextCompat.getColor(getContext(), R.color.ucrop_color_statusbar));
        mToolbarColor = intent.getIntExtra(UCrop.Options.EXTRA_TOOL_BAR_COLOR, ContextCompat.getColor(getContext(), R.color.ucrop_color_toolbar));
        mActiveWidgetColor = intent.getIntExtra(UCrop.Options.EXTRA_UCROP_COLOR_WIDGET_ACTIVE, ContextCompat.getColor(getContext(), R.color.ucrop_color_widget_active));
        mToolbarWidgetColor = intent.getIntExtra(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, ContextCompat.getColor(getContext(), R.color.ucrop_color_toolbar_widget));
        mToolbarCancelDrawable = intent.getIntExtra(UCrop.Options.EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE, R.drawable.ucrop_ic_cross);
        mToolbarCropDrawable = intent.getIntExtra(UCrop.Options.EXTRA_UCROP_WIDGET_CROP_DRAWABLE, R.drawable.ucrop_ic_done);
        mToolbarTitle = intent.getStringExtra(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR);
        mToolbarTitle = mToolbarTitle != null ? mToolbarTitle : getResources().getString(R.string.ucrop_label_edit_photo);
        mLogoColor = intent.getIntExtra(UCrop.Options.EXTRA_UCROP_LOGO_COLOR, ContextCompat.getColor(getContext(), R.color.ucrop_color_default_logo));
        mShowBottomControls = !intent.getBooleanExtra(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS, false);
        mRootViewBackgroundColor = intent.getIntExtra(UCrop.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR, ContextCompat.getColor(getContext(), R.color.ucrop_color_crop_background));

        setupAppBar(view);
        initiateRootViews(view);

        if (mShowBottomControls) {
            ViewGroup photoBox = (ViewGroup) view.findViewById(R.id.ucrop_photobox);
            View.inflate(getContext(), R.layout.ucrop_controls, photoBox);

            mWrapperStateAspectRatio = (ViewGroup) view.findViewById(R.id.state_aspect_ratio);
            mWrapperStateAspectRatio.setOnClickListener(mStateClickListener);
            mWrapperStateRotate = (ViewGroup) view.findViewById(R.id.state_rotate);
            mWrapperStateRotate.setOnClickListener(mStateClickListener);
            mWrapperStateScale = (ViewGroup) view.findViewById(R.id.state_scale);
            mWrapperStateScale.setOnClickListener(mStateClickListener);

            mLayoutAspectRatio = (ViewGroup) view.findViewById(R.id.layout_aspect_ratio);
            mLayoutRotate = (ViewGroup) view.findViewById(R.id.layout_rotate_wheel);
            mLayoutScale = (ViewGroup) view.findViewById(R.id.layout_scale_wheel);
            mLayoutRotate.setVisibility(View.GONE);
            mLayoutScale.setVisibility(View.GONE);
            mWrapperControls = (FrameLayout)view.findViewById(R.id.wrapper_controls);
            mWrapperStates = (LinearLayout)view.findViewById(R.id.wrapper_states);
            mWrapperControls.setVisibility(View.GONE);
            uCropShadow = (ImageView)view.findViewById(R.id.ucrop_shadow);

            setupAspectRatioWidget(intent, view);
            setupRotateWidget(view);
            setupScaleWidget(view);
            setupStatesWrapper(view);

        }
    }

    private void setupAppBar(View view) {
        //setStatusBarColor(mStatusBarColor);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);

        // Set all of the Toolbar coloring
        toolbar.setBackgroundColor(mToolbarColor);
        toolbar.setTitleTextColor(mToolbarWidgetColor);

        final TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setTextColor(mToolbarWidgetColor);
        toolbarTitle.setText(mToolbarTitle);

        final ImageView imageDone = (ImageView) view.findViewById(R.id.image_done);
        Drawable ic_done = getResources().getDrawable( R.drawable.ucrop_ic_done);
        if (ic_done != null) {
            ic_done.mutate();
            ic_done.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
            imageDone.setImageDrawable(ic_done);
        }

        ImageView imageCancel = (ImageView) view.findViewById(R.id.image_cancel);
        imageCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupImagePositionOnHide();
            }
        });

        //toolbar.setVisibility(View.GONE);

//        final ImageView imageLoader = (ImageView) view.findViewById(R.id.image_load);
//        Drawable ic_load = getResources().getDrawable( R.drawable.ucrop_vector_loader_animated);
//        if (ic_load != null) {
//            try {
//                ic_load.mutate();
//                ic_load.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
//                imageLoader.setImageDrawable(ic_load);
//            } catch (IllegalStateException e) {
//                Log.i(TAG, String.format("%s - %s", e.getMessage(), getString(R.string.ucrop_mutate_exception_hint)));
//            }
//            ((Animatable) ic_load).start();
//            imageLoader.setVisibility(View.GONE);
//        }

        imageDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("click","click");
                cropAndSaveImage();


            }
        });

         //Color buttons inside the Toolbar
//        Drawable stateButtonDrawable = ContextCompat.getDrawable(getContext(), mToolbarCancelDrawable).mutate();
//        stateButtonDrawable.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
//        toolbar.setNavigationIcon(stateButtonDrawable);
//
//        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
//        final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayShowTitleEnabled(false);
//        }
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.ucrop_menu_activity, menu);
//        MenuItem menuItemLoader = menu.findItem(R.id.menu_loader);
//        Drawable menuItemLoaderIcon = menuItemLoader.getIcon();
//        if (menuItemLoaderIcon != null) {
//            try {
//                menuItemLoaderIcon.mutate();
//                menuItemLoaderIcon.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
//                menuItemLoader.setIcon(menuItemLoaderIcon);
//            } catch (IllegalStateException e) {
//                Log.i(TAG, String.format("%s - %s", e.getMessage(), getString(R.string.ucrop_mutate_exception_hint)));
//            }
//            ((Animatable) menuItemLoader.getIcon()).start();
//        }
//
//        MenuItem menuItemCrop = menu.findItem(R.id.menu_crop);
//        Drawable menuItemCropIcon = ContextCompat.getDrawable(getContext(), mToolbarCropDrawable);
//        if (menuItemCropIcon != null) {
//            menuItemCropIcon.mutate();
//            menuItemCropIcon.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
//            menuItemCrop.setIcon(menuItemCropIcon);
//        }
//        super.onCreateOptionsMenu(menu,inflater);
//    }



    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_crop).setVisible(!mShowLoader);
        menu.findItem(R.id.menu_loader).setVisible(mShowLoader);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_crop) {



            cropAndSaveImage();
        } else if (item.getItemId() == android.R.id.home) {
            //onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private final View.OnClickListener mStateClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //if (!v.isSelected()) {
                setWidgetState(v.getId());
            //}
        }
    };

    private void setWidgetState(@IdRes int stateViewId) {
        if (!mShowBottomControls) return;
        if(stateViewId == R.id.state_scale){
            if(mWrapperStateScale.isSelected()){
                //isSelectCropButton = false;
                mWrapperStateScale.setSelected(false);
                mWrapperStateScale.setBackgroundColor(getResources().getColor(R.color.ucrop_color_widget_background));
                mOverlayView.setCropGridColor(Color.TRANSPARENT);
                mOverlayView.invalidate();
            } else {
                mWrapperStateScale.setSelected(true);
                mWrapperStateScale.setBackgroundColor(getResources().getColor(R.color.ucrop_color_press_button));
                mOverlayView.setCropGridColor(getResources().getColor(R.color.ucrop_color_default_crop_grid));
                mOverlayView.invalidate();
            }

        }
        mWrapperStateAspectRatio.setSelected(stateViewId == R.id.state_aspect_ratio);
        //mWrapperStateRotate.setSelected(stateViewId == R.id.state_rotate);


        mLayoutAspectRatio.setVisibility(stateViewId == R.id.state_aspect_ratio ? View.VISIBLE : View.GONE);
        //mLayoutRotate.setVisibility(stateViewId == R.id.state_rotate ? View.VISIBLE : View.GONE);
        //mWrapperControls.setVisibility(stateViewId == R.id.state_rotate ? View.VISIBLE : View.GONE);
 //       mLayoutScale.setVisibility(stateViewId == R.id.state_scale ? View.VISIBLE : View.GONE);
        //mLayoutAspectRatio.setVisibility(View.GONE);
        //mLayoutRotate.setVisibility(View.GONE);
        //mLayoutScale.setVisibility(View.GONE);

//        if (stateViewId == R.id.state_scale) {
//            setAllowedGestures(2);
//        } else if (stateViewId == R.id.state_rotate) {
//            //setAllowedGestures(1);
//            mWrapperStateScale.setSelected(false);
//            rotateByAngle(90);
//        } else {
//            setAllowedGestures(2);
//        }

        if (stateViewId == R.id.state_scale) {
            if(mWrapperStateScale.isSelected()){

                //mOverlayView.setCropGridColor(getResources().getColor(R.color.ucrop_color_default_crop_grid));
                setAllowedGestures(2);
            } else {

                setRotateSetting();
            }
        } else {
            //setAllowedGestures(1);
            mOverlayView.setCropGridColor(Color.TRANSPARENT);
            mOverlayView.invalidate();
            setRotateSetting();
            mWrapperStateScale.setSelected(false);
            rotateByAngle(90);
        }
    }

    private void setAllowedGestures(int tab) {
        mGestureCropImageView.setScaleEnabled(mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == SCALE);
        mGestureCropImageView.setRotateEnabled(mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == ROTATE);
    }

    private void setRotateSetting(){
        mGestureCropImageView.setScaleEnabled(false);
        mGestureCropImageView.setRotateEnabled(false);
    }

    private void setupAspectRatioWidget(@NonNull Intent intent, View view) {

        int aspectRationSelectedByDefault = intent.getIntExtra(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0);
        ArrayList<AspectRatio> aspectRatioList = intent.getParcelableArrayListExtra(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS);

        if (aspectRatioList == null || aspectRatioList.isEmpty()) {
            aspectRationSelectedByDefault = 2;

            aspectRatioList = new ArrayList<>();
            aspectRatioList.add(new AspectRatio(null, 1, 1));
            aspectRatioList.add(new AspectRatio(null, 3, 4));
            aspectRatioList.add(new AspectRatio(getString(R.string.ucrop_label_original).toUpperCase(),
                    CropImageView.SOURCE_IMAGE_ASPECT_RATIO, CropImageView.SOURCE_IMAGE_ASPECT_RATIO));
            aspectRatioList.add(new AspectRatio(null, 3, 2));
            aspectRatioList.add(new AspectRatio(null, 16, 9));
        }

        LinearLayout wrapperAspectRatioList = (LinearLayout) view.findViewById(R.id.layout_aspect_ratio);

        FrameLayout wrapperAspectRatio;
        AspectRatioTextView aspectRatioTextView;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.weight = 1;
        for (AspectRatio aspectRatio : aspectRatioList) {
            wrapperAspectRatio = (FrameLayout) getLayoutInflater(Bundle.EMPTY).inflate(R.layout.ucrop_aspect_ratio, null);
            wrapperAspectRatio.setLayoutParams(lp);
            aspectRatioTextView = ((AspectRatioTextView) wrapperAspectRatio.getChildAt(0));
            aspectRatioTextView.setActiveColor(mActiveWidgetColor);
            aspectRatioTextView.setAspectRatio(aspectRatio);

            wrapperAspectRatioList.addView(wrapperAspectRatio);
            mCropAspectRatioViews.add(wrapperAspectRatio);
        }

        mCropAspectRatioViews.get(aspectRationSelectedByDefault).setSelected(true);

        for (ViewGroup cropAspectRatioView : mCropAspectRatioViews) {
            cropAspectRatioView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mGestureCropImageView.setTargetAspectRatio(
                            ((AspectRatioTextView) ((ViewGroup) v).getChildAt(0)).getAspectRatio(v.isSelected()));
                    if (!v.isSelected()) {
                        for (ViewGroup cropAspectRatioView : mCropAspectRatioViews) {
                            cropAspectRatioView.setSelected(cropAspectRatioView == v);
                        }
                    }
                }
            });
        }
    }

    private void setupRotateWidget(View view) {
        mTextViewRotateAngle = ((TextView) view.findViewById(R.id.text_view_rotate));
        mTextViewRotateAngle.setVisibility(View.GONE);
        ((HorizontalProgressWheelView) view.findViewById(R.id.rotate_scroll_wheel)).setVisibility(View.GONE);
        ((HorizontalProgressWheelView) view.findViewById(R.id.rotate_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        mGestureCropImageView.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT);
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });

        ((HorizontalProgressWheelView) view.findViewById(R.id.rotate_scroll_wheel)).setMiddleLineColor(mActiveWidgetColor);


        view.findViewById(R.id.wrapper_reset_rotate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetRotation();
            }
        });
        view.findViewById(R.id.wrapper_rotate_by_angle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateByAngle(90);
            }
        });
    }

    private void resetRotation() {
        mGestureCropImageView.postRotate(-mGestureCropImageView.getCurrentAngle());
        mGestureCropImageView.setupInitialImagePosition(mGestureCropImageView.getImageWeight(),mGestureCropImageView.getImageHeight());
        mGestureCropImageView.setImageToWrapCropBounds();
    }

    private void rotateByAngle(int angle) {
        mGestureCropImageView.postRotate(angle);
        mGestureCropImageView.zoomInImage(mGestureCropImageView.getCurrentScale());
        mGestureCropImageView.setImageToWrapCropBounds();
    }

    private void setupScaleWidget(View view) {
        mTextViewScalePercent = ((TextView) view.findViewById(R.id.text_view_scale));
        mTextViewScalePercent.setVisibility(View.GONE);
        ((HorizontalProgressWheelView) view.findViewById(R.id.scale_scroll_wheel)).setVisibility(View.GONE);
        ((HorizontalProgressWheelView) view.findViewById(R.id.scale_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        if (delta > 0) {
                            mGestureCropImageView.zoomInImage(mGestureCropImageView.getCurrentScale()
                                    + delta * ((mGestureCropImageView.getMaxScale() - mGestureCropImageView.getMinScale()) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT));
                        } else {
                            mGestureCropImageView.zoomOutImage(mGestureCropImageView.getCurrentScale()
                                    + delta * ((mGestureCropImageView.getMaxScale() - mGestureCropImageView.getMinScale()) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT));
                        }
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });
        ((HorizontalProgressWheelView) view.findViewById(R.id.scale_scroll_wheel)).setMiddleLineColor(mActiveWidgetColor);
    }

    private void setupStatesWrapper(View view) {
        ImageView stateScaleImageView = (ImageView) view.findViewById(R.id.image_view_state_scale);
        ImageView stateRotateImageView = (ImageView) view.findViewById(R.id.image_view_state_rotate);
        ImageView stateAspectRatioImageView = (ImageView) view.findViewById(R.id.image_view_state_aspect_ratio);

        //stateScaleImageView.setImageDrawable(new SelectedStateListDrawable(stateScaleImageView.getDrawable(), mActiveWidgetColor));
        stateScaleImageView.setImageDrawable(new SelectedStateListDrawable(stateAspectRatioImageView.getDrawable(), mActiveWidgetColor));
        stateRotateImageView.setImageDrawable(new SelectedStateListDrawable(stateRotateImageView.getDrawable(), mActiveWidgetColor));
        stateAspectRatioImageView.setImageDrawable(new SelectedStateListDrawable(stateAspectRatioImageView.getDrawable(), mActiveWidgetColor));
        //stateScaleImageView.setVisibility(View.GONE);
        //stateRotateImageView.setVisibility(View.GONE);
        //stateAspectRatioImageView.setVisibility(View.GONE);
    }

    private void initiateRootViews(View view) {
        mUCropView = (UCropView) view.findViewById(R.id.ucrop);
        mGestureCropImageView = mUCropView.getCropImageView();
        mOverlayView = mUCropView.getOverlayView();
        mGestureCropImageView.setTransformImageListener(mImageListener);
        imageLogo = (ImageView) view.findViewById(R.id.image_view_logo);
        imageLogo.setColorFilter(mLogoColor, PorterDuff.Mode.SRC_ATOP);
        background = (FrameLayout) view.findViewById(R.id.ucrop_frame);
        background.setBackgroundColor(Color.TRANSPARENT);
    }



    private TransformImageView.TransformImageListener mImageListener = new TransformImageView.TransformImageListener() {
        @Override
        public void onRotate(float currentAngle) {
            setAngleText(currentAngle);
        }

        @Override
        public void onScale(float currentScale) {
            setScaleText(currentScale);
        }

        @Override
        public void onLoadComplete() {
            mUCropView.animate().alpha(1).setDuration(300).setInterpolator(new AccelerateInterpolator());
            mBlockingView.setClickable(false);
            mShowLoader = false;
            getActivity().supportInvalidateOptionsMenu();
        }

        @Override
        public void onLoadFailure(@NonNull Exception e) {
            setResultError(e);
            finish();
        }

    };

    protected void setResultError(Throwable throwable) {
//        setResult(UCrop.RESULT_ERROR, new Intent().putExtra(UCrop.EXTRA_ERROR, throwable));
    }

    private void setAngleText(float angle) {
        if (mTextViewRotateAngle != null) {
            mTextViewRotateAngle.setText(String.format(Locale.getDefault(), "%.1f°", angle));
        }
    }

    private void setScaleText(float scale) {
        if (mTextViewScalePercent != null) {
            mTextViewScalePercent.setText(String.format(Locale.getDefault(), "%d%%", (int) (scale * 100)));
        }
    }

    private void setImageData(@NonNull Intent intent) {
        Uri inputUri = intent.getParcelableExtra(UCrop.EXTRA_INPUT_URI);
        Uri outputUri = intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI);
        processOptions(intent);

        if (inputUri != null && outputUri != null) {
            try {
                mGestureCropImageView.setImageUri(inputUri, outputUri);
            } catch (Exception e) {
                setResultError(e);
                finish();
            }
        } else {
            setResultError(new NullPointerException(getString(R.string.ucrop_error_input_data_is_absent)));
            finish();
        }
    }

    private void processOptions(@NonNull Intent intent) {
        // Bitmap compression options
        String compressionFormatName = intent.getStringExtra(UCrop.Options.EXTRA_COMPRESSION_FORMAT_NAME);
        Bitmap.CompressFormat compressFormat = null;
        if (!TextUtils.isEmpty(compressionFormatName)) {
            compressFormat = Bitmap.CompressFormat.valueOf(compressionFormatName);
        }
        mCompressFormat = (compressFormat == null) ? DEFAULT_COMPRESS_FORMAT : compressFormat;

        mCompressQuality = intent.getIntExtra(UCrop.Options.EXTRA_COMPRESSION_QUALITY, UCropActivity.DEFAULT_COMPRESS_QUALITY);

        // Gestures options
        int[] allowedGestures = intent.getIntArrayExtra(UCrop.Options.EXTRA_ALLOWED_GESTURES);
        if (allowedGestures != null && allowedGestures.length == TABS_COUNT) {
            mAllowedGestures = allowedGestures;
        }

        // Crop image view options
        mGestureCropImageView.setMaxBitmapSize(intent.getIntExtra(UCrop.Options.EXTRA_MAX_BITMAP_SIZE, CropImageView.DEFAULT_MAX_BITMAP_SIZE));
        mGestureCropImageView.setMaxScaleMultiplier(intent.getFloatExtra(UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER, CropImageView.DEFAULT_MAX_SCALE_MULTIPLIER));
        mGestureCropImageView.setImageToWrapCropBoundsAnimDuration(intent.getIntExtra(UCrop.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION, CropImageView.DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION));

        // Overlay view options
        mOverlayView.setFreestyleCropEnabled(intent.getBooleanExtra(UCrop.Options.EXTRA_FREE_STYLE_CROP, OverlayView.DEFAULT_FREESTYLE_CROP_MODE != OverlayView.FREESTYLE_CROP_MODE_DISABLE));

        mOverlayView.setDimmedColor(intent.getIntExtra(UCrop.Options.EXTRA_DIMMED_LAYER_COLOR, getResources().getColor(R.color.ucrop_color_default_dimmed)));
        mOverlayView.setCircleDimmedLayer(intent.getBooleanExtra(UCrop.Options.EXTRA_CIRCLE_DIMMED_LAYER, OverlayView.DEFAULT_CIRCLE_DIMMED_LAYER));

        mOverlayView.setShowCropFrame(intent.getBooleanExtra(UCrop.Options.EXTRA_SHOW_CROP_FRAME, OverlayView.DEFAULT_SHOW_CROP_FRAME));
        mOverlayView.setCropFrameColor(intent.getIntExtra(UCrop.Options.EXTRA_CROP_FRAME_COLOR, getResources().getColor(R.color.ucrop_color_default_crop_frame)));
        mOverlayView.setCropFrameStrokeWidth(intent.getIntExtra(UCrop.Options.EXTRA_CROP_FRAME_STROKE_WIDTH, getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width)));

        mOverlayView.setShowCropGrid(intent.getBooleanExtra(UCrop.Options.EXTRA_SHOW_CROP_GRID, OverlayView.DEFAULT_SHOW_CROP_GRID));
        mOverlayView.setCropGridRowCount(intent.getIntExtra(UCrop.Options.EXTRA_CROP_GRID_ROW_COUNT, OverlayView.DEFAULT_CROP_GRID_ROW_COUNT));
        mOverlayView.setCropGridColumnCount(intent.getIntExtra(UCrop.Options.EXTRA_CROP_GRID_COLUMN_COUNT, OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT));
        mOverlayView.setCropGridColor(intent.getIntExtra(UCrop.Options.EXTRA_CROP_GRID_COLOR, getResources().getColor(R.color.ucrop_color_default_crop_grid)));
        mOverlayView.setCropGridStrokeWidth(intent.getIntExtra(UCrop.Options.EXTRA_CROP_GRID_STROKE_WIDTH, getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width)));

        // Aspect ratio options
        float aspectRatioX = intent.getFloatExtra(UCrop.EXTRA_ASPECT_RATIO_X, 0);
        float aspectRatioY = intent.getFloatExtra(UCrop.EXTRA_ASPECT_RATIO_Y, 0);

        int aspectRationSelectedByDefault = intent.getIntExtra(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0);
        ArrayList<AspectRatio> aspectRatioList = intent.getParcelableArrayListExtra(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS);

        if (aspectRatioX > 0 && aspectRatioY > 0) {
            if (mWrapperStateAspectRatio != null) {
                mWrapperStateAspectRatio.setVisibility(View.GONE);
            }
            mGestureCropImageView.setTargetAspectRatio(aspectRatioX / aspectRatioY);
        } else if (aspectRatioList != null && aspectRationSelectedByDefault < aspectRatioList.size()) {
            mGestureCropImageView.setTargetAspectRatio(aspectRatioList.get(aspectRationSelectedByDefault).getAspectRatioX() /
                    aspectRatioList.get(aspectRationSelectedByDefault).getAspectRatioY());
        } else {
            mGestureCropImageView.setTargetAspectRatio(CropImageView.SOURCE_IMAGE_ASPECT_RATIO);
        }

        // Result bitmap max size options
        int maxSizeX = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_X, 0);
        int maxSizeY = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_Y, 0);

        if (maxSizeX > 0 && maxSizeY > 0) {
            mGestureCropImageView.setMaxResultImageSizeX(maxSizeX);
            mGestureCropImageView.setMaxResultImageSizeY(maxSizeY);
        }
    }

    private void setInitialState() {
        if (mShowBottomControls) {
            if (mWrapperStateAspectRatio.getVisibility() == View.VISIBLE) {
                setWidgetState(R.id.state_aspect_ratio);
            } else {
                setWidgetState(R.id.state_scale);
            }
        } else {
            setAllowedGestures(0);
        }
    }

    private void addBlockingView(View view) {
        if (mBlockingView == null) {
            mBlockingView = new View(getContext());
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.addRule(RelativeLayout.BELOW, R.id.toolbar);
            mBlockingView.setLayoutParams(lp);
            mBlockingView.setClickable(true);
        }

        ((RelativeLayout) view.findViewById(R.id.ucrop_photobox)).addView(mBlockingView);
    }

    private void finish(){
        //Toast.makeText(getContext(),"Finish",Toast.LENGTH_SHORT).show();
    }

    protected void cropAndSaveImage() {
        mBlockingView.setClickable(true);
        mShowLoader = true;
        getActivity().supportInvalidateOptionsMenu();

        mGestureCropImageView.cropAndSaveImage(mCompressFormat, mCompressQuality, new BitmapCropCallback() {

            @Override
            public void onBitmapCropped(@NonNull Uri resultUri, int offsetX, int offsetY, int imageWidth, int imageHeight) {
                setResultUri(resultUri, mGestureCropImageView.getTargetAspectRatio(), offsetX, offsetY, imageWidth, imageHeight);
                finish();
                setupImagePositionOnHidePositive();
            }

            @Override
            public void onCropFailure(@NonNull Throwable t) {
                setResultError(t);
                finish();
            }
        });
    }

    protected void setResultUri(Uri uri, float resultAspectRatio, int offsetX, int offsetY, int imageWidth, int imageHeight) {
        onFragmentResultUriListener.setResultUri(RESULT_OK, new Intent()
                .putExtra(UCrop.EXTRA_OUTPUT_URI, uri)
                .putExtra(UCrop.EXTRA_OUTPUT_CROP_ASPECT_RATIO, resultAspectRatio)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, offsetX)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, offsetY)
        );

    }

    public interface OnFragmentResultUriListener{
        void setResultUri(int resultCode, Intent data);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentResultUriListener) {
            onFragmentResultUriListener = (OnFragmentResultUriListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentResultUriListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onFragmentResultUriListener = null;
    }

    private void some() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
    }

    private int getWidth(){
        return width-(width/3/3*2);
    }

    private int getHeight(){
        return height-(height/9);
    }

    public void onBackPressed(){
        setupImagePositionOnHide();
    }
}

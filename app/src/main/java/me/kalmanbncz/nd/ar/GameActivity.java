package me.kalmanbncz.nd.ar;

import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import me.kalmanbncz.nd.R;

public class GameActivity extends AndroidApplication {

    public static final int LIST_MODE = 2;

    protected FrameLayout main;

    private Renderer renderer;

    private LinearLayout settingsList;

    private RelativeLayout buttonsHolder;

    private int settingsMode = 0;

    private Resources res;

    private ImageButton resetButton;

    private android.widget.ProgressBar progressBar;

    private Thread loaderThread;


    @Override
    protected void onResume() {
        super.onResume();
        initialize();
    }

    @Override
    protected void onDestroy() {
        Assets.dispose();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (loaderThread != null && loaderThread.isAlive())
            loaderThread.interrupt();
        super.onPause();
    }

    private void initialize() {
        res = getResources();
        settingsList = (LinearLayout) findViewById(R.id.settings_list);
        buttonsHolder = (RelativeLayout) findViewById(R.id.settings_button_holder);
        buttonsHolder.bringToFront();
        resetButton = (ImageButton) findViewById(R.id.reset_button);
//        loaderThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Assets.create();
//                while (Assets.loading) {
//                    if (Assets.manager.update()) {
//                        GameActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(GameActivity.this, "loaded", Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                        progressBar.setProgress((int) Assets.manager.getProgress());
//                        break;
//                    }
//                    progressBar.setProgress((int) Assets.manager.getProgress());
//                }
//            }
//        });
//        loaderThread.start();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        renderer = new Renderer(this, progressBar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.r = 8;
        cfg.g = 8;
        cfg.b = 8;
        cfg.a = 8;

        View view = initializeForView(renderer, cfg);

        if (graphics.getView() instanceof SurfaceView) {
            SurfaceView glView = (SurfaceView) graphics.getView();
            glView.getHolder().setFormat(PixelFormat.RGBA_8888);
        }

        main = (FrameLayout) findViewById(R.id.main_layout);
        main.addView(new CameraPreviewSurfaceView(getApplicationContext()));
        main.addView(view);

        if (view instanceof SurfaceView) {
            ((SurfaceView) view).setZOrderMediaOverlay(true);
        }
        view.bringToFront();
        view.setOnTouchListener(renderer);
    }

    public void handleItemsClick(View v) {
        switch (v.getId()) {
            case R.id.settings_button:
                settingsList.setVisibility(settingsList.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                settingsMode = LIST_MODE;
                break;
            case R.id.close_button:
                if (settingsMode == LIST_MODE) {
                    settingsList.setVisibility(View.GONE);
                    settingsMode = 0;
                } else {
                    settingsList.setVisibility(View.VISIBLE);
                    settingsMode = LIST_MODE;
                }
                break;
            case R.id.reset_button:
                Renderer.debug = !Renderer.debug;
                break;
        }
    }
}

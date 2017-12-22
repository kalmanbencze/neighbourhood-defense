package me.kalmanbncz.nd.ar;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import me.kalmanbncz.nd.processing.ImageProcessor;


public class CameraPreviewSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static Mat staticMat;

    public static int width;

    public static int height;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    private final SurfaceHolder mSurfaceHolder;

    private final ImageProcessor processor;

    private Camera.Parameters parameters;

    private PipedInputStream in;

    private PipedOutputStream out;

    private Camera mCamera;

    private int bytesLength = -1;

    private Mat mYuv;


    public CameraPreviewSurfaceView(final Context pContext) {
        super(pContext);

        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        processor = ImageProcessor.getInstance();
    }

    public void surfaceCreated(final SurfaceHolder pSurfaceHolder) {
        // camera setup
        mCamera = Camera.open();

//        parameters = mCamera.getParameters();
//        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
//        List<int[]> fps = parameters.getSupportedPreviewFpsRange();
//        int[] fpsGood = fps.get(fps.size()-1);
//
//        // change preview size
//        final Camera.Size cs = sizes.get(0);
//        width = cs.width;
//        height = cs.height;
        mYuv = new Mat(height, width, CvType.CV_8UC4);
//        parameters.setPreviewSize(cs.width, cs.height);
//        parameters.setPreviewFpsRange(fpsGood[0], fpsGood[1]);
//        parameters.setPreviewFormat(ImageFormat.NV21);
//
//        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
//
//        if (parameters.isAutoExposureLockSupported()){
//            parameters.setAutoExposureLock(false);
//            parameters.setExposureCompensation(parameters.getMaxExposureCompensation());
//        }
//
//
//
//
//
//        mCamera.setParameters(parameters);

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            this.mCamera.setPreviewCallback(this);
            mCamera.startPreview();

        } catch (IOException e) {
            mCamera.stopPreview();
            mCamera.release();
        }

        /*try {
            out = new PipedOutputStream();
            in = new PipedInputStream(out, 5);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        /*Thread reader = new Thread() {

            @Override
            public void run() {
                byte[] readByte = new byte[1];
                try {
                    while (in.available() != 0) {
                        int a = in.read(readByte, 0, bytesLength);
                        process(readByte);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };*/

        //reader.start();

    }

    private void process(Mat readByte) {
//        processor.process(readByte);
    }

    public void surfaceDestroyed(final SurfaceHolder pSurfaceHolder) {
        this.mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        this.mCamera.release();
        this.mCamera = null;
    }

    public void surfaceChanged(final SurfaceHolder pSurfaceHolder, final int pPixelFormat, final int pWidth,
                               final int pHeight) {
        final Camera.Parameters parameters = this.mCamera.getParameters();
        final List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();

        final Camera.Size previewSize = previewSizes.get(0);

        parameters.setPreviewSize(previewSize.width, previewSize.height);

        this.mCamera.setParameters(parameters);
        this.mCamera.startPreview();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

//        YuvImage im = new YuvImage(bytes, ImageFormat.NV21, 640,
//                480, null);
//        Rect r = new Rect(0,0,640,480);
//        if (bytesLength == -1) {
//            bytesLength = bytes.length;
//        }
//        try {
//            out.write(bytes, 0, bytesLength);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        mYuv.put(0, 0, bytes);
        process(mYuv);


//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        im.compressToJpeg(r, parameters.getJpegQuality(), baos);

//        Objdetect o = new Objdetect();
//        String strData = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
//
//        mPreview.updateMatrix();

    }

}
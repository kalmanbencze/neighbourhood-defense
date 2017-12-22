package me.kalmanbncz.nd.processing;

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import android.util.Log;
import me.kalmanbncz.nd.ar.CameraPreviewSurfaceView;


/**
 * Created by Kali on 11/22/2014 .
 */
public class ImageProcessor {


    public static final int VIEW_MODE_CANNY = 0;

    public static final int VIEW_MODE_HOUGHLINES = 1;

    private static final int VIEW_MODE_GFTT = 2;

    private static final int VIEW_MODE_OPFLOW = 3;

    public static ImageProcessor instance;

    private SizedStack<Mat> imageStack = new SizedStack<>(6);

    private Point pt, pt1, pt2;

    private List<Byte> byteStatus;

    private List<Point> corners, cornersThis, cornersPrev;

    private Mat mRgba, mGray, mIntermediateMat,
            lines, matOpFlowPrev, matOpFlowThis;

    private MatOfFloat mMOFerr;

    private MatOfByte mMOBStatus;

    private MatOfPoint2f mMOP2fptsPrev, mMOP2fptsThis, mMOP2fptsSafe;

    private MatOfPoint MOPcorners;

    private Scalar colorRed;

    private Size sSize5;

    private int processMode = 1;

    private org.opencv.core.Size mSize, mSizeLarge;

    ImageProcessor() {
        Log.d("ImageProcessor", "image processor");

        mRgba = new Mat(CameraPreviewSurfaceView.height / 2, CameraPreviewSurfaceView.width / 2, CvType.CV_8UC4);
        mIntermediateMat = new Mat(CameraPreviewSurfaceView.height, CameraPreviewSurfaceView.width, CvType.CV_8UC4);

        byteStatus = new ArrayList<>();
        colorRed = new Scalar(255, 0, 0, 255);
        corners = new ArrayList<>();
        cornersThis = new ArrayList<>();
        cornersPrev = new ArrayList<>();
        lines = new Mat();

        mGray = new Mat();
        mMOP2fptsPrev = new MatOfPoint2f();
        mMOP2fptsThis = new MatOfPoint2f();
        mMOP2fptsSafe = new MatOfPoint2f();
        mMOFerr = new MatOfFloat();
        mMOBStatus = new MatOfByte();
        MOPcorners = new MatOfPoint();
        matOpFlowThis = new Mat();
        matOpFlowPrev = new Mat();

        mSize = new Size(640, 360);
        mSizeLarge = new Size(1920, 1080);

        pt = new Point(0, 0);
        pt1 = new Point(0, 0);
        pt2 = new Point(0, 0);

//        sMatSize = new Size();
        sSize5 = new Size(5, 5);
    }

    public static ImageProcessor getInstance() {
        if (instance == null) {
            instance = new ImageProcessor();
        }
        return instance;
    }

    public void addToStack(Mat img) {
        imageStack.push(img);
    }

    public Mat process(Mat img) {
        int iCannyLowerThreshold;
        int iCannyUpperThreshold;

        iCannyLowerThreshold = 45;
        iCannyUpperThreshold = 75;
        //img.copyTo(mRgba);
        Imgproc.resize(img, mRgba, mSize);

        switch (processMode) {
            case VIEW_MODE_CANNY:
                iCannyLowerThreshold = 35;
                iCannyUpperThreshold = 75;
            case VIEW_MODE_HOUGHLINES:

                Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

                // doing a gaussian blur prevents getting a lot of false hits
                Imgproc.GaussianBlur(mGray, mGray, sSize5, 2, 2);

                // the lower this figure the more spurious circles you get
                // 50 upper looks good in CANNY, but 75 is better when converting that into Hough circles

                Imgproc.Canny(mGray, mGray, iCannyLowerThreshold, iCannyUpperThreshold);

                int iHoughLinesThreshold = 80;
                int iHoughLinesMinLineSize = 10;
                int iHoughLinesGap = 10;
                Imgproc.HoughLinesP(mGray, lines, 1, Math.PI / 180, iHoughLinesThreshold, iHoughLinesMinLineSize, iHoughLinesGap);

                for (int x = 0; x < Math.min(lines.cols(), 40); x++) {
                    double[] vecHoughLines = lines.get(0, x);

                    if (vecHoughLines.length == 0)
                        break;

                    double x1 = vecHoughLines[0];
                    double y1 = vecHoughLines[1];
                    double x2 = vecHoughLines[2];
                    double y2 = vecHoughLines[3];

                    pt1.x = x1;
                    pt1.y = y1;
                    pt2.x = x2;
                    pt2.y = y2;

                    Core.line(mGray, pt1, pt2, colorRed, 1);
                }
                break;
            case VIEW_MODE_GFTT:
                Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

                // DON'T do a gaussian blur here, it makes the results poorer and
                // takes 0.5 off the fps rate

                int iGFFTMax = 40;
                Imgproc.goodFeaturesToTrack(mGray, MOPcorners, iGFFTMax, 0.01, 20);

                int y = MOPcorners.rows();

                corners = MOPcorners.toList();

                int iLineThickness = 3;
                for (int x = 0; x < y; x++)
                    Core.circle(mRgba, corners.get(x), 6, colorRed, iLineThickness - 1);
                //DrawCross (mRgba, colorRed, corners.get(x));
                break;
            case VIEW_MODE_OPFLOW:

                iGFFTMax = 40;
                if (mMOP2fptsPrev.rows() == 0) {
                    //Log.d("Baz", "First time opflow");
                    // first time through the loop so we need prev and this mats
                    // plus prev points
                    // get this mat
                    Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

                    // copy that to prev mat
                    matOpFlowThis.copyTo(matOpFlowPrev);

                    // get prev corners
                    Imgproc.goodFeaturesToTrack(matOpFlowPrev, MOPcorners, iGFFTMax, 0.05, 20);
                    mMOP2fptsPrev.fromArray(MOPcorners.toArray());

                    // get safe copy of this corners
                    mMOP2fptsPrev.copyTo(mMOP2fptsSafe);
                } else {
                    //Log.d("Baz", "Opflow");
                    // we've been through before so
                    // this mat is valid. Copy it to prev mat
                    matOpFlowThis.copyTo(matOpFlowPrev);

                    // get this mat
                    Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

                    // get the corners for this mat
                    Imgproc.goodFeaturesToTrack(matOpFlowThis, MOPcorners, iGFFTMax, 0.05, 20);
                    mMOP2fptsThis.fromArray(MOPcorners.toArray());

                    // retrieve the corners from the prev mat
                    // (saves calculating them again)
                    mMOP2fptsSafe.copyTo(mMOP2fptsPrev);

                    // and save this corners for next time through

                    mMOP2fptsThis.copyTo(mMOP2fptsSafe);
                }


           	/*
               Parameters:
           		prevImg first 8-bit input image
           		nextImg second input image
           		prevPts vector of 2D points for which the flow needs to be found; point coordinates must be single-precision floating-point numbers.
           		nextPts output vector of 2D points (with single-precision floating-point coordinates) containing the calculated new positions of input features in the second
           		image; when OPTFLOW_USE_INITIAL_FLOW flag is passed, the vector must have the same size as in the input.
           		status output status vector (of unsigned chars); each element of the vector is set to 1 if the flow for the corresponding features has been found, otherwise, it
           		is set to 0.
           		err output vector of errors; each element of the vector is set to an error for the corresponding feature, type of the error measure can be set in flags parameter;
           		 if the flow wasn't found then the error is not defined (use the status parameter to find such cases).
            */
                Video.calcOpticalFlowPyrLK(matOpFlowPrev, matOpFlowThis, mMOP2fptsPrev, mMOP2fptsThis, mMOBStatus, mMOFerr);
                iLineThickness = 3;
                cornersPrev = mMOP2fptsPrev.toList();
                cornersThis = mMOP2fptsThis.toList();
                byteStatus = mMOBStatus.toList();

                y = byteStatus.size() - 1;

                int x3;
                for (x3 = 0; x3 < y; x3++) {
                    if (byteStatus.get(x3) == 1) {
                        pt = cornersThis.get(x3);
                        pt2 = cornersPrev.get(x3);

                        Core.circle(mRgba, pt, 5, colorRed, iLineThickness - 1);

                        Core.line(mRgba, pt, pt2, colorRed, iLineThickness);
                    }
                }
                break;
        }
        CameraPreviewSurfaceView.staticMat = mGray;
        Imgproc.resize(mGray, img, mSizeLarge);
        return img;
    }

    public void releaseMats() {
        mRgba.release();
        mIntermediateMat.release();
        mGray.release();
        lines.release();
        MOPcorners.release();
        mRgba.release();
        mIntermediateMat.release();
        mGray.release();
    }
}

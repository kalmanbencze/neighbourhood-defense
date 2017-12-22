/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.kalmanbncz.nd.threadpool;

import org.opencv.core.Mat;
import me.kalmanbncz.nd.threadpool.ProcessingRunnable.TaskProcessingMethods;

/**
 * This class manages PhotoDownloadRunnable and PhotoDownloadRunnable objects.  It does't perform
 * the download or decode; instead, it manages persistent storage for the tasks that do the work.
 * It does this by implementing the interfaces that the download and decode classes define, and
 * then passing itself as an argument to the constructor of a download or decode object. In effect,
 * this allows PhotoTask to start on a Thread, run a download in a delegate object, then
 * run a decode, and then start over again. This class can be pooled and reused as necessary.
 */
public class ProcessingTask implements TaskProcessingMethods {

    /*
     * An object that contains the ThreadPool singleton.
     */
    private static ProcessingManager sProcessingManager;

    /*
     * Field containing the Thread this task is running on.
     */
    Thread mThreadThis;

    // A buffer for containing the bytes that make up the image
    Mat mMat;

    // Is the cache enabled for this transaction?
    private boolean mCacheEnabled;

    /*
     * Fields containing references to the two runnable objects that handle downloading and
     * decoding of the image.
     */
    private Runnable mProcessingRunnable;

    // The Thread on which this task is currently running.
    private Thread mCurrentThread;

    /**
     * Creates an PhotoTask containing a download object and a decoder object.
     */
    ProcessingTask() {
        // Create the runnables
        mProcessingRunnable = new ProcessingRunnable(this);
        sProcessingManager = ProcessingManager.getInstance();
    }

    /**
     * Initializes the Task
     * @param processingManager A ThreadPool object
     */
    void initializeDownloaderTask(ProcessingManager processingManager,
                                  Mat image) {
        // Sets this object's ThreadPool field to be the input argument
        sProcessingManager = processingManager;
        mMat = image;
        // Sets the cache flag to the input argument
//        mCacheEnabled = cacheFlag;
    }

    // Implements HTTPDownloaderRunnable.getByteBuffer
    @Override
    public Mat getMat() {

        // Returns the global field
        return mMat;
    }

    // Implements PhotoDownloadRunnable.setByteBuffer. Sets the image buffer to a buffer object.
    @Override
    public void setMat(Mat imageBuffer) {
        mMat = imageBuffer;
    }

    /**
     * Recycles an PhotoTask object before it's put back into the pool. One reason to do
     * this is to avoid memory leaks.
     */
    void recycle() {
        // Releases references to the byte buffer and the BitMap
        mMat = null;
    }

    // Detects the state of caching
    boolean isCacheEnabled() {
        return mCacheEnabled;
    }

    // Delegates handling the current state of the task to the PhotoManager object
    @Override
    public void handleState(int state) {
        sProcessingManager.handleState(this, state);
    }

    // Returns the instance that downloaded the image
    Runnable getProcessingRunnable() {
        return mProcessingRunnable;
    }

    /*
     * Returns the Thread that this Task is running on. The method must first get a lock on a
     * static field, in this case the ThreadPool singleton. The lock is needed because the
     * Thread object reference is stored in the Thread object itself, and that object can be
     * changed by processes outside of this app.
     */
    public Thread getCurrentThread() {
        synchronized (sProcessingManager) {
            return mCurrentThread;
        }
    }

    /*
     * Sets the identifier for the current Thread. This must be a synchronized operation; see the
     * notes for getCurrentThread()
     */
    public void setCurrentThread(Thread thread) {
        synchronized (sProcessingManager) {
            mCurrentThread = thread;
        }
    }

    // Implements ImageCoderRunnable.setImage(). Sets the Bitmap for the current image.
//    @Override
//    public void setMat(Mat decodedImage) {
//        mProcessedMat = decodedImage;
//    }

    // Implements PhotoDownloadRunnable.setHTTPDownloadThread(). Calls setCurrentThread().
    @Override
    public void setProcessingThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    /*
     * Implements PhotoDownloadRunnable.handleHTTPState(). Passes the download state to the
     * ThreadPool object.
     */

}

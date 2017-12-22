/*
 * Copyright (C) ${year} The Android Open Source Project
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
import me.kalmanbncz.nd.processing.ImageProcessor;
import me.kalmanbncz.nd.util.Log;

/**
 * This task downloads bytes from a resource addressed by a URL.  When the task
 * has finished, it calls handleState to report its results.
 * <p>
 * Objects of this class are instantiated and managed by instances of PhotoTask, which
 * implements the methods of {@link TaskProcessingMethods}. PhotoTask objects call
 * {@link #ProcessingRunnable(TaskProcessingMethods) PhotoDownloadRunnable()} with
 * themselves as the argument. In effect, an PhotoTask object and a
 * PhotoDownloadRunnable object communicate through the fields of the PhotoTask.
 */
class ProcessingRunnable implements Runnable {
    // Constants for indicating the state of the download
    static final int PROCESSING_STARTED = 1;

    static final int TASK_COMPLETE = 0;

    // Sets the size for each read action (bytes)
    private static final int READ_SIZE = 1024 * 2;

    // Sets a tag for this class
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "PhotoDownloadRunnable";

    // Defines a field that contains the calling object of type PhotoTask.
    final TaskProcessingMethods mProcessingTask;

    /**
     * This constructor creates an instance of PhotoDownloadRunnable and stores in it a reference
     * to the PhotoTask instance that instantiated it.
     * @param photoTask The PhotoTask, which implements TaskRunnableDecodeMethods
     */
    ProcessingRunnable(TaskProcessingMethods photoTask) {
        mProcessingTask = photoTask;
    }

    /*
     * Defines this object's task, which is a set of instructions designed to be run on a Thread.
     */
    @SuppressWarnings("resource")
    @Override
    public void run() {

        /*
         * Stores the current Thread in the the PhotoTask instance, so that the instance
         * can interrupt the Thread.
         */
        mProcessingTask.setProcessingThread(Thread.currentThread());

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        /*
         * Gets the image cache buffer object from the PhotoTask instance. This makes the
         * to both PhotoDownloadRunnable and PhotoTask.
         */
        Mat mat = mProcessingTask.getMat();

        if (mat != null) {
            mProcessingTask.setMat(ImageProcessor.getInstance().process(mat));
        } else {
            Log.log("Mat was null");
        }


        mProcessingTask.handleState(TASK_COMPLETE);
    }

    /**
     * An interface that defines methods that PhotoTask implements. An instance of
     * PhotoTask passes itself to an PhotoDownloadRunnable instance through the
     * PhotoDownloadRunnable constructor, after which the two instances can access each other's
     * variables.
     */
    interface TaskProcessingMethods {

        /**
         * Sets the Thread that this instance is running on
         * @param currentThread the current Thread
         */
        void setProcessingThread(Thread currentThread);

        /**
         * Returns the current contents of the download buffer
         * @return The byte array downloaded from the URL in the last read
         */
        Mat getMat();

        /**
         * Sets the current contents of the download buffer
         * @param mat The bytes that were just read
         */
        void setMat(Mat mat);

        /**
         * Defines the actions for each state of the PhotoTask instance.
         * @param state The current state of the task
         */
        void handleState(int state);
    }
}


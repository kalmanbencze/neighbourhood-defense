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

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.opencv.core.Mat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import me.kalmanbncz.nd.processing.SizedStack;
import me.kalmanbncz.nd.util.Log;

/**
 * This class creates pools of background threads for downloading
 * Picasa images from the web, based on URLs retrieved from Picasa's featured images RSS feed.
 * The class is implemented as a singleton; the only way to get an PhotoManager instance is to
 * call {@link #getInstance}.
 * <p>
 * The class sets the pool size and cache size based on the particular operation it's performing.
 * The algorithm doesn't apply to all situations, so if you re-use the code to implement a pool
 * of threads for your own app, you will have to come up with your choices for pool size, cache
 * size, and so forth. In many cases, you'll have to set some numbers arbitrarily and then
 * measure the impact on performance.
 * <p>
 * This class actually uses two threadpools in order to limit the number of
 * simultaneous image decoding threads to the number of available processor
 * cores.
 * <p>
 * Finally, this class defines a handler that communicates back to the UI
 * thread to change the bitmap to reflect the state.
 */
public class ProcessingManager {
    /*
     * Status indicators
     */
    static final int PROCESSING_STARTED = 1;

    static final int TASK_COMPLETE = 0;

    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 0;

    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    // Sets the initial threadpool size to 8
    private static final int CORE_POOL_SIZE = 8;

    // Sets the maximum threadpool size to 8
    private static final int MAXIMUM_POOL_SIZE = 8;

    public static SizedStack<Mat> matStack = new SizedStack<>(6);

    /**
     * NOTE: This is the number of total available cores. On current versions of
     * Android, with devices that use plug-and-play cores, this will return less
     * than the total number of cores. The total number of cores is not
     * available in current Android implementations.
     */
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    // A single instance of PhotoManager, used to implement the singleton pattern
    private static ProcessingManager sInstance = null;

    // A static block that sets class fields
    static {
        // The time unit for "keep alive" is in seconds
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        // Creates a single static instance of PhotoManager
        sInstance = new ProcessingManager();
    }

    /*
     * Creates a cache of byte arrays indexed by image URLs. As new items are added to the
     * cache, the oldest items are ejected and subject to garbage collection.
     */
    private final LruCache<Integer, Mat> mPhotoCache;

    // A queue of Runnables for the image download pool
    private final BlockingQueue<Runnable> mProcessingWorkQueue;

    // A queue of PhotoManager tasks. Tasks are handed to a ThreadPool.
    private final Queue<ProcessingTask> mProcessingTaskWorkQueue;

    // A managed pool of background download threads
    private final ThreadPoolExecutor mProcessingThreadPool;

    // An object that manages Messages in a Thread
    private Handler mHandler;

    /**
     * Constructs the work queues and thread pools used to download and decode images.
     */
    private ProcessingManager() {

        /*
         * Creates a work queue for the pool of Thread objects used for downloading, using a linked
         * list queue that blocks when the queue is empty.
         */
        mProcessingWorkQueue = new LinkedBlockingQueue<Runnable>();

        /*
         * Creates a work queue for the set of of task objects that control downloading and
         * decoding, using a linked list queue that blocks when the queue is empty.
         */
        mProcessingTaskWorkQueue = new LinkedBlockingQueue<ProcessingTask>(6);

        /*
         * Creates a new pool of Thread objects for the download work queue
         */
        mProcessingThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mProcessingWorkQueue);

        // Instantiates a new cache based on the cache size estimate
        mPhotoCache = new LruCache<Integer, Mat>(6);
        /*
         * Instantiates a new anonymous Handler object and defines its
         * handleMessage() method. The Handler *must* run on the UI thread, because it moves photo
         * Bitmaps from the PhotoTask object to the View object.
         * To force the Handler to run on the UI thread, it's defined as part of the PhotoManager
         * constructor. The constructor is invoked when the class is first referenced, and that
         * happens when the View invokes startProcessing. Since the View runs on the UI Thread, so
         * does the constructor and the Handler.
         */
        mHandler = new Handler(Looper.getMainLooper()) {

            /*
             * handleMessage() defines the operations to perform when the
             * Handler receives a new Message to process.
             */
            @Override
            public void handleMessage(Message inputMessage) {

                // Gets the image task from the incoming Message object.
                ProcessingTask processingTask = (ProcessingTask) inputMessage.obj;

                // Sets an PhotoView that's a weak reference to the
                // input ImageView

                // If this input view isn't null
//                if (localView != null) {

                    /*
                     * Gets the URL of the *weak reference* to the input
                     * ImageView. The weak reference won't have changed, even if
                     * the input ImageView has.
                     */
//                    URL localURL = localView.getLocation();

                    /*
                     * Compares the URL of the input ImageView to the URL of the
                     * weak reference. Only updates the bitmap in the ImageView
                     * if this particular Thread is supposed to be serving the
                     * ImageView.
                     */
//                    if (photoTask.getImageURL() == localURL)

                        /*
                         * Chooses the action to take, based on the incoming message
                         */
                switch (inputMessage.what) {

//                            // If the download has started, sets background color to dark green
//                            case DOWNLOAD_STARTED:
//                                localView.setStatusResource(R.drawable.imagedownloading);
//                                break;
//
//                            /*
//                             * If the download is complete, but the decode is waiting, sets the
//                             * background color to golden yellow
//                             */
//                            case DOWNLOAD_COMPLETE:
//                                // Sets background color to golden yellow
//                                localView.setStatusResource(R.drawable.decodequeued);
//                                break;
//                            // If the decode has started, sets background color to orange
                    case PROCESSING_STARTED:
//                                localView.setStatusResource(R.drawable.decodedecoding);
                        break;
                            /*
                             * The decoding is done, so this sets the
                             * ImageView's bitmap to the bitmap in the
                             * incoming message
                             */
                    case TASK_COMPLETE:
//                                localView.setImageBitmap(processingTask.getImage());
//                                synchronized (CameraPreviewSurfaceView.staticMat) {
                        //CameraPreviewSurfaceView.staticMat = processingTask.getMat();
//                                };
                        Log.log("thread finished");
                        recycleTask(processingTask);
                        break;
                    // The download failed, sets the background color to dark red
//                            case DOWNLOAD_FAILED:
//                                localView.setStatusResource(R.drawable.imagedownloadfailed);
//
//                                // Attempts to re-use the Task object
//                                recycleTask(processingTask);
//                                break;
                    default:
                        // Otherwise, calls the super method
                        super.handleMessage(inputMessage);
                }
//                }
            }
        };
    }

    /**
     * Returns the PhotoManager object
     * @return The global PhotoManager object
     */
    public static ProcessingManager getInstance() {

        return sInstance;
    }

    /**
     * Cancels all Threads in the ThreadPool
     */
    public static void cancelAll() {

        /*
         * Creates an array of tasks that's the same size as the task work queue
         */
        ProcessingTask[] taskArray = new ProcessingTask[sInstance.mProcessingTaskWorkQueue.size()];

        // Populates the array with the task objects in the queue
        sInstance.mProcessingTaskWorkQueue.toArray(taskArray);

        // Stores the array length in order to iterate over the array
        int taskArraylen = taskArray.length;

        /*
         * Locks on the singleton to ensure that other processes aren't mutating Threads, then
         * iterates over the array of tasks and interrupts the task's current Thread.
         */
        synchronized (sInstance) {

            // Iterates over the array of tasks
            for (int taskArrayIndex = 0; taskArrayIndex < taskArraylen; taskArrayIndex++) {

                // Gets the task's current thread
                Thread thread = taskArray[taskArrayIndex].mThreadThis;

                // if the Thread exists, post an interrupt to it
                if (null != thread) {
                    thread.interrupt();
                }
            }
        }
    }

    /**
     * Stops a download Thread and removes it from the threadpool
     * @param downloaderTask The download task associated with the Thread
     */
    static public void removeProcessing(ProcessingTask downloaderTask) {

        // If the Thread object still exists and the download matches the specified URL
        if (downloaderTask != null) {

            /*
             * Locks on this class to ensure that other processes aren't mutating Threads.
             */
            synchronized (sInstance) {

                // Gets the Thread that the downloader task is running on
                Thread thread = downloaderTask.getCurrentThread();

                // If the Thread exists, posts an interrupt to it
                if (null != thread)
                    thread.interrupt();
            }
            /*
             * Removes the download Runnable from the ThreadPool. This opens a Thread in the
             * ThreadPool's work queue, allowing a task in the queue to start.
             */
            sInstance.mProcessingThreadPool.remove(downloaderTask.getProcessingRunnable());
        }
    }

    /**
     * Starts an image download and decode
     * @return The task instance that will handle the work
     */
    static public ProcessingTask startProcessing() {

        /*
         * Gets a task from the pool of tasks, returning null if the pool is empty
         */
        if (sInstance.mProcessingTaskWorkQueue.size() < 5) {
            sInstance.mProcessingTaskWorkQueue.offer(new ProcessingTask());
        }
        ProcessingTask processingTask = sInstance.mProcessingTaskWorkQueue.poll();

        // If the queue was empty, create a new task instead.
//        if (null == processingTask) {
//            processingTask = new ProcessingTask();
//            sInstance.mProcessingTaskWorkQueue.add(processingTask);
//            Log.log("New ProcessingTask");
//        }

        if (processingTask != null) {
            // Initializes the task
            processingTask.initializeDownloaderTask(ProcessingManager.sInstance, matStack.pop());


        /*
         * Provides the download task with the cache buffer corresponding to the URL to be
         * downloaded.
         */
//        processingTask.setByteBuffer(sInstance.mPhotoCache.get(processingTask.getImageURL()));

            // If the byte buffer was empty, the image wasn't cached
//        if (null == processingTask.getByteBuffer()) {

            /*
             * "Executes" the tasks' download Runnable in order to download the image. If no
             * Threads are available in the thread pool, the Runnable waits in the queue.
             */
            sInstance.mProcessingThreadPool.execute(processingTask.getProcessingRunnable());

            // Sets the display to show that the image is queued for downloading and decoding.
//            imageView.setStatusResource(R.drawable.imagequeued);

            // The image was cached, so no download is required.
//        } else {

            /*
             * Signals that the download is "complete", because the byte array already contains the
             * undecoded image. The decoding starts.
             */

//            sInstance.handleState(processingTask, DOWNLOAD_COMPLETE);
//        }
        }

        // Returns a task object, either newly-created or one from the task pool
        return processingTask;
    }

    /**
     * Handles state messages for a particular task object
     * @param processingTask A task object
     * @param state The state of the task
     */
    public void handleState(ProcessingTask processingTask, int state) {
        switch (state) {

            // The task finished downloading and decoding the image
            case TASK_COMPLETE:
                // Gets a Message object, stores the state in it, and sends it to the Handler
                Message completeMessage = mHandler.obtainMessage(state, processingTask);
                completeMessage.sendToTarget();
                break;
            default:
                mHandler.obtainMessage(state, processingTask).sendToTarget();
                break;
        }

    }

    /**
     * Recycles tasks by calling their internal recycle() method and then putting them back into
     * the task queue.
     * @param downloadTask The task to recycle
     */
    void recycleTask(ProcessingTask downloadTask) {

        // Frees up memory in the task
        downloadTask.recycle();

        // Puts the task object back into the queue for re-use.
        mProcessingTaskWorkQueue.offer(downloadTask);
        startProcessing();
    }
}

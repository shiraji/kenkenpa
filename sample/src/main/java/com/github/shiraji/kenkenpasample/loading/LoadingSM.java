package com.github.shiraji.kenkenpasample.loading;

import android.util.Log;
import com.github.shiraji.kenkenpa.annotations.Hop;
import com.github.shiraji.kenkenpa.annotations.Hops;
import com.github.shiraji.kenkenpa.annotations.KenKenPa;
import com.github.shiraji.kenkenpa.annotations.TakeOff;

/**
 * Sample code for using KenKenPa. This sample has following states:
 * <ul>
 * <li>INIT</li>
 * <li>READY</li>
 * <li>LOADING</li>
 * <li>CLOSE</li>
 * </ul>
 * The interesting point is the developer does not require to have "isLoading" flag.
 */
@KenKenPa(defaultState = "INIT")
public abstract class LoadingSM {

    public static LoadingSM create() {
        return new KenKenPa_LoadingSM();
    }

    @Hop(from = "INIT", to = "READY")
    public void init() {
        Log.i("LoadingSM", "initializing network...");
    }

    @Hops({@Hop(from = "READY", to = "LOADING"), @Hop(from = "LOADING", to = "LOADING")})
    public void load() {
        Log.i("LoadingSM", "loading...");
    }

    @Hop(from = "LOADING", to = "CLOSE")
    public void close() {
        Log.i("LoadingSM", "clear network...");
    }

    @Hop(from = "CLOSE", to = "READY")
    public void reset() {
        Log.i("LoadingSM", "reset...");
    }

    @TakeOff("READY")
    public void launch() {
        // Emulating network access.
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);

                    // Done. let's notify!
                    if (mListener != null) {
                        mListener.done();
                    }
                } catch (InterruptedException e) {
                }
            }
        }.start();
    }

    interface NetworkDoneListener {
        void done();
    }

    private NetworkDoneListener mListener;

    public void setListener(NetworkDoneListener listener) {
        mListener = listener;
    }
}
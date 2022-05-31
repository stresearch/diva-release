package com.visym.collector.capturemodule.model;

import android.os.Handler;
import android.os.Message;

public class VideoCaptureTimer implements Runnable {

    private Handler timerHandler;
    private int counter;
    private int maxTime;

    public VideoCaptureTimer(Handler timerHandler, int maxTime) {
        this.timerHandler = timerHandler;
        this.maxTime = maxTime;
    }

    @Override
    public void run() {
        timerHandler.postDelayed(this, 1000);
        Message message = timerHandler.obtainMessage();
        if (counter == maxTime){
            counter = 0;
            message.arg1 = -1;
            timerHandler.sendMessage(message);
            return;
        }
        counter++;
        message.arg1 = counter;
        timerHandler.sendMessage(message);
    }
}

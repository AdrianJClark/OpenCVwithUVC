package com.serenegiant.uvcpupildetector;

import java.nio.ByteBuffer;

public interface MyCallbackListener {
    public void onFrame(ByteBuffer frame);
}
package com.pupillabs.pupil;

import java.nio.ByteBuffer;

public interface MyCallbackListener {
    public void onFrame(ByteBuffer frame);
}
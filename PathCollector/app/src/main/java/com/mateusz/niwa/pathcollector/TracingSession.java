package com.mateusz.niwa.pathcollector;

import java.util.Date;

public class TracingSession {
    private Trace mTrace;
    private boolean mLock;

    public void begin() {
        if (mTrace != null)
            throw new RuntimeException("Attempted to use expired tracing session");

        mLock = true;
        mTrace = new Trace();
    };

    public void end() {
        mLock = false;
    }

    public void moveTo(float x, float y) {
        mTrace.appendPoint(x, y);
    }

    public void moveTo(float x, float y, long timeInMillis) {
        mTrace.appendPoint(x, y, timeInMillis);
    }

    public Trace getTrace() {
        if (mTrace == null)
            throw new RuntimeException("Attempted to retrieve trace from empty tracing session");

        if (mLock)
            throw new RuntimeException("Attempted to retrieve trace from locked session");

        return mTrace;
    }


}

package com.mateusz.niwa.pathcollector;

public interface ITraceRepository {
    public void addTrace(Trace trace, String tag);
    public void pull();
    public void push();
}

package com.pspdfkit.react.events;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import androidx.annotation.IdRes;

public class PdfViewTappedEvent extends Event<PdfViewTappedEvent> {
    public static final String EVENT_NAME = "pdfViewTappedEvent";
    private float x;
    private float y;

    public PdfViewTappedEvent(@IdRes int id, float x, float y) {
        super(id);
        this.x = x;
        this.y = y;
    }

    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
        WritableMap eventData = Arguments.createMap();
        eventData.putDouble("x", x);
        eventData.putDouble("y", y);
        rctEventEmitter.receiveEvent(getViewTag(), getEventName(), eventData);
    }
}

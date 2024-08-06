package com.pspdfkit.react.events;


import androidx.annotation.IdRes;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

/**
 * Event sent by the {@link com.pspdfkit.views.PdfView} containing info about the current zoom level.
 */
public class PdfViewZoomLevelChangedEvent extends Event<PdfViewZoomLevelChangedEvent> {

    public static final String EVENT_NAME = "pdfViewZoomLevelChanged";

    private final float zoomLevel;

    public PdfViewZoomLevelChangedEvent(@IdRes int viewID,
                                        float zoomLevel) {
        super(viewID);
        this.zoomLevel = zoomLevel;
    }

    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
        WritableMap eventData = Arguments.createMap();
        eventData.putDouble("zoomLevel", zoomLevel);
        rctEventEmitter.receiveEvent(getViewTag(), getEventName(), eventData);
    }
}

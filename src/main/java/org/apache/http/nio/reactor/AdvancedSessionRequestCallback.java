package org.apache.http.nio.reactor;
//
public interface AdvancedSessionRequestCallback
extends SessionRequestCallback {
    void initiated(SessionRequest sessionRequest);
}

package net.ericsson.emovs.cast;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Copyright (c) 2017 Ericsson. All Rights Reserved
 *
 * This SOURCE CODE FILE, which has been provided by Ericsson as part
 * of an Ericsson software product for use ONLY by licensed users of the
 * product, includes CONFIDENTIAL and PROPRIETARY information of Ericsson.
 *
 * USE OF THIS SOFTWARE IS GOVERNED BY THE TERMS AND CONDITIONS OF
 * THE LICENSE STATEMENT AND LIMITED WARRANTY FURNISHED WITH
 * THE PRODUCT.
 */

public class EmpReceiverChannel implements Cast.MessageReceivedCallback {
    private static final String TAG = "EmpReceiverChannel";
    private static final String NAMESPACE = "urn:x-cast:com.ericsson.cast.receiver";

    private static volatile EmpReceiverChannel sEmpReceiverChannel;

    private final Set<EmpCastListener> listeners = new HashSet<>();
    private CastSession mCastSession;

    private EmpReceiverChannel(CastContext castContext) {
        SessionManagerListenerImpl sessionManagerListener = new SessionManagerListenerImpl();
        castContext.getSessionManager().addSessionManagerListener(
                sessionManagerListener, CastSession.class);

        mCastSession = castContext.getSessionManager().getCurrentCastSession();
        connectMessageReceiver();
    }

    private void connectMessageReceiver() {
        if(null != mCastSession) {
            try {
                mCastSession.setMessageReceivedCallbacks(NAMESPACE, this);
            } catch (IOException e) {
                Log.e(TAG, "Error connecting message receiver", e);
            }
        }
    }

    public static EmpReceiverChannel getSharedInstance(CastContext castContext) {
        if(null == sEmpReceiverChannel) {
            sEmpReceiverChannel = new EmpReceiverChannel(castContext);
        }
        return sEmpReceiverChannel;
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
        Log.d(TAG, "onMessageReceived: " + message);

        try {
            JSONObject jsonMessage = new JSONObject(message);

            switch (jsonMessage.getString("type")) {
                case "tracksupdated":
                    List<MediaTrack> audioTracks = new ArrayList<>();
                    List<MediaTrack> subtitleTracks = new ArrayList<>();
                    Boolean subTrackSelected = false;

                    JSONObject tracksInfo = jsonMessage.getJSONObject("data").getJSONObject("tracksInfo");

                    JSONArray activeTracks = tracksInfo.getJSONArray("activeTrackIds");
                    List<Integer> activeTrackIds = new ArrayList<>();
                    for(int ii = 0; ii < activeTracks.length(); ii++) {
                        activeTrackIds.add(activeTracks.getInt(ii));
                    }

                    JSONArray tracks = tracksInfo.getJSONArray("tracks");
                    for (int ii = 0; ii < tracks.length(); ii++) {
                        JSONObject track = tracks.getJSONObject(ii);
                        int id = track.getInt("id");
                        Boolean selected = activeTrackIds.contains(id);
                        switch (track.getString("type")) {
                            case "audio":
                                audioTracks.add(new MediaTrack(id, track.getString("label"), track.getString("language"), selected));
                                break;
                            case "text":
                                if (selected) { subTrackSelected = true; }
                                subtitleTracks.add(new MediaTrack(id, track.getString("label"), track.getString("language"), selected));
                                break;
                        }
                    }

                    for (EmpCastListener listener: listeners ) {
                        listener.onTracksUpdated(audioTracks, subtitleTracks);
                    }
                    break;
                case "volumechange":
                    JSONObject data = jsonMessage.getJSONObject("data");
                    int volume = data.getInt("volume");
                    boolean muted = data.getBoolean("muted");
                    for (EmpCastListener listener: listeners ) {
                        listener.onVolumeChanged(volume, muted);
                    }
                    break;
                case "durationchange":
                    int duration = jsonMessage.getInt("data");
                    for (EmpCastListener listener: listeners ) {
                        listener.onDurationChange(duration);
                    }
                    break;
                case "timeShiftEnabled":
                    boolean timeShiftEnabled = jsonMessage.getBoolean("data");
                    for (EmpCastListener listener: listeners ) {
                        listener.onTimeshiftEnabled(timeShiftEnabled);
                    }
                    break;
                case "isLive":
                    boolean isLive = jsonMessage.getBoolean("data");
                    for (EmpCastListener listener: listeners ) {
                        listener.onLive(isLive);
                    }
                    break;
                case "error":
                    JSONObject error = jsonMessage.getJSONObject("data");
                    String errorMessage = error.has("message") ? error.getString("message") : "";
                    String code = error.has("code") ? Integer.toString(error.getInt("code")) : "N/A";
                    for (EmpCastListener listener: listeners) {
                        listener.onError(code, errorMessage);
                    }
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing custom message", e);
        }
    }

    public void refreshControls() {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "refreshcontrols");
            sendMessage(message.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error while building message", e);
        }
    }

    public void showTextTrack(String language) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "showtexttrack");
            JSONObject data = new JSONObject();
            data.put("language", language);
            message.put("data", data);
            sendMessage(message.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error while building message", e);
        }
    }

    public void hideTextTrack() {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "hidetexttrack");
            sendMessage(message.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error while building message", e);
        }
    }

    public void selectAudioTrack(String language) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "selectaudiotrack");
            JSONObject data = new JSONObject();
            data.put("language", language);
            message.put("data", data);
            sendMessage(message.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error while building message", e);
        }
    }

    private void sendMessage(String message) {
        if (null == mCastSession) {
            return;
        }

        mCastSession.sendMessage(NAMESPACE, message)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    Log.w(TAG, "(" + status.getStatusCode() + ")" +
                                            (status.getStatusMessage() == null ? "" : status.getStatusMessage()));
                                }
                            }
                        }
                );
    }

    public void addListener(EmpCastListener listener){
        if(listener != null) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(EmpCastListener listener) {
        if(listener != null) {
            this.listeners.remove(listener);
        }
    }

    private class SessionManagerListenerImpl implements SessionManagerListener<CastSession> {

        @Override
        public void onSessionStarting(CastSession castSession) {
            Log.d(TAG, "onSessionStarting");
        }

        @Override
        public void onSessionStarted(CastSession castSession, String s) {
            Log.d(TAG, "onSessionStarted");
            onAppConnected(castSession);
        }

        @Override
        public void onSessionStartFailed(CastSession castSession, int i) {
            Log.d(TAG, "onSessionStartFailed (" + CastStatusCodes.getStatusCodeString(i) + ")");
        }

        @Override
        public void onSessionEnding(CastSession castSession) {
            Log.d(TAG, "onSessionEnding");
        }

        @Override
        public void onSessionEnded(CastSession castSession, int i) {
            Log.d(TAG, "onSessionEnded (" + CastStatusCodes.getStatusCodeString(i) + ")");
            onAppDisconnected();
        }

        @Override
        public void onSessionResuming(CastSession castSession, String s) {
            Log.d(TAG, "onSessionResuming");
        }

        @Override
        public void onSessionResumed(CastSession castSession, boolean b) {
            Log.d(TAG, "onSessionResumed");
            onAppConnected(castSession);
        }

        @Override
        public void onSessionResumeFailed(CastSession castSession, int i) {
            Log.d(TAG, "onSessionResumeFailed (" + CastStatusCodes.getStatusCodeString(i) + ")");

        }

        @Override
        public void onSessionSuspended(CastSession castSession, int i) {
            Log.d(TAG, "onSessionSuspended");
            onAppDisconnected();
        }

        private void onAppConnected(CastSession castSession) {
            mCastSession = castSession;
            connectMessageReceiver();
            refreshControls();
        }

        private void onAppDisconnected() {
            mCastSession = null;
        }
    }
}

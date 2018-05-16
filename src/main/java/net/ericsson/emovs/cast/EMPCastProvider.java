package net.ericsson.emovs.cast;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import net.ericsson.emovs.cast.interfaces.IEmpCastListener;
import net.ericsson.emovs.cast.models.EmpCustomData;
import net.ericsson.emovs.cast.models.EmpExposureSettings;
import net.ericsson.emovs.cast.models.MediaTrack;
import net.ericsson.emovs.cast.ui.activities.ExpandedControlsActivity;
import net.ericsson.emovs.utilities.emp.EMPRegistry;
import net.ericsson.emovs.utilities.interfaces.IPlayable;
import net.ericsson.emovs.utilities.models.EmpAsset;
import net.ericsson.emovs.utilities.models.EmpChannel;
import net.ericsson.emovs.utilities.models.EmpImage;
import net.ericsson.emovs.utilities.models.EmpProgram;

import java.util.List;

import static com.google.android.gms.cast.MediaStatus.PLAYER_STATE_UNKNOWN;

/**
 * Created by Joao Coelho on 2017-12-04.
 */

public class EMPCastProvider {
    private static final String TAG = EMPCastProvider.class.toString();
    private CastContext castContext;
    private EmpReceiverChannel empReceiverChannel;
    private EmptyEmpCastListener startCastingListener;

    protected IPlayable playable;

    private static class EMPCastProviderHolder {
        private final static EMPCastProvider sInstance = new EMPCastProvider();
    }

    /**
     *
     * @return
     */
    public static EMPCastProvider getInstance() {
        return EMPCastProvider.EMPCastProviderHolder.sInstance;
    }

    /**
     *
     */
    public EMPCastProvider() {
        try {
            this.castContext = CastContext.getSharedInstance(EMPRegistry.applicationContext());
            this.empReceiverChannel = EmpReceiverChannel.getSharedInstance(this.castContext);
        }
        catch(Exception e) {
            this.castContext = null;
            this.empReceiverChannel = null;
            Log.d(TAG, "Chromecast capabilities not found.");
        }
    }

    /**
     *
     * @return
     */
    public CastContext getCastContext() {
        return this.castContext;
    }

    /**
     *
     * @return
     */
    public CastSession getCurrentCastSession() {
        if (this.castContext != null && this.castContext.getSessionManager() != null) {
            return this.castContext.getSessionManager().getCurrentCastSession();
        }
        return null;
    }

    /**
     *
     * @param playable
     * @param onReady
     */
    public void startCasting(IPlayable playable, EmpCustomData customData, final Runnable onReady, final Runnable onError) {
        if (this.getCurrentCastSession() == null) {
            if (onError != null) {
                onError.run();
            }
            return;
        }

        final RemoteMediaClient remoteMediaClient = this.getCurrentCastSession().getRemoteMediaClient();
        if (remoteMediaClient == null) {
            //displayAlertMessage("Unable to get remote media client");
            return;
        }

        if (this.startCastingListener != null) {
            empReceiverChannel.removeListener(startCastingListener);
        }

        startCastingListener = new EmptyEmpCastListener() {
            @Override
            public void onTracksUpdated(List<MediaTrack> audioTracks, List<MediaTrack> subtitleTracks) {
                if (onReady != null) {
                    onReady.run();
                }
            }

            @Override
            public void onError(String errorCode, String message) {
                if (onError != null) {
                    onError.run();
                }
            }
        };

        empReceiverChannel.addListener(startCastingListener);

        remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
            @Override
            public void onStatusUpdated() {
                empReceiverChannel.refreshControls();
            }

            @Override
            public void onMetadataUpdated() {
            }

            @Override
            public void onQueueStatusUpdated() {
            }

            @Override
            public void onPreloadStatusUpdated() {
            }

            @Override
            public void onSendingRemoteMediaRequest() {
            }

            @Override
            public void onAdBreakStatusUpdated() {
            }
        });

        if (customData == null) {
            customData = new EmpCustomData();
        }

        if (playable instanceof EmpProgram) {
            EmpProgram empProgram = (EmpProgram) playable;
            customData.setProgramId(empProgram.programId);
            customData.channelId = empProgram.channelId;
        }
        else if (playable instanceof EmpAsset) {
            EmpAsset empAsset = (EmpAsset) playable;
            customData.assetId = empAsset.assetId;
        }
        else if (playable instanceof EmpChannel) {
            EmpChannel empChannel = (EmpChannel) playable;
            customData.channelId = empChannel.channelId;
        }

        this.playable = playable;
        MediaInfo media = buildMediaInfo(playable, customData);

        remoteMediaClient.load(media, customData.autoplay, customData.startTime, customData.toJson());
    }

    /**
     *
     * @return
     */
    public EmpReceiverChannel getReceiverChannel() {
        return this.empReceiverChannel;
    }

    /**
     *
     */
    public void showExpandedControls() {
        Intent intent = new Intent(EMPRegistry.applicationContext(), ExpandedControlsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        EMPRegistry.applicationContext().startActivity(intent);
    }

    private MediaInfo buildMediaInfo(IPlayable playable, EmpCustomData customData) {
        String locale = customData.locale;

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        movieMetadata.putString(MediaMetadata.KEY_TITLE, playable.getLocalized().getTitle(locale));
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, playable.getLocalized().getDescriptions(locale));

        EmpImage image;
        if (customData.imageType != null) {
            image = playable.getLocalized().getImage(locale, EmpImage.Orientation.LANDSCAPE, customData.imageType);
        }
        else {
            image = playable.getLocalized().getImage(locale, EmpImage.Orientation.LANDSCAPE);
        }

        if (image != null && image.url != null) {
            movieMetadata.addImage(new WebImage(Uri.parse(image.url)));
        }

        MediaInfo.Builder builder;


        if (playable instanceof EmpAsset || playable instanceof EmpProgram) {
            EmpAsset asset = (EmpAsset) playable;
            builder = new MediaInfo.Builder(asset.assetId);
        }
        else if (playable instanceof EmpChannel) {
            EmpChannel channel = (EmpChannel) playable;

            builder = new MediaInfo.Builder(channel.channelId);
        }
        else {
            return null;
        }

        return builder.setMetadata(movieMetadata)
                .setStreamType(MediaInfo.STREAM_TYPE_NONE)
                .setContentType("video/mp4").build();
    }

}

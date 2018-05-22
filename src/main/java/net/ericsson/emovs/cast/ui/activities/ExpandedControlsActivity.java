package net.ericsson.emovs.cast.ui.activities;

import android.support.annotation.LayoutRes;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;

import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity;

import net.ericsson.emovs.cast.EMPCastProvider;
import net.ericsson.emovs.cast.EmptyEmpCastListener;
import net.ericsson.emovs.cast.R;
import net.ericsson.emovs.cast.interfaces.IEmpCastListener;
import net.ericsson.emovs.cast.models.MediaTrack;
import net.ericsson.emovs.cast.ui.TrackSelectorFragment;

import java.util.List;

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

public class ExpandedControlsActivity extends ExpandedControllerActivity {
    IEmpCastListener empCastListener;
    TrackSelectorFragment trackSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            if (this.empCastListener != null) {
                EMPCastProvider.getInstance().getReceiverChannel().removeListener(this.empCastListener);
            }

            this.empCastListener = new EmptyEmpCastListener() {
                @Override
                public void onTracksUpdated(List<MediaTrack> audioTracks, List<MediaTrack> subtitleTracks) {
                    if (audioTracks.size() > 1 || subtitleTracks.size() > 0) {
                        bindTrackSelectionButton();
                    }
                    else {
                        ImageView ccBtn = geTracksButton();
                        if (ccBtn != null) {
                            ccBtn.setEnabled(false);
                            ccBtn.setClickable(false);
                        }
                    }
                }
            };

            bindTrackSelectionButton();
            EMPCastProvider.getInstance().getReceiverChannel().addListener(this.empCastListener);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        // NOTE: uncomment when you need a custom expanded layout
        //if (layoutResID == R.layout.cast_expanded_controller_activity) {
        //    getDelegate().setContentView(R.layout.emp_cast_expanded_controller_activity);
        //    return;
        //}
        super.setContentView(layoutResID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.cast_menu, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.empCastListener != null) {
            EMPCastProvider.getInstance().getReceiverChannel().removeListener(this.empCastListener);
        }
    }

    private ImageView geTracksButton() {
        for (int i = 0; i < getButtonSlotCount(); ++i) {
            if (getButtonTypeAt(i) == R.id.cast_button_type_closed_caption) {
                return getButtonImageViewAt(i);
            }
        }
        return null;
    }

    private void bindTrackSelectionFragment(ImageView  ccBtn) {
        ViewGroup ccBtnHolder = (ViewGroup) ccBtn.getParent().getParent();
        TrackSelectorFragment fragment = new TrackSelectorFragment();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(ccBtnHolder.getId(), fragment);
        ft.commit();
    }

    private void bindTrackSelectionButton() {
        if (this.trackSelector == null) {
            this.trackSelector = new TrackSelectorFragment();
        }

        ImageView ccBtn = geTracksButton();

        if (ccBtn != null) {
            ccBtn.setEnabled(true);
            ccBtn.setClickable(true);
            ccBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (trackSelector != null) {
                        trackSelector.show(getSupportFragmentManager(), "TrackSelection");
                    }
                }
            });
            bindTrackSelectionFragment(ccBtn);
        }
    }
}

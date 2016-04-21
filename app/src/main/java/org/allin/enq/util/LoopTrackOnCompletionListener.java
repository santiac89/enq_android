package org.allin.enq.util;

import android.media.MediaPlayer;

/**
 * Created by santiagocarullo on 4/19/16.
 */
public class LoopTrackOnCompletionListener implements MediaPlayer.OnCompletionListener {

    int loopTimes = 0;

    @Override
    public void onCompletion(MediaPlayer mp) {

        if (loopTimes < 2) {
            loopTimes++;
            mp.seekTo(0);
            mp.start();
            return;
        }

        mp.reset();
        mp.release();
        mp = null;
    }

}

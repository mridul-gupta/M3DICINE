package com.m3dicine.recorder;

import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.github.mikephil.charting.charts.LineChart;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static java.lang.Thread.sleep;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;

@LargeTest
public class AudioActivityTest {
    private final String TAG = "AudioActivityTest";


    @Rule
    public ActivityTestRule<AudioActivity> rule = new ActivityTestRule<>(AudioActivity.class);
    private AudioActivity audioActivity;

    private String mHasAudio;
    private int mOutputDuration = 0;
    private String mOutputTrackType;

    @Before
    public void setUp() {
        audioActivity = rule.getActivity();
        allowPermissionsIfNeeded();
    }


    @Test
    public void checkAllViews() {
        onView(withId(R.id.tv_status)).check(matches(notNullValue())).check(matches(CoreMatchers.<View>instanceOf(TextView.class)));
        onView(withId(R.id.bt_status)).check(matches(notNullValue())).check(matches(CoreMatchers.<View>instanceOf(Button.class)));
        onView(withId(R.id.chart_audio)).check(matches(notNullValue())).check(matches(CoreMatchers.<View>instanceOf(LineChart.class)));
        onView(withId(R.id.tv_play_counter)).check(matches(notNullValue())).check(matches(CoreMatchers.<View>instanceOf(TextView.class)));
        onView(withId(R.id.v_playhead)).check(matches(notNullValue())).check(matches(CoreMatchers.<View>instanceOf(View.class)));
        onView(withId(R.id.bt_recordplay)).check(matches(notNullValue())).check(matches(CoreMatchers.<View>instanceOf(ImageButton.class)));
        onView(withId(R.id.bt_bottom)).check(matches(notNullValue())).check(matches(CoreMatchers.<View>instanceOf(Button.class)));
    }

    @Test
    public void recordAudio() {
        try {
            sleep(1000);

            onView(withId(R.id.bt_recordplay)).perform(click());

            /* wait for recording to finish */
            sleep(Utils.MAX_TIME + 1000);

            /* check ui state post recording */
            onView(withId(R.id.tv_status)).check(matches(withText(R.string.playback_view)));
            //onView(withId(R.id.bt_recordplay)).check(matches(withDrawable(R.drawable.record)));
            onView(withId(R.id.bt_status)).check(matches(withText(R.string.ready)));
            onView(withId(R.id.bt_status)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)));
            onView(withId(R.id.tv_play_counter)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            onView(withId(R.id.bt_bottom)).check(matches(withText(R.string.back_rec)));
            onView(withId(R.id.bt_bottom)).check(matches(isEnabled()));

            /* check recorded file buffer  */
            assertTrue(validateAudio());

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void getOutputAudioProperty(String outputFilePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            Log.v(TAG, "file Path = " + outputFilePath);
            mmr.setDataSource(outputFilePath);

            mHasAudio = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
            mOutputTrackType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            mOutputDuration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            sleep(1000);

            mmr.release();
        } catch (Exception e) {
            Log.v(TAG, e.toString());
            mmr.release();
        }
    }

    private boolean validateAudio() {
        boolean validAudio = false;

        getOutputAudioProperty(Utils.getOutputFileName(audioActivity));

        Log.v("validateAudio", "Length = " + mOutputDuration);
        if (mOutputDuration > Utils.MAX_TIME &&
                mHasAudio.equals("yes") &&
                mOutputTrackType.equals("audio/mp4")) {
            validAudio = true;
        }
        return validAudio;
    }

    private static void allowPermissionsIfNeeded() {
        try {
            sleep(1000);

            UiDevice device = UiDevice.getInstance(getInstrumentation());
            UiObject allowPermissions = device.findObject(new UiSelector().text("Allow"));
            if (allowPermissions.exists()) {
                allowPermissions.click();
            }
        } catch (UiObjectNotFoundException e) {
            System.out.println("There is no permissions dialog to interact with");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}




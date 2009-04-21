/* Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.spanglers.minigolfscore;

import android.app.Activity;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class SettingsScreen extends Activity {
	static final int[] HOLE_CHOICES = {9, 18, 36, 54, 72};
	public static final String SAVE_FILENAME = "settings_screen.dat";
	
	SeekBar mSeekPlayerCount;
	SeekBar mSeekHoleCount;
	CheckBox mCheckBoxLandscape;
	
	final ScoreData mScoreData = new ScoreData();		// Current save file
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        final TextView textPlayerCount = (TextView)findViewById(R.id.player_count);
        final TextView textHoleCount = (TextView)findViewById(R.id.hole_count);
        
        mSeekPlayerCount = (SeekBar)findViewById(R.id.seek_player_count);
        mSeekPlayerCount.setMax(7);
        mSeekPlayerCount.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
				textPlayerCount.setText(String.format("Players: %d", progress + 1));
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        mSeekHoleCount = (SeekBar)findViewById(R.id.seek_hole_count);
        mSeekHoleCount.setMax(HOLE_CHOICES.length - 1);
        mSeekHoleCount.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
				textHoleCount.setText(String.format("Holes: %d", HOLE_CHOICES[progress]));
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    
       mCheckBoxLandscape = (CheckBox)findViewById(R.id.checkbox_always_landscape);
        
        final Button buttonOk = (Button) findViewById(R.id.ok_button);
        buttonOk.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	/* Load the main screen's settings file.  This ensures we save only the settings
            	 * we actually can save, and don't modify the others. 
            	 */
            	loadSettings(MiniGolfScore.SAVE_FILENAME);
            	// Then save the modified settings back
            	saveSettings(MiniGolfScore.SAVE_FILENAME);
            	
            	finish();
            }
        });

        final Button buttonCancel = (Button) findViewById(R.id.cancel_button);
        buttonCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	finish();
            }
        });
        
    }

    private void loadSettings(String filename) {
    	mScoreData.loadFromFile(this, filename);
    }
    
    /**
     * Save settings to the specified file
     */
    private void saveSettings(String filename) {
    	int holes = HOLE_CHOICES[mSeekHoleCount.getProgress()];
    	int players = mSeekPlayerCount.getProgress() + 1;
    	mScoreData.setDimensions(players, holes);
    	
    	mScoreData.setForceLandscape(mCheckBoxLandscape.isChecked());
    	
		mScoreData.saveToFile(this, filename);
    }
    
	/**
	 * Activity is being paused.  It may be killed after onPause() returns.   
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// Log.d("MiniGolfScore.Settings", "onPause()");
		saveSettings(SAVE_FILENAME);
	}
    
    /** Activity is being resumed after pause.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// Log.d("MiniGolfScore.Settings", "onResume()");

		// Restore saved settings
		loadSettings(SAVE_FILENAME);
        
        mSeekPlayerCount.setProgress(mScoreData.getPlayerCount() - 1);
        
        int holes = mScoreData.getHoleCount();
        int h;
        for (h = 0; h < HOLE_CHOICES.length && holes > HOLE_CHOICES[h]; h++);
        mSeekHoleCount.setProgress(h);
        
        mCheckBoxLandscape.setChecked(mScoreData.getForceLandscape());
	}
    
}
    
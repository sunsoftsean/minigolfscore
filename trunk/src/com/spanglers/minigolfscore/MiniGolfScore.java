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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.spanglers.minigolfscore.SheetView.OnEditListener;

public class MiniGolfScore extends Activity {

	static final String SAVE_FILENAME = "save.dat"; // Filename to save settings/scores to

	// Saved instance state keys
	static final String KEY_EDIT_PLAYER = "mEditPlayer"; 
	static final String KEY_EDIT_HOLE = "mEditHole"; 
	static final String KEY_DIALOG_TITLE = "mDialogTitle"; 

	// Dialog IDs
	static final int DIALOG_CONFIRM_CLEAR = 0;
	static final int DIALOG_EDIT_PLAYER_NAME = 1;
	static final int DIALOG_EDIT_SCORE = 2;
	static final int DIALOG_NUMBER_BUTTONS[] = {
		R.id.button_0,		
		R.id.button_1,		
		R.id.button_2,		
		R.id.button_3,		
		R.id.button_4,		
		R.id.button_5,		
		R.id.button_6,		
		R.id.button_7,		
		R.id.button_8,		
		R.id.button_9,		
	};
	
	SheetView mScoreSheet;	// Score sheet
	Button mButtonUndo;		// Undo button
	Button mButtonNext;		// Next button
	View mViewEditPlayer;	// Custom view for edit player dialog
	View mViewEditScore;	// Custom view for edit score dialog
	
	// Instance state
	int mEditPlayer;		// Player index to edit
	int mEditHole;			// Hole index to edit
	String mDialogTitle;	// Title of the current dialog
	
	final ScoreData mScoreData = new ScoreData();		// Current save file
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mScoreSheet = (SheetView) findViewById(R.id.score);
        mScoreSheet.setScoreData(mScoreData);
        mScoreSheet.SetOnEditListener(new OnEditListener() {
			@Override
			public void onEditPar(int hole) {
				mEditPlayer = -1;
				mEditHole = hole;
	        	showDialog(DIALOG_EDIT_SCORE);
			}

			@Override
			public void onEditPlayerName(int player) {
				mEditPlayer = player;
	        	showDialog(DIALOG_EDIT_PLAYER_NAME);
			}

			@Override
			public void onEditScore(int player, int hole) {
				mEditPlayer = player;
				mEditHole = hole;
	        	showDialog(DIALOG_EDIT_SCORE);
			}
        });
        
        // Restore data from the bundle, if any
        if (savedInstanceState != null) {
    		// Log.d("MiniGolfScore", "restoring from savedInstanceState");
        	
        	mEditPlayer = savedInstanceState.getInt(KEY_EDIT_PLAYER);
        	mEditHole = savedInstanceState.getInt(KEY_EDIT_HOLE);
        	
        	/* Obnoxiously, we need to save/restore the title of the active dialog ourselves. 
        	 * Depending on the way the dialog is created...
        	 *   1. Both onCreateDialog() and onPrepareDialog() are both called (on first launch of 
        	 *      the dialog).
        	 *   2. Only onPrepareDialog() is called (on second launch).
        	 *   3. Only onCreateDialog() is called (if the screen is rotated).
        	 * To handle case 3, we need to save the title we set in onPrepareDialog(), so that we
        	 * can use it as the default dialog title in onCreateDialog().
        	 */
        	mDialogTitle = savedInstanceState.getString(KEY_DIALOG_TITLE);
        }
    	if (mDialogTitle == null)
    		mDialogTitle = "Edit Something";
        
        
        mButtonUndo = (Button) findViewById(R.id.undo_button);
        mButtonUndo.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	if (mScoreData.undoLast()) {
            		// Move the selection back to match the undo
            		/* TODO: This is a little inaccurate, since it may change the selected player
            		 * if what we're undoing is par, or change the selected hole if what we're
            		 * undoing is a player name.  Usually it'll be ok, because popping up a 
            		 * dialog to change par or player name causes the score data to get saved so
            		 * we'll have the right selection.
            		 */
            		mScoreSheet.setSelectedHole(mScoreData.getSavedSelHole());
            		mScoreSheet.setSelectedPlayer(mScoreData.getSavedSelPlayer());
            		mScoreSheet.checkForRelayout();	// May need to re-layout if player name changed
            	}
            }
        });
        
        mButtonNext = (Button) findViewById(R.id.next_button);
        mButtonNext.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Perform action on clicks
            	mScoreSheet.moveSelectionBy(0, 1);
            }
        });
        
        // Set up the edit score dialog
        mViewEditScore = LayoutInflater.from(this).inflate(R.layout.dialog_edit_score, null);
        OnClickListener number_button_listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Button b = (Button)v;
	        	EditText t = (EditText)mViewEditScore.findViewById(R.id.score_edit);
	        	Editable e = t.getText();
	        	e.replace(t.getSelectionStart(), t.getSelectionEnd(), b.getText());
	        	t.setSelection(t.getSelectionEnd());
			}
        };
        for (int id: DIALOG_NUMBER_BUTTONS) {
        	Button b = (Button)mViewEditScore.findViewById(id);
        	b.setOnClickListener(number_button_listener);
        }
    }

	@Override
    protected Dialog onCreateDialog(int id) {
    	
    	// Log.d("MiniGolfScore", String.format("onCreateDialog %d", id));
    	
        LayoutInflater factory = LayoutInflater.from(this);
        switch (id) {
        case DIALOG_CONFIRM_CLEAR:
            return new AlertDialog.Builder(MiniGolfScore.this)
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setTitle(R.string.dialog_confirm_clear_title)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	// User clicked Yes; clear scores.
                    	mScoreData.resetScores();
                    	mScoreSheet.setSelectedHole(0);
                    	mScoreSheet.setSelectedPlayer(0);
                    }
                })
                .setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	// User clicked Cancel; do nothing
                    }
                })
                .create();
            
        case DIALOG_EDIT_PLAYER_NAME:
            mViewEditPlayer = factory.inflate(R.layout.dialog_edit_player, null);
            return new AlertDialog.Builder(MiniGolfScore.this)
                //.setIcon(R.drawable.alert_dialog_icon)
                .setTitle(String.format(mDialogTitle))
                .setView(mViewEditPlayer)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	// User clicked OK, so set player name.
                    	EditText t = (EditText)mViewEditPlayer.findViewById(R.id.player_name_edit);
                    	// Log.d("MiniGolfScore", String.format("set player name %s", t.getText()));
                   		mScoreData.setPlayerName(mEditPlayer, t.getText().toString());
                   		mScoreSheet.checkForRelayout();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	// User clicked cancel; do nothing
                    }
                })
                .create();
        	
        case DIALOG_EDIT_SCORE:
            return new AlertDialog.Builder(MiniGolfScore.this)
            //.setIcon(R.drawable.alert_dialog_icon)
            .setTitle(mDialogTitle)
            .setView(mViewEditScore)
            .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	// User clicked OK, so set score.
                	EditText t = (EditText)mViewEditScore.findViewById(R.id.score_edit);
                	// Log.d("MiniGolfScore", String.format("set score %s", t.getText()));
                	if (mEditPlayer == -1) {
                		// Editing par for the hole
                		mScoreData.setPar(mEditHole, Integer.parseInt(t.getText().toString()));
                    	mScoreSheet.invalidate();
                    } else {
                		// Editing score for a player
                    	mScoreData.setScore(mEditPlayer, mEditHole, Integer.parseInt(t.getText().toString()));
                    	mScoreSheet.invalidate();
                	}
                	
                }
            })
            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	// User clicked cancel; do nothing
                }
            })
            .create();
        }
        return null;
    }

	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu, menu);
	    return true;
	}

	private String getEmailText() {
		String t = "";
		
		int holes = mScoreData.getHoleCount();
		int players = mScoreData.getPlayerCount();
		boolean relative = mScoreSheet.getScoreRelative();
		
		t += "Hole:";
		for (int h = 0; h < holes; h++)
			t += String.format(" %d", h + 1);
		t += "/ Total (+/-)\n";
		
		t += "Par:";
		int totalPar = 0;
		for (int h = 0; h < holes; h++) {
			int s = mScoreData.getPar(h);
			t += String.format(" %d", s);
			totalPar += s;
		}
		t += String.format(" / %d\n", totalPar);
		
		for (int p = 0; p < players; p++) {
			t += String.format("%s: ", mScoreData.getPlayerName(p));
			int total = 0;
			int vspar = 0;
			for (int h = 0; h < holes; h++) {
				int s = mScoreData.getScore(p, h);
				int v = s - mScoreData.getPar(h);
				if (s > 0) {
					if (relative)
						t += String.format(" %+d", v);
					else
						t += String.format(" %d", s);
					total += s;
					vspar += v;
				} else {
					t += " -";
				}
			}
			t += String.format(" / %d (%+d)\n", total, vspar);
		}
		
    	return t;
	}
	
	/* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.clear:
        	showDialog(DIALOG_CONFIRM_CLEAR);
            return true;
        case R.id.settings:
        	// Save current settings to the settings screen file
        	saveSettings(SettingsScreen.SAVE_FILENAME);
        	startActivity(new Intent(this, SettingsScreen.class));
            return true;
        case R.id.email:
        	// Log.d("MiniGolfScore", "E-MAIL MENU");
        	Intent i = new Intent(Intent.ACTION_SEND);
        	i.setType("text/html");
        	i.putExtra(Intent.EXTRA_SUBJECT, "Mini golf scores");
        	// TODO: should really put EXTRA_STREAM with a stream containing the HTML.
        	i.putExtra(Intent.EXTRA_TEXT, getEmailText());
        	startActivity(Intent.createChooser(i, "Send golf scores"));
            return true;
        case R.id.help:
        	startActivity(new Intent(this, HelpScreen.class));
            return true;
        }
        return false;
    }

    /**
     * Save settings to the specified file
     */
    private void saveSettings(String filename) {
		mScoreData.setSavedScoreRelative(mScoreSheet.getScoreRelative()); 	
		mScoreData.setSavedSelPlayer(mScoreSheet.getSelectedPlayer());
		mScoreData.setSavedSelHole(mScoreSheet.getSelectedHole());
		mScoreData.saveToFile(this, filename);
    }
    
	/**
	 * Activity is being paused.  It may be killed after onPause() returns.   
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// Log.d("MiniGolfScore", "onPause()");
		saveSettings(SAVE_FILENAME);
	}

    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	AlertDialog ad = (AlertDialog)dialog;
    	EditText t;
    	
    	// Log.d("MiniGolfScore", String.format("onPrepareDialog %d", id));
    	
        switch (id) {
        case DIALOG_EDIT_PLAYER_NAME:
        	mDialogTitle = String.format("Set player %d name", mEditPlayer + 1); 
        	ad.setTitle(mDialogTitle);
        	t = (EditText)mViewEditPlayer.findViewById(R.id.player_name_edit);
            t.setText(mScoreData.getPlayerName(mEditPlayer));
            t.selectAll();
            break;
        	
        case DIALOG_EDIT_SCORE:
        	t = (EditText)mViewEditScore.findViewById(R.id.score_edit);
        	if (mEditPlayer == -1) {
        		mDialogTitle = String.format("Set par for hole %d", mEditHole + 1);
                t.setText(Integer.toString(mScoreData.getPar(mEditHole)));
        	} else {
        		mDialogTitle = String.format("Number of strokes for %s on hole %d", 
        				                     mScoreData.getPlayerName(mEditPlayer), mEditHole + 1);
                t.setText(Integer.toString(mScoreData.getScore(mEditPlayer, mEditHole)));
        	}
        	ad.setTitle(mDialogTitle);
            t.selectAll();
            break;
        }
    }
    
    /** Activity is being resumed after pause.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// Log.d("MiniGolfScore", "onResume()");

		// Restore saved settings
        mScoreData.loadFromFile(this, SAVE_FILENAME);
        
        if (mScoreData.getForceLandscape())
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        
        mScoreSheet.setScoreRelative(mScoreData.getSavedScoreRelative());
        mScoreSheet.setSelectedPlayer(mScoreData.getSavedSelPlayer());
        mScoreSheet.setSelectedHole(mScoreData.getSavedSelHole());
		mScoreSheet.checkForRelayout();
	}
    
    /** Save state of application, for possible reuse in onCreate().
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// Log.d("MiniGolfScore", "onSaveInstanceState()");
		
		// Save instance state
		outState.putInt(KEY_EDIT_PLAYER, mEditPlayer);
		outState.putInt(KEY_EDIT_HOLE, mEditHole);
		outState.putString(KEY_DIALOG_TITLE, mDialogTitle);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Log.d("MiniGolfScore", String.format("onConfigurationChanged() %d %d", newConfig.keyboardHidden, newConfig.orientation));
		// Force to landscape
		super.onConfigurationChanged(newConfig);
	}
}


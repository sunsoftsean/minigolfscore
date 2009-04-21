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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.ContextWrapper;
//import android.util.Log;

public class ScoreData {
	// Constants
	static final int SAVE_FILE_COOKIE = 20000613; // Cookie at start of filename
	static final int SAVE_FILE_VERSION = 6; // Current data format version for save file
	static final int DEFAULT_PAR = 3; // Default par for new holes
	static final int UNDO_NONE = 0;	// Nothing to undo
	static final int UNDO_PAR = 1;	// Can undo setPar()
	static final int UNDO_PLAYER_NAME = 2;	// Can undo setPlayerName()
	static final int UNDO_SCORE = 3;	// Can undo setScore()
	
	// Score data
	private int mPlayerCount = 2; // Number of players
	private int mHoleCount = 18; // Number of holes
	private String[] mPlayerNames; // Player names
	private int[] mPar; // Par for each hole
	private int[][] mScores; // Score for each player,hole

	// Undo data
	private int mUndoType = 0;	// Type of undo data
	private int mUndoValue;	// Value to undo, for par/score
	private String mUndoName; // Value to undo, for setPlayerName()
	private int mUndoPlayer; // Player index for undo
	private int mUndoHole; // Hole index for undo
	
	// Saved settings
	private int mSavedSelPlayer; // Selected player
	private int mSavedSelHole; // Selected hole
	private boolean mSavedScoreRelative; // Is score displayed relative to par
	private boolean mForceLandscape = false; // Force landscape mode?

	/**
	 * Constructor.
	 */
	public ScoreData() {
		initData();
	}

	/**
	 * @return Force-landscape setting
	 */
	public boolean getForceLandscape() {
		return mForceLandscape;
	}

	public int getHoleCount() {
		return mHoleCount;
	}

	/**
	 * Get par for the specified hole.
	 * 
	 * @param hole
	 *            Hole index to get (0-based, so hole 1 is index 0).
	 * @return Par for the specified hole.
	 */
	public int getPar(int hole) {
		if (hole < 0 || hole >= mHoleCount)
			return -1; // TODO: Throw exception
		return mPar[hole];
	}

	public int getPlayerCount() {
		return mPlayerCount;
	}

	/**
	 * Get the name of the specified player.
	 * 
	 * @param player
	 *            Player index to get.
	 * @return The name of the player.
	 */
	public String getPlayerName(int player) {
		if (player < 0 || player >= mPlayerCount)
			return "bad player index"; // TODO: Throw exception
		return mPlayerNames[player];
	}

	public boolean getSavedScoreRelative() {
		return mSavedScoreRelative;
	}

	public int getSavedSelHole() {
		return mSavedSelHole;
	}

	public int getSavedSelPlayer() {
		return mSavedSelPlayer;
	}

	/**
	 * Get score for the player for the hole.
	 * 
	 * @param player
	 *            Player index.
	 * @param hole
	 *            Hole index (0-based).
	 * @return
	 */
	public int getScore(int player, int hole) {
		if (hole < 0 || hole >= mHoleCount || player < 0 || player >= mPlayerCount)
			return -1; // TODO: Throw exception

		return mScores[player][hole];
	}

	/**
	 * Initialize all fields.
	 */
	private void initData() {
		resetScores();
		resetPlayerNamesAndPar();

		mSavedSelPlayer = 0;
		mSavedSelHole = 0;
		mSavedScoreRelative = false;
	}

	/**
	 * Restore settings from a private file.
	 */
	public void loadFromFile(ContextWrapper wrapper, String filename) {
		ObjectInputStream s = null;
		boolean goodSave = false;
		try {
			FileInputStream f = wrapper.openFileInput(filename);
			s = new ObjectInputStream(f);

			// Check header to make sure this is a file we know how to read
			if (s.readInt() != SAVE_FILE_COOKIE) {
				// Log.d("MiniGolfScore", "Save filename cookie mismatch");
				return;
			}
			if (s.readInt() != SAVE_FILE_VERSION) {
				// Log.d("MiniGolfScore", "Save filename version mismatch");
				return;
			}

			mPlayerCount = s.readInt();
			mHoleCount = s.readInt();

			mPlayerNames = new String[mPlayerCount];
			for (int p = 0; p < mPlayerCount; p++)
				mPlayerNames[p] = (String) s.readObject();

			mPar = new int[mHoleCount];
			for (int h = 0; h < mHoleCount; h++)
				mPar[h] = s.readInt();

			mScores = new int[mPlayerCount][mHoleCount];
			for (int p = 0; p < mPlayerCount; p++) {
				for (int h = 0; h < mHoleCount; h++)
					mScores[p][h] = s.readInt();
			}

			mSavedScoreRelative = s.readBoolean();
			mSavedSelPlayer = s.readInt();
			mSavedSelHole = s.readInt();
			mForceLandscape = s.readBoolean();
			
			// Read undo data
			mUndoType = s.readInt();
			mUndoHole = s.readInt();
			mUndoPlayer = s.readInt();
			mUndoValue = s.readInt();
			mUndoName = (String) s.readObject();
			
			goodSave = true;

		} catch (FileNotFoundException e) {
			// Log.d("MiniGolfScore", "No save file found");
		} catch (IOException e) {
			// Log.d("MiniGolfScore", "Error parsing save file");
		} catch (ClassNotFoundException e) {
			// Log.d("MiniGolfScore", "Error parsing save file - class not found");
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
					// Log.d("MiniGolfScore", "Error closing save file");
					// TODO: do something about being unable to read the file
				}
			}
			if (!goodSave)
				initData(); // Didn't find a good save, so reinitialize data
		}
	}

	private void resetPlayerNamesAndPar() {
		mPlayerNames = new String[mPlayerCount];
		for (int p = 0; p < mPlayerCount; p++)
			mPlayerNames[p] = String.format("Player %d", p + 1);

		mPar = new int[mHoleCount];
		for (int h = 0; h < mHoleCount; h++)
			mPar[h] = DEFAULT_PAR;
	}

	/**
	 * Resets and reallocates scores.
	 */
	public void resetScores() {

		mScores = new int[mPlayerCount][mHoleCount];
		for (int p = 0; p < mPlayerCount; p++) {
			for (int h = 0; h < mHoleCount; h++)
				mScores[p][h] = 0;
		}
		resetUndo();	// This action cannot be undone
	}

	/**
	 * Reset undo buffer
	 */
	private void resetUndo() {
		mUndoType = UNDO_NONE;	
		mUndoName = "";
		mUndoValue = 0;
		mUndoPlayer = 0;
		mUndoHole = 0;
	}

	/**
	 * Save settings to a private file.
	 */
	public void saveToFile(ContextWrapper wrapper, String filename) {
		// If settings aren't actually dirty, we don't need to save them
		// TODO: This doesn't work now that we could potentially be saving to multiple files.
		// if (!mDirty)
		//	 return;

		try {
			FileOutputStream f = wrapper.openFileOutput(filename, 0);
			ObjectOutputStream s = new ObjectOutputStream(f);
			s.writeInt(SAVE_FILE_COOKIE);
			s.writeInt(SAVE_FILE_VERSION);

			// Write scorepad
			s.writeInt(mPlayerCount);
			s.writeInt(mHoleCount);

			for (int p = 0; p < mPlayerCount; p++)
				s.writeObject(mPlayerNames[p]);

			for (int h = 0; h < mHoleCount; h++)
				s.writeInt(mPar[h]);

			for (int p = 0; p < mPlayerCount; p++) {
				for (int h = 0; h < mHoleCount; h++)
					s.writeInt(mScores[p][h]);
			}

			// Write settings
			s.writeBoolean(mSavedScoreRelative);
			s.writeInt(mSavedSelPlayer);
			s.writeInt(mSavedSelHole);
			s.writeBoolean(mForceLandscape);
			
			// Write undo data
			// TODO: Would be more elegant to save only the necessary fields
			s.writeInt(mUndoType);
			s.writeInt(mUndoHole);
			s.writeInt(mUndoPlayer);
			s.writeInt(mUndoValue);
			s.writeObject(mUndoName);
			
			s.close();
		} catch (FileNotFoundException e) {
			// TODO: do something about being unable to save the file
		} catch (IOException e) {
			// TODO: do something about being unable to save the file
		}
	}

	/**
	 * Set the size of the sheet. Old data will be clipped to fit the new size.
	 * 
	 * @param players
	 *            New number of players
	 * @param holes
	 *            New number of holes
	 */
	public void setDimensions(int players, int holes) {
		if (players < 1 || players > 20 || holes < 1 || holes > 90)
			return; // TODO: Throw exception

		// Preserve references to old arrays
		String[] oldPlayerNames = mPlayerNames;
		int[] oldPar = mPar;
		int[][] oldScores = mScores;

		// Copy based on the smaller of the old and new sizes
		int copyPlayerCount = Math.min(mPlayerCount, players);
		int copyHoleCount = Math.min(mHoleCount, holes);

		mPlayerCount = players;
		mHoleCount = holes;

		// Reallocate arrays
		resetPlayerNamesAndPar();
		resetScores();

		// Copy over old data
		for (int p = 0; p < copyPlayerCount; p++) {
			mPlayerNames[p] = oldPlayerNames[p];
			for (int h = 0; h < copyHoleCount; h++)
				mScores[p][h] = oldScores[p][h];
		}
		for (int h = 0; h < copyHoleCount; h++)
			mPar[h] = oldPar[h];
	}

	/**
	 * @param Set force-landscape setting
	 */
	public void setForceLandscape(boolean forceLandscape) {
		mForceLandscape = forceLandscape;
	}
	
	/**
	 * Set par for a hole.
	 * 
	 * @param hole
	 *            Hole index (0-based).
	 * @param par
	 */
	public void setPar(int hole, int par) {
		if (hole < 0 || hole >= mHoleCount)
			return; // TODO: Throw exception
		if (mPar[hole] == par)
			return;
		
		// Save undo data
		mUndoType = UNDO_PAR;
		mUndoHole = hole;
		mUndoValue = mPar[hole];
		
		mPar[hole] = par;
	}

	/**
	 * Set the name of a player.
	 * 
	 * @param player
	 *            Player index.
	 * @param name
	 *            Name for the player.
	 */
	public void setPlayerName(int player, String name) {
		if (player < 0 || player >= mPlayerCount)
			return; // TODO: Throw exception
		if (mPlayerNames[player].equals(name))
			return;
		
		// Save undo data
		mUndoType = UNDO_PLAYER_NAME;
		mUndoPlayer = player;
		mUndoName = mPlayerNames[player];
		
		mPlayerNames[player] = name;
	}

	public void setSavedScoreRelative(boolean savedScoreRelative) {
		mSavedScoreRelative = savedScoreRelative;
	}

	public void setSavedSelHole(int savedSelHole) {
		mSavedSelHole = savedSelHole;
	}

	public void setSavedSelPlayer(int savedSelPlayer) {
		mSavedSelPlayer = savedSelPlayer;
	}

	/**
	 * Set the score for a player.
	 * 
	 * @param player
	 *            Player number.
	 * @param hole
	 *            Hole number.
	 * @param score
	 *            Score to set.
	 */
	public void setScore(int player, int hole, int score) {
		if (hole < 0 || hole >= mHoleCount || player < 0 || player >= mPlayerCount)
			return; // TODO: Throw exception

		if (mScores[player][hole] == score)
			return;	// No change
		
		// Save undo data
		mUndoType = UNDO_SCORE;
		mUndoPlayer = player;
		mUndoHole = hole;
		mUndoValue = mScores[player][hole];
		
		mScores[player][hole] = score;
	}

	/**
	 * Undoes the last setScore(), setPlayerName() or setPar().
	 * @return true if something was undone.
	 */
	public boolean undoLast() {
		switch(mUndoType) {
		case UNDO_PAR:
			setPar(mUndoHole, mUndoValue);
			setSavedSelHole(mUndoHole);
			return true;
		case UNDO_PLAYER_NAME:
			setPlayerName(mUndoPlayer, mUndoName);
			setSavedSelPlayer(mUndoPlayer);
			return true;
		case UNDO_SCORE:
			setScore(mUndoPlayer, mUndoHole, mUndoValue);
			setSavedSelHole(mUndoHole);
			setSavedSelPlayer(mUndoPlayer);
			return true;
		}
		return false;
	}
	
	/**
	 * @return true if there is an action to undo.
	 */
	public boolean canUndo() {
		return (mUndoType != UNDO_NONE);
	}
}
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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
//import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;


/**
 * Mini golf score sheet view
 */
public class SheetView extends View {
	// Constants
	private static final String HEADER_TEXT_PLAYER = "HOLE";
	private static final String HEADER_TEXT_SCORE = "Score";
	private static final String HEADER_TEXT_PAR = "par";
	private static final String HEADER_TEXT_VS_PAR = "+ / -";
	
    // Layout fields
    private Paint mPaintHeader;					// Painter for header row
    private Paint mPaintPar;					// Painter for par row
    private Paint mPaintPlayer;					// Painter for player row
    private Paint mPaintBackground;				// Painter shared between backgrounds
    private Paint mPaintGrid;					// Painter shared between grid lines
    private int mColorBackgroundHeader;			// Background color of header row
    private int mColorBackgroundPar;			// Background color of par row
    private int mColorBackgroundPlayerEven;		// Background color of even player number 
    private int mColorBackgroundPlayerOdd;		// Background color of odd player number
    private int mColorBackgroundActiveRow;		// Background color of active row or column
    private int mColorBackgroundActiveCell;		// Background color of active cell (intersection of active row and column)
    
    // Measured dimensions
    private int mRowHeightHeader;				// Row height of status
    private int mRowHeightPar;					// Row height of par
    private int mRowHeightPlayer;				// Row height of player
    private int mTextOffsetHeader;				// Vertical offset of text inside header row
    private int mTextOffsetPar;					// Vertical offset of text inside par row
    private int mTextOffsetPlayer;				// Vertical offset of text inside player row
    private int mColWidthPlayer;				// Column width of player
    private int mColWidthHole;					// Column width of hole
    private int mColWidthScore;					// Column width of score
    
    private int mScrollableWidth;				// Width of scrollable columns
    private int mScrollableHeight;				// Height of scrollable rows
    private int mFixedLeft, mFixedRight, mFixedTop, mFixedBottom;	// Width of unscrollable regions at edges
    private int mScrollVisibleWidth;			// Width of visible scrollable region 
    private int mScrollVisibleHeight;			// Height of visible scrollable region 

    // Data
    private ScoreData mData;				// Score data
    
    // Data for the grid control
    private int[] mPlayerTotal;					// Total player score, for holes played so far

    // Current control settings
    private boolean mScoreRelative = false;		// Show relative instead of absolute score
    private int mSelPlayer = -1;				// Selected player; -1 = none
    private int mSelHole = -1; 					// Selected hole; -1 = none

    private OnEditListener mOnEditListener;
    private GestureDetector mGestureDetector;

    
    /** ******************************************************************************************
     * Constructor for manual instantiation.
     */
    public SheetView(Context context) {
        // Call the more complex constructor
        this(context, null);
    }

    /** ******************************************************************************************
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     *
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public SheetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSheetView();

        // Read settings from XML
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SheetView);
        // TODO: Read settings; currently all hard-coded in init.
        a.recycle();
    }

    private final void initSheetView() {
        // Log.d("SheetView", "initSheetView()");

        mPaintHeader = new Paint();
        mPaintHeader.setAntiAlias(true);
        mPaintHeader.setTypeface(Typeface.DEFAULT_BOLD);
        mPaintHeader.setTextSize(16);
        mPaintHeader.setColor(0xFF000000);
        mPaintHeader.setTextAlign(Align.CENTER);

        mPaintPar = new Paint();
        mPaintPar.setAntiAlias(true);
        mPaintPar.setTextSize(16);
        mPaintPar.setColor(0xFF000000);
        mPaintPar.setTextAlign(Align.CENTER);
        
        mPaintPlayer = new Paint();
        mPaintPlayer.setAntiAlias(true);
        mPaintPlayer.setTextSize(30);
        mPaintPlayer.setColor(0xFF000000);

        mPaintBackground = new Paint();
        mPaintBackground.setStyle(Style.FILL);
        mColorBackgroundHeader = 0xFFC0C0FF;
        mColorBackgroundPar = 0xFFB0B0FF;
        mColorBackgroundPlayerEven = 0xFFFFFFFF; 
        mColorBackgroundPlayerOdd = 0xFFC0FFC0;
        mColorBackgroundActiveRow = 0xFFFFC080;
        mColorBackgroundActiveCell = 0xFFFF8000;
        
        mPaintGrid = new Paint();
        mPaintGrid.setStyle(Style.STROKE);
        mPaintGrid.setColor(0xFF000000);

        mData = new ScoreData();
        
        setPadding(0, 0, 0, 0);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);			// See View.onDraw() - since we override it, clear this flag

        initGestureDetector();

        setScrollBarStyle(SCROLLBARS_INSIDE_INSET);
        
        // TODO: This is how we allocate our score totals
        checkForRelayout();
    }

    /**
     * Initialize the gesture detector.
     */
    private final void initGestureDetector() {
        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        	@Override
        	public boolean onDown(MotionEvent ev) {
        		// Log.d("SheetView", "Gesture DOWN");
            	if (!hasFocus())
            		requestFocus();
        		return true;
        	}
        	
        	@Override
        	public boolean onSingleTapUp(MotionEvent ev) {
        		int x = (int)ev.getX();
        		int y = (int)ev.getY();
        		// Log.d("SheetView", String.format("Gesture TAP at %d, %d", x, y));
        		handlePress(x, y, false);
        		return true;
        	}
        	
			@Override
			public void onLongPress(MotionEvent ev) {
        		int x = (int)ev.getX();
        		int y = (int)ev.getY();
        		// Log.d("SheetView", String.format("Gesture LONG at %d, %d", x, y));
        		handlePress(x, y, true);
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        		// Log.d("SheetView", String.format("Gesture scroll %f,%f", distanceX, distanceY));
        		// If initial motion event was out of the fixed section, only scroll row/col
        		// TODO: Technically incorrect, since this doesn't account for padding
        		if (e1.getX() < mFixedLeft || e1.getX() > getWidth() - mFixedRight)
        			distanceX = 0;
        		if (e1.getY() < mFixedTop || e1.getY() > getHeight() - mFixedBottom)
        			distanceY = 0;

        		// TODO: Probably should use scrollTo or use a scroller, so that we don't truncate small scrolls
        		
        		scrollBy((int)distanceX, (int)distanceY);
        		return true;
        	}
        	// TODO: Handle fling
        });
    }
    
	/**
     * @see android.view.View#measure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	computeDesiredDimensions();
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            // Log.d("SheetView", "measureWidth exactly " + specSize);
        	result = specSize;
        } else {
            // Measure the text
            result = getPaddingLeft() + mFixedLeft + mScrollableWidth  + mFixedRight + getPaddingRight();
            // Log.d("SheetView", "measureWidth desired " + result);
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
                // Log.d("SheetView", "measureWidth at_most " + result);
            }
        }

        return result;
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            // Log.d("SheetView", "measureHeight exactly " + specSize);
            result = specSize;
        } else {
            // Measure the text
            result = getPaddingTop() + mFixedTop + mScrollableHeight + mFixedBottom + getPaddingBottom();
            // Log.d("SheetView", "measureHeight desired " + result);
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
                // Log.d("SheetView", "measureHeight at_most " + result);
            }
        }
        
        return result;
    }

    /**
     * Re-measure the contents, invalidate, and check for re-layout.
     */
    public void checkForRelayout() {
    	/* TODO: Is there a better way to do this?  Perhaps the ScoreData can have a listener
    	 * queue we can hook ourself into and be informed of changes.
    	 */

    	// TODO - kinda kludgy to forcibly reallocate this here
        mPlayerTotal = new int[mData.getPlayerCount()];
    	
		computeDesiredDimensions();
		requestLayout();
		
		/* Scroll to our current scroll position.  If our scroll range is smaller than it used
		 * to be, this will clip to the new valid range.
		 */
		scrollTo(getScrollX(), getScrollY());
		
		invalidate();
    }
    
	/** 
     * Computes the dimensions of the sheet cells, based on the current settings.
     * These are the desired dimensions, so are not affected by measure specs from onMeasure().
     */
    private void computeDesiredDimensions() {
    	
        mColWidthHole = 5 + (int)Math.max(mPaintHeader.measureText("36"), 
        								  mPaintPlayer.measureText("36"));
        mScrollableWidth = mData.getHoleCount() * mColWidthHole;
        
        mColWidthScore = 5 + (int)Math.max(mPaintHeader.measureText(HEADER_TEXT_SCORE), 
        							       mPaintPlayer.measureText("+36"));
      	mFixedRight = mColWidthScore;

        mRowHeightHeader = 5 + (int)(mPaintHeader.descent() - mPaintHeader.ascent());
        mRowHeightPar = 7 + (int)(mPaintPar.descent() - mPaintPar.ascent());
        mFixedTop = mRowHeightHeader + mRowHeightPar;
        mFixedBottom = 0;
        
        mRowHeightPlayer = 5 + (int)(mPaintPlayer.descent() - mPaintPlayer.ascent());
        mScrollableHeight = mData.getPlayerCount() * mRowHeightPlayer;
        
        mTextOffsetHeader = 2 -(int)mPaintHeader.ascent();
        mTextOffsetPar = 2 - (int)mPaintPar.ascent();
        mTextOffsetPlayer = 2 -(int)mPaintPlayer.ascent();
    	
        // Determine the maximum width of the player name column, based on the current names
        mColWidthPlayer = (int)Math.max(mPaintHeader.measureText(HEADER_TEXT_PLAYER),
        								mPaintPar.measureText(HEADER_TEXT_PAR));
        for (int p = 0; p < mData.getPlayerCount(); p++) {
        	int playerWidth = 7 + (int)mPaintPlayer.measureText(mData.getPlayerName(p));
        	if (mColWidthPlayer < playerWidth)
        		mColWidthPlayer = playerWidth;
        }
        if (mFixedLeft != mColWidthPlayer) {
            mFixedLeft = mColWidthPlayer;
    		setSelectedHole(mSelHole);	// Scroll selection into view if the column width changed
    		/* TODO: One thing that's a little odd there is that it only scrolls the hole into view 
    		 * if the player name changed AND the player width changed.  If just the player name
    		 * changes but the column width doesn't change, we don't scroll the hole into view.
    		 * From a UI-design standpoint, should we be consistent and always scroll the hole 
    		 * into view? 
    		 */
        }
    }
    
    /**
     * Called when size has changed.
	 * @see android.view.View#onSizeChanged(int, int, int, int)
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		
		// Assume scrollbars will be visible, so they count against our padding
		setHorizontalScrollBarEnabled(true);
		setVerticalScrollBarEnabled(true);

		// Determine our new scroll extents
		computeScollVisible();
		
		// Disable scrollbars we didn't end up needing
		if (mScrollVisibleWidth >= mScrollableWidth)
			setHorizontalScrollBarEnabled(false);
		if (mScrollVisibleHeight >= mScrollableHeight)
			setVerticalScrollBarEnabled(false);
			
		// Since that could have changed the padding, recompute the scroll extents
		computeScollVisible();

		/* TODO: That's kind of lame.  Is there a better way to get scrollbars not to reserve 
		 * their padding when invisible?
		 */
		
		// Re-scroll to our current scroll position, to clip it to the screen
		setSelectedHole(mSelHole);
		setSelectedPlayer(mSelPlayer);
    }

	/**
	 * Compute the scroll extents given the current padding.  Note that changing the scroll 
	 * extents may change the state of the scrollbars, which in turn affects the padding; this
	 * is taken care of in onSizeChanged().
	 */
	private void computeScollVisible() {
		mScrollVisibleWidth = Math.max(
				0, getWidth() - getPaddingLeft() - mFixedLeft - mFixedRight - getPaddingRight());
		mScrollVisibleHeight = Math.max(
				0, getHeight() - getPaddingTop() - mFixedTop - mFixedBottom - getPaddingBottom());
	}

	/**
     * Draw the control
     *
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        /* Save the canvas in its original scrolled state, so that we can restore it before exit.
         * If we don't do this, the scroll bars end up being drawn in the wrong place
         */
    	int x, y;

        Rect rect_pad = new Rect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), 
        						 getHeight() - getPaddingBottom());
        Rect rect_scroll = new Rect(mFixedLeft, mFixedTop, rect_pad.width() - mFixedRight, 
        		                    rect_pad.height() - mFixedBottom);

        int maxx = rect_pad.width();
    	int maxy = Math.min(rect_pad.height(), mFixedTop + mScrollableHeight + mFixedBottom);
    	int players = mData.getPlayerCount();
    	int holes = mData.getHoleCount();
    	
        // Untranslate the canvas and inset by the padding
        canvas.save();	// #1
        canvas.translate(getScrollX(), getScrollY());
        canvas.clipRect(rect_pad);
        canvas.translate(getPaddingLeft(), getPaddingTop());
        
        // Draw fixed top backgrounds
        mPaintBackground.setColor(mColorBackgroundHeader);
        canvas.drawRect(0, 0, maxx, mRowHeightHeader, mPaintBackground);
        mPaintBackground.setColor(mColorBackgroundPar);
        canvas.drawRect(0, mRowHeightHeader, maxx, mRowHeightHeader + mRowHeightPar, 
        		        mPaintBackground);
        	
        // Inset the clip rect and draw the player row backgrounds
        canvas.save();
        canvas.clipRect(0, mFixedTop, maxx, maxy - mFixedBottom);
        canvas.translate(0, mFixedTop - getScrollY());
        y = 0;
        for (int p = 0; p < players; p++) {
            if (p == mSelPlayer)
            	mPaintBackground.setColor(mColorBackgroundActiveRow);
            else if (p % 2 == 1)
            	mPaintBackground.setColor(mColorBackgroundPlayerOdd);
            else
            	mPaintBackground.setColor(mColorBackgroundPlayerEven);
            canvas.drawRect(0, y, maxx, y + mRowHeightPlayer, mPaintBackground);
            y += mRowHeightPlayer;
        }
        canvas.restore();
        
        // If there's an active column, inset the clip rect and draw its background and the
        if (mSelHole >= 0) {
            x = mSelHole * mColWidthHole;
        	mPaintBackground.setColor(mColorBackgroundActiveRow);
        	
            canvas.save();
            canvas.clipRect(mFixedLeft, 0, maxx - mFixedRight, getHeight());
            canvas.translate(mFixedLeft - getScrollX(), 0);
            canvas.drawRect(x, 0, x + mColWidthHole, maxy, mPaintBackground);
            canvas.restore();

            // If there's an active cell, draw it darker
            if (mSelPlayer >= 0) {
            	y = mSelPlayer * mRowHeightPlayer;
            	mPaintBackground.setColor(mColorBackgroundActiveCell);
                canvas.save();
                canvas.clipRect(rect_scroll);
                canvas.translate(mFixedLeft - getScrollX(), mFixedTop - getScrollY());
                canvas.drawRect(x, y, x + mColWidthHole, y + mRowHeightPlayer, mPaintBackground);
                canvas.restore();
            	
            }
        }
        
        // Draw fixed grid
        canvas.drawLine(0, mFixedTop - 2, maxx, mFixedTop - 2, mPaintGrid);
        canvas.drawLine(0, mFixedTop, maxx, mFixedTop, mPaintGrid);
        canvas.drawLine(mFixedLeft, 0, mFixedLeft, maxy, mPaintGrid);
        canvas.drawLine(mFixedLeft - 2, 0, mFixedLeft - 2, maxy, mPaintGrid);
        canvas.drawLine(maxx - mFixedRight, 0, maxx - mFixedRight, maxy, mPaintGrid);
        canvas.drawLine(maxx - mFixedRight + 2, 0, maxx - mFixedRight + 2, maxy, mPaintGrid);

        // Draw fixed top foregrounds
        canvas.drawText(HEADER_TEXT_PLAYER, mColWidthPlayer / 2, mTextOffsetHeader, mPaintHeader);
        canvas.drawText(HEADER_TEXT_SCORE, maxx - mColWidthScore / 2, mTextOffsetHeader, 
        		        mPaintHeader);

        // Draw headers for holes and total score
        canvas.save();
        canvas.clipRect(mFixedLeft, 0, maxx - mFixedRight, maxy);
        canvas.translate(mFixedLeft - getScrollX(), 0);
        for (int h = 0; h < holes; h++) {
            x = h * mColWidthHole;
            canvas.drawText(Integer.toString(h + 1), x + mColWidthHole / 2, mTextOffsetHeader, 
            		        mPaintHeader);
            canvas.drawLine(x + mColWidthHole, 0, x + mColWidthHole, maxy, mPaintGrid);
        }
        canvas.restore();
        
        // Draw par row
        canvas.drawLine(0, mRowHeightHeader, maxx, mRowHeightHeader, mPaintGrid);
        y = mRowHeightHeader + mTextOffsetPar;
        canvas.drawText(HEADER_TEXT_PAR, mColWidthPlayer / 2, y, mPaintPar);
        
        canvas.save();
        canvas.clipRect(mFixedLeft, 0, maxx - mFixedRight, maxy);
        canvas.translate(mFixedLeft - getScrollX(), 0);
        int total = 0;
        for (int h = 0; h < holes; h++) {
        	int par = mData.getPar(h);
        	if (par <= 0)
        		continue;
            x = h * mColWidthHole;
            canvas.drawText(Integer.toString(par), x + mColWidthHole / 2, y, mPaintPar);
        	total += par;
        }
        canvas.restore();
        if (mScoreRelative)
        	canvas.drawText(HEADER_TEXT_VS_PAR, maxx - mColWidthScore / 2, y, mPaintPar);
        else
        	canvas.drawText(Integer.toString(total), maxx - mColWidthScore / 2, y, mPaintPar);
        
        // Draw scores
        canvas.save();
        canvas.clipRect(rect_scroll);
        canvas.translate(mFixedLeft - getScrollX(), mFixedTop - getScrollY());
        mPaintPlayer.setTextAlign(Align.CENTER);
        for (int p = 0; p < players; p++) {
            y = p * mRowHeightPlayer + mTextOffsetPlayer;
            mPlayerTotal[p] = 0;
            for (int h = 0; h < holes; h++) {
            	int score = mData.getScore(p, h);
            	if (score <= 0)
            		continue;
	            x = h * mColWidthHole;
	            if (mScoreRelative) {
	            	int delta = score - mData.getPar(h);
	            	if (delta >= 10) {
	            		// Use a smaller font
	            		canvas.drawText(String.format("%+d", delta), x + mColWidthHole / 2, y, 
	            				        mPaintHeader);
	            	} else {
	            		canvas.drawText(String.format("%+d", delta), x + mColWidthHole / 2, y, 
	            				        mPaintPlayer);
	            	}
	            	mPlayerTotal[p] += score - mData.getPar(h);
	            } else {
	            	canvas.drawText(Integer.toString(score), x + mColWidthHole / 2, y, 
	            			        mPaintPlayer);
	            	mPlayerTotal[p] += score;
	            }
            }
        }
        canvas.restore();
        
        // Translate vertically to the current scroll position
        canvas.save();
        canvas.clipRect(0, mFixedTop, maxx, maxy - mFixedBottom);
        canvas.translate(0, mFixedTop - getScrollY());
        
        // Draw the non-scrolling portions of the player rows
        for (int p = 0; p < players; p++) {
        	y = p * mRowHeightPlayer;
            mPaintPlayer.setTextAlign(Align.LEFT);
            canvas.drawText(mData.getPlayerName(p), 2, y + mTextOffsetPlayer, mPaintPlayer);
            mPaintPlayer.setTextAlign(Align.CENTER);
            String s = String.format(mScoreRelative ? "%+d" : "%d", mPlayerTotal[p]);
            canvas.drawText(s, maxx - mColWidthScore / 2, y + mTextOffsetPlayer, mPaintPlayer);
            canvas.drawLine(0, y + mRowHeightPlayer, maxx, y + mRowHeightPlayer, mPaintGrid);
        }
        canvas.restore();

        // Restore the canvas, so that scrollbars will be drawn properly
        canvas.restore();	// #1
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
    {
    	super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    	// Need to redraw with/without highlight row/col 
    	invalidate();
    }

    @Override
    public boolean onTouchEvent (MotionEvent event)
    {
    	// Pass to gesture detector
    	return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event)
    {
    	// final CharSequence[] eventNames = {"DOWN", "UP", "MOVE", "CANCEL"};
    	// Log.d("SheetView", String.format("TrackballEvent %s %f %f %d", eventNames[event.getAction()], event.getX(), event.getY(), event.getHistorySize()));

    	switch(event.getAction()) {
    	case MotionEvent.ACTION_DOWN:
    		// Pop up score dialog
    		if (mSelPlayer >=0 && mSelHole >= 0 && mOnEditListener != null)
  				mOnEditListener.onEditScore(mSelPlayer, mSelHole);
    		return true;
    	case MotionEvent.ACTION_MOVE:
    		// TODO: Use scroller to integrate small movements
    		// TODO: Change selected player and/or hole
    		scrollBy((int)(event.getX() * 16), (int)(event.getY() * 16));
    		return true;
    	}
    	return true;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
    	// Log.d("SheetView", String.format("KeyDown %d %d", keyCode, event.getRepeatCount()));

    	switch(keyCode) {
    	case KeyEvent.KEYCODE_DPAD_DOWN:
			moveSelectionBy(1, 0);
    		return true;
    	case KeyEvent.KEYCODE_DPAD_UP:
			moveSelectionBy(-1, 0);
    		return true;
    	case KeyEvent.KEYCODE_DPAD_LEFT:
			moveSelectionBy(0, -1);
    		return true;
    	case KeyEvent.KEYCODE_DPAD_RIGHT:
			moveSelectionBy(0, 1);
    		return true;
    	case KeyEvent.KEYCODE_DPAD_CENTER:
    		// Pop up score dialog
    		if (mSelPlayer >=0 && mSelHole >= 0 && mOnEditListener != null)
  				mOnEditListener.onEditScore(mSelPlayer, mSelHole);
    		return true;
    	}
		return false;
    }

    @Override
    public void scrollTo(int x, int y)
    {
    	/* Clip scrolling.  Note that we also need to keep from scrolling at all if the scroll
    	 * extent is greater than the scroll range. */
    	
    	if(x < 0 || mScrollableWidth <= mScrollVisibleWidth)
    		x = 0;
    	else if(x > 0)
    		x = Math.min(x, mScrollableWidth - mScrollVisibleWidth);
    	
    	if(y < 0 || mScrollableHeight <= mScrollVisibleHeight)
    		y = 0;
    	else if(y > 0)
    		y = Math.min(y, mScrollableHeight - mScrollVisibleHeight);
    	
    	// Log.d("SheetView", String.format("scrollTo %d,%d", x, y));

    	super.scrollTo(x, y);
    }

    @Override
    protected int computeHorizontalScrollExtent()
    {
    	return mScrollVisibleWidth;
    }
    
    @Override
    protected int computeVerticalScrollExtent()
    {
    	return mScrollVisibleHeight;
    }
    
    @Override
    protected int computeHorizontalScrollRange()
    {
    	return mScrollableWidth;
    }
    @Override
    protected int computeVerticalScrollRange()
    {
    	return mScrollableHeight;
    }

    /**
     * Set the score data backing this view.
     * @param data
     */
    public void setScoreData(ScoreData data) {
    	mData = data;
    	requestLayout();
    	invalidate();
    }
    
    /**
     * Set the selected hole.
     * @param hole Hole number, or -1 if no hole selected.
     */
    public void setSelectedHole(int hole)
    {
    	if (hole < 0)
    		hole = -1;	// No selection
    	else if (hole >= mData.getHoleCount())
    		hole = mData.getHoleCount() - 1;
    	
		mSelHole = hole;
		
		// If selected hole isn't visible, scroll it onscreen
		int x = mSelHole * mColWidthHole;
		int dx = x - getScrollX();
		if (dx < 0)
			scrollBy(dx, 0);
		dx = (x + mColWidthHole) - (getScrollX() + mScrollVisibleWidth);
		if (dx > 0)
			scrollBy(dx, 0);
	
		invalidate();
    }
    
    /**
     * Set the selected player.
     * @param player Player number, or -1 if no player selected.
     */
	public void setSelectedPlayer(int player) {
    	if (player < 0)
    		player = -1;	// No selection
    	else if (player >= mData.getPlayerCount())
    		player = mData.getPlayerCount() - 1;
    	
		mSelPlayer = player;
		
		// If selected player isn't visible, scroll it onscreen
		int y = mSelPlayer * mRowHeightPlayer;
		int dy = y - getScrollY();
		if (dy < 0)
			scrollBy(0, dy);
		dy = (y + mRowHeightPlayer) - (getScrollY() + mScrollVisibleHeight);
		if (dy > 0)
			scrollBy(0, dy);
		
		invalidate();
	}

	/**
	 * Move the selection relative to its current location.  Prevents moving the selection
	 * out of the valid range.
	 * @param dPlayer Amount to move player selection.
	 * @param dHole Amount to move hole selection.
	 */
	public void moveSelectionBy(int dPlayer, int dHole) {
		// Move player if necessary
		if (dPlayer < 0 && mSelPlayer + dPlayer >= 0)
			setSelectedPlayer(mSelPlayer + dPlayer);
		else if (dPlayer > 0 && mSelPlayer + dPlayer < mData.getPlayerCount())
			setSelectedPlayer(mSelPlayer + dPlayer);

		// Move hole if necessary
		if (dHole < 0 && mSelHole + dHole >= 0)
			setSelectedHole(mSelHole + dHole);
		else if (dHole > 0 && mSelHole + dHole < mData.getHoleCount())
			setSelectedHole(mSelHole + dHole);
	}
	
    /* Getters */
    public boolean getScoreRelative() {
    	return mScoreRelative;
    }
	public int getSelectedHole() {
		return mSelHole;
	}
	public int getSelectedPlayer() {
		return mSelPlayer;
	}
	
    /**
     * Set whether displayed scores are absolute or relative to par.
     * @param scoreIsRelative If true, scores are displayed relative to par.
     */
    public void setScoreRelative(boolean scoreIsRelative) {
    	mScoreRelative = scoreIsRelative;
    	invalidate();
    }

    /**
     * Handle a gesture (tap or long press).
     * @param x X-coord of gesture.
     * @param y Y-coord of gesture.
     * @param isLong If true, gesture is a long-press; if false, a tap.
     */
    private void handlePress(int x, int y, boolean isLong) {
    	
		int hole = -1, player = -1;

		// Translate by fixed region and padding
		x -= getPaddingLeft() + mFixedLeft;
		y -= getPaddingTop() + mFixedTop;
		
		if (x >= 0 && x < mScrollVisibleWidth)
		{
			hole = (x + getScrollX()) / mColWidthHole;
			if (hole < mData.getHoleCount())
				setSelectedHole(hole);
			else
				hole = -1;
		}
		if (y >= 0 && y < mScrollVisibleHeight)
		{
			player = (y + getScrollY()) / mRowHeightPlayer;
			if (player < mData.getPlayerCount())
				setSelectedPlayer(player);
			else
				player = -1;
		}
		
		if (x < 0 && player >= 0)
		{
			// In players list
			if (isLong)
			{
				// Edit player name
				if (mOnEditListener != null)
					mOnEditListener.onEditPlayerName(player);
			}
			else if (mSelHole >= 0)
			{
				// Increment score for selected player and hole
				mData.setScore(player, mSelHole, mData.getScore(player, mSelHole) + 1);
				// TODO: better way to scroll selected hole into view?
		    	setSelectedHole(mSelHole);
			}
		}
		else if (y < 0 && hole >= 0 && isLong)
		{
			// In holes part of header; edit par for that hole
			if (mOnEditListener != null)
				mOnEditListener.onEditPar(hole);
		}
		else if (player >=0 && hole >= 0)
		{
			// Edit a score
			if (mOnEditListener != null)
				mOnEditListener.onEditScore(player, hole);
		}
		else if (x > mFixedRight && y < mFixedTop)
		{
			// In score region, so toggle whether scores are relative
			mScoreRelative = !mScoreRelative;
			invalidate();
		}
    }

    /**
     * Public interface for listeners to be called when parts of the sheet are edited.
     */
    public interface OnEditListener {
    	/**
    	 * Called when par needs to be edited for a hole. 
    	 * @param hole Hole index to edit.
    	 */
    	void onEditPar(int hole);
    	
    	/**
    	 * Called when a player name needs to be edited.
    	 * @param player Player index to edit.
    	 */
    	void onEditPlayerName(int player);
    	
    	/**
    	 * Called when a score needs to be edited.
    	 * @param player Player index to edit.
    	 * @param hole Hole index to edit.
    	 */
    	void onEditScore(int player, int hole);
    }
    
    public void SetOnEditListener(OnEditListener listener) {
    	mOnEditListener = listener;
    }
}



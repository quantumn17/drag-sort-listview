package com.mobeta.android.dslv;

import android.graphics.Point;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;

/**
 * Class that starts and stops item drags on a {@link DragSortListView}
 * based on touch gestures. This class also inherits from
 * {@link SimpleFloatViewManager}, which provides basic float View
 * creation.
 *
 * An instance of this class is meant to be passed to the methods
 * {@link DragSortListView#setTouchListener()} and
 * {@link DragSortListView#setFloatViewManager()} of your
 * {@link DragSortListView} instance.
 */
public class DragSortController extends SimpleFloatViewManager implements View.OnTouchListener, GestureDetector.OnGestureListener {
	public static final String TAG = "DragSortController";
    /**
     * Drag init mode enum.
     */
    public static final int ON_DOWN = 0;
    public static final int ON_DRAG = 1;
    public static final int ON_LONG_PRESS = 2;

    private int mDragInitMode = ON_DOWN;

    private boolean mSortEnabled = true;

    /**
     * Swipe mode enum.
     */
    public static final int CLICK_REMOVE = 0;
    public static final int FLING_REMOVE = 1;

    /**
     * The current swipe mode.
     */
    private int mSwipeMode;

    private boolean mSwipeEnabled = false;
    private boolean mIsRemoving = false;

    private GestureDetector mDetector;

    private GestureDetector mFlingSwipeDetector;

    private int mTouchSlop;

    public static final int MISS = -1;

    private int mHitPos = MISS;
    private int mFlingHitPos = MISS;

    private int mClickSwipeHitPos = MISS;

    private int[] mTempLoc = new int[2];

    private int mItemX;
    private int mItemY;

    private int mCurrX;
    private int mCurrY;

    private boolean mDragging = false;

    private float mFlingSpeed = 500f;

    private int mDragHandleId;

    private int mClickSwipeId;

    private int mFlingHandleId;
    private int mBackId;
    
    private boolean mCanDrag;

    private DragSortListView mDslv;
    private int mPositionX;

    /**
     * Calls {@link #DragSortController(DragSortListView, int)} with a
     * 0 drag handle id, FLING_RIGHT_REMOVE swipe mode,
     * and ON_DOWN drag init. By default, sorting is enabled, and
     * removal is disabled.
     *
     * @param dslv The DSLV instance
     */
    public DragSortController(DragSortListView dslv) {
        this(dslv, 0, ON_DOWN, FLING_REMOVE, 0);
    }

    public DragSortController(DragSortListView dslv, int dragHandleId, int dragInitMode, int swipeMode) {
        this(dslv, dragHandleId, dragInitMode, swipeMode, 0, 0);
    }

    public DragSortController(DragSortListView dslv, int dragHandleId, int dragInitMode, int swipeMode, int clickSwipeId) {
        this(dslv, dragHandleId, dragInitMode, swipeMode, clickSwipeId, 0, 0);
    }
    
    public DragSortController(DragSortListView dslv, int dragHandleId, int dragInitMode, int swipeMode, int clickSwipeId, int backId){
    	this(dslv, dragHandleId, dragInitMode, swipeMode, clickSwipeId, 0, backId);
    	Log.d(TAG, "back view id in constructor: " + backId);
    }

    /**
     * By default, sorting is enabled, and removal is disabled.
     *
     * @param dslv The DSLV instance
     * @param dragHandleId The resource id of the View that represents
     * the drag handle in a list item.
     */
    public DragSortController(DragSortListView dslv, int dragHandleId, int dragInitMode,
            int swipeMode, int clickSwipeId, int flingHandleId, int backId) {
        super(dslv);
        mDslv = dslv;
        mDetector = new GestureDetector(dslv.getContext(), this);
        mFlingSwipeDetector = new GestureDetector(dslv.getContext(), mFlingSwipeListener);
        mFlingSwipeDetector.setIsLongpressEnabled(false);
        mTouchSlop = ViewConfiguration.get(dslv.getContext()).getScaledTouchSlop();
        mDragHandleId = dragHandleId;
        mClickSwipeId = clickSwipeId;
        mFlingHandleId = flingHandleId;
        mBackId = backId;
        Log.d(TAG, "flingHandle id in setter: " + flingHandleId);
        Log.d(TAG, "back view id in setter: " + backId);
                
        setSwipeMode(swipeMode);
        setDragInitMode(dragInitMode);
    }


    public int getDragInitMode() {
        return mDragInitMode;
    }

    /**
     * Set how a drag is initiated. Needs to be one of
     * {@link ON_DOWN}, {@link ON_DRAG}, or {@link ON_LONG_PRESS}.
     *
     * @param mode The drag init mode.
     */
    public void setDragInitMode(int mode) {
        mDragInitMode = mode;
    }

    /**
     * Enable/Disable list item sorting. Disabling is useful if only item
     * removal is desired. Prevents drags in the vertical direction.
     *
     * @param enabled Set <code>true</code> to enable list
     * item sorting.
     */
    public void setSortEnabled(boolean enabled) {
        mSortEnabled = enabled;
    }

    public boolean isSortEnabled() {
        return mSortEnabled;
    }

    /**
     * One of {@link CLICK_REMOVE}, {@link FLING_RIGHT_REMOVE},
     * {@link FLING_LEFT_REMOVE},
     * {@link SLIDE_RIGHT_REMOVE}, or {@link SLIDE_LEFT_REMOVE}.
     */
    public void setSwipeMode(int mode) {
        mSwipeMode = mode;
    }

    public int getSwipeMode() {
        return mSwipeMode;
    }

    /**
     * Enable/Disable item removal without affecting swipe mode.
     */
    public void setSwipeEnabled(boolean enabled) {
        mSwipeEnabled = enabled;
    }

    public boolean isSwipeEnabled() {
        return mSwipeEnabled;
    }

    /**
     * Set the resource id for the View that represents the drag
     * handle in a list item.
     *
     * @param id An android resource id.
     */
    public void setDragHandleId(int id) {
        mDragHandleId = id;
    }

    /**
     * Set the resource id for the View that represents the fling
     * handle in a list item.
     *
     * @param id An android resource id.
     */
    public void setFlingHandleId(int id) {
        mFlingHandleId = id;
    }

    /**
     * Set the resource id for the View that represents click
     * removal button.
     *
     * @param id An android resource id.
     */
    public void setClickSwipeId(int id) {
        mClickSwipeId = id;
    }
    
    /* Set the resource id for the view that represents what
     * appears after a swipe
     */
    
    public void setBackId(int id){
    	mBackId = id;
    }

    /**
     * Sets flags to restrict certain motions of the floating View
     * based on DragSortController settings (such as swipe mode).
     * Starts the drag on the DragSortListView.
     *
     * @param position The list item position (includes headers).
     * @param deltaX Touch x-coord minus left edge of floating View.
     * @param deltaY Touch y-coord minus top edge of floating View.
     *
     * @return True if drag started, false otherwise.
     */
    public boolean startDrag(int position, int deltaX, int deltaY) {

        int dragFlags = 0;
        if (mSortEnabled && !mIsRemoving) {
            dragFlags |= DragSortListView.DRAG_POS_Y | DragSortListView.DRAG_NEG_Y;
        }
        if (mSwipeEnabled && mIsRemoving) {
            dragFlags |= DragSortListView.DRAG_POS_X;
            dragFlags |= DragSortListView.DRAG_NEG_X;
        }

        mDragging = mDslv.startDrag(position - mDslv.getHeaderViewsCount(), dragFlags, deltaX,
                deltaY);
        return mDragging;
    }

    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        if (!mDslv.isDragEnabled() || mDslv.listViewIntercepted()) {
            return false;
        }

        mDetector.onTouchEvent(ev);
        if (mSwipeEnabled && mDragging && mSwipeMode == FLING_REMOVE) {
            mFlingSwipeDetector.onTouchEvent(ev);
        }

        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mCurrX = (int) ev.getX();
                mCurrY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (mSwipeEnabled && mIsRemoving) {
                    int x = mPositionX >= 0 ? mPositionX : -mPositionX;
                    int swipePoint = mDslv.getWidth() / 2;
                    if (x > swipePoint) {
                        mDslv.stopDragWithVelocity(true, 0);
                    }
                }
            case MotionEvent.ACTION_CANCEL:
                mIsRemoving = false;
                mDragging = false;
                break;
        }

        return false;
    }

    /**
     * Overrides to provide fading when slide removal is enabled.
     */
    @Override
    public void onDragFloatView(View floatView, Point position, Point touch) {

        if (mSwipeEnabled && mIsRemoving) {
            mPositionX = position.x;
        }
    }

    /**
     * Get the position to start dragging based on the ACTION_DOWN
     * MotionEvent. This function simply calls
     * {@link #dragHandleHitPosition(MotionEvent)}. Override
     * to change drag handle behavior;
     * this function is called internally when an ACTION_DOWN
     * event is detected.
     *
     * @param ev The ACTION_DOWN MotionEvent.
     *
     * @return The list position to drag if a drag-init gesture is
     * detected; MISS if unsuccessful.
     */
    public int startDragPosition(MotionEvent ev) {
        return dragHandleHitPosition(ev);
    }

    public int startFlingPosition(MotionEvent ev) {
        Log.d("mobeta", "startFlingPosition called");
    	return mSwipeMode == FLING_REMOVE ? flingHandleHitPosition(ev) : MISS;
        
    }

    /**
     * Checks for the touch of an item's drag handle (specified by
     * {@link #setDragHandleId(int)}), and returns that item's position
     * if a drag handle touch was detected.
     *
     * @param ev The ACTION_DOWN MotionEvent.

     * @return The list position of the item whose drag handle was
     * touched; MISS if unsuccessful.
     */
    public int dragHandleHitPosition(MotionEvent ev) {
        return viewIdHitPosition(ev, mDragHandleId);
    }

    public int flingHandleHitPosition(MotionEvent ev) {
        return viewIdHitPosition(ev, mFlingHandleId);
    }

    public int viewIdHitPosition(MotionEvent ev, int id) {
    	Log.d("mobeta", "viewIdHitPosition called");
    	final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        int touchPos = mDslv.pointToPosition(x, y); // includes headers/footers

        final int numHeaders = mDslv.getHeaderViewsCount();
        final int numFooters = mDslv.getFooterViewsCount();
        final int count = mDslv.getCount();

        // We're only interested if the touch was on an
        // item that's not a header or footer.
        if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders
                && touchPos < (count - numFooters)) {

            Log.d("mobeta", "touch down on position " + String.valueOf(touchPos - mDslv.getFirstVisiblePosition()));
            final View item = mDslv.getChildAt(touchPos - mDslv.getFirstVisiblePosition());
            final int rawX = (int) ev.getRawX();
            final int rawY = (int) ev.getRawY();

            View dragBox = id == 0 ? item : (View) item.findViewById(id);
            if (dragBox != null) {
                dragBox.getLocationOnScreen(mTempLoc);

                if (rawX > mTempLoc[0] && rawY > mTempLoc[1] &&
                        rawX < mTempLoc[0] + dragBox.getWidth() &&
                        rawY < mTempLoc[1] + dragBox.getHeight()) {

                    mItemX = item.getLeft();
                    mItemY = item.getTop();

                    return touchPos;
                }
            }
        }

        return MISS;
    }

    @Override
    public boolean onDown(MotionEvent ev) {
        Log.d(TAG, "onDown called");
    	if (mSwipeEnabled && mSwipeMode == CLICK_REMOVE) {
            mClickSwipeHitPos = viewIdHitPosition(ev, mClickSwipeId);
        }

        mHitPos = startDragPosition(ev);
        if (mHitPos != MISS && mDragInitMode == ON_DOWN) {
            startDrag(mHitPos, (int) ev.getX() - mItemX, (int) ev.getY() - mItemY);
        }

        mIsRemoving = false;
        mCanDrag = true;
        mPositionX = 0;
        mFlingHitPos = startFlingPosition(ev);

        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        final int x1 = (int) e1.getX();
        final int y1 = (int) e1.getY();
        final int x2 = (int) e2.getX();
        final int y2 = (int) e2.getY();
        final int deltaX = x2 - mItemX;
        final int deltaY = y2 - mItemY;

        if (mCanDrag && !mDragging && (mHitPos != MISS || mFlingHitPos != MISS)) {
            if (mHitPos != MISS) {
                if (mDragInitMode == ON_DRAG && Math.abs(y2 - y1) > mTouchSlop && mSortEnabled) {
                    startDrag(mHitPos, deltaX, deltaY);
                }
                else if (mDragInitMode != ON_DOWN && Math.abs(x2 - x1) > mTouchSlop && mSwipeEnabled)
                {
                    mIsRemoving = true;
                    startDrag(mFlingHitPos, deltaX, deltaY);
                }
            } else if (mFlingHitPos != MISS){
            		if (Math.abs(x2 - x1) > mTouchSlop && mSwipeEnabled
            		//This has been added to the conditional
            		//to minimize accidental swipes when trying to scroll
            		&& Math.abs(x2 - x1) > 2 * Math.abs(y2 - y1)) {
            		mIsRemoving = true;
            		startDrag(mFlingHitPos, deltaX, deltaY);
            		} else if (Math.abs(y2 - y1) > mTouchSlop) {
            		mCanDrag = false; // if started to scroll the list then
            		// don't allow sorting nor fling-removing
            		}
            		}
        }
        // return whatever
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Log.d("mobeta", "lift listener long pressed");
        if (mHitPos != MISS && mDragInitMode == ON_LONG_PRESS) {
            mDslv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            startDrag(mHitPos, mCurrX - mItemX, mCurrY - mItemY);
        }
    }

    // complete the OnGestureListener interface
    @Override
    public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    // complete the OnGestureListener interface
    @Override
    public boolean onSingleTapUp(MotionEvent ev) {
        if (mSwipeEnabled && mSwipeMode == CLICK_REMOVE) {
            if (mClickSwipeHitPos != MISS) {
                mDslv.swipeItem(mClickSwipeHitPos - mDslv.getHeaderViewsCount());
            }
        }
        return true;
    }

    // complete the OnGestureListener interface
    @Override
    public void onShowPress(MotionEvent ev) {
        // do nothing
    }

    private GestureDetector.OnGestureListener mFlingSwipeListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                        float velocityY) {
                    Log.d("mobeta", "on fling swipe called");
                    if (mSwipeEnabled && mIsRemoving) {
                        int w = mDslv.getWidth();
                        int minPos = w / 5;
                        if (velocityX > mFlingSpeed) {
                            if (mPositionX > -minPos) {
                                mDslv.stopDragWithVelocity(true, velocityX, mBackId);
                            }
                        } else if (velocityX < -mFlingSpeed) {
                            if (mPositionX < minPos) {
                                mDslv.stopDragWithVelocity(true, velocityX, mBackId);
                            }
                        }
                        mIsRemoving = false;
                    }
                    return false;
                }
            };

}

package com.stepfitllc.stepfit.custom_views;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.client.annotations.NotNull;

/**
 * Created by fame on 15.07.16.
 */
public class TwoSmallOneBigGridLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = TwoSmallOneBigGridLayoutManager.class.getName();

    /**
     *         GRID
     *      ––––––––––
     *      |     | 1 |
     *      |  0  |–––|
     *      |     | 2 |
     *      |–––––––––|
     *      | 3 |     |
     *      |–––|  5  |
     *      | 4 |     |
     *      ––––––––––
     *
    */

    public static int
            GRID_BLOCK_SIZE = 6,
            GRID_HALF_BLOCK_SIZE = GRID_BLOCK_SIZE / 2,

            GRID_BIG_UPPER = 0,
            GRID_BIG_LOWER = 5,

            GRID_SMALL_RIGHT_UPPER = 1,
            GRID_SMALL_RIGHT_LOWER = 2,
            GRID_SMALL_LEFT_UPPER = 3,
            GRID_SMALL_LEFT_LOWER = 4;

    private int   mWidth, mHeight;
    private int mSmallWidth, mSmallHeight, mBigWidth, mBigHeight;
    private int mBigWidthSpec, mBigHeightSpec, mSmallWidthSpec, mSmallHeightSpec;
    private int[] mLefts, mRights, mTops, mHeights;

    private boolean mScrollEnabled = true;
    private double mAspect= 1.0;
    private SparseArray<View> mViewCache = new SparseArray<>();

    public TwoSmallOneBigGridLayoutManager() {

    }

    public TwoSmallOneBigGridLayoutManager(double rectAspect) {
        mAspect = Math.abs(rectAspect);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        fill(recycler);
    }

    @Override
    public void onScrollStateChanged(int state) {
        //super.onScrollStateChanged(state);
        Log.d("__SCROLL6", "state changed:" + state);
    }

    @Override
    public boolean canScrollVertically() {
        return mScrollEnabled;
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        Log.d("__SCROLL6", "items added");
        mScrollEnabled = false;
        super.onItemsAdded(recyclerView, positionStart, itemCount);
        mScrollEnabled = true;
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        Log.d("__SCROLL6", "items removed");
        mScrollEnabled = false;
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
        mScrollEnabled = true;
    }


    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
    }

    @Override
    public boolean onRequestChildFocus(RecyclerView parent, RecyclerView.State state, View child, View focused) {
        Log.d("__FOCUS", "f1:" + getPosition(child) + " f2:" + getPosition(focused));
        return super.onRequestChildFocus(parent, state, child, focused);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int dt = scrollBy(dy);
        offsetChildrenVertical(-dt);
        fill(recycler);
        return dt;
    }

    public void updateViewDimens() {
        mWidth  = getWidth();
        mHeight = getHeight();

        mBigWidth = (int) (mAspect * mWidth);
        mBigHeight = mBigWidth;
        mSmallWidth = mWidth - mBigWidth;
        mSmallHeight = mBigHeight / 2;
        mBigWidthSpec = View.MeasureSpec.makeMeasureSpec(mBigWidth, View.MeasureSpec.EXACTLY);
        mBigHeightSpec = View.MeasureSpec.makeMeasureSpec(mBigHeight, View.MeasureSpec.EXACTLY);
        mSmallWidthSpec = View.MeasureSpec.makeMeasureSpec(mSmallWidth, View.MeasureSpec.EXACTLY);
        mSmallHeightSpec = View.MeasureSpec.makeMeasureSpec(mSmallHeight, View.MeasureSpec.EXACTLY);

        mTops    = getTops(0, mSmallHeight);
        mLefts   = getLefts(mBigWidth, mSmallWidth);
        mRights  = getRights(mLefts, mBigWidth, mSmallWidth);
        mHeights = getHeights(mBigHeight, mSmallHeight);
    }

    private View getMatchingView() {
        View matchingView = null;
        int childCount = getChildCount();

        for (int i = 0; i < childCount; ++i) {
            View view = getChildAt(i);
            int bottom = getDecoratedBottom(view);
            if (bottom > 0) {
                matchingView = view;
                break;
            }
        }
        return matchingView;
    }

    private void fill(RecyclerView.Recycler recycler) {
        View matchingChild = getMatchingView();

        mViewCache.clear();

        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View view = getChildAt(i);
            mViewCache.put(getPosition(view), view);
        }

        for (int i = 0; i < mViewCache.size(); ++i)
            detachView(mViewCache.valueAt(i));

        updateLayout(matchingChild, recycler);

        for (int i = 0; i < mViewCache.size(); ++i)
            recycler.recycleView(mViewCache.valueAt(i));

    }

    private void updateLayout(@Nullable View matchingView, RecyclerView.Recycler recycler) {
        int itemCount = getItemCount();
        if (itemCount == 0) return;

        int pos = 0;

        if (matchingView != null) {
            pos = getPosition(matchingView);
            int initialTop = getDecoratedTop(matchingView);

            int gridPos = pos % GRID_BLOCK_SIZE;
            if (gridPos == GRID_SMALL_RIGHT_LOWER || gridPos == GRID_SMALL_LEFT_LOWER)
                initialTop -= mSmallHeight;

            mTops = getTops(initialTop, mSmallHeight);
        } else {
            updateViewDimens();
        }

        Log.d("__FILL4", "layout w:" + mWidth + " layout h:" + mHeight +
                "\n initial pos:" + pos +
                "\n r w:" + mSmallWidth + " r h:" + mSmallHeight +
                "\n sq w:" + mBigWidth + " sq h:" + mBigHeight +
                "\n item count:" + itemCount + " pos:" + pos);

        int topOffset = mBigHeight * (pos / GRID_HALF_BLOCK_SIZE);

        for (int i = pos - 1; i >= 0; --i) {
            int gridPos = getGridPos(i);

            int top    = mBigHeight * (i / GRID_HALF_BLOCK_SIZE) + mTops[gridPos] - topOffset;
            int bottom = top + mHeights[gridPos];

            if (bottom < 0) {
                int prevPos = i - 1;
                if (prevPos >= 0) {
                    int prevGridPos = getGridPos(prevPos);
                    int prevTop =  mBigHeight * ( prevPos / GRID_HALF_BLOCK_SIZE) + mTops[prevGridPos] - topOffset;
                    int prevBottom = prevTop + mHeights[prevGridPos];
                    if (prevBottom > 0) continue;
                }
                break;
            }

            View view = mViewCache.get(i);
            if (view != null) {
                attachView(view);
                mViewCache.remove(i);
            } else {
                int left  = mLefts[gridPos];
                int right = mRights[gridPos];

                view = recycler.getViewForPosition(i);

                if (right - left == mSmallWidth)
                    view.measure(mSmallWidthSpec, mSmallHeightSpec);
                else
                    view.measure(mBigWidthSpec, mBigHeightSpec);

                addView(view);
                layoutDecorated(view, left, top, right, bottom);
            }
        }

        for (int i = pos; i < itemCount; ++i) {
            int gridPos = getGridPos(i);

            int top    = mBigHeight * (i / GRID_HALF_BLOCK_SIZE) + mTops[gridPos] - topOffset;
            int bottom = top + mHeights[gridPos];

            Log.d("__FILL2",  " i:" + i + " grid pos:" + gridPos +
                    "\n  t:" + top + " b:" + bottom +
                    "\n d:" + (top - mHeight));

            if (top > mHeight) {
                int nextPos = i + 1;
                if (nextPos < itemCount) {
                    int nextTop =  mBigHeight * ( nextPos / GRID_HALF_BLOCK_SIZE) + mTops[getGridPos(nextPos)] - topOffset;
                    if (nextTop < mHeight) continue;
                }
                //if (gridPos == GRID_SMALL_LEFT_LOWER) continue;
                break;
            }

            if (bottom < 0) continue;

            View view = mViewCache.get(i);
            if (view != null) {
                attachView(view);
                mViewCache.remove(i);
                Log.d("__FILL2", " i:" + i +
                        "\n t:"+ getDecoratedTop(view) + " b:" + getDecoratedBottom(view) + " l:" + getDecoratedLeft(view) + " r:" + getDecoratedRight(view));
            } else {
                int left  = mLefts[gridPos];
                int right = mRights[gridPos];

                view = recycler.getViewForPosition(i);

                if (right - left == mSmallWidth)
                    view.measure(mSmallWidthSpec, mSmallHeightSpec);
                else
                    view.measure(mBigWidthSpec, mBigHeightSpec);

                addView(view);
                layoutDecorated(view, left, top, right, bottom);
            }
        }

    }

    private int scrollBy(int dy) {
        int childCount = getChildCount();
        if (childCount == 0 || dy == 0)
            return 0;

        View firstView = getChildAt(0);
        View lastView  = getChildAt(childCount - 1);

        int top    = getDecoratedTop(firstView);
        int bottom = getDecoratedBottom(lastView);

        if (childCount > 1) {
            View secondView = getChildAt(1);
            View beforeLastView = getChildAt(childCount - 2);

            top = Math.min(top, getDecoratedTop(secondView));
            bottom = Math.max(bottom, getDecoratedBottom(beforeLastView));
        }

        int dt           = 0;
        int itemCount    = getItemCount();
        int lastViewPos  = getPosition(lastView);
        int firstViewPos = getPosition(firstView);

        Log.d("__SCROLL", "top:" + top + " bottom:" + bottom  + " height:" + mHeight +
                " dist:" + (bottom - top) +  " itemCount:" + itemCount + " dy:" + dy + " topPos:" + getPosition(firstView) + " botPos:" + getPosition(lastView));

        if (lastViewPos - firstViewPos == itemCount - 1 && bottom - top < mHeight)
            return 0;

        if (dy < 0) {
            if (top < 0)
                dt = dy;
            else if (firstViewPos == 0)
                dt = Math.max(top, dy);

        } else if (dy > 0) {
            if (bottom > mHeight)
                dt = dy;
            else if (lastViewPos == itemCount - 1)
                dt = Math.min(bottom - mHeight, dy);
        }
        Log.d("__SCROLL", " dt:" + dt);
        return dt;
    }

    public int getGridPos(int pos) {
        return pos % GRID_BLOCK_SIZE;
    }

    public boolean gridPosAtBigItem(int gridPos) {
        return gridPos == GRID_BIG_LOWER || gridPos == GRID_BIG_UPPER;
    }

    private static int[] getLefts(int bigWidth, int smallWidth) {
        return new int[] {
                0,
                bigWidth,
                bigWidth,
                0,
                0,
                smallWidth
        };
    }

    private static int[] getTops(int initialTop, int smallHeight) {
        return new int[] {
                initialTop,
                initialTop,
                initialTop + smallHeight,
                initialTop,
                initialTop + smallHeight,
                initialTop
        };
    }

    private static int[] getRights(@NotNull int[] lefts, int bigWidth, int smallWidth) {
        return new int[] {
                lefts[0] + bigWidth,
                lefts[1] + smallWidth,
                lefts[2] + smallWidth,
                lefts[3] + smallWidth,
                lefts[4] + smallWidth,
                lefts[5] + bigWidth,
        };
    }

    private static int[] getHeights(int bigHeight, int smallHeight) {
        return new int[] {
                bigHeight,
                smallHeight,
                smallHeight,
                smallHeight,
                smallHeight,
                bigHeight
        };
    }
}

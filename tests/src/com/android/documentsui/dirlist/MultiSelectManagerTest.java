/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.documentsui.dirlist;

import android.support.v7.widget.RecyclerView;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.SparseBooleanArray;

import com.android.documentsui.TestInputEvent;
import com.android.documentsui.dirlist.MultiSelectManager.Selection;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SmallTest
public class MultiSelectManagerTest extends AndroidTestCase {

    private static final List<String> items;
    static {
        items = new ArrayList<String>();
        for (int i = 0; i < 100; ++i) {
            items.add(Integer.toString(i));
        }
    }

    private MultiSelectManager mManager;
    private TestCallback mCallback;
    private TestSelectionEnvironment mEnv;

    public void setUp() throws Exception {
        mCallback = new TestCallback();
        mEnv = new TestSelectionEnvironment(items);
        mManager = new MultiSelectManager(mEnv, MultiSelectManager.MODE_MULTIPLE);
        mManager.addCallback(mCallback);
    }

    public void testMouseClick_StartsSelectionMode() {
        click(7);
        assertSelection(items.get(7));
    }

    public void testMouseClick_NotifiesSelectionChanged() {
        click(7);
        mCallback.assertSelectionChanged();
    }

    public void testMouseClick_ShiftClickExtendsSelection() {
        longPress(7);
        shiftClick(11);
        assertRangeSelection(7, 11);
    }

    public void testMouseClick_NoPosition_ClearsSelection() {
        longPress(7);
        click(11);
        click(RecyclerView.NO_POSITION);
        assertSelection();
    }

    public void testSetSelectionFocusBegin() {
        mManager.setItemsSelected(Lists.newArrayList(items.get(7)), true);
        mManager.setSelectionRangeBegin(7);
        shiftClick(11);
        assertRangeSelection(7, 11);
    }

    public void testLongPress_StartsSelectionMode() {
        longPress(7);
        assertSelection(items.get(7));
    }

    public void testLongPress_SecondPressExtendsSelection() {
        longPress(7);
        longPress(99);
        assertSelection(items.get(7), items.get(99));
    }

    public void testSingleTapUp_UnselectsSelectedItem() {
        longPress(7);
        tap(7);
        assertSelection();
    }

    public void testSingleTapUp_NoPosition_ClearsSelection() {
        longPress(7);
        tap(11);
        tap(RecyclerView.NO_POSITION);
        assertSelection();
    }

    public void testSingleTapUp_ExtendsSelection() {
        longPress(99);
        tap(7);
        tap(13);
        assertSelection(items.get(7), items.get(99), items.get(13));
    }

    public void testSingleTapUp_ShiftCreatesRangeSelection() {
        longPress(7);
        shiftTap(17);
        assertRangeSelection(7, 17);
    }

    public void testSingleTapUp_ShiftCreatesRangeSeletion_Backwards() {
        longPress(17);
        shiftTap(7);
        assertRangeSelection(7, 17);
    }

    public void testSingleTapUp_SecondShiftClickExtendsSelection() {
        longPress(7);
        shiftTap(11);
        shiftTap(17);
        assertRangeSelection(7, 17);
    }

    public void testSingleTapUp_MultipleContiguousRangesSelected() {
        longPress(7);
        shiftTap(11);
        tap(20);
        shiftTap(25);
        assertRangeSelected(7, 11);
        assertRangeSelected(20, 25);
        assertSelectionSize(11);
    }

    public void testSingleTapUp_ShiftReducesSelectionRange_FromPreviousShiftClick() {
        longPress(7);
        shiftTap(17);
        shiftTap(10);
        assertRangeSelection(7, 10);
    }

    public void testSingleTapUp_ShiftReducesSelectionRange_FromPreviousShiftClick_Backwards() {
        mManager.onLongPress(TestInputEvent.tap(17));
        shiftTap(7);
        shiftTap(14);
        assertRangeSelection(14, 17);
    }

    public void testSingleTapUp_ShiftReversesSelectionDirection() {
        longPress(7);
        shiftTap(17);
        shiftTap(0);
        assertRangeSelection(0, 7);
    }

    public void testSingleSelectMode() {
        mManager = new MultiSelectManager(mEnv, MultiSelectManager.MODE_SINGLE);
        mManager.addCallback(mCallback);
        longPress(20);
        tap(13);
        assertSelection(items.get(13));
    }

    public void testSingleSelectMode_ShiftTap() {
        mManager = new MultiSelectManager(mEnv, MultiSelectManager.MODE_SINGLE);
        mManager.addCallback(mCallback);
        longPress(13);
        shiftTap(20);
        assertSelection(items.get(20));
    }

    public void testSingleSelectMode_ShiftDoesNotExtendSelection() {
        mManager = new MultiSelectManager(mEnv, MultiSelectManager.MODE_SINGLE);
        mManager.addCallback(mCallback);
        longPress(20);
        keyToPosition(22, true);
        assertSelection(items.get(22));
    }

    public void testProvisionalSelection() {
        Selection s = mManager.getSelection();
        assertSelection();

        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(1, true);
        provisional.append(2, true);
        s.setProvisionalSelection(getItemIds(provisional));
        assertSelection(items.get(1), items.get(2));

        provisional.delete(1);
        provisional.append(3, true);
        s.setProvisionalSelection(getItemIds(provisional));
        assertSelection(items.get(2), items.get(3));

        s.applyProvisionalSelection();
        assertSelection(items.get(2), items.get(3));

        provisional.clear();
        provisional.append(3, true);
        provisional.append(4, true);
        s.setProvisionalSelection(getItemIds(provisional));
        assertSelection(items.get(2), items.get(3), items.get(4));

        provisional.delete(3);
        s.setProvisionalSelection(getItemIds(provisional));
        assertSelection(items.get(2), items.get(3), items.get(4));
    }

    private static Set<String> getItemIds(SparseBooleanArray selection) {
        Set<String> ids = new HashSet<>();

        int count = selection.size();
        for (int i = 0; i < count; ++i) {
            ids.add(items.get(selection.keyAt(i)));
        }

        return ids;
    }

    private void longPress(int position) {
        mManager.onLongPress(TestInputEvent.tap(position));
    }

    private void tap(int position) {
        mManager.onSingleTapUp(TestInputEvent.tap(position));
    }

    private void shiftTap(int position) {
        mManager.onSingleTapUp(TestInputEvent.shiftTap(position));
    }

    private void click(int position) {
        mManager.onSingleTapUp(TestInputEvent.click(position));
    }

    private void shiftClick(int position) {
        mManager.onSingleTapUp(TestInputEvent.shiftClick(position));
    }

    private void keyToPosition(int position, boolean shift) {
        mManager.attemptChangeFocus(position, shift);
    }

    private void assertSelected(String... expected) {
        for (int i = 0; i < expected.length; i++) {
            Selection selection = mManager.getSelection();
            String err = String.format(
                    "Selection %s does not contain %s", selection, expected[i]);
            assertTrue(err, selection.contains(expected[i]));
        }
    }

    private void assertSelection(String... expected) {
        assertSelectionSize(expected.length);
        assertSelected(expected);
    }

    private void assertRangeSelected(int begin, int end) {
        for (int i = begin; i <= end; i++) {
            assertSelected(items.get(i));
        }
    }

    private void assertRangeSelection(int begin, int end) {
        assertSelectionSize(end - begin + 1);
        assertRangeSelected(begin, end);
    }

    private void assertSelectionSize(int expected) {
        Selection selection = mManager.getSelection();
        assertEquals(selection.toString(), expected, selection.size());
    }

    private static final class TestCallback implements MultiSelectManager.Callback {

        Set<String> ignored = new HashSet<>();
        private boolean mSelectionChanged = false;

        @Override
        public void onItemStateChanged(String modelId, boolean selected) {}

        @Override
        public boolean onBeforeItemStateChange(String modelId, boolean selected) {
            return !ignored.contains(modelId);
        }

        @Override
        public void onSelectionChanged() {
            mSelectionChanged = true;
        }

        void assertSelectionChanged() {
            assertTrue(mSelectionChanged);
        }
    }
}

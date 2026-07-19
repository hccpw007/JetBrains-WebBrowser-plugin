package com.cpw.browser.ui;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;

// BookmarkBar 溢出计算逻辑单元测试
public class BookmarkBarTest {

    // 所有书签都能放下时返回全部数量
    @Test
    public void allFitWhenWidthSufficient() {
        List<Integer> widths = Arrays.asList(100, 100, 100);
        assertEquals(3, BookmarkBar.computeVisibleCount(400, widths));
    }

    // 宽度不足时返回可放下的数量
    @Test
    public void stopsAtCapacity() {
        List<Integer> widths = Arrays.asList(100, 100, 100);
        assertEquals(2, BookmarkBar.computeVisibleCount(250, widths));
    }

    // 可用宽度为 0 时返回 0
    @Test
    public void emptyWhenNoWidth() {
        List<Integer> widths = Arrays.asList(100, 100);
        assertEquals(0, BookmarkBar.computeVisibleCount(0, widths));
    }

    // 空列表返回 0
    @Test
    public void emptyListReturnsZero() {
        assertEquals(0, BookmarkBar.computeVisibleCount(400, Collections.emptyList()));
    }
}

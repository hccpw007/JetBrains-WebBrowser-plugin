// 历史记录列表数据模型，按日期分组：今天、本周各天、以前
package com.cpw.browser.ui;

import com.cpw.browser.history.BrowsingHistoryState;
import com.cpw.browser.history.HistoryEntry;
import com.cpw.browser.util.TranslationUtil;

import javax.swing.AbstractListModel;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryEntriesModel extends AbstractListModel<Object> {

    // 历史分组：今天的条目标题
    private static final String HEADER_TODAY = "history.today";
    // 历史分组：以前的条目标题
    private static final String HEADER_OLDER = "history.older";

    // 历史列表数据项（包含分组标题和历史条目）
    private List<Object> items = buildItems();

    // 刷新历史列表数据
    public void refresh() {
        items = buildItems();
        fireContentsChanged(this, 0, Math.max(items.size(), 0));
    }

    // 获取列表项数量
    @Override
    public int getSize() {
        return items.size();
    }

    // 获取指定索引的列表项
    @Override
    public Object getElementAt(int index) {
        return items.get(index);
    }

    // 构建历史列表条目（按日期分组）
    private List<Object> buildItems() {
        List<HistoryEntry> entries = BrowsingHistoryState.getInstance().getEntries();
        // 无历史记录则返回空列表
        if (entries.isEmpty()) return List.of();

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        long todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long mondayStart = monday.atStartOfDay(zone).toInstant().toEpochMilli();

        List<HistoryEntry> todayGroup = new ArrayList<>();
        Map<DayOfWeek, List<HistoryEntry>> weekGroups = new HashMap<>();
        List<HistoryEntry> earlierGroup = new ArrayList<>();

        // 按日期分组归类历史记录
        for (HistoryEntry e : entries) {
            if (e.getTimestamp() >= todayStart) {
                todayGroup.add(e);
            } else if (e.getTimestamp() >= mondayStart) {
                DayOfWeek dow = Instant.ofEpochMilli(e.getTimestamp()).atZone(zone).getDayOfWeek();
                weekGroups.computeIfAbsent(dow, k -> new ArrayList<>()).add(e);
            } else {
                earlierGroup.add(e);
            }
        }

        List<Object> result = new ArrayList<>();
        if (!todayGroup.isEmpty()) {
            result.add(TranslationUtil.getText(HEADER_TODAY));
            result.addAll(todayGroup);
        }
        for (DayOfWeek dow : Arrays.asList(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )) {
            List<HistoryEntry> group = weekGroups.get(dow);
            if (group != null && !group.isEmpty()) {
                result.add(dow);
                result.addAll(group);
            }
        }
        if (!earlierGroup.isEmpty()) {
            result.add(TranslationUtil.getText(HEADER_OLDER));
            result.addAll(earlierGroup);
        }
        return result;
    }
}

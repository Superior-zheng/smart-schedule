package com.zhangwenzheng.smartschedule.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoryEventService {

    private record HistoryEvent(String md, int year, String title, String summary) {}

    private static final List<HistoryEvent> EVENTS = List.of(
            new HistoryEvent("01-01", 1949, "《人民日报》创刊", "新中国重要媒体正式创刊。"),
            new HistoryEvent("01-11", 1995, "WTO成立", "世界贸易组织正式成立，推动全球经贸协作。"),
            new HistoryEvent("01-27", 1945, "奥斯维辛集中营获解放", "国际社会铭记历史，反对战争与种族灭绝。"),
            new HistoryEvent("02-04", 2004, "Facebook上线", "社交网络时代的重要节点。"),
            new HistoryEvent("02-11", 1990, "曼德拉出狱", "南非反种族隔离进程迈出关键一步。"),
            new HistoryEvent("03-10", 1876, "电话首次成功通话", "贝尔完成电话实验，通信方式被改写。"),
            new HistoryEvent("03-14", 1879, "爱因斯坦诞辰", "现代物理学重要奠基者诞生。"),
            new HistoryEvent("04-22", 1970, "首个世界地球日", "全球环保行动的重要起点。"),
            new HistoryEvent("04-23", 1995, "世界读书日设立", "联合国教科文组织倡导全民阅读。"),
            new HistoryEvent("05-08", 1945, "欧洲反法西斯战争胜利", "二战欧洲战场迎来胜利时刻。"),
            new HistoryEvent("05-12", 2008, "汶川抗震救灾全面展开", "全国上下同心协力，守望相助。"),
            new HistoryEvent("06-05", 1972, "世界环境日确立", "全球环保议题被持续推进。"),
            new HistoryEvent("06-26", 1945, "《联合国宪章》签署", "现代国际秩序的重要基石。"),
            new HistoryEvent("07-20", 1969, "人类首次登月", "阿波罗11号实现“迈向月球的一大步”。"),
            new HistoryEvent("08-08", 2008, "北京奥运会开幕", "“同一个世界，同一个梦想”感动世界。"),
            new HistoryEvent("08-26", 1920, "美国女性获得联邦选举权", "女性平权进程中的里程碑。"),
            new HistoryEvent("09-03", 2015, "中国人民抗日战争胜利70周年纪念", "铭记历史、珍爱和平。"),
            new HistoryEvent("09-15", 2008, "大型强子对撞机启动", "人类探索微观世界迈上新台阶。"),
            new HistoryEvent("10-01", 1949, "中华人民共和国成立", "新中国成立的重要历史时刻。"),
            new HistoryEvent("10-24", 1945, "联合国正式成立", "国际合作与和平发展的象征。"),
            new HistoryEvent("11-09", 1989, "柏林墙倒塌", "欧洲冷战格局迎来历史性转折。"),
            new HistoryEvent("11-20", 1989, "《儿童权利公约》通过", "全球儿童权益保护迈入新阶段。"),
            new HistoryEvent("12-10", 1948, "《世界人权宣言》通过", "现代人权理念的重要文献。"),
            new HistoryEvent("12-25", 1991, "苏联解体", "世界格局发生重大变化。")
    );

    private final Map<String, HistoryEvent> byMonthDay;

    public HistoryEventService() {
        Map<String, HistoryEvent> map = new HashMap<>();
        for (HistoryEvent event : EVENTS) {
            map.put(event.md, event);
        }
        this.byMonthDay = map;
    }

    public Map<String, Object> getEventOfDay(LocalDate date) {
        String md = date.format(DateTimeFormatter.ofPattern("MM-dd"));
        HistoryEvent event = byMonthDay.get(md);

        // 若当天没有精确匹配，按“年内第几天”做稳定轮换：同一天全员一致，第二天自动变化
        if (event == null) {
            int idx = (date.getDayOfYear() - 1) % EVENTS.size();
            event = EVENTS.get(idx);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("date", date.toString());
        res.put("monthDay", md);
        res.put("year", event.year());
        res.put("title", event.title());
        res.put("summary", event.summary());
        return res;
    }
}

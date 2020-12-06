package query.timeseries.impl;

import model.avro.EventInfo;
import query.timeseries.TimeSeriesDB;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class InMemoryTimeSeries implements TimeSeriesDB {

    private final Map<Class, Supplier<Function<Object, EventInfo>>> eventBuilder = new ConcurrentHashMap<>();

    private final ClassValue<Function<Object, EventInfo>> classValue = new ClassValue<Function<Object, EventInfo>>() {
        @Override
        protected Function<Object, EventInfo> computeValue(Class type) {
            return eventBuilder.get(type).get();
        }
    };

    private final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private ConcurrentSkipListMap<String, EventInfo> events = new ConcurrentSkipListMap<>();

    @Override
    public <T> void register(Class<T> cls, Supplier<Function<Object, EventInfo>> fn) {
        eventBuilder.put(cls, fn);
    }

    @Override
    public <T> EventInfo insert(T row) {
        Function<Object, EventInfo> fn = classValue.get(row.getClass());
        EventInfo event = fn.apply(row);
        events.put(event.getEventTime().toString(), event);
        return event;
    }

    @Override
    public void gt(LocalDateTime now, Function<EventInfo, Boolean> fn) {
        ConcurrentNavigableMap<String, EventInfo> matched = events.tailMap(now.format(f));
        for (Map.Entry<String, EventInfo> e : matched.entrySet()) {
            if (!fn.apply(e.getValue())) {
                break;
            }
        }
    }
}

package query.kv.memory;

import query.kv.SSTable;
import query.kv.TableInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemorySSTable<Row_Type> implements SSTable<Row_Type> {

    private final Map<String, Row_Type> rawRows = new ConcurrentHashMap<>();
    private final NavigableMap<String, Row_Type> indexRows = new ConcurrentSkipListMap<>();
    private final TableInfo<Row_Type> tableInfo;

    public InMemorySSTable(TableInfo<Row_Type> tableInfo) {
        this.tableInfo = tableInfo;
    }

    @Override
    public Map<String, Function<Row_Type, Object>> schema() {
        return tableInfo.getSchema();
    }

    @Override
    public Map<String, Function<Row_Type, String>> indexes() {
        return tableInfo.getIndexes();
    }

    @Override
    public List<String> cols() {
        return tableInfo
                .getSchema()
                .entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public void scan(Consumer<Row_Type> consumer, int limit) {
        rawRows.entrySet().stream()
                .limit(limit)
                .map(Map.Entry::getValue)
                .forEach(consumer::accept);

    }

    @Override
    public void search(String indexName, String searchValue, Consumer<Row_Type> consumer, int limit) {
        String indexKey = buildIndexKey(indexName, searchValue);
        Stream<Row_Type> rows = rows(indexKey, limit);
        rows.forEach(consumer::accept);
    }

    @Override
    public void search(String indexName, String searchValue, Collection<Row_Type> container, int limit) {
        search(indexName, searchValue, container::add, limit);
    }

    private Stream<Row_Type> rows(String indexKey, int limit) {
        Stream<Map.Entry<String, Row_Type>> filterRows = indexRows
                .tailMap(indexKey).entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(indexKey));

        Stream<Row_Type> rows = filterRows
                .limit(limit)
                .map(Map.Entry::getValue);

        return rows;
    }

    private String buildIndexKey(String indexName, String matchValue) {
        return String.format("%s/%s/%s", tableInfo.getTableName(), indexName, matchValue);
    }

    @Override
    public void insert(Row_Type row) {
        addRecord(row);
    }

    private void addRecord(Row_Type row) {
        String key = tableInfo.getPk().apply(row);
        rawRows.put(key, row);
        buildIndex(row, key);
    }

    @Override
    public void rangeSearch(String index, String start, String end, Collection<Row_Type> container, int limit) {
        String startKey = buildIndexKey(index, start);
        String endKey = buildIndexKey(index, end);
        Stream<Row_Type> rows = rows(startKey, endKey, limit);
        rows.forEach(container::add);

    }

    @Override
    public Row_Type get(String pk) {
        return rawRows.get(pk);
    }

    @Override
    public void update(Row_Type record) {
        addRecord(record);
    }

    private Stream<Row_Type> rows(String startKey, String endKey, int limit) {
        Stream<Map.Entry<String, Row_Type>> filterRows = indexRows
                .subMap(startKey, true, endKey, true)
                .entrySet()
                .stream();

        Stream<Row_Type> rows = filterRows
                .limit(limit)
                .map(Map.Entry::getValue);

        return rows;
    }

    private void buildIndex(Row_Type row, String key) {
        for (Map.Entry<String, Function<Row_Type, String>> index : tableInfo.getIndexes().entrySet()) {
            String indexValue = index.getValue().apply(row);
            String indexName = index.getKey();
            String indexKey = String.format("%s/%s/%s/%s", tableInfo.getTableName(), indexName, indexValue, key);
            indexRows.put(indexKey, row);
        }
    }

    @Override
    public Object columnValue(String col, Object row) {
        return tableInfo
                .getSchema()
                .get(col.toLowerCase())
                .apply((Row_Type) row);
    }


    @Override
    public String toString() {
        return String.format("Table[%s]", tableInfo.getTableName());
    }
}

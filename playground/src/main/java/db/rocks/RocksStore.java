package db.rocks;

import com.google.gson.Gson;
import db.KeyValueStore;
import db.SSTable;
import org.rocksdb.RocksDB;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyMap;

public class RocksStore implements KeyValueStore {
    private final Map<String, SSTable<?>> tables = new HashMap<>();
    private final RocksDB rocksDB;

    public RocksStore(File rootFolder) {
        this.rocksDB = RocksDBDriver.openDatabase(rootFolder);
    }

    @Override
    public <Row_Type> SSTable<Row_Type> createTable(String tableName, Class<Row_Type> type, Map<String, Function<Row_Type, Object>> schema, Map<String, Function<Row_Type, String>> indexes) {
        Function<Row_Type, byte[]> toJson = row -> new Gson().toJson(row).getBytes();
        Function<byte[], Row_Type> toRecord = rawBytes -> new Gson().fromJson(new String(rawBytes), type);

        return createTable(tableName, schema, indexes, toJson, toRecord);
    }

    private <Row_Type> void registerTable(String tableName, SSTable<Row_Type> SSTable) {
        tables.put(tableName, SSTable);
    }

    @Override
    public <Row_Type> SSTable<Row_Type> createTable(String tableName, Class<Row_Type> type, Map<String, Function<Row_Type, Object>> schema) {
        return createTable(tableName, type, schema, emptyMap());
    }

    @Override
    public <Row_Type> SSTable<Row_Type> createTable(String tableName,
                                                    Map<String, Function<Row_Type, Object>> schema,
                                                    Map<String, Function<Row_Type, String>> indexes,
                                                    Function<Row_Type, byte[]> encoder, Function<byte[], Row_Type> decoder) {
        SSTable<Row_Type> SSTable = new RocksSSTable<>(rocksDB, tableName, indexes, schema, encoder, decoder);
        registerTable(tableName, SSTable);
        return SSTable;
    }

    @Override
    public List<String> desc(String tableName) {
        SSTable<?> SSTable = tables.get(tableName);
        return SSTable.cols();
    }

    public void close() {
        this.rocksDB.close();
    }
}

package io.liveoak.pgsql.meta;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class Catalog {

    private final Set<String> schemas;
    private final String defaultSchema;
    private Map<TableRef, Table> tables;

    public Catalog(Set<String> schemas, String defaultSchema, Map<TableRef, Table> tables) {
        this.schemas = Collections.unmodifiableSet(schemas);
        this.defaultSchema = defaultSchema;

        Map<TableRef, Table> tablesWithIds = new LinkedHashMap<>();
        Map<TableRef, List<ForeignKey>> referredKeys = new LinkedHashMap<>();

        // group by name (same name in multiple schemas)
        LinkedHashMap<String, List<TableRef>> seenNames = new LinkedHashMap<>();
        for (TableRef ref: tables.keySet()) {
            List<TableRef> fullNames = seenNames.get(ref.name());
            if (fullNames == null) {
                fullNames = new LinkedList<>();
                seenNames.put(ref.name(), fullNames);
            }
            fullNames.add(ref);

            // compile referred keys
            List<ForeignKey> reffk = tables.get(ref).foreignKeys();
            for (ForeignKey fk: reffk) {
                List<ForeignKey> reffks = referredKeys.get(fk.tableRef());
                if (reffks == null) {
                    reffks = new LinkedList<>();
                    referredKeys.put(fk.tableRef(), reffks);
                }
                reffks.add(fk);
            }
        }

        Collection<Table> unordered = new LinkedList<>();
        // set ids as either short (name) or long (schema.name)
        for (Map.Entry<String, List<TableRef>> e: seenNames.entrySet()) {
            if (e.getValue().size() > 1) {
                for (TableRef ref: e.getValue()) {
                    unordered.add(new Table(ref.asUnquotedIdentifier(), tables.get(ref), referredKeys.get(ref)));
                }
            } else {
                TableRef ref = e.getValue().get(0);
                unordered.add(new Table(e.getKey(), tables.get(ref), referredKeys.get(ref)));
            }
        }

        // order by table.id() - exposed table identifier which may or may not contain schema component
        Collection<Table> ordered = new TreeSet<>((o1, o2) -> {
            return o1.id().compareTo(o2.id());
        });
        ordered.addAll(unordered);

        for (Table t: ordered) {
            tablesWithIds.put(t.tableRef(), t);
        }
        this.tables = Collections.unmodifiableMap(tablesWithIds);
    }

    public Table table(TableRef tableRef) {
        if (tableRef.schema() != null) {
            return tables.get(tableRef);
        } else {
            for (Table t: tables.values()) {
                if (t.name().equals(tableRef.name())) {
                    return t;
                }
            }
        }
        return null;
    }

    public List<String> tableIds() {
        List<String> ret = new LinkedList<>();
        for (Table t: tables.values()) {
            ret.add(t.id());
        }
        return ret;
    }

    public Set<String> schemas() {
        return schemas;
    }

    public String defaultSchema() {
        return defaultSchema;
    }

    public Table tableById(String id) {
        return table(new TableRef(id));
    }
}

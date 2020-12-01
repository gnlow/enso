package org.enso.table.data.index;

import org.enso.table.data.column.storage.LongStorage;
import org.enso.table.data.column.storage.Storage;
import org.enso.table.data.table.Column;

import java.util.*;
import java.util.stream.Collectors;

public class HashIndex extends Index {
  private final Storage storage;
  private final Map<Object, List<Integer>> locs;
  private final String name;
  private HashIndex uniqueIndex = null;

  private HashIndex(Storage storage, Map<Object, List<Integer>> locs, String name) {
    this.storage = storage;
    this.locs = locs;
    this.name = name;
  }

  private HashIndex(String name, Storage storage) {
    Map<Object, List<Integer>> locations = new HashMap<>();
    for (int i = 0; i < storage.size(); i++) {
      List<Integer> its =
          locations.computeIfAbsent(storage.getItemBoxed(i), x -> new ArrayList<>());
      its.add(i);
    }
    this.locs = locations;
    this.storage = storage;
    this.name = name;
  }

  public static HashIndex fromStorage(String name, Storage storage) {
    return new HashIndex(name, storage);
  }

  public Object iloc(int i) {
    return storage.getItemBoxed(i);
  }

  @Override
  public List<Integer> loc(Object item) {
    return locs.get(item);
  }

  @Override
  public String ilocString(int loc) {
    return String.valueOf(iloc(loc));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Index mask(BitSet mask, int cardinality) {
    return HashIndex.fromStorage(name, storage.mask(mask, cardinality));
  }

  @Override
  public Index countMask(int[] counts, int total) {
    return HashIndex.fromStorage(name, storage.countMask(counts, total));
  }

  @Override
  public Index orderMask(int[] mask) {
    return HashIndex.fromStorage(name, storage.orderMask(mask));
  }

  private void initUniqueIndex() {
    BitSet mask = new BitSet();
    for (List<Integer> positions : locs.values()) {
      mask.set(positions.get(0));
    }
    uniqueIndex = HashIndex.fromStorage(name, storage.mask(mask, locs.size()));
  }

  @Override
  public Column count() {
    initUniqueIndex();
    long[] result = new long[uniqueIndex.storage.size()];
    for (int i = 0; i < uniqueIndex.size(); i++) {
      result[i] = locs.get(uniqueIndex.iloc(i)).size();
    }
    Storage storage = new LongStorage(result, uniqueIndex.size(), new BitSet());
    return new Column("count", uniqueIndex, storage);
  }

  // TODO Other indexes
  @Override
  public Index concat(Index other) {
    return HashIndex.fromStorage(name, this.storage.concat(((HashIndex) other).storage));
  }

  @Override
  public Column toColumn() {
    return new Column(name, this, storage);
  }

  private int size() {
    return storage.size();
  }
}

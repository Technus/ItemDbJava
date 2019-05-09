package com.dgs.dapc.itemDB.headless.properties;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ObservableReadSortList<V> implements ObservableList<V> {
    ObservableList<V> backingList;

    public ObservableReadSortList(ObservableList<V> backingList){
        this.backingList = backingList;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        backingList.addListener(listener);
    }

    @Override
    public void addListener(ListChangeListener<? super V> listener) {
        backingList.addListener(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        backingList.removeListener(listener);
    }

    @Override
    public void removeListener(ListChangeListener<? super V> listener) {
        backingList.removeListener(listener);
    }

    @Override
    public boolean addAll(V... elements) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean setAll(V... elements) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean setAll(Collection<? extends V> col) {
        if(backingList.size()==col.size() && col.containsAll(backingList)) {
            return backingList.setAll(col);
        }
        throw new NoSuchMethodError();
    }

    @Override
    public boolean removeAll(V... elements) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean retainAll(V... elements) {
        throw new NoSuchMethodError();
    }

    @Override
    public void remove(int from, int to) {
        throw new NoSuchMethodError();
    }

    @Override
    public FilteredList<V> filtered(Predicate<V> predicate) {
        return backingList.filtered(predicate);
    }

    @Override
    public SortedList<V> sorted(Comparator<V> comparator) {
        return backingList.sorted(comparator);
    }

    @Override
    public SortedList<V> sorted() {
        return backingList.sorted();
    }

    @Override
    public int size() {
        return backingList.size();
    }

    @Override
    public boolean isEmpty() {
        return backingList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backingList.contains(o);
    }

    @Override
    public Iterator<V> iterator() {
        return backingList.iterator();
    }

    @Override
    public Object[] toArray() {
        return backingList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backingList.toArray(a);
    }

    @Override
    public boolean add(V v) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean remove(Object o) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backingList.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new NoSuchMethodError();
    }

    @Override
    public void replaceAll(UnaryOperator<V> operator) {
        throw new NoSuchMethodError();
    }

    @Override
    public void sort(Comparator<? super V> c) {
        backingList.sort(c);
    }

    @Override
    public void clear() {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean equals(Object o) {
        return backingList.equals(o);
    }

    @Override
    public int hashCode() {
        return backingList.hashCode();
    }

    @Override
    public V get(int index) {
        return backingList.get(index);
    }

    @Override
    public V set(int index, V element) {
        throw new NoSuchMethodError();
    }

    @Override
    public void add(int index, V element) {
        throw new NoSuchMethodError();
    }

    @Override
    public V remove(int index) {
        throw new NoSuchMethodError();
    }

    @Override
    public int indexOf(Object o) {
        return backingList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return backingList.lastIndexOf(o);
    }

    @Override
    public ListIterator<V> listIterator() {
        return backingList.listIterator();
    }

    @Override
    public ListIterator<V> listIterator(int index) {
        return backingList.listIterator(index);
    }

    @Override
    public List<V> subList(int fromIndex, int toIndex) {
        return backingList.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<V> spliterator() {
        return backingList.spliterator();
    }

    @Override
    public boolean removeIf(Predicate<? super V> filter) {
        throw new NoSuchMethodError();
    }

    @Override
    public Stream<V> stream() {
        return backingList.stream();
    }

    @Override
    public Stream<V> parallelStream() {
        return backingList.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super V> action) {
        backingList.forEach(action);
    }
}

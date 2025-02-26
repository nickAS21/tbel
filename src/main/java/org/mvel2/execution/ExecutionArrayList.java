package org.mvel2.execution;

import org.mvel2.ExecutionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionArrayList<E> extends ArrayList<E> implements ExecutionObject {

    private static final Comparator stringCompDesc = new Comparator() {
        public int compare(Object o1, Object o2) {
            String first = String.valueOf(o1);
            String second = String.valueOf(o2);
            return second.compareTo(first);
        }
    };
    private static final Comparator numericCompAsc = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double first = Double.parseDouble(String.valueOf(o1));
            Double second = Double.parseDouble(String.valueOf(o2));
            return first.compareTo(second);
        }
    };
    private static final Comparator numericCompDesc = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double first = Double.parseDouble(String.valueOf(o1));
            Double second = Double.parseDouble(String.valueOf(o2));
            return second.compareTo(first);
        }
    };

    private final ExecutionContext executionContext;

    private long memorySize = 0;

    public ExecutionArrayList(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public ExecutionArrayList(Collection<? extends E> c, ExecutionContext executionContext) {
        super(c);
        this.executionContext = executionContext;
        for (int i = 0; i < size(); i++) {
            E val = get(i);
            this.memorySize += this.executionContext.onValAdd(this, i, val);
        }
    }

    public boolean push(E e) {
        return this.add(e);
    }

    public E pop() {
        int size = size();
        if (size == 0) {
            return null;
        } else {
            return this.remove(size - 1);
        }
    }

    public E shift() {
        return remove(0);
    }

    public void unshift(E e) {
        add(0, e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean res = super.addAll(c);
        int i = c.size();
        for (E val : c) {
            this.memorySize += this.executionContext.onValAdd(this, i++, val);
        }
        return res;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean res = super.addAll(index, c);
        int i = index;
        for (E val : c) {
            this.memorySize += this.executionContext.onValAdd(this, i++, val);
        }
        return res;
    }

    @Override
    public void add(int index, E e) {
        super.add(index, e);
        this.memorySize += this.executionContext.onValAdd(this, index, e);
    }

    @Override
    public boolean add(E e) {
        boolean res = super.add(e);
        this.memorySize += this.executionContext.onValAdd(this, size() - 1, e);
        return res;
    }

    @Override
    public E remove(int index) {
        E value = super.remove(index);
        this.memorySize -= this.executionContext.onValRemove(this, index, value);
        return value;
    }

    @Override
    public boolean remove(Object value) {
        int index = super.indexOf(value);
        if (index >= 0) {
            this.remove(index);
            return true;
        }
        return false;
    }

    @Override
    public E set(int index, E element) {
        E oldValue = super.set(index, element);
        this.memorySize -= this.executionContext.onValRemove(this, index, oldValue);
        this.memorySize += this.executionContext.onValAdd(this, index, element);
        return oldValue;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        var list = super.subList(fromIndex, toIndex);
        return new ExecutionArrayList(list, this.executionContext);
    }

    public ExecutionArrayList<E> slice() {
        return new ExecutionArrayList<>(this, this.executionContext);
    }

    public ExecutionArrayList<E> slice(int start) {
        return slice(start, this.size());
    }

    public ExecutionArrayList<E> slice(int start, int end) {
        start = initStartIndex(start);
        end = initEndIndex(end);
        return new ExecutionArrayList<>(this.subList(start, end), this.executionContext);
    }

    public int length() {
        return size();
    }

    public List<E> toUnmodifiable() {
        return ExecutionCollections.unmodifiableExecutionList(this, this.executionContext);
    }

    @Override
    public long memorySize() {
        return memorySize;
    }

    public int indexOf(Object o, int fromIndex) {
        int index = this.slice(fromIndex).indexOf(o);
        return index == -1 ? index : index + fromIndex;
    }

    public String join() {
        return join(",");
    }

    public String join(String separator) {
        String strJoin = this.stream()
                .map(Object::toString)
                .collect(Collectors.joining(separator));
        return strJoin
                .replaceAll(", ", ",")
                .replaceAll("\\[", "")
                .replaceAll("]", "");
    }

    public void sort() {
        this.sort(true);
    }

    public void sort(boolean asc) {
        if (validateClazzInArrayIsOnlyString()) {
            super.sort(asc ? null : stringCompDesc);
        } else {
            super.sort(asc ? numericCompAsc : numericCompDesc);
        }
    }

    public ExecutionArrayList<E> toSorted() {
        return this.toSorted(true);
    }

    public ExecutionArrayList<E> toSorted(boolean asc) {
        ExecutionArrayList newList = this.slice();
        newList.sort(asc);
        return newList;
    }

    public void reverse() {
        Collections.reverse(this);
    }

    public List toReversed() {
        ExecutionArrayList newList = this.slice();
        newList.reverse();
        return newList;
    }

    public List concat(Collection c) {
        ExecutionArrayList newList = this.slice();
        newList.addAll(c);
        return newList;
    }

    public List splice(int start) {
        return this.splice(start, this.size() - start);
    }

    public List splice(int start, int deleteCount, E... values) {
        start = initStartIndex(start);
        deleteCount = deleteCount < 0 ? 0 : Math.min(deleteCount, (this.size() - start));
        List<E> removed = new ArrayList<>();
        while (deleteCount > 0) {
            removed.add(this.remove(start));
            deleteCount--;
        }
        int insertIdx = start;
        for (E e : values) {
            this.add(insertIdx++, e);
        }
        return new ExecutionArrayList<>(removed, this.executionContext);
    }

    public List toSpliced(int start) {
        return this.toSpliced(start, 0);
    }

    public List toSpliced(int start, int deleteCount, E... values) {
        ExecutionArrayList newList = this.slice();
        newList.splice(start, deleteCount, values);
        return newList;
    }

    public List with(int index, E value) {
        int parseIndex = index < 0 ? index + this.size() : index;
        if (parseIndex >= this.size() || parseIndex < 0) {
            throw new IllegalArgumentException("Index: " + index + ", Size: " + this.size());
        } else {
            ExecutionArrayList newList = this.slice();
            newList.splice(parseIndex, 0, value);
            return newList;
        }
    }

    public List fill(E value) {
        return fill(value, 0);
    }

    public List fill(E value, int start) {
        return fill(value, start, this.size());
    }

    public List fill(E value, int start, int end) {
        start = initStartIndex(start);
        end = initEndIndex(end);

        if (start < this.size() && end > start) {
            for (int i = start; i < end; ++i) {
                super.set(i, value);
            }
        }
        return this;
    }

    public boolean validateClazzInArrayIsOnlyString() {
        return !super.stream().anyMatch(e -> !(e instanceof String));
    }

    private int initStartIndex(int start) {
        return start < -this.size() ? 0 :
                start < 0 ? start + this.size() :
                        start;
    }

    private int initEndIndex(int end) {
        return end < -this.size() ? 0 :
                end < 0 ? end + this.size() :
                        Math.min(end, this.size());
    }
}

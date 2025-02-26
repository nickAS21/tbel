package org.mvel2.execution;

import org.mvel2.ExecutionContext;

import java.util.LinkedHashSet;
import java.util.Set;

public class ExecutionLinkedHashSet<E> extends LinkedHashSet<E> implements ExecutionObject {

    private final ExecutionContext executionContext;

    private long memorySize = 0;

    public ExecutionLinkedHashSet(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public ExecutionLinkedHashSet(Set<? extends E> s, ExecutionContext executionContext) {
        super(s);
        this.executionContext = executionContext;
        for (E val : this) {
            this.memorySize += this.executionContext.onValAdd(this, val);
        }
    }

    @Override
    public boolean add(E e) {
        if (super.add(e)) {
            this.memorySize += this.executionContext.onValAdd(this, e);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (super.remove(o)) {
            this.memorySize -= this.executionContext.onValRemove(this, o);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        for (E val : this) {
            this.executionContext.onValRemove(this, val);
        }
        super.clear();
        this.memorySize = 0;
    }

    @Override
    public long memorySize() {
        return this.memorySize;
    }
}

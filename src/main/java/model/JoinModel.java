package model;

import java.util.List;

public class JoinModel<T> {
    private Long count;
    private List<T> values;
    private List<JoinObject> joinedObjects;

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public List<T> getValues() {
        return values;
    }

    public void setAccounts(List<T> values) {
        this.values = values;
    }

    public List<JoinObject> getJoinedObjects() {
        return joinedObjects;
    }

    public void setJoinedAccounts(List<JoinObject> joinedObjects) {
        this.joinedObjects = joinedObjects;
    }
}

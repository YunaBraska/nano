package berlin.yuna.nano.helper;

import static berlin.yuna.typemap.logic.TypeConverter.convertObj;

public record Pair<L, R>(L left, R right) {

    public <T> T left(final Class<T> type) {
        return convertObj(left, type);
    }

    public <T> T right(final Class<T> type) {
        return convertObj(right, type);
    }
}

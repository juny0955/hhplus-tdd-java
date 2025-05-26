package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint validateLeftPoint(long amount) {
        if (point - amount < 0)
            throw new IllegalArgumentException(PointError.NOT_ENOUGH_POINT_TO_USE.getMessage());

        return this;
    }

    public UserPoint validateMaxPoint(long amount) {
        if (point + amount > 100000)
            throw new IllegalArgumentException(PointError.EXCEED_MAX_HOLD_POINT.getMessage());

        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;

        UserPoint userPoint = (UserPoint)object;
        return id == userPoint.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

}

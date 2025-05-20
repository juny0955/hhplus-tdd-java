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
            throw new IllegalArgumentException("사용가능한 포인트가 부족합니다");

        return this;
    }

    public UserPoint validateMaxPoint(long amount) {
        if (point + amount > 100000)
            throw new IllegalArgumentException("유저의 보유 포인트는 10만원을 넘을수 없습니다");

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

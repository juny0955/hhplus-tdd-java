package io.hhplus.tdd.point;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PointError {

	NOT_ENOUGH_POINT_TO_USE("사용가능한 포인트가 부족합니다."),
	EXCEED_MAX_USE_POINT("최대 사용가능한 포인트는 5000원 입니다."),
	BELOW_MIN_CHARGE_POINT("최소 충전 포인트는 500원 이상이어야 합니다."),
	EXCEED_MAX_HOLD_POINT("유저의 보유 포인트는 10만원을 넘을 수 없습니다.");

	private final String message;
}

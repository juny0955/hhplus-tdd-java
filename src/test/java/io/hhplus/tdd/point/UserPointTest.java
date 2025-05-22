package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserPointTest {

	@Test
	void 남은포인트_검증_정상() {
		UserPoint userPoint = new UserPoint(1, 1000, System.currentTimeMillis());

		UserPoint validatedUserPoint = userPoint.validateLeftPoint(500);

		assertThat(userPoint).isEqualTo(validatedUserPoint);
	}

	@Test
	void 남은포인트_검증_예외_부족함() {
		UserPoint userPoint = new UserPoint(1, 1000, System.currentTimeMillis());

		assertThrows(IllegalArgumentException.class, () -> userPoint.validateLeftPoint(2000));
	}

	@Test
	void 최대보유포인트_검증_정상() {
		UserPoint userPoint = new UserPoint(1, 1000, System.currentTimeMillis());

		UserPoint validatedUserPoint = userPoint.validateMaxPoint(500);

		assertThat(userPoint).isEqualTo(validatedUserPoint);
	}

	@Test
	void 최대보유포인트_검증_예외_초과함() {
		UserPoint userPoint = new UserPoint(1, 99999, System.currentTimeMillis());

		assertThrows(IllegalArgumentException.class, () -> userPoint.validateMaxPoint(2000));
	}
}
package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

	private final UserPointTable userPointTable;
	private final PointHistoryTable pointHistoryTable;

	public UserPoint getUserPoint(long userId) {
		UserPoint userPoint = userPointTable.selectById(userId);

		if (userPoint == null)
			throw new IllegalArgumentException("유저가 존재하지 않습니다.");

		return userPoint;
	}

	public List<PointHistory> getUserPointHistories(long userId) {
		UserPoint userPoint = userPointTable.selectById(userId);

		if (userPoint == null)
			throw new IllegalArgumentException("유저가 존재하지 않습니다");

		return pointHistoryTable.selectAllByUserId(userId);
	}

	public UserPoint chargeUserPoint(long userId, long amount) {
		if (amount < 500)
			throw new IllegalArgumentException("최소 충전 포인트는 500원 이상이어야 합니다");

		UserPoint userPoint = userPointTable.selectById(userId);

		if (userPoint == null)
			throw new IllegalArgumentException("유저가 존재하지 않습니다");

		if (userPoint.point() + amount > 100000)
			throw new IllegalArgumentException("유저의 보유 포인트는 10만원을 넘을수 없습니다");

		UserPoint updateUserPoint = userPointTable.insertOrUpdate(userId, userPoint.point() + amount);
		pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
		return updateUserPoint;
	}

	public UserPoint useUserPoint(long userId, long amount) {
		return null;
	}
}

package io.hhplus.tdd.point;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

	private final UserPointTable userPointTable;
	private final PointHistoryTable pointHistoryTable;

	/**
	 * 유저 포인트 조회
	 * @param userId 조회할 유저 ID
	 * @return 조회한 유저 포인트
	 */
	public UserPoint getUserPoint(long userId) {
		UserPoint userPoint = userPointTable.selectById(userId);

		if (userPoint == null)
			throw new NoSuchElementException("유저 포인트가 존재하지 않습니다.");

		return userPoint;
	}

	/**
	 * 유저 포인트 이용 내역 조회
	 * @param userId 조회할 유저 ID
	 * @return 조회한 유저 포인트 내역 목록
	 */
	public List<PointHistory> getUserPointHistories(long userId) {
		UserPoint userPoint = userPointTable.selectById(userId);

		if (userPoint == null)
			throw new NoSuchElementException("유저 포인트가 존재하지 않습니다.");

		return pointHistoryTable.selectAllByUserId(userId);
	}

	/**
	 * 유저 포인트 충전
	 * @param userId 충전할 유저 ID
	 * @param amount 충전할 포인트 금액
	 * @return 충전 후 유저 포인트
	 */
	public UserPoint chargeUserPoint(long userId, long amount) {
		if (amount < 500)
			throw new IllegalArgumentException("최소 충전 포인트는 500원 이상이어야 합니다");

		UserPoint userPoint = userPointTable.selectById(userId);

		if (userPoint == null)
			throw new NoSuchElementException("유저 포인트가 존재하지 않습니다.");

		if (userPoint.point() + amount > 100000)
			throw new IllegalArgumentException("유저의 보유 포인트는 10만원을 넘을수 없습니다");

		UserPoint updateUserPoint = userPointTable.insertOrUpdate(userId, userPoint.point() + amount);
		pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
		return updateUserPoint;
	}

	/**
	 * 유저 포인트 사용
	 * @param userId 사용할 유저 ID
	 * @param amount 사용할 포인트 금액
	 * @return 사용 후 유저 포인트
	 */
	public UserPoint useUserPoint(long userId, long amount) {
		if (amount > 5000)
			throw new IllegalArgumentException("최대 사용가능한 포인트는 5000원 입니다");

		UserPoint userPoint = userPointTable.selectById(userId);

		if (userPoint == null)
			throw new NoSuchElementException("유저 포인트가 존재하지 않습니다.");

		if (userPoint.point() - amount < 0)
			throw new IllegalArgumentException("사용가능한 포인트가 부족합니다");

		UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, userPoint.point() - amount);
		pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
		return updatedUserPoint;
	}
}

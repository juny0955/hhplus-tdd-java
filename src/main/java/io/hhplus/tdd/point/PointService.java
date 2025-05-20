package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

	private static final long MIN_CHARGE_POINT = 500;
	private static final long MAX_USE_POINT = 5000;

	private final UserPointTable userPointTable;
	private final PointHistoryTable pointHistoryTable;

	/**
	 * 유저 포인트 조회
	 * @param userId 조회할 유저 ID
	 * @return 조회한 유저 포인트
	 */
	public UserPoint getUserPoint(long userId) {
		return userPointTable.selectById(userId);
	}

	/**
	 * 유저 포인트 이용 내역 조회
	 * @param userId 조회할 유저 ID
	 * @return 조회한 유저 포인트 내역 목록
	 */
	public List<PointHistory> getUserPointHistories(long userId) {
		return pointHistoryTable.selectAllByUserId(userId);
	}

	/**
	 * 유저 포인트 충전
	 * @param userId 충전할 유저 ID
	 * @param amount 충전할 포인트 금액
	 * @return 충전 후 유저 포인트
	 */
	public UserPoint chargeUserPoint(long userId, long amount) {
		if (amount < MIN_CHARGE_POINT)
			throw new IllegalArgumentException("최소 충전 포인트는 500원 이상이어야 합니다");

		UserPoint userPoint = userPointTable.selectById(userId);

		return processUpdateUserPoint(userPoint.validateMaxPoint(amount), amount, TransactionType.CHARGE);
	}

	/**
	 * 유저 포인트 사용
	 * @param userId 사용할 유저 ID
	 * @param amount 사용할 포인트 금액
	 * @return 사용 후 유저 포인트
	 */
	public UserPoint useUserPoint(long userId, long amount) {
		if (amount > MAX_USE_POINT)
			throw new IllegalArgumentException("최대 사용가능한 포인트는 5000원 입니다");

		UserPoint userPoint = userPointTable.selectById(userId);

		return processUpdateUserPoint(userPoint.validateLeftPoint(amount), amount, TransactionType.USE);
	}

	/**
	 * UserPoint 수정 후 PointHistoryTable에 이력 저장
	 * @param userPoint 수정할 UserPoint
	 * @param amount 수정할 금액
	 * @param transactionType 수정 타입
	 * @return 수정된 UserPoint
	 */
	private UserPoint processUpdateUserPoint(UserPoint userPoint, long amount, TransactionType transactionType) {
		long calculateAmount = transactionType.equals(TransactionType.CHARGE)
			? userPoint.point() + amount
			: userPoint.point() - amount;

		UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userPoint.id(), calculateAmount);
		pointHistoryTable.insert(userPoint.id(), amount, transactionType, System.currentTimeMillis());

		return updatedUserPoint;
	}
}

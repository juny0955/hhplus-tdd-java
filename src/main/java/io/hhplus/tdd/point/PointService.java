package io.hhplus.tdd.point;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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

	private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

	/**
	 * 유저 락을 가져오거나 생성
	 * @param userId 유저 ID
	 * @return 해당 사용자 ReentrantLock
	 */
	private ReentrantLock getUserLock(long userId) {
		return userLocks.computeIfAbsent(userId, id -> new ReentrantLock(true)); // 공정 모드를 사용해야 순서대로 처리됨
	}

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
			throw new IllegalArgumentException(PointError.BELOW_MIN_CHARGE_POINT.getMessage());

		ReentrantLock lock = getUserLock(userId);
		lock.lock();
		try {
			UserPoint userPoint = userPointTable.selectById(userId);

			return processUpdateUserPoint(userPoint.validateMaxPoint(amount), amount, TransactionType.CHARGE);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 유저 포인트 사용
	 * @param userId 사용할 유저 ID
	 * @param amount 사용할 포인트 금액
	 * @return 사용 후 유저 포인트
	 */
	public UserPoint useUserPoint(long userId, long amount) {
		if (amount > MAX_USE_POINT)
			throw new IllegalArgumentException(PointError.EXCEED_MAX_USE_POINT.getMessage());

		ReentrantLock lock = getUserLock(userId);
		lock.lock();
		try {
			UserPoint userPoint = userPointTable.selectById(userId);

			return processUpdateUserPoint(userPoint.validateLeftPoint(amount), amount, TransactionType.USE);
		} finally {
			lock.unlock();
		}
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

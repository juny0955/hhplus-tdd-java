package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

	@InjectMocks
	private PointService pointService;

	@Mock
	private UserPointTable userPointTable;

	@Mock
	private PointHistoryTable pointHistoryTable;

	@Test
	void 유저포인트조회_정상() {
		long userId = 1;
		UserPoint userPoint = UserPoint.empty(userId);

		when(userPointTable.selectById(userId)).thenReturn(userPoint);

		UserPoint result = pointService.getUserPoint(userId);

		verify(userPointTable, times(1)).selectById(userId);
		assertThat(result).isEqualTo(userPoint);
	}

	@Test
	void 유저포인트조회_예외_유저존재하지않음() {
		long userId = 1;

		when(userPointTable.selectById(userId)).thenReturn(null);

		assertThrows(IllegalArgumentException.class, () -> pointService.getUserPoint(userId));

		verify(userPointTable, times(1)).selectById(userId);
	}

	@Test
	void 유저포인트내역조회_정상() {
		long pointHistoryId = 1;
		long userId = 1;
		List<PointHistory> pointHistories = List.of(
			new PointHistory(pointHistoryId, userId, 100, TransactionType.CHARGE, System.currentTimeMillis())
		);

		when(userPointTable.selectById(userId)).thenReturn(UserPoint.empty(userId));
		when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(pointHistories);

		List<PointHistory> result = pointService.getUserPointHistories(userId);

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
		assertThat(result).isEqualTo(pointHistories);
	}

	@Test
	void 유저포인트내역조회_예외_유저존재하지않음() {
		long pointHistoryId = 1;
		long userId = 1;
		List<PointHistory> pointHistories = List.of(
			new PointHistory(pointHistoryId, userId, 100, TransactionType.CHARGE, System.currentTimeMillis())
		);

		when(userPointTable.selectById(userId)).thenReturn(null);
		when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(pointHistories);

		assertThrows(IllegalArgumentException.class, () -> pointService.getUserPointHistories(userId));

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, never()).selectAllByUserId(userId);
	}

	@Test
	void 유저포인트충전_정상() {
		long userId = 1;
		long amount = 1000;
		UserPoint userPoint = new UserPoint(userId, 10000, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(userPoint);

		UserPoint result = pointService.chargeUserPoint(userId, amount);

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, times(1)).insert(userId, amount, TransactionType.CHARGE, anyLong());
		verify(userPointTable, times(1)).insertOrUpdate(userId, userPoint.point() + amount);
		assertThat(result).isEqualTo(userPoint);
		assertThat(result.point()).isEqualTo(userPoint.point() + amount);
	}

	@Test
	void 유저포인트충전_예외_유저찾을수없음() {
		long userId = 1;
		long amount = 1000;
		UserPoint userPoint = new UserPoint(userId, 10000, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(null);

		assertThrows(IllegalArgumentException.class, () -> pointService.chargeUserPoint(userId, amount));

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, never()).insert(userId, amount, TransactionType.CHARGE, anyLong());
		verify(userPointTable, never()).insertOrUpdate(userId, userPoint.point() + amount);
	}

	/**
	 * 유저의 최대 보유 포인트는 10만원을 넘을 수 없음
	 */
	@Test
	void 유저포인트충전_예외_최대포인트초과() {
		long userId = 1;
		long amount = 1000;
		UserPoint userPoint = new UserPoint(userId, 99999, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(userPoint);

		assertThrows(IllegalArgumentException.class, () -> pointService.chargeUserPoint(userId, amount));

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, never()).insert(userId, amount, TransactionType.CHARGE, anyLong());
		verify(userPointTable, never()).insertOrUpdate(userId, userPoint.point() + amount);
	}

	/**
	 * 유저가 보유한 포인트가 10만원일때 정상 충전됨
	 */
	@Test
	void 유저포인트충전_10만원() {
		long userId = 1;
		long amount = 1000;
		UserPoint userPoint = new UserPoint(userId, 99000, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(userPoint);

		UserPoint result = pointService.chargeUserPoint(userId, amount);

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, times(1)).insert(userId, amount, TransactionType.CHARGE, anyLong());
		verify(userPointTable, times(1)).insertOrUpdate(userId, userPoint.point() + amount);
		assertThat(result).isEqualTo(userPoint);
		assertThat(result.point()).isEqualTo(userPoint.point() + amount);
	}

	/**
	 * 유저의 포인트 충전 최소 금액은 500원 이상
	 */
	@Test
	void 유저포인트충전_예외_최소충전포인트미만() {
		long userId = 1;
		long amount = 499;
		UserPoint userPoint = new UserPoint(userId, 10000, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(userPoint);

		assertThrows(IllegalArgumentException.class, () -> pointService.chargeUserPoint(userId, amount));

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, never()).insert(userId, amount, TransactionType.CHARGE, anyLong());
		verify(userPointTable, never()).insertOrUpdate(userId, userPoint.point() + amount);
	}

	/**
	 * 유저가 500원을 충전했을때 정상 충전됨
	 */
	@Test
	void 유저포인트충전_500원충전() {
		long userId = 1;
		long amount = 500;
		UserPoint userPoint = new UserPoint(userId, 10000, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(userPoint);

		UserPoint result = pointService.chargeUserPoint(userId, amount);

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, times(1)).insert(userId, amount, TransactionType.CHARGE, anyLong());
		verify(userPointTable, times(1)).insertOrUpdate(userId, userPoint.point() + amount);
		assertThat(result).isEqualTo(userPoint);
		assertThat(result.point()).isEqualTo(userPoint.point() + amount);
	}

	@Test
	void 유저포인트사용_정상() {
		long userId = 1;
		long amount = 1000;
		UserPoint userPoint = new UserPoint(userId, 10000, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(userPoint);

		UserPoint result = pointService.useUserPoint(userId, amount);

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, times(1)).insert(userId, amount, TransactionType.USE, anyLong());
		verify(userPointTable, times(1)).insertOrUpdate(userId, userPoint.point() - amount);
		assertThat(result).isEqualTo(userPoint);
		assertThat(result.point()).isEqualTo(userPoint.point() - amount);
	}

	@Test
	void 유저포인트사용_예외_유저찾을수없음() {
		long userId = 1;
		long amount = 1000;
		UserPoint userPoint = new UserPoint(userId, 10000, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(null);

		assertThrows(IllegalArgumentException.class, () -> pointService.useUserPoint(userId, amount));

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, never()).insert(userId, amount, TransactionType.USE, anyLong());
		verify(userPointTable, never()).insertOrUpdate(userId, userPoint.point() - amount);
	}

	@Test
	void 유저포인트사용_예외_포인트부족() {
		long userId = 1;
		long amount = 1000;
		UserPoint userPoint = new UserPoint(userId, 0, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(userPoint);

		assertThrows(IllegalArgumentException.class, () -> pointService.useUserPoint(userId, amount));

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, never()).insert(userId, amount, TransactionType.USE, anyLong());
		verify(userPointTable, never()).insertOrUpdate(userId, userPoint.point() - amount);
	}

	/**
	 * 유저의 최대 사용가능한 포인트는 5000원을 초과하지못함
	 */
	@Test
	void 유저포인트사용_예외_최대사용가능포인트초과() {
		long userId = 1;
		long amount = 5001;
		UserPoint userPoint = new UserPoint(userId, 10000, System.currentTimeMillis());

		when(userPointTable.selectById(userId)).thenReturn(userPoint);

		assertThrows(IllegalArgumentException.class, () -> pointService.useUserPoint(userId, amount));

		verify(userPointTable, times(1)).selectById(userId);
		verify(pointHistoryTable, never()).insert(userId, amount, TransactionType.USE, anyLong());
		verify(userPointTable, never()).insertOrUpdate(userId, userPoint.point() - amount);
	}

}
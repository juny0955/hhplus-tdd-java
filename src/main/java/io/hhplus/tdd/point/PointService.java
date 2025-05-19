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
		return null;
	}

	public List<PointHistory> getUserPointHistories(long userId) {
		return null;
	}

	public UserPoint chargeUserPoint(long userId, long amount) {
		return null;
	}

	public UserPoint useUserPoint(long userId, long amount) {
		return null;
	}
}

package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserPointTable userPointTable;

	@Autowired
	private PointHistoryTable pointHistoryTable;

	private static final long USER_ID = 1;
	private static final long INIT_POINT = 50000;
	private static final int THREAD_SIZE = 10;

	@BeforeEach
	void beforeEach() {
		userPointTable.insertOrUpdate(USER_ID, INIT_POINT);
	}

	/**
	 * 동시 충전 테스트
	 * 사용자가 여러번 동시에 포인트 충전하는 상황 가정
	 */
	@Test
	void 동시_포인트_충전_테스트() throws ExecutionException, InterruptedException, TimeoutException {
		long amount = 1000;

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (int i = 0; i < THREAD_SIZE; i++) {
			futures.add(CompletableFuture.runAsync(() -> {
				try {
					mockMvc.perform(patch("/point/{id}/charge", USER_ID)
							.contentType(MediaType.APPLICATION_JSON)
							.content(String.valueOf(amount)))
						.andExpect(status().isOk());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

		// 최종 잔액 확인
		UserPoint userPoint = userPointTable.selectById(USER_ID);
		long finalPoint = INIT_POINT + THREAD_SIZE * amount; // 50000 + 10 * 1000 = 60000원
		assertThat(userPoint.point()).isEqualTo(finalPoint);

		// 히스토리 개수 확인 10건
		List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);
		assertThat(histories).hasSize(THREAD_SIZE);
	}

	/**
	 * 동시 포인트 사용 테스트
	 * 사용자가 여러번 동시에 포인트 사용하는 상황 가정
	 */
	@Test
	void 동시_포인트_사용_테스트() throws ExecutionException, InterruptedException, TimeoutException {
		long amount = 1000;

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (int i = 0; i < THREAD_SIZE; i++) {
			futures.add(CompletableFuture.runAsync(() -> {
				try {
					mockMvc.perform(patch("/point/{id}/use", USER_ID)
							.contentType(MediaType.APPLICATION_JSON)
							.content(String.valueOf(amount)))
						.andExpect(status().isOk());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

		// 최종 잔액 확인
		UserPoint userPoint = userPointTable.selectById(USER_ID);
		long finalPoint = INIT_POINT + THREAD_SIZE * amount; // 50000 - 10 * 1000 = 40000원
		assertThat(userPoint.point()).isEqualTo(finalPoint);

		// 히스토리 개수 확인 10건
		List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);
		assertThat(histories).hasSize(THREAD_SIZE);
	}

	/**
	 * 동시 포인트 충전, 사용 테스트
	 * 1. 요청 순서대로 처리되는지 확인
	 * 2. 충전과 사용 동시 요청시 해당 사용자에대한 락이 잘 동작하는지 확인
	 */
	@Test
	void 동시_충전_사용_요청_테스트() throws Exception {
		long chargeAmount = 2000;
		long useAmount = 1000;

		// 요청 타입 준비 THREAD_SIZE와 동일 10개
		// 사용 4번 충전 6번
		TransactionType[] transactionTypes = {
			TransactionType.USE, TransactionType.USE, TransactionType.CHARGE,
			TransactionType.CHARGE, TransactionType.CHARGE, TransactionType.USE,
			TransactionType.CHARGE, TransactionType.CHARGE, TransactionType.USE,
			TransactionType.CHARGE
		};

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		// 실패 경우는 제외함 초기 포인트 50000원 감안
		for (TransactionType transactionType : transactionTypes) {
			futures.add(CompletableFuture.runAsync(() -> {
				if (transactionType.equals(TransactionType.USE)) {
					try {
						mockMvc.perform(patch("/point/{id}/use", USER_ID)
								.contentType(MediaType.APPLICATION_JSON)
								.content(String.valueOf(useAmount)))
							.andExpect(status().isOk());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				} else {
					try {
						mockMvc.perform(patch("/point/{id}/charge", USER_ID)
								.contentType(MediaType.APPLICATION_JSON)
								.content(String.valueOf(chargeAmount)))
							.andExpect(status().isOk());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}));
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

		// 최종 잔액 확인
		UserPoint userPoint = userPointTable.selectById(USER_ID);
		/**
		 * 충전: 2000 * 6 = 12000
		 * 사용: 1000 * 4 = 4000
		 * 50000 + 12000 - 4000 = 58000
		 */
		long finalPoint = INIT_POINT + Arrays.stream(transactionTypes)
			.mapToLong(type -> type == TransactionType.CHARGE ? chargeAmount : -useAmount)
			.sum();
		assertThat(userPoint.point()).isEqualTo(finalPoint);

		// 히스토리 체크
		List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);
		assertThat(histories).hasSize(THREAD_SIZE);

		// 히스토리 요청 순서 확인
		for (int i = 0; i < 10; i++)
			assertThat(histories.get(i).type()).isEqualTo(transactionTypes[i]);

	}

}

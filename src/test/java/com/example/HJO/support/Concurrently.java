package com.example.HJO.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

/**
 * 동시 실행기. n개 작업을 모두 준비시킨 뒤 신호 한 번으로 동시에 출발시킨다.
 * 차례로 submit만 하면 앞 요청이 끝난 뒤 뒤 요청이 시작돼 경합이 거의 생기지 않는다.
 */
public final class Concurrently {

	private static final long TIMEOUT_SECONDS = 60;

	private Concurrently() {
	}

	/** task(i)를 i = 0..n-1에 대해 동시에 실행하고, 결과를 i 순서대로 돌려준다 */
	public static <T> List<T> run(int n, IntFunction<T> task) {
		ExecutorService pool = Executors.newFixedThreadPool(n);
		CountDownLatch ready = new CountDownLatch(n);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<T>> futures = new ArrayList<>(n);
			for (int i = 0; i < n; i++) {
				int index = i;
				futures.add(pool.submit(() -> {
					ready.countDown();
					start.await();
					return task.apply(index);
				}));
			}
			if (!ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException("workers were not ready in time");
			}
			start.countDown();

			List<T> results = new ArrayList<>(n);
			for (Future<T> future : futures) {
				results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
			}
			return results;
		}
		catch (Exception ex) {
			throw new IllegalStateException("concurrent run failed", ex);
		}
		finally {
			pool.shutdownNow();
		}
	}

}

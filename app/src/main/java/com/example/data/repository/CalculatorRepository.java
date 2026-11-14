package com.example.data.repository;

import androidx.lifecycle.LiveData;
import com.example.data.local.dao.CalcHistoryDao;
import com.example.data.local.entity.CalcHistoryEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class CalculatorRepository {
    private final CalcHistoryDao calcHistoryDao;
    private final ExecutorService ioExecutor;

    public CalculatorRepository(CalcHistoryDao calcHistoryDao, ExecutorService ioExecutor) {
        this.calcHistoryDao = calcHistoryDao;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<List<CalcHistoryEntity>> observeHistory() {
        return calcHistoryDao.observeHistory();
    }

    public void addCalculation(String expression, String result) {
        ioExecutor.execute(() -> calcHistoryDao.insert(
                new CalcHistoryEntity(expression, result, System.currentTimeMillis())
        ));
    }

    public void clearHistory() {
        ioExecutor.execute(calcHistoryDao::clearAll);
    }
}

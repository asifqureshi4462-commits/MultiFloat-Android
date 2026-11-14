package com.example.data.repository;

import androidx.lifecycle.LiveData;
import com.example.data.local.dao.TodoDao;
import com.example.data.local.entity.TodoEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class TodoRepository {
    private final TodoDao todoDao;
    private final ExecutorService ioExecutor;

    public TodoRepository(TodoDao todoDao, ExecutorService ioExecutor) {
        this.todoDao = todoDao;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<List<TodoEntity>> observeAllTodos() {
        return todoDao.observeAllTodos();
    }

    public void addTodo(String title, String priority) {
        if (title == null || title.trim().isEmpty()) return;
        ioExecutor.execute(() -> todoDao.insert(
                new TodoEntity(title.trim(), false, priority, System.currentTimeMillis())
        ));
    }

    public void toggleTodo(TodoEntity entity) {
        ioExecutor.execute(() -> {
            entity.completed = !entity.completed;
            todoDao.update(entity);
        });
    }

    public void deleteTodo(long id) {
        ioExecutor.execute(() -> todoDao.deleteById(id));
    }

    public void clearCompleted() {
        ioExecutor.execute(todoDao::clearCompleted);
    }
}

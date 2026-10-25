package com.andresouza.tasks.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
            SELECT t FROM Task t
            WHERE t.owner.id = :ownerId
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Task> search(@Param("ownerId") Long ownerId,
                      @Param("status") TaskStatus status,
                      @Param("priority") TaskPriority priority,
                      @Param("search") String search,
                      Pageable pageable);

    @Query("SELECT t.status AS status, COUNT(t) AS total FROM Task t WHERE t.owner.id = :ownerId GROUP BY t.status")
    List<StatusCount> countByStatus(@Param("ownerId") Long ownerId);

    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.owner.id = :ownerId AND t.status <> com.andresouza.tasks.task.TaskStatus.DONE
              AND t.dueDate < :today
            """)
    long countOverdue(@Param("ownerId") Long ownerId, @Param("today") LocalDate today);

    interface StatusCount {
        TaskStatus getStatus();

        long getTotal();
    }
}

package com.xiongdwm.future_backend.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.PalOnlineSnapshot;

@Repository
public interface PalOnlineSnapshotRepository extends JpaRepository<PalOnlineSnapshot, Long> {

    /** 查询某时间段内的快照，按时间升序 */
    List<PalOnlineSnapshot> findByRecordedAtBetweenOrderByRecordedAtAsc(Date start, Date end);

    /** 查询某时间之后的所有快照 */
    List<PalOnlineSnapshot> findByRecordedAtAfterOrderByRecordedAtAsc(Date after);

    /** 删除早于指定时间的快照 */
    @Modifying
    @Query("DELETE FROM PalOnlineSnapshot s WHERE s.recordedAt < :before")
    int deleteByRecordedAtBefore(Date before);
}

package org.apache.servicecomb.saga.omega.idempotency;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;

/**
 * @author: takumiCX
 * @create: 2019-03-23
 * @email: takumicx@zju.edu.cn
 **/
public interface TxIdempotencyRepository extends CrudRepository<TxIdempotency, Long> {

    TxIdempotency findByGlobalTxId(@Param("globalTxId") String globalTxId);

    TxIdempotency findByGlobalTxIdAndAndParentTxId(@Param("globalTxId") String globalTxId,
                                                   @Param("parentTxId") String parentTxId);

    //select for update
    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM org.apache.servicecomb.saga.omega.idempotency.TxIdempotency AS t " +
            "WHERE t.globalTxId = :globalTxId " +
            " AND t.parentTxId = :parentTxId ")
    TxIdempotency queryForUpdate(@Param("globalTxId") String globalTxId, @Param("parentTxId") String parentTxId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE org.apache.servicecomb.saga.omega.idempotency.TxIdempotency t " +
            " SET t.state = :state," +
            " t.result = :result " +
            " WHERE t.globalTxId = :globalTxId " +
            " AND t.parentTxId = :parentTxId")
    void updateStateAndResult(@Param("state") String state, @Param("result") byte[] result,
                              @Param("globalTxId") String globalTxId, @Param("parentTxId") String parentTxId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE org.apache.servicecomb.saga.omega.idempotency.TxIdempotency t " +
            " SET t.state = :state" +
            " WHERE t.globalTxId = :globalTxId " +
            " AND t.parentTxId = :parentTxId")
    void updateState(@Param("globalTxId") String globalTxId, @Param("parentTxId") String parentTxId, @Param("state") String state);
}

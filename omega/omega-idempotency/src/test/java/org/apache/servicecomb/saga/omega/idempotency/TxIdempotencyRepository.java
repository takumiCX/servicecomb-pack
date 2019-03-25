package org.apache.servicecomb.saga.omega.idempotency;

import org.springframework.data.repository.CrudRepository;

/**
 * @author: takumiCX
 * @create: 2019-03-23
 * @email: takumicx@zju.edu.cn
 **/
public interface TxIdempotencyRepository extends CrudRepository<TxIdempotency, Long> {

    TxIdempotency findByGlobalTxId(String globalTxId);

}

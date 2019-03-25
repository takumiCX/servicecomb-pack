package org.apache.servicecomb.saga.omega.idempotency;

/**
 * @author: takumiCX
 * @create: 2019-03-24
 * @email: takumicx@zju.edu.cn
 **/
public enum IdempotencyState {
    PENDING,
    SUCCESS,
    FAILED,
    COMPENSATED
}

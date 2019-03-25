package org.apache.servicecomb.saga.omega.idempotency;

import org.apache.servicecomb.saga.omega.context.CallbackContext;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transactional;
import java.util.Date;

/**
 * @author: takumiCX
 * @create: 2019-03-24
 * @email: takumicx@zju.edu.cn
 **/
public class IdempotencyManager {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyManager.class);

    private final TxIdempotencyRepository txIdempotencyRepository;

    private final ServiceConfig serviceConfig;

    private final MessageSerializer messageSerializer;

    private final MessageDeserializer messageDeserializer;

    public IdempotencyManager(TxIdempotencyRepository txIdempotencyRepository, ServiceConfig serviceConfig,
                              MessageSerializer messageSerializer, MessageDeserializer messageDeserializer) {
        this.txIdempotencyRepository = txIdempotencyRepository;
        this.serviceConfig = serviceConfig;
        this.messageSerializer = messageSerializer;
        this.messageDeserializer = messageDeserializer;
    }


    /**
     * 幂等性控制前置方法,在业务方法执行完之前执行
     *
     * @param context
     * @return
     */
    @Transactional
    public Object startControl(OmegaContext context) {

        String globalTxId = context.globalTxId();
        String parentTxId = context.localTxId();
        TxIdempotency txIdempotency = new TxIdempotency();
        txIdempotency.setState(IdempotencyState.PENDING.toString());
        txIdempotency.setCreatedAt(new Date());
        txIdempotency.setUpdatedAt(new Date());
        txIdempotency.setServiceName(serviceConfig.serviceName());
        txIdempotency.setInstanceId(serviceConfig.instanceId());
        txIdempotency.setGlobalTxId(globalTxId);
        txIdempotency.setParentTxId(parentTxId);
        try {
            txIdempotencyRepository.save(txIdempotency);
        } catch (Exception e) {
            //如果已经插入记录则打印日志,继续执行原有流程
            logger.warn("TxIdempotency has inserted!globalTxId:{},parentTxId:{}", globalTxId, parentTxId);
        }

        TxIdempotency idempotency = txIdempotencyRepository.queryForUpdate(globalTxId, parentTxId);

        if (IdempotencyState.SUCCESS.toString().equals(idempotency.getState())) {
            //已成功
            return messageDeserializer.deserialize(idempotency.getResult())[0];
        } else if (IdempotencyState.PENDING.equals(idempotency.getState())) {
            //正在执行
            throw new RuntimeException("Idempotent error!");
        }
        return null;
    }

    /**
     * 幂等性控制后置方法,该方法在业务方法执行完之后执行
     *
     * @param context
     * @param result
     * @param e
     * @throws Throwable
     */
    @Transactional
    public void endControl(OmegaContext context, Object result, Throwable e) throws Throwable {
        String globalTxId = context.globalTxId();
        String localTxId = context.localTxId();
        //因为在执行业务方法之前插入了一条记录,所以这里肯定能查询出结果
        TxIdempotency txIdempotency = txIdempotencyRepository.queryForUpdate(globalTxId, localTxId);
        String state = txIdempotency.getState();
        if (IdempotencyState.SUCCESS.toString().equals(state)) {
            //之前已经执行成功
            throw new RuntimeException("Idempotent error!");
        }

        if (e != null) {
            txIdempotencyRepository.updateStateAndResult(IdempotencyState.FAILED.toString(),
                    messageSerializer.serialize(new Object[]{e.toString()}), globalTxId, localTxId);
            throw e;

        } else {
            txIdempotencyRepository.updateStateAndResult(IdempotencyState.SUCCESS.toString(),
                    messageSerializer.serialize(new Object[]{result}), globalTxId, localTxId);
        }

    }

    @Transactional
    public void startCompensate(String globalTxId, String parentTxId, String compensationMethod, Object[] payloads, CallbackContext context) {

        TxIdempotency txIdempotency = txIdempotencyRepository.queryForUpdate(globalTxId, parentTxId);
        boolean hasCompensated = IdempotencyState.COMPENSATED.toString().equals(txIdempotency.getState());
        if (!hasCompensated) {
            context.apply(globalTxId, parentTxId, compensationMethod, payloads);
            txIdempotencyRepository.updateState(globalTxId, parentTxId, IdempotencyState.COMPENSATED.toString());
        }
    }
}

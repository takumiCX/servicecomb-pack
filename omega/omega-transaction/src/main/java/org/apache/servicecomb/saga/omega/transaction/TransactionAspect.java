/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.transaction;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.idempotency.IdempotencyManager;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class TransactionAspect {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OmegaContext context;

    private final CompensableInterceptor interceptor;

    //幂等性控制
    private final IdempotencyManager idempotencyManager;

    public TransactionAspect(SagaMessageSender sender, OmegaContext context, IdempotencyManager idempotencyManager) {
        this.context = context;
        this.idempotencyManager = idempotencyManager;
        this.interceptor = new CompensableInterceptor(context, sender);
    }

    @Around("execution(@org.apache.servicecomb.saga.omega.transaction.annotations.Compensable * *(..)) && @annotation(compensable)")
    Object advise(ProceedingJoinPoint joinPoint, Compensable compensable) throws Throwable {
        Object result = null;
        Throwable throwable = null;
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String localTxId = context.localTxId();
        try {
            result = idempotencyManager.startControl(context);
            if (result != null) return result;
            context.newLocalTxId();
            LOG.debug("Updated context {} for compensable method {} ", context, method.toString());
            int retries = compensable.retries();
            RecoveryPolicy recoveryPolicy = RecoveryPolicyFactory.getRecoveryPolicy(retries);
            result = recoveryPolicy.apply(joinPoint, compensable, interceptor, context, localTxId, retries);
        } catch (Throwable e) {
            throwable = e;
        } finally {
            idempotencyManager.endControl(context, result, throwable);
            context.setLocalTxId(localTxId);
            LOG.debug("Restored context back to {}", context);

        }
        return result;
    }
}

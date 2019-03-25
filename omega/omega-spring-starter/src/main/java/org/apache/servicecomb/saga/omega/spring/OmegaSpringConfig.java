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

package org.apache.servicecomb.saga.omega.spring;

import com.google.common.collect.ImmutableList;
import org.apache.servicecomb.saga.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.saga.omega.connector.grpc.core.FastestSender;
import org.apache.servicecomb.saga.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.saga.omega.connector.grpc.core.LoadBalanceContextBuilder;
import org.apache.servicecomb.saga.omega.connector.grpc.core.TransactionType;
import org.apache.servicecomb.saga.omega.connector.grpc.saga.SagaLoadBalanceSender;
import org.apache.servicecomb.saga.omega.connector.grpc.tcc.TccLoadBalanceSender;
import org.apache.servicecomb.saga.omega.context.CallbackContext;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.context.UniqueIdGenerator;
import org.apache.servicecomb.saga.omega.format.KryoMessageFormat;
import org.apache.servicecomb.saga.omega.format.MessageFormat;
import org.apache.servicecomb.saga.omega.idempotency.IdempotencyManager;
import org.apache.servicecomb.saga.omega.idempotency.TxIdempotencyRepository;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.saga.omega.transaction.tcc.DefaultParametersContext;
import org.apache.servicecomb.saga.omega.transaction.tcc.ParametersContext;
import org.apache.servicecomb.saga.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.saga.omega.transaction.tcc.TccMessageSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

//fixme
@EntityScan(basePackages = "org.apache.servicecomb.saga.omega")
@Configuration
class OmegaSpringConfig {

  @Bean(name = {"omegaUniqueIdGenerator"})
  IdGenerator<String> idGenerator() {
    return new UniqueIdGenerator();
  }

  @Bean
  OmegaContext omegaContext(@Qualifier("omegaUniqueIdGenerator") IdGenerator<String> idGenerator) {
    return new OmegaContext(idGenerator);
  }

  @Bean(name = {"compensationContext"})
  CallbackContext compensationContext(OmegaContext omegaContext) {
    return new CallbackContext(omegaContext);
  }

  @Bean(name = {"coordinateContext"})
  CallbackContext coordinateContext(OmegaContext omegaContext) {
    return new CallbackContext(omegaContext);
  }

  @Bean
  ServiceConfig serviceConfig(@Value("${spring.application.name}") String serviceName) {
    return new ServiceConfig(serviceName);
  }

  @Bean
  //fixme
  IdempotencyManager idempotencyManager(ServiceConfig serviceConfig, TxIdempotencyRepository txIdempotencyRepository){
    MessageFormat messageFormat = new KryoMessageFormat();
    return new IdempotencyManager(txIdempotencyRepository, serviceConfig,messageFormat,messageFormat);
  }

  @Bean
  ParametersContext parametersContext() {
    return new DefaultParametersContext();
  }

  @Bean
  AlphaClusterConfig alphaClusterConfig(
      @Value("${alpha.cluster.address:localhost:8080}") String[] addresses,
      @Value("${alpha.cluster.ssl.enable:false}") boolean enableSSL,
      @Value("${alpha.cluster.ssl.mutualAuth:false}") boolean mutualAuth,
      @Value("${alpha.cluster.ssl.cert:client.crt}") String cert,
      @Value("${alpha.cluster.ssl.key:client.pem}") String key,
      @Value("${alpha.cluster.ssl.certChain:ca.crt}") String certChain,
      @Lazy MessageHandler handler,
      @Lazy TccMessageHandler tccMessageHandler) {

    MessageFormat messageFormat = new KryoMessageFormat();
    AlphaClusterConfig clusterConfig = AlphaClusterConfig.builder()
        .addresses(ImmutableList.copyOf(addresses))
        .enableSSL(enableSSL)
        .enableMutualAuth(mutualAuth)
        .cert(cert)
        .key(key)
        .certChain(certChain)
        .messageDeserializer(messageFormat)
        .messageSerializer(messageFormat)
        .messageHandler(handler)
        .tccMessageHandler(tccMessageHandler)
        .build();
    return clusterConfig;
  }

  @Bean(name = "sagaLoadContext")
  LoadBalanceContext sagaLoadBalanceSenderContext(
      AlphaClusterConfig alphaClusterConfig,
      ServiceConfig serviceConfig,
      @Value("${omega.connection.reconnectDelay:3000}") int reconnectDelay,
      @Value("${omega.connection.sending.timeout:8}") int timeoutSeconds) {
    LoadBalanceContext loadBalanceSenderContext = new LoadBalanceContextBuilder(
        TransactionType.SAGA,
        alphaClusterConfig,
        serviceConfig,
        reconnectDelay,
        timeoutSeconds).build();
    return loadBalanceSenderContext;
  }

  @Bean
  SagaMessageSender sagaLoadBalanceSender(@Qualifier("sagaLoadContext") LoadBalanceContext loadBalanceSenderContext) {
    final SagaMessageSender sagaMessageSender = new SagaLoadBalanceSender(loadBalanceSenderContext, new FastestSender());
    sagaMessageSender.onConnected();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        sagaMessageSender.onDisconnected();
        sagaMessageSender.close();
      }
    }));
    return sagaMessageSender;
  }

  @Bean(name = "tccLoadContext")
  LoadBalanceContext loadBalanceSenderContext(
      AlphaClusterConfig alphaClusterConfig,
      ServiceConfig serviceConfig,
      @Value("${omega.connection.reconnectDelay:3000}") int reconnectDelay,
      @Value("${omega.connection.sending.timeout:8}") int timeoutSeconds) {
    LoadBalanceContext loadBalanceSenderContext = new LoadBalanceContextBuilder(
        TransactionType.TCC,
        alphaClusterConfig,
        serviceConfig,
        reconnectDelay,
        timeoutSeconds).build();
    return loadBalanceSenderContext;
  }

  @Bean
  TccMessageSender tccLoadBalanceSender(@Qualifier("tccLoadContext") LoadBalanceContext loadBalanceSenderContext) {
    final TccMessageSender tccMessageSender = new TccLoadBalanceSender(loadBalanceSenderContext, new FastestSender());
    tccMessageSender.onConnected();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        tccMessageSender.onDisconnected();
        tccMessageSender.close();
      }
    }));
    return tccMessageSender;
  }
}

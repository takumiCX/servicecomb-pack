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
CREATE TABLE IF NOT EXISTS TxIdempotency (
-- 主键ID
  surrogateId bigint NOT NULL AUTO_INCREMENT,
-- 微服务名称
  serviceName varchar(36) NOT NULL,
-- 微服务实例ID
  instanceId varchar(36) NOT NULL,
-- 创建时间
  createdAt datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
-- 更新时间
  updatedAt datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
-- 全局事务ID
  globalTxId varchar(36) NOT NULL,
-- -- 本地事务ID
--   localTxId varchar(36) NOT NULL,
-- 父事务ID
  parentTxId varchar(36) DEFAULT NULL,
-- 状态
  state varchar(50) NOT NULL,
  result blob,


-- 过期时间
--   expiryTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (surrogateId),
  UNIQUE INDEX idempotency_key (globalTxId,parentTxId)
) DEFAULT CHARSET=utf8;
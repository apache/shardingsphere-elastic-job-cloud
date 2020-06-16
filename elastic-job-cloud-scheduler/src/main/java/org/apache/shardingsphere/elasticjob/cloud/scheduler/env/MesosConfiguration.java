/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.elasticjob.cloud.scheduler.env;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Mesos config.
 */
@RequiredArgsConstructor
@Getter
public final class MesosConfiguration {
    
    /**
     * Framework name.
     */
    public static final String FRAMEWORK_NAME = "Elastic-Job-Cloud";
    
    /**
     * Framework failover timeout in seconds. Default is one week.
     */
    public static final double FRAMEWORK_FAILOVER_TIMEOUT_SECONDS = 60 * 60 * 24 * 7D;
    
    private final String user;
    
    private final String url;
    
    private final String hostname;
}

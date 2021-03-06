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

package org.apache.shardingsphere.elasticjob.cloud.scheduler.config.job;

import org.apache.shardingsphere.elasticjob.cloud.scheduler.producer.ProducerManager;
import org.apache.shardingsphere.elasticjob.cloud.scheduler.state.ready.ReadyService;
import org.apache.shardingsphere.elasticjob.cloud.reg.base.CoordinatorRegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

import java.util.Collections;
import java.util.concurrent.Executors;

/**
 * Cloud job configuration change listener.
 */
@Slf4j
public final class CloudJobConfigurationListener implements TreeCacheListener {
    
    private final CoordinatorRegistryCenter regCenter;
    
    private final ProducerManager producerManager;
    
    private final ReadyService readyService;
    
    public CloudJobConfigurationListener(final CoordinatorRegistryCenter regCenter, final ProducerManager producerManager) {
        this.regCenter = regCenter;
        readyService = new ReadyService(regCenter);
        this.producerManager = producerManager;
    }
    
    @Override
    public void childEvent(final CuratorFramework client, final TreeCacheEvent event) throws Exception {
        String path = null == event.getData() ? "" : event.getData().getPath();
        if (isJobConfigNode(event, path, Type.NODE_ADDED)) {
            CloudJobConfiguration jobConfig = getJobConfig(event);
            if (null != jobConfig) {
                producerManager.schedule(jobConfig);
            }
        } else if (isJobConfigNode(event, path, Type.NODE_UPDATED)) {
            CloudJobConfiguration jobConfig = getJobConfig(event);
            if (null == jobConfig) {
                return;
            }
            if (CloudJobExecutionType.DAEMON == jobConfig.getJobExecutionType()) {
                readyService.remove(Collections.singletonList(jobConfig.getJobName()));
            }
            if (!jobConfig.getTypeConfig().getCoreConfig().isMisfire()) {
                readyService.setMisfireDisabled(jobConfig.getJobName());
            }
            producerManager.reschedule(jobConfig.getJobName());
        } else if (isJobConfigNode(event, path, Type.NODE_REMOVED)) {
            String jobName = path.substring(CloudJobConfigurationNode.ROOT.length() + 1, path.length());
            producerManager.unschedule(jobName);
        }
    }
    
    private boolean isJobConfigNode(final TreeCacheEvent event, final String path, final Type type) {
        return type == event.getType() && path.startsWith(CloudJobConfigurationNode.ROOT) && path.length() > CloudJobConfigurationNode.ROOT.length();
    }
    
    private CloudJobConfiguration getJobConfig(final TreeCacheEvent event) {
        try {
            return CloudJobConfigurationGsonFactory.fromJson(new String(event.getData().getData()));
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            log.warn("Wrong Cloud Job Configuration with:", ex.getMessage());
            // CHECKSTYLE:ON
            return null;
        }
    }
    
    /**
     * Start the listener service of the cloud job service.
     */
    public void start() {
        getCache().getListenable().addListener(this, Executors.newSingleThreadExecutor());
    }
    
    /**
     * Stop the listener service of the cloud job service.
     */
    public void stop() {
        getCache().getListenable().removeListener(this);
    }
    
    private TreeCache getCache() {
        TreeCache result = (TreeCache) regCenter.getRawCache(CloudJobConfigurationNode.ROOT);
        if (null != result) {
            return result;
        }
        regCenter.addCacheData(CloudJobConfigurationNode.ROOT);
        return (TreeCache) regCenter.getRawCache(CloudJobConfigurationNode.ROOT);
    }
}

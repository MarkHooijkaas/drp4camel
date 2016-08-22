/**
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
package org.kisst.drp4camel.drp;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.spi.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DrpComponent extends UriEndpointComponent {
    private final static Logger LOG= LoggerFactory.getLogger(DrpComponent.class);

    private static final AtomicInteger START_COUNTER = new AtomicInteger();

    // must keep a map of consumers on the component to ensure endpoints can lookup old consumers
    // later in case the DrpEndpoint was re-created due the old was evicted from the endpoints LRUCache
    // on DefaultCamelContext
    private static final ConcurrentMap<String, List<DrpConsumer>> CONSUMERS = new ConcurrentHashMap<>();
    private boolean block;
    @Metadata(defaultValue = "30000")
    private long timeout = 30000L;

    public DrpComponent() {
        super(DrpEndpoint.class);
    }

    public static Collection<Endpoint> getConsumerEndpoints() {
        Collection<Endpoint> endpoints = new ArrayList<Endpoint>(CONSUMERS.size());
        for (List<DrpConsumer> list : CONSUMERS.values()) {
            if (! list.isEmpty())
                endpoints.add(list.get(0).getEndpoint());
        }
        return endpoints;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        DrpEndpoint answer = new DrpEndpoint(uri, this);
        answer.setBlock(block);
        answer.setTimeout(timeout);
        answer.configureProperties(parameters);
        return answer;
    }

    public DrpConsumer getConsumer(DrpEndpoint endpoint) {
        String key = getConsumerKey(endpoint.getEndpointUri());
        List<DrpConsumer> list = CONSUMERS.get(key);

        if (list==null) {
            LOG.info("Found no consumers for {}",key);
            return null;
        }
        if (list.isEmpty())
            return null;
        int count=0;
        for (DrpConsumer cons: list) {
            if (cons.isStarted()) {
                LOG.info("Used consumer number {} of {} for {}",count, list.size(),key);
                return cons;
            }
            count++;
        }
        LOG.info("No started consumer of {} registered consumers for {}",count, list.size(),key);
        return null;
    }

    public void addConsumer(DrpEndpoint endpoint, DrpConsumer consumer) {
        String key = getConsumerKey(endpoint.getEndpointUri());
        List<DrpConsumer> list = CONSUMERS.get(key);
        if (list==null)
            list=new ArrayList<>(1);
        else
            list=new ArrayList<>(list);
        list.add(consumer);
        LOG.info("Adding consumer {} for endpoint {}", list.size(), endpoint);
        CONSUMERS.put(key, list);
    }

    // TODO: better mechanism to handle suspend
    // This method is called twice, once from suspend, and once from stop, so should not check if it is known
    public void removeConsumer(DrpEndpoint endpoint, DrpConsumer consumer) {
        //LOG.info("Removing consumer for endpoint {}", endpoint);
        String key = getConsumerKey(endpoint.getEndpointUri());
        List<DrpConsumer> list = CONSUMERS.get(key);
        if (list==null)
            throw new RuntimeException("No consumer to remove for endpoint "+consumer.getEndpoint().toString());
            //return;
        else {
            list=new ArrayList<>(list);
            boolean found = list.remove(consumer);
            if (! found)
                throw new RuntimeException("Could not find specific consumer to remove for endpoint "+consumer.getEndpoint().toString());
                //return;
        }
        LOG.info("Removed consumer for endpoint {}, {} consumers remaining", endpoint, list.size());
        if (list.isEmpty())
            CONSUMERS.remove(key);
        else
            CONSUMERS.put(key, list);
    }

    private static String getConsumerKey(String uri) {
        if (uri.contains("?")) {
            // strip parameters
            uri = uri.substring(0, uri.indexOf('?'));
        }
        return uri;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        START_COUNTER.incrementAndGet();
    }

    @Override
    protected void doStop() throws Exception {
        if (START_COUNTER.decrementAndGet() <= 0) {
            // clear queues when no more DrpComponents in use
            CONSUMERS.clear();
        }
        super.doStop();
    }

    public boolean isBlock() {
        return block;
    }

    /**
     * If sending a message to a drp endpoint which has no active consumer,
     * then we can tell the producer to block and wait for the consumer to become active.
     */
    public void setBlock(boolean block) {
        this.block = block;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}

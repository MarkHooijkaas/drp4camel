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

import org.apache.camel.Consumer;
import org.apache.camel.MultipleConsumersSupport;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;


@UriEndpoint(scheme = "drp", title = "Dynamic Routing Point", syntax = "drp:name", consumerClass = DrpConsumer.class, label = "core,endpoint")
public class DrpEndpoint extends DefaultEndpoint implements MultipleConsumersSupport {

    @UriPath(description = "Name of drp endpoint") @Metadata(required = "true")
    private String name;

    @UriParam(label = "producer")
    private boolean block;
    @UriParam(label = "producer", defaultValue = "30000")
    private long timeout = 30000L;

    public DrpEndpoint(String endpointUri, DrpComponent component) {
        super(endpointUri, component);
    }

    @Override
    public DrpComponent getComponent() {
        return (DrpComponent) super.getComponent();
    }

    @Override
    public Producer createProducer() throws Exception {
        if (block) {
            return new DrpBlockingProducer(this);
        } else {
            return new DrpProducer(this);
        }
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        Consumer answer = new DrpConsumer(this, new DrpProcessor(processor, this));
        configureConsumer(answer);
        return answer;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public DrpConsumer getConsumer() {
        return getComponent().getConsumer(this);
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

    /**
     * The timeout value to use if block is enabled.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override public boolean isMultipleConsumersSupported() { return true; }
}

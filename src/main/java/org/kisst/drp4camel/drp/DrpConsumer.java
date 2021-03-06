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

import org.apache.camel.Processor;
import org.apache.camel.SuspendableService;
import org.apache.camel.impl.DefaultConsumer;

/**
 * The drp consumer
 */
public class DrpConsumer extends DefaultConsumer implements SuspendableService {

    public DrpConsumer(DrpEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    public DrpEndpoint getEndpoint() {
        return (DrpEndpoint) super.getEndpoint();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        getEndpoint().getComponent().addConsumer(getEndpoint(), this);
    }

    @Override
    protected void doStop() throws Exception {
        getEndpoint().getComponent().removeConsumer(getEndpoint(), this);
        super.doStop();
    }

    @Override
    protected void doSuspend() throws Exception { suspend(); }

    @Override
    protected void doResume() throws Exception { resume(); }

}

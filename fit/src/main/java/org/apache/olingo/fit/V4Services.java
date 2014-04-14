/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.fit;

import org.apache.olingo.fit.utils.XHTTPMethodInterceptor;
import javax.ws.rs.Path;
import org.apache.cxf.interceptor.InInterceptors;
import org.apache.olingo.commons.api.data.Feed;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.fit.utils.ResolvingReferencesInterceptor;
import org.springframework.stereotype.Service;

@Service
@Path("/V40/Static.svc")
@InInterceptors(classes = {XHTTPMethodInterceptor.class, ResolvingReferencesInterceptor.class})
public class V4Services extends AbstractServices {

  public V4Services() throws Exception {
    super(ODataServiceVersion.V40);
  }

  @Override
  protected void setInlineCount(final Feed feed, final String count) {
    if ("true".equals(count)) {
      feed.setCount(feed.getEntries().size());
    }
  }
}
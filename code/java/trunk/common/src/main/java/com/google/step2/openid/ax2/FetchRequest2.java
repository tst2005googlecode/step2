/**
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.google.step2.openid.ax2;

import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.FetchRequest;

/**
 * Temporary fix to enable more flexible request handling than openid4java
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class FetchRequest2 extends FetchRequest {
  @Override
  public String getTypeUri() {
    return AxMessage2.OPENID_NS_AX_FINAL;
  }

  public FetchRequest2() {
    super();
  }
  
  public FetchRequest2(ParameterList params) {
    super(params);
  }

  public static FetchRequest2 createFetchRequest2(ParameterList params)
    throws MessageException {
      FetchRequest2 req = new FetchRequest2(params);

      if (!req.isValid()) {
        throw new MessageException("Invalid parameters for a fetch request");
      }

      return req;
  }
  
  private static final String COUNT = "count.";

  /**
   * Need to override this since there is a bug in the openid4java
   * implementation as of 11/3/08:
   * http://code.google.com/p/openid4java/issues/detail?id=73
   */
  @Override
  public int getCount(String alias) {
    if ("unlimited".equals(_parameters.getParameterValue(COUNT + alias))) {
      // 0 means unlimited
      return 0;
    } else if (_parameters.hasParameter(COUNT + alias)) {
      try {
        return Integer.parseInt(_parameters.getParameterValue(COUNT + alias));
      } catch (NumberFormatException e) {
        // we'll treat malformed count parameters the same as absent parameters,
        // returning at most one value
        return 1;
      }
    } else {
      // according to spec, an absent count parameter means to return at most
      // one value
      return 1;
    }
  }
}

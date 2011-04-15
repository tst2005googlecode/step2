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

import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.FetchResponse;

/**
 * Temporary fix to enable more flexible request handling.
 *
 * @author balfanz@google.com (Dirk Balfanz)
 */
public class FetchResponse2 extends FetchResponse {
  @Override
  public String getTypeUri() {
    return AxMessage2.OPENID_NS_AX_FINAL;
  }

  public FetchResponse2() {
    super();
  }

  public FetchResponse2(ParameterList params) {
    super(params);
  }

  /**
   * The openid4java library will only allow omission of the claimed_id and
   * identity attributes from requests and responses if some other extension
   * included in the message claims to provide an alternative "identifier".
   */
  @Override
  public boolean providesIdentifier() {
    // TODO(balfanz): provide a better implementation
    return true;
  }

  @Override
  public boolean signRequired() {
    return true;
  }

  public static FetchResponse2 createFetchResponse2(ParameterList params) {
    FetchResponse2 resp = new FetchResponse2(params);

    //TODO(balfanz): it would be nice if the FetchReponse.isValid() method was
    // at least protected so we could validate the response.
    return resp;
  }

}

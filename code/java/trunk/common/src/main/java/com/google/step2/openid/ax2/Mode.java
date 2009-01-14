package com.google.step2.openid.ax2;

import org.openid4java.message.MessageException;

enum Mode { 
  FETCH_REQUEST("fetch_request"),
  FETCH_RESPONSE("fetch_response");
  
  private final String mode;

  private Mode(String value) {
    this.mode = value;
  }
  
  @Override
  public String toString() {
    return mode;
  }
  
  static Mode getMode(String mode) throws MessageException {
    if (FETCH_REQUEST.mode.equals(mode)) {
      return FETCH_REQUEST;
    } else if (FETCH_RESPONSE.mode.equals(mode)) {
      return FETCH_RESPONSE;
    }
    throw new MessageException("Unknown mode: " + mode);
  }
}

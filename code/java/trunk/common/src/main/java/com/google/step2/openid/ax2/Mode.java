package com.google.step2.openid.ax2;

import org.openid4java.message.MessageException;

enum Mode { 
  FETCH_REQUEST("fetch_request"),
  FETCH_RESPONSE("fetch_response"),
  VALIDATE_REQUEST("validate_request"),
  VALIDATE_RESPONSE_SUCCESS("validate_reqeust_success"),
  VALIDATE_RESPONSE_FAILURE("validate_reqeust_failure");
  
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
    } if (VALIDATE_REQUEST.mode.equals(mode)) {
      return VALIDATE_REQUEST;
    } if (VALIDATE_RESPONSE_SUCCESS.mode.equals(mode)) {
      return VALIDATE_RESPONSE_SUCCESS;
    } if (VALIDATE_RESPONSE_FAILURE.mode.equals(mode)) {
      return VALIDATE_RESPONSE_FAILURE;
    }
    throw new MessageException("Unknown mode: " + mode);
  }
}

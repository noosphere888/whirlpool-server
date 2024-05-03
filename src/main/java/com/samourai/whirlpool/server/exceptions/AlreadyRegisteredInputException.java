package com.samourai.whirlpool.server.exceptions;

import com.samourai.javaserver.exceptions.NotifiableException;

public class AlreadyRegisteredInputException extends NotifiableException {

  public AlreadyRegisteredInputException(String message) {
    super(ServerErrorCode.INPUT_ALREADY_REGISTERED, message);
  }
}

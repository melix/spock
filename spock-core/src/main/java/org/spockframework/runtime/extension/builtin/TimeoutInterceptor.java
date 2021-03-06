/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.spockframework.runtime.extension.builtin;

import org.spockframework.runtime.SpockTimeoutError;
import org.spockframework.runtime.extension.IMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;

import spock.lang.Timeout;

/**
 * Implementation of @Timeout.
 *
 * @author Peter Niederwieser
 */

public class TimeoutInterceptor implements IMethodInterceptor {
  private final Timeout timeout;

  public TimeoutInterceptor(Timeout timeout) {
    this.timeout = timeout;
  }

  public void intercept(final IMethodInvocation invocation) throws Throwable {
    final Throwable[] exception = new Throwable[1];

    Thread thread = new Thread() {
      public void run() {
        try {
          invocation.proceed();
        } catch (Throwable t) {
          exception[0] = t;
        }
      }
    };

    thread.start();
    thread.join(timeout.unit().toMillis(timeout.value()));
    if (thread.isAlive()) {
      StackTraceElement[] stack = thread.getStackTrace();

      // IDEA: Isn't thread.stop() more likey to succeed (considering it throws
      // an Error instead of an Exception)? Are its risks tolerable here?
      thread.interrupt();
      SpockTimeoutError error = new SpockTimeoutError("method timed out after %s %s",
          timeout.value(), timeout.unit().toString().toLowerCase());
      error.setStackTrace(stack);
      throw error;
    }

    if (exception[0] != null)
      throw exception[0];
  }
}
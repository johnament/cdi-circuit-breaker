/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package ws.ament.circuits;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.jodah.failsafe.RetryPolicy;

@ApplicationScoped
class CircuitRegistry {
   @Inject
   private Instance<RetryPolicy> retryPolicyInstance;

   private final Map<Method, MethodExecutor> executors = new HashMap<>();

   MethodExecutor getExecutor(Method method) {
      return executors.computeIfAbsent(method, (Method method1) -> {
         NamedLiteral namedLiteral = new NamedLiteral(method1.getName());
         Instance<RetryPolicy> policies = retryPolicyInstance.select(namedLiteral);
         if(!policies.isAmbiguous() && !policies.isUnsatisfied()) {
            return new DefinedRetryMethodExecutor(policies.get());
         }
         Retries annotation = method1.getDeclaredAnnotation(Retries.class);
         return new AnnotationBackedMethodExecutor(annotation);
      });
   }

   private static final class NamedLiteral extends AnnotationLiteral<Named> implements Named {
      private final String name;

      public NamedLiteral(String name) {
         this.name = name;
      }
      @Override
      public String value() {
         return name;
      }
   }
}

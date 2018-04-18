/*
 * Copyright 2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.context;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.Executable;
import io.micronaut.core.util.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * An abstract {@link Executable} for a method
 * @author graemerocher
 * @since 1.0
 */
@Internal
abstract class AbstractExecutable implements Executable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractExecutableMethod.class);

    protected final Class declaringType;
    protected final String methodName;
    protected final Class[] argTypes;

    private Argument[] arguments;
    private Method method;

    AbstractExecutable(Class declaringType, String methodName, Argument[] arguments) {
        Objects.requireNonNull(declaringType, "Declaring type cannot be null");
        Objects.requireNonNull(methodName, "Method name cannot be null");

        this.argTypes = Argument.toClassArray(arguments);
        this.declaringType = declaringType;
        this.methodName = methodName;

        if( ArrayUtils.isNotEmpty(arguments) ) {
            this.arguments = arguments;
        }
        else {
            this.arguments= Argument.ZERO_ARGUMENTS;
        }

    }

    /**
     * Soft resolves the target {@link Method} avoiding reflection until as late as possible
     * @return The method
     */
    public final Method getTargetMethod() {
        if(method == null) {
            Method method = ReflectionUtils.getMethod(declaringType, methodName, argTypes)
                    .orElseThrow(() -> {
                        if(LOG.isWarnEnabled()) {
                            if(LOG.isWarnEnabled()) {
                                LOG.warn("Type [{}] previously declared a method [{}] which has been removed or changed. It is recommended you re-compile the class or library against the latest version to remove this warning.", declaringType, methodName);
                            }
                        }
                        return ReflectionUtils.newNoSuchMethodError(declaringType, methodName, argTypes);
                    });

            // instrument the arguments with annotation data from the method
            method.setAccessible(true);
            Argument[] newArguments = new Argument[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                Argument argument = arguments[i];
                newArguments[i] = Argument.of(
                        method,
                        argument.getName(),
                        i,
                        null,
                        argument.getTypeParameters()
                );
            }
            this.arguments = newArguments;
            this.method = method;
            return method;
        }
        else {
            return method;
        }
    }

    @Override
    public Argument[] getArguments() {
        // initialize
        getTargetMethod();
        return arguments;
    }
}
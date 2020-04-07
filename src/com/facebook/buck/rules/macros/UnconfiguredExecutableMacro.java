/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.buck.rules.macros;

import com.facebook.buck.core.model.TargetConfiguration;
import com.facebook.buck.core.model.UnconfiguredBuildTargetWithOutputs;
import com.facebook.buck.core.util.immutables.BuckStyleValue;

/** Unconfigured graph version of {@link com.facebook.buck.rules.macros.ExecutableMacro}. */
@BuckStyleValue
public abstract class UnconfiguredExecutableMacro
    extends AbstractUnconfiguredExecutableTargetOrHostMacro {

  @Override
  public ExecutableMacro configure(
      TargetConfiguration targetConfiguration, TargetConfiguration hostConfiguration) {
    return ExecutableMacro.of(getTargetWithOutputs().configure(targetConfiguration));
  }

  public static UnconfiguredExecutableMacro of(UnconfiguredBuildTargetWithOutputs buildTarget) {
    return ImmutableUnconfiguredExecutableMacro.ofImpl(buildTarget);
  }
}
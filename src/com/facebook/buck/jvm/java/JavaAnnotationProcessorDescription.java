/*
 * Copyright 2017-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java;

import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.parser.NoSuchBuildTargetException;
import com.facebook.buck.rules.AbstractDescriptionArg;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.CellPathResolver;
import com.facebook.buck.rules.Description;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.SourcePathRuleFinder;
import com.facebook.buck.rules.TargetGraph;
import com.facebook.buck.util.HumanReadableException;
import com.facebook.buck.versions.VersionPropagator;
import com.facebook.infer.annotation.SuppressFieldNotInitialized;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Description of a rule that builds a Java compiler plugin (either a {@link
 * javax.annotation.processing.Processor} or TODO(jkeljo): a {@link com.sun.source.util.Plugin}).
 */
public class JavaAnnotationProcessorDescription
    implements Description<JavaAnnotationProcessorDescription.Arg>,
        VersionPropagator<JavaAnnotationProcessorDescription.Arg> {
  @Override
  public Class<Arg> getConstructorArgType() {
    return Arg.class;
  }

  @Override
  public <A extends Arg> BuildRule createBuildRule(
      TargetGraph targetGraph,
      BuildRuleParams params,
      BuildRuleResolver resolver,
      CellPathResolver cellRoots,
      A args)
      throws NoSuchBuildTargetException {
    JavacPluginProperties.Builder propsBuilder = JavacPluginProperties.builder();
    propsBuilder.addProcessorNames(args.processorClass);
    for (BuildRule dep : params.getBuildDeps()) {
      if (!(dep instanceof JavaLibrary)) {
        throw new HumanReadableException(
            String.format(
                "%s: dependencies must produce JVM libraries; %s is a %s",
                params.getBuildTarget(), dep.getBuildTarget(), dep.getType()));
      }
      propsBuilder.addDep(dep);
    }

    boolean reuseClassLoader = !args.isolateClassLoader;
    propsBuilder.setCanReuseClassLoader(reuseClassLoader);
    propsBuilder.setDoesNotAffectAbi(args.doesNotAffectAbi);
    propsBuilder.setSupportsAbiGenerationFromSource(args.supportsAbiGenerationFromSource);
    JavacPluginProperties properties = propsBuilder.build();

    SourcePathResolver pathResolver = new SourcePathResolver(new SourcePathRuleFinder(resolver));
    return new JavaAnnotationProcessor(
        params.copyAppendingExtraDeps(properties.getClasspathDeps()), pathResolver, properties);
  }

  @SuppressFieldNotInitialized
  public static class Arg extends AbstractDescriptionArg {
    public ImmutableSortedSet<BuildTarget> deps = ImmutableSortedSet.of();
    public String processorClass;
    public boolean isolateClassLoader = false;
    /**
     * A value of false indicates that the annotation processor generates classes that are intended
     * for use outside of the code being processed. Annotation processors that affect the ABI of the
     * rule in which they run must be run during ABI generation from source.
     *
     * <p>Defaults to false because that's the "safe" value. When migrating to ABI generation from
     * source, having as few ABI-affecting processors as possible will yield the fastest ABI
     * generation.
     */
    public boolean doesNotAffectAbi = false;

    /**
     * If true, allows ABI-affecting annotation processors to run during ABI generation from source.
     * To run during ABI generation from source, an annotation processor must meet all of the
     * following criteria:
     * <li>
     *
     *     <ul>
     *       Uses only the public APIs from JSR-269 (annotation processing). Access to the Compiler
     *       Tree API may also be possible via a Buck support library.
     * </ul>
     *
     * <ul>
     *   Does not require details about types beyond those being compiled as a general rule. There
     *   are ways to ensure type information is available on a case by case basis, at some
     *   performance cost.
     * </ul>
     *
     * Defaults to false because that's the "safe" value. When migrating to ABI generation from
     * source, having as many ABI-affecting processors as possible running during ABI generation
     * will result in the flattest build graph.
     */
    public boolean supportsAbiGenerationFromSource = false;
  }
}

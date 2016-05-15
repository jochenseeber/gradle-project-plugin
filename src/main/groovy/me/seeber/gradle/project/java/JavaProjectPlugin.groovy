/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2015, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package me.seeber.gradle.project.java

import groovy.transform.TypeChecked
import me.seeber.gradle.plugin.AbstractProjectPlugin
import me.seeber.gradle.project.base.BaseProjectPlugin

import org.gradle.api.artifacts.ComponentSelection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar

@TypeChecked
class JavaProjectPlugin extends AbstractProjectPlugin<JavaProjectExtension> {

    private Logger logger = Logging.getLogger(JavaProjectPlugin)

    JavaProjectPlugin() {
        super("javaConfig", JavaProjectExtension)
    }

    void initialize() {
        project.with {
            plugins.apply(BaseProjectPlugin)
            plugins.apply(JavaPlugin)

            JavaPluginConvention java = convention(JavaPluginConvention)

            java.with {
                sourceCompatibility = 1.8
                targetCompatibility = 1.8
            }

            tasks.create("sourcesJar", Jar).with {
                description = "Assembles a jar archive containing the sources."
                classifier = "sources"
                from java.sourceSets.getByName("main").allJava
            }
        }
    }

    void configure() {
        project.with {
            configurations.all { Configuration configuration ->
                configuration.resolutionStrategy.componentSelection.all { ComponentSelection selection ->
                    ExcludeRule matching = this.config.bannedComponents.rules.find { ExcludeRule exclude ->
                        (exclude.group == null || exclude.group == selection.candidate.group) &&
                                (exclude.module == null || exclude.module == selection.candidate.module)
                    }

                    if(matching) {
                        selection.reject("Dependency ${selection.candidate} is banned by rule ${matching.group}:${matching.module}")
                    }
                }
            }
        }
    }
}

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

import groovy.transform.InheritConstructors
import groovy.transform.TypeChecked
import me.seeber.gradle.plugin.AbstractProjectPlugin
import me.seeber.gradle.util.Projects

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc

@TypeChecked
@InheritConstructors
abstract class JavaBaseProjectPlugin<T extends JavaBaseProjectExtension> extends AbstractProjectPlugin<T> {

    @Override
    protected void initialize() {
        Project.metaClass.mixin(Projects)

        project.with {
            plugins.apply(JavaProjectPlugin)

            JavaPluginConvention java = convention.findPlugin(JavaPluginConvention)

            Javadoc javadoc = tasks.withType(Javadoc).getAt("javadoc")

            Jar javadocJar = tasks.create("javadocJar", Jar)
            javadocJar.with {
                description = "Assembles a jar archive containing the JavaDoc documentation."
                classifier = "javadoc"
                from(javadoc.destinationDir)
            }

            Jar testsJar = tasks.create("testsJar", Jar)
            testsJar.with {
                description = "Assembles a jar archive containing the unit tests."
                classifier = "tests"
                from(java.sourceSets.getAt("test").output)
            }

            Jar sourcesJar = tasks.withType(Jar).getAt("sourcesJar")

            artifacts.add("archives", sourcesJar)
            artifacts.add("archives", javadocJar)
            artifacts.add("archives", testsJar)

            dependencies.add("compile", [group: "org.eclipse.jdt", name: "org.eclipse.jdt.annotation", version: "2.0.0"])
        }
    }
}

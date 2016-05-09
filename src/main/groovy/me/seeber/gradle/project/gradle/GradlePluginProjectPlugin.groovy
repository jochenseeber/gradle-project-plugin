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
package me.seeber.gradle.project.gradle

import groovy.transform.TypeChecked
import me.seeber.gradle.plugin.AbstractProjectPlugin
import me.seeber.gradle.project.base.BaseProjectExtension
import me.seeber.gradle.project.java.JavaProjectPlugin
import me.seeber.gradle.distribution.maven.MavenDistributionPlugin

import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin

import com.gradle.publish.PluginBundleExtension
import com.gradle.publish.PublishPlugin

@TypeChecked
class GradlePluginProjectPlugin extends AbstractProjectPlugin<GradlePluginProjectExtension> {

    GradlePluginProjectPlugin() {
        super("gradlePluginConfig", GradlePluginProjectExtension)
    }

    void initialize() {
        this.project.with {
            plugins.apply(JavaProjectPlugin)
            plugins.apply(MavenDistributionPlugin)
            plugins.apply(GroovyPlugin)
            plugins.apply(JavaGradlePluginPlugin)
            plugins.apply(PublishPlugin)

            dependencies.gradleApi()
            dependencies.localGroovy()

            dependencies.add("compile", dependencies.gradleApi())
            dependencies.add("compile", dependencies.localGroovy())

            dependencies.add("testCompile", [group: "org.spockframework", name: "spock-core", version: "1.0-groovy-2.4"]) { ExternalDependency d ->
                d.exclude(group: "org.codehaus.groovy")
            }
        }
    }

    void configure() {
        this.project.with {
            Groovydoc groovydocTask = tasks.withType(Groovydoc).getAt("groovydoc")

            tasks.create("groovydocJar", Jar).with {
                description = "Assembles a jar archive containing the groovydoc documentation."
                classifier = "groovydoc"
                from(groovydocTask.destinationDir)
            }

            Jar sourcesJar =  tasks.withType(Jar).getAt("sourcesJar")
            Jar groovydocJar = tasks.withType(Jar).getAt("groovydocJar")

            artifacts.add("archives", sourcesJar)
            artifacts.add("archives", groovydocJar)

            // Generate test metadata for eclipse
            tasks.findByName("eclipse")?.dependsOn(tasks.getByName("pluginUnderTestMetadata"))

            extension(PublishingExtension).with {
                publications.create(this.config.publicationName, MavenPublication).with {
                    from(components.getAt("java"))
                    artifact(source: sourcesJar, classifier: "sources")
                    artifact(source: groovydocJar, classifier: "groovydoc")
                }
            }
        }
    }

    void complete() {
        BaseProjectExtension base = extension(BaseProjectExtension)

        extension(PluginBundleExtension).with {
            description = description ?: base.project.description

            plugins.each {
                description = description ?: base.project.description
            }
        }
    }
}

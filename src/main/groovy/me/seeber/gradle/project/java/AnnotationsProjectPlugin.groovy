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

import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Zip

@TypeChecked
class AnnotationsProjectPlugin extends AbstractProjectPlugin<AnnotationsProjectExtension> {

    AnnotationsProjectPlugin() {
        super("annotationsConfig", AnnotationsProjectExtension)
    }

    void initialize() {
        // Apply java project plugin
        project.plugins.apply(JavaProjectPlugin)
    }

    void configure() {
        project.with {
            BasePluginConvention java = convention.getPlugin(BasePluginConvention)

            Zip annotationsJar = tasks.create("annotationsJar", Zip)
            annotationsJar.with {
                description = "Assembles a jar archive containing the external annotations."
                classifier = "annotations"
                extension = "jar"
                from("src/annotations/resources")

                conventionMapping.with {
                    map("destinationDir") { java.libsDir }
                }
            }

            artifacts.add("archives", annotationsJar)

            plugins.withType(MavenPublishPlugin) {
                extension(PublishingExtension).with {
                    publications.create(config.publicationName, MavenPublication).with { artifact(source: annotationsJar) }
                }
            }
        }
    }
}

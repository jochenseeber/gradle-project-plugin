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
package me.seeber.gradle.distribution.maven

import groovy.transform.TypeChecked
import me.seeber.gradle.plugin.AbstractProjectPlugin
import me.seeber.gradle.project.base.BaseProjectExtension
import me.seeber.gradle.project.base.BaseProjectPlugin
import me.seeber.groovy.util.NodeWrapper

import org.gradle.api.Task
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.GenerateMavenPom

@TypeChecked
class MavenDistributionPlugin extends AbstractProjectPlugin<MavenDistributionExtension> {

    MavenDistributionPlugin() {
        super("mavenConfig", MavenDistributionExtension)
    }

    void initialize() {
        project.plugins.apply(BaseProjectPlugin)
        project.plugins.apply(MavenPublishPlugin)
    }

    void complete() {
        Task publishTask = project.tasks.findByName("publishToMavenLocal")

        if(publishTask != null) {
            project.tasks.create("install", Task).with {
                dependsOn publishTask
                description = "Install to local repository, convenience alias for 'publishToMavenLocal'"
            }
        }

        this.project.tasks.withType(GenerateMavenPom) { GenerateMavenPom task ->
            task.doFirst { configureGeneratePomTask(task) }
        }
    }

    protected void configureGeneratePomTask(GenerateMavenPom task) {
        task.pom.with {
            withXml { XmlProvider xml ->
                NodeWrapper root = new NodeWrapper(xml.asNode())
                completeMavenPomXml(root)
            }
        }
    }

    protected void completeMavenPomXml(NodeWrapper root) {
        BaseProjectExtension config = extension(BaseProjectExtension)

        root["inceptionYear"].init(config.inceptionYear as String)
        root["url"].init(config.websiteUrl as String)
        root["organization"]["name"].init(config.organization.name)
        root["licenses"]["license"]["name"].init(config.license.name)
        root["licenses"]["license"]["url"].init(config.license.url?.toASCIIString())
        root["scm"]["connection"].init(config.repository.connection?.with { "scm:${config.repository.type}:${it}" })
        root["scm"]["developerConnection"].init(config.repository.developerConnection?.with { "scm:${config.repository.type}:${it}" })
        root["scm"]["url"].init(config.repository.websiteUrl?.with { it.toASCIIString() })
        root["issueManagement"]["url"].init(config.issueTracker.websiteUrl?.with { it.toASCIIString() })

        NodeWrapper dependencies = root["dependencies"]

        dependencies.children.each { NodeWrapper dependencyNode ->
            String groupId = dependencyNode["groupId"].text
            String artifactId = dependencyNode["artifactId"].text
            String scope = dependencyNode["scope"].text
            String classifier = dependencyNode["classifier"].text
            String type = dependencyNode["type"].text

            if(scope == "runtime") {
                Dependency dependency = project.configurations.getAt("compile").allDependencies.find { Dependency d ->
                    boolean found = false

                    if(d.group == groupId) {
                        if(d instanceof ModuleDependency) {
                            ModuleDependency moduleDependency = d as ModuleDependency

                            if(moduleDependency.artifacts.empty && d.name == artifactId) {
                                found = true
                            }
                            else {
                                DependencyArtifact artifact = moduleDependency.artifacts.find { DependencyArtifact a ->
                                    a.name == artifactId && a.classifier == classifier && a.type == type
                                }

                                found = (artifact != null)
                            }
                        }
                        else if(d.name == artifactId) {
                            found = true
                        }
                    }

                    found
                }

                if(dependency) {
                    dependencyNode["scope"].text = "compile"
                }
            }
        }
    }
}

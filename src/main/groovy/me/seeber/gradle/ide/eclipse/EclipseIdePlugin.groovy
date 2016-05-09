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
package me.seeber.gradle.ide.eclipse

import groovy.transform.TypeChecked

import java.nio.file.Path

import me.seeber.gradle.plugin.AbstractProjectPlugin
import me.seeber.gradle.project.base.BaseProjectPlugin

import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.plugins.WarPlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel

@TypeChecked
class EclipseIdePlugin extends AbstractProjectPlugin<EclipseIdeExtension> {

    EclipseIdePlugin() {
        super("eclipseConfig", EclipseIdeExtension)
    }

    void initialize() {
        project.plugins.apply(BaseProjectPlugin)
        project.plugins.apply(EclipsePlugin)

        project.plugins.withType(WarPlugin) {
            project.plugins.apply(EclipseWtpPlugin)
        }

        EclipseIdeExtension eclipse = getConfig()

        // Prevent NPE when loading resources
        URLConnection dummyConnection = new URLConnection(new URL("file:///")) {
                    void connect() {
                        throw new IOException()
                    }
                }

        dummyConnection.defaultUseCaches = false

        EclipseIdePlugin.getResourceAsStream("org.eclipse.jdt.core.prefs").withStream { InputStream input ->
            eclipse.jdt.corePrefs = new Properties()
            eclipse.jdt.corePrefs.load(input)
        }

        EclipseIdePlugin.getResourceAsStream("org.eclipse.jdt.ui.prefs").withStream { InputStream input ->
            eclipse.jdt.uiPrefs = new Properties()
            eclipse.jdt.uiPrefs.load(input)
        }

        extension(EclipseModel).classpath.with {
            downloadSources = true
            downloadJavadoc = true
        }
    }

    void configure() {
        project.with {
            tasks.getByName("eclipseJdt").doFirst { beforeJdt() }
            tasks.getByName("eclipseClasspath").doFirst { beforeClasspath() }
            tasks.getByName("cleanEclipseJdt").doLast { afterCleanJdt() }
        }
    }

    protected void beforeJdt() {
        extension(EclipseModel).with {
            jdt?.file.withProperties { Properties properties ->
                properties.putAll(config.jdt.corePrefs)
            }
        }

        Properties prefs = new Properties(config.jdt.uiPrefs)

        this.project.file("${project.projectDir}/.settings/org.eclipse.jdt.ui.prefs").withWriter("UTF-8") { Writer out ->
            prefs.store(out, null)
        }
    }

    protected void afterCleanJdt() {
        this.project.delete(".settings/org.eclipse.jdt.ui.prefs")
    }

    protected void beforeClasspath() {
        extension(EclipseModel).classpath.file.with {
            withXml { XmlProvider xml ->
                Node classpathNode = xml.asNode()
                NodeList classpathentries = classpathNode["classpathentry"] as NodeList

                classpathentries.findAll{ it["@kind"] == "lib" }.forEach { Node classpathentry ->
                    ResolvedArtifact artifact = lookupResolvedArtifact(project.rootProject, new File(classpathentry["@path"] as String))

                    if(artifact != null) {
                        configureLibraryEntry(classpathentry, "${artifact.name}-annotations")
                    }
                }

                List<Node> jreContainers = classpathentries.findAll {
                    it["@kind"] == "con" && it["@path"].toString().startsWith("org.eclipse.jdt.launching.JRE_CONTAINER/")
                }

                jreContainers.each { Node classpathentry ->
                    configureLibraryEntry(classpathentry, "jdk-annotations")
                }

                List<Node> sourceEntries = classpathentries.findAll { it["@kind"] == "src" }

                sourceEntries.each { Node classpathentry ->
                    configureSourceEntry(classpathentry)
                }
            }
        }
    }

    protected void configureSourceEntry(Node entry) {
        String path = entry.attribute("path") as String
        Path resolvedBuildDir = project.projectDir.toPath().resolve(path)

        if(resolvedBuildDir.startsWith(project.buildDir.toPath())) {
            Node ignoreOptionalProblems = getAttributeNode(entry, "ignore_optional_problems")
            ignoreOptionalProblems.@value = "true"
        }
    }

    protected void configureLibraryEntry(Node classpathentry, CharSequence name) {
        File annotationPath = lookupAnnotationPath(project.rootProject, name)

        if(annotationPath != null) {
            File relativeAnnotationPath = project.projectDir.toPath().relativize(annotationPath.toPath()).toFile()

            logger.info "Adding annotations library '${relativeAnnotationPath}'"

            Node annotationpath = getAttributeNode(classpathentry, "annotationpath")
            annotationpath.@value = relativeAnnotationPath
        }
    }

    protected ResolvedArtifact lookupResolvedArtifact(Project rootProject, File file) {
        ResolvedArtifact artifact = null

        rootProject.allprojects.find { Project project ->
            project.configurations.find { configuration ->
                artifact = configuration.resolvedConfiguration.resolvedArtifacts.find { it.file == file }
            }
        }

        artifact
    }

    protected File lookupAnnotationPath(Project rootProject, CharSequence name) {
        ResolvedArtifact artifact = null

        rootProject.allprojects.find { Project project ->
            project.configurations.find { Configuration configuration ->
                artifact = configuration.resolvedConfiguration.resolvedArtifacts.find { ResolvedArtifact candidate ->
                    candidate.name == name && candidate.classifier == "annotations"
                }
            }
        }

        File annotationPath = artifact?.file

        if(artifact != null) {
            // If the annotation library is built by the current project, try to use the source folder instead
            ArchivePublishArtifact sourceArtifact = null

            rootProject.allprojects.find { Project project ->
                if(project.group == artifact.moduleVersion.id.group) {
                    project.configurations.find { Configuration configuration ->
                        sourceArtifact = configuration.artifacts.withType(ArchivePublishArtifact).find { ArchivePublishArtifact candidate ->
                            candidate.name == artifact.name && candidate.classifier == artifact.classifier && candidate.type == artifact.type && candidate.extension == artifact.extension
                        }
                    }
                }
            }

            if(sourceArtifact != null) {
                List<File> sourceDirectories = getSourceDirectories(sourceArtifact.archiveTask.rootSpec as DefaultCopySpec)

                if(sourceDirectories.size() == 1) {
                    annotationPath = sourceDirectories.first()
                }
            }
        }

        annotationPath
    }

    protected Node getAttributeNode(Node entry, String name) {
        Node attributes = entry.attributes[0] as Node
        attributes = entry.attributes[0] as Node ?: new Node(entry, "attributes")

        Node attribute = attributes.find { Node n -> n["@name"] == name } as Node
        attribute ?: new Node(attributes, "attribute", [name: name])
    }

    protected List<File> getSourceDirectories(CopySpecInternal copySpec) {
        List<File> directories = new ArrayList<>()

        if(copySpec instanceof DefaultCopySpec) {
            copySpec.sourcePaths.each {
                File file = copySpec.fileResolver.resolve(it)
                directories.add(file)
            }

            copySpec.children.each { CopySpecInternal child ->
                List<File> childDirectories = getSourceDirectories(child as DefaultCopySpec)
                directories += childDirectories
            }
        }

        directories
    }

}

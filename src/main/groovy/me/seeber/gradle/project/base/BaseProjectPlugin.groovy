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
package me.seeber.gradle.project.base

import groovy.transform.TypeChecked
import me.seeber.gradle.plugin.AbstractProjectPlugin
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin

import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import org.gradle.util.VersionNumber

@TypeChecked
class BaseProjectPlugin extends AbstractProjectPlugin<BaseProjectExtension> {

    BaseProjectPlugin() {
        super("projectConfig", BaseProjectExtension)
    }

    void initialize() {
        if(VersionNumber.parse(project.gradle.gradleVersion) < VersionNumber.parse("2.12")) {
            throw new GradleException("Base project plugin requires Gradle 2.12 or higher.")
        }

        BaseProjectExtension config = getConfig()

        project.with {
            // Add default repositories
            repositories.with {
                mavenLocal()
                mavenCentral()
                jcenter()
            }

            // Add and configure license plugin
            plugins.apply(LicensePlugin)

            String relativeBuildDir = this.project.relativePath(new File(this.project.buildDir, "**"))

            extension(LicenseExtension).with {
                header = file("LICENSE.txt")
                exclude(relativeBuildDir)
            }

            // Create task to update the local license file
            UpdateLicenseTask updateLicenseTask = tasks.create("licenseUpdate", UpdateLicenseTask)
            updateLicenseTask.description = "Download configured license into license file."
            updateLicenseTask.conventionMapping.with {
                map("copyrightName") { config.organization.name }
                map("copyrightYear") { config.inceptionYear }
                map("licenseUrl") { config.license.sourceUrl }
                map("licenseFile") { extension(LicenseExtension).header }
            }

            // Create task to update the README
            Copy updateReadmeTask = tasks.create("readmeUpdate", Copy)
            updateReadmeTask.with {
                description = "Update README from template"
                from "src/doc/templates"
                into project.projectDir
                include "README.template.md"
                expand(project: project)
                rename { String name ->
                    name.replace(".template.", ".")
                }
            }
        }
    }

    void complete() {
        project.with {
            // Don't cache changing modules
            configurations*.with {
                resolutionStrategy.cacheChangingModulesFor(0, "seconds")
            }

            // Remove build directories from license task
            tasks.withType(nl.javadude.gradle.plugins.license.License).each { nl.javadude.gradle.plugins.license.License license ->
                license.doFirst {
                    license.source = license.source - project.fileTree(project.buildDir)
                }
            }

            tasks.findByName("assemble")?.with {
                dependsOn(tasks.getByName("readmeUpdate"))
            }
        }
    }
}
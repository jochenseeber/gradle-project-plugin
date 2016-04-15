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
package me.seeber.gradle.plugin

import groovy.transform.TypeChecked

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@TypeChecked
abstract class AbstractProjectPlugin<E extends AbstractProjectExtension> extends AbstractProjectElement implements Plugin<Project> {

    protected Project project

    protected final String extensionName

    protected final Class<E> extensionClass

    protected final Logger logger

    AbstractProjectPlugin() {
        this(null, AbstractProjectExtension)
    }

    AbstractProjectPlugin(String extensionName, Class<E> extensionClass) {
        this.extensionName = extensionName
        this.extensionClass = extensionClass
        this.logger = Logging.getLogger(getClass())
    }

    E getConfig() {
        project.extensions.getByType(extensionClass)
    }

    void apply(Project project) {
        this.project = project

        if(extensionName != null) {
            project.extensions.create(extensionName, extensionClass, project)
        }

        initialize()

        project.afterEvaluate { configure() }

        project.gradle.projectsEvaluated  { complete() }
    }

    protected void initialize() {
    }

    protected void configure() {
    }

    protected void complete() {
    }

    Project getProject() {
        project
    }
}

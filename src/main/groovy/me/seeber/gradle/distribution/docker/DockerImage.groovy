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
package me.seeber.gradle.distribution.docker

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.CopySpec

class DockerImage implements Named {

    final Project project

    final String name

    final CopySpec copySpec = project.copySpec()

    final Set<Object> dependsOn = []

    String repository

    String tag

    DockerImage(Project project, String name) {
        this.project = project
        this.name = name
        this.tag = project.version
    }

    void from(Object from, Closure configuration) {
        copySpec.from(from, configuration);
    }

    void with(CopySpec... copySpecs) {
        copySpec.with(copySpecs)
    }

    void dependsOn(Object... tasks) {
        this.dependsOn.addAll(tasks);
    }
}
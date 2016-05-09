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

import groovy.transform.TypeChecked
import me.seeber.gradle.plugin.AbstractProjectPlugin
import me.seeber.gradle.project.base.BaseProjectPlugin

import org.gradle.api.Task
import org.gradle.api.tasks.Copy

import com.bmuschko.gradle.docker.DockerExtension
import com.bmuschko.gradle.docker.DockerRegistryCredentials
import com.bmuschko.gradle.docker.DockerRemoteApiPlugin
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage

@TypeChecked
class DockerDistributionPlugin extends AbstractProjectPlugin<DockerDistributionExtension> {

    DockerDistributionPlugin() {
        super("dockerConfig", DockerDistributionExtension)
    }

    void initialize() {
        project.plugins.apply(BaseProjectPlugin)
        project.plugins.apply(DockerRemoteApiPlugin)

        extension(DockerExtension).with {
            System.getenv()["DOCKER_HOST"]?.identity { String host ->
                url = host.replaceFirst("tcp://", "https://")
            }

            registryCredentials = new DockerRegistryCredentials()
        }
    }

    void complete() {
        extension(DockerExtension).with {
            registryCredentials.with {
                url = extension(DockerDistributionExtension).registryUrl?.toASCIIString() ?: url
                username = username ?: getPropertyValue("docker.user")
                password = password ?: getPropertyValue("docker.password")
                email = email ?: getPropertyValue("docker.email")
            }
        }

        project.with {
            config.images.all { DockerImage image ->
                Copy copyImageFilesTask = tasks.create("docker${image.name.capitalize()}CopyFiles", Copy)
                copyImageFilesTask.with {
                    description = "Copy files for Docker image '${image.name}'"
                    group = "docker"

                    into "${buildDir}/docker/${image.name}"

                    from("src/docker/${image.name}") { expand project: project }

                    with image.copySpec
                }

                DockerBuildImage buildImageTask = tasks.create("docker${image.name.capitalize()}Build", DockerBuildImage)
                buildImageTask.with {
                    description = "Build Docker image '${image.name}'"
                    group = "docker"

                    tag = "${image.repository}:${image.tag}"
                    inputDir = file("${buildDir}/docker/${image.name}")
                    pull = config.pull

                    dependsOn copyImageFilesTask
                }

                DockerPushImage pushImageTask = tasks.create("docker${image.name.capitalize()}Push", DockerPushImage)
                pushImageTask.with {
                    description = "Push Docker image '${image.name}'"
                    group = "docker"

                    imageName = image.repository
                    tag = image.tag

                    dependsOn buildImageTask
                }
            }

            config.images.all { DockerImage image ->
                Task copyImageFilesTask = tasks.getByName("docker${image.name.capitalize()}CopyFiles")
                copyImageFilesTask.dependsOn(image.dependsOn)
            }
        }
    }
}

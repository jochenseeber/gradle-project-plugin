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

import java.net.URI

import groovy.lang.Closure
import groovy.transform.InheritConstructors
import groovy.transform.TypeChecked
import me.seeber.gradle.plugin.AbstractProjectExtension;

@TypeChecked
@InheritConstructors
class BaseProjectExtension extends AbstractProjectExtension {

    Integer inceptionYear

    URI websiteUrl

    Organization organization

    License license

    Repository repository

    IssueTracker issueTracker

    Release release

    License getLicense() {
        license = license ?: new License()
    }

    void license(Closure configure) {
        this.project.configure(getLicense(), configure)
    }

    Repository getRepository() {
        repository = repository ?: new Repository()
    }

    void repository(Closure configure) {
        this.project.configure(getRepository(), configure)
    }

    IssueTracker getIssueTracker() {
        issueTracker = issueTracker ?: new IssueTracker()
    }

    void issueTracker(Closure configure) {
        this.project.configure(getIssueTracker(), configure)
    }

    Release getRelease() {
        release = release ?: new Release()
    }

    void release(Closure configure) {
        this.project.configure(getRelease(), configure)
    }

    Organization getOrganization() {
        organization = organization ?: new Organization()
    }

    void organization(Closure configure) {
        this.project.configure(getOrganization(), configure)
    }
}

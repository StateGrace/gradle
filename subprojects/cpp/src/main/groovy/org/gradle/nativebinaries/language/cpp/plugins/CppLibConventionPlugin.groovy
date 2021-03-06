/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativebinaries.language.cpp.plugins

import org.gradle.api.Incubating
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A convention-based plugin that automatically adds a single C++ source set named "main" and wires it into a {@link org.gradle.nativebinaries.Library} named "main".
 */
@Incubating
class CppLibConventionPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.plugins.apply(CppPlugin)

        project.with {
            libraries {
                main {
                    baseName = project.name
                }
            }
        }
    }

}
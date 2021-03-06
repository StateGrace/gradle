/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.nativebinaries.language.cpp

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.nativebinaries.language.cpp.fixtures.AvailableToolChains
import org.gradle.nativebinaries.language.cpp.fixtures.AvailableToolChains.ToolChainCandidate
import org.gradle.nativebinaries.language.cpp.fixtures.ExecutableFixture
import org.gradle.nativebinaries.language.cpp.fixtures.RequiresInstalledToolChain
import org.gradle.nativebinaries.language.cpp.fixtures.app.CppCallingCHelloWorldApp

import static org.gradle.nativebinaries.language.cpp.fixtures.ToolChainRequirement.Gcc

@RequiresInstalledToolChain(Gcc)
class GccToolChainCustomisationIntegrationTest extends AbstractIntegrationSpec {
    def AvailableToolChains.InstalledToolChain gcc
    def helloWorldApp = new CppCallingCHelloWorldApp()

    def setup() {
        gcc = findGcc()

        buildFile << """
            apply plugin: 'cpp'
            apply plugin: 'c'

            model {
                toolChains {
                    ${gcc.buildScriptConfig}
                }
            }

            executables {
                main {
                    binaries.all {
                        lib libraries.hello.static
                    }
                }
            }
            libraries {
                hello {}
            }
"""

        helloWorldApp.executable.writeSources(file("src/main"))
        helloWorldApp.library.writeSources(file("src/hello"))
    }

    def findGcc() {
        for (ToolChainCandidate candidate : AvailableToolChains.toolChains) {
            if (candidate instanceof AvailableToolChains.InstalledGcc) {
                return candidate
            }
        }
        throw new IllegalStateException("No GCC found")
    }

    def ExecutableFixture executable(Object path) {
        return gcc.executable(file(path))
    }

    def "can add binary configuration to target a platform"() {
        when:
        buildFile << """
            model {
                toolChains {
                    ${gcc.id} {
                        addPlatformConfiguration(new ArmArchitecture())
                    }
                }
                platforms {
                    arm {
                        architecture "arm"
                    }
                    i386 {
                        architecture "i386"
                    }
                }
            }

            class ArmArchitecture implements TargetPlatformConfiguration {
                boolean supportsPlatform(Platform element) {
                    return element.getArchitecture().name == "arm"
                }

                List<String> getCppCompilerArgs() {
                    ["-m32", "-DFRENCH"]
                }

                List<String> getCCompilerArgs() {
                    ["-m32", "-DFRENCH"]
                }

                List<String> getObjectiveCCompilerArgs() {
                    []
                }

                List<String> getObjectiveCppCompilerArgs() {
                    []
                }

                List<String> getAssemblerArgs() {
                    []
                }

                List<String> getLinkerArgs() {
                    ["-m32"]
                }

                List<String> getStaticLibraryArchiverArgs() {
                    []
                }
            }
"""

        and:
        succeeds "armMainExecutable", "i386MainExecutable"

        then:
        executable("build/binaries/mainExecutable/arm/main").binaryInfo.arch.name == "x86"
        executable("build/binaries/mainExecutable/arm/main").exec().out == helloWorldApp.frenchOutput

        executable("build/binaries/mainExecutable/i386/main").binaryInfo.arch.name == "x86"
        executable("build/binaries/mainExecutable/i386/main").exec().out == helloWorldApp.englishOutput
    }

    def "can add action to tool chain that modifies tool arguments prior to execution"() {
        when:
        buildFile << """
            model {
                toolChains {
                    ${gcc.id} {
                        cppCompiler.withArguments { args ->
                            Collections.replaceAll(args, "CUSTOM", "-O3")
                        }
                        CCompiler.withArguments { args ->
                            Collections.replaceAll(args, "CUSTOM", "-O3")
                        }
                        linker.withArguments { args ->
                            int customIndex = args.indexOf("CUSTOM")
                            if (customIndex >= 1) {
                                // Remove "-Xlinker" "CUSTOM"
                                args.remove(customIndex)
                                args.remove(customIndex - 1)
                            }
                        }
                        staticLibArchiver.withArguments { args ->
                            args.remove "CUSTOM"
                        }
                    }
                }
            }
            binaries.all {
                cppCompiler.args "CUSTOM"
                cCompiler.args "CUSTOM"
                linker.args "CUSTOM"
            }
            binaries.withType(StaticLibraryBinary) {
                staticLibArchiver.args "CUSTOM"
            }
"""
        then:
        succeeds "mainExecutable"

        then:
        executable("build/binaries/mainExecutable/main").exec().out == helloWorldApp.englishOutput
    }
}

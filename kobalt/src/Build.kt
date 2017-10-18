import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.project

val hm = project {
    name = "Hindley-Milner"
    group = "io.github.aedans"
    artifactId = "hm"
    version = "0.1"

    dependencies {
        compile("org.jetbrains.kotlin:kotlin-runtime:1.1.2")
        compile("org.jetbrains.kotlin:kotlin-stdlib:1.1.2")
        compile("com.github.h0tk3y.betterParse:better-parse:0.2.1")
    }

    assemble {
        jar {
        }
    }
}

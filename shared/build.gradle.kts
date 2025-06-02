import org.apache.avro.Schema
import org.apache.avro.compiler.specific.SpecificCompiler

plugins {
    kotlin("jvm") version "2.0.21"
    id("java-library")
}

group = "uk.ac.york.gpig.teamb"

repositories {
    mavenCentral()
}

buildscript{
    repositories {
        mavenCentral()
    }
    dependencies{
        classpath("org.apache.avro:avro-compiler:1.12.0")
    }
}

dependencies {
    implementation("org.apache.avro:avro-compiler:1.12.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

fun generateAvroPojos(){
    val schemaFiles = File("shared/src/main/resources/avro").listFiles().filter{file -> file.extension == "avsc"}
    schemaFiles.forEach {
        val schema = Schema.Parser().parse(it)
        val compiler = SpecificCompiler(schema)
        compiler.compileToDestination(null, File("shared/build/generated/avro"))
    }
}

tasks.register("avroGen"){
    doFirst {
        // clean up old generated code
        project.file("build/generated/avro").deleteRecursively()
        // The avro library will fail if the file does not already exist
        project.file("build/generated/avro/uk/ac/york/gpig/teamb").mkdirs()
    }
    doLast{
        generateAvroPojos()
    }
}

sourceSets {
    main{
        java{
            srcDir("build/generated/avro")
        }
        kotlin{
            srcDir("src/main")
        }
    }
}
# Trident Compound

A set of shadable tools to reduce the boilerplate needed when developing mods for Minecraft using the MinecraftForge
API.

### For users

If you've gotten an error mentioniong Compound when running your game first check what mod was referencing Compound and
create an issue on that mod. If the issue is determined to be with Compound and not the mod using Compound then the
developer can make an issue on this repo.

### For developers

To add compound to your project add a reference to a repo that contains the library, the most up-to-date version will
always be available on the trident repo.

To add the trident repo to your project add the following to your build.gradle

```gradle
repositories {
    maven {
        url 'https://repo.tridevmc.com/'
    }
}
```

To add a Compound module to your project add a dependency with the given format

```gradle
compile "com.tridevmc.compound:compound-${compoundModule}:${compoundVersion}"
```

Although not strictly required, it's recommended that you shade compound into your mod jar and relocate it to another
package in your jar. This prevents conflicts if there's more than one mod that depends on Compound in a pack.

To add ShadowJar to your project insert the following in your buildscript

```gradle
plugins {
    id 'com.github.johnrengelman.shadow' version '4.0.4'
}
```

Then to tell ShadowJar to shade compound into your output jar add the following

```gradle
shadowJar {
    classifier = ""

    dependencies {
        include(dependency("com.tridevmc.compound:compound-${compoundModule}:${compoundVersion}"))
    }

    relocate "com.tridevmc.compound", "my.mod.package.compound"
}
```
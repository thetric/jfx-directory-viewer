# jfx-directory-explorer

[![Travis](https://img.shields.io/travis/thetric/jfx-directory-explorer.svg?style=flat-square)](https://travis-ci.org/thetric/jfx-directory-explorer)

Provides a JavaFX component displaying the files and directories of a directory as a list.
The view is updated automatically if something in the directory has changed.

## Motivation
Sadly JavaFX does not provide such a component out of the box.
Although JFX provides file and directory choosers you cannot embed them into your application.
This little library wants to change this!

## Add as dependency

You can get the library via [JitPack.io](https://jitpack.io/#thetric/jfx-directory-explorer).

```gradle
// add JitPack.io repository
repositories {
    maven { url 'https://jitpack.io' }
}

// add dependency
dependencies {
    compile 'com.github.thetric:jfx-directory-explorer:1.0'
}
```

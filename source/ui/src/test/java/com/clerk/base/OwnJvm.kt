package com.clerk.base

/**
 * The test classes that each run in a JVM of their own. Paparazzi's layoutlib fails to start in a
 * JVM where Robolectric ran, and one Paparazzi class fails beside another, so neither kind of class
 * shares a JVM.
 */
interface OwnJvm

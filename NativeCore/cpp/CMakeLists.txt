cmake_minimum_required(VERSION 3.22)
project(clerk_quickjs C)
set(CMAKE_C_STANDARD 11)
add_library(clerk_quickjs SHARED clerk_quickjs.c quickjs/quickjs.c quickjs/dtoa.c quickjs/libregexp.c quickjs/libunicode.c)
target_include_directories(clerk_quickjs PRIVATE quickjs)
target_compile_definitions(clerk_quickjs PRIVATE _GNU_SOURCE)
target_compile_options(clerk_quickjs PRIVATE -fvisibility=hidden -fstack-protector-strong)
if(ANDROID)
  # Keep debug metadata (and the resulting ELF build ID) independent of checkout paths.
  target_compile_options(clerk_quickjs PRIVATE
    "-ffile-prefix-map=${CMAKE_SOURCE_DIR}=clerk-native-core"
    "-fdebug-prefix-map=${CMAKE_BINARY_DIR}=clerk-native-core-build"
    "-fdebug-prefix-map=${CMAKE_ANDROID_NDK}=android-ndk")
  target_link_libraries(clerk_quickjs m dl)
  target_link_options(clerk_quickjs PRIVATE -Wl,-z,max-page-size=16384)
else()
  find_package(JNI REQUIRED)
  target_include_directories(clerk_quickjs PRIVATE ${JNI_INCLUDE_DIRS})
  target_link_libraries(clerk_quickjs m)
endif()

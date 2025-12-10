cmake_minimum_required(VERSION 3.22.1)

project("helldeck_llama")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fvisibility=hidden")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")

find_library(log-lib log)
find_library(android-lib android)

set(NATIVE_SOURCES app/src/main/cpp/llama_jni.cpp)

# If llama.cpp is present, include it; else build a stub that still loads
if(EXISTS ${CMAKE_SOURCE_DIR}/third_party/llama.cpp)
    add_subdirectory(${CMAKE_SOURCE_DIR}/third_party/llama.cpp ${CMAKE_BINARY_DIR}/llama_build)
    add_library(helldeck_llama SHARED ${NATIVE_SOURCES})
    target_include_directories(helldeck_llama PRIVATE app/src/main/cpp ${CMAKE_SOURCE_DIR}/third_party/llama.cpp)
    target_link_libraries(helldeck_llama llama ${log-lib} ${android-lib})
    target_compile_definitions(helldeck_llama PRIVATE HAVE_LLAMA_CPP=1)
else()
    add_library(helldeck_llama SHARED ${NATIVE_SOURCES})
    target_include_directories(helldeck_llama PRIVATE app/src/main/cpp)
    target_link_libraries(helldeck_llama ${log-lib} ${android-lib})
endif()

set_target_properties(helldeck_llama PROPERTIES LIBRARY_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")

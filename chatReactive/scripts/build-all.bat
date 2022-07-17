@REM SBT runner script
call .\sbt.bat gateway/docker:publishLocal;userservice/docker:publishLocal;messageroom/docker:publishLocal;messageregistry/docker:publishLocal
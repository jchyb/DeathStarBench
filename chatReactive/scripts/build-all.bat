@REM SBT runner script
call .\sbt.bat gateway/docker:publishLocal
call .\sbt.bat userservice/docker:publishLocal
call .\sbt.bat messageroom/docker:publishLocal
call .\sbt.bat messageregistry/docker:publishLocal
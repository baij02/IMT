@set MSBUILD_PATH=C:\Program Files (x86)\Microsoft Visual Studio\2017\Community\MSBuild\15.0\Bin
@set JDK_PATH=c:\Program Files\Java\jdk1.8.0_191\bin
@set FIJI_PATH=c:\Fiji.app

@mkdir ..\Binaries
"%JDK_PATH%\javac" -cp "%FIJI_PATH%\jars\*" -d ..\Binaries IMTLaucher\*.java


@pause
@echo off
setlocal enabledelayedexpansion

set paramCount=0
for %%x in (%*) do (
   set /A paramCount+=1
   set "param[!paramCount!]=%%~x"
)

 SET BUILD_TYPE=%param[1]%
 SET IS_LIB=%param[2]%
 SET PRODUCT_FLAVOR=%param[3]%
 SET PRODUCT_NAME=%param[4]%
 SET TRANSFER_MODE=%param[5]%
 SET AUTHENTICATION=%param[6]%
 SET SERVER_URL=%param[7]%
 SET LOGGING=%param[8]%
 SET CUSTOMER=%param[9]%
 SET CERTIFICATION=%param[10]%
 SET ESTIMATION_TIME=%param[11]%
 SET CLOUD_PAIRING=%param[12]%
 SET DYNAMIC_ESTIMATION=%param[13]%
 SET VERSION_NAME=%param[14]%

if "%BUILD_TYPE%" == "clean" (
  goto clean
)
if "%BUILD_TYPE%" == "help" (
  goto help
)  
if "%BUILD_TYPE%" == "" (
  goto help
)
if "%PRODUCT_NAME%" == "" (
  goto help
)
if "%TRANSFER_MODE%" == "" (
  goto help
)
if "%AUTHENTICATION%" == "" (
  goto help
)
if "%SERVER_URL%" == "" (
  goto help
)
if "%LOGGING%" == "" (
  goto help
)
if "%CUSTOMER%" == "" (
  goto help
)
if "%VERSION_NAME%" == "" (
  goto help
)
if "%CERTIFICATION%" == "" (
  goto help
)
if "%ESTIMATION_TIME%" == "" (
  goto help
)
if "%CLOUD_PAIRING%" == "" (
  goto help
)
if "%DYNAMIC_ESTIMATION%" == "" (
  goto help
)
if "%IS_LIB%" == "" (
  goto help
)

 echo ##### PRODUCT-%PRODUCT_NAME%#####
 SET APK_NAME=%PRODUCT_NAME%-%TRANSFER_MODE%-%CUSTOMER%-%VERSION_NAME%.apk
 SET AAR_NAME=wirelessdeviceswitch.aar


 call generate_config.bat %PRODUCT_NAME% %TRANSFER_MODE% %AUTHENTICATION% %SERVER_URL% %LOGGING% %CUSTOMER% %CERTIFICATION% %ESTIMATION_TIME% %CLOUD_PAIRING% %DYNAMIC_ESTIMATION%
 copy /Y  .\temp_config.xml .\app\src\main\assets\config.xml > NUL

goto build
  
:clean
echo ##### Cleaning Build #####
call gradlew clean
goto end
 
:build 

 if "%IS_LIB%" == "yes" (
    echo ##### Preparing %PRODUCT_NAME% lib  #####
	copy /Y  .\app\build_lib.gradle .\app\build.gradle  > NUL
	copy /Y  .\app\AndroidManifest_lib.xml .\app\src\main\AndroidManifest.xml  > NUL
 )
  
 if "%BUILD_TYPE%" == "debug" (
    if "%NO_CLEAN%" == "" (
    echo ##### cleaning build for %PRODUCT_NAME% #####
    call gradlew clean
	)
    echo ##### Building %BUILD_TYPE% build For %PRODUCT_NAME% #####
    call gradlew assemble%PRODUCT_FLAVOR%Debug
	if "%IS_LIB%" == "yes" (
          ren .\app\build\outputs\aar\app.aar %AAR_NAME%
    ) else (
          ren .\app\build\outputs\apk\app-%PRODUCT_FLAVOR%-release-unsigned.apk %APK_NAME%
    )
	goto end
 ) 
 
 if "%BUILD_TYPE%" == "release" (
    if "%NO_CLEAN%" == "" (
      echo ##### cleaning build for %PRODUCT_NAME% #####
      call gradlew clean
	)
    echo ##### Building %BUILD_TYPE% build For %PRODUCT_NAME% #####
    call gradlew assemble%PRODUCT_FLAVOR%Release
	::ren .\app\build\outputs\apk\app-%PRODUCT_FLAVOR%-release-unsigned.apk %APK_NAME%
	goto end
 )
 
:help
echo ---------------------------------------------------------------------------------------------------------------
echo USAGE: make [Build Type] [is lib] [Product Flavour ][Product Name] [Transfer Mode][Authentication][Server URL][LOGGING][Customer][CERTIFICATION CHECK][Estimation Time][CLOUD PAIRING][DYNAMIC ESTIMATION][Version Number][is lib]
echo Ex: make debug obfuscation playstore WDS WDIRECT false https://wds.pervacioone.com true Pervacio false true true false 1.0.0 no
echo ---------------------------------------------------------------------------------------------------------------
echo [debug, release]                		Indicates the type of the build
echo [yes, no]			                    Indicates to build APK or Lib
echo [pervacio,sprint]                		Indicates the product flavour
echo [WDS,MMDS]                			    Indicates the name of the product
echo [WLAN,WDIRECT]                			Indicates data transfer mode
echo [true,false]                			Indicates Store Authentication required or not.
echo [SERVER URL]                	        Indicates server url for logging and Authentication.
echo [true,false]                			Indicates Database logging required or not.
echo [Customer Name Ex: Pervacio]           Indicates Customer Name.
echo [true,false]                           Indicates Certification check required or not.
echo [true,false]                           Indicates Estimation time check required or not.
echo [true,false]                           Indicates Cloud pairing required or not.
echo [true,false]                           Indicates Dynamic Estimation required or not.
echo [Apk Version, Ex: V1.0.0]			    Indicates the version name
echo ---------------------------------------------------------------------------------------------------------------
:end


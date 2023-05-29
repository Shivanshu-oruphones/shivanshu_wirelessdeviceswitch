@echo off
setlocal enabledelayedexpansion

set paramCount=0
for %%x in (%*) do (
   set /A paramCount+=1
   set "param[!paramCount!]=%%~x"
)

SET PRODUCT=%param[1]%
SET TRANSFER_MODE=%param[2]%
SET AUTHENTICATION=%param[3]%
SET SERVER_URL=%param[4]%
SET LOGGING=%param[5]%
SET CUSTOMER=%param[6]%
SET CERTIFICATION=%param[7]%
SET ESTIMATION=%param[8]%
SET CLOUD_PAIRING=%param[9]%
SET DYNAMIC_ESTIMATION = %param[10]%

echo ^<?xml version="1.0" encoding="utf-8"?^> > temp_config.xml
echo ^<product-config^> >> temp_config.xml
echo    ^<product^>%PRODUCT%^</product^> >> temp_config.xml
echo    ^<transfer_mode^>%TRANSFER_MODE%^</transfer_mode^> >> temp_config.xml
echo    ^<authentication_required^>%AUTHENTICATION%^</authentication_required^> >> temp_config.xml
echo    ^<server_url^>%SERVER_URL%^</server_url^> >> temp_config.xml
echo    ^<transaction_logging^>%LOGGING%^</transaction_logging^> >> temp_config.xml
echo    ^<customer^>%CUSTOMER%^</customer^> >> temp_config.xml
echo    ^<certification_required^>%CERTIFICATION%^</certification_required^> >> temp_config.xml
echo    ^<estimation_required^>%ESTIMATION%^</estimation_required^> >> temp_config.xml
echo    ^<cloud_pairing^>%CLOUD_PAIRING%^</cloud_pairing^> >> temp_config.xml
echo    ^<dynamic_estimation^>%DYNAMIC_ESTIMATION%^</dynamic_estimation^> >> temp_config.xml
echo ^</product-config^> >> temp_config.xml
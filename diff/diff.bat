@echo off
setlocal enabledelayedexpansion

:: List of commits
set commits=4393192

:: Counter for numbering files
set /a count=1

:: Clear old id.txt if exists
del id.txt 2>nul

for %%a in (%commits%) do (
   set filename=!count!.txt

   :: Save commit diff in numbered file
   echo -------------------------------------------------- > !filename!
   echo COMMIT ID: %%a >> !filename!
   echo -------------------------------------------------- >> !filename!
   curl -s -L https://github.com/am-abdulmueed/PluginStream/commit/%%a.diff >> !filename!

   :: Reset message variable
   set msg=

   :: Fetch commit message (first line only)
   for /f "tokens=* usebackq" %%m in (`curl -s https://api.github.com/repos/recloudstream/cloudstream/commits/%%a ^| findstr /i "\"message\""`) do (
      if not defined msg (
         set msg=%%m
         set msg=!msg:"=!
         set msg=!msg:message=!
         set msg=!msg: ,=!
         set msg=!msg:  =!
      )
   )

   :: Write mapping with commit message + hash + filename
   echo !msg! ^| %%a = !filename! >> id.txt

   set /a count+=1
)

echo Done! Check 1.txt, 2.txt... and id.txt for clean mapping.
pause

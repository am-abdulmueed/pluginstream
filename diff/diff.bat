@echo off
setlocal enabledelayedexpansion

:: List of commits
set commits=3908d96 25c295e 1dcb795 1769aee 77b4a08 77b4a08 1bb603d b1dd88b c74a3f1 f236df2 55a3f71 c75caf6 fa0ec97 d0a7860 d2a187d a025f1a d191245 68ac142 55b43eb 9403f51 8edea2f 25e18eb b387087 2f01b9f 87db46e f4f21b8 b3ab413 37f701d 144c1fe

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
   curl -s -L https://github.com/prateek-chaubey/YTPro/commit/%%a.diff >> !filename!

   :: Reset message variable
   set msg=

   :: Fetch commit message (first line only)
   for /f "tokens=* usebackq" %%m in (`curl -s https://api.github.com/repos/prateek-chaubey/YTPro/commits/%%a ^| findstr /i "\"message\""`) do (
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

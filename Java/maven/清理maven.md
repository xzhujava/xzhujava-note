maven清理本地仓库(本地有依赖，idea加载失败)
--

### 1、注意事项和位置：
如图:
![位置](image/maven.png)
* .bat文件要和仓库在同一平级目录
* REPOSITORY_PATH要改成你自己仓库的地址
### 2、删除.lastUpdated文件(失败的jar包
使用.bat文件

注明：REPOSITORY_PATH=D:\apache-maven-3.6.3\repository\mavenRepo 改成你仓库的地址
```shell
set REPOSITORY_PATH=D:\software\Java\maven\repository
for /f "delims=" %%i in ('dir /b /s "%REPOSITORY_PATH%\*lastUpdated*"') do (
    del /s /q %%i
)
pause
```
***
### 3、删除_remote.repositories文件
```shell
set REPOSITORY_PATH=D:\software\Java\maven\repository
rem 正在搜索...
for /f "delims=" %%i in ('dir /b /s "%REPOSITORY_PATH%\*_remote.repositories*"') do (
    del /s /q %%i
)
rem 搜索完毕
pause
```
***说明：如果你私服里面没有jar包。本地仓库有_remote.repositories文件，idea无法加载你本地jar包会出现。maven依赖爆红。找不到本地jar包***
***
### 4、删除_maven.repositories文件(没影响可留)
```shell
set REPOSITORY_PATH=D:\software\Java\maven\repository
rem 正在搜索...
for /f "delims=" %%i in ('dir /b /s "%REPOSITORY_PATH%\*_maven.repositories*"') do (
    del /s /q %%i
)
rem 搜索完毕
pause
```
***
***以上内容亲测实用，使用时一定修改REPOSITORY_PATH为自己仓库地址***
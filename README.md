# TransProxy3

## About

This is the transparent proxy app [posted on XDA](http://forum.xda-developers.com/showthread.php?t=766569) 
by user "daveba".

Its simply here in a git repo for ease of future development.

## Redsocks

This app depends on [redsocks]() so below are the steps required to recompile the redsocks library and libevent that 
redsocks in turn depends on.

### Compiling Redsocks

A precompiled binary ships with the example app "Transproxy3" **but** its ARM binary only, so need to recompile redsocks 
ourselves to have a x86 version for example or to just get a newer version.

**Steps:**

```
1. git clone https://github.com/darkk/redsocks.git
2. ## add fetch = +refs/pull/*/head:refs/remotes/origin/pr/* to .git/config as per https://help.github.com/articles/checking-out-pull-requests-locally
3. git fetch origin
4. git checkout pr/20
5. cd jni/libevent
6. ./configure
7. cd ../../
8. export PATH=$PATH:/path/to-nadroid-ndk
9. ndk-build
```

## License 

Anyone is given full permission to dissect, fix, reuse anything and include it in their ROM as required.

_(as per original developer "daveba")_

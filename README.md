SafHelper
============

Open source Android library for files writing on SD card.

## Features

SafHelper using Storage Access Framework (SAF) to retrieve permission, create directories and write files on SD card.

## Integration

Add dependency in build.gradle:
```groovy
compile 'ru.bartwell:safhelper:1.0.0'
```

## Usage
```kotlin
private lateinit var safHelper: SafHelper

private fun onButtonClick() {
    writeFile(true)
}

private fun writeFile(requestPermissions: Boolean) {
    val userFile = File(main_path.text.toString())

    if (userFile.parent == null) {
        showToast("Wrong path")
    } else {
        safHelper = SafHelper(this, userFile.parent)
        if (safHelper.isApplicable()) {
            if (safHelper.isPermissionGranted()) {
                try {
                    if (safHelper.mkdirs(userFile.parent)) {
                        val outputStream = safHelper.createFile(userFile.path)
                        outputStream.use {
                            it.write("Text in the file".toByteArray())
                        }
                        showToast("Success")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Error: " + e.message)
                }
            } else {
                if (requestPermissions) {
                    safHelper.requestPermissions(this)
                } else {
                    showToast("Permissions not granted")
                }
            }
        } else {
            showToast("File is not on SD Card or Android version < 5.0")
        }
    }
}

override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
    if (safHelper.onActivityResult(this, requestCode, resultCode, resultData)) {
        writeFile(false)
    }
}
```

## License

Copyright © 2017 Artem Bazhanov

Ultra Debugger is provided under an Apache 2.0 License.

Ultra Debugger uses [NanoHttpd](https://github.com/NanoHttpd/nanohttpd) to serve HTTP requests (NanoHttpd Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias All rights reserved).
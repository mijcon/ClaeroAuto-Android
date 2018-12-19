package com.myclaero.claeroauto

import android.app.Application
import com.parse.Parse
import com.parse.ParseACL

class ParseStarter : Application() {

    override fun onCreate() {
        super.onCreate()

        Parse.enableLocalDatastore(this)

        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId("fd50b9c25169e32ec8c84c53731561d5342d9081")
                .server("https://parse.myclaero.com/parse/")
                .build()
        )

        val userACL = ParseACL()
        userACL.publicReadAccess = false
        userACL.publicWriteAccess = false
        ParseACL.setDefaultACL(userACL, true)
    }
}
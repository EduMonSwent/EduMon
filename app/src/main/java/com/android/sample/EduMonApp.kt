package com.android.sample

import android.app.Application
import android.content.Context
class EduMonApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}

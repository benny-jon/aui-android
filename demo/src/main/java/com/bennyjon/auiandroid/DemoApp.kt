package com.bennyjon.auiandroid

import android.app.Application

class DemoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DemoServiceLocator.init(this)
    }
}

package io.github.jixiaoyong.wanandroid

import android.app.Application
import cf.android666.applibrary.Logger
import cf.android666.applibrary.view.Toast
import io.github.jixiaoyong.wanandroid.utils.DatabaseUtils
import io.github.jixiaoyong.wanandroid.utils.NetUtils

/**
 * author: jixiaoyong
 * email: jixiaoyong1995@gmail.com
 * website: https://jixiaoyong.github.io
 * date: 2019-11-05
 * description: todo
 */
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.isLog = BuildConfig.DEBUG

        DatabaseUtils.initDatabase(this)
        Toast.init(this)
        NetUtils.init(this)
    }
}
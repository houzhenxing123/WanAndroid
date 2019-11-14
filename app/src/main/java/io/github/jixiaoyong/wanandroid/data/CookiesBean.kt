package io.github.jixiaoyong.wanandroid.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * author: jixiaoyong
 * email: jixiaoyong1995@gmail.com
 * website: https://jixiaoyong.github.io
 * date: 2019-11-14
 * description: todo
 */
@Entity(tableName = "cookies")
data class CookiesBean(
        var hostName: String? = "",
        var cookiesStr: String? = "",
        @PrimaryKey(autoGenerate = true)
        var _id: Int? = null
)
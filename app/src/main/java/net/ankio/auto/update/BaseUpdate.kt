/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.update

import android.app.Activity
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.ankio.auto.R
import net.ankio.auto.request.RequestsUtils
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.Setting
import org.markdownj.MarkdownProcessor
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

abstract class BaseUpdate(context: Activity) {
    protected val url = "https://dl.ez-book.org/自动记账"
    abstract val repo: String
    var download = ""
    open var github = ""


    abstract val dir: String

    fun pan(): String {
        return "$url/$dir"
    }

    protected fun switchGithub(uri: String): String {
        ConfigUtils.getString(
            Setting.UPDATE_CHANNEL,
            UpdateChannel.GithubRaw.name
        ).let {
            return when (it) {
                //UpdateChannel.GithubRaw.name,UpdateChannel.Github.name -> "https://github.com/$uri"
                // https://ghproxy.net/https://github.com/AutoAccountingOrg/AutoRule/releases/download/v0.3.9/v0.3.9.zip
                UpdateChannel.GithubProxy.name -> "https://ghproxy.net/https://github.com/$uri"
                // https://cf.ghproxy.cc/https://github.com/AutoAccountingOrg/AutoRule/releases/download/v0.3.9/v0.3.9.zip
                UpdateChannel.GithubProxy2.name -> " https://cf.ghproxy.cc/https://github.com/$uri"
                //https://hub.gitmirror.com/https://github.com/AutoAccountingOrg/AutoRule/releases/download/v0.3.9/v0.3.9.zip
                UpdateChannel.GithubMirror.name -> "https://hub.gitmirror.com/https://github.com/$uri"
                //https://dl.ghpig.top/https://github.com/AutoAccountingOrg/AutoRule/releases/download/v0.3.9/v0.3.9.zip
                //https://dgithub.xyz/AutoAccountingOrg/AutoRule/releases/download/v0.3.9/v0.3.9.zip
                UpdateChannel.GithubD.name -> "https://dgithub.xyz/$uri"
                //https://kkgithub.com/AutoAccountingOrg/AutoRule/releases/download/v0.3.9/v0.3.9.zip
                UpdateChannel.GithubKK.name -> "https://kkgithub.com/$uri"
                else -> "https://github.com/$uri"
            }
        }
    }

    var version = ""
    var log = ""
    var date = ""
    protected val request = RequestsUtils(context, 5)
    abstract fun ruleVersion(): String
    abstract fun onCheckedUpdate()

    suspend fun check(showToast: Boolean = false): Boolean {
        Logger.d("检查更新中")

        val updateChannel = ConfigUtils.getString(
            Setting.UPDATE_CHANNEL,
            UpdateChannel.GithubRaw.name
        )

        // 如果是本地导入，直接返回 false，不需要检查版本
        if (updateChannel == UpdateChannel.Local.name) {
            if (showToast) {
                ToastUtils.info(R.string.no_need_to_update)
            }
            return false
        }

        val list = when (updateChannel) {
            UpdateChannel.Cloud.name -> checkVersionFromPan(ruleVersion())
            else -> checkVersionFromGithub(ruleVersion())
        }
        
        // val list = if (ConfigUtils.getString(
        //         Setting.UPDATE_CHANNEL,
        //         UpdateChannel.GithubRaw.name
        //     ) !== UpdateChannel.Cloud.name
        // ) {
        //     checkVersionFromGithub(ruleVersion())
        // } else {
        //     checkVersionFromPan(ruleVersion())
        // }

        version = list[0]
        log = list[1]
        date = list[2]


        return if (version != "") {

            Logger.i("版本: $version")
            Logger.i("日志: $log")
            Logger.i("更新时间: $date")
            onCheckedUpdate()
            true
        } else {
            if (showToast) {
                ToastUtils.info(R.string.no_need_to_update)
            }
            false
        }

    }

    /**
     * 从网盘检查版本
     */
    private suspend fun checkVersionFromPan(localVersion: String): Array<String> {
        try {
            request.get("${pan()}/index.json").let {
                val json = Gson().fromJson(it.second, JsonObject::class.java)
                version = json.get("version").asString
                log = MarkdownProcessor().markdown(json.get("log").asString)
                date = json.get("date").asString
                date = date(date)
                if (checkVersionLarge(localVersion, version)) {
                    return arrayOf(
                        version,
                        log,
                        date
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e("从网盘检查更新失败", e)
        }
        return arrayOf("", "", "")
    }

    /**
     * 从github检查版本
     */
    open suspend fun checkVersionFromGithub(localVersion: String): Array<String> {

        try {
            request.get(github).let {
                val json = Gson().fromJson(it.second, JsonObject::class.java)
                version = json.get("tag_name").asString
                log = json.get("body").asString
                date = json.get("published_at").asString

                //转换日期
                date = date(date)


                val processor = MarkdownProcessor()

                // 转换 Markdown 为 HTML
                log = processor.markdown(log)

                Logger.d("本地版本: $localVersion,云端版本: $version")

                if (checkVersionLarge(localVersion, version)) {
                    return arrayOf(
                        version,
                        log,
                        date
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e("从Github检查更新失败", e)
        }
        return arrayOf("", "", "")
    }

    abstract suspend fun update(finish: () -> Unit)


    fun date(date: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outputFormatter = DateFormat.getDateTimeInstance(
                DateFormat.DEFAULT,
                DateFormat.DEFAULT,
                Locale.getDefault()
            ).apply {
                timeZone = TimeZone.getDefault()
            }

            val parsedDate = formatter.parse(date)
            if (parsedDate != null) {
                outputFormatter.format(parsedDate)
            } else {
                date
            }
        } catch (e: Exception) {
            Logger.e("日期转换失败", e)
            date
        }
    }

    /**
     * 检查两个版本号哪个大
     * @param localVersion 本地版本号
     * @param cloudVersion 云端版本号
     */
    private fun checkVersionLarge(localVersion: String, cloudVersion: String): Boolean {
        val channel = ConfigUtils.getString(
            Setting.CHECK_UPDATE_TYPE,
            UpdateType.Stable.name
        )
        val localParts = localVersion.replace("-${channel}", "").replace("_", "").split(".")
        val cloudParts = cloudVersion.replace("-${channel}", "").replace("_", "").split(".")
        // 找出较长的版本号长度，补齐较短版本号的空位
        val maxLength = maxOf(localParts.size, cloudParts.size)

        for (i in 0 until maxLength) {
            val localPart = localParts.getOrNull(i)?.toLongOrNull() ?: 0  // 如果某个部分不存在，默认视为0
            val cloudPart = cloudParts.getOrNull(i)?.toLongOrNull() ?: 0
            if (cloudPart > localPart) {
                return true
            }
        }
        return false
    }


}

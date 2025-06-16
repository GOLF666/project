package com.example.jenmix.jen8

import android.content.Context
import android.content.SharedPreferences

object UserPrefs {

    private const val PREF_NAME = "user_info"
    private const val KEY_GENDER = "gender"
    private const val KEY_HEIGHT = "height"
    private const val KEY_AGE = "age"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // ✅ 取得使用者資料
    fun getGender(context: Context): String =
        getPrefs(context).getString(KEY_GENDER, "") ?: ""

    fun getHeight(context: Context): Float =
        getPrefs(context).getFloat(KEY_HEIGHT, 0f)

    fun getAge(context: Context): Int =
        getPrefs(context).getInt(KEY_AGE, 0)

    // ✅ 儲存使用者完整資料
    fun saveUserInfo(context: Context, gender: String, height: Float, age: Int) {
        getPrefs(context).edit().apply {
            putString(KEY_GENDER, gender)
            putFloat(KEY_HEIGHT, height)
            putInt(KEY_AGE, age)
            apply()
        }
    }

    // ✅ 單筆更新（新增 save 開頭函式）
    fun setGender(context: Context, gender: String) {
        getPrefs(context).edit().putString(KEY_GENDER, gender).apply()
    }

    fun setHeight(context: Context, height: Float) {
        getPrefs(context).edit().putFloat(KEY_HEIGHT, height).apply()
    }

    fun setAge(context: Context, age: Int) {
        getPrefs(context).edit().putInt(KEY_AGE, age).apply()
    }

    fun saveGender(context: Context, gender: String) = setGender(context, gender)
    fun saveHeight(context: Context, height: Float) = setHeight(context, height)
    fun saveAge(context: Context, age: Int) = setAge(context, age)

    // ✅ 判斷是否已完成設定
    fun isSetupCompleted(context: Context): Boolean {
        return getGender(context).isNotEmpty() &&
                getHeight(context) > 0f &&
                getAge(context) > 0
    }

    // ✅ 清除使用者資料
    fun clearUserInfo(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}

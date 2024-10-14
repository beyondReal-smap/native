package com.dmonster.smap.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.dmonster.smap.R
import com.google.android.material.snackbar.Snackbar
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "Common"
var snackBar: Snackbar? = null

fun showSnackBar(
    activity: Activity, message: String
) {
    hideSnackBar()
    val view = activity.findViewById<ConstraintLayout>(R.id.root)
    snackBar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT).apply {
        show()
    }
}

fun showSnackBar(
    view: View, message: String
) {
    hideSnackBar()
    snackBar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT).apply {
        show()
    }
}

fun hideSnackBar() {
    if (snackBar != null) snackBar!!.dismiss()
}

// 상태바 투명하게 바꿔주는 함수
fun setTransparentStatusBar(
    window: Window, rootView: View? = null, isLightStatusBar: Boolean = true
) {
    window.apply {
        @Suppress("DEPRECATION") if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            decorView.systemUiVisibility = when {
                !isLightStatusBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }

                !isLightStatusBar && Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }

                else -> View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        } else {
            setDecorFitsSystemWindows(!isLightStatusBar)
            if (isLightStatusBar) {
                // 상태바 내부 Contents Color 초기화
                decorView.windowInsetsController?.setSystemBarsAppearance(
                    0, APPEARANCE_LIGHT_STATUS_BARS
                )
                rootView?.setPadding(0, 0, 0, getNavigationBarHeight(context))
            } else {
                // 상태바 내부 Contents Color 세팅
                decorView.windowInsetsController?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }

        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        statusBarColor = if (isLightStatusBar) {
            Color.TRANSPARENT
        } else {
            Color.WHITE
        }
    }
}

// 시스템의 Navigation Bar 높이를 가져오는 함수
fun getNavigationBarHeight(context: Context): Int {
    val resources = context.resources
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    if (resourceId > 0) {
        return resources.getDimensionPixelSize(resourceId)
    }
    return 0
}

fun Cache.clearMalformedUrls(): Cache {
    // corrupt 된 캐시 삭제하기
    val urlIterator = urls()
    while (urlIterator.hasNext()) {
        if (urlIterator.next().toHttpUrlOrNull() == null) {
            urlIterator.remove()
        }
    }

    return this
}

fun setPref(context: Context, key: String, value: String) {
    val pref: SharedPreferences = context.getSharedPreferences(TAG, Activity.MODE_PRIVATE)
    val editor: SharedPreferences.Editor = pref.edit()
    editor.putString(key, value)
    editor.apply()
}

fun getPref(context: Context, key: String): String {
    val pref: SharedPreferences = context.getSharedPreferences(TAG, Activity.MODE_PRIVATE)
    return if (pref.contains(key)) {
        pref.getString(key, "")!!
    } else {
        ""
    }
}

fun setPref(context: Context, key: String, value: Boolean) {
    val pref: SharedPreferences = context.getSharedPreferences(TAG, Activity.MODE_PRIVATE)
    val editor: SharedPreferences.Editor = pref.edit()
    editor.putBoolean(key, value)
    editor.apply()
}

fun getPref(context: Context, key: String, value: Boolean): Boolean {
    val pref: SharedPreferences = context.getSharedPreferences(TAG, Activity.MODE_PRIVATE)
    /*return try {
        pref?.contains(key)
    }catch (e : Exception){
        value
    }*/
    return if (pref.contains(key)) {
        true
    } else {
        value
    }
}

// 값(Key Data) 삭제하기
fun delPref(context: Context, key: String) {
    val pref = context.getSharedPreferences(TAG, Activity.MODE_PRIVATE)
    val editor = pref.edit()
    editor.remove(key)
    editor.apply()
}

// 값(ALL Data) 삭제하기
fun delAllPref(context: Context) {
    val pref = context.getSharedPreferences(TAG, Activity.MODE_PRIVATE)
    val editor = pref.edit()
    editor.clear()
    editor.apply()
}

fun getBattery(context: Context): String {
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
        context.registerReceiver(null, ifilter)
    }

    val batteryPct: Float? = batteryStatus?.let { intent ->
        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        level * 100 / scale.toFloat()
    }

    //Log.e("getBattery", "batteryPct = $batteryPct")
    return batteryPct.toString()
}

@RequiresApi(Build.VERSION_CODES.Q)
fun saveImageOnAboveAndroidQ(context: Context, bitmap: Bitmap): String? {
    createTmpDir(context)
    val dirName = tmpDirName(context)
    val contentResolver = context.contentResolver

    val fileName = System.currentTimeMillis().toString() + ".png" // 파일이름 현재시간.png

    /*
    * ContentValues() 객체 생성.
    * ContentValues는 ContentResolver가 처리할 수 있는 값을 저장해둘 목적으로 사용된다.
    * */
    val contentValues = ContentValues()
    contentValues.apply {
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/${dirName}") // 경로 설정
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName) // 파일이름을 put해준다.
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.IS_PENDING, 1) // 현재 is_pending 상태임을 만들어준다.
        // 다른 곳에서 이 데이터를 요구하면 무시하라는 의미로, 해당 저장소를 독점할 수 있다.
    }

    // 이미지를 저장할 uri를 미리 설정해놓는다.
    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    try {
        if(uri != null) {
            val image = contentResolver.openFileDescriptor(uri, "w", null)
            // write 모드로 file을 open한다.

            if(image != null) {
                val fos = FileOutputStream(image.fileDescriptor)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                //비트맵을 FileOutputStream를 통해 compress한다.
                fos.close()

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // 저장소 독점을 해제한다.
                contentResolver.update(uri, contentValues, null, null)

                return getRealPathFromUri(context, uri)
            }
        }
    } catch(e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}

fun saveImageOnUnderAndroidQ(context: Context, bitmap: Bitmap): String? {
    createTmpDir(context)
    val dirName = tmpDirName(context)

    val fileName = System.currentTimeMillis().toString() + ".png"
    val externalStorage = Environment.getExternalStorageDirectory().absolutePath
    val path = "$externalStorage/${Environment.DIRECTORY_DCIM}/${dirName}"
    //val path = "$externalStorage/DCIM/imageSave"
    val dir = File(path)

    if(dir.exists().not()) {
        dir.mkdirs() // 폴더 없을경우 폴더 생성
    }

    try {
        val fileItem = File("$dir/$fileName")
        fileItem.createNewFile()
        //0KB 파일 생성.

        val fos = FileOutputStream(fileItem) // 파일 아웃풋 스트림

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        //파일 아웃풋 스트림 객체를 통해서 Bitmap 압축.

        fos.close() // 파일 아웃풋 스트림 객체 close

        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
        // 브로드캐스트 수신자에게 파일 미디어 스캔 액션 요청. 그리고 데이터로 추가된 파일에 Uri를 넘겨준다.

        return "$dir/$fileName"
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}

private fun getRealPathFromUri(context: Context, contentUri: Uri?): String? {
    var cursor: Cursor? = null
    return try {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
        val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        cursor.getString(column_index)
    } finally {
        cursor?.close()
    }
}

fun createTmpDir(context: Context){
    var tmpDirtLastName = getPref(context, "tmp_dir")
    if (TextUtils.isEmpty(tmpDirtLastName)) {
        tmpDirtLastName = System.currentTimeMillis().toString()
        setPref(context, "tmp_dir", tmpDirtLastName)
    }
    val tmpDirName = "smap" + tmpDirtLastName
    val path = Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DCIM + "/" + tmpDirName
    val dir = File(path)
    if (!dir.exists()) {
        dir.mkdirs()
    }
}

fun tmpDirName(context: Context): String {
    return "smap" + getPref(context, "tmp_dir")
}

fun allTmpFileDelete(context: Context){
    val externalStorage = Environment.getExternalStorageDirectory().absolutePath
    val tmpDirName = tmpDirName(context)
    val path = "$externalStorage/${Environment.DIRECTORY_DCIM}/${tmpDirName}"

    val dir = File(path)
    val childFilelist = dir.listFiles()

    if (dir.exists()) {
        if (childFilelist != null) {
            for (childFile in childFilelist) {
                val contentUri: Uri? = getImageContentUri(context, childFile.absolutePath)

                context.contentResolver.delete(contentUri!!, null, null)
            }
        }
    }
}

private fun getImageContentUri(context: Context, absPath: String): Uri? {
    Log.v(TAG, "getImageContentUri: $absPath")
    val cursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media._ID),
        MediaStore.Images.Media.DATA + "=? ",
        arrayOf(absPath),
        null
    )
    return if (cursor != null && cursor.moveToFirst()) {
        val index = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
        val id = cursor.getInt(index)
        Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(id))
    } else if (absPath.isNotEmpty()) {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DATA, absPath)
        context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )
    } else {
        null
    }
}
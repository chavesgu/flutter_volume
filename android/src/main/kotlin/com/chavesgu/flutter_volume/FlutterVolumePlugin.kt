package com.chavesgu.flutter_volume

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.lang.ref.WeakReference
import android.content.IntentFilter


/** FlutterVolumePlugin */
class FlutterVolumePlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
  private val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
  private lateinit var channel : MethodChannel
  private var context:Context? = null
  private var activity:Activity? = null
  private var volumeReceiver: VolumeReceiver? = null

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }
  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }
  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }
  override fun onDetachedFromActivity() {
    activity = null
  }
  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "chavesgu/flutter_volume")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }
  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    context = null
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "getVolume") {
      if (activity!=null) {
        result.success(getVolumePercent())
      }
    } else if (call.method == "listen") {
      registerVolumeReceiver()
    } else if (call.method == "dispose") {
      unregisterVolumeReceiver()
    } else {
      result.notImplemented()
    }
  }

  private fun getVolumePercent(): Float {
    val audioManager:AudioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
    val maxVolumeLevel = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    return (volumeLevel / maxVolumeLevel)
  }

  private fun registerVolumeReceiver() {
    if (volumeReceiver==null) {
      val intentFilter = IntentFilter()
      intentFilter.addAction(ACTION_VOLUME_CHANGED)
      volumeReceiver = VolumeReceiver(this)
      context!!.registerReceiver(volumeReceiver, intentFilter)
    }
  }

  private fun unregisterVolumeReceiver() {
    if (volumeReceiver!=null) context!!.unregisterReceiver(volumeReceiver)
    volumeReceiver = null
  }

  inner class VolumeReceiver(plugin: FlutterVolumePlugin): BroadcastReceiver() {
    private var observe: WeakReference<FlutterVolumePlugin> = WeakReference(plugin)

    override fun onReceive(context: Context, intent: Intent) {
      if (isReceiveVolumeChange(intent)) {
        val map = HashMap<String, Float>()
        map["value"] = getVolumePercent()
        channel.invokeMethod("setVolume", map)
      }
    }

    private fun isReceiveVolumeChange(intent: Intent): Boolean {
      return (intent.action != null
              && intent.action == ACTION_VOLUME_CHANGED
              && intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_MUSIC)
    }
  }
}

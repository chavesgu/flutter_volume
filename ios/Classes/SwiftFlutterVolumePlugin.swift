import Flutter
import UIKit
import AVFoundation

public class SwiftFlutterVolumePlugin: NSObject, FlutterPlugin {
  var channel:FlutterMethodChannel!;
  var isListen:Bool = false;
  let audioSession = AVAudioSession.sharedInstance();
  let defaultNotificationCenter = NotificationCenter.default;
  
  public static func register(with registrar: FlutterPluginRegistrar) {
    let instance = SwiftFlutterVolumePlugin();
    instance.channel = FlutterMethodChannel(name: "chavesgu/flutter_volume", binaryMessenger: registrar.messenger());
    registrar.addMethodCallDelegate(instance, channel: instance.channel);
//    registrar.addApplicationDelegate(instance);
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getVolume":
      var volume: Float?;
      do {
        try audioSession.setActive(true);
        volume = audioSession.outputVolume;
      } catch {
        print("Error Setting Up Audio Session");
      }
      result(volume);
      break;
    case "listen":
      activateAudioSession();
      defaultNotificationCenter.addObserver(
                  self,
                  selector: #selector(activateAudioSession),
                  name: UIApplication.didBecomeActiveNotification,
        object: nil);
      break;
    case "dispose":
      isListen = false;
      audioSession.removeObserver(self, forKeyPath: "outputVolume")
      defaultNotificationCenter.removeObserver(self,name: UIApplication.didBecomeActiveNotification,object: nil);
      break;
    default:
      result("iOS " + UIDevice.current.systemVersion)
    }
  }
  
  @objc func activateAudioSession() {
      do {
        try audioSession.setCategory(AVAudioSession.Category.ambient);
        try audioSession.setActive(true);
          if !isListen {
              audioSession.addObserver(self,
                                       forKeyPath: "outputVolume",
                                       options: .new,
                                       context: nil);
            isListen = true;
          }
      } catch {
          print("activateAudioSession error")
      }
  }
  
  override public func observeValue(forKeyPath keyPath: String?,
                                        of object: Any?,
                                        change: [NSKeyValueChangeKey: Any]?,
                                        context: UnsafeMutableRawPointer?) {
    if keyPath == "outputVolume" {
//      print("observe volume: \(audioSession.outputVolume)");
      channel.invokeMethod("setVolume", arguments: ["value": audioSession.outputVolume])
    }
  }
}

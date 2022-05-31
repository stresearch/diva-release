//
//  Utilities.swift
//  AcuityLinkHiAdmin
//
//  Created by Srujan on 1/28/19.
//  Copyright © 2019 Boston Technology Corporation. All rights reserved.
//

import UIKit
import AVFoundation

enum ScreenSize {

    static let SCREEN_WIDTH = UIScreen.main.bounds.size.width
    static let SCREEN_HEIGHT = UIScreen.main.bounds.size.height

    static let SCREEN_MAX_LENGTH = max(ScreenSize.SCREEN_WIDTH, ScreenSize.SCREEN_HEIGHT)

    static let SCREEN_MIN_LENGTH = min(ScreenSize.SCREEN_WIDTH, ScreenSize.SCREEN_HEIGHT)
}

enum DeviceType {

    static let IS_IPHONE_4_OR_LESS = UIDevice.current.userInterfaceIdiom == .phone && ScreenSize
        .SCREEN_MAX_LENGTH < 568.0

    static let IS_IPHONE_5 = UIDevice.current.userInterfaceIdiom == .phone && ScreenSize
        .SCREEN_MAX_LENGTH == 568.0

    static let IS_IPHONE_6 = UIDevice.current.userInterfaceIdiom == .phone && ScreenSize
        .SCREEN_MAX_LENGTH == 667.0

    static let IS_IPHONE_6P = UIDevice.current.userInterfaceIdiom == .phone && ScreenSize
        .SCREEN_MAX_LENGTH == 736.0

    static let IS_IPAD = UIDevice.current.userInterfaceIdiom == .pad && ScreenSize.SCREEN_MAX_LENGTH
        >= 1024.0

    static let IS_IPHONEX = UIDevice.current.userInterfaceIdiom == .phone && ScreenSize
        .SCREEN_MAX_LENGTH == 812.0
    
    static let IS_IPHONEMAX = UIDevice.current.userInterfaceIdiom == .phone && ScreenSize
    .SCREEN_MAX_LENGTH == 896.0
}

class Utilities: NSObject {

    class func getAppVersion() -> String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        let bundleVersion = Bundle.main.infoDictionary?["CFBundleVersion"] as? String
        return version! + "." + bundleVersion!
    }

    class func getBundleIdentifier() -> String { return Bundle.main.bundleIdentifier! }

    class func getDictionaryFromLocalJSON(filename: String) -> JSONDictionary? {

        guard let fileURL = Bundle.main.url(forResource: filename, withExtension: "json") else {
            return nil
        }
        let data = try! Data(contentsOf: fileURL, options: .mappedIfSafe)
        let jsonResult = try? JSONSerialization.jsonObject(with: data, options: .mutableLeaves)
        return jsonResult as? JSONDictionary

    }

    class func getDataFromLocalJSON(filename: String) -> Data? {
        guard let fileURL = Bundle.main.url(forResource: filename, withExtension: "json") else {
            return nil
        }
        do {
            return try Data(contentsOf: fileURL, options: .mappedIfSafe)
        } catch  {
          
        }
        return nil
    }
  
    class func getFilePathFromLocal(fileName: String, type: String) -> String? {
      
      guard let path = Bundle.main.path(forResource: fileName, ofType: type) else {
          debugPrint("video.m4v not found")
        return nil
      }
      return path
    }
  
    class func getDataFromDirectory(fileurl: URL) -> Data? {
       
      let filename = "videoinfo.json"
       if let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {

           let fileURL = dir.appendingPathComponent(filename)
            if FileManager.default.fileExists(atPath: fileURL.path) {
                print(true)
                do {
                    return try Data(contentsOf: fileURL, options: .mappedIfSafe)
                } catch  {

                }
            } else {
              print(false)
            }
      }
      return nil
    }
    
    class func getDataFromCacheDirectoryByName(fileurl: URL, filename: String?) -> Data? {
        if let dir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first {
            var fileURL = fileurl
            if let filename = filename, !filename.isEmpty{
            fileURL = dir.appendingPathComponent(filename)
            }
            if FileManager.default.fileExists(atPath: fileURL.path) {
                print(true)
                do {
                    return try Data(contentsOf: fileURL, options: .mappedIfSafe)
                } catch  {
                  
                }
            } else {
              print(false)
            }
      }
      return nil
    }
    
    ///Retrive UrlString for Specific Namefile in Cache IMP - Wont check or retrieve the data, just makes urlString;     fileName - Name+Extension example -Training1.mp4/Training1.json
    
    class func getFileUrlStringFromCache(fileName: String) -> String {
        var outputPath: String {
            get{
                let cachesDirectory = NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true)
                return "\(cachesDirectory[0])/\(fileName)"
            }
        }
        return outputPath
    }
    
    class func getImageWithColor(color: UIColor, size: CGSize, rounded: Bool) -> UIImage {
        let rect = CGRect(x: 0, y: 0, width: size.width, height: size.height)
        UIGraphicsBeginImageContextWithOptions(size, false, 0)
        color.setFill()
        UIRectFill(rect)
        let image: UIImage = UIGraphicsGetImageFromCurrentImageContext()!
        UIGraphicsEndImageContext()
        if rounded {
            let img = image.withRoundedCorners()!
            return img
        }
        return image
    }
  
    class func getVideoRelativePathUrl() -> URL? {
        //let filename = "video.mov"
        // if let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
              let cachesDirectory = NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true)
              let fileURL = "\(cachesDirectory[0])/video.mp4" //dir.appendingPathComponent(filename)
              if FileManager.default.fileExists(atPath: fileURL) {
                  print(true)
                  return URL(fileURLWithPath: fileURL)
              } else {
                print(false)
              }
         // }
        return nil
    }
    
    class func getDataFromVideoRelativePathUrl(fileurl: URL) -> Data? {
        do {
            return try Data(contentsOf: fileurl, options: .mappedIfSafe)
        } catch  {
          print(error)
        }
        return nil
    }
    
  
    class func getDictionaryFromDirectory(fileurl: URL) -> JSONDictionary? {
      let data = try! Data(contentsOf: fileurl, options: .mappedIfSafe)
      let jsonResult = try? JSONSerialization.jsonObject(with: data, options: .mutableLeaves)
      return jsonResult as? JSONDictionary
    }
  
    class func saveJsonToFile(dic: [String: Any], fileName: String) -> URL? {
        
        let filename = fileName
        if let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {

          let fileURL = dir.appendingPathComponent(filename)

          do {
            if let jsonData = try? JSONSerialization.data(withJSONObject: dic, options: .init(rawValue: 0)) {
                // Check if everything went well
                  try jsonData.write(to: fileURL)
                
                  return fileURL
                  // Do something cool with the new JSON data
            }
          }
          catch {/* error handling here */}
        }
        return nil
    }
    
    ///File - Remove Cache File
    class func removeFileFromCache(fileName: String) {
        //Removing BlurredVideo.mp4 and Video.mp4
        let fileManager = FileManager()
        if fileManager.fileExists(atPath: Utilities.getFileUrlStringFromCache(fileName: fileName)) {
            try? fileManager.removeItem(atPath: Utilities.getFileUrlStringFromCache(fileName: fileName))
        }
    }
    
    class func imageTobase64(image: UIImage) -> String {
        var base64String = ""
        let cim = CIImage(image: image)
        if cim != nil {
            let imageData = image.lowQualityJPEGData
            base64String = imageData.base64EncodedString(
                options: Data.Base64EncodingOptions.lineLength64Characters
            )
        }
        return base64String
    }

    class func base64ToImage(base64: String) -> UIImage {
        var img: UIImage = UIImage()
        if !base64.isEmpty {
            if let decodedData = Data(
                base64Encoded: base64,
                options: Data.Base64DecodingOptions.ignoreUnknownCharacters
            ) {
                let decodedimage = UIImage(data: decodedData)
                img = (decodedimage as UIImage?)!
            }

        }
        return img
    }

    class func resizeImage(image: UIImage, targetSize: CGSize) -> UIImage? {
        let size = image.size

        let widthRatio = targetSize.width / size.width
        let heightRatio = targetSize.height / size.height

        // Figure out what our orientation is, and use that to form the rectangle
        var newSize: CGSize
        if widthRatio > heightRatio {
            newSize = CGSize(width: size.width * heightRatio, height: size.height * heightRatio)
        } else {
            newSize = CGSize(width: size.width * widthRatio, height: size.height * widthRatio)
        }

        // This is the rect that we've calculated out and this is what is actually used below
        let rect = CGRect(x: 0, y: 0, width: newSize.width, height: newSize.height)

        // Actually do the resizing to the rect using the ImageContext stuff
        UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        image.draw(in: rect)
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return newImage
    }
    
    class func getOrientationInString(value: Int) -> String {
        switch AVCaptureVideoOrientation(rawValue: value) {
        case .portrait:
            return "portrait"
        case .landscapeLeft:
            return "landscapeLeft"
        case .landscapeRight:
            return "landscapeRight"
        default:
            return "portrait"
        }
    }
    
    class func getOrientationInInt(value: String) -> Int {
        if value == "portrait" {
            return 1
        }
        else if value == "landscapeLeft" {
            return 4
        }
        else if value == "landscapeRight" {
            return 3
        }
        else {
            return 1
        }
    }
    
    class func CGPointDistanceSquared(from: CGPoint, to: CGPoint) -> CGFloat {
        return (from.x - to.x) * (from.x - to.x) + (from.y - to.y) * (from.y - to.y)
    }
    
    class func CGPointDistance(from: CGPoint, to: CGPoint) -> CGFloat {
        return sqrt(CGPointDistanceSquared(from: from, to: to))
    }
    
    class func getPublicIPAddress() -> String {
        var publicIP = ""
        do {
            try publicIP = String(contentsOf: URL(string: "https://api.ipify.org/?format=text")!, encoding: String.Encoding.utf8)
            publicIP = publicIP.trimmingCharacters(in: CharacterSet.whitespaces)
        }
        catch {
            print("Error: \(error)")
        }
        return publicIP
    }

}

typealias JSONDictionary = [String: Any]

extension UIImage {
    var highestQualityJPEGData: Data { return self.jpegData(compressionQuality: 1.0)! as Data }

    var highQualityJPEGData: Data { return self.jpegData(compressionQuality: 0.75)! as Data }

    var mediumQualityJPEGData: Data { return self.jpegData(compressionQuality: 0.5)! as Data }

    var lowQualityJPEGData: Data { return self.jpegData(compressionQuality: 0.25)! as Data }

    var lowestQualityJPEGData: Data { return self.jpegData(compressionQuality: 0.0)! as Data }
}

public enum Model : String {

//Simulator
case simulator     = "simulator/sandbox",

//iPod
iPod1              = "iPod 1",
iPod2              = "iPod 2",
iPod3              = "iPod 3",
iPod4              = "iPod 4",
iPod5              = "iPod 5",

//iPad
iPad2              = "iPad 2",
iPad3              = "iPad 3",
iPad4              = "iPad 4",
iPadAir            = "iPad Air ",
iPadAir2           = "iPad Air 2",
iPadAir3           = "iPad Air 3",
iPad5              = "iPad 5", //iPad 2017
iPad6              = "iPad 6", //iPad 2018
iPad7              = "iPad 7", //iPad 2019

//iPad Mini
iPadMini           = "iPad Mini",
iPadMini2          = "iPad Mini 2",
iPadMini3          = "iPad Mini 3",
iPadMini4          = "iPad Mini 4",
iPadMini5          = "iPad Mini 5",

//iPad Pro
iPadPro9_7         = "iPad Pro 9.7\"",
iPadPro10_5        = "iPad Pro 10.5\"",
iPadPro11          = "iPad Pro 11\"",
iPadPro12_9        = "iPad Pro 12.9\"",
iPadPro2_12_9      = "iPad Pro 2 12.9\"",
iPadPro3_12_9      = "iPad Pro 3 12.9\"",

//iPhone
iPhone4            = "iPhone 4",
iPhone4S           = "iPhone 4S",
iPhone5            = "iPhone 5",
iPhone5S           = "iPhone 5S",
iPhone5C           = "iPhone 5C",
iPhone6            = "iPhone 6",
iPhone6Plus        = "iPhone 6 Plus",
iPhone6S           = "iPhone 6S",
iPhone6SPlus       = "iPhone 6S Plus",
iPhoneSE           = "iPhone SE",
iPhone7            = "iPhone 7",
iPhone7Plus        = "iPhone 7 Plus",
iPhone8            = "iPhone 8",
iPhone8Plus        = "iPhone 8 Plus",
iPhoneX            = "iPhone X",
iPhoneXS           = "iPhone XS",
iPhoneXSMax        = "iPhone XS Max",
iPhoneXR           = "iPhone XR",
iPhone11           = "iPhone 11",
iPhone11Pro        = "iPhone 11 Pro",
iPhone11ProMax     = "iPhone 11 Pro Max",
iPhoneSE2          = "iPhone SE 2nd gen",
iPhone12Mini       = "iPhone 12 Mini",
iPhone12           = "iPhone 12",
iPhone12Pro        = "iPhone 12 Pro",
iPhone12ProMax     = "iPhone 12 Pro Max",

//Apple TV
AppleTV            = "Apple TV",
AppleTV_4K         = "Apple TV 4K",
unrecognized       = "?unrecognized?"
}

// #-#-#-#-#-#-#-#-#-#-#-#-#
// MARK: UIDevice extensions
// #-#-#-#-#-#-#-#-#-#-#-#-#

public extension UIDevice {

var type: Model {
    var systemInfo = utsname()
    uname(&systemInfo)
    let modelCode = withUnsafePointer(to: &systemInfo.machine) {
        $0.withMemoryRebound(to: CChar.self, capacity: 1) {
            ptr in String.init(validatingUTF8: ptr)
        }
    }

    let modelMap : [String: Model] = [

        //Simulator
        "i386"      : .simulator,
        "x86_64"    : .simulator,

        //iPod
        "iPod1,1"   : .iPod1,
        "iPod2,1"   : .iPod2,
        "iPod3,1"   : .iPod3,
        "iPod4,1"   : .iPod4,
        "iPod5,1"   : .iPod5,

        //iPad
        "iPad2,1"   : .iPad2,
        "iPad2,2"   : .iPad2,
        "iPad2,3"   : .iPad2,
        "iPad2,4"   : .iPad2,
        "iPad3,1"   : .iPad3,
        "iPad3,2"   : .iPad3,
        "iPad3,3"   : .iPad3,
        "iPad3,4"   : .iPad4,
        "iPad3,5"   : .iPad4,
        "iPad3,6"   : .iPad4,
        "iPad6,11"  : .iPad5, //iPad 2017
        "iPad6,12"  : .iPad5,
        "iPad7,5"   : .iPad6, //iPad 2018
        "iPad7,6"   : .iPad6,
        "iPad7,11"  : .iPad7, //iPad 2019
        "iPad7,12"  : .iPad7,

        //iPad Mini
        "iPad2,5"   : .iPadMini,
        "iPad2,6"   : .iPadMini,
        "iPad2,7"   : .iPadMini,
        "iPad4,4"   : .iPadMini2,
        "iPad4,5"   : .iPadMini2,
        "iPad4,6"   : .iPadMini2,
        "iPad4,7"   : .iPadMini3,
        "iPad4,8"   : .iPadMini3,
        "iPad4,9"   : .iPadMini3,
        "iPad5,1"   : .iPadMini4,
        "iPad5,2"   : .iPadMini4,
        "iPad11,1"  : .iPadMini5,
        "iPad11,2"  : .iPadMini5,

        //iPad Pro
        "iPad6,3"   : .iPadPro9_7,
        "iPad6,4"   : .iPadPro9_7,
        "iPad7,3"   : .iPadPro10_5,
        "iPad7,4"   : .iPadPro10_5,
        "iPad6,7"   : .iPadPro12_9,
        "iPad6,8"   : .iPadPro12_9,
        "iPad7,1"   : .iPadPro2_12_9,
        "iPad7,2"   : .iPadPro2_12_9,
        "iPad8,1"   : .iPadPro11,
        "iPad8,2"   : .iPadPro11,
        "iPad8,3"   : .iPadPro11,
        "iPad8,4"   : .iPadPro11,
        "iPad8,5"   : .iPadPro3_12_9,
        "iPad8,6"   : .iPadPro3_12_9,
        "iPad8,7"   : .iPadPro3_12_9,
        "iPad8,8"   : .iPadPro3_12_9,

        //iPad Air
        "iPad4,1"   : .iPadAir,
        "iPad4,2"   : .iPadAir,
        "iPad4,3"   : .iPadAir,
        "iPad5,3"   : .iPadAir2,
        "iPad5,4"   : .iPadAir2,
        "iPad11,3"  : .iPadAir3,
        "iPad11,4"  : .iPadAir3,


        //iPhone
        "iPhone3,1" : .iPhone4,
        "iPhone3,2" : .iPhone4,
        "iPhone3,3" : .iPhone4,
        "iPhone4,1" : .iPhone4S,
        "iPhone5,1" : .iPhone5,
        "iPhone5,2" : .iPhone5,
        "iPhone5,3" : .iPhone5C,
        "iPhone5,4" : .iPhone5C,
        "iPhone6,1" : .iPhone5S,
        "iPhone6,2" : .iPhone5S,
        "iPhone7,1" : .iPhone6Plus,
        "iPhone7,2" : .iPhone6,
        "iPhone8,1" : .iPhone6S,
        "iPhone8,2" : .iPhone6SPlus,
        "iPhone8,4" : .iPhoneSE,
        "iPhone9,1" : .iPhone7,
        "iPhone9,3" : .iPhone7,
        "iPhone9,2" : .iPhone7Plus,
        "iPhone9,4" : .iPhone7Plus,
        "iPhone10,1" : .iPhone8,
        "iPhone10,4" : .iPhone8,
        "iPhone10,2" : .iPhone8Plus,
        "iPhone10,5" : .iPhone8Plus,
        "iPhone10,3" : .iPhoneX,
        "iPhone10,6" : .iPhoneX,
        "iPhone11,2" : .iPhoneXS,
        "iPhone11,4" : .iPhoneXSMax,
        "iPhone11,6" : .iPhoneXSMax,
        "iPhone11,8" : .iPhoneXR,
        "iPhone12,1" : .iPhone11,
        "iPhone12,3" : .iPhone11Pro,
        "iPhone12,5" : .iPhone11ProMax,
        "iPhone12,8" : .iPhoneSE2,
        "iPhone13,1" : .iPhone12Mini,
        "iPhone13,2" : .iPhone12,
        "iPhone13,3" : .iPhone12Pro,
        "iPhone13,4" : .iPhone12ProMax,

        //Apple TV
        "AppleTV5,3" : .AppleTV,
        "AppleTV6,2" : .AppleTV_4K
    ]

    if let model = modelMap[String.init(validatingUTF8: modelCode!)!] {
        if model == .simulator {
            if let simModelCode = ProcessInfo().environment["SIMULATOR_MODEL_IDENTIFIER"] {
                if let simModel = modelMap[String.init(validatingUTF8: simModelCode)!] {
                    return simModel
                }
            }
        }
        return model
    }
    return Model.unrecognized
  }
    
    var hasNotch: Bool {
        let bottom = UIApplication.shared.keyWindow?.safeAreaInsets.bottom ?? 0
        return bottom > 0
    }
}

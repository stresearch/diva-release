//
//  VideoInfo.swift
//  STR
//
//  Created by Srujan on 04/02/20.
//  
//

import Codextended

// MARK: - Frame
struct Frame: Codable {
    let height, width, x, y: Int
}

struct Cordinate: Codable {
    
    var frame: Frame
    var time: Int
    
    init(zframe: Frame, ztime: Int) {
        frame = zframe
        time = ztime
    }
}

// MARK: - Welcome
struct WelcomeAtEncode: Codable {
    let metadata: Metadata
    let activity: [Activity]
    let object: [Object]
    
    private struct JSONKey {
        static let metadata = "metadata"
        static let activity = "activity"
        static let object = "object"
    }
    
    func getJsonBody() -> JSONDictionary {
        return [
            JSONKey.metadata: metadata.convertToDictionary(),
            JSONKey.activity: activity.map({$0.convertToDictionary()}),
            JSONKey.object: object.map({$0.convertToDictionary()})
        ]
    }
}

struct WelcomeAtDecode: Codable {
    let metadata: Metadata2
    let activity: [Activity]
    let object: [Object]
    
    private struct JSONKey {
        static let metadata = "metadata"
        static let activity = "activity"
        static let object = "object"
    }
    
    func getJsonBody() -> JSONDictionary {
        return [
            JSONKey.metadata: metadata.convertToDictionary(),
            JSONKey.activity: activity.map({$0.convertToDictionary()}),
            JSONKey.object: object.map({$0.convertToDictionary()})
        ]
    }
}

// MARK: - Activity
class Activity: Codable, Equatable {
    static func == (lhs: Activity, rhs: Activity) -> Bool {
         return ((lhs.startFrame == rhs.startFrame) && (lhs.endFrame == rhs.endFrame) && (lhs.label == rhs.label) )
    }
    
    
    var label: String
    var startFrame: Int
    var endFrame: Int?
    let objectIndex: [Int]
    var selectedIndex: Int

    init(activityName: String, sTime: Int, activityIndex: Int) {
        label = activityName
        startFrame = sTime
        objectIndex = [0]
        selectedIndex = activityIndex
    }
      
    public func convertToDictionary() -> [String : Any] {
        let dic: [String: Any] = [
            JSONKey.label: self.label,
            JSONKey.startFrame: self.startFrame,
            JSONKey.endFrame: self.endFrame ?? 0,//CHECK
            JSONKey.objectIndex: self.objectIndex
        ]
        return dic
    }

    enum CodingKeys: String, CodingKey {
        case label
        case startFrame = "start_frame"
        case endFrame = "end_frame"
        case objectIndex = "object_index"
    }

    private struct JSONKey {
        static let label = "label"
        static let  startFrame = "start_frame"
        static let  endFrame = "end_frame"
        static let  objectIndex = "object_index"
        static let  selectedIndex = "index"
    }

    // MARK: - Initializers
    required init(from decoder: Decoder) throws {
        label = (try? decoder.decode(JSONKey.label)) ?? ""
        startFrame = (try? decoder.decode(JSONKey.startFrame)) ?? 0
        endFrame = (try? decoder.decode(JSONKey.endFrame)) ?? 0
        objectIndex = (try? decoder.decode(JSONKey.objectIndex)) ?? [0]
        selectedIndex = (try? decoder.decode(JSONKey.selectedIndex)) ?? 0
    }
}

// MARK: - Metadata
struct Metadata: Codable {
    let projectID, collectionID, collectorID, collectionName, projectName, programName: String
    let subjectIDS: [String]
    var videoID: String
    let deviceType: String
    let deviceIdentifier: String
    let osVersion: String
    let collectedDate: String
    let blurredFaces: Int
    let frameRate, frameWidth, frameHeight, duration: Double
    let orientation: String
    let appVersion: String
    let ipAddress: String
    let activityLongNames: String
    let activityShortNames: String
//    let uploadedDate: String
    
    enum CodingKeys: String, CodingKey {
        case projectID = "project_id"
        case collectionID = "collection_id"
        case collectorID = "collector_id"
        case collectionName = "collection_name"
        case projectName = "project_name"
        case programName = "program_name"
        case subjectIDS = "subject_ids"
        case videoID = "video_id"
        case deviceType = "device_type"
        case deviceIdentifier = "device_identifier"
        case osVersion = "os_version"
        case collectedDate = "collected_date"
        case blurredFaces = "blurred_faces"
        case frameRate = "frame_rate"
        case frameWidth = "frame_width"
        case frameHeight = "frame_height"
        case appVersion = "app_version"
        case ipAddress = "ipAddress"
        case activityLongNames = "category"
        case activityShortNames = "shortname"
//        case uploadedDate = "uploaded_date"
        case duration, orientation
    }
    
    private struct JSONKey {
        static let projectID = "project_id"
        static let collectionID = "collection_id"
        static let collectorID = "collector_id"
        static let collectionName = "collection_name"
        static let projectName = "project_name"
        static let programName = "program_name"
        static let subjectIDS = "subject_ids"
        static let videoID = "video_id"
        static let deviceType = "device_type"
        static let deviceIdentifier = "device_identifier"
        static let osVersion = "os_version"
        static let collectedDate = "collected_date"
        static let blurredFaces = "blurred_faces"
        static let frameRate = "frame_rate"
        static let frameWidth = "frame_width"
        static let frameHeight = "frame_height"
        static let duration = "duration"
        static let orientation = "orientation"
        static let appVersion = "app_version"
        static let ipAddress = "ipAddress"
        static let activityLongNames = "category"
        static let activityShortNames = "shortname"
//        static let uploadedDate = "uploaded_date"
    }
    
    
    public func convertToDictionary() -> [String : Any] {
        let dic: [String: Any] = [
            JSONKey.projectID: self.projectID,
            JSONKey.collectionID: self.collectionID,
            JSONKey.collectorID: self.collectorID,
            JSONKey.collectionName: self.collectionName,
            JSONKey.projectName: self.projectName,
            JSONKey.programName: self.programName,
            JSONKey.subjectIDS: self.subjectIDS,
            JSONKey.videoID: self.videoID,
            JSONKey.deviceType: self.deviceType,
            JSONKey.deviceIdentifier: self.deviceIdentifier,
            JSONKey.osVersion: self.osVersion,
            JSONKey.collectedDate: self.collectedDate,
            JSONKey.blurredFaces: self.blurredFaces,
            JSONKey.frameRate: self.frameRate,
            JSONKey.frameWidth: self.frameWidth,
            JSONKey.frameHeight: self.frameHeight,
            JSONKey.duration: self.duration,
            JSONKey.orientation: self.orientation,
            JSONKey.appVersion: self.appVersion,
            JSONKey.ipAddress: self.ipAddress,
            JSONKey.activityLongNames: self.activityLongNames,
            JSONKey.activityShortNames: self.activityShortNames,
//            JSONKey.uploadedDate: self.uploadedDate
        ]
        return dic
    }
}

struct Metadata2: Codable {
    var projectID: String!
    var collectionID, collectorID, collectionName, projectName, programName: String?
    var subjectIDS: [String]?
    var videoID: String
    var deviceType: String?
    var deviceIdentifier: String?
    var osVersion: String?
    var collectedDate: String?
    var blurredFaces: Int?
    var frameRate, frameWidth, frameHeight, duration: Double?
    var orientation: String
    var appVersion: String?
    var ipAddress: String?
    let activityLongNames: String?
    let activityShortNames: String?
    let uploadedDate: String?
    
    enum CodingKeys: String, CodingKey {
        case projectID = "project_id"
        case collectionID = "collection_id"
        case collectorID = "collector_id"
        case collectionName = "collection_name"
        case projectName = "project_name"
        case programName = "program_name"
        case subjectIDS = "subject_ids"
        case videoID = "video_id"
        case deviceType = "device_type"
        case deviceIdentifier = "device_identifier"
        case osVersion = "os_version"
        case collectedDate = "collected_date"
        case blurredFaces = "blurred_faces"
        case frameRate = "frame_rate"
        case frameWidth = "frame_width"
        case frameHeight = "frame_height"
        case appVersion = "app_version"
        case ipAddress = "ipAddress"
        case activityLongNames = "category"
        case activityShortNames = "shortname"
        case uploadedDate = "uploaded_date"
        case duration, orientation
    }
    
    private struct JSONKey {
        static let projectID = "project_id"
        static let collectionID = "collection_id"
        static let collectorID = "collector_id"
        static let collectionName = "collection_name"
        static let projectName = "project_name"
        static let programName = "program_name"
        static let subjectIDS = "subject_ids"
        static let videoID = "video_id"
        static let deviceType = "device_type"
        static let deviceIdentifier = "device_identifier"
        static let osVersion = "os_version"
        static let collectedDate = "collected_date"
        static let blurredFaces = "blurred_faces"
        static let frameRate = "frame_rate"
        static let frameWidth = "frame_width"
        static let frameHeight = "frame_height"
        static let duration = "duration"
        static let orientation = "orientation"
        static let appVersion = "app_version"
        static let ipAddress = "ipAddress"
        static let activityLongNames = "category"
        static let activityShortNames = "shortname"
        static let uploadedDate = "uploaded_date"
        
    }
    
    
    public func convertToDictionary() -> [String : Any] {
        let dic: [String: Any] = [
            JSONKey.projectID: self.projectID ?? "",
            JSONKey.collectionID: self.collectionID ?? "",
            JSONKey.collectorID: self.collectorID ?? "",
            JSONKey.collectionName: self.collectionName ?? "",
            JSONKey.projectName: self.projectName ?? "",
            JSONKey.programName: self.programName ?? "",
            JSONKey.subjectIDS: self.subjectIDS ?? "",
            JSONKey.videoID: self.videoID,
            JSONKey.deviceType: self.deviceType ?? "",
            JSONKey.deviceIdentifier: self.deviceIdentifier ?? "",
            JSONKey.osVersion: self.osVersion ?? "",
            JSONKey.collectedDate: self.collectedDate ?? "",
            JSONKey.blurredFaces: self.blurredFaces ?? "",
            JSONKey.frameRate: self.frameRate ?? "",
            JSONKey.frameWidth: self.frameWidth ?? "",
            JSONKey.frameHeight: self.frameHeight ?? "",
            JSONKey.duration: self.duration ?? "",
            JSONKey.orientation: self.orientation,
            JSONKey.appVersion: self.appVersion ?? "",
            JSONKey.ipAddress: self.ipAddress ?? "",
            JSONKey.activityLongNames: self.activityLongNames ?? "",
            JSONKey.activityShortNames: self.activityShortNames ?? "",
            JSONKey.uploadedDate: self.uploadedDate ?? ""
            
        ]
        return dic
    }
}

// MARK: - Object
struct Object: Codable {
    let label: String
    var boundingBox: [BoundingBox]

    enum CodingKeys: String, CodingKey {
        case label
        case boundingBox = "bounding_box"
    }
    
    private struct JSONKey {
        static let label = "label"
        static let boundingBox = "bounding_box"
    }
    
    public func convertToDictionary() -> [String : Any] {
        let dic: [String: Any] = [
            JSONKey.label: self.label,
            JSONKey.boundingBox: self.boundingBox.map({$0.convertToDictionary()})
        ]
        return dic
    }
}

// MARK: - BoundingBox
struct BoundingBox: Codable {
    var frame: Frame
    var frameIndex: Int

    init(zframe: Frame, index: Int) {
        frame = zframe
        frameIndex = index
    }
    
    enum CodingKeys: String, CodingKey {
        case frame
        case frameIndex = "frame_index"
    }
    
    public func convertToDictionary() -> [String : Any] {
        let frame = ["x": self.frame.x, "y": self.frame.y, "width": self.frame.width, "height": self.frame.height]
        let dic: [String: Any] = ["frame": frame, "frame_index": self.frameIndex]
        return dic
    }
}

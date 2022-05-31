//
//  Constants.swift
//  STR
//
//  Created by Srujan on 10/06/19.
//  
//

import UIKit
import Foundation


// Completion Handlet`

typealias statusHandler = (_ status: Bool) -> Void
typealias statusErrorHandler = (_ status: Bool, _ error: ApiError?) -> Void
//typealias handler = (_ data:Data?,_ status : Bool,_ error :ApiError?)->()


// Messages and Strings

let kUploadCompleted = "Upload Complete"
let kUpdateCompleted = "Update Complete"
let kExitCollection = "Exit this collection?"
let kExitEditVideo = "Are you sure you want to exit the editor?"
let kEndActivity = "Are you sure want to end this Activity?"
let kDiscardDraftedVideo = "Retake this collection?"
let kDiscardDraftedConsentVideoAtSetUp = "Retake this consent video?"
let kErrorProcessingVideo = "Error Occured While Processing Video"
let kRecaptureVideo = "Recapture Video"
let kExportFailed = "Export Failed"
let kExportFailedMessage = "Go to Setting and allow this app to use gallery to save video"
let kDeleteCompleted = "Delete Completed"
let kDownloadFailed = "Download Failed"
let kNoActivities = "Your collection has no activities. Submit?"
let kRetake = "Retake Video"
let kReEditActivity = "Re-Edit Activity"
let kUploadFailed = "Upload Failed"
let kReSubmit = "You resubmit the video"
let kTrainingVideoDataMissing = "Training video for this collection is under progress"
let kExitConsent = "Exit consent"

let kConsentVideoMaxDuration = 15
let kCollectionVideoMaxDuration = 30
// Applies scale value on existing zoom
let kEditVideoZoomScaleValue: CGFloat = 0.5
// Max zoom level eg: zoom can be applied 4 times
let kEditVideoMaxZoomLevel: CGFloat = 4

//MARK: DropBox
let kDropBoxAppKey = ""

//MARK: PayPal
let kPayPalClientID = ""
let kPayPalSecret = ""

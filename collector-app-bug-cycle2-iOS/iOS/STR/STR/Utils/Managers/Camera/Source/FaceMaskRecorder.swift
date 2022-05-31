//
//  FaceMarkRecorder.swift
//  STR
//
//  Created by Srujan on 07/01/20.
//  
//

import AVFoundation
import UIKit
import CoreGraphics
import GLKit

public struct RenderSettings {
    
    //MARK:- Public properties
    
    /// Video Size
    public let videoSize : CGSize = {
        print("2o---")
        if VideoVariables.deviceOrientationRawValue < 3  {
            return CGSize(width: 1080, height: 1920)
        }
        return CGSize(width: 1920, height: 1080)
    }()
    
    /// frames per second
    public let fps: Int32 = 30
    
    /// Video codec
    public let avCodecKey = AVVideoCodecType.h264
    
    /// Video File Type
    public let fileType = AVFileType.mp4
    
    /// file Extension computed property
    var videoFileNameExt : String {
        
        switch fileType {
        case .mov :
            return ".mov"
            
        case .mp4 :
            return ".mp4"
            
        case .m4v :
            return ".m4v"
            
        default : return ".mov"
            
        }
    }
    
    let outputSessionPreset = AVCaptureSession.Preset.medium
    
    
    var sourceBufferAttributes: [String: Any] {
        return [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA ,
            kCVPixelBufferWidthKey as String: videoSize.width  ,
            kCVPixelBufferHeightKey as String: videoSize.height ,
            kCVPixelFormatOpenGLESCompatibility as String: true
        ]
    }
    
    /// Settings for video output
    var outputSettings: [String: Any] {
        return [
            AVVideoCodecKey: avCodecKey,
            AVVideoWidthKey: videoSize.width,
            AVVideoHeightKey: videoSize.height,
            AVVideoCompressionPropertiesKey : [
                AVVideoAverageBitRateKey : 2300000,
            ]
        ]
    }

    var outputURL: URL? {
        get{
            let path = Utilities.getFileUrlStringFromCache(fileName: "video.mp4")
            let newPath = URL(fileURLWithPath: path)            
            Utilities.removeFileFromCache(fileName: "video.mp4")
            return newPath
        }
    }
    
}

extension FileManager {
    func clearTmpDirectory() {
        do {
            let tmpDirectory = try contentsOfDirectory(atPath: NSTemporaryDirectory())
            try tmpDirectory.forEach {[unowned self] file in
                let path = String(format: "%@%@", NSTemporaryDirectory(), file)
                try self.removeItem(atPath: path)
            }
        } catch {
            print(error)
        }
    }
}

protocol FaceBlurRecorderDataSource: class {
    func subjectFrameCurrentRect() -> CGRect
}

final public class VideoRecorder: NSObject {
    
    /// Running AVCaptureSession
    private var captureSession : AVCaptureSession!
    
    /// Current Status of Recording
    fileprivate var isRecording = false
    
    /// CameraControllerDelegate
    weak var delegate : VideoControllerDelegate?
    weak var datasource: FaceBlurRecorderDataSource?
    
    /// Video Settings
    fileprivate lazy var videoSettings = RenderSettings()
    
    /// AV Asset writer
    fileprivate var assetWriter: AVAssetWriter!
    
    /// Video Writer Input
    fileprivate var videoWriterInput: AVAssetWriterInput!
    
    /// Start Time
    fileprivate var sessionAtSourceTime: CMTime?
    
    fileprivate var devicePosition: VideoController.CameraSelection = .front
    
    ///
    let sessionQueue: DispatchQueue
    
    var frameIndex = -1
    
    /// Video Data Output
    fileprivate let videoDataOutput : AVCaptureVideoDataOutput = {
        let videoOutput = AVCaptureVideoDataOutput()
        videoOutput.alwaysDiscardsLateVideoFrames = false
        videoOutput.videoSettings = [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA
        ]
        return videoOutput
    }()
    
     let videoQueue = DispatchQueue(label: "videoOutput")

    /// Pixel Buffer Writer
    fileprivate var videoWriterInputPixelBufferAdaptor: AVAssetWriterInputPixelBufferAdaptor!
    
    /// Video Color Space
    fileprivate lazy var sDeviceRgbColorSpace = CGColorSpaceCreateDeviceRGB()
    
    ///
    fileprivate lazy var bitmapInfo = CGBitmapInfo.byteOrder32Little
        .union(CGBitmapInfo(rawValue: CGImageAlphaInfo.noneSkipFirst.rawValue))
    
    /// Face Mask Recorder Errors
    private enum VideoRecorder : Error {
        case outputUrlNull
        case videoWriterNotAvailable
        case wrongVideoInput
        case wrongAudioInput
    }
    
    /**
     Only Initializer fot the class.
     
     - Parameter session: Current running capture session
     - Parameter delegate: CameraControllerDelegate for callbacks
     
     */
    init(session :AVCaptureSession,
         delegate : VideoControllerDelegate?,
         sessionQueue: DispatchQueue) {
        self.captureSession = session
        self.delegate = delegate
        self.sessionQueue = sessionQueue
        super.init()
        self.didStartRecording()
    }
    
    private func didStartRecording() {
        sessionQueue.async { [weak self] in
            do {
                try self?.addVideoOutputs()
            }
            catch {
                print(error as Any)
            }
        }
    }
    
    private func didStopRecording() {
        sessionQueue.async { [weak self] in
            do {
                try self?.removeVideoOutputs()
            }
            catch {
                print(error as Any)
            }
        }
    }
    
    /// Removing outputs from session
    deinit {
        self.captureSession.beginConfiguration()
        self.captureSession.removeOutput(videoDataOutput)
        self.captureSession.commitConfiguration()
        print("deallocated \(String(describing: self))")
    }
    
}

extension VideoRecorder {
    
    /// Add AVCaptureVideoDataOutput to current capture session
    final private func addVideoOutputs() throws {
        
        //1.
        guard captureSession.isRunning else {
            throw CameraControllerError.captureSessionIsMissing
        }
        self.captureSession.beginConfiguration()
        
        // Add video output
        if captureSession.canAddOutput(videoDataOutput) {
            videoDataOutput.setSampleBufferDelegate(self, queue: videoQueue)
            captureSession.addOutput(videoDataOutput)
        }
        
        self.captureSession.commitConfiguration()
    }
    
    final private func removeVideoOutputs() throws {
        
        // 1.
        guard captureSession.isRunning else {
            throw CameraControllerError.captureSessionIsMissing
        }
        
        self.captureSession.beginConfiguration()
    
        // Remove video output
        videoDataOutput.setSampleBufferDelegate(nil, queue: videoQueue)
        captureSession.removeOutput(videoDataOutput)
        
        self.captureSession.commitConfiguration()
        
    }
    
 
}

//MARK:- Audio ,Video Sample Buffer output Delegates
extension VideoRecorder : AVCaptureVideoDataOutputSampleBufferDelegate {
    
    //There is only one same method for both of these delegates
    public func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        
        guard CMSampleBufferDataIsReady(sampleBuffer) else { return }
        
                if output == videoDataOutput {
                    /* TODO: Fix Issue with Landscape recording */
                    _ = AVCaptureVideoOrientation(rawValue: VideoVariables.deviceOrientationRawValue)
                    connection.videoOrientation = AVCaptureVideoOrientation(rawValue: VideoVariables.deviceOrientationRawValue) ?? .portrait //.portrait
        //            print("connection.videoOrientation---\(VideoVariables.deviceOrientationRawValue)---\(UIDevice.current.orientation.rawValue)")
                }
        
        
        //Overlay means processing on the images, not audio
//        if output == videoDataOutput {
//            //Important: Correct your video orientation from your device orientation
//            switch UIDevice.current.orientation {
//            case .landscapeRight:
//                connection.videoOrientation = .landscapeLeft
//            case .landscapeLeft:
//                connection.videoOrientation = .landscapeRight
//            case .portrait:
//                connection.videoOrientation = .portrait
//            case .portraitUpsideDown:
//                connection.videoOrientation = .portraitUpsideDown
//            default:
//                connection.videoOrientation = .portrait //Make `.portrait` as default (should check will `.faceUp` and `.faceDown`)
//            }
//        }
        DispatchQueue.main.async {
            self.delegate?.cameraController(self, didVideoOutputSampleBufferChange: true)
        }
        let writable = canWrite()
        
        if writable, sessionAtSourceTime == nil {
            // start writing
            sessionAtSourceTime = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
            assetWriter.startSession(atSourceTime: sessionAtSourceTime!)
            print("Started")
            self.frameIndex = -1
        }
        
        if output == videoDataOutput , writable, videoWriterInput.isReadyForMoreMediaData {
            videoWriterInput.append(sampleBuffer)
            frameIndex += 1
            self.delegate?.cameraController(self, didChangeFrame: frameIndex)
        }
    }
    
    public func captureOutput(_ output: AVCaptureOutput, didDrop sampleBuffer: CMSampleBuffer,
                              from connection: AVCaptureConnection) {
        //print("Dropped")
    }
}

extension VideoRecorder {
    
    fileprivate func canWrite() -> Bool {
        return isRecording
            && assetWriter != nil
            && assetWriter.status == .writing
    }
    
    /// Whenever you want to record new video, you have to re-setup the writer again!
    func setUpWriter() throws {
        
        //1.
        guard let outputUrl = self.videoSettings.outputURL else {
            throw VideoRecorder.outputUrlNull
        }
        //2.
        do {
            //1. Creating writer for the fileType
            assetWriter = try AVAssetWriter(outputURL: outputUrl, fileType: self.videoSettings.fileType)
        }
        catch {
            debugPrint(error.localizedDescription)
        }
        //3.
        guard let videoWriter = self.assetWriter else {
            throw VideoRecorder.videoWriterNotAvailable
        }
        
        
        //4. Add video input
        videoWriterInput = AVAssetWriterInput(mediaType: AVMediaType.video, outputSettings: self.videoSettings.outputSettings)
        
        videoWriterInput.expectsMediaDataInRealTime = true
        
        if videoWriter.canAdd(videoWriterInput) {
            videoWriter.add(videoWriterInput)
        } else {
            throw VideoRecorder.wrongVideoInput
        }
        
        //5. Add Audio input here if req.
        
        //6. We're using AVAssetWriterInputPixelBufferAdaptor to write rendered videos
        videoWriterInputPixelBufferAdaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: videoWriterInput, sourcePixelBufferAttributes: self.videoSettings.sourceBufferAttributes)
        
        //7.
        videoWriter.startWriting()
    }
    
}


// MARK: - To start / stop Recording
extension VideoRecorder {
    
    /// Start Recording
    func start(completion : @escaping (_ status : Bool) -> Void) {
        
        guard !isRecording else { return }
        isRecording = true
        sessionAtSourceTime = nil
        
        // Whenever we start recording , need to create writer again
        do {
            try setUpWriter()
            DispatchQueue.main.async {
                completion(true)
            }
        } catch {
            print(error as Any)
            DispatchQueue.main.async {
                completion(false)
            }
        }
    }
    
    /// Stop Recording
    func stop(_ completion : @escaping (_ url : URL?)-> Void) {
        
        guard isRecording else { return }
        isRecording = false
        assetWriter.finishWriting { [unowned self] in
            
            self.sessionAtSourceTime = nil
            let url = self.assetWriter.outputURL
            let asset = AVAsset(url: url)
            print("Encode duration: ",CMTimeGetSeconds(asset.duration))
            print("encode fps: ", Double(self.frameIndex)/Double(CMTimeGetSeconds(asset.duration)))
            // To remove the layers from view
            DispatchQueue.main.async { [weak self] in
                guard let self = self else {return}
                self.delegate?.cameraController(self, didUpdate: nil, bufferFrameRect : nil)
            }
            print("frame index: ",self.frameIndex)
            //self.didStopRecording()
            completion(url)
        }
        
    }
    
    func cancel() {
        isRecording = false
        assetWriter.cancelWriting()
    }
    
    func pause() {
        isRecording = false
    }
    
    func resume() {
        isRecording = true
    }
    
}

// Deep copy the pixel buffer
extension CVPixelBuffer {
    func copy() -> CVPixelBuffer {
        precondition(CFGetTypeID(self) == CVPixelBufferGetTypeID(), "copy() cannot be called on a non-CVPixelBuffer")
        
        var _copy : CVPixelBuffer?
        CVPixelBufferCreate(
            kCFAllocatorDefault,
            CVPixelBufferGetWidth(self),
            CVPixelBufferGetHeight(self),
            CVPixelBufferGetPixelFormatType(self),
            nil,
            &_copy)
        
        guard let copy = _copy else { fatalError() }
        
        CVPixelBufferLockBaseAddress(self, CVPixelBufferLockFlags.readOnly)
        CVPixelBufferLockBaseAddress(copy, CVPixelBufferLockFlags(rawValue: 0))
        
        
        let copyBaseAddress = CVPixelBufferGetBaseAddress(copy)
        let currBaseAddress = CVPixelBufferGetBaseAddress(self)
        
        
        //copy the content of pixel buffer to rendered pixel buffer
        // memcpy(*str1,*str2,size_t n)
        // *str1 -> destination pointer , *str2 -> source ,size_t -> number of bytes to be copied.
        memcpy(copyBaseAddress, currBaseAddress, CVPixelBufferGetDataSize(self))
        
        //  CVPixelBufferUnlockBaseAddress(copy, CVPixelBufferLockFlags(rawValue: 0))
        //    CVPixelBufferUnlockBaseAddress(self, CVPixelBufferLockFlags.readOnly)
        
        
        return copy
    }
}


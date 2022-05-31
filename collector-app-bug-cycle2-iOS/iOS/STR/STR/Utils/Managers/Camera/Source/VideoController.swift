//
//  EasyCamViewController.swift
//  CIFaceMask
//
//  Created by Tushar on 8/13/18.
//  Copyright © 2018 BTC Soft. All rights reserved.
//

import AVFoundation
import UIKit


/// A UIViewController Camera View Subclass

open class VideoController : UIViewController {
    
    // MARK: Enumeration Declaration
    
    /// Enumeration for Camera Selection
    
    public enum CameraSelection {
        
        /// Camera on the back of the device
        case rear
        
        /// Camera on the front of the device
        case front
    }
    
    /// Enumeration for video quality of the capture session. Corresponds to a AVCaptureSessionPreset
    
    public enum VideoQuality {
        
        /// AVCaptureSessionPresetHigh
        case high
        
        /// AVCaptureSessionPresetMedium
        case medium
        
        /// AVCaptureSessionPresetLow
        case low
        
        /// AVCaptureSessionPreset352x288
        case resolution352x288
        
        /// AVCaptureSessionPreset640x480
        case resolution640x480
        
        /// AVCaptureSessionPreset1280x720
        case resolution1280x720
        
        /// AVCaptureSessionPreset1920x1080
        case resolution1920x1080
        
        /// AVCaptureSessionPreset3840x2160
        case resolution3840x2160
        
        /// AVCaptureSessionPresetiFrame960x540
        case iframe960x540
        
        /// AVCaptureSessionPresetiFrame1280x720
        case iframe1280x720
    }
    
    /// Result from Capture Session Setup
    
    fileprivate enum SessionSetupResult {
        case success // success
        case notAuthorized //User Denied
        case configurationFailed // Unknown issue
    }
    
    //MARK :- Public Properties
    
    /// Public Camera Delegate for the Custom View Controller Subclass
    
    public weak var videoDelegate: VideoControllerDelegate?
    
    /// Video Recording Type
    
    public var isVideoRecordingWithBlur = false
    
    /// Maxiumum video duration if CamButton is used
    
    public var maximumVideoDuration : Double     = 0.0
    
    /// Video capture quality
    
    public var videoQuality : VideoQuality  = .high
    
    /// Sets whether flash is enabled for photo and video capture
    
    public var flashEnabled                      = false
    
    /// Sets whether Pinch to Zoom is enabled for the capture session
    
    public var pinchToZoom                       = true
    
    /// Sets the maximum zoom scale allowed during gestures gesture
    
    public var maxZoomScale: CGFloat                = CGFloat.greatestFiniteMagnitude
    
    /// Sets whether Tap to Focus and Tap to Adjust Exposure is enabled for the capture session
    
    public var tapToFocus                        = true
    
    /// Sets whether the capture session should adjust to low light conditions automatically
    ///
    /// Only supported on iPhone 5 and 5C
    
    public var lowLightBoost                     = true
    
    /// Set whether Cam should allow background audio from other applications
    
    public var allowBackgroundAudio              = true
    
    /// Sets whether a double tap to switch cameras is supported
    
    public var doubleTapCameraSwitch            = true
    
    /// Sets whether swipe vertically to zoom is supported
    
    public var swipeToZoom                     = true
    
    /// Sets whether swipe vertically gestures should be inverted
    
    public var swipeToZoomInverted             = false
    
    /// Set default launch camera
    
    public var defaultCamera  :CameraSelection  = .rear
    
    /// Sets wether the taken photo or video should be oriented according to the device orientation
    
    public var shouldUseDeviceOrientation      = false
    
    /// Sets whether or not View Controller supports auto rotation
    
    public var allowAutoRotate                = false
    
    /// Specifies the [videoGravity] for the preview layer.
    public var videoGravity                   : CameraVideoGravity = .resizeAspectFill
    
    /// Sets whether or not video recordings will record audio
    /// Setting to true will prompt user for access to microphone on View Controller launch.
    public var audioEnabled                   = true
    
    /// Public access to Pinch Gesture
    fileprivate(set) public var pinchGesture  : UIPinchGestureRecognizer!
    
    /// Public access to Pan Gesture
    //fileprivate(set) public var panGesture    : UIPanGestureRecognizer!
    
    
    // MARK: Public Get-only Variable Declarations
    
    /// Returns true if video is currently being recorded
    
    private(set) public var isVideoRecording      = false
    
    public var videoTimerEnds = false
    
    /// Returns the CameraSelection corresponding to the currently utilized camera
    
    private(set) public var currentCamera        = CameraSelection.front
    
    /// Face Mask Recorder
    private(set) var faceMaskRecorder : VideoRecorder?
    
    
    // MARK: Private Constant Declarations
    
    /// Current Capture Session
    
    public let session                           = AVCaptureSession()
    
    /// Serial queue used for setting up session
    
    fileprivate let sessionQueue                 = DispatchQueue(label: "session queue", attributes: [])
    
    // MARK: Private Variable Declarations
    
    /// Variable for storing current zoom scale
    
    fileprivate var zoomScale                    = CGFloat(1.0)
    
    /// Variable for storing initial zoom scale before Pinch to Zoom begins
    
    fileprivate var beginZoomScale               = CGFloat(1.0)
    
    /// Variable to store result of capture session setup
    
    fileprivate var setupResult                  = SessionSetupResult.success
    
    /// BackgroundID variable for video recording
    
    fileprivate var backgroundRecordingID        : UIBackgroundTaskIdentifier? = nil
    
    /// Movie File Output variable
    
    fileprivate var movieFileOutput              : AVCaptureMovieFileOutput?
    
    /// Photo File Output variable
    
    fileprivate var photoFileOutput              : AVCapturePhotoOutput?
    
    /// Current Video Device reference
    
    fileprivate var currentVideoDevice                : AVCaptureDevice? {
        switch currentCamera {
        case .rear : return rearCamera
        case .front : return frontCamera
        }
    }
    
    /*  fileprivate var currentVideoDevice1                : AVCaptureDevice? {
     switch currentCamera {
     case .rear : return rearCamera
     case .front : return frontCamera
     }
     }*/
    
    /// Front camera
    fileprivate var frontCamera                  : AVCaptureDevice?
    
    /// Back Camera
    fileprivate var rearCamera                   : AVCaptureDevice?
    
    ///Current Device Inputs computed property
    fileprivate var currentVideoDeviceInput           : AVCaptureDeviceInput?{
        
        set {
            switch currentCamera {
            case .rear: self.rearCameraInput = newValue
            case .front : self.frontCameraInput = newValue
            }
        }
        
        get {
            switch currentCamera {
            case .rear :
                if let rearCameraInput = self.rearCameraInput {
                    return rearCameraInput
                } else {
                    return try? AVCaptureDeviceInput(device: rearCamera!)
                }
            case .front :
                if let frontCameraInput = self.frontCameraInput {
                    return frontCameraInput
                }else {
                    return try? AVCaptureDeviceInput(device: frontCamera!)
                }
                
            }
        }
    }
    
    /// Front camera inputs
    fileprivate var frontCameraInput             : AVCaptureDeviceInput?
    
    /// Back Camera inputs
    fileprivate var rearCameraInput              : AVCaptureDeviceInput?
    
    
    /// PreviewView for the capture session
    
    fileprivate var previewLayer                 : PreviewView!
    
    /// Pan Translation
    
    fileprivate var previousPanTranslation       : CGFloat = 0.0
    
    /// Last changed orientation
    
    fileprivate var deviceOrientation            : UIDeviceOrientation?
    
    var pinchIndex = 0
    var pinchMode = ""
    var lastPinchMode = ""
    public var lock: Bool = false
    public var pinchModeChange: Bool = false
    public var lockedPinchMode = ""
    
    var subjectCurrentFrame: CGRect?
    
    var safeAreaValueForVideo: CGFloat = UIDevice.current.hasNotch ? 73 : 0
    var screenHeightWithSafeArea: CGFloat = UIDevice.current.hasNotch ? (73 * 2) : 0
    
    /// Disable view autorotation for forced portrait recorindg
    
    override open var shouldAutorotate: Bool {
        return allowAutoRotate
    }
    
    // MARK: Overidden Methods
    
    /// ViewDidLoad Implementation
    
    override open func viewDidLoad() {
        super.viewDidLoad()
        
        previewLayer = PreviewView(frame: view.frame, videoGravity: videoGravity)
        previewLayer.center = view.center
        view.addSubview(previewLayer)
        view.sendSubviewToBack(previewLayer)
        
        if UIDevice.current.hasNotch {
            
            self.view.backgroundColor = .black
            
            previewLayer.translatesAutoresizingMaskIntoConstraints = false
            let guide = view.safeAreaLayoutGuide
            let valscreenIphone11Height =  ScreenSize.SCREEN_HEIGHT
            var valTopAnchor: CGFloat = 0
            var valBottomAnchor: CGFloat = 0
            if valscreenIphone11Height == 896 {
                valTopAnchor = UIDevice.current.hasNotch ? 29 + 7 : 0
                valBottomAnchor = UIDevice.current.hasNotch ? 39 + 7 : 0
            }
            else {
                valTopAnchor = UIDevice.current.hasNotch ? 29 : 0
                valBottomAnchor = UIDevice.current.hasNotch ? 39 : 0
            }
            
            NSLayoutConstraint.activate([
                previewLayer.leadingAnchor.constraint(equalTo: guide.leadingAnchor),
                previewLayer.trailingAnchor.constraint(equalTo: guide.trailingAnchor),
                previewLayer.topAnchor.constraint(equalTo: guide.topAnchor, constant: valTopAnchor),
                guide.bottomAnchor.constraint(equalTo: previewLayer.bottomAnchor, constant: valBottomAnchor)
            ])
        }
        
        // Add Gesture Recognizers
        addGestureRecognizers()
        
        previewLayer.session = session
        
        testDevicesAuth()
        
        sessionQueue.async { [unowned self] in
            self.configureSession()
        }
        
        if ScreenSize.SCREEN_HEIGHT == 896 {
            safeAreaValueForVideo = UIDevice.current.hasNotch ? 73 + 7 : 0
            screenHeightWithSafeArea = UIDevice.current.hasNotch ? ((73 + 7) * 2) : 0
        } else {
            safeAreaValueForVideo = UIDevice.current.hasNotch ? 73 : 0
            screenHeightWithSafeArea = UIDevice.current.hasNotch ? (73 * 2) : 0
        }
        
    }
    
    /// ViewDidAppear(_ animated:) Implementation
    
    override open func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        self.showProgress()
        // Subscribe to device rotation notifications
        if shouldUseDeviceOrientation {
            self.deviceOrientation = UIDevice.current.orientation
        }
        
        // Set background audio preference
        
        setBackgroundAudioPreference()
        
        let boolViewLoad = viewLoad()
        if boolViewLoad {
            self.hideProgress()
        }
    }
    
    func viewLoad() -> Bool {
        switch self.setupResult {
        case .success:
            // Begin Session
            self.session.startRunning()
            
            if self.isVideoRecordingWithBlur { // Initializing the facemaskRecoder here once session starts working
                self.faceMaskRecorder = VideoRecorder(session: self.session,
                                                      delegate: self.videoDelegate, sessionQueue: self.sessionQueue)
                self.faceMaskRecorder?.datasource = self
            }
            
            // Preview layer video orientation can be set only after the connection is created
            DispatchQueue.main.async { [unowned self] in
                self.previewLayer.videoPreviewLayer.connection?.videoOrientation = self.getPreviewLayerOrientation()
            }
            
        case .notAuthorized:
            // Prompt to App Settings
            self.promptToAppSettings()
        case .configurationFailed:
            // Unknown Error
            DispatchQueue.main.async(execute: { [unowned self] in
                let message = NSLocalizedString("Unable to capture media", comment: "Alert message when something goes wrong during capture session configuration")
                let alertController = UIAlertController(title: "Alert", message: message, preferredStyle: .alert)
                alertController.addAction(UIAlertAction(title: NSLocalizedString("OK", comment: "Alert OK button"), style: .cancel, handler: nil))
                self.present(alertController, animated: true, completion: nil)
            })
        }
        return true
    }
    
    /// ViewDidDisappear(_ animated:) Implementation
    override open func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        
        // If session is running, stop the session
        if session.isRunning == true {
            self.session.stopRunning()
        }
        
    }
    
    /// Orientation management
    open override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        //        print(previewLayer)
        if !UIDevice.current.orientation.isFlat {
            self.deviceOrientation = UIDevice.current.orientation
        }
        
    }
    
    deinit {
        print("deallocated \(String(describing: self))")
    }
    
    // MARK: ViewDidLayoutSubviews
    
    /// ViewDidLayoutSubviews() Implementation
    private func updatePreviewLayer(layer: AVCaptureConnection, orientation: AVCaptureVideoOrientation) {
        
        // print(previewLayer)
        layer.videoOrientation = orientation
        
        previewLayer.frame = self.view.bounds
    }
    
    // MARK: Public Functions
    
    public func toggleTorch(on: Bool) {
        guard let device = currentVideoDevice else { return }
        
        if device.hasTorch {
            do {
                try device.lockForConfiguration()
                
                if on == true {
                    device.torchMode = .on
                } else {
                    device.torchMode = .off
                }
                
                device.unlockForConfiguration()
            } catch {
                print("Torch could not be used")
            }
        } else {
            print("Torch is not available")
        }
    }
    
    public func torchMode() -> AVCaptureDevice.TorchMode {
        guard let device = currentVideoDevice else { return .off }
        return device.torchMode
    }
    
    public func isTorchAvailable(_ output: @escaping (Bool) -> Void) {
        
        guard let device = currentVideoDevice else {
            output(false)
            return
        }
        
        if device.hasTorch {
            output(true)
        } else {
            output(false)
            print("Torch is not available")
        }
    }
    
    /**
     
     Begin recording video of current session
     
     CameraControllerDelegate function CameraControllerDidBeginRecordingVideo() will be called
     
     */
    
    public func startVideoRecording() {
        if isVideoRecordingWithBlur && self.faceMaskRecorder != nil {
            removeVideoAndBlurVideoFromCache()
            
            self.isVideoRecording = true
            self.faceMaskRecorder?.start(completion: { [unowned self] (status) in
                if status == true {
                    self.videoDelegate?.cameraController(self, didBeginRecordingVideo: self.currentCamera, zView: self.previewLayer)
                }
            })
            
            
        } else if !isVideoRecordingWithBlur {
            
            guard let movieFileOutput = self.movieFileOutput else {
                return
            }
            
            if currentCamera == .rear && flashEnabled == true {
                // turn on the torch
            }
            
            sessionQueue.async { [unowned self] in
                if !movieFileOutput.isRecording {
                    if UIDevice.current.isMultitaskingSupported {
                        self.backgroundRecordingID = UIApplication.shared.beginBackgroundTask(expirationHandler: nil)
                    }
                    
                    // Update the orientation on the movie file output video connection before starting recording.
                    let movieFileOutputConnection = self.movieFileOutput?.connection(with: AVMediaType.video)
                    
                    
                    //flip video output if front facing camera is selected
                    if self.currentCamera == .front {
                        movieFileOutputConnection?.isVideoMirrored = true
                    }
                    
                    movieFileOutputConnection?.videoOrientation = self.getVideoOrientation() ?? .portrait
                    
                    // Start recording to a temporary file.
                    //let outputFileName = UUID().uuidString
                    //let outputFilePath = (NSTemporaryDirectory() as NSString).appendingPathComponent((outputFileName as NSString).appendingPathExtension("mov")!)
                    let filename = "video.mov"
                    if let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
                        
                        let fileURL = dir.appendingPathComponent(filename)
                        
                        movieFileOutput.startRecording(to: fileURL, recordingDelegate: self)
                        self.isVideoRecording = true
                        DispatchQueue.main.async { [unowned self] in
                            self.videoDelegate?.cameraController(self, didBeginRecordingVideo: self.currentCamera, zView: self.previewLayer)
                        }
                        
                        /*movieFileOutput.startRecording(to: URL(fileURLWithPath: outputFilePath), recordingDelegate: self)
                         self.isVideoRecording = true
                         DispatchQueue.main.async {
                         self.videoDelegate?.cameraController(self, didBeginRecordingVideo: self.currentCamera, zView: self.previewLayer)*/
                    }
                }
                else {
                    movieFileOutput.stopRecording()
                    self.isVideoRecording = false
                }
            }
            
        }
        
    }
    
    /**
     
     Stop video recording video of current session
     
     CameraControllerDelegate function CameraControllerDidFinishRecordingVideo() will be called
     
     When video has finished processing, the URL to the video location will be returned by CameraControllerDidFinishProcessingVideoAt(url:)
     
     */
    
    public func stopVideoRecording() {
        if isVideoRecording {
            if isVideoRecordingWithBlur && self.faceMaskRecorder != nil {
                
                self.isVideoRecording = false
                self.faceMaskRecorder?.stop({ (url) in
                    if let url = url {
                        DispatchQueue.main.async { [unowned self] in
                            self.videoDelegate?.cameraController(self, didFinishProcessVideoAt: url)
                            self.videoDelegate?.cameraController_enableButtonView()
                        }
                    }
                })
            } else {
                if self.movieFileOutput?.isRecording == true {
                    self.isVideoRecording = false
                    movieFileOutput!.stopRecording()
                    // turn off the torch if it's on
                    DispatchQueue.main.async { [unowned self] in
                        self.videoDelegate?.cameraController(self, didFinishRecordingVideo: self.currentCamera)
                        self.videoDelegate?.cameraController_enableButtonView()
                    }
                }
                else {
                    self.isVideoRecording = false //Do
                }
            }
        }
    }
    
    public func discardVideoRecording() {
        self.isVideoRecording = false
        self.faceMaskRecorder?.cancel()
    }
    
    /**
     
     Switch between front and rear camera
     
     CameraControllerDelegate function CameraControllerDidSwitchCameras(camera:  will be return the current camera selection
     
     */
    
    func switchCamera() throws {
        
        //1.
        guard isVideoRecording != true else {
            print("[Cam]: Switching between cameras while recording video is not supported")
            return
        }
        guard session.isRunning else { throw CameraControllerError.captureSessionIsMissing }
        
        //2.
        session.beginConfiguration()
        
        guard let currentDeviceInput = self.currentVideoDeviceInput , session.inputs.contains(currentDeviceInput) else {
            session.commitConfiguration()
            throw CameraControllerError.invalidOperation
        }
        
        let newPosition : CameraSelection = (self.currentCamera == .rear) ? .front : .rear
        
        session.removeInput(currentDeviceInput)
        
        self.currentCamera = newPosition
        
        sessionQueue.async { [unowned self] in
            
            if let newInputs = self.currentVideoDeviceInput {
                
                if self.session.canAddInput(newInputs) {
                    self.session.addInput(newInputs)
                    self.currentVideoDeviceInput = newInputs
                } else {
                    print(CameraControllerError.inputsAreInvalid)
                }
            }
            
            DispatchQueue.main.async { [unowned self] in
                self.videoDelegate?.cameraController(self, didSwitchCameras: self.currentCamera)
            }
            self.session.commitConfiguration()
        }
        
    }
    
    // MARK: Private Functions
    
    func removeVideoAndBlurVideoFromCache() {
        //Removing BlurredVideo.mp4 and Video.mp4
        let fileManager = FileManager()
        if fileManager.fileExists(atPath: Utilities.getFileUrlStringFromCache(fileName: "video.mp4")) {
            try? fileManager.removeItem(atPath: Utilities.getFileUrlStringFromCache(fileName: "video.mp4"))
        }
        if fileManager.fileExists(atPath: Utilities.getFileUrlStringFromCache(fileName: "videoBlurred.mp4")) {
            try? fileManager.removeItem(atPath: Utilities.getFileUrlStringFromCache(fileName: "videoBlurred.mp4"))
        }
        if fileManager.fileExists(atPath: Utilities.getFileUrlStringFromCache(fileName: "videoinfo.json")) {
            try? fileManager.removeItem(atPath: Utilities.getFileUrlStringFromCache(fileName: "videoinfo.json"))
        }
        
    }
    
    /// Test authorization status for Camera and Micophone
    private func testDevicesAuth() {
        
        switch AVCaptureDevice.authorizationStatus(for: AVMediaType.video){
        case .authorized:
            
            // already authorized
            break
        case .notDetermined:
            
            // not yet determined
            sessionQueue.suspend()
            AVCaptureDevice.requestAccess(for: AVMediaType.video, completionHandler: { [unowned self] granted in
                if !granted {
                    self.setupResult = .notAuthorized
                }
                self.sessionQueue.resume()
            })
        default:
            // already been asked. Denied access
            setupResult = .notAuthorized
        }
    }
    
    /// Configure session, add inputs and outputs
    
    fileprivate func configureSession() {
        guard setupResult == .success else {
            return
        }
        
        // Set default camera
        
        currentCamera = defaultCamera
        
        // begin configuring session
        
        session.beginConfiguration()
        addInputs()
        addOutputs()
        session.commitConfiguration()
    }
    
    /// Add inputs for Capture Session
    fileprivate func addInputs() {
        addVideoInput()
        addAudioInput()
    }
    
    
    fileprivate func addOutputs() {
        
        configureVideoPreset() // configure video recording quality , to be changed from configuration later
        
        if !isVideoRecordingWithBlur{
            configureVideoOutput() // Add AVCaptureMovieFileOutput to camera session or wait for session to start running than enable masking
        }
        configurePhotoOutput()
    }
    
    // Front facing camera will always be set to VideoQuality.high
    // If set video quality is not supported, videoQuality variable will be set to VideoQuality.high
    /// Configure image quality preset
    
    fileprivate func configureVideoPreset() {
        if currentCamera == .front {
            session.sessionPreset = videoInputPresetFromVideoQuality(quality: .high)
        } else {
            if session.canSetSessionPreset(videoInputPresetFromVideoQuality(quality: videoQuality)) {
                session.sessionPreset = videoInputPresetFromVideoQuality(quality: videoQuality)
            } else {
                session.sessionPreset = videoInputPresetFromVideoQuality(quality: .high)
            }
        }
    }
    
    /// Add Video Inputs
    
    fileprivate func addVideoInput() {
        
        //1. Obtaining and configuring the necessary capture devices.To represent the actual iOS device’s cameras
        func configureCaptureDevices() throws {
            
            //1.
            let session = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInWideAngleCamera], mediaType: AVMediaType.video, position: .unspecified)
            
            guard !session.devices.isEmpty else {
                throw CameraControllerError.noCamerasAvailable
            }
            
            //2.
            for camera in session.devices {
                
                switch camera.position {
                case .front : self.frontCamera = camera
                case .back :  self.rearCamera = camera
                default : continue
                    
                }
            }
            
            if let device = currentVideoDevice {
                do {
                    try device.lockForConfiguration()
                    if device.isFocusModeSupported(.continuousAutoFocus) {
                        device.focusMode = .continuousAutoFocus
                        if device.isSmoothAutoFocusSupported {
                            device.isSmoothAutoFocusEnabled = true
                        }
                    }
                    
                    if device.isExposureModeSupported(.continuousAutoExposure) {
                        device.exposureMode = .continuousAutoExposure
                    }
                    
                    if device.isWhiteBalanceModeSupported(.continuousAutoWhiteBalance) {
                        device.whiteBalanceMode = .continuousAutoWhiteBalance
                    }
                    
                    if device.isLowLightBoostSupported && lowLightBoost == true {
                        device.automaticallyEnablesLowLightBoostWhenAvailable = true
                    }
                    /*
                    let frameRate: Double = 20
                    guard let range = device.activeFormat.videoSupportedFrameRateRanges.first,
                        range.minFrameRate...range.maxFrameRate ~= frameRate
                        else {
                            print("Requested FPS is not supported by the device's activeFormat !")
                            device.unlockForConfiguration()
                            return
                    }
                    device.activeVideoMinFrameDuration = CMTimeMake(value: 1, timescale: Int32(frameRate))
                    device.activeVideoMaxFrameDuration = CMTimeMake(value: 1, timescale: Int32(frameRate))
                    */
                    device.unlockForConfiguration()
                } catch {
                    print("[Cam]: Error locking configuration")
                }
            }
            
            /*  if let device1 = currentVideoDevice1 {
             do {
             try device1.lockForConfiguration()
             if device1.isFocusModeSupported(.continuousAutoFocus) {
             device1.focusMode = .continuousAutoFocus
             if device1.isSmoothAutoFocusSupported {
             device1.isSmoothAutoFocusEnabled = true
             }
             }
             
             if device1.isExposureModeSupported(.continuousAutoExposure) {
             device1.exposureMode = .continuousAutoExposure
             }
             
             if device1.isWhiteBalanceModeSupported(.continuousAutoWhiteBalance) {
             device1.whiteBalanceMode = .continuousAutoWhiteBalance
             }
             
             if device1.isLowLightBoostSupported && lowLightBoost == true {
             device1.automaticallyEnablesLowLightBoostWhenAvailable = true
             }
             
             device1.unlockForConfiguration()
             } catch {
             print("[Cam]: Error locking configuration")
             }
             }*/
        }
        
        //3.takes capture devices and connect them to capture session
        func configureDeviceInputs() throws {
            
            if let device = currentVideoDevice/*, let device1 = currentVideoDevice1*/ {
                
                do {
                    let videoDeviceInput = try AVCaptureDeviceInput(device: device)
                    
                    if session.canAddInput(videoDeviceInput) {
                        session.addInput(videoDeviceInput)
                        self.currentVideoDeviceInput = videoDeviceInput
                        
                    } else {
                        print("[Cam]: Could not add video device input to the session")
                        print(session.canSetSessionPreset(videoInputPresetFromVideoQuality(quality: videoQuality)))
                        setupResult = .configurationFailed
                        session.commitConfiguration()
                        throw CameraControllerError.inputsAreInvalid
                    }
                } catch {
                    print("[Cam]: Could not create video device input: \(error)")
                    setupResult = .configurationFailed
                    return
                }
                
                /* do {
                 let videoDeviceInput1 = try AVCaptureDeviceInput(device: device1)
                 
                 if session.canAddInput(videoDeviceInput1) {
                 session.addInput(videoDeviceInput1)
                 self.currentVideoDeviceInput = videoDeviceInput1
                 
                 } else {
                 print("[Cam]: Could not add video device input to the session")
                 print(session.canSetSessionPreset(videoInputPresetFromVideoQuality(quality: videoQuality)))
                 setupResult = .configurationFailed
                 session.commitConfiguration()
                 throw CameraControllerError.inputsAreInvalid
                 }
                 } catch {
                 print("[Cam]: Could not create video device input: \(error)")
                 setupResult = .configurationFailed
                 return
                 }*/
                
            } else {
                throw CameraControllerError.selectedCameraNotAvailable
            }
        }
        
        DispatchQueue(label: "prepare").async {
            do {
                try configureCaptureDevices()
                try configureDeviceInputs()
            }
            catch {
                print(error as Any)
            }
        }
        
    }
    
    /// Add Audio Inputs
    
    fileprivate func addAudioInput()  {
        guard audioEnabled == true else {
            return
        }
        do {
            //1. Setup your microphone
            let audioDevice = AVCaptureDevice.default(for: .audio)
            
            //2. Add microphone to your session
            if let audioDevice = audioDevice {
                
                let audioDeviceInput = try AVCaptureDeviceInput(device: audioDevice)
                if session.canAddInput(audioDeviceInput) {
                    session.addInput(audioDeviceInput)
                } else {
                    print("[cam] : Unable to add microphone input")
                }
                
            } else {
                print("[cam] : Unable to find any microphone")
            }
        } catch {
            print( error as Any)
        }
    }
    
    
    /// Configure Movie Output : Output for recording video,without SampleBuffer
    
    fileprivate func configureVideoOutput() {
        let movieFileOutput = AVCaptureMovieFileOutput()
        
        if self.session.canAddOutput(movieFileOutput) {
            self.session.addOutput(movieFileOutput)
            if let connection = movieFileOutput.connection(with: AVMediaType.video) {
                if connection.isVideoStabilizationSupported {
                    connection.preferredVideoStabilizationMode = .auto
                }
            }
            self.movieFileOutput = movieFileOutput
        }
    }
    
    /// Configure Photo Output
    
    fileprivate func configurePhotoOutput() {
        self.photoFileOutput = AVCapturePhotoOutput()
        
        let photoSettings = AVCapturePhotoSettings(format: [AVVideoCodecKey : AVVideoCodecType.jpeg])
        
        self.photoFileOutput!.setPreparedPhotoSettingsArray([photoSettings], completionHandler: nil)
        
        if self.session.canAddOutput(photoFileOutput!) {
            self.session.addOutput(photoFileOutput!)
        }
    }
    
    fileprivate func getPreviewLayerOrientation() -> AVCaptureVideoOrientation {
        // Depends on layout orientation, not device orientation
        switch UIApplication.shared.statusBarOrientation {
        case .portrait, .unknown:
            return AVCaptureVideoOrientation.portrait
        case .landscapeLeft:
            return AVCaptureVideoOrientation.landscapeLeft
        case .landscapeRight:
            return AVCaptureVideoOrientation.landscapeRight
        case .portraitUpsideDown:
            return AVCaptureVideoOrientation.portraitUpsideDown
        @unknown default:
            return AVCaptureVideoOrientation.portrait
        }
    }
    
    fileprivate func getVideoOrientation() -> AVCaptureVideoOrientation? {
        guard shouldUseDeviceOrientation, let deviceOrientation = self.deviceOrientation else { return previewLayer!.videoPreviewLayer.connection?.videoOrientation }
        
        switch deviceOrientation {
        case .landscapeLeft:
            return .landscapeRight
        case .landscapeRight:
            return .landscapeLeft
        case .portraitUpsideDown:
            return .portraitUpsideDown
        default:
            return .portrait
        }
    }
    
    fileprivate func getImageOrientation(forCamera: CameraSelection) -> UIImage.Orientation {
        guard shouldUseDeviceOrientation, let deviceOrientation = self.deviceOrientation else { return forCamera == .rear ? .right : .leftMirrored }
        
        switch deviceOrientation {
        case .landscapeLeft:
            return forCamera == .rear ? .up : .downMirrored
        case .landscapeRight:
            return forCamera == .rear ? .down : .upMirrored
        case .portraitUpsideDown:
            return forCamera == .rear ? .left : .rightMirrored
        default:
            return forCamera == .rear ? .right : .leftMirrored
        }
    }
    
    /**
     Returns a UIImage from Image Data.
     
     - Parameter imageData: Image Data returned from capturing photo from the capture session.
     
     - Returns: UIImage from the image data, adjusted for proper orientation.
     */
    
    fileprivate func processPhoto(_ imageData: Data) -> UIImage? {
        
        if let dataProvider = CGDataProvider(data: imageData as CFData),
            let cgImageRef = CGImage(jpegDataProviderSource: dataProvider, decode: nil, shouldInterpolate: true, intent: CGColorRenderingIntent.defaultIntent) {
            
            // Set proper orientation for photo
            // If camera is currently set to front camera, flip image
            
            let image = UIImage(cgImage: cgImageRef, scale: 1.0, orientation: self.getImageOrientation(forCamera: self.currentCamera))
            
            return image
        }
        
        return nil
        
    }
    
    /// Handle Denied App Privacy Settings
    
    fileprivate func promptToAppSettings() {
        // prompt User with UIAlertView
        
        DispatchQueue.main.async(execute: { [unowned self] in
            let message = NSLocalizedString("Collector doesn't have permission to use the camera, please change privacy settings", comment: "Alert message when the user has denied access to the camera")
            let alertController = UIAlertController(title: "Alert", message: message, preferredStyle: .alert)
            alertController.addAction(UIAlertAction(title: NSLocalizedString("OK", comment: "Alert OK button"), style: .cancel, handler: nil))
            alertController.addAction(UIAlertAction(title: NSLocalizedString("Settings", comment: "Alert button to open Settings"), style: .default, handler: { action in
                
                if let url = URL(string:UIApplication.openSettingsURLString) {
                    if UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url, options: [:], completionHandler: nil)
                    }
                }
                
            }))
            self.present(alertController, animated: true, completion: nil)
        })
    }
    
    /**
     Returns an AVCapturePreset from VideoQuality Enumeration
     
     - Parameter quality: ViewQuality enum
     
     - Returns: String representing a AVCapturePreset
     */
    
    fileprivate func videoInputPresetFromVideoQuality(quality: VideoQuality) -> AVCaptureSession.Preset {
        switch quality {
        case .high: return .high
        case .medium: return .medium
        case .low: return .low
        case .resolution352x288: return .cif352x288
        case .resolution640x480: return .vga640x480
        case .resolution1280x720: return .hd1280x720
        case .resolution1920x1080: return .hd1920x1080
        case .iframe960x540: return .iFrame960x540
        case .iframe1280x720: return .iFrame1280x720
        case .resolution3840x2160:
            if #available(iOS 9.0, *) {
                return .hd4K3840x2160
            }
            else {
                print("[Cam]: Resolution 3840x2160 not supported")
                return .high
            }
        }
    }
    
    /// Sets whether Cam should enable background audio from other applications or sources
    fileprivate func setBackgroundAudioPreference() {
        guard allowBackgroundAudio == true else {
            return
        }
        
        guard audioEnabled == true else {
            return
        }
        
        do {
            try AVAudioSession.sharedInstance().setCategory(AVAudioSession.Category.playAndRecord, mode: .default,options: [.duckOthers, .defaultToSpeaker])
            session.automaticallyConfiguresApplicationAudioSession = false
        }
        catch {
            print("[Cam]: Failed to set background audio preference")
            
        }
    }
}

// MARK: AVCaptureFileOutputRecordingDelegate

extension VideoController : AVCaptureFileOutputRecordingDelegate {
    
    /// Process newly captured video and write it to temporary directory
    public func fileOutput(_ output: AVCaptureFileOutput, didFinishRecordingTo outputFileURL: URL, from connections: [AVCaptureConnection], error: Error?) {
        if let currentBackgroundRecordingID = backgroundRecordingID {
            backgroundRecordingID = UIBackgroundTaskIdentifier.invalid
            
            if currentBackgroundRecordingID != UIBackgroundTaskIdentifier.invalid {
                UIApplication.shared.endBackgroundTask(currentBackgroundRecordingID)
            }
        }
        if error != nil {
            print("[Cam]: Movie file finishing error: \(error as Any)")
            DispatchQueue.main.async { [unowned self] in
                self.videoDelegate?.cameraController(self, didFailToRecordVideo: error!)
            }
        } else {
            //Call delegate function with the URL of the outputfile
            DispatchQueue.main.async { [unowned self] in
                self.videoDelegate?.cameraController(self, didFinishProcessVideoAt: outputFileURL)
            }
        }
    }
    
}

// Mark: UIGestureRecognizer Declarations

extension VideoController {
    
    private func applyScaleOnPreviewLayer(pinchGesture: UIPinchGestureRecognizer, mode: String, loc: CGPoint) {
        do {
            guard let captureDevice = self.currentVideoDevice else {
                return
            }
            try captureDevice.lockForConfiguration()
            
            zoomScale = min(maxZoomScale, max(1.0, min(beginZoomScale * pinchGesture.scale,  captureDevice.activeFormat.videoMaxZoomFactor)))
            //            captureDevice.videoZoomFactor = zoomScale
            previewLayer.transform = CGAffineTransform(scaleX: zoomScale, y: zoomScale)
            // Call Delegate function with current zoom scale
            //            print(zoomScale)
            DispatchQueue.main.async { [unowned self] in
                self.videoDelegate?.cameraController(self, didChangeZoomLevel: self.zoomScale, zView: self.previewLayer, direction: mode, gesture: pinchGesture, location: loc)
            }
            
            captureDevice.unlockForConfiguration()
            
        } catch {
            print("[Cam]: Error locking configuration")
        }
    }
    
    /// Handle pinch gesture
    
    @objc fileprivate func zoomGesture(pinch: UIPinchGestureRecognizer) {
        guard pinchToZoom == true else {
            //ignore pinch
            return
        }
        let pinchMode = mode(pinch)
        
        if pinchMode.0 == "" {
            /* DispatchQueue.main.async { [unowned self] in
             self.videoDelegate?.cameraController(self, didChangeZoomLevel: (self.beginZoomScale * pinch.scale), zView: self.previewLayer, direction: "D", gesture: self.pinchGesture, location: CGPoint.zero)
             }*/
            return
        }
        
        if lock == true {
            if pinchMode.0 != "D" {
                lock = false
                if pinchMode.0 == "H" {
                    lockedPinchMode = "V"
                } else {
                    lockedPinchMode = "H"
                }
            }
        }
        
        if lockedPinchMode == "" {
            lockedPinchMode = "H"
        }
        
        // print("lock: \(lockPinchMode)")
        if pinchMode.0 == "D" { //|| pinchMode.0 == lockedPinchMode {
            if pinchMode.1 != nil {
                applyScaleOnPreviewLayer(pinchGesture: pinch, mode: "D", loc: pinchMode.1!)
            }
        }
        else if pinchMode.0 == "H" { //|| pinchMode.0 == lockedPinchMode {
            if pinchMode.1 != nil {
                applyScaleOnPreviewLayer(pinchGesture: pinch, mode: "H", loc: pinchMode.1!)
            }
        }
        else if pinchMode.0 == "V" { //|| pinchMode.0 == lockedPinchMode {
            if pinchMode.1 != nil {
                applyScaleOnPreviewLayer(pinchGesture: pinch, mode: "V", loc: pinchMode.1!)
            }
        }
            
        else if pinchMode.0 != "" {
            
            if pinchMode.1 != nil {
                DispatchQueue.main.async { [unowned self] in
                    self.videoDelegate?.cameraController(self, didChangeZoomLevel: (self.beginZoomScale * pinch.scale), zView: self.previewLayer, direction: pinchMode.0, gesture: self.pinchGesture, location: pinchMode.1!)
                }
            }
        }
    }
    
    @objc private func panGesture(pan: UIPanGestureRecognizer) {
        
        guard swipeToZoom == true && self.currentCamera == .rear else {
            //ignore pan
            return
        }
        let currentTranslation    = pan.translation(in: view).y
        let translationDifference = currentTranslation - previousPanTranslation
        
        do {
            guard let captureDevice = self.currentVideoDevice else {
                return
            }
            try captureDevice.lockForConfiguration()
            
            let currentZoom = captureDevice.videoZoomFactor
            
            if swipeToZoomInverted == true {
                zoomScale = min(maxZoomScale, max(1.0, min(currentZoom - (translationDifference / 75),  captureDevice.activeFormat.videoMaxZoomFactor)))
            } else {
                zoomScale = min(maxZoomScale, max(1.0, min(currentZoom + (translationDifference / 75),  captureDevice.activeFormat.videoMaxZoomFactor)))
                
            }
            
            captureDevice.videoZoomFactor = zoomScale
            
            // Call Delegate function with current zoom scale
            DispatchQueue.main.async { [unowned self] in
                self.videoDelegate?.cameraController(self, didChangeZoomLevel: self.zoomScale)
            }
            
            captureDevice.unlockForConfiguration()
            
        } catch {
            print("[Cam]: Error locking configuration")
        }
        
        if pan.state == .ended || pan.state == .failed || pan.state == .cancelled {
            previousPanTranslation = 0.0
        } else {
            previousPanTranslation = currentTranslation
        }
    }
    
    func mode(_ sender: UIPinchGestureRecognizer) -> (String, CGPoint?) {
        
        // very important:
        if sender.numberOfTouches < 2 {
            //print("avoided an obscure crash!!")
            return ("", nil)
        }
        // var pinchMode = ""
        let A = sender.location(ofTouch: 0, in: self.view)
        let B = sender.location(ofTouch: 1, in: self.view)
        //        print("\n")
        //        print("Loc A: \(A)")
        //        print("Loc B: \(B)")
        
        let xD = abs( A.x - B.x )
        let yD = abs( A.y - B.y )
        
        let ratio = xD / yD
        
        if (xD == 0) {
            pinchMode = "V"
        } else if (yD == 0) {
            pinchMode = "H"
        } else if (ratio > 2) { // 1.5
            pinchMode = "H"
        } else if (ratio < 0.5) { // 0.3
            pinchMode = "V"
        } else {
            pinchMode = "D"
        }
        
        if sender.state == .began {
            //        print("began")
            if pinchMode == "V" || pinchMode == "D" {
                if A.y < B.y {
                    pinchIndex = 0
                } else {
                    pinchIndex = 1
                }
            } else if pinchMode == "H" {
                if A.x < B.x {
                    pinchIndex = 0
                } else {
                    pinchIndex = 1
                }
            }
        }
        pinchModeChange = false
        
        if sender.state == .changed {
            if lastPinchMode != pinchMode {
                pinchModeChange = true
                if lastPinchMode == "V" && lockedPinchMode == "H" {
                    pinchGesture.scale = 1.0
                } else if lastPinchMode == "H" && lockedPinchMode == "V" {
                    pinchGesture.scale = 1.0
                }
                
                if pinchMode == "V" || pinchMode == "D" {
                    if A.y < B.y {
                        pinchIndex = 0
                    } else {
                        pinchIndex = 1
                    }
                } else if pinchMode == "H" {
                    if A.x < B.x {
                        pinchIndex = 0
                    } else {
                        pinchIndex = 1
                    }
                }
            }
        }
        
        let loc = sender.location(ofTouch: pinchIndex, in: self.view)
//              print("index: \(pinchIndex)")
        lastPinchMode = pinchMode
        
        //print("mode: \(pinchMode)")
        return (pinchMode, loc)
    }
    
    /**
     Add pinch gesture recognizer and double tap gesture recognizer to currentView
     
     - Parameter view: View to add gesture recognzier
     
     */
    
    fileprivate func addGestureRecognizers() {
        pinchGesture = UIPinchGestureRecognizer(target: self, action: #selector(zoomGesture(pinch:)))
        pinchGesture.delegate = self
        previewLayer.addGestureRecognizer(pinchGesture)
    }
}

// MARK: UIGestureRecognizerDelegate

extension VideoController : UIGestureRecognizerDelegate {
    
    /// Set beginZoomScale when pinch begins
    
    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        if gestureRecognizer.isKind(of: UIPinchGestureRecognizer.self) {
            beginZoomScale = zoomScale;
        }
        return true
    }
}

extension VideoController : CameraButtonDelegate {
    @objc func canRotate() -> Void {}
    /// Set UITapGesture to start/stop video
    
    public func buttonWasTapped() {
        if videoTimerEnds {
            return
        }
        
        if isVideoRecording {
            self.videoDelegate?.cameraController_disableButtonView()
            stopVideoRecording()
        } else {
            self.videoDelegate?.cameraController_disableButtonView()
            //Devide Orientation
            var bufDeviceOrientationRawValue = UIDevice.current.orientation.rawValue
            if bufDeviceOrientationRawValue < 1 || bufDeviceOrientationRawValue > 4 { //
                bufDeviceOrientationRawValue = 1
            }
            VideoVariables.deviceOrientationRawValue = bufDeviceOrientationRawValue
            //DispatchQueue.main.after(TimeInterval.zero + 0.5) {
                self.videoDelegate?.cameraController_enableButtonView()
                let initialSubjectFrame = CGRect(origin: CGPoint(x: 0, y: (self.view.frame.size.height - self.view.frame.size.width)/2), size: CGSize(width: self.view.frame.size.width, height: self.view.frame.size.width))
                let zframe = self.view.convert(initialSubjectFrame, to: self.previewLayer)
                self.updateCurrentSubjectRect(rect: zframe)
                self.startVideoRecording()
            //}
            
        }
    }
    
    /// Called if maximum duration is reached
    
    public func longPressDidReachMaximumDuration() {
        //stopVideoRecording()
    }
    
    /// Called when time interval updated
    
    public func updatedTimerValue(time: Double) {
        
    }
}

extension VideoController: FaceBlurRecorderDataSource {
    func subjectFrameCurrentRect() -> CGRect {
        return subjectCurrentFrame ?? .zero
    }
}

extension VideoController {
    
    func updateCurrentSubjectRect(rect: CGRect){
        
//        if let formatDesc = self.currentVideoDeviceInput?.device.activeFormat.formatDescription {
        var dimesions = CGSize.zero
        if VideoVariables.deviceOrientationRawValue < 3  {
            dimesions = CGSize(width: 1080, height: 1920)
        }
        else {
            dimesions = CGSize(width: 1920, height: 1080)
        }
        
        //let subjectFrame = CGRect(x: rect.origin.y, y: rect.origin.x, width: rect.size.height, height: rect.size.height)

        var subjectFrame = CGRect.zero
        var screenWidth: CGFloat = 0.0
        var screenHeight: CGFloat = 0.0
        
        if VideoVariables.deviceOrientationRawValue < 3 {
            subjectFrame = CGRect(x: rect.origin.x, y: rect.origin.y, width: rect.size.width, height: rect.size.height)
            screenWidth = ScreenSize.SCREEN_WIDTH
            screenHeight = UIDevice.current.hasNotch ? ScreenSize.SCREEN_HEIGHT - screenHeightWithSafeArea : ScreenSize.SCREEN_HEIGHT
        } else {
            subjectFrame = CGRect(x: rect.origin.y, y: rect.origin.x, width: rect.size.height, height: rect.size.width)
            screenWidth = UIDevice.current.hasNotch ? ScreenSize.SCREEN_HEIGHT - screenHeightWithSafeArea : ScreenSize.SCREEN_HEIGHT
            screenHeight = ScreenSize.SCREEN_WIDTH
        }
        
//      let dimesions = CMVideoFormatDescriptionGetDimensions(formatDesc)
        let height: CGFloat = CGFloat(dimesions.height)
        let width: CGFloat = CGFloat(dimesions.width)
        
        let aspectWidth = CGFloat(width) / screenWidth
        let aspectHeight = CGFloat(height) / screenHeight
        
        let newSubjectWidth = aspectWidth * subjectFrame.width
        let newSubjectHeight = aspectHeight * subjectFrame.height
        
        let newX = subjectFrame.origin.x * aspectWidth
        let newY = (subjectFrame.origin.y * aspectHeight)
        
        let updatedRect = CGRect(x: newX, y: newY, width: newSubjectWidth, height: newSubjectHeight)
        
        self.subjectCurrentFrame = updatedRect
        
//        }
    }
    
}

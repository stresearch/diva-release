//
//  CIFaceMaskVC.swift
//  CIFaceMask
//
//  Created by Tushar on 8/13/18.
//  Copyright © 2018 BTC Soft. All rights reserved.
//

import UIKit
import Photos
import SVProgressHUD

let kMediaContentDefaultScale: CGFloat = 1
let kProcessedTemporaryVideoFileName = "/processed.mov"
let kMediaContentTimeValue: Int64 = 1
let kMediaContentTimeScale: Int32 = 30

class RecordVideoVC: VideoController {
    
    
    //MARK:- Outlets
    @IBOutlet fileprivate var buttonToggleCamera: UIButton!
    @IBOutlet fileprivate var toggleFlashButton: UIButton!
    @IBOutlet fileprivate var buttonClose: UIButton!
    @IBOutlet fileprivate var buttonBack: UIButton!
    @IBOutlet weak var buttonInfo: UIButton!
    @IBOutlet weak var collectionView: UICollectionView!
    @IBOutlet weak var recordBottomContraint: NSLayoutConstraint!
    @IBOutlet weak var recordView: RecordView!
    @IBOutlet weak var stackView: UIStackView!
    
    @IBOutlet weak var blackViewTop: UIView!
    @IBOutlet weak var blackViewBottom: UIView!
    @IBOutlet weak var blackViewTopHeight: NSLayoutConstraint!
    @IBOutlet weak var blackViewBottomHeight: NSLayoutConstraint!
    
    //MARK:- Enums
    enum CurrentCameraMode {
        case video,photo
    }
    
    //MARK :- Properties
    var currentMode = CurrentCameraMode.video
    var timer: Timer?
    var timerSeconds = kCollectionVideoMaxDuration
    var blurredFaces = 0
    var newLayer : CAShapeLayer?
    let shapeLayer = CAShapeLayer()
    
    lazy var viewTop = ResizableView()
    lazy var viewLeft = ResizableView()
    lazy var viewBottom = ResizableView()
    lazy var viewRight = ResizableView()
    lazy var viewSubject = ResizableView()
    
    var videoUrlPath: URL?
    lazy var cordinates: [Cordinate] = []
    lazy var activities: [Activity] = []
    lazy var boundindBox: [BoundingBox] = []
    lazy var isActivityClicked: Bool = false
    
    var zoomedView: UIView?
    var touchStart = CGPoint.zero
    lazy var listOfActivities: [String] = []
    
    var doubleTapToggle = false
    
    var valTimeLblCenter: CGPoint = .zero
    var valButtonViewCenter: CGPoint = .zero
    var valPreviousMode: Int = 0
    
    static var valDidEnterBackground: Bool = false
    
    static var operationJson: Operation?    // make this weak If required
    var queueOperationJson = OperationQueue()
    static var valBlurActionStatus: String = "NotStarted"
    
    var clearPreviewFromNavigation = true
    
    private var customPopUp: CustomPopUp?
    
    private var videoProcessingBegins = false
    
    override var prefersStatusBarHidden: Bool {
        return true
    }
    
    // MARK: view lifecycle
    deinit {
        Log("\(self) I'm gone ") // Keep eye on this
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        
        maximumVideoDuration = 30.0
        shouldUseDeviceOrientation = true
        allowAutoRotate = true
        audioEnabled = false
        isVideoRecordingWithBlur = true
        collectionView.contentInsetAdjustmentBehavior = .never
        addTapGestures()
        self.navigationController?.navigationBar.isHidden = true
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        stackView.spacing = DeviceType.IS_IPHONE_4_OR_LESS || DeviceType.IS_IPHONE_5 ? 20 : 30
        videoDelegate = self
        if ProjectService.instance.currentCollection?.activityShortNames?.count ?? 0 > 0 {
            listOfActivities = ProjectService.instance.currentCollection?.activityShortNames?.components(separatedBy: ",") ?? []
            collectionView.isHidden = false
        } else {
            listOfActivities = []
            collectionView.isHidden = true
        }
        collectionView.reloadData()
        
        shapeLayer.frame = view.frame
        view.layer.addSublayer(shapeLayer)
        activities = []
        cordinates = []
        boundindBox = []
        addSubject()
        setupRecordButton()
        valButtonViewCenter = self.recordView.buttonView.center
        valTimeLblCenter = self.recordView.timeLbl.center
        NotificationCenter.default.addObserver(self, selector: #selector(self.rotated), name: UIDevice.orientationDidChangeNotification, object: nil)
        
        //Clear Navigation
        let arrNavigation = navigationController?.viewControllers
        
        if arrNavigation?.count ?? 0 > 2 && clearPreviewFromNavigation {// && previewFlow != .MyVideos
            let arrControllerCheck = arrNavigation![arrNavigation!.count - 2]
            if arrControllerCheck.isKind(of: ConsentVideoPreviewVC.self) {
                self.navigationController?.viewControllers.remove(at: arrNavigation!.count - 2)
            }
        }
        
        clearPreviewFromNavigation = true
        
        if UIDevice.current.hasNotch {
            blackViewTop.isHidden = false
            blackViewBottom.isHidden = false
            
            if ScreenSize.SCREEN_HEIGHT == 896 {
                blackViewTopHeight.constant = 80
                blackViewBottomHeight.constant = 80
            } else {
                blackViewTopHeight.constant = 73
                blackViewBottomHeight.constant = 73
            }
        }
        else {
            blackViewTop.isHidden = true
            blackViewBottom.isHidden = true
        }
        
        swipeToPop()
        self.zoomedView = self.view
        view.bringSubviewToFront(collectionView)
        view.bringSubviewToFront(recordView)
        view.bringSubviewToFront(buttonClose)
        view.bringSubviewToFront(buttonBack)
        view.bringSubviewToFront(toggleFlashButton)
        view.bringSubviewToFront(stackView)
        //updateCurrentSubjectRect(rect:  self.viewSubject.frame)
        self.videoProcessingBegins = false
        self.videoTimerEnds = false
        self.rotated()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        NotificationCenter.default.addObserver(self, selector: #selector(willEnterForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(didEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        print("viewDidDisappear---")
        
        super.viewDidDisappear(animated)
        self.videoProcessingBegins = false
        NotificationCenter.default.removeObserver(self, name: UIDevice.orientationDidChangeNotification, object: nil)
        
        NotificationCenter.default.removeObserver(self, name: UIApplication.willEnterForegroundNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: UIApplication.didEnterBackgroundNotification, object: nil)
        removeCustomViews()
    }
    
    // MARK: Private Methods
    @objc func willEnterForeground() {
        DispatchQueue.main.after(TimeInterval.zero + 0.08) {
            self.hideProgress()
        }
        
        RecordVideoVC.operationJson?.cancel()
    }
    
    @objc func didEnterBackground() {
        deinitialisingTasks()
    }
    
    private func deinitialisingTasks() {
        RecordVideoVC.operationJson?.cancel()
        
        //To discard Video if went to background
        if isVideoRecording {
            self.discardVideoRecording()
            recordView.updateButtonView(isVideoRecording: isVideoRecording)
            recordView.recordButton.delegate = self
        }
        timer?.invalidate()
        timerSeconds = kCollectionVideoMaxDuration
        RecordVideoVC.valDidEnterBackground = true
        self.hideProgress()
    }
  
    private func addTapGestures() {
        
        let singleTapGesture = UITapGestureRecognizer(target: self, action: #selector(singleTapGesture(tap:)))
        singleTapGesture.numberOfTapsRequired = 1
        singleTapGesture.delegate = self
        self.view.addGestureRecognizer(singleTapGesture)
        
        let doubleTapGesture = UITapGestureRecognizer(target: self, action: #selector(doubleTapGesture(tap:)))
        doubleTapGesture.numberOfTapsRequired = 2
        doubleTapGesture.delegate = self
        self.view.addGestureRecognizer(doubleTapGesture)
        
        singleTapGesture.require(toFail: doubleTapGesture)
    }
    
    @objc fileprivate func singleTapGesture(tap: UITapGestureRecognizer) {
        
        let tapPoint = tap.location(in: self.view)
        DispatchQueue.main.async { [unowned self] in
            self.videoDelegate?.cameraController(self, didSingleFocusAtPoint: tapPoint)
        }
    }
    
    @objc fileprivate func doubleTapGesture(tap: UITapGestureRecognizer) {
        
        let tapPoint = tap.location(in: self.view)
        DispatchQueue.main.async { [unowned self] in
            self.videoDelegate?.cameraController(self, didDoubleFocusAtPoint: tapPoint)
        }
    }

    
    func swipeToPop() {
        
        self.navigationController?.interactivePopGestureRecognizer?.isEnabled = false
        self.navigationController?.interactivePopGestureRecognizer?.delegate = nil
    }
  
    private func setupRecordButton() {
        recordView.updateButtonView(isVideoRecording: isVideoRecording)
        recordView.recordButton.delegate = self
    }
    
    @objc func rotated() {
        
        if UIDevice.current.orientation.isLandscape && valButtonViewCenter != .zero {
                        
            if UIDevice.current.orientation == .landscapeLeft {
                self.collectionView.frame.origin.y = UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 80 : 73) : 0
                self.collectionView.transform = CGAffineTransform(rotationAngle: CGFloat.pi / 2)
                self.collectionView.frame.origin.y = UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 80 : 73) : 0
                self.collectionView.frame.origin.x = 15
                self.collectionView.frame.size.height = (self.view.frame.size.height - (UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 160 : 146) : 0))
                self.collectionView.frame.size.width = 60
                
                self.recordView.translatesAutoresizingMaskIntoConstraints = false
                self.recordView.timeLbl.transform = CGAffineTransform(rotationAngle: CGFloat.pi / 2)
                self.recordView.buttonView.transform = CGAffineTransform(rotationAngle: CGFloat.pi / 2)
                self.recordView.timeLbl.center = CGPoint(x: valButtonViewCenter.x + 56, y: valButtonViewCenter.y)
                
                self.recordView.translatesAutoresizingMaskIntoConstraints = true
                
                self.stackView.translatesAutoresizingMaskIntoConstraints = false
                //self.stackView.frame.size.width = 250
                self.stackView.transform = CGAffineTransform(rotationAngle: CGFloat.pi / 2)
                self.stackView.frame.origin.y = self.view.frame.height - self.stackView.frame.size.height - (UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 81 : 74) : 16)
                self.stackView.frame.origin.x = self.view.frame.width - self.stackView.frame.size.width - 16
                self.stackView.translatesAutoresizingMaskIntoConstraints = true
                
                self.buttonBack.transform = CGAffineTransform(rotationAngle: CGFloat.pi / 2)
                self.buttonBack.frame.origin.x = self.view.frame.width - self.buttonBack.frame.size.width - 16
                self.buttonBack.frame.origin.y = UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 81 : 74) : 40
                
                valPreviousMode = 3
                
                VideoVariables.deviceOrientationRawValue = (isVideoRecording || videoProcessingBegins)
                                                            ? VideoVariables.deviceOrientationRawValue
                                                            : 3
            } else {
                self.collectionView.frame.origin.x = view.frame.size.width - 75
                self.collectionView.transform = CGAffineTransform(rotationAngle: -(CGFloat.pi / 2))
                self.collectionView.frame.origin.x = view.frame.size.width - 75
                self.collectionView.frame.origin.y = UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 80 : 73) : 0
                self.collectionView.frame.size.height = (self.view.frame.size.height - (UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 160 : 146) : 0))
                self.collectionView.frame.size.width = 60
                
                self.recordView.translatesAutoresizingMaskIntoConstraints = false
                self.recordView.timeLbl.transform = CGAffineTransform(rotationAngle: -(CGFloat.pi / 2))
                self.recordView.buttonView.transform = CGAffineTransform(rotationAngle: -(CGFloat.pi / 2))
                self.recordView.timeLbl.center = CGPoint(x: valButtonViewCenter.x - 56, y: valButtonViewCenter.y)
                self.recordView.translatesAutoresizingMaskIntoConstraints = true
                
                self.buttonBack.transform = CGAffineTransform(rotationAngle: -(CGFloat.pi / 2))
                self.buttonBack.frame.origin.x = 16
                self.buttonBack.frame.origin.y = self.view.frame.height - self.buttonBack.frame.size.height - (UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 81 : 74) : 16)
                
                self.stackView.translatesAutoresizingMaskIntoConstraints = false
                //self.stackView.frame.size.width = 250
                self.stackView.transform = CGAffineTransform(rotationAngle: -(CGFloat.pi / 2))
                self.stackView.frame.origin.y = (UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 81 : 74) : 40)
                self.stackView.frame.origin.x = 16
                self.stackView.translatesAutoresizingMaskIntoConstraints = true
                
                valPreviousMode = 4
                VideoVariables.deviceOrientationRawValue = (isVideoRecording || videoProcessingBegins)
                                                            ? VideoVariables.deviceOrientationRawValue
                                                            : 4
            }
            self.recordBottomContraint.constant = 20
            
        } else {
                        
            if valPreviousMode == 3 || valPreviousMode == 4 {
                self.recordView.translatesAutoresizingMaskIntoConstraints = false
                self.recordView.timeLbl.transform = CGAffineTransform(rotationAngle: 0)
                self.recordView.buttonView.transform = CGAffineTransform(rotationAngle: 0)
                
                self.recordView.timeLbl.center = valTimeLblCenter
                self.recordView.buttonView.center = valButtonViewCenter
                self.recordView.translatesAutoresizingMaskIntoConstraints = true
            } else if valPreviousMode == 1 {
                self.recordView.translatesAutoresizingMaskIntoConstraints = false
                self.recordView.timeLbl.center = valTimeLblCenter
                self.recordView.buttonView.center = valButtonViewCenter
                self.recordView.translatesAutoresizingMaskIntoConstraints = true
            } else {
                self.recordView.transform = CGAffineTransform(rotationAngle: (CGFloat.pi * 2))
            }
            
            self.recordBottomContraint.constant = 75
            self.collectionView.frame.origin.y = view.frame.size.height - 75
            self.collectionView.transform = .identity
            self.collectionView.frame.origin.y = view.frame.size.height - 75
            self.collectionView.frame.origin.x = 0
            self.collectionView.frame.size.height = 60
            self.collectionView.frame.size.width = (self.view.frame.size.width)
            
            self.buttonBack.transform = .identity
            self.buttonBack.frame.origin.x = 16
            self.buttonBack.frame.origin.y = UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 81 : 74) : 40
            
            self.stackView.translatesAutoresizingMaskIntoConstraints = false
            self.stackView.transform = .identity
            //self.stackView.frame.size.width = 180
            self.stackView.frame.origin.y = UIDevice.current.hasNotch ? (ScreenSize.SCREEN_HEIGHT == 896 ? 81 : 74) : 40
            self.stackView.frame.origin.x = self.view.frame.width - self.stackView.frame.size.width - 16
            self.stackView.translatesAutoresizingMaskIntoConstraints = true
            
            valPreviousMode = 1
            VideoVariables.deviceOrientationRawValue = (isVideoRecording || videoProcessingBegins)
                                                        ? VideoVariables.deviceOrientationRawValue
                                                        : 1
        }
    }
    
    override open func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        if let touch = touches.first {
          touchStart = touch.location(in: self.view)
        }
        reDrawSubjectAsceptRatio(touches)
        saveSubjectCordinatesOnChange()
    }
  
    override open func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        
        reDrawSubjectAsceptRatio(touches)
      
        if let touch = touches.first {
            let currentPoint = touch.location(in: self.view)
            let previous = touch.previousLocation(in: self.view)
          let centerX = self.view.center.x
          let centerY = self.view.center.y
          
          if (currentPoint.x < centerX) && (currentPoint.y < centerY) {
            // Upper Left Touch Detect
            updateSubjectHeightOnVerticalPinchGesture(currentPoint: currentPoint, previousPoint: previous, isIncrease: false)
            updateSubjectWidthOnHorizontalPinchGesture(currentPoint: currentPoint, previousPoint: previous, isIncrease: false)
          } else if (currentPoint.x > centerX) && (currentPoint.y < centerY) {
            // Upper Right Touch Detect
            updateSubjectHeightOnVerticalPinchGesture(currentPoint: currentPoint, previousPoint: previous, isIncrease: false)
            updateSubjectWidthOnHorizontalPinchGesture(currentPoint: currentPoint, previousPoint: previous, isIncrease: true)
          } else if (currentPoint.x < centerX) && (currentPoint.y > centerY) {
            // Lower Left Touch Detect
            updateSubjectHeightOnVerticalPinchGesture(currentPoint: currentPoint, previousPoint: previous, isIncrease: true)
            updateSubjectWidthOnHorizontalPinchGesture(currentPoint: currentPoint, previousPoint: previous, isIncrease: false)
          } else if (currentPoint.x > centerX) && (currentPoint.y > centerY) {
            // Lower Right Touch Detect
            updateSubjectHeightOnVerticalPinchGesture(currentPoint: currentPoint, previousPoint: previous, isIncrease: true)
            updateSubjectWidthOnHorizontalPinchGesture(currentPoint: currentPoint, previousPoint: previous, isIncrease: true)
          }
            updateShadedViewFrames(toSize: view.frame.size)
        }
      
        saveSubjectCordinatesOnChange()
    }
  
    override open func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
    //  print("Touch End")
    }
  
    private func saveSubjectCordinatesOnChange() {
        
        if isVideoRecording {
            
            var zframe = view.convert(viewSubject.frame, to: zoomedView)
            updateCurrentSubjectRect(rect: zframe)
            zframe = self.subjectCurrentFrame ?? .zero
            let zCordinate = Cordinate(zframe: Frame(height: Int(zframe.size.height), width: Int(zframe.size.width), x: Int(zframe.origin.x), y: Int(zframe.origin.y)), ztime: (kCollectionVideoMaxDuration - timerSeconds))
            if let index = cordinates.firstIndex(where: { $0.time == (kCollectionVideoMaxDuration - timerSeconds) }) {
                cordinates[index] = zCordinate
            } else {
                cordinates.append(zCordinate)
            }
        }
    }
  
    private func reDrawSubjectAsceptRatio(_ touches: Set<UITouch>) {
        
        if touches.count == 2 {
            
            var arrTouches: [UITouch] = []
            
            for touch in touches {
              arrTouches.append(touch)
            }
            
            let touch = arrTouches[0].location(in: self.view)
            let touch1 = arrTouches[1].location(in: self.view)
            
            if !checkTapPointWithInRadius(point: touch) && !checkTapPointWithInRadius(point: touch1) {
                viewSubject.frame.size.width = (touch.x - touch1.x) < 0 ? -(touch.x - touch1.x) : (touch.x - touch1.x)
                viewSubject.frame.size.height = (touch.y - touch1.y) < 0 ? -(touch.y - touch1.y) : (touch.y - touch1.y)
                viewSubject.center.x = view.center.x
                viewSubject.center.y = view.center.y
            }
        }
    }
  
    private func checkTapPointWithInRadius(point: CGPoint) -> Bool {
        
        let centerX = self.view.center.x
        let centerY = self.view.center.y
        let zframe = view.convert(viewSubject.frame, to: self.view)
        var radius: CGFloat = 0
        
        if (point.x < centerX) && (point.y < centerY) {
            // Upper Left Touch Detect
            radius = Utilities.CGPointDistance(from: CGPoint(x: zframe.origin.x, y: zframe.origin.y), to: point)
        } else if (point.x > centerX) && (point.y < centerY) {
          // Upper Right Touch Detect
          radius = Utilities.CGPointDistance(from: CGPoint(x: zframe.origin.x + zframe.size.width, y: zframe.origin.y), to:
          point)
        } else if (point.x < centerX) && (point.y > centerY) {
          // Lower Left Touch Detect
          radius = Utilities.CGPointDistance(from: CGPoint(x: zframe.origin.x, y: zframe.origin.y + zframe.size.height), to: point)
        } else if (point.x > centerX) && (point.y > centerY) {
          // Lower Right Touch Detect
          radius = Utilities.CGPointDistance(from: CGPoint(x: zframe.origin.x + zframe.size.width, y: zframe.origin.y + zframe.size.height), to: point)
        }
        return radius < 30
    }
    
    private func applyPinchGestureOnSubject(scale: CGFloat, position: String, pinch: UIPinchGestureRecognizer, touch: CGPoint) {
        
        switch pinch.state {
        case .began:
            //print("touch began")
            touchStart = touch
        case .changed:
            //print("touch changed")
            
            if pinchModeChange == true {
                touchStart = touch
            }
            
            let currentPoint = touch
            //print("Current: \(currentPoint)")
            //print("Previous: \(touchStart)")
            if position == "D" || position == "H" || position == "V" { // || position == "H" || position == "V"
                //print("currentPoint---\(currentPoint)---\(touchStart)---\(scale)---\(position)")
              updateSubjectFramesOnDiagonalPinchGesture(currentPoint: currentPoint)
            } else if position == "H" {
                //updateSubjectFramesOnHorizontalPinchGesture(currentPoint: currentPoint)
            } else if position == "V" {
                //updateSubjectFramesOnVerticalPinchGesture(currentPoint: currentPoint)
            }
            
            updateShadedViewFrames(toSize: view.frame.size)
            
            touchStart = currentPoint
        case .ended:
            
            if viewSubject.frame.size.width == view.frame.size.width && viewSubject.frame.size.height == view.frame.size.height {
                lock = true
            } else {
                lock = false
            }
            touchStart = CGPoint.zero
        default:
            break
        }
        //self.subjectCurrentFrame = self.viewSubject.frame
    }
    
    func updateSubjectFramesOnDiagonalPinchGesture(currentPoint: CGPoint) {
      
      let labelH = viewSubject.frame.size.height
      let labelY = viewSubject.frame.origin.y
    
      let labelW = viewSubject.frame.size.width
      let labelX = viewSubject.frame.origin.x

      if currentPoint.y > touchStart.y { // increase
        
        if UIDevice.current.orientation.isLandscape {
            viewSubject.frame.origin.y = (labelH == 50) ? labelY : (labelY + (currentPoint.y - touchStart.y))
            viewSubject.frame.size.height = labelH - ((currentPoint.y - touchStart.y) * 2)
            viewSubject.frame.origin.x = (labelW == 50) ? labelX : (labelX + (currentPoint.x - touchStart.x))
            viewSubject.frame.size.width = labelW - ((currentPoint.x - touchStart.x) * 2)
        } else {
            if currentPoint.x > self.view.frame.width / 2 {
                viewSubject.frame.origin.y = (labelH == 50) ? labelY : (labelY + (currentPoint.y - touchStart.y))
                viewSubject.frame.size.height = labelH - ((currentPoint.y - touchStart.y) * 2)
                viewSubject.frame.origin.x = (labelW == 50) ? labelX : (labelX - (currentPoint.x - touchStart.x))
                viewSubject.frame.size.width = labelW + ((currentPoint.x - touchStart.x) * 2)
            }
            else {
                viewSubject.frame.origin.y = (labelH == 50) ? labelY : (labelY + (currentPoint.y - touchStart.y))
                viewSubject.frame.size.height = labelH - ((currentPoint.y - touchStart.y) * 2)
                viewSubject.frame.origin.x = (labelW == 50) ? labelX : (labelX - (currentPoint.x - touchStart.x))
                viewSubject.frame.size.width = labelW - ((currentPoint.x - touchStart.x) * 2)
            }
            
            
        }
      } else {
        
        if UIDevice.current.orientation.isLandscape {
            viewSubject.frame.origin.y = labelY - (touchStart.y - currentPoint.y)
            viewSubject.frame.size.height = labelH + ((touchStart.y - currentPoint.y) * 2)
            viewSubject.frame.origin.x = labelX - (touchStart.x - currentPoint.x)
            viewSubject.frame.size.width = labelW + ((touchStart.x - currentPoint.x) * 2)
        } else {
            if currentPoint.x > self.view.frame.width / 2 {
            viewSubject.frame.origin.y = labelY - (touchStart.y - currentPoint.y)
            viewSubject.frame.size.height = labelH + ((touchStart.y - currentPoint.y) * 2)
            viewSubject.frame.origin.x = labelX + (touchStart.x - currentPoint.x)
            viewSubject.frame.size.width = labelW - ((touchStart.x - currentPoint.x) * 2)
            }
            else {
            viewSubject.frame.origin.y = labelY - (touchStart.y - currentPoint.y)
            viewSubject.frame.size.height = labelH + ((touchStart.y - currentPoint.y) * 2)
            viewSubject.frame.origin.x = labelX + (touchStart.x - currentPoint.x)
            viewSubject.frame.size.width = labelW + ((touchStart.x - currentPoint.x) * 2)
            }
        }
      }

      viewSubject.frame.origin.y = viewSubject.frame.origin.y < safeAreaValueForVideo ? safeAreaValueForVideo : viewSubject.frame.origin.y
      viewSubject.frame.origin.y = viewSubject.frame.origin.y > (view.frame.size.height/2 - viewSubject.frame.size.height/2) ? (view.frame.size.height/2 - viewSubject.frame.size.height/2) : viewSubject.frame.origin.y
      viewSubject.frame.size.height = viewSubject.frame.size.height > (view.frame.height - screenHeightWithSafeArea) ? (view.frame.height - screenHeightWithSafeArea) : viewSubject.frame.size.height
      viewSubject.frame.size.height = viewSubject.frame.size.height < 50 ? 50 : viewSubject.frame.size.height
    
      viewSubject.frame.origin.x = viewSubject.frame.origin.x > (view.frame.size.width/2 - viewSubject.frame.size.width/2) ? (view.frame.size.width/2 - viewSubject.frame.size.width/2) : viewSubject.frame.origin.x
      viewSubject.frame.origin.x = viewSubject.frame.origin.x < 0 ? 0 : viewSubject.frame.origin.x
      viewSubject.frame.size.width = viewSubject.frame.size.width > view.frame.width ? view.frame.width : viewSubject.frame.size.width
      viewSubject.frame.size.width = viewSubject.frame.size.width < 50 ? 50 : viewSubject.frame.size.width
      
      viewSubject.center.x = view.center.x
      viewSubject.center.y = view.center.y
    }
  
  func updateSubjectWidthOnHorizontalPinchGesture(currentPoint: CGPoint, previousPoint: CGPoint, isIncrease: Bool) {
        
        let labelW = viewSubject.frame.size.width
        let labelX = viewSubject.frame.origin.x
        if currentPoint.x > previousPoint.x {
          
          let diff = currentPoint.x - previousPoint.x
          
            /*if UIDevice.current.orientation == .landscapeLeft {
                viewSubject.frame.origin.x = (labelW == 50) ? labelX : (isIncrease ? (labelX + diff) : (labelX - diff))
                viewSubject.frame.size.width = isIncrease ? (labelW - (diff * 2)) : (labelW + (diff * 2))
            } else {*/
                viewSubject.frame.origin.x = (labelW == 50) ? labelX : (isIncrease ? (labelX - diff) : (labelX + diff))
                viewSubject.frame.size.width = isIncrease ? (labelW + (diff * 2)) : (labelW - (diff * 2))
            //}
            
        } else {
          
            let diff = previousPoint.x - currentPoint.x
          
            /*if UIDevice.current.orientation == .landscapeLeft {
                viewSubject.frame.origin.x = isIncrease ? (labelX - diff) : (labelX + diff)
                viewSubject.frame.size.width = isIncrease ? (labelW + (diff * 2)) : (labelW - (diff * 2))
            } else {*/
                viewSubject.frame.origin.x = isIncrease ? (labelX + diff) : (labelX - diff)
                viewSubject.frame.size.width = isIncrease ? (labelW - (diff * 2)) : (labelW + (diff * 2))
            //}
        }
        
        viewSubject.frame.origin.x = viewSubject.frame.origin.x > (view.frame.size.width/2 - viewSubject.frame.size.width/2) ? (view.frame.size.width/2 - viewSubject.frame.size.width/2) : viewSubject.frame.origin.x
        viewSubject.frame.origin.x = viewSubject.frame.origin.x < 0 ? 0 : viewSubject.frame.origin.x
        viewSubject.frame.size.width = viewSubject.frame.size.width > view.frame.width ? view.frame.width : viewSubject.frame.size.width
        viewSubject.frame.size.width = viewSubject.frame.size.width < 50 ? 50 : viewSubject.frame.size.width
        
        viewSubject.center.x = view.center.x
    }
  
    func updateSubjectHeightOnVerticalPinchGesture(currentPoint: CGPoint, previousPoint: CGPoint, isIncrease: Bool) {
        
        let labelH = viewSubject.frame.size.height
        
        let labelY = viewSubject.frame.origin.y
        
        if currentPoint.y > previousPoint.y {

            let diff = currentPoint.y - previousPoint.y
          
            /*if UIDevice.current.orientation == .landscapeRight {
                viewSubject.frame.origin.y = (labelH == 50) ? labelY : (isIncrease ? (labelY + diff) : (labelY - diff))
                viewSubject.frame.size.height = isIncrease ? (labelH - (diff * 2)) : (labelH + (diff * 2))
            } else {*/
                viewSubject.frame.origin.y = (labelH == 50) ? labelY : (isIncrease ? (labelY - diff) : (labelY + diff))
                viewSubject.frame.size.height = isIncrease ? (labelH + (diff * 2)) : (labelH - (diff * 2))
            //}
        } else {

            let diff = previousPoint.y - currentPoint.y
          
            /*if UIDevice.current.orientation == .landscapeRight {
                viewSubject.frame.origin.y = isIncrease ? (labelY - diff) : (labelY + diff)
                viewSubject.frame.size.height = isIncrease ? (labelH + (diff * 2)) : (labelH - (diff * 2))
            } else {*/
                viewSubject.frame.origin.y = isIncrease ? (labelY + diff) : (labelY - diff)
                viewSubject.frame.size.height = isIncrease ? (labelH - (diff * 2)) : (labelH + (diff * 2))
            //}
        }
        
        viewSubject.frame.origin.y = viewSubject.frame.origin.y > (view.frame.size.height/2 - viewSubject.frame.size.height/2) ? (view.frame.size.height/2 - viewSubject.frame.size.height/2) : viewSubject.frame.origin.y
        viewSubject.frame.origin.y = viewSubject.frame.origin.y < safeAreaValueForVideo ? safeAreaValueForVideo : viewSubject.frame.origin.y
        viewSubject.frame.size.height = viewSubject.frame.size.height > (view.frame.height - screenHeightWithSafeArea) ? (view.frame.height - screenHeightWithSafeArea) : viewSubject.frame.size.height
        viewSubject.frame.size.height = viewSubject.frame.size.height < 50 ? 50 : viewSubject.frame.size.height
      
        viewSubject.center.y = view.center.y
    }

    func updateShadedViewFrames(toSize: CGSize) {
        
        viewTop.frame.origin.y = viewTop.frame.origin.y < 0 ? 0 : viewTop.frame.origin.y
        viewTop.frame.size.height = viewSubject.frame.origin.y
        viewTop.frame.size.width = toSize.width
        
        viewBottom.frame.origin.y = viewSubject.frame.origin.y + viewSubject.frame.size.height
        viewBottom.frame.size.height = toSize.height - (viewSubject.frame.origin.y + viewSubject.frame.size.height)
        viewBottom.frame.size.width = toSize.width
        
        viewLeft.frame.origin.x = viewLeft.frame.origin.x < 0 ? 0 : viewLeft.frame.origin.x
        viewLeft.frame.origin.y = viewSubject.frame.origin.y
        viewLeft.frame.size.width = viewSubject.frame.origin.x
        viewLeft.frame.size.height = viewSubject.frame.size.height
        
        viewRight.frame.origin.x = viewSubject.frame.origin.x + viewSubject.frame.size.width
        viewRight.frame.origin.y = viewSubject.frame.origin.y
        viewRight.frame.size.width = toSize.width - (viewSubject.frame.origin.x + viewSubject.frame.size.width)
        viewRight.frame.size.height = viewSubject.frame.size.height
    }
    
    private func addSubject() {
        
        viewSubject = ResizableView(frame: CGRect(origin: CGPoint(x: 0, y: ((view.frame.size.height - view.frame.size.width)/2 - 50)), size: CGSize(width: view.frame.size.width, height: view.frame.size.width - 50)))
        viewSubject.center = view.center
        //label.layer.borderWidth = 2.0
        //label.layer.borderColor = UIColor.yellow.cgColor
        view.addSubview(viewSubject)
        
        viewTop = ResizableView(frame: CGRect(x: 0, y: 0, width: view.frame.width, height: viewSubject.frame.origin.y))
        viewTop.backgroundColor = UIColor.black
        viewTop.alpha = 0.7
        viewBottom = ResizableView(frame: CGRect(x: 0, y: viewSubject.frame.origin.y + viewSubject.frame.size.height, width: view.frame.width, height: view.frame.size.height - (viewSubject.frame.origin.y + viewSubject.frame.size.height)))
        viewBottom.backgroundColor = UIColor.black
        viewBottom.alpha = 0.7
        viewLeft = ResizableView(frame: CGRect(x: 0, y: viewSubject.frame.origin.y, width: 0, height: viewSubject.frame.height))
        viewLeft.backgroundColor = UIColor.black
        viewLeft.alpha = 0.7
        viewRight = ResizableView(frame: CGRect(x: view.frame.width, y: viewSubject.frame.origin.y, width: 0, height: viewSubject.frame.height))
        viewRight.backgroundColor = UIColor.black
        viewRight.alpha = 0.7
        
        view.addSubview(viewTop)
        view.addSubview(viewBottom)
        view.addSubview(viewLeft)
        view.addSubview(viewRight)
    }
    
    private func removeCustomViews() {
        viewTop.removeFromSuperview()
        viewBottom.removeFromSuperview()
        viewLeft.removeFromSuperview()
        viewRight.removeFromSuperview()
        viewSubject.removeFromSuperview()
        // recordView?.removeFromSuperview()
        timerSeconds = kCollectionVideoMaxDuration
    }
    
    private func startTimer() {
        
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true, block: { [weak self] (timer) in
            guard let self = self else { return }
            self.timerSeconds -= 1
            let value = "\(kCollectionVideoMaxDuration - self.timerSeconds)"
            self.recordView?.timeLbl.text = "00:\(value.count == 1 ? ("0" + value) : value)"
            
            if self.timerSeconds == 0 {
                DispatchQueue.main.after(TimeInterval.zero + 0.08) {
                    self.showProgress()
                    self.videoTimerEnds = true
                    self.stopVideoRecording()
                    timer.invalidate()
                    self.addSubjectEndFrame()
                }
            }
        })
    }
    
    private func addSubjectEndFrame() {
        
        if activities.count > 0 && activities.last?.endFrame == nil {
            activities[activities.count-1].endFrame = ((self.faceMaskRecorder?.frameIndex ?? 0) < 0 ? 0 : (self.faceMaskRecorder?.frameIndex ?? 0))
        }
        
        let tempActivities = activities.filter({$0.endFrame != 0})
        
        if tempActivities.count != activities.count {
            activities = []
            activities = tempActivities
        }
    }
    
    private func navigateToPreviewVC() {
        
        let dirUrl = createJsonFile()
        
        DispatchQueue.main.async {
            self.hideProgress()
            let previewVC: ConsentVideoPreviewVC = (self.storyboard?.instantiateViewController())!
            
            print("Video Url Path: \(self.videoUrlPath?.absoluteString ?? "")")
            print("Json Url Path: \(dirUrl)")
            previewVC.arrOfObject = ProjectService.instance.currentCollection?.objectsList == "" ? [] : (ProjectService.instance.currentCollection?.objectsList?.components(separatedBy: ",") ?? [])
            previewVC.videoURL = URL(fileURLWithPath: Utilities.getFileUrlStringFromCache(fileName: "video.mp4")) // self.videoUrlPath
            previewVC.fileUrl = dirUrl
            previewVC.previewFlow = .RecordVideo
            
            let arrProgress = self.view.subviews.filter{($0.isKind(of: SVProgressHUD.self))}
            if arrProgress.count > 0 {
                for _ in arrProgress {
                    self.hideProgress()
                }
            }
            
            self.navigationController?.pushViewController(previewVC, animated: true)
            //self.present(previewVC, animated: true, completion: nil)
        }
    }
    
    private func createJsonFile() -> String {
        videoUrlPath = URL(fileURLWithPath: Utilities.getFileUrlStringFromCache(fileName: "video.mp4"))
        let currentCollection = ProjectService.instance.currentCollection
        
        let size = RenderSettings().videoSize
        let duration: TimeInterval = round(CMTimeGetSeconds(AVAsset(url: videoUrlPath!).duration) * 1000)/1000//CMTimeGetSeconds(AVAsset(url: videoUrlPath!).duration)
        let durationVal = duration.truncate(places: 0)
        
        let fps = Double((self.faceMaskRecorder?.frameIndex ?? 0) + 1 )/duration
        let orientation = Utilities.getOrientationInString(value: VideoVariables.deviceOrientationRawValue)
        let user = Collector.currentCollector
        let deviceType = "\(UIDevice().type)"
        let osVersion = UIDevice.current.systemVersion
        let currentUTCDate = DateHelper.dateUTCCurrentDate(dateformateString: "yyyy-MM-dd'T'HH:mm:ssZ")
        let ipAddress: String = Utilities.getPublicIPAddress()
        
        let metaData = Metadata(projectID: currentCollection?.projectId ?? "Person", collectionID: currentCollection?.collectionId ?? "", collectorID: user.userId, collectionName: currentCollection?.collectionName ?? "", projectName: currentCollection?.projectName ?? "", programName: currentCollection?.programName ?? "", subjectIDS: [ConsentResponse.instance.subjectID ?? ""], videoID: UUID().uuidString, deviceType: deviceType, deviceIdentifier: "ios", osVersion: osVersion, collectedDate: currentUTCDate, blurredFaces: blurredFaces, frameRate: fps, frameWidth: Double(size.width), frameHeight: Double(size.height), duration: durationVal, orientation: orientation, appVersion: Utilities.getAppVersion(), ipAddress: ipAddress, activityLongNames: currentCollection?.activities ?? "", activityShortNames: currentCollection?.activityShortNames ?? "")
                
        let objects = Object(label: currentCollection?.defaultObject ?? "Person", boundingBox: boundindBox)
        let data = WelcomeAtEncode(metadata: metaData, activity: activities, object: [objects])
        let body = data.getJsonBody()
        //print("body---\(body)")
        
        
        let fileUrl = Utilities.saveJsonToFile(dic: body, fileName: "videoinfo.json")
        return fileUrl!.absoluteString
    }
    
    func getIP()-> String? {
        
        var address: String?
        var ifaddr: UnsafeMutablePointer<ifaddrs>? = nil
        if getifaddrs(&ifaddr) == 0 {
            
            var ptr = ifaddr
            while ptr != nil {
                defer { ptr = ptr?.pointee.ifa_next }
                
                let interface = ptr?.pointee
                let addrFamily = interface?.ifa_addr.pointee.sa_family
                if addrFamily == UInt8(AF_INET) || addrFamily == UInt8(AF_INET6) {
                    let name: String = String(cString: (interface?.ifa_name)!)
                    if name == "en0" {
                        var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                        getnameinfo(interface?.ifa_addr, socklen_t((interface?.ifa_addr.pointee.sa_len)!), &hostname, socklen_t(hostname.count), nil, socklen_t(0), NI_NUMERICHOST)
                        address = String(cString: hostname)
                    }
                }
            }
            freeifaddrs(ifaddr)
        }
        return address
    }
    
    private func enableOrDisableInfoButton(enable: Bool) {
        if enable {
            self.buttonInfo.isEnabled = true
            self.buttonToggleCamera.isEnabled = true
        } else {
            self.buttonInfo.isEnabled = false
            self.buttonToggleCamera.isEnabled = false
        }
    }
    
    // Custom Pop Up to Present Collection Description
    private func setUpCustomView() {
        
        guard let popUp = CustomPopUp.instanceFromNib()
            else { return }
        popUp.delegate = self
        customPopUp = popUp
        popUp.viewBackground.cornerRadius = 10
        popUp.alpha = 0
        //popUp.viewHeightConstant.constant = 270
        //popUp.titleTextView.text = ProjectService.instance.currentCollection?.collectionDescription ?? ""
        
        let str1 = "<span style=\"font-family: '-apple-system'; font-size:15px;font-style:Medium;\">\(ProjectService.instance.currentCollection?.collectionDescription?.replacingOccurrences(of: "\n", with: "<br>") ?? "")</span>"
        let htmlData = NSString(string: str1).data(using: String.Encoding.utf8.rawValue)
        let options = [NSAttributedString.DocumentReadingOptionKey.documentType: NSAttributedString.DocumentType.html]
        let attributedString = try! NSAttributedString(data: htmlData!,
        options: options,
        documentAttributes: nil)
        popUp.titleTextView.attributedText = attributedString
        
        if popUp.titleTextView.text?.height(withConstrainedWidth: ScreenSize.SCREEN_WIDTH - 110, font: popUp.titleTextView.font!) ?? 0 < (ScreenSize.SCREEN_HEIGHT - 200) {
            popUp.viewHeightConstant.constant = (popUp.titleTextView.text?.height(withConstrainedWidth: ScreenSize.SCREEN_WIDTH - 110, font: popUp.titleTextView.font!) ?? 0) + 90
        } else {
            popUp.viewHeightConstant.constant = ScreenSize.SCREEN_HEIGHT - 200
        }
        
        self.view.addSubview(popUp)
        popUp.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            popUp.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
            popUp.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
            popUp.topAnchor.constraint(equalTo: self.view.topAnchor),
            popUp.bottomAnchor.constraint(equalTo: self.view.bottomAnchor)
        ])
        UIView.animate(withDuration: 0.5, delay: 0.1, options: [.curveEaseIn], animations: { [weak self] in
            guard let _ = self else {return}
                popUp.alpha = 1
            }, completion: nil
        )
    }

    private func removeCustomView() {
        if let popUp = self.customPopUp {
            UIView.animate(withDuration: 0.5, delay: 0.1, options: [.curveEaseOut], animations: { [weak self] in
                guard let _ = self else {return}
                    popUp.alpha = 0
                }, completion: { (finished) in
                    popUp.removeFromSuperview()
            })
        }
    }
    
    private func handleTorchButton() {
        isTorchAvailable { [weak self] in
            guard let self = self else { return }
            self.toggleFlashButton.isEnabled = self.currentCamera == .front ? false : $0
        }
    }
}

//MARK: Save Video
extension RecordVideoVC {
    final class func saveVideo(withUrl url : URL,
                               completion: @escaping (_ status: Bool,_ url: URL?) -> ()) {
        print(url,"video url---\(PHPhotoLibrary.authorizationStatus().rawValue)")
        
        func save() {
            DispatchQueue.global(qos: .userInitiated).async {
                
                PHPhotoLibrary.shared().performChanges({
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: url)
                }) { saved, error in
                    if saved {
                        completion(true, url)
                    }
                    if error != nil {
                        completion(false, nil)
                    }
                }
            }
        }
        
        if PHPhotoLibrary.authorizationStatus() == .authorized {
            save()
        } else if (PHPhotoLibrary.authorizationStatus() == .denied || PHPhotoLibrary.authorizationStatus() == .restricted) {
            completion(false, nil)
        } else {
            PHPhotoLibrary.requestAuthorization({ (status) in
                if status == .authorized {
                    save()
                }
            })
    }
        
    }
}

//MARK:- Actions
extension RecordVideoVC {
    
    @IBAction func toggleFlash(_ sender : UIButton) {
        
        self.flashEnabled.toggle()
        
        if !flashEnabled {
            toggleTorch(on: false)
            toggleFlashButton.setImage(#imageLiteral(resourceName: "flash_off"), for: .normal)
        } else {
            toggleTorch(on: true)
            toggleFlashButton.setImage(#imageLiteral(resourceName: "flash_on"), for: .normal)
        }
        
        sender.isSelected = !sender.isSelected
    }
    
    @IBAction func closeButtonAction(_ sender : UIButton) {
        UIUtilities.showAlertMessageWithTwoActionsAndHandler("Close", errorMessage: kExitCollection, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
            
            self.deinitialisingTasks()
            self.navigationController?.popToRootViewController(animated: true)
        }) {
        }
    }
    
    @IBAction func backButtonAction(_ sender: UIButton) {
        if (ProjectService.instance.currentCollection?.isTrainingVideoEnabled ?? false) && ProjectService.instance.trainingVideoUrl != "" {
            self.navigationController?.popViewController(animated: true)
        } else if (ProjectService.instance.currentCollection?.isConsentRequired ?? false) {
            self.navigationController?.popToViewController(ofClass: ConsentEmailVC.self)
        } else {
            self.navigationController?.popToViewController(ofClass: ActivityViewController.self)
        }
    }
    
    @IBAction func infoButtonAction(_ sender: UIButton) {
        self.setUpCustomView()
    }
    
    @IBAction func switchCameraButtonAction(_ sender: UIButton) {
        do {
            self.showProgress()
            try switchCamera()
        } catch {
            self.hideProgress()
            print(error as Any)
        }
        
        self.handleTorchButton()
    }
}

//MARK: CustomPopUP Delegate Methods
extension RecordVideoVC: CustomPopUpDelegate {
    
    func didTapOnOkBtn() {
        self.removeCustomView()
    }
}

//MARK: VideoController Delegate Methods
extension RecordVideoVC : VideoControllerDelegate {
    
    func cameraController(_ cameraController: VideoRecorder, didVideoOutputSampleBufferChange buffer: Bool) {

        if toggleFlashButton.imageView?.image == #imageLiteral(resourceName: "flash_on") && !isVideoRecording && torchMode() == .off {
            toggleTorch(on: true)
        }
        
        if !videoProcessingBegins {
            self.hideProgress()
        }
    }
    
    func cameraController(_ cameraController: VideoRecorder, didChangeFrame index: Int) {
        
        let size = subjectCurrentFrame?.size ?? CGSize.zero
        let origin = subjectCurrentFrame?.origin ?? CGPoint.zero
        
        let frame = Frame(height: Int(size.height), width: Int(size.width), x: Int(origin.x), y: Int(origin.y))
        let box = BoundingBox(zframe: frame, index: index)
        boundindBox.append(box)
    }
    
    
    func cameraController(_ cameraController: VideoController, didTake photo: UIImage) {
        //saveImageToCameraRoll(image: photo)
    }
    
    func cameraController(_ cameraController: VideoController, didFinishProcessVideoAt url: URL) {
        self.videoProcessingBegins = true
        self.showProgress()
        self.timer?.invalidate()
        self.addSubjectEndFrame()
        UIView.animate(withDuration: 0.2) { [weak self] in
            guard let self = self else {return}
            self.recordView?.updateButtonView(isVideoRecording: self.isVideoRecording)
        }
        enableOrDisableInfoButton(enable: true)
        DispatchQueue.global(qos: .utility).async { [weak self] in
          self?.updateBlurOnVideo(url: url)
        }
        print("casualurl---\(url)")
    }
    
    func cameraController_enableButtonView() {
        self.recordView.enableButtonView()
    }
    
    func cameraController_disableButtonView() {
        self.recordView.disableButtonView()
    }
    
    func cameraController(_ faceMaskRecorder: VideoRecorder,
                          didUpdate maskFrameRect: CGRect?,bufferFrameRect: CGRect?) {
        
    }
    
    func cameraController(_ cameraController: VideoController, didChangeZoomLevel zoom: CGFloat, zView: UIView, direction: String, gesture: UIPinchGestureRecognizer, location: CGPoint) {
        
        if direction != "" {
            applyPinchGestureOnSubject(scale: zoom, position: direction, pinch: gesture, touch: location)
        }
        
        zoomedView = zView
        if isVideoRecording {
            saveSubjectCordinatesOnChange()
        }
    }
    
    
    func cameraController(_ cameraController: VideoController, didBeginRecordingVideo camera: VideoController.CameraSelection, zView: UIView) {
        
        UIView.animate(withDuration: 0.2) { [weak self] in
            guard let self = self else {return}
            self.recordView?.timeLbl.isHidden = false
            self.recordView?.updateButtonView(isVideoRecording: self.isVideoRecording)
        }
        self.startTimer()
        zoomedView = zView
        var zframe = view.convert(viewSubject.frame, to: zView)
        updateCurrentSubjectRect(rect: zframe)
        
        if activities.count > 0 && activities.last?.endFrame == nil {
            if let lastActivity = activities.last {
                activities = []
                activities.append(lastActivity)
            } else {
                activities = []
            }
        } else {
            activities = []
        }
        
        zframe = self.subjectCurrentFrame ?? .zero
        let zCordinate = Cordinate(zframe: Frame(height: Int(zframe.size.height), width: Int(zframe.size.width), x: Int(zframe.origin.x), y: Int(zframe.origin.y)), ztime: (kCollectionVideoMaxDuration - timerSeconds))
        cordinates.append(zCordinate)
        
        enableOrDisableInfoButton(enable: false)
        
    }
     
    func cameraController(_ cameraController: VideoController, didSingleFocusAtPoint point: CGPoint) {
        var indexPathForDeselction: IndexPath
        var indexPath1: IndexPath?
        let valLastSelectedRow : Int = activities.last?.selectedIndex ?? 0
        indexPathForDeselction = IndexPath(item: valLastSelectedRow, section: 0)
        
        let indexZero = IndexPath(item: 0, section: 0)
        
        if self.faceMaskRecorder?.frameIndex == 0 || self.faceMaskRecorder?.frameIndex == 1 {
            return
        }
        
        if listOfActivities.count > 0 && !checkTapPointWithInRadius(point: point) && !collectionView.frame.contains(point) && !videoTimerEnds {
            if indexPathVisibalityDetector(indexPathForDeselction, listOfActivities.count) {
                if activities.count > 0 && activities.last?.endFrame == nil {
                    activities[activities.count-1].endFrame = ((self.faceMaskRecorder?.frameIndex ?? 0) < 0 ? 0 : (self.faceMaskRecorder?.frameIndex ?? 0)) - 1
                    let item = collectionView.cellForItem(at: indexPathForDeselction) as? ActivitiesCollectionViewCell
                    item?.labelActivity.backgroundColor = UIColor.darkGray
                    item?.isSelected = false
                }
                else if activities.count > 0 && valLastSelectedRow == listOfActivities.count - 1 { //Last Activity Hard Coding
//                    let selectedRow = activities.last?.selectedIndex ?? 0
//                    if valLastSelectedRow == listOfActivities.count - 1 { // not a double Tap //&& !doubleTapToggle
                        indexPath1 = indexZero
//                    }
//                    else {
//                        indexPath1 = IndexPath(item: valLastSelectedRow , section: 0)
//                    }
                    let item1 = collectionView.cellForItem(at: indexPath1!) as? ActivitiesCollectionViewCell
                    item1?.labelActivity.backgroundColor = UIColor.appColor(.main)
                    item1?.isSelected = true
                    let selectedActivity = Activity(activityName: listOfActivities[indexPath1!.row], sTime: ((self.faceMaskRecorder?.frameIndex ?? 0) < 0 ? 0 : (self.faceMaskRecorder?.frameIndex ?? 0)), activityIndex: indexPath1!.row)
//                    if !isVideoRecording {
//                        activities.removeAll()
//                    }
                    activities.append(selectedActivity)
                    collectionView.selectItem(at: indexPath1, animated: true, scrollPosition: .right)
                    doubleTapToggle = false
                    return //IMP
                }
                
                //ONLY SELECTION
                doubleTapToggle = false
                if ((activities.last?.selectedIndex ?? -1) + 1) < listOfActivities.count {
                    indexPath1 = IndexPath(item: ((activities.last?.selectedIndex ?? -1) + 1), section: 0) //IndexPath(item: activities.count, section: 0)
                    
                    let item1 = collectionView.cellForItem(at: indexPath1!) as? ActivitiesCollectionViewCell
                    item1?.labelActivity.backgroundColor = UIColor.appColor(.main)
                    item1?.isSelected = true
                    //let selectedActivity = Activitylabel(activityName: listOfActivities[indexPath1!.row], sTime: (30 - timerSeconds), activityIndex: indexPath1!.row)
                    let selectedActivity = Activity(activityName: listOfActivities[indexPath1!.row], sTime: ((self.faceMaskRecorder?.frameIndex ?? 0) < 0 ? 0 : (self.faceMaskRecorder?.frameIndex ?? 0)), activityIndex: indexPath1!.row)
                    
//                    if !isVideoRecording {
//                        //            activities.removeAll()
//                    }
                    activities.append(selectedActivity)
                    collectionView.selectItem(at: indexPath1, animated: true, scrollPosition: .right)
                }
                else {
                    print("DoubleTap---\(String(describing: activities.last?.selectedIndex))")
                    
                }
            }
        }
    }
    
    func indexPathVisibalityDetector(_ indexPathInconcern : IndexPath, _ lastItemRow : Int) -> Bool {
        let selectedRow = indexPathInconcern.row
        let previousRow = indexPathInconcern.row - 1
        
        let previousIndexPath : IndexPath
        var finalIndexToScroll : IndexPath?
        
        if selectedRow > -1 && selectedRow < lastItemRow - 1 {
            previousIndexPath = IndexPath(item: previousRow, section: indexPathInconcern.section)
            
            if let itemPrevious = collectionView.cellForItem(at: indexPathInconcern) as? ActivitiesCollectionViewCell {
                
                let cellBounds = itemPrevious.contentView.convert(itemPrevious.contentView.bounds, to: UIScreen.main.coordinateSpace)
                if UIScreen.main.bounds.intersects(cellBounds) && activities.last?.endFrame != nil && indexPathInconcern.row != 0 {
                    finalIndexToScroll = indexPathInconcern //previousIndexPath
                }
                else {
                    finalIndexToScroll = indexPathInconcern
                }
            }
            else { //Hidden Cell
                if activities.count > 0 && activities.last?.selectedIndex ?? 0 > 0 {//Hidden Cell and deselected (DoubleTap)
                    finalIndexToScroll = previousIndexPath
                }
                else {
                    finalIndexToScroll = indexPathInconcern
                }
            }
        }
        else if selectedRow > -1 && selectedRow == lastItemRow - 1 {
            if activities.count > 0 && activities.last?.endFrame == nil {
                let lastIndex = IndexPath(item: lastItemRow - 1, section: 0)
                collectionView.scrollToItem(at: lastIndex, at: UICollectionView.ScrollPosition.right, animated: true) //right
                return true // IMP
            }
            else if !doubleTapToggle {
                let firstIndex = IndexPath(item: 0, section: 0)
                finalIndexToScroll = firstIndex
            }
        }
        
        guard let finalIndex = finalIndexToScroll else {
            return true
        }
        collectionView.scrollToItem(at: finalIndex, at: UICollectionView.ScrollPosition.left, animated: true)
        return true
    }
  
    func cameraController(_ cameraController: VideoController, didDoubleFocusAtPoint point: CGPoint) {
        
        if !collectionView.frame.contains(point) && !videoTimerEnds {
            doubleTapToggle = false
            if activities.count > 0 && activities.last?.endFrame == nil {
              let indexPath = IndexPath(item: activities.last!.selectedIndex, section: 0)
              let item = collectionView.cellForItem(at: indexPath) as? ActivitiesCollectionViewCell
              item?.labelActivity.backgroundColor = UIColor.darkGray
              item?.isSelected = false
              activities[activities.count-1].endFrame = ((self.faceMaskRecorder?.frameIndex ?? 0) < 0 ? 0 : (self.faceMaskRecorder?.frameIndex ?? 0)) - 1
            } else if activities.count > 0 {
              let indexPath = IndexPath(item: activities.last!.selectedIndex, section: 0)
              let item = collectionView.cellForItem(at: indexPath) as? ActivitiesCollectionViewCell
              item?.labelActivity.backgroundColor = UIColor.appColor(.main)
              item?.isSelected = true
              activities[activities.count-1].endFrame = nil
            }
        }
    }
}

extension RecordVideoVC: ActivitiesCollectionCellDelegate {
    
    func didTapOnActivity(index: Int) {
        
        if videoTimerEnds {
            return
        }
        
        var indexPathForDeselction: IndexPath
        var indexPath1: IndexPath?
        let valLastSelectedRow : Int = activities.last?.selectedIndex ?? 0
        indexPathForDeselction = IndexPath(item: valLastSelectedRow, section: 0)
        var previousIndexPath = IndexPath.init(item: index, section: 0)
        
        if index > 0 && index < listOfActivities.count - 1 {
            previousIndexPath = IndexPath.init(item: index - 1, section: 0)
        }
            
        if activities.count > 0 && activities.last?.endFrame == nil {
            activities[activities.count-1].endFrame = ((self.faceMaskRecorder?.frameIndex ?? 0) < 0 ? 0 : (self.faceMaskRecorder?.frameIndex ?? 0)) - 1
            let item = collectionView.cellForItem(at: indexPathForDeselction) as? ActivitiesCollectionViewCell
            item?.labelActivity.backgroundColor = UIColor.darkGray
            item?.isSelected = false
            
            if index != valLastSelectedRow {
                indexPath1 = IndexPath(item: index, section: 0) //IndexPath(item: activities.count, section: 0)
                                
                let item1 = collectionView.cellForItem(at: indexPath1!) as? ActivitiesCollectionViewCell
                item1?.labelActivity.backgroundColor = UIColor.appColor(.main)
                item1?.isSelected = true
                let selectedActivity = Activity(activityName: listOfActivities[indexPath1!.row], sTime: ((self.faceMaskRecorder?.frameIndex ?? 0) < 0 ? 0 : (self.faceMaskRecorder?.frameIndex ?? 0)), activityIndex: indexPath1!.row)
                
                activities.append(selectedActivity)
                collectionView.selectItem(at: indexPath1, animated: true, scrollPosition: .right)
                doubleTapToggle = false
            }
        } else {
            indexPath1 = IndexPath(item: index, section: 0) //IndexPath(item: activities.count, section: 0)
                            
            let item1 = collectionView.cellForItem(at: indexPath1!) as? ActivitiesCollectionViewCell
            item1?.labelActivity.backgroundColor = UIColor.appColor(.main)
            item1?.isSelected = true
            let selectedActivity = Activity(activityName: listOfActivities[indexPath1!.row], sTime: ((self.faceMaskRecorder?.frameIndex ?? 0) < 0 ? 0 : (self.faceMaskRecorder?.frameIndex ?? 0)), activityIndex: indexPath1!.row)
            
            activities.append(selectedActivity)
            collectionView.selectItem(at: indexPath1, animated: true, scrollPosition: .right)
            doubleTapToggle = false
        }
        
        let _ = indexPathVisibalityDetector(previousIndexPath, listOfActivities.count)
    }
}

//MARK: CollectionView DataSource
extension RecordVideoVC: UICollectionViewDataSource {
    
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return listOfActivities.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        
        // get a reference to our storyboard cell
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier:
            ActivitiesCollectionViewCell.reuseIdentifier, for: indexPath as IndexPath) as! ActivitiesCollectionViewCell
        
        // Use the outlet in our custom class to get a reference to the UILabel in the cell
        cell.labelActivity.text = "   " + listOfActivities[indexPath.row] + "   "
        cell.labelActivity.backgroundColor = UIColor.darkGray
        cell.delegate = self
        cell.inderPathRow = indexPath.row
        if let valBackgroundDecider = activities.last?.selectedIndex, valBackgroundDecider == indexPath.row { // as the collection view will refresh regularly and the selected row has to be set
            cell.labelActivity.backgroundColor = UIColor.appColor(.main)
        }
        return cell
        
    }
    
}

//MARK: Video Blur
extension RecordVideoVC {
    
    func updateBlurOnVideo(url: URL) {
        let asset = AVAsset(url: url)
        createVideoComposition(for: asset)
    }
    
     
    private func createVideoComposition(for asset: AVAsset) {
         print(Thread.current.isMainThread,"Main thread checker")
//        let faceDetector = CIDetector(ofType: CIDetectorTypeFace,
//                                      context: nil,
//                                      options: [
//                                        CIDetectorAccuracy: CIDetectorAccuracyLow,
//                                        CIDetectorTracking: true
//        ])!
        
        let faceDetector = CIDetector(ofType: CIDetectorTypeFace,
                                      context: nil,
                                      options: [CIDetectorAccuracy: CIDetectorAccuracyLow])!
       
        //CIDetectorMinFeatureSize: 0.1,
        //CIDetectorNumberOfAngles: 50
        var count = 0
        let videoComposition = AVVideoComposition(asset: asset) { request in

            DispatchQueue.global(qos: .background).async { [weak self] in
                guard let self = self else { return }
                let sourceImage = request.sourceImage
                //print("encode time: ",request.compositionTime.value)
                var outerSubjectFrame: CGRect = .zero
                
                if let box = self.boundindBox.first(where: {$0.frameIndex == count}) {
                    let frame = box.frame
                    outerSubjectFrame = CGRect(x: frame.x, y: frame.y, width: frame.width, height: frame.height)
                }
                count += 1
                
//                if let index = self.cordinates.lastIndex(where: {$0.time <= Int(request.compositionTime.seconds)}) {
//                  let frame = self.cordinates[index].frame
//                  //self?.updateCurrentSubjectRect(rect: CGRect(x: frame!.x, y: frame!.y, width: frame!.width, height: frame!.height))
//                  //outerSubjectFrame = self?.subjectFrameCurrentRect() ?? .zero
//                    outerSubjectFrame = CGRect(x: frame.x, y: frame.y, width: frame.width, height: frame.height)
//                }
              
                let features = faceDetector.features(in: sourceImage)
                .compactMap ({ $0 as? CIFaceFeature })
                let filteredImage = self.blurFaces(on: sourceImage, features: features, subjectFrame: outerSubjectFrame)
                
                if filteredImage != nil {
                    request.finish(with: filteredImage!, context: nil)
                } else {
                    request.finish(with: sourceImage, context: nil)
                }
            }
        }
        
        saveBlurredVideo(videoAsset: asset, videoComposition: videoComposition)
        self.navigateToPreviewVC()
    }
    
    func saveBlurredVideo(videoAsset: AVAsset, videoComposition: AVVideoComposition) {
        if videoAsset.isPlayable && videoAsset.isReadable && videoAsset.isExportable && videoAsset.isComposable {
            
            let operationExportSession = BlockOperation {
                RecordVideoVC.valBlurActionStatus = "InProgress"
                print("RecordVideoVC.valBlurActionStatus1---\(RecordVideoVC.valBlurActionStatus)")
                
                let exporter = VideoSessionExporter(withAsset: videoAsset)
                exporter.outputFileType = AVFileType.mp4
                let tmpURL = URL(fileURLWithPath: Utilities.getFileUrlStringFromCache(fileName: "videoBlurred.mp4"))
                exporter.outputURL = tmpURL
                
                let compressionDict: [String: Any] = [
                    AVVideoAverageBitRateKey: NSNumber(integerLiteral: 2300000),
                    AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel as String,
                ]
                let size = RenderSettings().videoSize
                
                exporter.videoOutputConfiguration = [
                    AVVideoCodecKey: AVVideoCodecType.h264,
                    AVVideoWidthKey: NSNumber(integerLiteral: Int(size.width)),
                    AVVideoHeightKey: NSNumber(integerLiteral: Int(size.height)),
                    AVVideoScalingModeKey: AVVideoScalingModeResizeAspectFill,
                    AVVideoCompressionPropertiesKey: compressionDict
                ]
                exporter.videoComposition = videoComposition
                
                Utilities.removeFileFromCache(fileName: "videoBlurred.mp4")
                
                exporter.export(progressHandler: { (progress) in
                    //                    print("progress---\(progress)")
                }, completionHandler: { result in
                    switch result {
                    case .success(let status):
                        switch status {
                        case .completed:
                            print("VideoSessionExporter, export completed, \(exporter.outputURL?.description ?? "")")
                            
                            RecordVideoVC.valBlurActionStatus = "Completed"
                            //self.navigateToPreviewVC()
                            break
                            
                        default:
                            print("RecordVideoVC.valBlurActionStatus---\(RecordVideoVC.valBlurActionStatus)")
                            //                        DispatchQueue.main.async {
                            //                            RecordVideoVC.valBlurActionStatus = "Completed"
                            //                        }
                            break
                        }
                        break
                        
                    case .failure(let error):
                        print("VideoSessionExporter, failed to export \(error)")
                        
                        RecordVideoVC.valBlurActionStatus = "InProgress"
                        
                        if RecordVideoVC.valDidEnterBackground &&
                            videoAsset.isPlayable && videoAsset.isReadable && videoAsset.isExportable && videoAsset.isComposable {
                            
                            RecordVideoVC.valDidEnterBackground = false
                            exporter.reset()
                            
                            self.saveBlurredVideo(videoAsset: videoAsset, videoComposition: videoComposition)
                        }
                        else {
                            RecordVideoVC.valBlurActionStatus = "Failed"
                        }
                        
                        break
                    }
                })
            }
            
            self.queueOperationJson.addOperation(operationExportSession)
            RecordVideoVC.operationJson = operationExportSession
            
        }
    }
   
    public func blurFaces(on ciImage: CIImage, features: [CIFaceFeature], subjectFrame: CGRect) -> CIImage? {
        
        let pixelateFiler = CIFilter(name: "CIPixellate")
        pixelateFiler?.setValue(ciImage.clampedToExtent(), forKey: kCIInputImageKey)
        pixelateFiler?.setValue(max(ciImage.extent.width, ciImage.extent.height) / 60.0, forKey: kCIInputScaleKey)
        
        
        var maskImage: CIImage?
        var featuresCount = 0
        for feature in features {
            
            //let faceRect = feature.bounds
            let centerX = feature.bounds.origin.x + feature.bounds.size.width / 2.0
            let centerY = feature.bounds.origin.y + feature.bounds.size.height / 2.0
            let radius = min(feature.bounds.size.width, feature.bounds.size.height) / 1.8
            
            let tempRect = CGRect(x: centerX - radius, y: centerY - radius, width: radius * 2, height: radius * 2)
            
            if subjectFrame.intersects(tempRect) {
                continue
            }
            
            featuresCount += 1
            
            let radialGradient = CIFilter(name: "CIRadialGradient")
            radialGradient?.setValue(radius, forKey: "inputRadius0")
            radialGradient?.setValue(radius + 1, forKey: "inputRadius1")
            radialGradient?.setValue(CIColor(red: 0, green: 1, blue: 0, alpha: 1), forKey: "inputColor0")
            radialGradient?.setValue(CIColor(red: 0, green: 0, blue: 0, alpha: 0), forKey: "inputColor1")
            radialGradient?.setValue(CIVector(x: centerX, y: centerY), forKey: kCIInputCenterKey)
            
            let circleImage = radialGradient?.outputImage?.cropped(to: ciImage.extent)
            
            if (maskImage == nil) {
                maskImage = circleImage
            } else {
                let filter =  CIFilter(name: "CISourceOverCompositing")
                filter?.setValue(circleImage, forKey: kCIInputImageKey)
                filter?.setValue(maskImage, forKey: kCIInputBackgroundImageKey)
                maskImage = filter?.outputImage
            }
        }
        
        let composite = CIFilter(name: "CIBlendWithMask")
        composite?.setValue(pixelateFiler?.outputImage, forKey: kCIInputImageKey)
        composite?.setValue(ciImage, forKey: kCIInputBackgroundImageKey)
        composite?.setValue(maskImage, forKey: kCIInputMaskImageKey)
        blurredFaces = featuresCount > blurredFaces ? featuresCount : blurredFaces
        return composite?.outputImage
        
    }
    
}


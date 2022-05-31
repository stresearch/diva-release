//
//  ConsentVideoPreviewVC.swift
//  STR
//
//  Created by Srujan on 07/01/20.
//  
//

import UIKit
import AVFoundation
import AWSAppSync

enum VideoPreviewFlow {
    case DemoVideo
    case ConsentVideo
    case RecordVideo
    case MyVideos
    case Ratings
}

class ConsentVideoPreviewVC: UIViewController {
    
    @IBOutlet weak var barView: UIView!
    @IBOutlet weak var buttonClose: UIButton!
    @IBOutlet weak var buttonRetake: UIButton!
    @IBOutlet weak var buttonEdit: UIButton!
    @IBOutlet weak var buttonCloseConstraintTop: NSLayoutConstraint!
    @IBOutlet weak var buttonRetakeConstraintTop: NSLayoutConstraint!
    @IBOutlet weak var buttonCloseConstraintLeading: NSLayoutConstraint!
    @IBOutlet weak var buttonRetakeConstraintTrailing: NSLayoutConstraint!
    
    // <JEBYRNE: Info button>
    @IBOutlet weak var buttonInfo: UIButton!
    var collectionDescription = ""
    private var customPopUp: CustomPopUp?
    // </JEBYRNE>
    
    var scrubBarView: VideoScrubBarView?
    
    var videoURL: URL!
    var fileUrl: String!
    var serverTraining: Bool = false
    var previewFlow = VideoPreviewFlow.RecordVideo
    fileprivate lazy var player = PlayerController()
    
    //var isComingFromConsentVideo = false
    var subjectView = ResizableView()
    var activityLabel = UILabel()
    var subjectCordinates: [BoundingBox]? //[Cordinate]?
    var subjectActivities: [Activity]? //[Activitylabel]?
    var frameCount = 0
    var fileName = ""
    
    var uploadState: Bool = false // False - Not started or Completed
        
    var timerSubmitBlurr: Timer?
    
    lazy var ratingQuestionInstances: [RatingInstance] = []
    lazy var ratingQuestionResponse = CreateStrRatingInput(id: "", reviewerId: "")
    lazy var updatedRatingQuestionResponse = UpdateStrRatingInput(id: "", reviewerId: "")
    var previewScrubHt : NSLayoutConstraint?
    
    var safeAreaValueForVideo: CGFloat = UIDevice.current.hasNotch ? 73 : 0
    var screenHeightWithSafeArea: CGFloat = UIDevice.current.hasNotch ? (73 * 2) : 0
    
    var subjectEditedCordinates: [Object] = []
    var arrOfSubjectView: [ResizableView] = []
    var arrOfObject: [String] = [] {
        didSet {
            addObjectViews()
        }
    }
    
    // MARK: object lifecycle
    
    deinit {
        self.player.willMove(toParent: nil)
        self.player.view.removeFromSuperview()
        self.player.removeFromParent()
        Log("\(self) I'm gone ")
    }
    
    
    // MARK: view lifecycle
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.view.backgroundColor = UIDevice.current.hasNotch ? .black : .white
        
        if previewFlow == .MyVideos || previewFlow == .Ratings {
            self.navigationController?.isNavigationBarHidden = true
        }
        
        if (previewFlow == .RecordVideo || previewFlow == .MyVideos || previewFlow == .Ratings) || previewFlow == .DemoVideo {
            if VideoVariables.deviceOrientationRawValue < 3  {
                if UIDevice.current.orientation != .portrait {
                    DispatchQueue.main.async {
                        let value = UIInterfaceOrientation.portrait.rawValue
                        UIDevice.current.setValue(value, forKey: "orientation")
                        
                        AppUtility.lockOrientation(.portrait)
                    }
                }
            }
            else if VideoVariables.deviceOrientationRawValue == 4 {
                if UIDevice.current.orientation != .landscapeLeft {
                    DispatchQueue.main.async {
                        let value1 = UIInterfaceOrientation.portrait.rawValue
                        UIDevice.current.setValue(value1, forKey: "orientation")
                        let value = UIInterfaceOrientation.landscapeLeft.rawValue
                        UIDevice.current.setValue(value, forKey: "orientation")
                        
                        AppUtility.lockOrientation(.landscapeLeft)
                    }
                }
            }
            else {
                if UIDevice.current.orientation != .landscapeRight {
                    DispatchQueue.main.async {
                        let value1 = UIInterfaceOrientation.portrait.rawValue
                        UIDevice.current.setValue(value1, forKey: "orientation")
                        let value = UIInterfaceOrientation.landscapeRight.rawValue
                        UIDevice.current.setValue(value, forKey: "orientation")
                        
                        AppUtility.lockOrientation(.landscapeRight)
                    }
                }
            }
        }
        else {
            if UIDevice.current.orientation != .portrait {
                let value = UIInterfaceOrientation.portrait.rawValue
                UIDevice.current.setValue(value, forKey: "orientation")
                AppUtility.lockOrientation(.portrait)
            }
        }
        NotificationCenter.default.addObserver(self, selector: #selector(willEnterForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(didEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        
        let valscreenIphone11Height =  ScreenSize.SCREEN_HEIGHT
        if valscreenIphone11Height == 896 {
            safeAreaValueForVideo = UIDevice.current.hasNotch ? 73 + 7 : 0
            screenHeightWithSafeArea = UIDevice.current.hasNotch ? ((73 + 7) * 2) : 0
        } else {
            safeAreaValueForVideo = UIDevice.current.hasNotch ? 73 : 0
            screenHeightWithSafeArea = UIDevice.current.hasNotch ? (73 * 2) : 0
        }
        
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        UIView.setAnimationsEnabled(false)
        
        if self.buttonEdit.isEnabled {
            DispatchQueue.main.async {
                let value1 = UIInterfaceOrientation.landscapeLeft.rawValue
                UIDevice.current.setValue(value1, forKey: "orientation")

                let value = UIInterfaceOrientation.portrait.rawValue
                UIDevice.current.setValue(value, forKey: "orientation")

                AppUtility.lockOrientation(.portrait)
            }
        }
        UIView.setAnimationsEnabled(true)
        NotificationCenter.default.removeObserver(self, name: UIApplication.willEnterForegroundNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: UIApplication.didEnterBackgroundNotification, object: nil)
        
        if videoURL != nil {
            self.player.movedToBeginning()
        }
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        setUpView()
        self.player.fillMode = .resizeAspect //.resizeAspect
        
        if UIDevice.current.hasNotch {
            let valscreenIphone11Height =  ScreenSize.SCREEN_HEIGHT
        
            if VideoVariables.deviceOrientationRawValue < 3  {
                
                if valscreenIphone11Height == 896 {
                    buttonCloseConstraintTop.constant = buttonCloseConstraintTop.constant + 8
                    buttonRetakeConstraintTop.constant = buttonRetakeConstraintTop.constant + 8
                }
            } else {
                if valscreenIphone11Height == 896 {
                    buttonCloseConstraintLeading.constant = buttonCloseConstraintLeading.constant + 18
                    buttonRetakeConstraintTrailing.constant = buttonRetakeConstraintTrailing.constant + 18
                } else {
                    buttonCloseConstraintLeading.constant = buttonCloseConstraintLeading.constant + 10
                    buttonRetakeConstraintTrailing.constant = buttonRetakeConstraintTrailing.constant + 10
                }
                buttonCloseConstraintTop.constant = 20
                buttonRetakeConstraintTop.constant = 20
            }
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        //        print("max duration: ",player.maximumDuration)
        //        print("num of frames: ",player.numberOfFrames)
        //        print("fps: ",Double(player.numberOfFrames)/player.maximumDuration)
        //        print("interval: ",player.maximumDuration/Double(player.numberOfFrames))
        if videoURL != nil {
            self.player.playFromBeginning()
        }
        resetObjectAndActivityLabel()
    }
    
    // MARK: Private Methods and Actions
    
    @objc func willEnterForeground() {
        RecordVideoVC.operationJson?.cancel() //Blurred Video
        if uploadState {
            print("self.showProgress()---yes")
            self.showProgress()
        }
        else {
            print("self.showProgress()---no")
            self.hideProgress()
        }
    }
    
    @objc func didEnterBackground() {
        deinitialisingTasks()
    }
    
    private func deinitialisingTasks() {
        RecordVideoVC.operationJson?.cancel() //Blurred Video
        RecordVideoVC.valDidEnterBackground = true //Blurred Video
        
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarView.self))}.first as? PreviewScrubBarView
        previewScrubHt?.constant = 80
        previewScrubBar?.viewRatingQuestions.isHidden = true
        previewScrubBar?.layoutIfNeeded()
        
        self.hideProgress()
    }
    
    private func setUpView() {
        self.navigationController?.navigationBar.isHidden = true
        switch previewFlow {
        case .ConsentVideo:
            setupScrubber()
        case .RecordVideo:
            self.buttonEdit.isHidden = false
            self.buttonEdit.isEnabled = true
            
            if let jsonData = Utilities.getDataFromDirectory(fileurl: URL(string: fileUrl)!) {
                loadPreviewInfoDetails(jsonData: jsonData)
            }
            setupScrubber()
        case .DemoVideo:
            
            if ProjectService.instance.trainingVideoUrl != "" {

                if serverTraining {
                    if let jsonData = Utilities.getDataFromCacheDirectoryByName(fileurl: URL(fileURLWithPath: fileUrl), filename: "Training1.json") {
                        loadPreviewInfoDetails(jsonData: jsonData)
                    }
                }
                else {
                    if let jsonData = Utilities.getDataFromLocalJSON(filename: fileUrl) {
                        loadPreviewInfoDetails(jsonData: jsonData)
                    }
                }
                setupScrubber()
                
                buttonRetake.isHidden = false
                buttonRetake.setImage(#imageLiteral(resourceName: "close"), for: .normal)
                buttonClose.setImage(#imageLiteral(resourceName: "back_small"), for: .normal)
                
            } else {
                UIUtilities.showAlertMessageWithActionHandler(kTrainingVideoDataMissing, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                        // navigate to activity record
                        let recordVideoVC: RecordVideoVC = (self.storyboard?.instantiateViewController())!
                        // update the navigation root view controller to record video vc
                        // remove all the other vc from memory and re instantiate the tab bar vc
                        // update the navgation bar
                        self.navigationController?.pushViewController(recordVideoVC, animated: true)
                }
            }
            
        case .MyVideos:
            if let jsonData = Utilities.getDataFromCacheDirectoryByName(fileurl: URL(string: fileUrl)!, filename: fileName) {
                loadPreviewInfoDetails(jsonData: jsonData)
            }
            setupScrubber()
            
            buttonRetake.isHidden = true
            buttonClose.setImage(#imageLiteral(resourceName: "back_small"), for: .normal)
        case .Ratings:
            
            //self.buttonInfo.isHidden = false  // JEBYRNE: enable (?)
            //self.buttonInfo.isEnabled = true

            if let jsonData = Utilities.getDataFromCacheDirectoryByName(fileurl: URL(string: fileUrl)!, filename: fileName) {
                loadPreviewInfoDetails(jsonData: jsonData)
            }
            setUpPreviewScrubber()
            
            buttonRetake.isHidden = true
            buttonClose.setImage(#imageLiteral(resourceName: "back_small"), for: .normal)
            
            //collectionDescription = ProjectService.instance.currentCollection?.collectionDescription ?? ""

        }
        addPlayer()
        
    }
    
    private func addPlayer() {
        //self.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        self.player.playerDelegate = self
        self.player.playbackDelegate = self
        self.player.playerView.playerBackgroundColor = .black
        self.addChild(self.player)
        self.view.insertSubview(self.player.view, at: 0)
        self.player.didMove(toParent: self)
        self.player.url = videoURL
        self.player.playbackLoops = false
        self.player.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            self.player.view.topAnchor.constraint(equalTo: self.view.topAnchor),
            self.player.view.bottomAnchor.constraint(equalTo: self.view.bottomAnchor),
            self.player.view.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
            self.player.view.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
        ])
        let tapGestureRecognizer: UITapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(handleTapGestureRecognizer(_:)))
        tapGestureRecognizer.numberOfTapsRequired = 1
        self.player.view.addGestureRecognizer(tapGestureRecognizer)
        
    }
    
    private func resetObjectAndActivityLabel() {
        updateObjectsOnFrameChange(frameIndex: 1, objects: subjectEditedCordinates, animationValue: 0.01)
    }
    
    private func updateObjectsOnFrameChange(frameIndex: Int, objects: [Object], animationValue: Double) {

        UIView.animate(withDuration: animationValue) {
            
            for (index, value) in objects.enumerated() {
                
                if !self.view.subviews.contains(self.arrOfSubjectView[index]) {
                    
                    //self.arrOfSubjectView[index] = ResizableView()
                    //self.arrOfSubjectView[index].alpha = 0
                    self.arrOfSubjectView[index].layer.borderWidth = 2.0
                    self.arrOfSubjectView[index].layer.borderColor = UIColor.appColor(.main)?.cgColor //self.getObjectColor(object: value.label)
                    //self.view.addSubview(self.arrOfSubjectView[index])
                    self.view.insertSubview(self.arrOfSubjectView[index], aboveSubview: self.player.playerView)
                    //self.view.bringSubviewToFront(self.arrOfSubjectView[index])
                    
                    self.arrOfSubjectView[index].isHidden = true
                }
                
                if value.boundingBox.count > 0 {
                    
                    if let box = value.boundingBox.first(where: {$0.frameIndex == (frameIndex - 1)}) {
                        let frame = box.frame
                        let convertedFrame = self.getSubjectFrame(rect: CGRect(x: frame.x, y: frame.y, width: frame.width, height: frame.height))
                        self.arrOfSubjectView[index].frame = convertedFrame
                        
                        self.arrOfSubjectView[index].isHidden = false
                    } else {
                        self.arrOfSubjectView[index].isHidden = true
                        self.activityLabel.isHidden = true
                        self.activityLabel.backgroundColor = UIColor.clear
                        self.activityLabel.frame.size.height = 0
                        self.activityLabel.text = ""
                    }
                } else {
                    self.arrOfSubjectView[index].isHidden = true
                    self.activityLabel.isHidden = true
                    self.activityLabel.backgroundColor = UIColor.clear
                    self.activityLabel.frame.size.height = 0
                    self.activityLabel.text = ""
                }
                
                if (value.label == self.arrOfObject.first) && !self.arrOfSubjectView[index].isHidden {
                    
                    if !self.view.subviews.contains(self.activityLabel) {
                        self.activityLabel.textColor = UIColor.black
                        self.activityLabel.backgroundColor = UIColor.appColor(.main)
                        self.activityLabel.numberOfLines = 10
                        self.activityLabel.text = ""
                        self.activityLabel.textAlignment = .center
                        self.activityLabel.frame = CGRect.zero
                        self.activityLabel.sizeToFit()
                        //self.view.addSubview(self.activityLabel)
                        self.view.insertSubview(self.activityLabel, aboveSubview: self.arrOfSubjectView[index])
                        //self.view.bringSubviewToFront(self.activityLabel)
                        self.activityLabel.isHidden = true
                    }
                    
                        
                    var activityies: [Activity] = []
                    
                    if let activities = self.subjectActivities?.filter({Int(frameIndex - 1) >= $0.startFrame && Int(frameIndex-1) <= $0.endFrame ?? 0}) {
                        for activity in activities {
                            if !activityies.contains(where: {$0.label == activity.label}) {
                                activityies.append(activity)
                            }
                        }
                    }
                    
                    if activityies.count > 0 {

                        self.activityLabel.backgroundColor = UIColor.appColor(.main)
                        self.activityLabel.text = self.getModifiedActivityText(activities: activityies)
                        self.activityLabel.frame.origin.x = self.arrOfSubjectView[index].frame.origin.x
                        self.activityLabel.frame.size.width = self.activityLabel.intrinsicContentSize.width + 10
                        self.activityLabel.frame.size.height = CGFloat(35 * activityies.count)
                        self.activityLabel.frame.origin.y = (self.arrOfSubjectView[index].frame.origin.y < self.safeAreaValueForVideo + CGFloat(35 * activityies.count)) ? self.arrOfSubjectView[index].frame.origin.y : (self.arrOfSubjectView[index].frame.origin.y - self.activityLabel.frame.size.height)
                        self.activityLabel.isHidden = false
                    } else {
                        self.activityLabel.isHidden = true
                        self.activityLabel.backgroundColor = UIColor.clear
                        self.activityLabel.frame.size.height = 0
                        self.activityLabel.text = ""
                    }
                }
            }
        }
    }
    
    private func getModifiedActivityText(activities: [Activity]) -> String {
        var text = ""
        for activity in activities {
            text = text == "" ? activity.label : text + "\n\n" + activity.label
        }
        return text
    }
    
    private func addObjectViews() {
        
        guard arrOfObject.count > 0 else {
            let view = ResizableView()
            arrOfSubjectView.append(view)
            return
        }
        
        for value in arrOfObject {
            let view = ResizableView()
            if value == arrOfObject.first { view.isDefaultObject = true }
            arrOfSubjectView.append(view)
        }
    }
    
    private func setupScrubber() {
        
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
        guard let previewView = PreviewScrubBarWithSubmit.instanceFromNib(), previewScrubBar == nil
            else { return }
        self.view.addSubview(previewView)
        previewView.delegate = self
        previewView.translatesAutoresizingMaskIntoConstraints = false
        
        var valScrubHt = 83 //73
        if previewFlow == .DemoVideo {
            valScrubHt = 116
            previewView.descriptionLbl.isHidden = false
            previewView.transeperantView.isHidden = false
            let arrOftrainingVideosOverlay = ProjectService.instance.currentCollection?.trainingVideosOverlay?
                            .replacingOccurrences(of: "[", with: "")
                            .replacingOccurrences(of: "]", with: "")
                            .components(separatedBy: ",") ?? []
            previewView.descriptionLbl.text = arrOftrainingVideosOverlay.count > 0 ? arrOftrainingVideosOverlay[ProjectService.instance.trainingVideoTextIndex] : LocalizableString.trainingVideoOverLayText.localizedString
        }
        else {
            previewView.descriptionLbl.isHidden = true
            previewView.transeperantView.isHidden = false
        }
        
        var leadTrailConstant: CGFloat = 0
        
        if VideoVariables.deviceOrientationRawValue < 3  {
            
        } else {
            if ScreenSize.SCREEN_HEIGHT == 896 {
                leadTrailConstant = UIDevice.current.hasNotch ? 72 + 8 : 0
            }
            else {
                leadTrailConstant = UIDevice.current.hasNotch ? 72 : 0
            }
        }
        
        previewView.transeperantView.backgroundColor = UIDevice.current.hasNotch
                                                        ? UIColor.clear
                                                        : #colorLiteral(red: 0, green: 0, blue: 0, alpha: 0.5)
        
        NSLayoutConstraint.activate([
            previewView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor, constant: -leadTrailConstant),
            previewView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor, constant: leadTrailConstant),
            previewView.bottomAnchor.constraint(equalTo: self.barView.bottomAnchor, constant: 0),
            previewView.heightAnchor.constraint(equalToConstant: CGFloat(valScrubHt)) //80
        ])
        //        previewView.bottomAnchor.constraint(equalTo: self., constant: 0).isActive = true
        
        guard let scrubView = VideoScrubBarView.instanceFromNib()
            else { return }
        previewView.barView.addSubview(scrubView)
        scrubView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            scrubView.trailingAnchor.constraint(equalTo: previewView.barView.trailingAnchor),
            scrubView.leadingAnchor.constraint(equalTo: previewView.barView.leadingAnchor),
            scrubView.topAnchor.constraint(equalTo: previewView.barView.topAnchor),
            scrubView.bottomAnchor.constraint(equalTo: previewView.barView.bottomAnchor)
        ])
        if previewFlow == .DemoVideo {
            previewView.buttonSubmit.setTitle("Next", for: .normal)
        }
        else if previewFlow == .MyVideos {
            previewView.buttonSubmit.setTitle("OK", for: .normal)
        }
        
        scrubView.delegate = self
        self.scrubBarView = scrubView
        self.scrubBarView?.playerController = player
    }
    
    private func setUpPreviewScrubber() {
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarView.self))}.first
        guard let previewView = PreviewScrubBarView.instanceFromNib(), previewScrubBar == nil
            else { return }
        self.view.addSubview(previewView)
        previewView.delegate = self
        previewView.viewRatingQuestions.isHidden = true
        previewView.textViewFeedbackText.text = ""
        previewView.buttonTumbsUp.isUserInteractionEnabled = false
        previewView.buttonTumbsDown.isUserInteractionEnabled = false
        previewView.buttonSubmit.isEnabled = false
        previewView.buttonSubmit.setTitleColor(.darkGray, for: .normal)
        previewView.translatesAutoresizingMaskIntoConstraints = false
        
        var leadTrailConstant: CGFloat = 0
        
        if VideoVariables.deviceOrientationRawValue < 3  {
            
        } else {
            if ScreenSize.SCREEN_HEIGHT == 896 {
                leadTrailConstant = UIDevice.current.hasNotch ? 72 + 8 : 0
            }
            else {
                leadTrailConstant = UIDevice.current.hasNotch ? 72 : 0
            }
        }
        
        previewView.backgroundColor = UIDevice.current.hasNotch
                                        ? #colorLiteral(red: 0, green: 0, blue: 0, alpha: 0.5) //UIColor.clear
                                        : #colorLiteral(red: 0, green: 0, blue: 0, alpha: 0.5)
        
        NSLayoutConstraint.activate([
            previewView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor, constant: -leadTrailConstant),
            previewView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor, constant: leadTrailConstant),
            previewView.bottomAnchor.constraint(equalTo: self.barView.bottomAnchor, constant: 0),
            //            previewView.heightAnchor.constraint(equalToConstant: 70)
        ])
        previewScrubHt =  previewView.heightAnchor.constraint(equalToConstant: 80)
        previewScrubHt?.isActive = true
        
        guard let scrubView = VideoScrubBarView.instanceFromNib()
            else { return }
        previewView.barView.addSubview(scrubView)
        scrubView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            scrubView.trailingAnchor.constraint(equalTo: previewView.barView.trailingAnchor),
            scrubView.leadingAnchor.constraint(equalTo: previewView.barView.leadingAnchor),
            scrubView.topAnchor.constraint(equalTo: previewView.barView.topAnchor),
            scrubView.bottomAnchor.constraint(equalTo: previewView.barView.bottomAnchor)
        ])
        self.scrubBarView = scrubView
        scrubView.delegate = self
        self.scrubBarView?.playerController = player
    }
    
    private func loadPreviewInfoDetails(jsonData: Data) {
        
        let decoder = JSONDecoder()
        do {
            let info = try decoder.decode(WelcomeAtDecode.self, from: jsonData)
            //subjectActivities = info.activitylabels
            //subjectCordinates = info.cordinates
            subjectActivities = info.activity
            subjectCordinates = info.object[0].boundingBox
            subjectEditedCordinates = info.object
            let projectId = ProjectService.instance.currentCollection?.programName ?? ""
            let projectName = ProjectService.instance.currentCollection?.projectName ?? ""
            fileName = LocalizableString.recordVideoS3FolderStructure.localizedString + projectId + "/" + projectName + "/" + info.metadata.videoID
        } catch {
            print(error.localizedDescription)
        }
    }
    
    private func loadVideoFromLocal() {
        videoURL = URL(fileURLWithPath: Utilities.getFilePathFromLocal(fileName: "PreviewVideo", type: "mov")!)
    }
    
    private func getSubjectFrame(rect: CGRect) -> CGRect {
        
        let subjectFrame = rect
        
        let height: CGFloat = CGFloat(self.player.naturalSize.height)
        let width: CGFloat = CGFloat(self.player.naturalSize.width)
        
        var screenWidth: CGFloat = 0.0
        var screenHeight: CGFloat = 0.0
        
        var safeAreaX: CGFloat = 0
        var safeAreaY: CGFloat = 0
        
        if VideoVariables.deviceOrientationRawValue < 3 {
            screenWidth = ScreenSize.SCREEN_WIDTH
            screenHeight = UIDevice.current.hasNotch ? ScreenSize.SCREEN_HEIGHT - screenHeightWithSafeArea : ScreenSize.SCREEN_HEIGHT
            safeAreaY = UIDevice.current.hasNotch ? safeAreaValueForVideo : 0
        } else {
            screenWidth = UIDevice.current.hasNotch ? ScreenSize.SCREEN_HEIGHT - screenHeightWithSafeArea : ScreenSize.SCREEN_HEIGHT
            screenHeight = ScreenSize.SCREEN_WIDTH
            safeAreaX = UIDevice.current.hasNotch ? safeAreaValueForVideo : 0
        }
        
        let aspectWidth = CGFloat(width) / screenWidth
        let aspectHeight = CGFloat(height) / screenHeight
        
        let newSubjectWidth = subjectFrame.width / aspectWidth
        let newSubjectHeight = subjectFrame.height / aspectHeight
        
        let newX = subjectFrame.origin.x / aspectWidth
        let newY = (subjectFrame.origin.y / aspectHeight)
        
        return CGRect(x: newX + safeAreaX, y: newY + safeAreaY, width: newSubjectWidth, height: newSubjectHeight)
    }
    
    private func presentRatingQuestion() {
        
        let now = Date()
        let formatter = DateFormatter()
        formatter.dateFormat = "MM/dd/yyyy, hh:mm:ss a"
        formatter.timeZone = TimeZone(abbreviation: "EST")

        let datetimeNow = formatter.string(from: now)
//
        if let ratingInstance = ratingQuestionInstances.first(where: {$0.frameValue == String(frameCount)}) {
            if ratingInstance.update {
                ratingQuestionResponse.id = ""
                updatedRatingQuestionResponse.id = ratingInstance.id
                updatedRatingQuestionResponse.reviewerId = Collector.currentCollector.userId
                updatedRatingQuestionResponse.ratingResponses = ratingInstance.questionShortName
                updatedRatingQuestionResponse.collectionId = ratingInstance.collectionId
                updatedRatingQuestionResponse.collectionName = ratingInstance.collectionName
                updatedRatingQuestionResponse.programId = ratingInstance.programId
                updatedRatingQuestionResponse.projectId = ratingInstance.projectId
                updatedRatingQuestionResponse.projectName = ratingInstance.projectName
                updatedRatingQuestionResponse.videoId = ratingInstance.videoId
                updatedRatingQuestionResponse.week = ratingInstance.week
                updatedRatingQuestionResponse.videoUploadedDate = ratingInstance.videoUploadedTime
                updatedRatingQuestionResponse.submittedTime = datetimeNow
            } else {
                updatedRatingQuestionResponse.id = ""
                ratingQuestionResponse.id = ratingInstance.id
                ratingQuestionResponse.reviewerId = Collector.currentCollector.userId
                ratingQuestionResponse.ratingResponses = ratingInstance.questionShortName
                ratingQuestionResponse.collectionId = ratingInstance.collectionId
                ratingQuestionResponse.collectionName = ratingInstance.collectionName
                ratingQuestionResponse.programId = ratingInstance.programId
                ratingQuestionResponse.projectId = ratingInstance.projectId
                ratingQuestionResponse.projectName = ratingInstance.projectName
                ratingQuestionResponse.videoId = ratingInstance.videoId
                ratingQuestionResponse.week = ratingInstance.week
                ratingQuestionResponse.videoUploadedDate = ratingInstance.videoUploadedTime
                ratingQuestionResponse.submittedTime = datetimeNow
            }
            
            self.player.pause()
            
            let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarView.self))}.first as? PreviewScrubBarView
            previewScrubBar?.textViewFeedbackText.text = ratingInstance.question
            let extraCharArr = ratingInstance.question.components(separatedBy: "\"")
            var textAfterAppendingExtraChar = ratingInstance.question

            for (index, _) in extraCharArr.enumerated() {
                if index != 0 {
                   textAfterAppendingExtraChar = textAfterAppendingExtraChar + "ii"
                }
            }
            let height = textAfterAppendingExtraChar.height(withConstrainedWidth: previewScrubBar?.textViewFeedbackText.frame.width ?? 0, font: (previewScrubBar?.textViewFeedbackText.font!)!)
            previewScrubBar?.buttonTumbsUp.isUserInteractionEnabled = true
            previewScrubBar?.buttonTumbsDown.isUserInteractionEnabled = true
            previewScrubBar?.buttonTumbsUp.setImage(#imageLiteral(resourceName: "unselect_thumb_up"), for: .normal)
            previewScrubBar?.buttonTumbsDown.setImage(#imageLiteral(resourceName: "unselect_thumb_down"), for: .normal)
            let finalHeight = 108 + (17*((height)/17))
            previewScrubHt?.constant = finalHeight > 200 ? 200 : finalHeight
            previewScrubBar?.viewRatingQuestions.isHidden = false
            previewScrubBar?.layoutIfNeeded()
        }
    }
    
    private func retakeCollection() {
        
        let arrNavigation = navigationController?.viewControllers
        var valCount = 0
        
        if arrNavigation?.count ?? 0 > 2 && previewFlow != .MyVideos && previewFlow != .Ratings {
            let arrControllerCheck = arrNavigation![arrNavigation!.count - 2]
            if arrControllerCheck.isKind(of: RecordVideoVC.self) {
                valCount = 1
            }
        }
        
        let value1 = UIInterfaceOrientation.landscapeLeft.rawValue //rotate first and then pop
        UIDevice.current.setValue(value1, forKey: "orientation")
        let value = UIInterfaceOrientation.portrait.rawValue
        UIDevice.current.setValue(value, forKey: "orientation")
        AppUtility.lockOrientation(.portrait)
        
        RecordVideoVC.operationJson?.cancel() //Blurred Video
        RecordVideoVC.valDidEnterBackground = false //Blurred Video
        
        if valCount == 1 {
            DispatchQueue.main.async {
                self.hideProgress()
                self.navigationController?.viewControllers.remove(at: arrNavigation!.count - 2)
                let recordVideoVC: RecordVideoVC = (self.storyboard?.instantiateViewController())!
                self.navigationController?.pushViewController(recordVideoVC, animated: true)
            }
        }
        else {
            DispatchQueue.main.async {
                self.hideProgress()
                self.navigationController?.popViewController(animated: true) //IMP
            }
        }
    }
    
    private func verifyVideoProcessingStatus() {
        
        if RecordVideoVC.valBlurActionStatus == "InProgress" {
            self.showProgress()
            self.view.isUserInteractionEnabled = false
            self.startTimer()
        } else if RecordVideoVC.valBlurActionStatus == "Completed" {
            if ProjectService.instance.isPracticeProject ?? false {
                self.practiceProject()
            }
            else {
                self.showProgress()
                RecordVideoVC.valBlurActionStatus = "NotStarted"
                self.view.isUserInteractionEnabled = false
                self.timerSubmitBlurr?.invalidate()
                
                if !self.buttonEdit.isEnabled {
                    self.hideProgress()
                    self.navigateToEditVideo()
                } else {
                    self.uploadJsonFile()
                }
            }
            
        } else if (RecordVideoVC.valBlurActionStatus == "Failed") || (RecordVideoVC.valBlurActionStatus == "NotStarted") {//alert
            
            let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
            if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
                previewScrubBar1.buttonSubmit.isEnabled = true
                previewScrubBar1.buttonSubmit.setTitleColor(.white, for: .normal)
            }
            self.uploadState = false
            
            self.hideProgress()
            RecordVideoVC.valBlurActionStatus = "NotStarted"
            self.timerSubmitBlurr?.invalidate()
            self.buttonEdit.isEnabled = true
            self.view.isUserInteractionEnabled = true
            DispatchQueue.main.async {
                UIUtilities.showAlertMessage(kRecaptureVideo, errorMessage: kErrorProcessingVideo, errorAlertActionTitle: "OK", viewControllerUsed: self)
            }
        }
        
    }
    
    private func navigateToEditVideo() {
        let collection = ProjectService.instance.currentCollection
        let authStoryBoard = UIStoryboard(name: "Activity", bundle: nil)
        let previewVC = authStoryBoard.instantiateViewController(withIdentifier: "EditVideoVC") as! EditVideoVC
        previewVC.editVideoFlow = .VideoCollection
        previewVC.fileUrl = self.fileUrl
        previewVC.videoURL = self.videoURL
        previewVC.arrDropDownObject = collection?.objectsList == "" ? [] : (collection?.objectsList?.components(separatedBy: ",") ?? [])
        previewVC.arrDropDownActivity = collection?.activityShortNames == "" ? [] : (collection?.activityShortNames?.components(separatedBy: ",") ?? [])
        previewVC.projectName = ProjectService.instance.currentCollection?.projectName ?? ""
        previewVC.programID = ProjectService.instance.currentCollection?.programName ?? ""
        previewVC.collectionDescription = ProjectService.instance.currentCollection?.collectionDescription ?? ""
        self.navigationController?.pushViewController(previewVC, animated: true)
    }
    
    // MARK:- Actions
    private func navigateToNextScreen() {
        self.hideProgress()
        switch previewFlow {
        case .ConsentVideo:
            
            if ConsentResponse.instance.isEditConsent {
                UIUtilities.showAlertMessageWithActionHandler(kUploadCompleted, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                    self.navigationController?.popToViewController(ofClass: EditConsentVC.self)
                }
            } else {
                
                if (ProjectService.instance.currentCollection?.isTrainingVideoEnabled ?? false) {
                    
                    self.view.isUserInteractionEnabled = true
                    let previewVC: ConsentVideoPreviewVC = (self.storyboard?.instantiateViewController())!
                    previewVC.fileUrl = ""
                    previewVC.videoURL = nil
                    if ProjectService.instance.trainingVideoUrl != "" {
                        
                        let videoURL = URL(fileURLWithPath: ProjectService.instance.trainingVideoUrl)
                        let asset = AVAsset(url: videoURL)
                        let videoTrack = asset.tracks(withMediaType: AVMediaType.video)[0]
                        
                        if videoTrack.naturalSize.width > videoTrack.naturalSize.height {
                            VideoVariables.deviceOrientationRawValue = Utilities.getOrientationInInt(value: "landscapeLeft")
                        } else {
                            VideoVariables.deviceOrientationRawValue = Utilities.getOrientationInInt(value: "portrait")
                        }
                        
                        previewVC.videoURL = videoURL
                        
                    }
                    
                    previewVC.previewFlow = .DemoVideo
                    previewVC.serverTraining = true
                    self.navigationController?.pushViewController(previewVC, animated: true)
                    
                } else {
                    // navigate to activity record
                    let recordVideoVC: RecordVideoVC = (self.storyboard?.instantiateViewController())!
                    // update the navigation root view controller to record video vc
                    // remove all the other vc from memory and re instantiate the tab bar vc
                    // update the navgation bar
                    self.navigationController?.pushViewController(recordVideoVC, animated: true)
                }
            }
        case .RecordVideo:
            self.navigationController?.popToRootViewController(animated: true)
        case .DemoVideo:
            
            // navigate to activity record
            let recordVideoVC: RecordVideoVC = (self.storyboard?.instantiateViewController())!
            // update the navigation root view controller to record video vc
            // remove all the other vc from memory and re instantiate the tab bar vc
            // update the navgation bar
            recordVideoVC.clearPreviewFromNavigation = false
            self.navigationController?.pushViewController(recordVideoVC, animated: true)
        case .MyVideos, .Ratings:
            self.navigationController?.isNavigationBarHidden = false
            self.navigationController?.popToRootViewController(animated: true)
            break
        }
    }
    
    @IBAction func closeBtnAction(_ sender: UIButton) {
        
        switch previewFlow {
        case .DemoVideo:
            self.hideProgress()
            self.navigationController?.popToViewController(ofClass: ConsentEmailVC.self)
        case .MyVideos, .Ratings:
            UserDefaults.standard.set(false, forKey: UserDefaults.Keys.deleteRatingVideo.rawValue)
            self.hideProgress()
            self.navigationController?.isNavigationBarHidden = false
            self.navigationController?.popViewController(animated: true)
        case .ConsentVideo:
            if ConsentResponse.instance.isEditConsent {
                UIUtilities.showAlertMessageWithTwoActionsAndHandler("Close", errorMessage: kExitConsent, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
                    
                    self.deinitialisingTasks()
                    self.navigationController?.popToViewController(ofClass: EditConsentVC.self)
                }) {
                }
            } else {
                UIUtilities.showAlertMessageWithTwoActionsAndHandler("Close", errorMessage: kExitCollection, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
                    self.hideProgress()
                    self.navigationController?.popToRootViewController(animated: true)
                }) {
                    
                }
            }
        default:
            UIUtilities.showAlertMessageWithTwoActionsAndHandler("Close", errorMessage: kExitCollection, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
                self.hideProgress()
                self.navigationController?.popToRootViewController(animated: true)
            }) {
                
            }
        }
    }
    
    @IBAction func editButtonAction(_ sender: UIButton) {
        self.buttonEdit.isEnabled = false
        self.player.pause()
        self.didTapOnSubmit()
    }

    // <JEBYRNE: add info (?) button during rating questions>
    @IBAction func infoBtnAction(_ sender: UIButton) {
        if collectionDescription != "" {
            self.player.pause()
            setUpCustomView()
        }
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
    // </JEBYRNE>

    // Custom Pop Up to Present Collection Description
    private func setUpCustomView() {
        guard let popUp = CustomPopUp.instanceFromNib()
            else { return }
        popUp.delegate = self
        customPopUp = popUp
        popUp.viewBackground.cornerRadius = 10
        popUp.alpha = 0
        
        //popUp.titleTextView.text = collectionDescription
        
        let str1 = "<span style=\"font-family: '-apple-system'; font-size:15px;font-style:Medium;\">\(collectionDescription)</span>"
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
    
    
    
    @IBAction func recordBtnPressed(_ sender: UIButton) {
        if previewFlow == .DemoVideo {
            UIUtilities.showAlertMessageWithTwoActionsAndHandler("Close", errorMessage: kExitCollection, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
                
                self.deinitialisingTasks()
                self.navigationController?.popToRootViewController(animated: true)
            }) {
            }
        } else {
            
            var valErrorMessage = kDiscardDraftedVideo
            if previewFlow == .ConsentVideo || ConsentResponse.instance.isEditConsent {
                valErrorMessage = kDiscardDraftedConsentVideoAtSetUp
            }
                        
            UIUtilities.showAlertMessageWithTwoActionsAndHandler("Alert", errorMessage: valErrorMessage, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
                
                self.retakeCollection()
            }) {
                
            }
        }
    }
    
    //MARK: S3 bucket Calls
    
    private func uploadJsonFile() {
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
        if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
            previewScrubBar1.buttonSubmit.isEnabled = false
            previewScrubBar1.buttonSubmit.setTitleColor(.darkGray, for: .normal)
        }
        //        self.showProgress()
        uploadState = true
        AWSS3Manager.shared.uploadOtherFile(fileUrl: URL(string: fileUrl)!, fileName: fileName, conentType: "json", progress: nil) { [weak self] (uploadedFileUrl, error) in
            guard let self = self else { return }
            if let finalPath = uploadedFileUrl as? String {
                print("Uploaded file url: " + finalPath)
                
                self.uploadVideo()
            } else {
                self.hideProgress()
                self.uploadState = false
                print("\(String(describing: error?.localizedDescription))")
                let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
                if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
                    previewScrubBar1.buttonSubmit.isEnabled = true
                    previewScrubBar1.buttonSubmit.setTitleColor(.white, for: .normal)
                }
            }
        }
    }
    
    private func uploadVideo() {
        
        if previewFlow == .ConsentVideo {// Helpful while checking
            print("videoURL---\(String(describing: videoURL))")
        } else {
            videoURL = URL(fileURLWithPath: Utilities.getFileUrlStringFromCache(fileName: "videoBlurred.mp4"))
        }
        
        AWSS3Manager.shared.uploadVideo(videoUrl: videoURL, fileName: fileName, progress: nil) { [weak self] (uploadedFileUrl, error) in
            guard let self = self else { return }
            
            self.uploadState = false
            
            let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
            if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
                previewScrubBar1.buttonSubmit.isEnabled = true
                previewScrubBar1.buttonSubmit.setTitleColor(.white, for: .normal)
            }
            if let finalPath = uploadedFileUrl as? String {
                print("Uploaded file url: " + finalPath)
                
                if self.previewFlow == .RecordVideo {
                    self.hideProgress()
                    UIUtilities.showAlertMessageWithActionHandler(kUploadCompleted, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                        self.navigateToNextScreen()
                    }
                } else if self.previewFlow == .ConsentVideo {
                    
                    if ConsentResponse.instance.isEditConsent {
                        self.navigateToNextScreen()
                    } else {
                        self.requestForUpdateSubjectInfo()
                    }
                }
                
            } else {
                self.hideProgress()
                let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
                if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
                    previewScrubBar1.buttonSubmit.isEnabled = true
                    previewScrubBar1.buttonSubmit.setTitleColor(.white, for: .normal)
                }
                
                UIUtilities.showAlertMessage(kUploadFailed, errorMessage: kReSubmit, errorAlertActionTitle: "Ok", viewControllerUsed: self)
                
                print("\(String(describing: error?.localizedDescription))")
            }
        }
    }
    
}

// <JEBYRNE>
//MARK: CustomPopUP Delegate Methods
extension ConsentVideoPreviewVC: CustomPopUpDelegate {
    func didTapOnOkBtn() {
        buttonInfo.setImage(#imageLiteral(resourceName: "question_white"), for: .normal)
        buttonInfo.backgroundColor = #colorLiteral(red: 0, green: 0, blue: 0, alpha: 0.66)
        self.removeCustomView()
    }
}


// MARK: - UIGestureRecognizer

extension ConsentVideoPreviewVC {
    
    @objc func handleTapGestureRecognizer(_ gestureRecognizer: UITapGestureRecognizer) {
        
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarView.self))}.first as? PreviewScrubBarView
        previewScrubHt?.constant = 80
        previewScrubBar?.viewRatingQuestions.isHidden = true
        previewScrubBar?.layoutIfNeeded()
        
        switch (self.player.playbackState.rawValue) {
        case PlaybackState.stopped.rawValue:
            self.player.playFromBeginning()
            break
        case PlaybackState.paused.rawValue:
            frameCount = frameCount == 0 ? frameCount : frameCount + 1
            self.player.playFromCurrentTime()
            break
        case PlaybackState.playing.rawValue:
            self.player.pause()
            break
        case PlaybackState.failed.rawValue:
            self.player.pause()
            break
        default:
            self.player.pause()
            break
        }
    }
    
}

// MARK: - PlayerDelegate

extension ConsentVideoPreviewVC: PlayerDelegate {
    
    func playerReady(_ player: PlayerController) {
        print("\(#function) ready")
    }
    
    func playerPlaybackStateDidChange(_ player: PlayerController) {
        print("\(#function) \(player.playbackState.description)")
        switch player.playbackState {
            case .stopped:
                resetObjectAndActivityLabel()
            default:
                break
        }
    }
    
    func playerBufferingStateDidChange(_ player: PlayerController) {
    }
    
    func playerBufferTimeDidChange(_ bufferTime: Double) {
    }
    
    func player(_ player: PlayerController, didFailWithError error: Error?) {
        print("\(#function) error.description")
    }
    
}

// MARK: - PlayerPlaybackDelegate

extension ConsentVideoPreviewVC: PlayerPlaybackDelegate {
    
    func playerCurrentTimeDidChange(_ player: PlayerController) {
        scrubBarView?.timerObserver(time: player.currentTime)
       
        frameCount += 1
        
        switch previewFlow {
        case .ConsentVideo, .DemoVideo, .MyVideos:
            break
        case .RecordVideo:
            updateObjectsOnFrameChange(frameIndex: frameCount, objects: subjectEditedCordinates, animationValue: 0.2)
        case .Ratings:
            updateObjectsOnFrameChange(frameIndex: frameCount, objects: subjectEditedCordinates, animationValue: 0.2)
            presentRatingQuestion()
        }
        
    }
    
    func playerPlaybackWillStartFromBeginning(_ player: PlayerController) {
        frameCount = 0
    }
    
    func playerPlaybackDidEnd(_ player: PlayerController) {
        self.player.movedToBeginning()
        frameCount = 0
        scrubBarView?.timerObserver(time: .zero)
    }
    
    func playerPlaybackWillLoop(_ player: PlayerController) {
    }
    
    func playerPlaybackDidLoop(_ player: PlayerController) {
    }
}

extension ConsentVideoPreviewVC: DemoScrubBarDelegate {
    
    func didTapOnSubmitButton() {
        
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarView.self))}.first
        if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarView {
            previewScrubBar1.buttonSubmit.isEnabled = false
            previewScrubBar1.buttonSubmit.setTitleColor(.darkGray, for: .normal)
        }
        requestForDeleteRatingVideo()
    }
    
    func didTapOnTumbsUpOrDown(value: Int) {
        
        switch previewFlow {
            
        case .Ratings:
            //            self.dismiss(animated: true, completion: nil)
            
            let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarView.self))}.first as? PreviewScrubBarView
            previewScrubBar?.buttonTumbsUp.isUserInteractionEnabled = false
            previewScrubBar?.buttonTumbsDown.isUserInteractionEnabled = false
            previewScrubBar?.buttonSubmit.isEnabled = true
            previewScrubBar?.buttonSubmit.setTitleColor(.white, for: .normal)
            if value == 1 {
                ratingQuestionResponse.ratingResponses = "good"
                updatedRatingQuestionResponse.ratingResponses = "good"
                
                
                previewScrubBar?.buttonTumbsUp.setImage(#imageLiteral(resourceName: "thumb_up"), for: .normal)
            } else {
                ratingQuestionResponse.ratingResponses = "bad" +  (ratingQuestionResponse.ratingResponses ?? "")
                updatedRatingQuestionResponse.ratingResponses = "bad" +  (updatedRatingQuestionResponse.ratingResponses ?? "")
                previewScrubBar?.buttonTumbsDown.setImage(#imageLiteral(resourceName: "thumb_down"), for: .normal)
            }
            
             DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
               // your code here
                self.previewScrubHt?.constant = 80
                previewScrubBar?.viewRatingQuestions.isHidden = true
                previewScrubBar?.layoutIfNeeded()
                self.frameCount += 1
                self.player.playFromCurrentTime()
                
                if self.ratingQuestionResponse.id == "" {
                    print("requestForUpdateRatingQuestionResponse: ",  self.updatedRatingQuestionResponse)
                    self.requestForUpdateRatingQuestionResponse()
                } else {
                    print("ratingQuestionResponse: ",   self.ratingQuestionResponse)
                    self.requestForRatingQuestionResponse()
                }
            }
            
            
        default: break
        }
    }
}

extension ConsentVideoPreviewVC: ScrubBarDelegate {
    
    func didTapOnSubmit() {
                
        if !(self.previewFlow == .DemoVideo || self.previewFlow == .MyVideos || self.previewFlow == .Ratings)  {
            let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
            if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
                previewScrubBar1.buttonSubmit.isEnabled = false
                previewScrubBar1.buttonSubmit.setTitleColor(.darkGray, for: .normal)
            }
            self.uploadState = true
        }
        
        switch self.previewFlow {
        case .RecordVideo:
            
            if !self.buttonEdit.isEnabled {
                self.verifyVideoProcessingStatus()
            } else if (subjectActivities != nil && subjectActivities?.count ?? 0 > 0) || ProjectService.instance.currentCollection?.activityShortNames?.count ?? 0 == 0 {
                
                self.verifyVideoProcessingStatus()
                
            } else {
            
                UIUtilities.showAlertMessageWithTwoActionsAndHandler("Alert", errorMessage: kNoActivities, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
                    
                    self.verifyVideoProcessingStatus()
                }) {
                    self.uploadState = false
                    self.retakeCollection()
                }
            }
            
        case .ConsentVideo:
            self.showProgress()
            self.uploadVideo() //TBD
            
        case .DemoVideo:
            self.navigateToNextScreen()
        case.MyVideos, .Ratings: //NotRequired Ratings
            self.navigateToNextScreen()
            break
            
        }
        
    }
}

extension ConsentVideoPreviewVC:EditScrubBarDelegate {
    
    func didDragScrub(value: Float, ended: Bool) {
        
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarView.self))}.first as? PreviewScrubBarView
        previewScrubHt?.constant = 80
        previewScrubBar?.viewRatingQuestions.isHidden = true
        previewScrubBar?.layoutIfNeeded()
        
        let time =  CMTimeMake(value: Int64(value), timescale: 1)
        let valRatio = player.numberOfFrames / Int(player.maximumDuration)
        frameCount = Int(value * Float(valRatio))
        if ended {
            self.player.playFromDragTime(time: time)
        }
        else if frameCount > 0 {
            updateObjectsOnFrameChange(frameIndex: frameCount, objects: subjectEditedCordinates, animationValue: 0.2)
        }
    }
}

//MARK: API Call
extension ConsentVideoPreviewVC {
    
    private func requestForUpdateSubjectInfo() {
        
        ConsentAPI.updateSubject() { [weak self] (status, error) in
            guard let self = self else { return }
            if status {
                //self.updateConsentResponse()
                self.hideProgress()
                ConsentEmailVC.addRecentSubjectToLocalCache()
//                if (ConsentResponse.instance.subjectEmail == Collector.currentCollector.email) {
//                    self.requestForUpdateProfie()
//                }
                
                UIUtilities.showAlertMessageWithActionHandler(kUploadCompleted, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                    self.navigateToNextScreen()
                }
                
            } else if let err = error {
                self.hideProgress()
                self.presentDefaultAlertWithError(error: err, animated: true, completion: nil)
            }
        }
    }
    
    private func requestForUpdateProfie() {
        
        let data = UserSignUpData()
        data.firstName = Collector.currentCollector.firstName
        data.lastName = Collector.currentCollector.lastName
        
        Collector.currentCollector.consentSetUp = true
        
        UserAPI.updateProfileToDDB(params: data) { (_, _) in
            
        }
    }
    
    private func requestForRatingQuestionResponse() {
        
        RatingAPI.createResponseForRatingQuestions(ratingInput: ratingQuestionResponse) { [weak self] (status, error) in
            guard let self = self else { return }
            if status {
                if let index = self.ratingQuestionInstances.firstIndex(where: {$0.id == self.ratingQuestionResponse.id}) {
                    self.ratingQuestionInstances[index].update = true
                }
                
            } else {
                
            }
        }
    }
    
    private func requestForUpdateRatingQuestionResponse() {
        
        RatingAPI.updateResponseForRatingQuestions(ratingInput: updatedRatingQuestionResponse) { (status, error) in
            if status {
                
            } else {
                
            }
        }
    }
    
    private func requestForDeleteRatingVideo() {
        self.showProgress()
        let videoID = fileName.components(separatedBy: "/").last?.components(separatedBy: ".").first
        RatingAPI.deleteRatingVideo(videoId: videoID ?? "") {[weak self] (status, error) in
            guard let self = self else { return }
            if status {
                UserDefaults.standard.set(true, forKey: UserDefaults.Keys.deleteRatingVideo.rawValue)
            } else {
                UserDefaults.standard.set(false, forKey: UserDefaults.Keys.deleteRatingVideo.rawValue)
            }
            
            self.hideProgress()
            self.navigationController?.isNavigationBarHidden = false
            self.navigationController?.popViewController(animated: true)
        }
    }
    
    // MARK: API Related - Private Methods
    private func startTimer() { //For Blur Video Confirmation
        timerSubmitBlurr = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true, block: { (timer) in
            print("RecordVideoVC.valBlurActionStatus---\(RecordVideoVC.valBlurActionStatus)---\(ProjectService.instance.isPracticeProject ?? false)")
            if RecordVideoVC.valBlurActionStatus == "Completed" {
                timer.invalidate()
                RecordVideoVC.valBlurActionStatus = "NotStarted"
                if ProjectService.instance.isPracticeProject ?? false {
                    self.practiceProject()
                }
                else {
                    if !self.buttonEdit.isEnabled {
                        self.hideProgress()
                        self.navigateToEditVideo()
                    } else {
                        self.view.isUserInteractionEnabled = false
                        self.uploadJsonFile()
                    }
                }
            }
            else if (RecordVideoVC.valBlurActionStatus == "Failed") || (RecordVideoVC.valBlurActionStatus == "NotStarted") {//alert
                self.hideProgress()
                
                let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
                if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
                    previewScrubBar1.buttonSubmit.isEnabled = true
                    previewScrubBar1.buttonSubmit.setTitleColor(.white, for: .normal)
                }
                
                RecordVideoVC.valBlurActionStatus = "NotStarted"
                timer.invalidate()
                self.view.isUserInteractionEnabled = true
                DispatchQueue.main.async {
                    UIUtilities.showAlertMessage(kRecaptureVideo, errorMessage: kErrorProcessingVideo, errorAlertActionTitle: "OK", viewControllerUsed: self)
                }
            }
            
        })
    }
    
    func practiceProject() { //Practice project
        
        RecordVideoVC.saveVideo(withUrl: URL(fileURLWithPath: Utilities.getFileUrlStringFromCache(fileName: "videoBlurred.mp4"))) { (status, url) in
            if status {
                print("status---\(status)--")
                DispatchQueue.main.async {
                    self.view.isUserInteractionEnabled = true
                    self.hideProgress()
                    self.navigationController?.popToRootViewController(animated: true)
                }
            }
            else {
                print("status---\(status)--")
                //                            RecordVideoVC.valBlurActionStatus = "Completed"
                DispatchQueue.main.async {
                    self.view.isUserInteractionEnabled = true
                    self.hideProgress()
                    
                    UIUtilities.showAlertMessageWithActionHandler(kExportFailed, message: kExportFailedMessage, buttonTitle: "OK", viewControllerUsed: self) {
                        self.navigationController?.popToRootViewController(animated: true)
                    }
                }
            }
        }
    }
    
}

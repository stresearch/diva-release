//
//  EditVideoVC.swift
//  STR
//
//  Created by govind Pasad C on 11/6/20.
//
//

import UIKit
import AVFoundation
import AWSAppSync

enum EditVideoFlow {
    case VideoCollection
    case MyVideoPreview
    case MyVideoEdit
}

class EditVideoVC: UIViewController {
    
    @IBOutlet weak var barView: UIView!
    @IBOutlet weak var buttonClose: UIButton!
    @IBOutlet weak var buttonUndo: UIButton!
    @IBOutlet weak var buttonEditActivity: UIButton!
    @IBOutlet weak var buttonEditObject: UIButton!
    @IBOutlet weak var buttonEdit: UIButton!
    @IBOutlet weak var buttonSave: UIButton!
    @IBOutlet weak var buttonZoomIn: UIButton!
    @IBOutlet weak var buttonZoomOut: UIButton!
    @IBOutlet weak var buttonInfo: UIButton!
    @IBOutlet weak var backgroundViewZoom: ShadowView!
    
    @IBOutlet weak var textFieldEditActivity: customDropDown!
    @IBOutlet weak var textFieldEditObject: customDropDown!
    
    @IBOutlet weak var blackViewTop: UIView!
    @IBOutlet weak var blackViewBottom: UIView!
    @IBOutlet weak var blackViewTopHeight: NSLayoutConstraint!
    @IBOutlet weak var blackViewBottomHeight: NSLayoutConstraint!
    
    @IBOutlet weak var blackViewLeft: UIView!
    @IBOutlet weak var blackViewRight: UIView!
    @IBOutlet weak var blackViewLeftWidth: NSLayoutConstraint!
    @IBOutlet weak var blackViewRightWidth: NSLayoutConstraint!
    
    @IBOutlet weak var buttonCloseConstraintTop: NSLayoutConstraint!
    @IBOutlet weak var buttonUndoConstraintTrailing: NSLayoutConstraint!
    @IBOutlet weak var buttonCloseConstraintLeading: NSLayoutConstraint!
    
    var scrubBarView: VideoColoredScrubBarView?
    
    var videoURL: URL!
    var fileUrl: String!
    fileprivate lazy var player = PlayerController()
    
    var activityLabel = UILabel()
    var subjectCordinates: [Object]? //[Cordinate]?
    var subjectActivities: [Activity]? //[Activitylabel]?
    var subjectMetadata: Metadata?
    var frameCount = 0
    var fileName = ""
    
    var uploadState: Bool = false // False - Not started or Completed
    
    var timerSubmitBlurr: Timer?
    var previewScrubHt : NSLayoutConstraint?
    
    var subjectEditedCordinates: [Object] = []
    var subjectRedoEditedCordinates: [Object] = []
    var editSubjectCordinates: [BoundingBox] = []
    var subjectCurrentFrame: CGRect?
    var editObjectOn = false
    var isObjectTracking = false
    var arrDropDownObject: [String] = [] {
        didSet {
            addObjectViews()
        }
    }
    var selectedDDObject: String?
    var arrOfSubjectView: [ResizableView] = []
    
    private var objectExistingFrames: [Activity] = []
    private var objectMissingFrames: [Activity] = []
    private var editObjectMissingFrames: [Activity] = []
    private var editObjectExistingFrames: [Activity] = []
    private var editDefaultObjectStartFrame: Int?
    private var editDefaultObjectEndFrame: Int?
    private var isFirstObjectRecorded = true
    
    fileprivate var zoomScale = CGFloat(1.0)
    
    var selectedDropDownIndex = 0
    var editActivityOn: Bool = false
    var subjectUnEditedActivities: [Activity]?
    var subjectRedoActivities: [Activity]?
    var tapGestureRecognizer: UITapGestureRecognizer?
    
    var selectedDDActivity: String? // Selected Drop Down Activity
    var scrubX = Int()
    
    var arrDropDownActivity: [String] = []
    var submitSaveToggle = true
    var okButtonToggle = false
    var activityEditingInProgress = false
    var isFirstActivityRecorded = true
    
    var potraitSafeAreaValueForVideo: CGFloat = 0
    var screenHeightWithSafeArea: CGFloat = 0
    var landscapeSafeAreaValueForVideo: CGFloat = 0
    var screenWidthWithSafeArea: CGFloat = 0
    
    var programID = ""
    var projectName = ""
    var collectionDescription = ""
    
    private var customPopUp: CustomPopUp?
    
    var editVideoFlow: EditVideoFlow = .MyVideoEdit
    
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
        buttonEdit.isHidden = true
        MenuController.panGestureRecognizer?.isEnabled = false
        enableDisableRetake(enable: false)
        
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
        
        NotificationCenter.default.addObserver(self, selector: #selector(willEnterForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(didEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        UIView.setAnimationsEnabled(false)
        
        if editVideoFlow != .VideoCollection {
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
        
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        selectedDDActivity = nil
        selectedDDObject = nil
        
        setUpView()
        self.player.fillMode = .resizeAspect//.resizeAspect
        
        self.view.isMultipleTouchEnabled = true
        self.player.view.isMultipleTouchEnabled = true
        
        textFieldEditActivity.optionArray = arrDropDownActivity
        textFieldEditActivity.isUserInteractionEnabled = false
        textFieldEditActivity.onAButton = true
        textFieldEditActivity.tag = 1
        deactivateActivity()
        
        textFieldEditObject.tag = 2
        textFieldEditObject.isUserInteractionEnabled = false
        textFieldEditObject.optionArray = arrDropDownObject
        textFieldEditObject.onAButton = true
        deactivateObject()
        
        buttonEditActivity.isEnabled = arrDropDownActivity.count > 0
        buttonEditObject.isEnabled = arrDropDownObject.count > 0
        
        let imageVal = Utilities.getImageWithColor(color: .white, size: CGSize(width: 24, height: 24), rounded: true)
        scrubBarView?.slider.setThumbImage(imageVal, for: .normal)
        
        textFieldEditActivity.dropDownWidth = reccurForWidth(arr: textFieldEditActivity.optionArray)
        textFieldEditObject.dropDownWidth = reccurForWidth(arr: textFieldEditObject.optionArray)
        
        trimActivitiesOnObjectEditor()
        
        if UIDevice.current.hasNotch {
            blackViewTop.isHidden = false
            blackViewBottom.isHidden = false
            blackViewLeft.isHidden = false
            blackViewRight.isHidden = false
        }
        else {
            blackViewTop.isHidden = true
            blackViewBottom.isHidden = true
            blackViewLeft.isHidden = true
            blackViewRight.isHidden = true
        }
        
        buttonZoomOut.isEnabled = zoomScale == 1.0 ? false : true
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        self.setTopBottomBlackSpaceValue()
        self.player.playFromBeginning()
        
        scrubX = Int(scrubBarView?.slider.frame.origin.x ?? 0)
        
        if editVideoFlow != .MyVideoPreview {
            coloredProgress()
        }
        
        resetObjectAndActivityLabel(index: 1)
    }
    
    // MARK: Private Methods
    
    @objc func willEnterForeground() {
        
        if uploadState {
            self.showProgress()
        }
        else {
            self.hideProgress()
        }
    }
    
    @objc func didEnterBackground() {
    }
    
    private func setUpView() {
        self.navigationController?.navigationBar.isHidden = true
        
        switch editVideoFlow {
        case .VideoCollection:
            if let jsonData = Utilities.getDataFromDirectory(fileurl: URL(string: fileUrl)!) {
                loadPreviewInfoDetails(jsonData: jsonData)
            }
        default:
            if let jsonData = Utilities.getDataFromCacheDirectoryByName(fileurl: URL(string: fileUrl)!, filename: fileName) {
                loadPreviewInfoDetails(jsonData: jsonData)
                let currentUTCDate = DateHelper.dateUTCCurrentDate(dateformateString: "yyyy-MM-dd'T'HH:mm:ssZ")
                fileName = fileName + "_" + currentUTCDate
            }
        }
        setupScrubber()
        enableDisableSubmit(enable: false)
        nameChangeSubmitSave(save: true, text: nil)
        enableDisableButtonElements(enable: true)
        addPlayer()
    }
    
    private func addPlayer() {
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
        tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(handleTapGestureRecognizer(_:)))
        tapGestureRecognizer?.numberOfTapsRequired = 1
        tapGestureRecognizer?.delegate = self
        self.view.addGestureRecognizer(tapGestureRecognizer!)
        
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
                        if self.arrOfSubjectView[index].isDefaultObject {
                            self.activityLabel.isHidden = true
                            self.activityLabel.backgroundColor = UIColor.clear
                            self.activityLabel.frame.size.height = 0
                            self.activityLabel.text = ""
                        }
                    }
                } else {
                    self.arrOfSubjectView[index].isHidden = true
                    if self.arrOfSubjectView[index].isDefaultObject {
                        self.activityLabel.isHidden = true
                        self.activityLabel.backgroundColor = UIColor.clear
                        self.activityLabel.frame.size.height = 0
                        self.activityLabel.text = ""
                    }
                }
                
                if (value.label == self.arrDropDownObject.first) && !self.arrOfSubjectView[index].isHidden {
                    
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
                    
                    if self.editObjectOn {
                        self.activityLabel.backgroundColor = UIColor.appColor(.main)
                        self.activityLabel.text = self.selectedDDObject
                        self.activityLabel.frame.origin.x = self.arrOfSubjectView[index].frame.origin.x
                        self.activityLabel.frame.size.width = self.activityLabel.intrinsicContentSize.width + 10
                        self.activityLabel.frame.size.height = 35
                        self.activityLabel.frame.origin.y = self.arrOfSubjectView[index].frame.origin.y < self.potraitSafeAreaValueForVideo + 35 ? self.arrOfSubjectView[index].frame.origin.y : (self.arrOfSubjectView[index].frame.origin.y - self.activityLabel.frame.size.height)
                        self.activityLabel.isHidden = false
                    } else {
                        
                        var activityies: [Activity] = []
                        
                        if let activities = self.subjectActivities?.filter({Int(frameIndex - 1) >= $0.startFrame && Int(frameIndex-1) <= $0.endFrame ?? 0}) {
                            for activity in activities {
                                if !activityies.contains(where: {$0.label == activity.label}) {
                                    activityies.append(activity)
                                }
                            }
                        }
                        
                        if self.activityEditingInProgress {
                            
                            if !activityies.contains(where: {$0.label == self.selectedDDActivity ?? ""}) {
                                activityies.append(Activity(activityName: self.selectedDDActivity ?? "", sTime: 0, activityIndex: 0))
                            }
                        }
                        
                        if activityies.count > 0 {

                            self.activityLabel.backgroundColor = UIColor.appColor(.main)
                            self.activityLabel.text = self.getModifiedActivityText(activities: activityies)
                            self.activityLabel.frame.origin.x = self.arrOfSubjectView[index].frame.origin.x
                            self.activityLabel.frame.size.width = self.activityLabel.intrinsicContentSize.width + 10
                            self.activityLabel.frame.size.height = CGFloat(35 * activityies.count)
                            self.activityLabel.frame.origin.y = (self.arrOfSubjectView[index].frame.origin.y < self.potraitSafeAreaValueForVideo + CGFloat(35 * activityies.count)) ? self.arrOfSubjectView[index].frame.origin.y : (self.arrOfSubjectView[index].frame.origin.y - self.activityLabel.frame.size.height)
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
    }
    
    private func setupScrubber() {
        
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
        guard let previewView = PreviewScrubBarWithSubmit.instanceFromNib(), previewScrubBar == nil
            else { return }
        self.view.addSubview(previewView)
        previewView.delegate = self
        previewView.translatesAutoresizingMaskIntoConstraints = false
        
        let valScrubHt = 83
        
        previewView.descriptionLbl.isHidden = true
        previewView.transeperantView.isHidden = false
        
        previewView.transeperantView.backgroundColor = UIDevice.current.hasNotch
                                                        ? UIColor.clear
                                                        : #colorLiteral(red: 0, green: 0, blue: 0, alpha: 0.5)
        
        var leadTrailConstant: CGFloat = 0
        
        if VideoVariables.deviceOrientationRawValue < 3  {
            leadTrailConstant = potraitSafeAreaValueForVideo
        } else {
            leadTrailConstant = landscapeSafeAreaValueForVideo
        }
        
        NSLayoutConstraint.activate([
            previewView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor, constant: 0),
            previewView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor, constant: 0),
            previewView.bottomAnchor.constraint(equalTo: self.barView.bottomAnchor, constant: 0),
            previewView.heightAnchor.constraint(equalToConstant: CGFloat(valScrubHt)) //80
        ])
        //        previewView.bottomAnchor.constraint(equalTo: self., constant: 0).isActive = true
        
        guard let scrubView = VideoColoredScrubBarView.instanceFromNib()
            else { return }
        previewView.barView.addSubview(scrubView)
        scrubView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            scrubView.trailingAnchor.constraint(equalTo: previewView.barView.trailingAnchor),
            scrubView.leadingAnchor.constraint(equalTo: previewView.barView.leadingAnchor),
            scrubView.topAnchor.constraint(equalTo: previewView.barView.topAnchor),
            scrubView.bottomAnchor.constraint(equalTo: previewView.barView.bottomAnchor)
        ])
        scrubView.delegate = self
        self.scrubBarView = scrubView
        self.scrubBarView?.playerController = player
    }
    
    private func loadPreviewInfoDetails(jsonData: Data) {
        let decoder = JSONDecoder()
        do {
            let info = try decoder.decode(WelcomeAtEncode.self, from: jsonData)
            
            subjectMetadata = info.metadata
            subjectActivities = info.activity
            subjectUnEditedActivities = info.activity /// For Reset snd backUp
            subjectRedoActivities = info.activity
            subjectCordinates = info.object
            subjectEditedCordinates = info.object
            subjectRedoEditedCordinates = info.object
            fileName = LocalizableString.recordVideoS3FolderStructure.localizedString + programID + "/" + projectName + "/" + info.metadata.videoID
            
        } catch {
            print(error.localizedDescription)
        }
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
            safeAreaY = UIDevice.current.hasNotch ? potraitSafeAreaValueForVideo : 0
        } else {
            screenWidth = UIDevice.current.hasNotch ? ScreenSize.SCREEN_HEIGHT - screenWidthWithSafeArea : ScreenSize.SCREEN_HEIGHT
            screenHeight = ScreenSize.SCREEN_WIDTH
            safeAreaX = UIDevice.current.hasNotch ? landscapeSafeAreaValueForVideo : 0
        }
        
        let aspectWidth = CGFloat(width) / screenWidth
        let aspectHeight = CGFloat(height) / screenHeight
        
        let newSubjectWidth = subjectFrame.width / aspectWidth
        let newSubjectHeight = subjectFrame.height / aspectHeight
        
        let newX = subjectFrame.origin.x / aspectWidth
        let newY = (subjectFrame.origin.y / aspectHeight)
        
        var newRect = CGRect(x: newX + safeAreaX, y: newY + safeAreaY, width: newSubjectWidth, height: newSubjectHeight)
        
        newRect = player.playerView.convert(newRect, to: view)
        
        return newRect
    }
    
    private func resetObjectAndActivityLabel(index: Int) {
        if editObjectOn {
            let object = Object(label: selectedDDObject ?? "", boundingBox: editSubjectCordinates)
            updateObjectsOnFrameChange(frameIndex: index, objects: [object], animationValue: 0.01)
        } else if editActivityOn {
            if let defaultObject = subjectEditedCordinates.first(where: {$0.label == arrDropDownObject.first}) {
                updateObjectsOnFrameChange(frameIndex: index, objects: [defaultObject], animationValue: 0.01)
            } else {
                updateObjectsOnFrameChange(frameIndex: index, objects: [], animationValue: 0.01)
            }
        } else {
            updateObjectsOnFrameChange(frameIndex: index, objects: subjectEditedCordinates, animationValue: 0.01)
        }
    }
    
    private func getModifiedActivityText(activities: [Activity]) -> String {
        var text = ""
        for activity in activities {
            text = text == "" ? activity.label : text + "\n\n" + activity.label
        }
        return text
    }
    
    private func enableDisableSubmit(enable: Bool) {
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
        if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
            previewScrubBar1.buttonSubmit.isEnabled = enable
            previewScrubBar1.buttonSubmit.setTitleColor(enable ? .white : .darkGray, for: .normal)
        }
    }
    
    private func enableDisableRetake(enable: Bool) {
        if enable {
            buttonUndo.isUserInteractionEnabled = true
            self.buttonUndo.setImage(#imageLiteral(resourceName: "ic_undo_dark.png"), for: .normal)
            self.buttonUndo.backgroundColor = #colorLiteral(red: 1, green: 1, blue: 1, alpha: 0.66)
        } else {
            buttonUndo.isUserInteractionEnabled = false
            self.buttonUndo.setImage(#imageLiteral(resourceName: "ic_undo_white"), for: .normal)
            self.buttonUndo.backgroundColor = #colorLiteral(red: 0, green: 0, blue: 0, alpha: 0.66)
        }
    }
    
    private func nameChangeSubmitSave(save: Bool, text: String?) {
        self.okButtonToggle = text == nil ? false : true
        self.buttonClose.setImage(text == nil ? #imageLiteral(resourceName: "close") : #imageLiteral(resourceName: "back_small") , for: .normal)
        let title = save ? "Save" : (text == nil ? "Submit" : text)
        let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
        if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
            previewScrubBar1.buttonSubmit.setTitle(title, for: .normal)
        }
    }
    
    private func enableDisableButtonElements(enable: Bool) {
        switch editVideoFlow {
        case .MyVideoPreview:
            self.nameChangeSubmitSave(save: !enable, text: "OK")
            self.enableDisableSubmit(enable: enable)
            self.buttonUndo.isHidden = enable
            self.buttonEditActivity.isHidden = enable
            self.buttonEditObject.isHidden = enable
            self.buttonEdit.isHidden = !enable
            self.buttonZoomIn.isHidden = enable
            self.buttonZoomOut.isHidden = enable
            self.buttonInfo.isHidden = enable
            self.backgroundViewZoom.isHidden = enable
        case .VideoCollection:
            self.nameChangeSubmitSave(save: !enable, text: nil)
            self.enableDisableSubmit(enable: enable)
            self.submitSaveToggle = false
        default:
            break
        }
    }
    
    private func movePlayerToBegining() {
        self.player.movedToBeginning()
        frameCount = 0
        scrubBarView?.timerObserver(time: .zero)
    }
    
    private func reccurForWidth(arr: [String]) -> (CGFloat) {
        var widthDD: CGFloat = 150
        for arrStr in arr {
            let text = arrStr
            let size = text.size(withAttributes:[.font: UIFont.systemFont(ofSize:13.0)])
            let width = size.width
            widthDD = (width + 50) > widthDD ? (width + 50) : widthDD
            
        }
        print("widthDD---\(widthDD)---\(self.view.width)")
        return widthDD
    }
    
    private func setTopBottomBlackSpaceValue() {
        potraitSafeAreaValueForVideo = self.player.playerView.playerLayer.videoRect.origin.y
        screenHeightWithSafeArea = self.player.playerView.playerLayer.videoRect.origin.y * 2
        landscapeSafeAreaValueForVideo = self.player.playerView.playerLayer.videoRect.origin.x
        screenWidthWithSafeArea = self.player.playerView.playerLayer.videoRect.origin.x * 2
    
        blackViewTopHeight.constant = potraitSafeAreaValueForVideo
        blackViewBottomHeight.constant = potraitSafeAreaValueForVideo
        blackViewLeftWidth.constant = landscapeSafeAreaValueForVideo
        blackViewRightWidth.constant = landscapeSafeAreaValueForVideo
        
        if VideoVariables.deviceOrientationRawValue < 3  {
            self.buttonCloseConstraintLeading.constant = 20
            self.buttonCloseConstraintTop.constant = 1
            self.buttonUndoConstraintTrailing.constant = 20
        } else {
            self.buttonCloseConstraintLeading.constant = 1
            self.buttonCloseConstraintTop.constant = 20
            self.buttonUndoConstraintTrailing.constant = 1
        }
        
        setupScrubber()
    }
    
    // Transform Player view based on zoom scale value
    private func applyZoomOnPlayerView() {
        
        self.player.view.transform = CGAffineTransform(scaleX: zoomScale, y: zoomScale)
        self.resetObjectAndActivityLabel(index: frameCount)
    }
    
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
    
    // MARK:- Actions
    private func navigateToNextScreen() {
        self.hideProgress()
        self.navigationController?.popToRootViewController(animated: true)
    }
    
    @IBAction func zoomInButtonAction(_ sender: UIButton) {
        player.pause()
        zoomScale = (zoomScale == (kEditVideoZoomScaleValue * kEditVideoMaxZoomLevel + 1.0))
                    ? zoomScale
                    : zoomScale + kEditVideoZoomScaleValue
        applyZoomOnPlayerView()
        buttonZoomIn.isEnabled = (zoomScale == (kEditVideoZoomScaleValue * kEditVideoMaxZoomLevel + 1.0))
                                    ? false
                                    : true
        buttonZoomOut.isEnabled = true
    }
    
    @IBAction func zoomOutButtonAction(_ sender: UIButton) {
        player.pause()
        zoomScale = zoomScale == 1.0 ? 1.0 : zoomScale - kEditVideoZoomScaleValue
        applyZoomOnPlayerView()
        buttonZoomOut.isEnabled = zoomScale == 1.0 ? false : true
        buttonZoomIn.isEnabled = true
    }
    
    @IBAction func closeBtnAction(_ sender: UIButton) {
        self.hideDropDown()
        player.pause()
        
        if okButtonToggle {
            self.navigateToNextScreen()
        } else {
            UIUtilities.showAlertMessageWithTwoActionsAndHandler("Close", errorMessage: kExitEditVideo, errorAlertActionTitle: LocalizableString.ok.localizedString, errorAlertActionTitle2: LocalizableString.cancel.localizedString, viewControllerUsed: self, action1: {
                RecordVideoVC.valBlurActionStatus = "Completed"
                self.hideProgress()
                self.navigationController?.popViewController(animated: true)
            }) {
                
            }
        }
    }
    
    @IBAction func undoBtnPressed(_ sender: UIButton) {
        
        self.hideDropDown()
        
        // Alternative Solution Starts
        //self.subjectActivities = subjectRedoActivities
        
        if editObjectOn && selectedDDObject != "" {
            if selectedDDObject == arrDropDownObject.first {
                editObjectMissingFrames = []
                editObjectExistingFrames = []
                let activity = Activity(activityName: "", sTime: 0, activityIndex: 0)
                activity.endFrame = player.numberOfFrames - 1
                editObjectMissingFrames.append(activity)
            }
            //if editSubjectCordinates.count == 0 {
                editSubjectCordinates = []
                if selectedDDObject == arrDropDownObject.first {
                    editObjectExistingFrames = objectExistingFrames
                    editObjectMissingFrames = objectMissingFrames
                }
                subjectEditedCordinates = subjectRedoEditedCordinates
                editSubjectCordinates = subjectEditedCordinates.first(where: {$0.label == selectedDDObject ?? ""})?.boundingBox ?? []
//            } else {
//                editSubjectCordinates = []
//            }
            
        }
        
        if editActivityOn {
            //Uncomment to support deleting complete selected activity and get last change before edit start
//            if subjectRedoActivities != nil {
                self.subjectActivities = subjectRedoActivities
//            } else {
//                isFirstActivityRecorded = true
//                removeSameActivityLabel()
//            }
        }
        // Alternative Solution Ends
        coloredProgress()
        movePlayerToBegining()
        self.player.pause()
        
        
        resetObjectAndActivityLabel(index: 1)//SEE
        enableDisableRetake(enable: false)
    }
    
    @IBAction func activityEditorBtnPressed(_ sender: UIButton) {
        enableDisableRetake(enable: false)
        if selectedDropDownIndex == 0 {
            self.player.pause()
            if !editActivityOn {
                arrOfSubjectView.forEach({$0.removeFromSuperview()})
                activityLabel.isHidden = true //activityLabel.removeFromSuperview()
            }
            
            selectedDropDownIndex = 1
        } else if selectedDropDownIndex == 1 {
            selectedDropDownIndex = 0
        } else {
            self.player.pause()
            selectedDropDownIndex = 1
            textFieldEditObject.touchAction()
            arrOfSubjectView.forEach({$0.removeFromSuperview()})
            activityLabel.isHidden = true //activityLabel.removeFromSuperview()
        }
        self.textFieldEditActivity.touchAction()
    }
    
    @IBAction func objectEditorBtnPressed(_ sender: UIButton) {
        enableDisableRetake(enable: false)
        if selectedDropDownIndex == 0 {
            self.player.pause()
            if !editObjectOn {
                arrOfSubjectView.forEach({$0.removeFromSuperview()})
                activityLabel.isHidden = true //activityLabel.removeFromSuperview()
            }
            
            selectedDropDownIndex = 2
        } else if selectedDropDownIndex == 2 {
            selectedDropDownIndex = 0
        } else {
            self.player.pause()
            selectedDropDownIndex = 2
            textFieldEditActivity.touchAction()
        }
        self.textFieldEditObject.touchAction()
    }
    
    @IBAction func editBtnPressed(_ sender: UIButton) {
        self.buttonClose.setImage(#imageLiteral(resourceName: "close"), for: .normal)
        enableDisableSubmit(enable: false)
        nameChangeSubmitSave(save: true, text: nil)
        self.buttonUndo.isHidden = false
        self.buttonEditActivity.isHidden = false
        self.buttonEditObject.isHidden = false
        self.textFieldEditActivity.isHidden = false
        self.textFieldEditObject.isHidden = false
        self.buttonZoomIn.isHidden = false
        self.buttonZoomOut.isHidden = false
        self.buttonInfo.isHidden = false
        self.backgroundViewZoom.isHidden = false
        self.buttonEdit.isHidden = true
        submitSaveToggle = true
        if editVideoFlow == .MyVideoPreview {
            self.coloredProgress()
        }
        self.movePlayerToBegining()
        self.player.pause()
    }
    
    @IBAction func infoBtnAction(_ sender: UIButton) {
        if collectionDescription != "" {
            self.player.pause()
            setUpCustomView()
        }
    }
    
    func uploadProcess() {
        let jsonCreatedBool = createJsonFile()
        if jsonCreatedBool {
            uploadJsonFile()
        } else {
            self.hideProgress()
        }
    }
    
    private func createJsonFile() -> Bool {
        if let metaData = subjectMetadata, let activities = subjectActivities {
            
            let data = WelcomeAtEncode(metadata: metaData, activity: activities, object: subjectEditedCordinates)
            let body = data.getJsonBody()
            
            fileUrl = Utilities.saveJsonToFile(dic: body, fileName: "videoinfo.json")?.absoluteString
            if activities.count < 1 && arrDropDownActivity.count > 0 {
                DispatchQueue.main.async {
                    
                    UIUtilities.showAlertMessageWithTwoActionsAndHandler(kReEditActivity,
                                                                     errorMessage: kNoActivities,
                                                                     errorAlertActionTitle: LocalizableString.no.localizedString,
                                                                     errorAlertActionTitle2: LocalizableString.yes.localizedString,
                                                                     viewControllerUsed: self, action1: {
                                                                        self.enableDisableSubmit(enable: true)
                                                                        self.uploadState = false
                                                                        self.view.isUserInteractionEnabled = true
                                                                    }) {
                                                                        self.showProgress()
                                                                        self.uploadJsonFile()
                                                                    }
                }
                return false
            }
            
            return true
        }
        return false
    }
    
    //MARK: S3 bucket Calls
    private func uploadJsonFile() {
        enableDisableSubmit(enable: false)
        uploadState = true
        AWSS3Manager.shared.uploadOtherFile(fileUrl: URL(string: fileUrl)!, fileName: fileName, conentType: "json", progress: nil) { [weak self] (uploadedFileUrl, error) in
            guard let self = self else { return }
            self.uploadState = false
            
            self.enableDisableSubmit(enable: true)
            
            if let finalPath = uploadedFileUrl as? String {
                print("Uploaded file url: " + finalPath)
                
                if self.editVideoFlow == .VideoCollection {
                    self.uploadVideo()
                } else {
                    self.hideProgress()
                    UIUtilities.showAlertMessageWithActionHandler(kUploadCompleted, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                        self.navigateToNextScreen()
                    }
                }
                
            } else {
                self.hideProgress()
                self.uploadState = false
                print("\(String(describing: error?.localizedDescription))")
                self.enableDisableSubmit(enable: true)
                UIUtilities.showAlertMessage(kUploadFailed, errorMessage: kReSubmit, errorAlertActionTitle: "Ok", viewControllerUsed: self)
            }
        }
    }
    
    private func uploadVideo() {
        
        videoURL = URL(fileURLWithPath: Utilities.getFileUrlStringFromCache(fileName: "videoBlurred.mp4"))
        
        AWSS3Manager.shared.uploadVideo(videoUrl: videoURL, fileName: fileName, progress: nil) { [weak self] (uploadedFileUrl, error) in
            guard let self = self else { return }
            
            self.uploadState = false
            
            let previewScrubBar = self.view.subviews.filter{($0.isKind(of: PreviewScrubBarWithSubmit.self))}.first
            if previewScrubBar != nil, let previewScrubBar1 = previewScrubBar as? PreviewScrubBarWithSubmit {
                previewScrubBar1.buttonSubmit.isEnabled = true
            }
            if let finalPath = uploadedFileUrl as? String {
                print("Uploaded file url: " + finalPath)
                
                self.hideProgress()
                UIUtilities.showAlertMessageWithActionHandler(kUploadCompleted, message: "", buttonTitle: "OK", viewControllerUsed: self) {
                    self.navigateToNextScreen()
                }
                
            } else {
                self.hideProgress()
                self.uploadState = false
                print("\(String(describing: error?.localizedDescription))")
                self.enableDisableSubmit(enable: true)
                
                UIUtilities.showAlertMessage(kUploadFailed, errorMessage: kReSubmit, errorAlertActionTitle: "Ok", viewControllerUsed: self)                
            }
        }
    }
    
}

//MARK: CustomPopUP Delegate Methods
extension EditVideoVC: CustomPopUpDelegate {
    
    func didTapOnOkBtn() {
        buttonInfo.setImage(#imageLiteral(resourceName: "question_white"), for: .normal)
        buttonInfo.backgroundColor = #colorLiteral(red: 0, green: 0, blue: 0, alpha: 0.66)
        self.removeCustomView()
    }
}

// MARK: - UIGestureRecognizer

extension EditVideoVC {
    
    @objc func handleTapGestureRecognizer(_ gestureRecognizer: UITapGestureRecognizer) {
        
        guard selectedDropDownIndex == 0 else {
            self.hideDropDown()
            return
        }
        
        switch (self.player.playbackState.rawValue) {
        case PlaybackState.stopped.rawValue:
            self.player.playFromBeginning()
            break
        case PlaybackState.paused.rawValue:
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

extension EditVideoVC: PlayerDelegate {
    
    func playerReady(_ player: PlayerController) {
        print("\(#function) ready")
    }
    
    func playerPlaybackStateDidChange(_ player: PlayerController) {
        print("\(#function) \(player.playbackState.description)")
        
        switch player.playbackState {
        case .stopped:
            if !isObjectTracking {
                resetObjectAndActivityLabel(index: 1)
            }
            
        case .playing:
            // Alternative Solution Starts
            if editObjectOn && selectedDDObject != "" {
                removeSelectedObject()
                
//                if let _ = subjectRedoEditedCordinates.first(where: {$0.label == selectedDDObject}) {
//                    enableDisableRetake(enable: true)
//                }
            } else if editActivityOn && selectedDDActivity != "" {
                if let _ = subjectRedoEditedCordinates.first(where: {$0.label == arrDropDownObject.first ?? ""}), let _ = subjectRedoActivities?.first(where: {$0.label == selectedDDActivity}), isFirstActivityRecorded {
                    enableDisableRetake(enable: true)
                    enableDisableSubmit(enable: true)
                }
                removeSameActivityLabel()
            }
            // Alternative Solution Ends
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

extension EditVideoVC: PlayerPlaybackDelegate {
    
    func playerCurrentTimeDidChange(_ player: PlayerController) {
        
        if frameCount <= 5 {
            self.setTopBottomBlackSpaceValue()
        }
        scrubBarView?.timerObserver(time: player.currentTime)
        frameCount += 1
        
        if editObjectOn {
            if !isObjectTracking {
                let object = Object(label: selectedDDObject ?? "", boundingBox: editSubjectCordinates)
                updateObjectsOnFrameChange(frameIndex: frameCount, objects: [object], animationValue: 0.2)
            } else {
                self.saveSubjectCordinatesOnFrameChange(index: frameCount - 1)
            }
        } else if editActivityOn {
            if let defaultObject = subjectEditedCordinates.first(where: {$0.label == arrDropDownObject.first}) {
                updateObjectsOnFrameChange(frameIndex: frameCount, objects: [defaultObject], animationValue: 0.2)
            } else {
                updateObjectsOnFrameChange(frameIndex: frameCount, objects: [], animationValue: 0.2)
            }
        } else {
            updateObjectsOnFrameChange(frameIndex: frameCount, objects: subjectEditedCordinates, animationValue: 0.2)
        }
    }
    
    func playerPlaybackWillStartFromBeginning(_ player: PlayerController) {
        frameCount = 0
    }
    
    func playerPlaybackDidEnd(_ player: PlayerController) {

        if isObjectTracking {
            // Alternative Solution Starts
            getDefaultObjectMissingFrames()
            editDefaultObjectStartFrame = nil
            editDefaultObjectEndFrame = nil
            // Alternative Solution Ends
            isObjectTracking = false
        } else if activityEditingInProgress {
            
        } else {
            movePlayerToBegining()
            resetObjectAndActivityLabel(index: 1)
        }
    }
    
    func playerPlaybackWillLoop(_ player: PlayerController) {
        
    }
    
    func playerPlaybackDidLoop(_ player: PlayerController) {
    }
}

extension EditVideoVC: ScrubBarDelegate {
    
    func didTapOnSubmit() {
        
        if okButtonToggle {
            self.navigateToNextScreen()
        } else if submitSaveToggle {
            saveObjectChanges()
            nameChangeSubmitSave(save: false, text: nil)
            self.buttonUndo.isHidden = true
            self.buttonEditActivity.isHidden = true
            self.buttonEditObject.isHidden = true
            self.textFieldEditActivity.isHidden = true
            self.textFieldEditObject.isHidden = true
            self.buttonEdit.isHidden = false
            submitSaveToggle = false
            self.deactivateObject()
            self.deactivateActivity()
            enableDisableRetake(enable: false)
            
            self.resetObjectAndActivityLabel(index: 1)
            hideDropDown()
            movePlayerToBegining()
            self.player.pause()
        } else {
            enableDisableSubmit(enable: false)
            uploadState = true
            
            self.showProgress()
            self.view.isUserInteractionEnabled = false
            uploadProcess()
        }
    }
    
}

//MARK: Scrub Bar Delegate
extension EditVideoVC: EditScrubBarDelegate {
    
    func didDragScrub(value: Float, ended: Bool) {
        let time =  CMTimeMake(value: Int64(value), timescale: 1)
        
        let valRatio = player.numberOfFrames / Int(player.maximumDuration)
        frameCount = Int(value * Float(valRatio))
        
        if ended {
            self.player.playFromDragTime(time: time)
        }
        else if frameCount > 0 {
            if editObjectOn {
                let object = Object(label: selectedDDObject ?? "", boundingBox: editSubjectCordinates)
                updateObjectsOnFrameChange(frameIndex: frameCount, objects: [object], animationValue: 0.2)
            } else if editActivityOn {
                if let defaultObject = subjectEditedCordinates.first(where: {$0.label == arrDropDownObject.first}) {
                    updateObjectsOnFrameChange(frameIndex: frameCount, objects: [defaultObject], animationValue: 0.2)
                } else {
                    updateObjectsOnFrameChange(frameIndex: frameCount, objects: [], animationValue: 0.2)
                }
            } else {
                updateObjectsOnFrameChange(frameIndex: frameCount, objects: subjectEditedCordinates, animationValue: 0.2)
            }
        }
    }
}

//MARK: Touch Delegates Methods
extension EditVideoVC {
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        if editActivityOn {
            startActivity(touches, with: event)
            //activityEditingInProgress = true
        } else if editObjectOn {
            isObjectTracking = true
            reDrawSubjectAsceptRatio(touches, playVideo: false)
            editDefaultObjectStartFrame = frameCount
        }
    }
    
    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        if editActivityOn {
            let touchesCount = event?.allTouches?.count // to cancel multiple touches
            
            if activityEditingInProgress {
                var touchStart = CGPoint.zero
                if let touch = touches.first {
                    touchStart = touch.location(in: self.view)
                }
                
                if arrOfSubjectView.filter({($0.frame.contains(touchStart))}).count == 0 {
                    endActivity(touches, with: event)
                }
                
            }
            else if touchesCount ?? 0 > 1 && subjectActivities?.count ?? 0 > 0 && subjectActivities?.last?.endFrame == nil {
                endActivity(touches, with: event)
            }
        } else if editObjectOn && isObjectTracking {
            reDrawSubjectAsceptRatio(touches, playVideo: true)
            translateSubjectUsingSingleFinger(touches)
        }
    }
    
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        if editActivityOn {
            endActivity(touches, with: event)
        } else if editObjectOn {
            updateCurrentSubjectRect(boxRect: arrOfSubjectView[0].frame)
            getDefaultObjectMissingFrames()
            editDefaultObjectStartFrame = nil
            editDefaultObjectEndFrame = nil
            isObjectTracking = false
            if !(((touches.first?.view as? ShadowView) != nil)) {
                activityLabel.isHidden = true
                arrOfSubjectView[0].isHidden = true
            }
        }
        tapGestureRecognizer?.isEnabled = true
    }
    
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        if editActivityOn {
            endActivity(touches, with: event)
        } else if editObjectOn {
            isObjectTracking = false
        }
        tapGestureRecognizer?.isEnabled = true
    }
}

//MARK: Activity Editor
extension EditVideoVC {
    
    func trimActivitiesOnObjectEditor() {
        
        guard let defaultBB = subjectEditedCordinates.first, defaultBB.boundingBox.count > 0 else {
            return
        }
        
        editObjectMissingFrames = []
    
        var prevoiusFrameIndex = 0
        
        defaultBB.boundingBox.forEach { (box) in
            if let previousBox = defaultBB.boundingBox.first(where: {$0.frameIndex == box.frameIndex - 1}) {
                
                if box.frameIndex == defaultBB.boundingBox.last?.frameIndex ?? 0 {
                    if (player.numberOfFrames - 1) != box.frameIndex {
                        let activity = Activity(activityName: "", sTime: box.frameIndex + 1, activityIndex: 0)
                        activity.endFrame = player.numberOfFrames - 1
                        editObjectMissingFrames.append(activity)
                        return
                    }
                }
                
                if box.frameIndex - previousBox.frameIndex != 1 {
                    let activity = Activity(activityName: "", sTime: previousBox.frameIndex + 1, activityIndex: 0)
                    activity.endFrame = box.frameIndex - 1
                    editObjectMissingFrames.append(activity)
                } else {
                    prevoiusFrameIndex = box.frameIndex + 1
                }
                
            } else {
                if box.frameIndex != 0 {
                    let activity = Activity(activityName: "", sTime: prevoiusFrameIndex, activityIndex: 0)
                    activity.endFrame = box.frameIndex - 1
                    editObjectMissingFrames.append(activity)
                }
            }
        }
        
        editObjectExistingFrames = []
        
        if editObjectMissingFrames.count == 0 {
            let activity = Activity(activityName: "", sTime: 0, activityIndex: 0)
            activity.endFrame = (defaultBB.boundingBox.last?.frameIndex ?? 0)
            editObjectExistingFrames.append(activity)
        } else {
            if editObjectMissingFrames.first?.startFrame != 0 {
                let activity = Activity(activityName: "", sTime: 0, activityIndex: 0)
                activity.endFrame = (defaultBB.boundingBox.first?.frameIndex ?? 0) - 1
                editObjectExistingFrames.append(activity)
            }
            
            if editObjectMissingFrames.last?.endFrame != (player.numberOfFrames - 1) {
                let activity = Activity(activityName: "", sTime: (editObjectMissingFrames.last?.endFrame ?? 0) + 1, activityIndex: 0)
                activity.endFrame = player.numberOfFrames - 1
                editObjectExistingFrames.append(activity)
            }
            
            for (index, object) in editObjectMissingFrames.enumerated() {
                if index != 0 {
                    if (object.startFrame - (editObjectMissingFrames[index - 1].endFrame ?? 0)) != 1 {
                        let activity = Activity(activityName: "", sTime: (editObjectMissingFrames[index - 1].endFrame ?? 0) + 1, activityIndex: 0)
                        activity.endFrame = object.startFrame - 1
                        
                        if activity.startFrame != activity.endFrame {
                            editObjectExistingFrames.append(activity)
                        }
                    }
                }
            }
        }
        
        objectExistingFrames = editObjectExistingFrames
        objectMissingFrames = editObjectMissingFrames
    }
    
    func startActivity(_ touches: Set<UITouch>, with event: UIEvent?) {
        
        let touchesCount = event?.allTouches?.count // to cancel multiple touches
        
        if touchesCount == 1 {
            var touchStart = CGPoint.zero
            if let touch = touches.first {
                touchStart = touch.location(in: self.view)
            }
            if let viewTouch = arrOfSubjectView.first(where: {$0.frame.contains(touchStart)}), viewTouch.isDefaultObject, !viewTouch.isHidden {
                activityEditingInProgress = true
                let selectedActivity = Activity(activityName: selectedDDActivity!, sTime: frameCount, activityIndex: 1)
                
                if self.player.playbackState.rawValue != PlaybackState.playing.rawValue {
                    self.player.playFromCurrentTime()
                }
                
                // Alternative Solution Starts
//                if isFirstActivityRecorded {
//                    //removeSameActivityLabel()
//                }
//                self.subjectRedoActivities = nil //Uncomment to support deleting complete selected activity and get last change before edit start
//                self.subjectRedoActivities = subjectActivities //Uncomment to get last change even same instance
                // Alternative Solution Ends
                
                self.subjectActivities?.append(selectedActivity)
                //isFirstActivityRecorded = false
                
                enableDisableRetake(enable: true)
                enableDisableSubmit(enable: true)
                
                tapGestureRecognizer?.isEnabled = false                
            }
            else {
                tapGestureRecognizer?.isEnabled = true
            }
        }
        else {
            tapGestureRecognizer?.isEnabled = true
        }
    }
    
    func endActivity(_ touches: Set<UITouch>, with event: UIEvent?) {
        tapGestureRecognizer?.isEnabled = true
        
        if subjectActivities?.count ?? 0 > 0 && subjectActivities?.last?.endFrame == nil {
            if subjectActivities?.last?.endFrame == nil && (subjectActivities?.last?.startFrame ?? 0) >= 0 {
                if frameCount > 0 {
                    subjectActivities?.last?.endFrame = frameCount
                }
                else {
                    subjectActivities?.last?.endFrame = player.numberOfFrames
                }
            }
//            let frameDifference = (subjectActivities?.last?.endFrame ?? 0) - (subjectActivities?.last?.startFrame ?? 0)
//            if frameDifference < 15 {
//                subjectActivities?.removeLast()
//            }
//            trimActivities()
            coloredProgress()
        }
        activityEditingInProgress = false
    }
    
    func trimActivities() {
        if let buffSubjectActivities = subjectActivities {
            for arr in buffSubjectActivities {
                let frameDifference = (arr.endFrame ?? 0) - (arr.startFrame )
                if frameDifference < 15 {
                    if let index = self.subjectActivities!.firstIndex(of: arr) {
                        self.subjectActivities?.remove(at: index)
                    }
                }

            }
        }
        let frameDifference = (subjectActivities?.last?.endFrame ?? 0) - (subjectActivities?.last?.startFrame ?? 0)
        if frameDifference < 15 && subjectActivities?.count ?? 0 > 0 {
            subjectActivities?.removeLast()
        }
        
        
        let lastActivity = self.subjectActivities?.last
        if self.subjectActivities?.count ?? 0 > 1 {
            ///Same Label Remove
            let endFrame = subjectActivities?.last?.endFrame ?? 0 // Dont Rearrange
            let startFrame = subjectActivities?.last?.startFrame ?? 0
            let endFrameLimit = endFrame - 14
            let startFrameLimit = startFrame + 14
            
            var sameActivityOverlap = self.subjectActivities?.filter{
                ((($0.endFrame ?? 0 > startFrameLimit && $0.startFrame < startFrameLimit) || ($0.startFrame < endFrameLimit && $0.endFrame ?? 0 > endFrameLimit)) && $0.label == selectedDDActivity )
            }
            if (sameActivityOverlap?.contains(lastActivity!))! {
                if sameActivityOverlap?.count ?? 0 > 1 {
                    if let index = sameActivityOverlap!.firstIndex(of: lastActivity!) {
                        sameActivityOverlap?.remove(at: index)
                    }
                    for arr in sameActivityOverlap ?? [] {
                        if let index = self.subjectActivities!.firstIndex(of: arr) {
                            self.subjectActivities?.remove(at: index)
                        }
                    }
                }
            }
        }
    }
    
    private func removeSameActivityLabel() {
        
        // Alternative Solution Starts
        if let activityForSameLabel = self.subjectActivities, self.subjectActivities?.count ?? 0 > 0, isFirstActivityRecorded {
            // Alternative Solution Ends
            let arrSameLabel = activityForSameLabel.filter { $0.label == selectedDDActivity }
            if arrSameLabel.count > 0 {
                for arr in arrSameLabel {
                    if let index = self.subjectActivities!.firstIndex(of: arr) {
                        self.subjectActivities?.remove(at: index)
                    }
                }
            }
        }
        isFirstActivityRecorded = false
    }
    
    func coloredProgress() {
        
        if subjectActivities?.count ?? 0 > 0 {
            if let subviews = self.scrubBarView?.sliderColoredView.subviews {
                for view in subviews {
                    view.removeFromSuperview()
                }
            }
            let activityForLabel = self.subjectActivities! // For Label
            
            let arrSameLabel = activityForLabel.filter { $0.label == selectedDDActivity }
            let arrDifferentLabel = activityForLabel.filter { $0.label != selectedDDActivity }
            
            if arrDifferentLabel.count > 0 {
                coloredArrayReccur(array: arrDifferentLabel)
            }
            if arrSameLabel.count > 0 {
                coloredArrayReccur(array: arrSameLabel)
            }
        } else {
            if let subviews = self.scrubBarView?.sliderColoredView.subviews {
                for view in subviews {
                    view.removeFromSuperview()
                }
            }
            coloredArrayReccur(array: [])
        }
    }
    
    func coloredArrayReccur(array: [Activity]) {
        let totalWidth = Int(self.scrubBarView?.sliderColoredView.width ?? 0)
        for activity in array {
            let startValue = activity.startFrame
            let endValue = activity.endFrame ?? 0
            let totalFrameCount = player.numberOfFrames - 1
            
            let xValue = (Float(startValue) / Float(totalFrameCount)) * Float(totalWidth)
            let widthValue = (Float(endValue - startValue) / Float(totalFrameCount)) * Float(totalWidth)
            let activityView = UIView()
            let height = Int(self.scrubBarView?.sliderColoredView.height ?? 0)
            
            activityView.frame = CGRect(x: Int(xValue), y: 0, width: Int(widthValue), height: height)
            activityView.backgroundColor = .lightGray //activity.label == selectedDDActivity ? .white : .lightGray
            self.scrubBarView?.sliderColoredView.addSubview(activityView)
//            if activity.label == selectedDDActivity {
//                self.scrubBarView?.sliderColoredView.bringSubviewToFront(activityView)
//            }
            
        }
        
        for missingObject in editObjectMissingFrames {
            let startValue = missingObject.startFrame
            let endValue = missingObject.endFrame ?? 0
            let totalFrameCount = player.numberOfFrames
            
            let xValue = (Float(startValue) / Float(totalFrameCount)) * Float(totalWidth)
            let widthValue = (Float(endValue - startValue) / Float(totalFrameCount)) * Float(totalWidth)
            let activityView = UIView()
            let height = Int(self.scrubBarView?.sliderColoredView.height ?? 0)
            
            activityView.frame = CGRect(x: Int(xValue), y: 0, width: Int(widthValue), height: height)
            activityView.backgroundColor = .darkGray
            self.scrubBarView?.sliderColoredView.addSubview(activityView)
            self.scrubBarView?.sliderColoredView.bringSubviewToFront(activityView)
        }
    }
}

//MARK: Object Editor
extension EditVideoVC {
    
    private func translateSubjectUsingSingleFinger(_ touches: Set<UITouch>) {
        
        if let touch = touches.first {
            
            let currentPoint = touch.location(in: self.view)
            let previous = touch.previousLocation(in: self.view)
            
            if arrOfSubjectView[0].frame.contains(currentPoint) && !arrOfSubjectView[0].isHidden {
                
                tapGestureRecognizer?.isEnabled = false
                
                arrOfSubjectView[0].center = CGPoint(x: self.arrOfSubjectView[0].center.x + currentPoint.x - previous.x,
                                                  y: self.arrOfSubjectView[0].center.y + currentPoint.y - previous.y)
                arrOfSubjectView[0].frame.origin.y = arrOfSubjectView[0].frame.origin.y < potraitSafeAreaValueForVideo ? potraitSafeAreaValueForVideo : arrOfSubjectView[0].frame.origin.y
                arrOfSubjectView[0].frame.origin.y = arrOfSubjectView[0].frame.origin.y > ((view.frame.height - potraitSafeAreaValueForVideo) - arrOfSubjectView[0].frame.size.height) ? ((view.frame.height - potraitSafeAreaValueForVideo) - arrOfSubjectView[0].frame.size.height) : arrOfSubjectView[0].frame.origin.y
                arrOfSubjectView[0].frame.size.height = arrOfSubjectView[0].frame.size.height > (view.frame.height - screenHeightWithSafeArea) ? (view.frame.height - screenHeightWithSafeArea) : arrOfSubjectView[0].frame.size.height

                arrOfSubjectView[0].frame.origin.x = arrOfSubjectView[0].frame.origin.x > ((view.frame.size.width - landscapeSafeAreaValueForVideo) - arrOfSubjectView[0].frame.size.width) ? ((view.frame.size.width - landscapeSafeAreaValueForVideo) - arrOfSubjectView[0].frame.size.width) : arrOfSubjectView[0].frame.origin.x
                arrOfSubjectView[0].frame.origin.x = arrOfSubjectView[0].frame.origin.x < landscapeSafeAreaValueForVideo ? landscapeSafeAreaValueForVideo : arrOfSubjectView[0].frame.origin.x
                arrOfSubjectView[0].frame.size.width = arrOfSubjectView[0].frame.size.width > view.frame.width ? view.frame.width : arrOfSubjectView[0].frame.size.width
                
                arrOfSubjectView[0].isHidden = false
                
                updateobjectLabel()
                
                if self.player.playbackState.rawValue != PlaybackState.playing.rawValue {
                    self.player.playFromCurrentTime()
                }
                
                updateCurrentSubjectRect(boxRect: arrOfSubjectView[0].frame)
                
            } else {
                tapGestureRecognizer?.isEnabled = true
            }
        }
    }
    
    private func reDrawSubjectAsceptRatio(_ touches: Set<UITouch>, playVideo: Bool) {
        
        if touches.count == 2 {
            
            var arrTouches: [UITouch] = []
            
            for touch in touches {
                arrTouches.append(touch)
            }
            
            let touch = arrTouches[0].location(in: self.view)
            let touch1 = arrTouches[1].location(in: self.view)
                        
            arrOfSubjectView[0].frame.size.width = (touch.x - touch1.x) < 0 ? -(touch.x - touch1.x) : (touch.x - touch1.x)
            arrOfSubjectView[0].frame.size.height = (touch.y - touch1.y) < 0 ? -(touch.y - touch1.y) : (touch.y - touch1.y)
            arrOfSubjectView[0].frame.origin.x = touch.x < touch1.x ? touch.x : touch1.x
            arrOfSubjectView[0].frame.origin.y = touch.y < touch1.y ? touch.y : touch1.y
            
            arrOfSubjectView[0].frame.origin.y = arrOfSubjectView[0].frame.origin.y < potraitSafeAreaValueForVideo ? potraitSafeAreaValueForVideo : arrOfSubjectView[0].frame.origin.y
            arrOfSubjectView[0].frame.origin.y = arrOfSubjectView[0].frame.origin.y > ((view.frame.height - potraitSafeAreaValueForVideo) - arrOfSubjectView[0].frame.size.height) ? ((view.frame.height - potraitSafeAreaValueForVideo) - arrOfSubjectView[0].frame.size.height) : arrOfSubjectView[0].frame.origin.y
            arrOfSubjectView[0].frame.size.height = arrOfSubjectView[0].frame.size.height > (view.frame.height - screenHeightWithSafeArea) ? (view.frame.height - screenHeightWithSafeArea) : arrOfSubjectView[0].frame.size.height
            arrOfSubjectView[0].frame.size.height = arrOfSubjectView[0].frame.size.height < 50 ? 50 : arrOfSubjectView[0].frame.size.height

            arrOfSubjectView[0].frame.origin.x = arrOfSubjectView[0].frame.origin.x > ((view.frame.size.width - landscapeSafeAreaValueForVideo) - arrOfSubjectView[0].frame.size.width) ? ((view.frame.size.width - landscapeSafeAreaValueForVideo) - arrOfSubjectView[0].frame.size.width) : arrOfSubjectView[0].frame.origin.x
            arrOfSubjectView[0].frame.origin.x = arrOfSubjectView[0].frame.origin.x < landscapeSafeAreaValueForVideo ? landscapeSafeAreaValueForVideo : arrOfSubjectView[0].frame.origin.x
            arrOfSubjectView[0].frame.size.width = arrOfSubjectView[0].frame.size.width > view.frame.width ? view.frame.width : arrOfSubjectView[0].frame.size.width
            arrOfSubjectView[0].frame.size.width = arrOfSubjectView[0].frame.size.width < 50 ? 50 : arrOfSubjectView[0].frame.size.width
            arrOfSubjectView[0].isHidden = false
            
            updateobjectLabel()
            
            if self.player.playbackState.rawValue != PlaybackState.playing.rawValue && playVideo {
                self.player.playFromCurrentTime()
            }
            
            updateCurrentSubjectRect(boxRect: arrOfSubjectView[0].frame)
        }
    }
    
    private func updateobjectLabel() {
        if !self.view.subviews.contains(self.activityLabel) {
           self.activityLabel.textColor = UIColor.black
           self.activityLabel.numberOfLines = 10
           self.activityLabel.textAlignment = .center
           self.activityLabel.frame = CGRect.zero
           self.activityLabel.sizeToFit()
           self.view.insertSubview(self.activityLabel, aboveSubview: self.arrOfSubjectView[0])
       }
        self.activityLabel.backgroundColor = UIColor.appColor(.main)
        self.activityLabel.text = selectedDDObject
        self.activityLabel.frame.origin.y = self.arrOfSubjectView[0].frame.origin.y < self.potraitSafeAreaValueForVideo + 35 ? self.arrOfSubjectView[0].frame.origin.y : (self.arrOfSubjectView[0].frame.origin.y - self.activityLabel.frame.size.height)
        self.activityLabel.frame.origin.x = self.arrOfSubjectView[0].frame.origin.x
        self.activityLabel.frame.size.width = self.activityLabel.intrinsicContentSize.width + 10
        self.activityLabel.frame.size.height = 35
        self.activityLabel.isHidden = false
    }
    
    private func updateCurrentSubjectRect(boxRect: CGRect) {
        
        let rect = view.convert(boxRect, to: player.playerView)
        
        var subjectFrame = CGRect.zero
        var screenWidth: CGFloat = 0.0
        var screenHeight: CGFloat = 0.0
        
        if VideoVariables.deviceOrientationRawValue < 3 {
            let safeAreaY = UIDevice.current.hasNotch ? potraitSafeAreaValueForVideo : 0
            subjectFrame = CGRect(x: rect.origin.x, y: rect.origin.y - safeAreaY, width: rect.size.width, height: rect.size.height)
            screenWidth = ScreenSize.SCREEN_WIDTH
            screenHeight = UIDevice.current.hasNotch ? ScreenSize.SCREEN_HEIGHT - screenHeightWithSafeArea : ScreenSize.SCREEN_HEIGHT
        } else {
            let safeAreaX = UIDevice.current.hasNotch ? landscapeSafeAreaValueForVideo : 0
            subjectFrame = CGRect(x: rect.origin.x - safeAreaX, y: rect.origin.y, width: rect.size.width, height: rect.size.height)
            screenWidth = UIDevice.current.hasNotch ? ScreenSize.SCREEN_HEIGHT - screenWidthWithSafeArea : ScreenSize.SCREEN_HEIGHT
            screenHeight = ScreenSize.SCREEN_WIDTH
        }
        
        let height: CGFloat = CGFloat(self.player.naturalSize.height)
        let width: CGFloat = CGFloat(self.player.naturalSize.width)
        
        let aspectWidth = CGFloat(width) / screenWidth
        let aspectHeight = CGFloat(height) / screenHeight
        
        let newSubjectWidth = aspectWidth * subjectFrame.width
        let newSubjectHeight = aspectHeight * subjectFrame.height
        
        let newX = subjectFrame.origin.x * aspectWidth
        let newY = (subjectFrame.origin.y * aspectHeight)
        
        let updatedRect = CGRect(x: newX, y: newY, width: newSubjectWidth, height: newSubjectHeight)
        
        self.subjectCurrentFrame = updatedRect
        
    }
    
    private func saveSubjectCordinatesOnFrameChange(index: Int) {
        
        if editDefaultObjectStartFrame == nil {
            editDefaultObjectStartFrame = index
        }
        editDefaultObjectEndFrame = index
        
        let size = subjectCurrentFrame?.size ?? CGSize.zero
        let origin = subjectCurrentFrame?.origin ?? CGPoint.zero
        
        if !(size == .zero && origin == .zero) {
            let frame = Frame(height: Int(size.height), width: Int(size.width), x: Int(origin.x), y: Int(origin.y))
            let box = BoundingBox(zframe: frame, index: index)
            
            if let row = self.editSubjectCordinates.firstIndex(where: {$0.frameIndex == index}) {
                self.editSubjectCordinates[row].frame = frame
            } else {
                self.editSubjectCordinates.append(box)
            }
            enableDisableRetake(enable: true)
            enableDisableSubmit(enable: true)
        }
    }
    
    private func getDefaultObjectMissingFrames() {
        
        if (editDefaultObjectStartFrame != nil) && (editDefaultObjectEndFrame != nil) && isObjectTracking && selectedDDObject == arrDropDownObject.first {
            
            // Existing bounding box frames
            
            let editFirstIndex = editDefaultObjectStartFrame ?? 0
            let editEndindex = editDefaultObjectEndFrame ?? 0
            
            let missingStartFrame = editObjectExistingFrames.firstIndex(where: {editFirstIndex >= $0.startFrame && editFirstIndex <= $0.endFrame ?? 0})
            let missingEndFrame = editObjectExistingFrames.firstIndex(where: {editEndindex >= $0.startFrame && editEndindex <= $0.endFrame ?? 0})
            
            if missingStartFrame != nil && missingEndFrame != nil {
                let activity = Activity(activityName: "", sTime: editObjectExistingFrames[missingStartFrame ?? 0].startFrame, activityIndex: 0)
                activity.endFrame = editObjectExistingFrames[missingEndFrame ?? 0].endFrame
                
                if missingStartFrame == missingEndFrame {
                   editObjectExistingFrames.remove(at: missingStartFrame ?? 0)
                } else {
                    editObjectExistingFrames.remove(at: missingStartFrame ?? 0)
                    editObjectExistingFrames.remove(at: missingEndFrame ?? 0)
                }
                
                for (index, object) in editObjectExistingFrames.enumerated().reversed() {
                    if activity.startFrame < object.startFrame && activity.endFrame! > object.endFrame! {
                        editObjectExistingFrames.remove(at: index)
                    }
                }
                
                editObjectExistingFrames.append(activity)
            } else if missingStartFrame != nil {
                editObjectExistingFrames[missingEndFrame ?? 0].endFrame = editEndindex
                
                for (index, object) in editObjectExistingFrames.enumerated().reversed() {
                    if editObjectExistingFrames[missingEndFrame ?? 0].startFrame < object.startFrame && editObjectExistingFrames[missingEndFrame ?? 0].endFrame! > object.endFrame! {
                        editObjectExistingFrames.remove(at: index)
                    }
                }
            } else if missingEndFrame != nil {
                editObjectExistingFrames[missingStartFrame ?? 0].endFrame = editFirstIndex
                
                for (index, object) in editObjectExistingFrames.enumerated().reversed() {
                    if editObjectExistingFrames[missingStartFrame ?? 0].startFrame < object.startFrame && editObjectExistingFrames[missingStartFrame ?? 0].endFrame! > object.endFrame! {
                        editObjectExistingFrames.remove(at: index)
                    }
                }
            } else {
                let activity = Activity(activityName: "", sTime: editFirstIndex, activityIndex: 0)
                activity.endFrame = editEndindex
                
                for (index, object) in editObjectExistingFrames.enumerated().reversed() {
                    if activity.startFrame < object.startFrame && activity.endFrame! > object.endFrame!  {
                        editObjectExistingFrames.remove(at: index)
                    }
                }
                
                editObjectExistingFrames.append(activity)
            }
            
            editObjectExistingFrames = editObjectExistingFrames.sorted(by: {$0.startFrame < $1.startFrame})
            
            editObjectMissingFrames = []
            
            // Missing bounding box Frames
            
            if let _ = editObjectExistingFrames.first(where: {$0.startFrame == 0}) {
                
            } else {
                 let activity = Activity(activityName: "", sTime: 0, activityIndex: 0)
                 activity.endFrame = (editSubjectCordinates.first?.frameIndex ?? 0) - 1
                 editObjectMissingFrames.append(activity)
            }
            
//            if editObjectExistingFrames.first?.startFrame != 0 {
//                let activity = Activity(activityName: "", sTime: 0, activityIndex: 0)
//                activity.endFrame = (editSubjectCordinates.first?.frameIndex ?? 0) - 1
//                editObjectMissingFrames.append(activity)
//            }
            
//            if editObjectExistingFrames.last?.endFrame != (player.numberOfFrames - 1) {
//                let activity = Activity(activityName: "", sTime: (editObjectExistingFrames.last?.endFrame ?? 0) + 1, activityIndex: 0)
//                activity.endFrame = player.numberOfFrames - 1
//                editObjectMissingFrames.append(activity)
//            }
            
            if let _ = editObjectExistingFrames.first(where: {$0.endFrame == (player.numberOfFrames - 1)}) {
                
            } else {
                 let activity = Activity(activityName: "", sTime: (editObjectExistingFrames.last?.endFrame ?? 0) + 1, activityIndex: 0)
                 activity.endFrame = player.numberOfFrames - 1
                 editObjectMissingFrames.append(activity)
            }
            
            for (index, object) in editObjectExistingFrames.enumerated() {
                if index != 0 {
                    if (object.startFrame - (editObjectExistingFrames[index - 1].endFrame ?? 0)) != 1 {
                        let activity = Activity(activityName: "", sTime: (editObjectExistingFrames[index - 1].endFrame ?? 0) + 1, activityIndex: 0)
                        activity.endFrame = object.startFrame - 1
                        
                        if activity.startFrame != activity.endFrame {
                            editObjectMissingFrames.append(activity)
                        }
                    }
                }
            }
            
            coloredProgress()
        }
    }
    
    private func saveObjectChanges() {
        
        if editSubjectCordinates.count > 0 {
            if let row = self.subjectEditedCordinates.firstIndex(where: {$0.label == selectedDDObject}) {
                self.subjectEditedCordinates[row].boundingBox = []
                self.subjectEditedCordinates[row].boundingBox = editSubjectCordinates
            } else {
                let object = Object(label: selectedDDObject ?? "", boundingBox: editSubjectCordinates)
                self.subjectEditedCordinates.append(object)
            }
            objectExistingFrames = editObjectExistingFrames
            objectMissingFrames = editObjectMissingFrames
            enableDisableSubmit(enable: true)
        } else if selectedDDObject == arrDropDownObject.first {
            // Alternative Solution Starts
            if let _ = self.subjectEditedCordinates.firstIndex(where: {$0.label == selectedDDObject}) {
                coloredProgress()
            } else {
                editObjectMissingFrames = []
                let activity = Activity(activityName: "", sTime: 0, activityIndex: 0)
                activity.endFrame = player.numberOfFrames - 1
                editObjectMissingFrames.append(activity)
            }
            objectExistingFrames = editObjectExistingFrames
            objectMissingFrames = editObjectMissingFrames
            // Alternative Solution Ends
        }
    }
    
    private func addObjectViews() {
        
        guard arrDropDownObject.count > 0 else {
            let view = ResizableView()
            arrOfSubjectView.append(view)
            return
        }
        
        for value in arrDropDownObject {
            let view = ResizableView()
            if value == arrDropDownObject.first { view.isDefaultObject = true }
            arrOfSubjectView.append(view)
        }
    }
    
    private func getObjectColor(object: String) -> CGColor {
        let data = Data(object.utf8)
        let hexString = data.map{ String(format:"%02x", $0) }.joined()
        return UIColor(hexString: String(hexString.prefix(3))).cgColor
    }
    
    // Alternative Solution Starts
    private func removeSelectedObject() {
        if let index = self.subjectEditedCordinates.firstIndex(where: {$0.label == selectedDDObject}), isFirstObjectRecorded {
            if subjectRedoEditedCordinates.first(where: {$0.label == selectedDDObject})?.boundingBox.count ?? 0 > 0 {
                enableDisableRetake(enable: true)
                enableDisableSubmit(enable: true)
            }
            self.isFirstObjectRecorded = false
            self.subjectEditedCordinates.remove(at: index)
            if selectedDDObject == arrDropDownObject.first {
                editObjectExistingFrames = []
                editObjectMissingFrames = []
            }
        }
    }
    // Alternative Solution Ends
}

//MARK: DropDown Methods
extension EditVideoVC {
    func listShowed() {
        deactivateOnDropDown()
    }
    
    func listSelected(_ selectedText: String?) {
        
        saveObjectChanges()
        
        if selectedDropDownIndex != 0 {
            nameChangeSubmitSave(save: true, text: nil)
            enableDisableSubmit(enable: false)
            submitSaveToggle = true
            if selectedDropDownIndex == 1 {
                subjectRedoActivities = self.subjectActivities
                selectedDDActivity = selectedText
                selectedDDObject = nil
                activateActivity()
                deactivateObject()
                                
            } else {
                subjectRedoEditedCordinates = self.subjectEditedCordinates
                selectedDDObject = selectedText
                selectedDDActivity = nil
                deactivateActivity()
                activateObject()
            }
            
            let tempSubjectActivities = subjectActivities
            subjectActivities = []
            self.resetObjectAndActivityLabel(index: 1)
            subjectActivities = tempSubjectActivities
        }
        
        hideDropDown()
        movePlayerToBegining()
        
        print("listSelected---\(selectedDDActivity ?? "")")
    }
    
    func hideDropDown() {
        if selectedDropDownIndex != 0 {
            if selectedDropDownIndex == 1 {
                textFieldEditActivity.touchAction()
            } else {
                textFieldEditObject.touchAction()
            }
            selectedDropDownIndex = 0
        }
    }
    
    func activateActivity() {
        editActivityOn = true
        self.buttonEditActivity.setImage(#imageLiteral(resourceName: "activity_dark"), for: .normal)
        self.buttonEditActivity.backgroundColor = #colorLiteral(red: 1, green: 1, blue: 1, alpha: 0.66)
        isFirstActivityRecorded = true
        
        // Alternative Solution Starts
        let tempSubjectActivities = subjectActivities
        removeSameActivityLabel()
        isFirstActivityRecorded = true
        coloredProgress()
        subjectActivities = []
        subjectActivities = tempSubjectActivities
        subjectRedoActivities = self.subjectActivities //temp
        //enableDisableRetake(enable: true)
        // Alternative Solution Ends
    }
    
    func activateObject() {
        editObjectOn = true
        editSubjectCordinates = []
        
        // Alternative Solution Starts
        isFirstObjectRecorded = true
        subjectRedoEditedCordinates = subjectEditedCordinates
        if selectedDDObject == arrDropDownObject.first {
            let missingFrames = editObjectMissingFrames
            editObjectMissingFrames = []
            let activity = Activity(activityName: "", sTime: 0, activityIndex: 0)
            activity.endFrame = player.numberOfFrames - 1
            editObjectMissingFrames.append(activity)
            self.coloredProgress()
            editObjectMissingFrames = []
            editObjectMissingFrames = missingFrames
        } else {
            self.coloredProgress()
        }
        //enableDisableRetake(enable: true)
        // Alternative Solution Ends
        
        self.buttonEditObject.setImage(#imageLiteral(resourceName: "object_dark"), for: .normal)
        self.buttonEditObject.backgroundColor = #colorLiteral(red: 1, green: 1, blue: 1, alpha: 0.66)
    }
    
    func deactivateActivity() {
        textFieldEditActivity.selectedIndex = .none
        editActivityOn = false
        selectedDDActivity = nil//SEE
        self.textFieldEditActivity.selectedIndex = nil//SEE
        self.buttonEditActivity.setImage(#imageLiteral(resourceName: "activity_white"), for: .normal)
        self.buttonEditActivity.backgroundColor = #colorLiteral(red: 0, green: 0, blue: 0, alpha: 0.66)
        isFirstActivityRecorded = true
    }
    
    func deactivateObject() {
        textFieldEditObject.selectedIndex = .none
        editObjectOn = false
        selectedDDObject = nil
        editSubjectCordinates = []
        self.textFieldEditObject.selectedIndex = nil
        self.buttonEditObject.setImage(#imageLiteral(resourceName: "object_white"), for: .normal)
        self.buttonEditObject.backgroundColor = #colorLiteral(red: 0, green: 0, blue: 0, alpha: 0.66)
    }
    
    func deactivateOnDropDown() {
        if selectedDropDownIndex != 0 {
            if selectedDropDownIndex == 1 {
                textFieldEditActivity.touchAction()
            } else {
                textFieldEditObject.touchAction()
            }
            selectedDropDownIndex = 0
        }
    }
    
}

//MARK: Gesture Delegate
extension EditVideoVC: UIGestureRecognizerDelegate {
    
    public func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        
        if (touch.view?.isDescendant(of: self.player.playerView))! {
            return true
        }
        
        return false
    }
}


